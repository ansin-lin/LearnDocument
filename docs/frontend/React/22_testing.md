# 第 22 章 React 测试

## 本章目标

- 区分单元、组件、集成、E2E 和手动打鍵测试。
- 使用 Vitest 与 React Testing Library 验证用户可观察行为。
- 测试校验、事件、路由、异步请求、权限和错误恢复。
- 保持测试数据与 Mock 相互隔离，并能调查失败 Case。

## 1. 为什么需要测试

测试不是为了证明程序永远没有 Bug，而是用相同条件反复确认重要行为，尽早发现 Regression（回归缺陷）。前端测试关注用户输入、点击、显示、页面跳转和 API 联动结果，不应只检查内部函数名。

## 2. 测试层次与执行方式

| 层次 | 适合验证 | 员工系统示例 |
| --- | --- | --- |
| 单元 | 纯函数 | 表单校验、错误转换 |
| 组件 | 单个 UI 的公开行为 | 输入姓名、显示必填错误 |
| 集成 | 组件、路由、Store、Mock API 协作 | 查询后显示员工列表 |
| E2E | 真实浏览器和后台的关键流程 | 登录、检索、编辑成功 |

测试阶段与执行方式不是同一维度：

```text
执行方式
├─ 自动测试：Vitest / React Testing Library / Browser E2E
└─ 手动测试：浏览器打鍵、Network、视觉和兼容性确认
```

日本项目中的“画面単体テスト”有时包含较宽的页面和 API 联动范围，应以项目测试计划为准。正常系/異常系表示业务结果类别；境界値是设计观点，同一个 Case 可以同时属于“正常系 + 境界値”。

## 3. 安装与配置

课程封版项目使用：

```bash
npm install -D --save-exact vitest@5.0.1 jsdom@30.0.1 @testing-library/react@16.3.3 @testing-library/dom@10.4.2 @testing-library/user-event@14.6.7 @testing-library/jest-dom@7.0.1
```

`vite.config.js`：

```js
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    clearMocks: true,
  },
});
```

`src/test/setup.js`：

```js
import '@testing-library/jest-dom/vitest';
```

`package.json` scripts：

```json
{
  "scripts": {
    "test": "vitest",
    "test:run": "vitest run"
  }
}
```

`jsdom` 模拟浏览器 DOM，但不等于真实 Chrome。布局、下载、焦点细节和浏览器兼容仍需 E2E 或手动确认。

## 4. Vitest 的基本结构

```js
import { describe, expect, it } from 'vitest';
import { validateEmployee } from './validateEmployee';

describe('validateEmployee', () => {
  it('姓名为空时返回必填错误', () => {
    const errors = validateEmployee({ name: '', email: 'a@example.com' });
    expect(errors.name).toBe('姓名为必填项');
  });
});
```

- `describe()` 把相关 Case 分组；
- `it()` 定义一个可观察行为；
- `expect()` 建立期待结果；
- Matcher 表示比较方式。

| Matcher | 用途 |
| --- | --- |
| `toBe()` | 严格比较原始值 |
| `toEqual()` | 比较对象或数组内容 |
| `toContain()` | 确认包含文字或元素 |
| `toHaveBeenCalled()` | 确认 Mock 函数被调用 |
| `toHaveBeenCalledWith()` | 确认调用参数 |
| `toBeInTheDocument()` | 确认元素存在于 DOM |
| `toBeDisabled()` | 确认控件被禁用 |

每个 Case 应尽量只验证一个主要行为，名称写明条件和结果。

## 5. React Testing Library 的思路

```jsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, it, vi } from 'vitest';
import { EmployeeForm } from './EmployeeForm';

it('输入姓名并提交', async () => {
  const user = userEvent.setup();
  const onSubmit = vi.fn();

  render(<EmployeeForm onSubmit={onSubmit} />);

  await user.type(
    screen.getByRole('textbox', { name: '姓名' }),
    '田中太郎',
  );
  await user.click(screen.getByRole('button', { name: '保存' }));

  expect(onSubmit).toHaveBeenCalledWith(
    expect.objectContaining({ name: '田中太郎' }),
  );
});
```

- `render()` 把组件放入测试 DOM；
- `screen` 从当前页面查询元素；
- `userEvent.setup()` 建立更接近用户操作的输入工具；
- `vi.fn()` 建立可检查调用次数和参数的 Mock 函数。

优先按 role、label、可见文字查询。`data-testid` 只在没有合理语义查询方式时使用。不要直接断言内部 State 或私有函数。

## 6. 表单校验：正常、异常和边界值

假设姓名最大 50 字符：

| Case | 输入 | 分类 | 期待结果 |
| --- | --- | --- | --- |
| FORM-01 | 普通姓名 | 正常系 | 可以提交 |
| FORM-02 | 空字符串 | 異常系 | 显示必填错误，不提交 |
| FORM-03 | 49 字符 | 正常系 + 境界値 | 可以提交 |
| FORM-04 | 50 字符 | 正常系 + 境界値 | 可以提交 |
| FORM-05 | 51 字符 | 異常系 + 境界値 | 显示错误，不提交 |

```jsx
it('姓名为空时显示错误且不提交', async () => {
  const user = userEvent.setup();
  const onSubmit = vi.fn();
  render(<EmployeeForm onSubmit={onSubmit} />);

  await user.click(screen.getByRole('button', { name: '保存' }));

  expect(screen.getByRole('alert')).toHaveTextContent('姓名为必填项');
  expect(onSubmit).not.toHaveBeenCalled();
});
```

测试当前组件的职责边界。如果表单通过 `onSubmit` 通知 Page，那么“不发送 API”在组件测试中对应“不调用 `onSubmit`”；真正的 API 调用在 Page/Store 集成测试中确认。

## 7. 异步 UI 与 Service Mock

```jsx
import { render, screen } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';
import { EmployeeListPage } from './EmployeeListPage';
import { searchEmployees } from '../services/employeeService';

vi.mock('../services/employeeService', () => ({
  searchEmployees: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

it('读取中之后显示员工', async () => {
  searchEmployees.mockResolvedValue({
    items: [{ id: 1, employeeCode: 'EMP0001', name: '田中太郎' }],
    page: 1,
    size: 10,
    total: 1,
  });

  render(<EmployeeListPage />);

  expect(screen.getByRole('status')).toHaveTextContent('读取中');
  expect(await screen.findByText('田中太郎')).toBeInTheDocument();
});
```

`vi.mock()` 把真实 Service 替换为测试控制的函数。Mock 返回值必须符合第 11、17 章的真实契约，不能为了测试方便返回另一种结构。

异步查询：

- `getBy...`：元素应立即存在，不存在就失败；
- `queryBy...`：用于确认不存在；
- `findBy...`：等待元素出现；
- `waitFor()`：等待某个断言成立。

不要使用固定 `setTimeout` 等待。测试应等待明确的 UI 结果。

## 8. Empty、Error 与 Retry

至少覆盖：

```text
loading → success
loading → empty
loading → error → retry → success
```

```jsx
it('失败后点击重试可以显示列表', async () => {
  const user = userEvent.setup();
  searchEmployees
    .mockRejectedValueOnce(new Error('server error'))
    .mockResolvedValueOnce({
      items: [{ id: 1, employeeCode: 'EMP0001', name: '田中太郎' }],
      page: 1,
      size: 10,
      total: 1,
    });

  render(<EmployeeListPage />);

  expect(await screen.findByRole('alert')).toHaveTextContent('读取失败');
  await user.click(screen.getByRole('button', { name: '重试' }));
  expect(await screen.findByText('田中太郎')).toBeInTheDocument();
  expect(searchEmployees).toHaveBeenCalledTimes(2);
});
```

## 9. Router 与 Provider 测试环境

使用 Router Hook 的组件需要路由环境：

```jsx
import { MemoryRouter } from 'react-router-dom';

render(
  <MemoryRouter initialEntries={['/employees?keyword=田中']}>
    <EmployeeListPage />
  </MemoryRouter>,
);
```

Redux 组件要包 `Provider`；Context 组件要包对应 Provider；Zustand Store 应在每个 Case 前恢复初始状态。可以建立项目共通的 `renderWithProviders()`，但应让参数清楚表达当前 Router 和 Store 初始状态。

## 10. 权限测试

至少验证三层中属于前端的两层：

- 无权限用户看不到删除按钮；
- USER 直接打开新增/编辑路由时进入 403；
- ADMIN 可以进入并执行操作。

后端是否真正返回 403 属于 API 联调或 E2E 范围，不能只凭按钮隐藏断言后端安全已经成立。

## 11. Test Isolation

Case 不应无意识依赖执行顺序：

- 每个 Case 自己准备需要的数据；
- 清除 Mock 调用和临时实现；
- 重置全局 Store、Storage 和 Fake Timer；
- 不让前一个 Case 创建的数据成为后一个 Case 的隐含前提；
- E2E 明确测试账号、数据库初始数据和后处理。

Case 可以重复、单独执行，才容易调查失败原因。

## 12. 自动测试、E2E 与手动打鍵

| 对象 | Vitest / RTL | Browser E2E | 手动打鍵 |
| --- | ---: | ---: | ---: |
| 纯函数和校验 | ◎ | △ | △ |
| Props、事件、条件显示 | ◎ | ○ | ○ |
| 路由与页面流程 | ○ | ◎ | ◎ |
| 真实 API 和 Cookie | △ | ◎ | ◎ |
| Layout、键盘、浏览器差异 | △ | ○ | ◎ |
| 重复回归 | ◎ | ◎ | △ |

不是每条手工 Case 都必须改成 Vitest。布局视觉、复杂焦点、真实文件下载和跨系统流程更适合 E2E 或手动确认。

## 13. 测试式样、结果和 Evidence

日本项目常见记录：

| No | 测试观点 | 前提条件 | 操作/输入 | 期待结果 | 结果 | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| UT001 | 必填 | 新增页 | 姓名为空并保存 | 显示必填错误，不请求 API | ○ | UT001.png |

Evidence 可以是截图、Network、Console、测试输出或数据库结果，具体要求以项目规则为准。记录时隐藏密码、Cookie、Token 和个人信息。

## 14. 运行与失败调查

```bash
npm run test
npm run test:run
```

失败时依次确认：

1. 失败 Case 名和第一条错误；
2. 实际 DOM，可使用 `screen.debug()` 临时观察；
3. 查询角色和标签是否与页面一致；
4. Mock 路径、响应结构和调用次数；
5. 是否遗漏 Router、Provider 或 Store 重置；
6. 异步结果是否使用 `findBy...` 等待。

不要看到异步失败就随意增加 sleep。

## 15. 练习

1. 测试邮箱校验的正常、异常和边界值。
2. 测试必填错误与成功提交参数。
3. 测试 loading、empty、success、error、retry。
4. 用 MemoryRouter 测试关键字 Search Params。
5. 测试 USER 与 ADMIN 的按钮和路由行为。
6. 为一条手动打鍵 Case 编写对应自动测试，并说明未自动化部分。

## 本章检查点

- [ ] 能按风险选择单元、组件、集成、E2E 或手动测试。
- [ ] 能使用 `render`、`screen`、`userEvent` 和常用 Matcher。
- [ ] 优先通过角色、标签和可见文字验证公开行为。
- [ ] 能 Mock Service 并覆盖 loading、empty、success、error 和 retry。
- [ ] 每条 Case 可以独立执行，Mock 和 Store 不互相污染。
- [ ] 能从规格提取正常系、異常系和境界値，并留下合适 Evidence。
