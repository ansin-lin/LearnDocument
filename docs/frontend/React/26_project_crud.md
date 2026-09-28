# 第 26 章 React 实战：申请、确认、完成与一览

本章完成其余四个业务页面，并把申请草稿、React Router、Zustand 和后台 API 连接成完整流程。

## 1. 申请输入页面

`LeaveForm` 负责以下字段：

- 休假类型；
- 开始日和结束日；
- 申请理由；
- 引继状态。

表单值保存在 `LeaveApplyPage` 的局部 State。选择或输入结束时执行当前字段校验；点击“確認画面へ”时再执行一次全表单校验。

```text
输入/选择 → 当前字段即时校验
点击确认 → 全表单校验
           ├─ 失败：停留并聚焦第一个错误
           └─ 成功：保存 Zustand 草稿 → 确认页
```

前端校验用于及时提示，不能代替后台校验。字段规则以 API 规格为准，不在组件中临时创造另一套业务规则。

`LeaveForm` 通过 Props 接收值和错误，通过回调通知变化与提交；它不直接调用 Router、Store 或 Axios。

## 2. 申请草稿 Store

Application Store 至少保存：

```js
{
  draft: null,
  applications: [],
  loading: false,
  submitting: false,
  errorMessage: ''
}
```

并提供：

```text
setDraft(input)
clearDraft()
submitDraft()
loadApplications(query, signal)
cancelApplication(id)
reset()
```

`draft` 只是输入页到确认页之间的临时数据。用户直接打开确认 URL 且没有草稿时，应返回输入页并显示说明，不能提交空对象。

## 3. 申请确认页面

确认页使用 `ApplicationSummary` 只读显示草稿。用户可以返回修改，也可以提交：

```text
点击“申請する”
  ↓ 检查 submitting，防止重复点击
POST /api/leave-applications
  ├─ 成功：保存响应 → 清除草稿 → 完成页
  └─ 失败：保留草稿 → 显示字段或业务错误
```

后台返回字段错误时，应回到输入页并把错误显示在对应字段附近。系统错误留在确认页，允许用户恢复或重试。

请求超时不能直接判断为“保存失败”，因为后台可能已经完成保存。先查询申请一览确认是否存在刚才的记录，再决定是否重试；新增请求不要自动重试。

## 4. 申请完成页面

提交成功后导航到：

```text
/applications/{id}/complete
```

页面显示受付番号、休假期间、申请状态和返回首页/一览的入口。刷新完成页时，根据路由 ID 调用详情 API，不能再次执行新增请求。

```text
提交响应 → 完成页立即显示
刷新页面 → GET /api/leave-applications/:id → 显示同一申请
```

路由 ID 必须转换并校验。详情不存在时显示 404 业务信息，不伪装成空页面。

## 5. 申请一览页面

一览页组合：

```text
ApplicationListPage
├─ ApplicationSearch
├─ LoadingIndicator / AppMessage
└─ ApplicationTable
   └─ ApplicationStatusBadge
```

基础查询条件为状态和关键字：

```js
const query = {
  status: searchParams.get('status') ?? '',
  keyword: searchParams.get('keyword') ?? '',
};
```

条件提交后写入 Search Params，再由 Effect/Hook 请求 API。必须区分：

- 当前用户从未申请过：显示“暂无申请”；
- 存在申请但筛选无结果：显示“没有符合条件的申请”；
- API 读取失败：显示错误和重试；
- 读取成功：显示结果件数和表格。

状态显示统一使用映射，不直接把 `pending` 等代码值显示给用户。

## 6. 取消申请

只有 `pending` 行显示取消按钮。点击后确认，再调用：

```text
PATCH /api/leave-applications/:id/cancel
```

```jsx
async function handleCancel(id) {
  if (cancelingId !== null) return;
  if (!window.confirm('この申請を取り消しますか？')) return;

  setCancelingId(id);
  setErrorMessage('');

  try {
    await cancelApplication(id);
    await loadApplications(currentQuery);
  } catch (error) {
    setErrorMessage('取消失败，请重新读取后再试');
  } finally {
    setCancelingId(null);
  }
}
```

正式页面可以换成可访问的 Dialog。前端隐藏按钮只是改善体验，后台必须再次确认当前用户和状态。`approved`、`returned`、`cancelled` 均不能取消。

## 7. 日期时间显示

API 返回 ISO UTC，例如：

```text
2026-09-18T03:15:20.123Z
```

数据处理时保留原始值，渲染时使用第 25 章的 `formatJapanDateTime()`。不要通过截取字符串把 UTC 冒充日本时间。

## 8. 组件测试与联调

| 对象 | 验证内容 |
| --- | --- |
| `LeaveForm` | 输入收集、即时错误、提交中禁用 |
| `ApplicationSummary` | 草稿字段和显示文字 |
| `ApplicationTable` | Props 渲染、稳定 key、取消回调 |
| Application Store | 草稿、提交成功/失败、重复提交保护 |
| Router | 无草稿确认页、完成页刷新、未登录重定向 |
| 一览页 | 无申请、无筛选结果、读取失败、重试 |

单元/组件测试使用 API Mock，不连接真实 MySQL。最终联调启动课程后台，验证 Cookie、数据库和随机申请状态。

## 9. 业务验收

- [ ] 输入页即时显示字段错误，全表单正确后才能进入确认页。
- [ ] 确认页不允许修改字段，返回后输入仍保留。
- [ ] 连续点击提交只发送一次请求。
- [ ] 后台错误不会清除草稿或进入成功页。
- [ ] 提交成功显示受付番号，完成页刷新不重复新增。
- [ ] 后台随机生成的 `pending`、`approved`、`returned` 均能正确显示。
- [ ] 状态和关键字能够组合检索。
- [ ] 只有当前用户的 `pending` 申请能够取消。
- [ ] 退出再登录后，MySQL 中的申请仍然存在。

## 本章检查点

- [ ] 四个申请页面形成输入、确认、提交、完成和查询闭环。
- [ ] 局部表单 State 与跨页面 Zustand 草稿职责清楚。
- [ ] 前端校验和后台校验都存在，且后台结果优先。
- [ ] loading、empty、error、submitting 和 canceling 状态可区分。
