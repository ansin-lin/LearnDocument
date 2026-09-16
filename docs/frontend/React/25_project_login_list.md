# 第 25 章 实战：登录与员工列表

本章在第 24 章起始项目上新增认证、列表查询、搜索、分页与排序。完成后保留可运行状态，再进入 CRUD。

## 本章目标

- 复用第 14～15 章统一的 Auth Context 完成登录与安全返回。
- 用稳定查询字段构建可通过 Hook Linter 的列表请求 Hook。
- 验证登录、加载、空数据、成功、失败、取消、搜索、排序与分页。

## 1. 登录流程

```text
Login Form → validation → POST /login
        → Auth State → target route
        ↘ error → form remains usable
```

```tsx
// features/auth/LoginPage.tsx（LoginPage 组件内部片段）
const { setUser } = useAuth();

function safeReturnPath(value: unknown): string | null {
  if (typeof value !== 'object' || value === null) return null;
  const { pathname, search } = value as {
    pathname?: unknown;
    search?: unknown;
  };
  if (
    typeof pathname !== 'string' ||
    !pathname.startsWith('/') ||
    pathname.startsWith('//')
  ) {
    return null;
  }
  return pathname + (typeof search === 'string' ? search : '');
}

async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
  event.preventDefault();
  const nextErrors = validateLogin(form);
  setErrors(nextErrors);
  if (Object.keys(nextErrors).length > 0 || saving) return;

  setSaving(true);
  setServerError(null);
  try {
    const user = await login(form);
    setUser(user);
    navigate(safeReturnPath(location.state?.from) ?? '/employees', {
      replace: true,
    });
  } catch (cause) {
    setServerError(loginMessage(cause));
  } finally {
    setSaving(false);
  }
}
```

密码输入不回显、不写日志。失败消息避免泄漏“该邮箱是否存在”。这里的 `setUser` 来自第 14 章统一 `AuthContextValue`：传入用户后同时得到 `authenticated + AuthUser`，不会另外维护第二份登录状态。Protected Route 等待同一 Auth State 的会话检查完成。

## 2. 列表查询 Hook

```tsx
import type { AppError } from '../../types/appError';
import type { AsyncState } from '../../types/asyncState';
import type { Employee, EmployeeQuery } from '../../types/employee';
import type { PageResponse } from '../../types/pagination';

function useEmployeePage(query: EmployeeQuery) {
  const [state, setState] = useState<
    AsyncState<PageResponse<Employee>, AppError>
  >({ status: 'loading' });

  const { keyword, department, page, size, sort } = query;

  const reload = useCallback(async (signal?: AbortSignal) => {
    setState({ status: 'loading' });
    try {
      const nextQuery: EmployeeQuery = {
        keyword,
        department,
        page,
        size,
        sort,
      };
      const data = await searchEmployees(nextQuery, signal);
      if (!signal?.aborted) {
        setState({ status: 'success', data });
      }
    } catch (cause) {
      if (!signal?.aborted && !isCanceled(cause)) {
        setState({ status: 'error', error: toAppError(cause) });
      }
    }
  }, [keyword, department, page, size, sort]);

  useEffect(() => {
    const controller = new AbortController();
    void reload(controller.signal);
    return () => controller.abort();
  }, [reload]);

  return { state, reload };
}
```

项目沿用第 17 章的泛型 `AsyncState<T, E>` 判别联合，并明确把错误参数指定为第 18 章的 `AppError`。查询值从 Search Params 解析并规范化；表单草稿在提交时写回 URL。callback 只读取解构后的五个字段，依赖数组与实际读取值一一对应，可以通过 Hooks Linter；不要读取整个 `query` 却只列出它的属性依赖。

## 3. 页面渲染

```tsx
return (
  <>
    <EmployeeSearchForm initialQuery={query} onSearch={updateUrl} />
    <section aria-label="员工列表结果">
      {state.status === 'loading' && <Loading label="员工列表" />}
      {state.status === 'error' && (
        <ErrorPanel error={state.error} onRetry={() => void reload()} />
      )}
      {state.status === 'success' && state.data.items.length === 0 && (
        <EmptyEmployees />
      )}
      {state.status === 'success' && state.data.items.length > 0 && (
        <>
          <EmployeeTable employees={state.data.items} sort={query.sort} />
          <Pagination
            page={state.data.page}
            size={state.data.size}
            total={state.data.total}
          />
        </>
      )}
    </section>
  </>
);
```

搜索区位于共同返回结构中，因此列表 loading/error 时仍可修改条件。`onRetry` 用无参数函数包装异步 `reload`，避免把组件事件对象误传成 `AbortSignal`，也明确忽略 Promise 返回值。

## 4. 练习与测试

- 校验失败不请求登录 API。
- 登录成功进入安全目标 URL；失败恢复按钮。
- 列表覆盖 loading、empty、success、error/retry。
- 修改关键字重置 page=1；刷新保留查询。
- 快速改条件时旧请求被取消。
- USER 看不到写操作，但后端仍返回授权结果。
- USER 直接访问新增/编辑 URL 进入 403 页面，ADMIN 可以进入。

运行单测和构建，并在 Network 保存登录（隐藏敏感内容）与列表查询证据。

## 本章检查点

- [ ] Login、RequireAuth、Header 使用同一 AuthState 与 setUser。
- [ ] useCallback 和 useEffect 的依赖与其读取的响应式值一致。
- [ ] 列表覆盖四态、取消、URL 查询恢复和权限显示。
