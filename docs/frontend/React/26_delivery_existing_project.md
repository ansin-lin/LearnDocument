# 第 26 章 React SES 改修、回归测试与交付

真实项目经常是在既有代码和规格约束下实施小范围改修。本章继续使用第 23～25 章完成的 React 版“有給休暇申請システム”，实施与 Vue 练习相同的改修：为申请一览增加申请期间筛选。

## 1. 改修任务

> 申請一覧画面に「開始日」と「終了日」の検索条件を追加してください。両方が入力された場合は、指定期間と重なる申請を表示してください。既存の状態検索とキーワード検索はそのまま利用できること。

业务规则：

- 开始日和结束日都可以为空；
- 两者都有值时，开始日不能晚于结束日；
- 日期格式使用 `YYYY-MM-DD`；
- 新条件与状态、关键字同时生效；
- 清除按钮恢复全部条件；
- 条件保存在 URL Search Params；
- 后台负责最终日期校验和数据筛选；
- 不更换状态管理方式、不重写一览页、不升级无关依赖。

## 2. 修改前影响范围调查

先沿调用链确认修改点：

```text
ApplicationSearch
  ↓ onSearch(query)
ApplicationListPage
  ↓ Search Params
Application Store
  ↓ loadApplications(query)
applicationService
  ↓ GET /api/leave-applications
Backend → MySQL
```

调查范围至少包含：

| 位置 | 确认内容 |
| --- | --- |
| 检索组件 | 当前字段、默认值、提交和清除行为 |
| URL | 已有 `status`、`keyword` 是否保留 |
| Store | 查询对象是否原样传给 Service |
| Service | Axios `params` 是否过滤空值 |
| API 规格 | 参数名、日期格式、400 响应 |
| 测试 | 既有筛选、空数据、取消是否受影响 |

不确定参数包含边界还是重叠区间时，应先提出确认事项，不自行决定业务含义。

## 3. React 实现要求

在 `ApplicationSearch` 增加两个受控日期输入：

```jsx
<label htmlFor="from-date">開始日</label>
<input
  id="from-date"
  type="date"
  value={startDate}
  onChange={(event) => setStartDate(event.target.value)}
/>

<label htmlFor="to-date">終了日</label>
<input
  id="to-date"
  type="date"
  value={endDate}
  onChange={(event) => setEndDate(event.target.value)}
/>
```

提交时验证范围：

```js
if (startDate && endDate && startDate > endDate) {
  setDateError('開始日は終了日以前の日付を入力してください');
  return;
}

onSearch({ status, keyword, startDate, endDate });
```

`YYYY-MM-DD` 可以按字符串比较先后，因为排列顺序与日期顺序一致。后台仍需校验真实日期和业务范围。

更新 URL 时不能覆盖原条件，空条件不写入 Query String：

```js
const nextParams = {};

if (status) nextParams.status = status;
if (keyword.trim()) nextParams.keyword = keyword.trim();
if (startDate) nextParams.startDate = startDate;
if (endDate) nextParams.endDate = endDate;

setSearchParams(nextParams);
```

页面刷新后，表单初始值必须从 Search Params 恢复。清除时同时清空四个条件并重新查询。

## 4. API 与状态处理

Service 继续使用同一个一览接口：

```js
export async function getApplications(query, signal) {
  const response = await httpClient.get('/leave-applications', {
    params: query,
    signal,
  });
  return response.data.data;
}
```

不要另建一个只处理日期的重复 API 方法。查询切换时取消旧请求，避免旧结果覆盖新条件。

后端返回日期范围错误时，转换为字段或页面错误；网络失败时保留当前条件，并允许重试。查询失败不能把旧数据伪装成本次查询结果。

## 5. 测试观点

| Case | 条件 | 期待结果 |
| --- | --- | --- |
| DATE-01 | 两个日期都为空 | 与改修前结果一致 |
| DATE-02 | 只有开始日 | 返回开始日以后的符合记录 |
| DATE-03 | 只有结束日 | 返回结束日以前的符合记录 |
| DATE-04 | 开始日等于结束日 | 正常查询当天范围 |
| DATE-05 | 开始日晚于结束日 | 前端显示错误，不请求 API |
| DATE-06 | 日期 + 状态 | 两个条件同时生效 |
| DATE-07 | 日期 + 关键字 | 两个条件同时生效 |
| DATE-08 | 四个条件组合 | 返回全部条件共同匹配的数据 |
| DATE-09 | 刷新带条件 URL | 表单和结果恢复 |
| DATE-10 | 清除条件 | 恢复完整一览 |
| DATE-11 | 快速连续查询 | 旧响应不覆盖新结果 |
| DATE-12 | API 失败 | 保留条件，显示错误和重试 |

Regression Test 还要覆盖登录、注册、首页汇总、申请输入、确认提交、完成页刷新、原状态筛选、关键字检索和取消申请。

## 6. Review 与不具合记录

提交 Review 前记录：

- 改修规格和确认事项；
- 修改文件与修改理由；
- 影响到的组件、Store、Service、URL 和测试；
- 正常系、異常系、境界値结果；
- Network 请求参数和页面结果证据；
- 未验证内容与已知限制。

发现 Bug 时写明再现步骤、期待结果、实际结果、根因、修正和横向影响。不要只写“日期搜索有问题”。

## 7. 最终质量确认

```bash
npm run lint
npm run test
npm run build
npm run preview
git status
git diff
```

项目命令以实际 `package.json` 为准。预览环境还要确认：

- 七个业务 URL 可直接打开和刷新；
- 浏览器前进、后退与 Search Params 一致；
- 360px 宽度没有横向溢出；
- 键盘可以完成导航、输入、确认和取消；
- Console 没有未处理错误；
- Network 没有重复提交和无限请求；
- 不提交 `.env` 中的本地配置、密码、Cookie 或测试 Evidence 中的敏感信息。

## 8. 最终提交物

- React 项目源码与锁文件；
- README：安装、启动、API、测试、构建、目录和已知限制；
- 后台启动与 MySQL 联调记录；
- 七个页面的业务验收结果；
- 日期范围改修的影响调查和 DATE-01～DATE-12 结果；
- React 组件、Store、Router 的测试结果；
- HTML/CSS/JavaScript 旧实现与 React 新实现的对应表；
- 至少一条真实排错记录和交接事项。

## 最终检查点

- [ ] 七个页面已经使用 React 组件化重构，而不是复制七套旧 DOM 脚本。
- [ ] 用户、Session 和申请记录全部通过后台与 MySQL 管理。
- [ ] 日期筛选改修没有破坏状态、关键字、取消和其他既有功能。
- [ ] 代码、测试、构建、预览和联调均有可确认结果。
- [ ] 交接资料能够让下一名开发者重建环境并继续维护。
