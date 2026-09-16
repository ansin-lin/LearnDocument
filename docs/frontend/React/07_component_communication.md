# 第 7 章 组件通信与状态提升

## 本章目标

- 使用 Callback Props 把子组件事件交给父组件处理。
- 把共享状态放到最近共同父组件。
- 根据唯一事实来源判断 State 应放在哪里。

## 1. Child → Parent

子组件不能反向修改父组件数据，但可以调用父组件传入的回调：

```tsx
type EmployeeRowProps = {
  employee: Employee;
  onDelete: (id: number) => void;
};

function EmployeeRow({ employee, onDelete }: EmployeeRowProps) {
  return (
    <tr>
      <td>{employee.name}</td>
      <td>
        <button type="button" onClick={() => onDelete(employee.id)}>
          删除
        </button>
      </td>
    </tr>
  );
}
```

父组件决定真正如何更新：

```tsx
function handleDelete(id: number) {
  setEmployees((previous) => previous.filter((item) => item.id !== id));
}

<EmployeeRow employee={employee} onDelete={handleDelete} />
```

```text
Parent ──data props──▶ Child
Parent ◀─callback──── Child event
```

## 2. 状态提升

搜索框产生关键字，列表需要使用关键字。若各自保存一份，会失去同步。把状态放到最近共同父组件：

```tsx
function EmployeeListPage() {
  const [keyword, setKeyword] = useState('');
  const filtered = employees.filter((item) =>
    item.name.toLowerCase().includes(keyword.toLowerCase()),
  );

  return (
    <>
      <SearchArea keyword={keyword} onKeywordChange={setKeyword} />
      <EmployeeTable employees={filtered} />
    </>
  );
}
```

```text
          EmployeeListPage
          keyword / setKeyword
             /          \
   SearchArea          EmployeeTable
   修改 keyword        使用筛选结果
```

## 3. State 放在哪里

按顺序判断：

1. 只有一个组件使用：放在该组件附近。
2. 兄弟组件共享：提升到最近共同父组件。
3. 深层、多区域共享且更新频率合适：考虑 Context。
4. 复杂跨页面业务状态：再考虑 Store。
5. API 缓存、失效和重新获取：它是服务器状态，不要直接等同于普通全局状态。

状态尽量靠近使用位置，但必须有唯一所有者。不要复制 Props 到 State，除非明确要创建可独立编辑、可取消的草稿。

## 4. 常见错误

- 子组件直接修改父组件数据：应通过 Callback Props 报告事件。
- 兄弟组件各保存一份关键字：应提升到最近共同父组件。
- 为避免一层 Props 就使用全局 Store：先看共享范围和状态所有者。
- 把服务器返回列表复制到多个位置：会产生刷新与失效不一致。

## 5. 练习

1. 让 `EmployeeRow` 通过回调请求父组件删除。
2. 让搜索区与结果统计共享父级关键字。
3. 设计“编辑草稿”的状态位置，并说明取消编辑时如何恢复。
4. 排查两个搜索框显示不同关键字的问题，找出重复状态并改为唯一来源。

## 本章检查点

- [ ] 能实现 data 向下、callback 向上的组件通信。
- [ ] 能把共享 State 放到最近共同父组件。
- [ ] 能根据唯一事实来源说明 State 所有者。
