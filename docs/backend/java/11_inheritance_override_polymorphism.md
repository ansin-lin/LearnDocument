# 第11章 Java 继承、重写与多态

> 本章目标：理解继承解决什么问题，掌握 `extends`、`super`、`@Override`、方法重写、多态和 `instanceof` 的基本用法。

## 一、为什么需要继承

当多个类有相同属性和方法时，可以把共同部分抽取到父类。

例如正式员工和派遣员工都有姓名、部门和打印信息的方法。

不使用继承时：

```java
public class FullTimeEmployee {
    private String name;
    private String department;
}

public class ContractEmployee {
    private String name;
    private String department;
}
```

重复字段较多。可以抽取父类：

```java
public class Employee {
    protected String name;
    protected String department;
}
```

## 二、extends

`extends` 表示子类继承父类。

```java
public class Employee {
    protected String name;
    protected String department;

    public void printInfo() {
        System.out.println(name + " / " + department);
    }
}
```

```java
public class FullTimeEmployee extends Employee {
    private int monthlySalary;
}
```

`FullTimeEmployee` 可以使用父类的 `name`、`department` 和 `printInfo()`。

## 三、super

`super` 表示父类对象部分，常用于调用父类构造方法或父类方法。

```java
public class Employee {
    protected String name;

    public Employee(String name) {
        this.name = name;
    }
}
```

```java
public class FullTimeEmployee extends Employee {

    public FullTimeEmployee(String name) {
        super(name);
    }
}
```

子类构造方法中，`super(name)` 会调用父类构造方法。

## 四、方法重写

方法重写是子类重新定义父类已有的方法。

判断一个方法是否是重写，不能只看方法名相同。Java 会根据“方法签名”和继承关系判断。

### 4.1 重写成立的条件

方法重写必须满足：

1. 子类和父类之间必须有继承关系。
2. 子类方法名必须和父类方法名相同。
3. 子类方法参数列表必须和父类方法参数列表相同。
4. 子类方法返回值类型必须和父类方法返回值类型相同，或者是父类返回值类型的子类。
5. 子类方法访问权限不能比父类方法更严格。
6. `private` 方法不能被重写，因为子类无法继承父类的 `private` 方法。
7. `final` 方法不能被重写。
8. `static` 方法不属于对象行为，不能按普通重写理解。

其中“方法名 + 参数列表”通常称为方法签名。

```java
public class Employee {

    public String getEmployeeType() {
        return "员工";
    }
}
```

```java
public class FullTimeEmployee extends Employee {

    @Override
    public String getEmployeeType() {
        return "正式员工";
    }
}
```

`@Override` 用于告诉编译器：这个方法要重写父类方法。如果方法名或参数写错，编译器会报错。

### 4.2 @Override 的检查作用

推荐重写方法时总是写 `@Override`。

正确示例：

```java
public class Employee {

    public String getEmployeeType() {
        return "员工";
    }
}
```

```java
public class ContractEmployee extends Employee {

    @Override
    public String getEmployeeType() {
        return "派遣员工";
    }
}
```

错误示例：

```java
public class ContractEmployee extends Employee {

    @Override
    public String getEmployeType() {
        return "派遣员工";
    }
}
```

上面代码中 `getEmployeType()` 拼写错误，父类中没有这个方法。因为写了 `@Override`，编译器会直接报错，提醒这不是重写。

### 4.3 重写和重载的区别

| 对比 | 重写 Override | 重载 Overload |
| --- | --- | --- |
| 发生位置 | 父类和子类之间 | 同一个类中也可以发生 |
| 方法名 | 必须相同 | 必须相同 |
| 参数列表 | 必须相同 | 必须不同 |
| 返回值 | 相同或返回子类型 | 不作为判断依据 |
| 目的 | 改变父类方法行为 | 提供同名方法的不同参数版本 |

示例：这是重载，不是重写。

```java
public class Employee {

    public String getEmployeeType() {
        return "员工";
    }
}
```

```java
public class FullTimeEmployee extends Employee {

    public String getEmployeeType(String language) {
        return "正式员工";
    }
}
```

`getEmployeeType(String language)` 参数列表和父类不同，所以不是重写，而是子类中新增了一个重载方法。

## 五、多态

多态是指父类引用可以指向子类对象。

```java
public class PolymorphismDemo {

    public static void main(String[] args) {
        Employee employee = new FullTimeEmployee("Tanaka");
        System.out.println(employee.getEmployeeType()); // 输出：正式员工
    }
}
```

变量类型是 `Employee`，真实对象是 `FullTimeEmployee`。调用重写方法时，执行的是子类方法。

## 六、多态的意义

多态可以让代码依赖父类或接口，而不是依赖具体实现。

```java
public void printEmployeeType(Employee employee) {
    System.out.println(employee.getEmployeeType());
}
```

这个方法可以接收 `FullTimeEmployee`，也可以接收 `ContractEmployee`。

多态可以让同一个方法处理不同子类对象。

## 七、向上转型和向下转型

### 7.1 向上转型

向上转型是把子类对象赋值给父类引用。

```java
Employee employee = new FullTimeEmployee("Tanaka");
```

这是一种安全写法，因为正式员工本来就是员工的一种。向上转型后，可以调用父类中声明的方法。如果子类重写了该方法，运行时会执行子类版本。

### 7.2 向下转型

向下转型是把父类引用转换回子类类型。

```java
Employee employee = new FullTimeEmployee("Tanaka");

if (employee instanceof FullTimeEmployee fullTimeEmployee) {
    System.out.println(fullTimeEmployee.getEmployeeType()); // 输出：正式员工
}
```

向下转型必须先判断真实对象类型。否则可能出现 `ClassCastException`。

错误示例：

```java
Employee employee = new ContractEmployee("Suzuki");
FullTimeEmployee fullTimeEmployee = (FullTimeEmployee) employee; // 运行时异常
```

`employee` 的真实对象是 `ContractEmployee`，不能强制转换成 `FullTimeEmployee`。

## 八、多态中的方法和成员变量

多态调用成员方法时，看运行时真实对象。

```java
Employee employee = new FullTimeEmployee("Tanaka");
System.out.println(employee.getEmployeeType()); // 输出：正式员工
```

虽然变量类型是 `Employee`，但真实对象是 `FullTimeEmployee`，所以执行子类重写后的方法。

成员变量不按多态处理，成员变量看引用类型。

```java
public class Employee {
    public String type = "员工";
}
```

```java
public class FullTimeEmployee extends Employee {
    public String type = "正式员工";
}
```

```java
Employee employee = new FullTimeEmployee("Tanaka");
System.out.println(employee.type); // 输出：员工
```

项目中不建议通过同名成员变量表达多态行为。需要多态时，优先使用方法重写。

## 九、instanceof

`instanceof` 用于判断对象真实类型。

```java
if (employee instanceof FullTimeEmployee) {
    System.out.println("正式员工");
}
```

Java 17 可以使用模式匹配写法：

```java
if (employee instanceof FullTimeEmployee fullTimeEmployee) {
    System.out.println(fullTimeEmployee.getEmployeeType());
}
```

## 十、什么时候不适合继承

继承适合表达“子类是父类的一种”。如果只是想复用一点代码，不一定适合继承。

不适合继承的情况：

- 两个类不是明确的 `is-a` 关系。
- 父类修改会影响很多子类，关系过于紧密。
- 子类只是想使用另一个类的功能。
- 子类需要同时复用多个类的能力。

这种情况下，通常优先考虑把另一个类作为成员变量使用，也就是组合。

```java
public class CsvExporter {

    public void export(String text) {
        System.out.println(text);
    }
}
```

```java
public class EmployeeService {
    private CsvExporter csvExporter = new CsvExporter();

    public void exportEmployee(String employeeName) {
        csvExporter.export(employeeName);
    }
}
```

`EmployeeService` 不是 `CsvExporter` 的一种，只是使用 `CsvExporter` 的功能，所以组合比继承更合适。

## 十一、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 为了复用一点代码滥用继承 | 继承会形成强关系 | 优先判断是否真的是“is-a”关系 |
| 重写方法参数写错 | 实际变成重载 | 使用 `@Override` |
| 父类引用调用子类特有方法 | 编译类型看不到子类方法 | 先判断类型或重新设计抽象 |
| 子类构造忘记调用父类构造 | 父类没有无参构造 | 使用 `super(...)` |
| 没判断类型就强制向下转型 | 真实对象类型可能不匹配 | 先使用 `instanceof` 判断 |
| 通过同名成员变量理解多态 | 成员变量不按多态调用 | 使用方法重写表达多态 |

## 十二、本章练习

请完成：

1. 创建父类 `Employee`。
2. 创建子类 `FullTimeEmployee` 和 `ContractEmployee`。
3. 重写 `getEmployeeType()`。
4. 使用父类引用分别接收两个子类对象，并调用方法。
5. 使用 `instanceof` 判断对象类型后再向下转型。

## 十三、本章总结

- 继承用于表达“子类是父类的一种”。
- `extends` 用于继承父类。
- `super` 用于访问父类构造方法或方法。
- 方法重写让子类可以改变父类行为。
- 多态让父类引用可以指向子类对象。
- 多态调用方法看真实对象，成员变量看引用类型。
- 不满足“is-a”关系时，不要为了复用代码滥用继承。
