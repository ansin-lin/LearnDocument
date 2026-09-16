# 第 27 章 验收、既有项目改修与不具合调查

## 本章目标

- 交付可重建、可验证的 React 项目。
- 有顺序地阅读陌生项目并完成局部改修。
- 提供影响调查、自测与交接证据。

## 1. 最终验收

```text
环境重建 → npm ci（按 package-lock.json 精确安装）
质量检查 → lint / test / build
功能验收 → login / list / search / page / detail / create / edit / delete
异常验收 → validation / 401 / 403 / 404 / 409 / 500 / timeout
安全复核 → secrets / auth / authorization / logs
交付记录 → README / known issues / test evidence
```

安装依赖时优先使用团队规定的锁文件流程。README 应说明 Node 版本、环境变量名、后端前提、命令、测试数据准备与清理、限制和故障入口。

## 2. 阅读既有 React 项目

```text
package.json
 ↓ 版本、scripts、依赖
main.tsx
 ↓ Provider、Router、全局样式
App / Router
 ↓ URL 对应页面
Page
 ↓ State、Hook、Component
Hook / Store
 ↓ 数据所有权与副作用
Service
 ↓ 方法、URL、契约
API / Network
```

先沿一条真实功能链走通，再扩展全局地图。查看 `git status` 与 `git diff`，区分自己的修改和已有变更；遵守项目锁定版本，不因为教程写法不同就大规模重构。

### 2.1 先识别技术年代，不急着改写

| 观察点 | 可能看到的形态 | 调查重点 |
| --- | --- | --- |
| React | 17/18/19，类组件或函数组件 | 入口 API、StrictMode、状态与副作用写法 |
| Router | v5 的 `Switch`/`Redirect`、v6/v7 的 `Routes`/嵌套路由 | 以锁文件对应文档解释，不混用 API |
| Redux | 手写 action/reducer、Redux Toolkit slice | Store 创建、Provider、typed hooks 与异步流程 |
| 构建 | CRA、旧 Vite、现代 Vite 或框架 | scripts、环境变量规则、Node 版本与部署入口 |

识别这些差异是为了正确阅读和局部修改，不是收到一个缺陷就顺手升级 React、Router 或构建工具。升级属于独立任务，需要迁移说明、影响调查和完整回归。

## 3. 改修流程

示例需求：“员工列表追加部门筛选，并在刷新后保留条件。”

1. 确认规格：可选值、默认值、URL 参数、API 参数、权限和空数据。
2. 找到 Route/Page/SearchForm/Service/测试。
3. 调查相同筛选的其他页面与共通类型。
4. 写失败测试或明确再现步骤。
5. 局部修改类型、URL 解析、表单和 Service。
6. 验证旧关键字/分页/排序没有回归。
7. 记录影响范围、测试结果和未验证项。

## 4. 不具合调查模板

```text
现象：编辑页没有显示员工数据
再现条件：用户、URL、浏览器、数据 ID、发生频率
期待结果：规格中的正确画面
调查证据：Route → Param → Hook → Service → Network → State → Props → JSX
根因：在哪一层、为什么
修正：文件和行为变化
影响调查：同 Hook/Service/组件/接口的调用方
测试：正常、边界、异常、回归
未验证风险：环境或权限限制
```

不要只提交“已修复”。Review 人员应能从证据判断修改是否覆盖根因与横向影响。

## 5. 最终任务

1. 从全新目录按 README 重建并完成 lint/test/build。
2. 用 ADMIN 完成全部 CRUD，用 USER 验证只读边界。
3. 完成一次部门筛选改修并提交影响调查表。
4. 制造一个 Effect 竞态缺陷，按模板调查、修正和回归。
5. 演示并解释：数据从哪里来、保存在哪里、如何传递、何时修改、为何重渲染、何时请求、如何导航、为何使用全局状态。

全部检查通过后，学员才完成主线；只展示页面截图不算交付。

## 本章检查点

- [ ] 能从全新目录按文档重建、测试和构建项目。
- [ ] 能沿入口、Provider、Router、Page、Hook/Store、Service 到 API 阅读既有项目。
- [ ] 改修交付包含影响调查、横展開、回归证据和未验证风险。
