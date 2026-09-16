# 第 22 章 React 测试

## 本章目标

- 区分单元、组件、集成和 E2E 测试。
- 使用 Vitest 与 React Testing Library 测试用户行为。
- 覆盖加载、空数据、成功、失败、校验与权限状态。

## 1. 测试层次

| 层次 | 适合验证 | 员工系统示例 |
| --- | --- | --- |
| 单元 | 纯函数 | 表单校验、错误映射 |
| 组件 | 单个 UI 行为 | 输入关键字、显示错误 |
| 集成 | 多组件与请求协作 | 搜索后请求并显示列表 |
| E2E | 真实浏览器关键流程 | 登录到员工编辑成功 |

不要在每层重复所有断言。按风险选择最低但足够可信的层次。

## 2. 安装和配置

新建课程项目按封版基线安装并提交锁文件；既有项目不得照抄命令覆盖自己的版本，应先核对 [Vitest](https://vitest.dev/guide/) 与 [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/) 对应版本文档：

```bash
npm install -D --save-exact vitest@5.0.1 jsdom@30.0.1 @testing-library/react@16.3.3 @testing-library/dom@10.4.2 @testing-library/user-event@14.6.7 @testing-library/jest-dom@7.0.1
```

```ts
// vite.config.ts（片段）
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
});
```

```ts
// src/test/setup.ts
import '@testing-library/jest-dom/vitest';
```

在 `package.json` 增加 `"test": "vitest"`。若 TypeScript 不认识 `test` 配置，按 Vitest 当前 Vite 配置集成方式导入/补充类型，不复制与项目版本不兼容的旧配置。

## 3. 测试用户行为

```tsx
it('输入姓名并提交', async () => {
  const user = userEvent.setup();
  const onSubmit = vi.fn();
  render(<EmployeeForm onSubmit={onSubmit} />);

  await user.type(screen.getByRole('textbox', { name: '姓名' }), '田中');
  await user.click(screen.getByRole('button', { name: '保存' }));

  expect(onSubmit).toHaveBeenCalledWith(
    expect.objectContaining({ name: '田中' }),
  );
});
```

优先按 role、label、可见文字查询；`data-testid` 是缺少合理语义时的退路。不要断言内部 State 或私有函数。

## 4. 异步 UI

```tsx
render(<EmployeeListPage />);
expect(screen.getByRole('status')).toHaveTextContent('读取中');
expect(await screen.findByText('田中')).toBeInTheDocument();
```

使用 `findBy...` 或 `waitFor` 等待明确结果，不用固定 sleep。请求 Mock 应与真实契约一致，并在每个测试后重置。测试成功、空数组、500 和重试，不只测 happy path。

## 5. 练习

1. 单测邮箱校验。
2. 组件测试必填错误与成功提交。
3. 集成测试 loading → success 和 loading → error → retry。
4. 权限测试：无删除权限时按钮不可见。
5. 路由权限测试：USER 直接访问新增/编辑路径进入 403 页面，ADMIN 可进入。
6. 设计一条 E2E 登录→检索→编辑流程及测试数据清理方式。

## 本章检查点

- [ ] 能按风险选择单元、组件、集成或 E2E 测试。
- [ ] 优先用角色、标签和可见文字验证用户行为。
- [ ] 异步测试等待明确结果，并在测试间重置 Mock。
