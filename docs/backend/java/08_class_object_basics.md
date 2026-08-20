# 第8章 Java 类与对象基础

> 本章目标：理解类和对象的关系，能够定义一个简单类，创建对象，并给对象属性赋值。

## 一、为什么需要类

前面学习的变量、数组和字符串只能保存零散数据。实际写程序时，经常需要把一组相关数据放在一起。

例如员工信息包含：

- 员工编号
- 员工姓名
- 部门
- 邮箱

如果只用零散变量：

```java
Long employeeId = 1001L;
String employeeName = "Tanaka";
String department = "Sales";
String email = "tanaka@example.com";
```

变量多了以后，数据之间的关系不明显。类可以把这些数据组织成一个整体。

## 二、类是什么

类是对象的设计图。它描述一类事物有哪些属性和行为。

```java
public class Employee {
    Long id;
    String name;
    String department;
    String email;
}
```

这段代码定义了一个 `Employee` 类。

| 代码 | 说明 |
| --- | --- |
| `public class Employee` | 定义一个公开类，类名是 `Employee` |
| `Long id` | 员工编号属性 |
| `String name` | 员工姓名属性 |
| `String department` | 部门属性 |
| `String email` | 邮箱属性 |

类名使用大驼峰命名，例如 `Employee`、`OrderItem`、`UserAccount`。

### 2.1 类的定义格式

Java 中类的完整定义格式如下：

```java
[访问限定符] [修饰符] class 类名 [extends 父类名] [implements 接口名列表] {
    [类的成员变量（属性）说明]
    [类的构造方法定义]
    [类的成员方法定义]
}
```

格式说明：

1. 方括号 `[]` 中的内容是可选项，不是每个类都必须写。
2. 类的访问限定符用于决定这个类可以被哪些地方使用。
3. 类的修饰符用于决定这个类的特殊使用方式。
4. `class` 是定义类的关键字，必须小写。
5. 类名是 Java 标识符，应该表达清楚含义，一般使用大驼峰命名。
6. `extends` 表示继承父类，后续继承章节会详细讲。
7. `implements` 表示实现接口，后续接口章节会详细讲。

新人阶段先重点掌握下面这种最常见写法：

```java
public class Employee {
    Long id;
    String name;
}
```

### 2.2 类的访问限定符

普通顶级类可以使用的访问限定符只有两种：

| 类的访问限定符 | 简单含义 | 当前阶段理解 |
| --- | --- | --- |
| `public` | 公有的，其他地方可以访问 | 类通常先写成 `public` |
| 默认不写 | 同一个包中可以访问 | 先知道即可 |

`private` 和 `protected` 不能修饰普通顶级类。它们可以修饰成员变量、成员方法和内部类。

### 2.3 类的修饰符

类的修饰符用于表示类的特殊性质。

| 类的修饰符 | 简单含义 | 当前阶段理解 |
| --- | --- | --- |
| `abstract` | 抽象类，不能直接创建对象 | 后续抽象类章节讲 |
| `final` | 最终类，不能被继承 | 后续 `final` 章节讲 |

普通顶级类不能使用 `static` 修饰。`static` 可以修饰成员变量、成员方法和静态内部类。

本章先使用普通类，不展开 `abstract` 和 `final` 的细节。

## 三、对象是什么

对象是根据类创建出来的具体数据。

```java
public class EmployeeDemo {

    public static void main(String[] args) {
        Employee employee = new Employee();

        employee.id = 1001L;
        employee.name = "Tanaka";
        employee.department = "Sales";
        employee.email = "tanaka@example.com";

        System.out.println(employee.name); // 输出：Tanaka
    }
}
```

`new Employee()` 会创建一个员工对象。`employee` 是保存对象地址的引用变量。

## 四、类和对象的关系

```text
类：Employee
  ↓ new
对象1：id=1001, name=Tanaka
对象2：id=1002, name=Suzuki
```

一个类可以创建多个对象。每个对象都有自己独立的属性值。

```java
public class EmployeeDemo {

    public static void main(String[] args) {
        Employee employee1 = new Employee();
        employee1.name = "Tanaka";

        Employee employee2 = new Employee();
        employee2.name = "Suzuki";

        System.out.println(employee1.name); // 输出：Tanaka
        System.out.println(employee2.name); // 输出：Suzuki
    }
}
```

## 五、属性是什么

属性是对象保存的数据，也叫成员变量。

```java
public class Employee {
    Long id;
    String name;
    String department;
}
```

属性通常表示对象状态，例如姓名、年龄、价格、数量。

### 5.1 成员变量定义格式

成员变量的定义格式如下：

```java
[访问限定符] [修饰符] 数据类型 成员变量名 [= 初始值];
```

示例：

```java
public class Employee {
    public Long id;
    public String name;
    public String department;
}
```

格式说明：

1. 成员变量的访问限定符可以使用 `public`、`protected`、默认不写、`private`。
2. 成员变量可以使用的常见修饰符有 `static`、`final`。
3. 数据类型可以是基本类型，也可以是引用类型。
4. 成员变量写在类中、方法外。

成员变量访问限定符：

| 访问限定符 | 简单含义 | 常见用途 |
| --- | --- | --- |
| `public` | 任意位置可以访问 | 常量字段偶尔使用，普通属性不推荐 |
| `protected` | 同包或子类可以访问 | 继承相关场景 |
| 默认不写 | 同包可以访问 | 包内简单类 |
| `private` | 只能当前类内部访问 | 普通属性最常用 |

成员变量常见修饰符：

| 修饰符 | 简单含义 | 示例 |
| --- | --- | --- |
| `static` | 属于类本身，所有对象共享 | `static int count` |
| `final` | 赋值后不能重新赋值 | `final String name` |
| `static final` | 静态常量 | `static final String ACTIVE = "ACTIVE"` |

本章为了方便理解对象，部分属性会先不写 `private`。后续学习封装后，属性会改成 `private`，再通过 getter/setter 访问。

### 5.2 属性的默认值

成员变量如果没有手动赋值，会有默认值。

| 数据类型 | 示例 | 默认值 |
| --- | --- | --- |
| 整数类型 | `byte`、`short`、`int`、`long` | `0` |
| 小数类型 | `float`、`double` | `0.0` |
| 字符类型 | `char` | 空字符 |
| 布尔类型 | `boolean` | `false` |
| 引用类型 | `String`、数组、类、接口 | `null` |

局部变量没有默认值，使用前必须手动赋值。

### 5.3 成员变量和局部变量的区别

成员变量和局部变量都可以保存数据，但它们的位置、生命周期和默认值不同。

| 对比项 | 成员变量 | 局部变量 |
| --- | --- | --- |
| 定义位置 | 类中，方法外 | 方法、构造方法或代码块中 |
| 作用范围 | 当前对象内部都可以使用 | 只在定义它的代码块内部使用 |
| 默认值 | 有默认值 | 没有默认值，使用前必须赋值 |
| 生命周期 | 随对象创建而存在，随对象回收而结束 | 方法或代码块执行时创建，结束后失效 |
| 常见用途 | 保存对象状态，例如员工姓名、部门、邮箱 | 保存临时计算结果，例如循环计数、方法中间结果 |
| 命名习惯 | 表达对象属性含义 | 表达当前逻辑中的临时含义 |

示例：

```java
public class Employee {
    String name; // 成员变量：属于对象，默认值是 null

    public void printName() {
        String message = "员工姓名：" + name; // 局部变量：属于当前方法，使用前必须赋值
        System.out.println(message); // 输出：员工姓名：Tanaka
    }
}
```

如果成员变量和局部变量同名，可以使用 `this` 区分。

```java
public class Employee {
    String name;

    public void setName(String name) {
        this.name = name; // this.name 是成员变量，右边的 name 是方法参数
    }
}
```

## 六、引用是什么

对象创建在内存中，变量保存的是对象的引用。

```java
Employee employee1 = new Employee();
employee1.name = "Tanaka";

Employee employee2 = employee1;
employee2.name = "Suzuki";

System.out.println(employee1.name); // 输出：Suzuki
```

`employee1` 和 `employee2` 指向同一个对象，所以通过其中一个引用修改属性，另一个引用看到的值也会变化。

## 七、null 是什么

`null` 表示引用变量没有指向任何对象。

```java
Employee employee = null;
System.out.println(employee.name);
```

这段代码会出现 `NullPointerException`。使用对象前必须确认对象已经创建。

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 类名小写 | 不符合 Java 命名习惯 | 使用 `Employee` |
| 文件名和 public 类名不一致 | Java 要求一致 | `Employee.java` 中写 `public class Employee` |
| 顶级类使用 `private` 或 `protected` | 普通顶级类不能这样修饰 | 使用 `public` 或默认不写 |
| 顶级类使用 `static` | 普通顶级类不能使用 `static` | 删除 `static` |
| 对 `null` 调用属性或方法 | 没有创建对象 | 先 `new Employee()` |
| 一个类承担太多内容 | 类设计不清楚 | 按数据含义拆分类 |

## 九、本章总结

- 类是对象的设计图。
- 对象是根据类创建出来的具体实例。
- 属性保存对象数据。
- 引用变量保存对象地址。
- `null` 表示没有指向对象，使用时容易产生空指针异常。
