# 第 9 章 类的类型语法（会阅读）

现代 React 通常使用函数组件，现代 Vue 通常使用 Composition API，因此新人不需要先设计复杂的类体系。不过，旧代码、工具类和部分第三方库仍可能使用类。本章目标是能够阅读和简单修改类，不要求用类保存普通接口数据。

## 1. 什么时候会用到类

普通对象适合表示一组数据：

```ts
interface Product {
  id: number;
  name: string;
  price: number;
}

const product: Product = { id: 1, name: "キーボード", price: 5000 };
```

当程序需要反复按照同一规则创建对象，而且对象本身还要提供相关行为时，可以考虑类。

## 2. 类、构造函数与实例

```ts
class Product {
  id: number;
  name: string;
  price: number;

  constructor(id: number, name: string, price: number) {
    this.id = id;
    this.name = name;
    this.price = price;
  }

  getLabel(): string {
    return `${this.name}: ${this.price.toLocaleString()}円`;
  }
}

const product = new Product(1, "キーボード", 5000);
console.log(product.getLabel()); // キーボード: 5,000円
```

- `class Product`定义创建规则和方法。
- `constructor()`在执行`new Product(...)`时运行，用于初始化实例。
- `this`表示当前正在操作的实例。
- `new`创建出的具体对象叫作实例。

类名也能表示实例类型：

```ts
function printProduct(product: Product): void {
  console.log(product.name);
}

printProduct(new Product(2, "マウス", 3000)); // マウス
```

## 3. 属性必须完成初始化

在严格检查下，实例属性必须在声明处提供默认值，或者在构造函数中完成赋值。

```ts
class Counter {
  count = 0;

  increment(): void {
    this.count += 1;
  }
}

const counter = new Counter();
counter.increment();
console.log(counter.count); // 1
```

已有代码中可能看到`property!: Type`。这个`!`叫确定赋值断言，表示开发者承诺稍后一定赋值，但它不会自动创建数据。新人不应为了消除初始化错误而随意添加。

## 4. public、private 与 protected

访问修饰符限制成员可以从哪里使用：

| 修饰符 | 可以访问的位置 | 常见用途 |
| --- | --- | --- |
| `public` | 类内部、实例外部、子类 | 默认的公开成员 |
| `private` | 当前类内部 | 不希望外部直接修改的内部状态 |
| `protected` | 当前类内部和子类 | 为继承结构保留的成员 |

```ts
class Stock {
  private quantity: number;

  constructor(quantity: number) {
    this.quantity = quantity;
  }

  add(amount: number): void {
    if (amount <= 0) {
      throw new Error("增加数量必须大于0");
    }
    this.quantity += amount;
  }

  getQuantity(): number {
    return this.quantity;
  }
}

const stock = new Stock(10);
stock.add(2);
console.log(stock.getQuantity()); // 12
// console.log(stock.quantity);   // 错误：private成员不能从外部访问
```

`private`是 TypeScript 的访问检查，不应被当作密码等敏感信息的安全存储。浏览器端代码和数据仍可能被用户查看。

## 5. readonly 属性

```ts
class User {
  readonly id: number;
  name: string;

  constructor(id: number, name: string) {
    this.id = id;
    this.name = name;
  }
}

const user = new User(1, "田中");
user.name = "佐藤";
// user.id = 2; // 错误：只读属性不能重新赋值
```

`readonly`表示属性完成初始化后不能重新赋值。它是编译阶段限制，也不会自动让嵌套对象深层只读。

## 6. implements 检查类是否满足接口

```ts
interface Printable {
  print(): string;
}

class Report implements Printable {
  constructor(private title: string) {}

  print(): string {
    return `报告：${this.title}`;
  }
}

const report = new Report("月次集計");
console.log(report.print()); // 报告：月次集計
```

`implements Printable`要求`Report`提供接口规定的成员，但不会自动生成`print()`实现。示例中的构造函数参数属性`private title: string`同时声明并初始化私有属性；看到时要能认出，不要求优先采用这种简写。

## 7. 继承语法只需会读

旧代码可能用`extends`继承已有实现，用`super()`调用父类构造函数：

```ts
class Message {
  constructor(protected text: string) {}

  format(): string {
    return this.text;
  }
}

class ErrorMessage extends Message {
  format(): string {
    return `错误：${this.text}`;
  }
}

const message = new ErrorMessage("保存失败");
console.log(message.format()); // 错误：保存失败
```

新项目不要仅为复用少量字段建立多层继承。React/Vue 中的数据组合和组件组合通常比复杂类继承更常见。

## 8. 类不能自动校验外部数据

接口返回的普通对象不会因为形状相似就自动变成类实例：

```ts
class User {
  constructor(public name: string) {}

  greet(): string {
    return `你好，${this.name}`;
  }
}

const raw: unknown = { name: "田中" };
// raw.greet(); // 既没有完成类型收窄，也没有类实例的方法
```

外部数据仍应先验证。如果确实需要类的方法，应在验证后明确执行`new User(...)`；只用于显示和传输的数据通常保持普通对象即可。

## 9. 本章练习

1. 创建`Counter`类，在构造函数中接收初始值，并提供增加和读取方法。
2. 把内部计数改成`private`，确认外部不能直接修改。
3. 定义一个带`getLabel(): string`方法的接口，让一个简单类实现它。
4. 阅读第7节继承代码，说出`extends`、`protected`和方法覆盖分别起什么作用。
5. 说明为什么接口返回的普通对象不会自动拥有类的方法。

## 本章检查点

- 能区分类、实例、构造函数和普通对象。
- 能说明`new`、`this`以及实例方法的作用。
- 能看懂`public`、`private`、`protected`和`readonly`。
- 能说明`implements`只检查结构，不提供方法实现。
- 能阅读简单的`extends`继承代码，但不会为普通数据建立复杂类体系。
- 能说明类和接口都不会自动校验外部数据。
