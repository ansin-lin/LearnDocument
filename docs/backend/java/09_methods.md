# 第9章 Java 方法基础

> 本章目标：系统掌握方法的定义、调用、参数、返回值、重载和值传递，能够写出职责清楚的方法。

## 一、方法解决什么问题

方法用于把一段可以重复使用的逻辑封装起来。

不使用方法：

```java
public class PriceDemo {

    public static void main(String[] args) {
        int total1 = 100 * 3;
        int total2 = 200 * 5;

        System.out.println(total1); // 输出：300
        System.out.println(total2); // 输出：1000
    }
}
```

使用方法：

```java
public class PriceDemo {

    public static int calculateTotalPrice(int unitPrice, int quantity) {
        return unitPrice * quantity;
    }

    public static void main(String[] args) {
        System.out.println(calculateTotalPrice(100, 3)); // 输出：300
        System.out.println(calculateTotalPrice(200, 5)); // 输出：1000
    }
}
```

## 二、方法的结构

成员方法的完整定义格式如下：

```java
[访问限定符] [修饰符] 返回值类型 方法名([形式参数表]) [throws 异常表] {
    方法体
    return 返回值; // 可选
}
```

示例：

```java
public int calculateTotalPrice(int unitPrice, int quantity) {
    int totalPrice = unitPrice * quantity;
    return totalPrice;
}
```

| 部分 | 说明 |
| --- | --- |
| `public` | 访问修饰符，表示外部可以调用 |
| `int` | 返回值类型 |
| `calculateTotalPrice` | 方法名 |
| `int unitPrice, int quantity` | 参数列表 |
| `return totalPrice` | 返回计算结果 |

格式说明：

1. 访问限定符用于决定这个方法可以被哪些地方调用。
2. 修饰符用于表示方法的特殊使用方式。
3. 返回值类型必须是合法的 Java 数据类型；没有返回值时使用 `void`。
4. 方法名必须是合法 Java 标识符，一般使用小驼峰命名。
5. 形式参数表表示方法需要接收的数据，多个参数之间用逗号分隔。
6. `throws` 用于声明方法可能抛出的异常，后续异常章节会详细讲。
7. 方法体中写具体执行逻辑。

### 2.1 方法访问限定符的简单说明

| 访问限定符 | 简单含义 | 常见使用 |
| --- | --- | --- |
| `public` | 其他类可以调用 | 对外提供的方法 |
| `private` | 只有当前类内部可以调用 | 内部辅助方法 |
| `protected` | 同包或子类可以调用 | 继承相关场景 |
| 默认不写 | 同包可以调用 | 包内使用 |

新人阶段最常见的是 `public` 方法和 `private` 辅助方法。

### 2.2 方法修饰符的简单说明

| 修饰符 | 简单含义 | 当前阶段理解 |
| --- | --- | --- |
| `static` | 静态方法，可以通过类名调用 | `main` 方法和工具方法常见 |
| `final` | 最终方法，不能被子类重写 | 后续继承后再深入 |
| `abstract` | 抽象方法，没有方法体 | 后续抽象类章节讲 |
| `synchronized` | 同步方法，用于多线程 | 后续进阶内容 |
| `native` | 本地方法，调用非 Java 实现 | 了解即可 |

普通成员方法是最常见的方法形式。`static`、`final`、`abstract` 表示特殊方法行为，先理解它们的基本含义即可。

## 三、没有返回值的方法

没有返回值时使用 `void`。

```java
public class EmployeePrinter {

    public void printEmployeeName(String name) {
        System.out.println(name);
    }
}
```

`void` 方法可以不写 `return`，也可以写 `return;` 提前结束方法。

## 四、参数和返回值

参数是方法执行所需的输入，返回值是方法执行后的结果。

```java
public boolean isActiveEmployee(String status) {
    return "ACTIVE".equals(status);
}
```

调用：

```java
boolean active = isActiveEmployee("ACTIVE");
System.out.println(active); // 输出：true
```

参数和返回值应该表达清楚含义，不建议使用 `a`、`b`、`data1`。

## 五、方法调用过程

```java
public class EmployeeChecker {

    public boolean isActiveEmployee(String status) {
        return "ACTIVE".equals(status);
    }

    public void checkEmployee() {
        boolean active = isActiveEmployee("ACTIVE");
        System.out.println(active); // 输出：true
    }
}
```

执行 `checkEmployee()` 时，会进入 `isActiveEmployee()`，拿到返回值后再继续执行。

## 六、方法重载

方法重载是同一个类中方法名相同，但参数列表不同。

```java
public class EmployeeFinder {

    public String findEmployee(Long id) {
        return "find by id";
    }

    public String findEmployee(String email) {
        return "find by email";
    }
}
```

重载依据：

- 参数个数不同
- 参数类型不同
- 参数顺序不同

只改变返回值类型，不构成重载。

## 七、值传递

Java 的方法参数传递是值传递。

基本类型示例：

```java
public class ValueDemo {

    public static void changeNumber(int number) {
        number = 20;
    }

    public static void main(String[] args) {
        int count = 10;
        changeNumber(count);
        System.out.println(count); // 输出：10
    }
}
```

引用类型示例：

```java
public class ReferenceDemo {

    public static void changeName(Employee employee) {
        employee.name = "Suzuki";
    }

    public static void main(String[] args) {
        Employee employee = new Employee();
        employee.name = "Tanaka";

        changeName(employee);
        System.out.println(employee.name); // 输出：Suzuki
    }
}
```

引用类型传递的是引用值的副本，方法内部可以通过这个引用修改对象属性。

## 八、方法命名规范

方法名使用小驼峰，推荐“动词 + 名词”。

| 方法名 | 含义 |
| --- | --- |
| `findEmployee` | 查询员工 |
| `createEmployee` | 新增员工 |
| `updateEmployeeStatus` | 修改员工状态 |
| `deleteEmployee` | 删除员工 |
| `calculateTotalAmount` | 计算总金额 |

## 九、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 方法过长 | 一个方法做太多事 | 拆成多个小方法 |
| 方法名太模糊 | 看不出方法作用 | 使用动词加名词 |
| 返回值类型不匹配 | `return` 的值和声明不一致 | 修改返回值类型或返回值 |
| 误以为 Java 是引用传递 | 混淆引用值和对象 | 理解 Java 是值传递 |

## 十、本章练习

请完成：

1. 编写 `calculateTotalPrice(int unitPrice, int quantity)`。
2. 编写 `isActiveEmployee(String status)`。
3. 编写两个重载方法，分别按员工 ID 和邮箱查询员工。
4. 说明基本类型参数和引用类型参数传递的区别。
