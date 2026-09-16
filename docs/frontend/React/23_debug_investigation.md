# 第 23 章 调试与项目调查

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

## 3. 高频原因

- `/employees/new` 被动态 `:id` 错误处理。
- `useParams` 的字符串未校验，得到 `NaN`。
- Effect 漏了 `employeeId` 依赖，路由切换仍用旧数据。
- 旧请求晚到覆盖新员工。
- 表单初始 State 只在第一次渲染读取 Props，详情到达后没有按“草稿”规则初始化。
- 接口字段为 `joined_date`，前端却读取 `joinedDate`。

## 4. 企业改修记录

一份可 Review 的记录至少包含：现象与重现条件、期待结果、调查路径、根因证据、影响范围、修改文件、测试项目、结果证据和未验证风险。日本项目中常见的“横展開/影響調査”应落实为同类页面、共通组件、接口调用方和回归项，而不是只写一句“无影响”。

## 5. 练习

1. 人为删除 Effect 依赖，用上述路径调查并修复。
2. 制造字段名不一致，分别保存 Network 与 React DevTools 证据。
3. 给“编辑页空白”编写不具合票：再现步骤、原因、修正、横向影响和测试结果。
4. 从 `package.json` 开始，画出当前员工详情页的完整依赖链。

## 本章检查点

- [ ] 能沿 Route → Param → Hook/Effect → Service → Network → State → Props → JSX 调查。
- [ ] 能用事实证据区分请求前、请求中和渲染层问题。
- [ ] 能提交根因、影响范围、横展開、测试证据和未验证风险。
