# 统一练习规格：任务管理核心

本页是前端、MyBatis 和 Spring Boot 练习共同遵守的教学版契约。后续练习如果没有特别说明，不得自行改字段名、状态值或接口地址。

## 1. 业务目标与边界

员工可以查看项目任务，按条件查询任务，新建或编辑任务，并按允许的顺序改变任务状态。

本轮包含：列表、查询、新建、详情、编辑、状态变更。以下内容留到结课综合项目：登录与复杂权限、评论、附件、操作历史、通知、任务看板、进度更新、批量操作和统计报表。

## 2. 固定枚举

| 字段 | 值 | 页面显示 |
| --- | --- | --- |
| status | `TODO` | 未开始 |
| status | `IN_PROGRESS` | 进行中 |
| status | `DONE` | 已完成 |
| priority | `LOW` | 低 |
| priority | `MEDIUM` | 中 |
| priority | `HIGH` | 高 |

允许的状态迁移：

- `TODO -> IN_PROGRESS`
- `IN_PROGRESS -> TODO`
- `IN_PROGRESS -> DONE`
- `DONE -> IN_PROGRESS`

不允许 `TODO -> DONE`，也不允许更新为当前状态。非法迁移返回业务错误 `TASK_STATUS_INVALID`。

## 3. 数据模型

### projects

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| project_code | VARCHAR(20) | 必填、唯一 |
| name | VARCHAR(100) | 必填 |

### employees

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| employee_number | VARCHAR(20) | 必填、唯一 |
| name | VARCHAR(50) | 必填 |

### tasks

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| id | BIGINT | 主键、自增 |
| task_code | VARCHAR(20) | 必填、唯一，由后端生成 |
| project_id | BIGINT | 必填，关联 projects |
| title | VARCHAR(100) | 必填，去除首尾空格后 1～100 字符 |
| description | VARCHAR(1000) | 可空，最多 1000 字符 |
| assignee_id | BIGINT | 必填，关联 employees |
| priority | VARCHAR(20) | 必填，使用固定枚举 |
| status | VARCHAR(20) | 必填，新建时固定为 `TODO` |
| start_date | DATE | 必填 |
| due_date | DATE | 必填，不得早于 start_date |
| created_at | DATETIME | 由后端写入 |
| updated_at | DATETIME | 由后端写入 |

列表和详情向前端返回 `projectName`、`assigneeName`，但不在 tasks 表中重复保存这两个名称。

## 4. 初始数据

| 类型 | ID | 编号 | 名称 |
| --- | ---: | --- | --- |
| 项目 | 1 | PJ-001 | 社内门户更新 |
| 项目 | 2 | PJ-002 | 勤怠系统改善 |
| 员工 | 1 | E0001 | 山田太郎 |
| 员工 | 2 | E0002 | 佐藤花子 |
| 员工 | 3 | E0003 | 鈴木一郎 |

至少准备三条任务，分别覆盖 `TODO`、`IN_PROGRESS`、`DONE`。日期应选择运行练习时容易识别的固定测试日期，不依赖“今天”。

固定任务数据如下，所有前端模拟数据、SQL 初始数据和接口测试都从这三条开始：

| id | taskCode | projectId | title | assigneeId | priority | status | startDate | dueDate |
| ---: | --- | ---: | --- | ---: | --- | --- | --- | --- |
| 1 | T-0001 | 1 | 门户首页需求确认 | 1 | HIGH | TODO | 2026-04-01 | 2026-04-05 |
| 2 | T-0002 | 1 | 公告列表页面实现 | 2 | MEDIUM | IN_PROGRESS | 2026-04-02 | 2026-04-12 |
| 3 | T-0003 | 2 | 打卡记录查询测试 | 3 | LOW | DONE | 2026-03-20 | 2026-03-31 |

description 可由学员补充，但不得改变其他初始值。新建任务编号按已有最大数字加 1，例如下一条为 `T-0004`；并发下的正式编号生成方案属于综合项目范围。

## 5. 学员工作目录

```text
workhub-task-practice/
├─ frontend/
│  ├─ tasks.html
│  ├─ task-create.html
│  ├─ task-detail.html
│  ├─ task-edit.html
│  ├─ css/
│  └─ js/
├─ backend/
│  ├─ pom.xml
│  ├─ src/
│  └─ db/
├─ evidence/
└─ README.md
```

每个阶段综合练习完成后执行四步：运行功能、按验收项检查、在 `evidence/` 记录结果、保存一次可识别的 Git 提交。不能运行或验收未通过时，不进入下一阶段。

## 6. HTTP API

基础路径为 `/api`。

| 正式来源 | 方法与路径 | 教学版用途 |
| --- | --- | --- |
| API-TASK-001 | `GET /api/tasks` | 条件查询和分页 |
| API-TASK-002 | `GET /api/tasks/{id}` | 详情 |
| API-TASK-003 | `POST /api/tasks` | 新建 |
| API-TASK-004 | `PUT /api/tasks/{id}` | 编辑基本信息 |
| API-TASK-005 | `PUT /api/tasks/{id}/status` | 教学版把状态作为子资源整体更新；正式项目仍以正式 API 设计为准 |

列表查询参数：`keyword`、`projectId`、`assigneeId`、`status`、`page`、`pageSize`。`keyword` 对任务编号和标题做模糊查询；默认 `page=1`、`pageSize=20`，排序为 `updatedAt DESC, id DESC`。

所有响应使用统一结构：

```json
{
  "success": true,
  "code": "OK",
  "message": "处理成功",
  "data": {}
}
```

分页数据放在 `data` 中：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

新建请求不接收 `id`、`taskCode`、`status` 和时间字段。编辑请求只携带可编辑字段。状态请求格式为：

```json
{
  "status": "IN_PROGRESS"
}
```

## 7. 错误约定

| 情况 | HTTP 状态 | code |
| --- | ---: | --- |
| 参数格式或必填错误 | 400 | `VALIDATION_ERROR` |
| 非法状态迁移 | 400 | `TASK_STATUS_INVALID` |
| 任务、项目或员工不存在 | 404 | `RESOURCE_NOT_FOUND` |
| 未预期服务器错误 | 500 | `INTERNAL_ERROR` |

错误响应仍使用统一结构，`success` 为 `false`。页面向用户显示可理解的信息；详细异常只写入后端日志。

## 8. 技术基线

- 前端：原生 HTML、CSS、JavaScript，后续可迁移到 React。
- 后端：课程当前采用的 Java LTS、Spring Boot 3.x、MyBatis。
- 数据库：日常练习使用 MySQL 8.x；结课 Company WorkHub 按正式设计书使用其规定的数据库。
- 字符编码：UTF-8；日期交换格式：`yyyy-MM-dd`。
