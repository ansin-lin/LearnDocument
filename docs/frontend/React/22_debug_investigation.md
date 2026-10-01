# 第 22 章 调试与项目调查

## 本章目标

- 使用 Console、Network、Sources、Application 和 React DevTools 定位问题。
- 沿真实数据流调查“编辑页没有显示数据”。
- 形成可交接的证据、原因、修改与回归记录。

## 1. 工具分工

- Elements：DOM、语义、样式和事件目标。
- Console：运行错误、警告与临时日志；不记录敏感数据。
- Network：URL、方法、状态码、请求参数、响应、耗时与取消。
- Sources：断点、调用栈、作用域和 Source Map。
- Application：Cookie/Storage/缓存；查看敏感信息时注意屏幕与日志。
- React DevTools：组件树、Props、State、Context 与 Profiler。

先根据症状选择工具，不要所有问题都从 `console.log` 开始：

| 症状 | 优先工具 |
| --- | --- |
| 样式或 DOM 不正确 | Elements |
| 请求没有发送或响应异常 | Network |
| 运行时异常 | Console、Sources |
| State/Props 不符合预期 | React DevTools |
| Cookie 或 Storage 问题 | Application |
| 页面卡顿 | React Profiler |

## 2. 调查路径

症状：点击员工编辑后，画面没有初始值。

```text
Route
 ↓ 路径是否匹配
Page
 ↓ 是否被渲染
URL Parameter
 ↓ id 是否存在且有效
Effect / Hook
 ↓ 依赖与取消是否正确
Service
 ↓ URL/方法是否正确
Network / API
 ↓ 状态码与 JSON
State
 ↓ 成功后是否保存
Props
 ↓ 表单是否收到数据
JSX
 ↓ value 是否绑定到正确字段
```

每一步先记录事实，再下结论。例如“Network 没请求”说明问题位于请求之前；200 且响应正确后再检查状态和表单，不要先改 CSS。

### 2.1 Sources 断点的基本使用

1. 在事件处理函数或请求成功分支设置断点；
2. 从画面重新执行操作；
3. 查看 Call Stack 确认调用路径；
4. 查看 Scope 中的 `employeeId`、响应和 State 更新参数；
5. 单步执行，确认程序在哪个条件偏离预期。

构建工具通过 Source Map 把浏览器代码映射回 JSX 源文件。生产环境是否发布 Source Map 应遵循项目安全和运维规则。

## 3. 高频原因

- `/employees/new` 被动态 `:id` 错误处理。
- `useParams` 的字符串未校验，得到 `NaN`。
- Effect 漏了 `employeeId` 依赖，路由切换仍用旧数据。
- 旧请求晚到覆盖新员工。
- 表单初始 State 只在第一次渲染读取 Props，详情到达后没有按“草稿”规则初始化。
- 接口字段为 `joined_date`，前端却读取 `joinedDate`。

- 组件在 StrictMode 开发检查中被额外执行，暴露了缺少 cleanup 的 Effect；不要通过删除 StrictMode 掩盖副作用问题。
- Service Mock 没有重置，测试通过但浏览器真实请求失败。

## 4. 完整调查案例

症状：打开 `/employees/12/edit` 后请求返回 200，但姓名输入框为空。

### 4.1 记录事实

```text
Route：/employees/12/edit 匹配 EmployeeEditPage
Param：id = "12"，转换后 employeeId = 12
Network：GET /api/employees/12 → 200
Response：{ "id": 12, "name": "田中太郎", ... }
React DevTools：EmployeeEditPage 的 employee 已有 name
EmployeeForm Props：initialEmployee.name = "田中太郎"
DOM：input value = ""
```

事实表明请求、响应和父组件 State 正常，问题位于表单草稿初始化之后。

### 4.2 找到根因

```jsx
const [formValues, setFormValues] = useState({
  name: initialEmployee?.name ?? '',
});
```

`useState` 的初始值只在组件首次建立时使用。首次渲染时详情尚未到达，因此表单保存了空字符串；之后 Props 改变不会自动覆盖 State。

### 4.3 按规格修正

如果表单在员工 ID 改变时应重新建立草稿，可以让父页面在数据准备完成后渲染，并使用稳定 ID 作为 `key`：

```jsx
return employee
  ? <EmployeeForm key={employee.id} initialEmployee={employee} />
  : <LoadingIndicator />;
```

如果页面允许后台刷新数据，还必须先定义是否覆盖用户正在编辑的草稿，不能看到 Props 变化就无条件 `setFormValues()`。

### 4.4 横向影响与回归

检查新增表单、其他编辑表单、详情切换和返回导航是否使用相同初始化方式。回归至少覆盖：直接打开编辑 URL、列表进入编辑、从 ID 12 切到 ID 13、请求失败、用户编辑后不被无意覆盖。

## 5. 企业改修记录

一份可 Review 的记录至少包含：现象与重现条件、期待结果、调查路径、根因证据、影响范围、修改文件、测试项目、结果证据和未验证风险。日本项目中常见的“横展開/影響調査”应落实为同类页面、共通组件、接口调用方和回归项，而不是只写一句“无影响”。

推荐格式：

```text
现象：员工编辑页姓名为空
再现条件：直接打开 /employees/12/edit
期待结果：显示 API 返回的姓名
实际结果：输入框为空
根因：详情到达前初始化表单，后续 Props 未形成新草稿
修正：数据准备后按 employee.id 建立表单实例
影响范围：所有异步取得初始值的编辑表单
测试结果：直接打开、列表迁移、ID 切换、失败路径均通过
Evidence：Network、React DevTools、测试输出
未验证：生产后端高延迟环境
```

Evidence 中不得包含密码、Cookie、Token、真实个人资料和不必要的内部信息。

## 6. 调试后的清理

- 删除临时 `console.log`、断点语句和测试用固定数据；
- 保留真正有运维价值且不含敏感信息的日志；
- 运行受影响测试和构建；
- 使用 `git diff` 确认没有无关改动；
- 把原因和验证结果写入改修记录。

## 7. 练习

1. 人为删除 Effect 依赖，用上述路径调查并修复。
2. 制造字段名不一致，分别保存 Network 与 React DevTools 证据。
3. 给“编辑页空白”编写不具合票：再现步骤、原因、修正、横向影响和测试结果。
4. 从 `package.json` 开始，画出当前员工详情页的完整依赖链。
5. 为完整案例制作一份包含再现、证据、根因、横展開和回归结果的不具合票。

## 本章检查点

- [ ] 能沿 Route → Param → Hook/Effect → Service → Network → State → Props → JSX 调查。
- [ ] 能用事实证据区分请求前、请求中和渲染层问题。
- [ ] 能提交根因、影响范围、横展開、测试证据和未验证风险。
