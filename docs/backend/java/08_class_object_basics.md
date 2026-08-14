# 第8章 Java 类与对象基础

> 本章目标：理解类和对象的关系，能够定义一个简单类，创建对象，给对象属性赋值，并调用对象的简单方法。

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
2. 访问限定符用于决定这个类可以被哪些地方使用。
3. 修饰符用于决定这个类的特殊使用方式。
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

### 2.2 访问限定符的简单说明

访问限定符用于控制类、属性和方法可以被哪些代码访问。

| 访问限定符 | 简单含义 | 当前阶段理解 |
| --- | --- | --- |
| `public` | 公有的，其他地方可以访问 | 类通常先写成 `public` |
| `private` | 私有的，只能当前类内部访问 | 后续封装章节重点使用 |
| `protected` | 同包或子类可以访问 | 后续继承章节再深入 |
| 默认不写 | 同一个包中可以访问 | 先知道即可 |

本章为了方便理解对象，部分属性会先不写 `private`。后续学习封装后，属性会改成 `private`，再通过 getter/setter 访问。

### 2.3 修饰符的简单说明

修饰符用于表示类或成员的特殊性质。

| 修饰符 | 简单含义 | 当前阶段理解 |
| --- | --- | --- |
| `abstract` | 抽象类，不能直接创建对象 | 后续抽象类章节讲 |
| `final` | 最终类，不能被继承 | 后续 `final` 章节讲 |
| `static` | 属于类本身 | 后续 `static` 章节讲 |

本章先使用普通类，不展开 `abstract`、`final`、`static` 的细节。

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

1. 访问限定符用于控制成员变量能否被外部访问。
2. 修饰符用于表示成员变量的特殊使用方式。
3. 数据类型可以是基本类型，也可以是引用类型。
4. 成员变量写在类中、方法外。

### 5.2 属性的默认值

成员变量如果没有手动赋值，会有默认值。

| 数据类型 | 示例 | 默认值 |
| --- | --- | --- |
| 整数类型 | `byte`、`short`、`int`、`long` | `0` |
| 小数类型 | `float`、`double` | `0.0` |
| 字符类型 | `char` | 空字符 |
| 布尔类型 | `boolean` | `false` |
| 引用类型 | `String`、数组、类、接口 | `null` |

局部变量没有默认值，使用前必须手动赋值。成员变量和局部变量的区别会在后续方法章节中继续看到。

## 六、简单方法是什么

方法表示对象可以执行的动作。本章只做最小理解，下一章会系统讲方法。

```java
public class Employee {
    String name;

    void printName() {
        System.out.println(name);
    }
}
```

调用方法：

```java
public class EmployeeDemo {

    public static void main(String[] args) {
        Employee employee = new Employee();
        employee.name = "Tanaka";
        employee.printName(); // 输出：Tanaka
    }
}
```

## 七、引用是什么

对象创建在内存中，变量保存的是对象的引用。

```java
Employee employee1 = new Employee();
employee1.name = "Tanaka";

Employee employee2 = employee1;
employee2.name = "Suzuki";

System.out.println(employee1.name); // 输出：Suzuki
```

`employee1` 和 `employee2` 指向同一个对象，所以通过其中一个引用修改属性，另一个引用看到的值也会变化。

## 八、null 是什么

`null` 表示引用变量没有指向任何对象。

```java
Employee employee = null;
System.out.println(employee.name);
```

这段代码会出现 `NullPointerException`。使用对象前必须确认对象已经创建。

## 九、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 类名小写 | 不符合 Java 命名习惯 | 使用 `Employee` |
| 文件名和 public 类名不一致 | Java 要求一致 | `Employee.java` 中写 `public class Employee` |
| 对 `null` 调用属性或方法 | 没有创建对象 | 先 `new Employee()` |
| 一个类承担太多内容 | 类设计不清楚 | 按数据含义拆分类 |

## 十、本章总结

- 类是对象的设计图。
- 对象是根据类创建出来的具体实例。
- 属性保存对象数据。
- 方法表示对象行为。
- 引用变量保存对象地址。
- `null` 表示没有指向对象，使用时容易产生空指针异常。
