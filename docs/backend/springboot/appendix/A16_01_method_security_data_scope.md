# A16-1 URL权限、方法权限与数据范围设计

[第16章](../mainline/16_authorization_and_data_scope.md)只实现“USER只读本人详情、ADMIN可读列表和执行写操作”。本附录解释URL授权与方法授权的边界，并用该项目讨论更复杂的数据范围；部门、上司和组织树方案是设计扩展，不要求加入当前工程。最终实现见[R16](../reference/R16_authorization_complete_source.md)。

## 一、从身份到操作，再到记录

认证得到`Authentication`，其中包含用户名和`GrantedAuthority`集合。授权至少有两个问题：是否允许调用这类接口；即使允许，是否可访问这条记录。URL规则擅长按HTTP方法和路径判断，无法仅凭`/employees/1002`知道1002是否是当前用户本人。

```text
请求 → SecurityFilterChain
          └─ HttpMethod＋路径＋角色（功能权限）
       → Controller → EmployeeService代理
          └─ @PreAuthorize＋可信账号关系（数据范围）
       → Mapper → 数据库
```

前端隐藏按钮不构成授权。客户端提供的`id`是目标记录编号，客户端传来的`role`或`currentUserId`也不能当作当前身份。当前用户名来自服务器恢复的`Authentication`，与员工编号的关联来自`app_users.employee_id`。

## 二、URL规则的读取方式

| 本项目写法 | 判断对象 | 结果 |
| --- | --- | --- |
| `permitAll()` | 公开健康、CSRF和登录入口 | 无需认证，但POST登录仍受CSRF |
| `authenticated()` | 当前请求是否有已认证身份 | 不限定USER或ADMIN |
| `hasRole("ADMIN")` | 是否具有`ROLE_ADMIN` | 参数不写`ROLE_`前缀 |
| `hasAuthority("ROLE_ADMIN")` | 是否有完全相同的权限字符串 | 参数要写完整值 |
| `anyRequest().denyAll()` | 前述规则未匹配的请求 | 默认拒绝 |

`requestMatchers(HttpMethod.GET, "/employees")`只匹配列表GET；`/employees/*`匹配一个路径片段的详情。顺序重要：更具体的规则应放在兜底之前，首个匹配规则决定授权。`ROLE_`来自第15章`User.withUsername(...).roles(...)`对数据库`USER`／`ADMIN`的转换；若数据库改为存完整权限，应同步评审加载方式与所有表达式。

## 三、方法授权如何工作

`@EnableMethodSecurity`放在`SecurityConfig`上，为Spring管理的Service调用启用方法安全拦截。`@PreAuthorize`放在`EmployeeService.findById(Long id)`声明上，进入目标方法前计算表达式：

```java
@PreAuthorize("@employeeAccess.canRead(authentication, #p0)")
EmployeeResponse findById(Long id);
```

`@employeeAccess`是Bean名称，`authentication`是当前安全上下文，`#p0`是第一个方法参数`id`。`EmployeeAccess.canRead`先允许ADMIN；对USER用认证用户名查询`app_users`，比较可信的`employee_id`与目标id。检查失败时Service主体不执行，表现为403。把注解放在接口还是实现方法须结合项目代理形式和调用路径评审；本项目以接口声明作为当前稳定写法，完整代码见R16。

代理边界是重点：Controller通过Spring注入的`EmployeeService`调用会经过方法拦截器；单元测试`new EmployeeServiceImpl(...)`不会。同类内部`this.findById(...)`通常绕过代理；以后新增其他Service入口不能把这种内部调用当作权限保障。应通过带Spring上下文的集成测试验证真实入口。

## 四、数据范围放在Service还是SQL

本项目仅需单条本人详情：Service入口读可信关联再判定，成本和语义都清晰。复杂列表若要求USER看本人、本部门或下属，不能先查一页全量记录，再在Java中过滤：那会使`total`泄露或错误，翻页也可能遗漏。此时通常需把可信的访问范围作为Mapper查询条件，让列表SQL与第14章的`COUNT`使用同一范围。

```sql
-- 仅为设计片段：真实项目要先确认部门与账号数据模型
SELECT id, name, department
FROM employees
WHERE department = :trustedDepartment
ORDER BY id
LIMIT :pageSize OFFSET :offset;
```

占位符表示“由服务端可信身份解析出的部门”和分页值，不是本项目可直接执行的MyBatis XML。若用户属于多个部门，要定义交集/并集、离职或转部门生效时机、管理员例外、空范围结果和部门树继承规则，再设计参数化SQL、索引与测试。不能把请求体的`department`当授权范围直接拼入SQL。

允许访问对象的入口若仅根据客户端提供的id直接读记录且不检查归属，属于不安全直接对象引用（IDOR）风险。测试要改动路径id，证明USER本人200、他人403；还要检查日志与错误体不会泄露他人记录。

## 五、复杂范围设计时怎样避免列表与COUNT分裂

假设新规格要求“主管查看所属部门员工”，先由服务端根据认证用户名查主管的部门集合，再把**同一范围参数**传给列表和COUNT。以下只是设计示意，不是本章工程可直接执行的XML；真正实现前要补账号—部门关系表、索引和部门调动规则。

```text
Authentication中的用户名
  → 后端查可访问部门集合
  → 查询条件对象（筛选条件＋可信部门集合）
      ├─ 列表SQL：WHERE 用户筛选条件 AND 部门在可信集合内
      └─ COUNT SQL：完全相同的WHERE条件
```

若只给列表加范围而COUNT不加，`total`可能透露无权访问的数量；若只给COUNT加范围，列表可能直接泄露记录。空部门集合应返回空列表和0，而不是省略WHERE变成全表查询。排序字段仍用第14章白名单，不能把范围值或排序值拼接为任意SQL。对权限变更中的旧Session，还应明确是立即生效、下次登录生效，还是由后台主动使会话失效；不同选择会改变测试和运维手顺。

当前`employeeAccess.canRead(authentication, employeeId)`适合**单条详情**：为一个目标id核对一次账号关联。若列表返回20条员工，而Java循环对每条都调用它且每次查数据库，可能形成“1次列表查询＋20次权限查询”的N+1问题，性能随页大小增加；先查整页后过滤还会使本页数量、分页边界和`total`不一致。复杂列表应从可信认证信息取得允许范围，将范围参数化地加入列表与COUNT共用的WHERE条件，并检查部门/员工关联索引与执行计划。不能直接信任请求中的部门，也不能因空范围而删掉范围条件。

## 六、Review与扩展设计练习

接到“上司可读本部门”改修票时，先调查：上司身份从哪张表取得、员工跨部门如何处理、列表和COUNT是否共用范围条件、管理员是否绕过、权限变化后旧Session如何刷新。提交影响调查表及至少五个测试场景，不直接把`hasRole("MANAGER")`加到详情URL规则就宣称完成。

Review时至少检查：入口是否经Spring代理、详情是否有同等范围保护、列表和COUNT条件是否一致、是否对每行反复查询权限、异常响应是否透露目标记录是否存在、日志是否在授权前记录了他人敏感字段。这个检查清单用于设计调查，不要求学员现在把主管权限写进员工项目；当前工程仍以第16章的矩阵为准。

## 七、官方参考资料

- [Spring Security请求级授权](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
- [Spring Security方法级授权](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
