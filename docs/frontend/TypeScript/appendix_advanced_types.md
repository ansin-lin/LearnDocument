# 附录 高级类型与低频机制

完成第 14 章后按项目需要选读。本附录帮助阅读框架和库的类型，不是进入 React 或 Vue 前的必修内容。

## 1. 映射类型：批量修改属性规则

如果多个对象都需要“所有属性变为可选”，不必逐个重写结构。映射类型可以遍历属性名生成新类型：

```ts
type OptionalFields<T> = {
  [K in keyof T]?: T[K];
};
type Point = { x: number; y: number };
const change: OptionalFields<Point> = { x: 3 };
console.log(change);
```

`keyof T`取得属性名；`K in keyof T`逐个遍历；`T[K]`保留对应属性类型；`?`把生成的属性设为可选。这是类型运算，不是运行时循环。日常业务直接使用已学的`Partial<T>`即可。

## 2. 条件类型与 infer：根据类型选择结果

```ts
type UnwrapPromise<T> = T extends Promise<infer R> ? R : T;
const count: UnwrapPromise<Promise<number>> = 3;
const title: UnwrapPromise<string> = "测试";
console.log(count, title);
```

这里的`extends`用作匹配条件，不是类继承。若T匹配Promise，则`infer R`提取里面的结果类型；否则保留T。它只提取一层，不能代替会继续展开嵌套异步结果的`Awaited<T>`。一般业务优先使用内置工具。

## 3. 模板字面量类型：限制字符串格式

```ts
type ScreenId = "HOME" | "LEAVE";
type ScreenEvent = `${ScreenId}:open`;
const eventName: ScreenEvent = "LEAVE:open";
console.log(eventName);
// const wrong: ScreenEvent = "OTHER:open"; // 不符合允许的组合
```

外观像JavaScript模板字符串，但这里出现在类型位置，生成允许的字符串类型集合。适合事件名、键名约定；不会自动创建事件监听器。

## 4. 递归类型：描述树形数据

```ts
type MenuItem = {
  label: string;
  children?: MenuItem[];
};
const menu: MenuItem = {
  label: "管理",
  children: [{ label: "申请列表" }]
};
console.log(menu);
```

子节点仍是MenuItem，所以类型可以引用自身。它描述菜单树，不负责遍历菜单。复杂递归条件类型不属于基础项目要求。

## 5. 旧项目和库代码中的机制

| 名称 | 作用与阅读重点 |
| --- | --- |
| 声明合并 | 同名interface等声明可合并；阅读时需查清所有来源，业务模型避免分散定义 |
| namespace | 对声明进行命名分组，常见于旧式全局代码和声明文件；新前端项目主线使用ES模块 |
| Mixin | 组合类的能力；一般业务先考虑函数或对象组合 |
| 装饰器 | 在类或成员处加入声明式扩展；使用前核对框架、编译配置和装饰器模式，不混用不同模式的示例 |

结构兼容已经在第 8 章介绍：判断对象能否赋值时，主要检查需要的成员，不要求类型名字相同。

## 6. 阅读练习

把第 1 节Point增加一个必填的label属性，检查OptionalFields是否同步生成可选属性；把第 3 节ScreenId增加一个值，验证对应事件名是否可赋值。然后用内置Partial替换自定义映射类型，比较结果。

不要求为没有实际需求的场景设计多层嵌套类型。
