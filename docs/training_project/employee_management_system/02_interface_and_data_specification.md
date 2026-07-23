# 员工管理系统接口与数据规格

> 本文件是 Python、Java、Vue、React、SQL 和测试课程共用的契约。课程示例需要新增或修改字段时，应先更新本文件并确认影响范围。

## 一、命名约定

| 范围 | 约定 | 示例 |
| --- | --- | --- |
| URL | 小写复数名词，单词使用连字符 | `/api/v1/employees` |
| JSON 字段 | camelCase | `employeeId` |
| Python 变量 | snake_case | `employee_id` |
| Java/TypeScript 变量 | camelCase | `employeeId` |
| 数据库表和列 | snake_case | `employee_id` |
| 常量代码 | UPPER_SNAKE_CASE | `SYSTEM_ADMIN` |

不同语言内部命名可以遵循各自规范，但对外 JSON 契约保持 camelCase。

## 二、共通接口规则

- 基础路径：`/api/v1`
- 数据格式：JSON，文件上传和下载除外
- 字符编码：UTF-8
- 日期：`YYYY-MM-DD`
- 日期时间：ISO 8601
- 列表默认页码：`1`
- 列表默认每页：`20`
- 每页最大数量：`100`
- 认证失败：`401`
- 权限不足：`403`
- 资源不存在：`404`
- 数据冲突：`409`

## 三、数据字典

### 3.1 Employee

| JSON 字段 | 类型 | 必填 | 规则 |
| --- | --- | :---: | --- |
| `employeeId` | string | 是 | `EMP` + 4 位数字，创建后不可修改 |
| `employeeName` | string | 是 | 1～100 字符 |
| `email` | string | 是 | 邮箱格式，忽略大小写后唯一 |
| `departmentId` | string | 是 | 必须存在且有效 |
| `departmentName` | string | 响应 | 由部门数据取得 |
| `hireDate` | string(date) | 是 | `YYYY-MM-DD` |
| `status` | string | 是 | `ACTIVE`、`LEAVE`、`RETIRED` |
| `version` | integer | 更新 | 非负整数，用于并发控制 |
| `createdAt` | string(datetime) | 响应 | 创建时间 |
| `updatedAt` | string(datetime) | 响应 | 最后更新时间 |

### 3.2 Department

| JSON 字段 | 类型 | 必填 | 规则 |
| --- | --- | :---: | --- |
| `departmentId` | string | 是 | `D` + 3 位数字，创建后不可修改 |
| `departmentName` | string | 是 | 1～100 字符，唯一 |
| `active` | boolean | 是 | 是否有效 |
| `version` | integer | 更新 | 并发控制 |
| `createdAt` | string(datetime) | 响应 | 创建时间 |
| `updatedAt` | string(datetime) | 响应 | 最后更新时间 |

### 3.3 UserAccount

| JSON 字段 | 类型 | 必填 | 规则 |
| --- | --- | :---: | --- |
| `userId` | string | 是 | 账号内部编号 |
| `loginId` | string | 是 | 登录 ID，唯一 |
| `roleCode` | string | 是 | `SYSTEM_ADMIN`、`HR_STAFF`、`VIEWER` |
| `employeeId` | string/null | 否 | 关联员工，可为空 |
| `active` | boolean | 是 | 是否可以登录 |

密码散列不出现在普通接口响应中。

## 四、员工 API

| 方法 | 路径 | 作用 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/employees` | 分页查询员工 | 全部角色 |
| `GET` | `/api/v1/employees/{employeeId}` | 查询员工详情 | 全部角色 |
| `POST` | `/api/v1/employees` | 新增员工 | SYSTEM_ADMIN、HR_STAFF |
| `PUT` | `/api/v1/employees/{employeeId}` | 更新员工 | SYSTEM_ADMIN、HR_STAFF |
| `DELETE` | `/api/v1/employees/{employeeId}` | 逻辑删除员工 | SYSTEM_ADMIN |
| `POST` | `/api/v1/employees/imports` | CSV 导入 | SYSTEM_ADMIN、HR_STAFF |
| `GET` | `/api/v1/employees/export` | Excel 导出 | 全部角色 |

### 4.1 查询员工列表

```http
GET /api/v1/employees?page=1&size=20&employeeName=Tanaka&departmentId=D001&status=ACTIVE
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | :---: | --- |
| `page` | integer | 否 | 从 1 开始 |
| `size` | integer | 否 | 1～100 |
| `employeeName` | string | 否 | 姓名部分匹配 |
| `departmentId` | string | 否 | 部门编号 |
| `status` | string | 否 | 员工状态 |
| `sort` | string | 否 | 例如 `hireDate,desc` |

成功响应：

```json
{
  "items": [
    {
      "employeeId": "EMP0001",
      "employeeName": "Tanaka Taro",
      "email": "tanaka@example.com",
      "departmentId": "D001",
      "departmentName": "Development",
      "hireDate": "2024-04-01",
      "status": "ACTIVE",
      "version": 0,
      "createdAt": "2026-07-15T09:00:00+09:00",
      "updatedAt": "2026-07-15T09:00:00+09:00"
    }
  ],
  "page": 1,
  "size": 20,
  "total": 1
}
```

### 4.2 新增员工

```http
POST /api/v1/employees
Content-Type: application/json
```

请求 Body：

```json
{
  "employeeId": "EMP0001",
  "employeeName": "Tanaka Taro",
  "email": "tanaka@example.com",
  "departmentId": "D001",
  "hireDate": "2024-04-01",
  "status": "ACTIVE"
}
```

成功状态：`201 Created`

### 4.3 更新员工

```http
PUT /api/v1/employees/EMP0001
Content-Type: application/json
```

请求 Body：

```json
{
  "employeeName": "Tanaka Taro",
  "email": "new-tanaka@example.com",
  "departmentId": "D002",
  "hireDate": "2024-04-01",
  "status": "ACTIVE",
  "version": 0
}
```

更新成功后 `version` 增加。版本不一致返回 `409 Conflict`。

### 4.4 逻辑删除员工

```http
DELETE /api/v1/employees/EMP0001
```

成功状态：`204 No Content`

数据库记录不物理删除，员工状态更新为 `RETIRED` 并记录审计日志。

## 五、部门 API

| 方法 | 路径 | 作用 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/departments` | 查询部门 | 全部角色 |
| `GET` | `/api/v1/departments/{departmentId}` | 查询部门详情 | 全部角色 |
| `POST` | `/api/v1/departments` | 新增部门 | SYSTEM_ADMIN、HR_STAFF |
| `PUT` | `/api/v1/departments/{departmentId}` | 修改部门 | SYSTEM_ADMIN、HR_STAFF |

部门响应示例：

```json
{
  "departmentId": "D001",
  "departmentName": "Development",
  "active": true,
  "version": 0,
  "createdAt": "2026-07-15T09:00:00+09:00",
  "updatedAt": "2026-07-15T09:00:00+09:00"
}
```

## 六、认证 API

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | 登录 |
| `POST` | `/api/v1/auth/logout` | 退出 |
| `GET` | `/api/v1/auth/me` | 查询当前用户 |

贯穿项目统一使用 Bearer Access Token。登录请求：

```json
{
  "loginId": "training-user",
  "password": "example-password"
}
```

登录成功响应结构：

```json
{
  "accessToken": "example-access-token",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "userId": "U0001",
    "loginId": "training-user",
    "roleCode": "HR_STAFF",
    "employeeId": "EMP0001"
  }
}
```

后续请求使用：

```http
Authorization: Bearer example-access-token
```

示例密码和 Token 仅用于说明数据结构，不是可用凭据。课程可以另外讲解 Cookie 和 Session，但贯穿项目的前后端联调以本契约为准。

## 七、CSV 导入规格

CSV 表头：

```csv
employeeId,employeeName,email,departmentId,hireDate,status
```

示例：

```csv
employeeId,employeeName,email,departmentId,hireDate,status
EMP0001,Tanaka Taro,tanaka@example.com,D001,2024-04-01,ACTIVE
EMP0002,Sato Hanako,sato@example.com,D002,2025-04-01,LEAVE
```

导入结果示例：

```json
{
  "totalRows": 2,
  "successRows": 0,
  "failedRows": 1,
  "errors": [
    {
      "row": 3,
      "field": "departmentId",
      "code": "DEPARTMENT_NOT_FOUND",
      "message": "指定的部门不存在"
    }
  ]
}
```

基础版本只要存在一条错误，全部数据都不登记。

## 八、统一错误结构

```json
{
  "code": "EMPLOYEE_NOT_FOUND",
  "message": "指定的员工不存在",
  "fieldErrors": [],
  "traceId": "example-trace-id"
}
```

字段说明：

| 字段 | 作用 |
| --- | --- |
| `code` | 稳定的业务错误代码 |
| `message` | 面向用户或开发者的错误说明 |
| `fieldErrors` | 字段校验错误列表 |
| `traceId` | 日志调查用追踪编号 |

字段错误示例：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "输入内容不正确",
  "fieldErrors": [
    {
      "field": "email",
      "code": "INVALID_FORMAT",
      "message": "邮箱格式不正确"
    }
  ],
  "traceId": "example-trace-id"
}
```

## 九、基础错误代码

| HTTP 状态 | 业务代码 | 场景 |
| ---: | --- | --- |
| `400` | `INVALID_REQUEST` | 请求格式错误 |
| `401` | `AUTHENTICATION_REQUIRED` | 未登录或凭据无效 |
| `403` | `ACCESS_DENIED` | 权限不足 |
| `404` | `EMPLOYEE_NOT_FOUND` | 员工不存在 |
| `404` | `DEPARTMENT_NOT_FOUND` | 部门不存在 |
| `409` | `EMPLOYEE_ALREADY_EXISTS` | 员工编号或邮箱重复 |
| `409` | `OPTIMISTIC_LOCK_CONFLICT` | 数据版本冲突 |
| `413` | `FILE_TOO_LARGE` | 文件超过限制 |
| `422` | `VALIDATION_ERROR` | 字段校验失败 |
| `500` | `INTERNAL_ERROR` | 未预期服务器异常 |

## 十、数据库逻辑模型

```text
departments 1 ────── * employees
employees   1 ────── 0..1 user_accounts
roles       1 ────── * user_accounts
user_accounts 1 ──── * audit_logs
```

推荐表：

- `departments`
- `employees`
- `roles`
- `user_accounts`
- `audit_logs`
- `import_jobs`
- `import_errors`

数据库字段、主外键和索引在 SQL 与框架持久化章节中逐步设计，但必须保持本文件中的业务字段和关联关系。
