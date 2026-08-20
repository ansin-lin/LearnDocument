# 第12章 Java static、final、抽象类、接口与内部类

> 本章目标：理解 `static`、`final`、抽象类、接口和内部类的基本用法，能够看懂项目中常见的常量类、工具类、抽象父类、接口实现类和简单内部类写法。

## 一、static 是什么

`static` 表示成员属于类本身，而不是属于某一个对象。普通成员变量属于对象，每个对象各有一份。静态变量属于类，所有对象共享同一份。

```java
public class EmployeeCounter {
    public static int count = 0;

    public EmployeeCounter() {
        count++;
    }

    public static void main(String[] args) {
        new EmployeeCounter();
        new EmployeeCounter();

        System.out.println(EmployeeCounter.count); // 输出：2
    }
}
```

## 二、static 方法

`static` 方法可以通过类名直接调用，不需要先创建对象。

```java
public class StringUtil {

    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    public static void main(String[] args) {
        System.out.println(StringUtil.isBlank("")); // 输出：true
        System.out.println(StringUtil.isBlank("Java")); // 输出：false
    }
}
```

静态方法中不能直接访问普通成员变量，因为普通成员变量必须依赖具体对象。

## 三、static 代码块

`static` 代码块会在类第一次被使用时执行一次，常用于初始化静态数据。

```java
public class EmployeeStatus {
    public static final String ACTIVE;

    static {
        ACTIVE = "ACTIVE";
        System.out.println("初始化员工状态");
    }

    public static void main(String[] args) {
        System.out.println(EmployeeStatus.ACTIVE); // 输出：ACTIVE
    }
}
```

## 四、静态变量初始化顺序

静态成员按照代码从上到下的顺序初始化。

```java
public class StaticOrderDemo {
    public static int count = 10;

    static {
        count = count + 5;
    }

    public static void main(String[] args) {
        System.out.println(StaticOrderDemo.count); // 输出：15
    }
}
```

阅读代码时，要按出现顺序理解静态变量和静态代码块。

## 五、final 是什么

`final` 表示不可再次改变。

```java
final int maxCount = 100;
```

静态常量通常写成 `public static final`。

```java
public class EmployeeStatus {
    public static final String ACTIVE = "ACTIVE";
    public static final String RETIRED = "RETIRED";
}
```

常量命名使用大写加下划线。

## 六、final 修饰引用类型

`final` 修饰引用类型时，表示变量不能再指向其他对象，但对象内部内容是否能修改，取决于对象本身。

```java
public class FinalReferenceDemo {

    public static void main(String[] args) {
        final Employee employee = new Employee("Tanaka");

        employee.setName("Suzuki");
        System.out.println(employee.getName()); // 输出：Suzuki

        // employee = new Employee("Yamada"); // 编译错误：final 引用不能重新赋值
    }
}
```

`final employee` 不能重新指向新对象，但如果 `Employee` 提供了 `setName()`，对象内容仍然可以改变。

## 七、final 修饰类和方法

`final` 修饰类：不能被继承。

```java
public final class StringUtil {
}
```

`final` 修饰方法：不能被子类重写。

```java
public class Employee {

    public final String getSystemName() {
        return "Employee System";
    }
}
```

## 八、抽象类

抽象类用于抽取共同属性、共同方法，并把必须由子类实现的行为定义出来。

```java
public abstract class Employee {
    protected String name;

    public Employee(String name) {
        this.name = name;
    }

    public void printName() {
        System.out.println(name);
    }

    public abstract String getEmployeeType();
}
```

抽象类特点：

- 使用 `abstract class` 定义。
- 可以有普通成员变量。
- 可以有构造方法。
- 可以有普通方法。
- 可以有抽象方法。
- 不能直接 `new`。

抽象方法没有方法体，子类必须实现。

```java
public class FullTimeEmployee extends Employee {

    public FullTimeEmployee(String name) {
        super(name);
    }

    @Override
    public String getEmployeeType() {
        return "正式员工";
    }

    public static void main(String[] args) {
        Employee employee = new FullTimeEmployee("Tanaka");
        employee.printName(); // 输出：Tanaka
        System.out.println(employee.getEmployeeType()); // 输出：正式员工
    }
}
```

如果子类不实现父类的抽象方法，那么子类也必须声明为抽象类。

## 九、接口

接口用于定义一组能力或规范。

```java
public interface EmployeeRepository {
    Employee findById(Long id);

    void save(Employee employee);
}
```

实现类使用 `implements` 实现接口。

```java
public class MemoryEmployeeRepository implements EmployeeRepository {

    @Override
    public Employee findById(Long id) {
        return new Employee(id, "Tanaka");
    }

    @Override
    public void save(Employee employee) {
        System.out.println("保存员工：" + employee.getName());
    }
}
```

接口变量可以接收实现类对象。

```java
EmployeeRepository repository = new MemoryEmployeeRepository();
Employee employee = repository.findById(1001L);
System.out.println(employee.getName()); // 输出：Tanaka
```

接口常见规则：

- 接口不能直接 `new`。
- 一个类可以实现多个接口。
- 接口中的字段默认是 `public static final`。
- 接口中的抽象方法默认是 `public abstract`。
- Java 8 以后接口可以有 `default` 方法和 `static` 方法。

## 十、接口默认方法和静态方法

`default` 方法可以在接口中提供默认实现。

```java
public interface EmployeeRepository {

    Employee findById(Long id);

    default boolean existsById(Long id) {
        return findById(id) != null;
    }
}
```

接口中的 `static` 方法通过接口名调用。

```java
public interface EmployeeValidator {

    static boolean isValidName(String name) {
        return name != null && !name.isBlank();
    }
}
```

```java
System.out.println(EmployeeValidator.isValidName("Tanaka")); // 输出：true
```

## 十一、抽象类和接口的区别

| 对比 | 抽象类 | 接口 |
| --- | --- | --- |
| 关系 | “是什么”的父子关系 | “能做什么”的能力规范 |
| 继承数量 | 只能继承一个类 | 可以实现多个接口 |
| 构造方法 | 可以有构造方法 | 没有普通构造方法 |
| 成员变量 | 可以有普通字段 | 字段默认是常量 |
| 普通方法 | 可以有普通方法 | Java 8 后可以有默认方法 |
| 常见场景 | 抽取共同代码和共同状态 | 定义调用规范和能力边界 |

如果多个类有共同状态和共同代码，可以考虑抽象类。如果只想定义一组行为规范，优先考虑接口。

## 十二、内部类

内部类是定义在另一个类里面的类。

| 类型 | 简单说明 | 常见场景 |
| --- | --- | --- |
| 成员内部类 | 写在类中，和成员变量同级 | 内部对象强依赖外部对象 |
| 静态内部类 | 使用 `static` 修饰 | 作为外部类的辅助结构 |
| 局部内部类 | 写在方法内部 | 使用较少 |
| 匿名内部类 | 没有类名，临时实现接口或抽象类 | 回调、监听器、一次性实现 |

静态内部类示例：

```java
public class EmployeeReport {

    static class Row {
        private String text;

        public Row(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public static void main(String[] args) {
        EmployeeReport.Row row = new EmployeeReport.Row("1001,Tanaka");
        System.out.println(row.getText()); // 输出：1001,Tanaka
    }
}
```

### 12.1 匿名内部类

匿名内部类是没有类名的内部类。

它通常用于临时实现一个接口，或者临时继承一个抽象类。

基本格式：

```java
接口名 变量名 = new 接口名() {
    @Override
    public 返回值类型 方法名(参数列表) {
        方法内容
    }
};
```

这段代码中，`new 接口名() { ... }` 不是直接创建接口对象。

接口不能直接创建对象。这里真正创建的是一个“没有名字的实现类对象”。

#### 12.1.1 基于接口创建匿名内部类

示例：临时创建一个任务对象。

```java
public class AnonymousClassDemo {

    public static void main(String[] args) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                System.out.println("执行任务");
            }
        };

        task.run(); // 输出：执行任务
    }
}
```

代码说明：

- `Runnable` 是 Java 提供的接口。
- `Runnable` 中有一个 `run()` 方法。
- `new Runnable() { ... }` 表示临时创建一个 `Runnable` 接口的实现类对象。
- `@Override` 表示重写接口中的 `run()` 方法。
- `task.run()` 会执行匿名内部类中写好的 `run()` 方法。

匿名内部类经常用于“只使用一次”的接口实现。

如果为了这一点点逻辑单独写一个实现类，代码会比较分散。

#### 12.1.2 基于抽象类创建匿名内部类

匿名内部类也可以临时继承抽象类。

示例：临时创建一个报表导出任务。

```java
abstract class ExportTask {

    public void printStartMessage() {
        System.out.println("开始导出");
    }

    public abstract void export();
}

public class AnonymousAbstractClassDemo {

    public static void main(String[] args) {
        ExportTask task = new ExportTask() {
            @Override
            public void export() {
                System.out.println("导出员工CSV文件");
            }
        };

        task.printStartMessage(); // 输出：开始导出
        task.export(); // 输出：导出员工CSV文件
    }
}
```

代码说明：

- `ExportTask` 是抽象类，不能直接 `new ExportTask()`。
- `new ExportTask() { ... }` 表示临时创建一个继承 `ExportTask` 的子类对象。
- 匿名子类必须实现抽象方法 `export()`。
- 匿名子类对象可以调用父类中已经定义好的普通方法 `printStartMessage()`。

#### 12.1.3 匿名内部类的使用场景

匿名内部类常见于下面这些场景：

| 场景 | 说明 |
| --- | --- |
| 回调处理 | 把一段处理逻辑传给其他对象 |
| 事件监听 | 按钮点击、菜单点击等事件处理 |
| 线程任务 | 临时定义一个要执行的任务 |
| 测试代码 | 临时创建接口或抽象类的实现 |
| 一次性业务规则 | 某段逻辑只在当前位置使用 |

匿名内部类的缺点：

- 代码层级比较深。
- 方法体较长时可读性下降。
- 多个匿名内部类嵌套时不容易维护。

Java 8 以后，如果接口只有一个抽象方法，很多匿名内部类可以改写成 Lambda 表达式。

例如下面的匿名内部类：

```java
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("执行任务");
    }
};
```

可以简化成：

```java
Runnable task = () -> System.out.println("执行任务");
```

这里先理解“匿名内部类可以临时实现接口”即可。

Lambda 表达式作为简化匿名内部类的书写。

## 十三、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 常量命名小写 | 不符合 Java 命名习惯 | 使用 `UPPER_SNAKE_CASE` |
| 在静态方法中直接访问普通成员变量 | 普通成员变量属于对象 | 先创建对象，或改为静态成员 |
| 误以为 `final` 引用对象完全不可变 | `final` 只限制引用不能改 | 区分引用不可变和对象内容不可变 |
| 抽象类直接 `new` | 抽象类不能实例化 | 创建具体子类对象 |
| 子类没有实现抽象方法 | 抽象方法必须被实现 | 实现方法或把子类也声明为抽象类 |
| 接口中写大量实现细节 | 接口职责不清 | 接口只表达核心能力 |
| 忘记接口可以多实现 | 把接口当成普通继承 | 使用 `implements A, B` 实现多个接口 |
| 误以为 `new 接口名()` 是直接创建接口对象 | 匿名内部类语法容易误解 | 理解为创建接口的临时实现类对象 |
| 匿名内部类中写太多业务逻辑 | 代码可读性差 | 逻辑复杂时改成普通实现类 |

## 十四、本章练习

请完成：

1. 创建 `EmployeeStatus` 常量类，定义 `ACTIVE` 和 `RETIRED`。
2. 创建 `StringUtil` 工具类，编写 `isBlank(String text)` 静态方法。
3. 创建抽象类 `Employee`，定义普通方法 `printName()` 和抽象方法 `getEmployeeType()`。
4. 创建接口 `EmployeeRepository`，定义 `findById(Long id)` 和 `save(Employee employee)`。
5. 创建实现类 `MemoryEmployeeRepository`。
6. 创建一个静态内部类 `Row`，保存一行导出文本。
7. 使用匿名内部类创建一个 `Runnable` 对象，并调用 `run()` 输出 `执行数据检查`。

## 十五、本章总结

- `static` 表示属于类本身，适合静态变量、工具方法和静态初始化。
- `final` 表示不可再次改变，但修饰引用类型时不代表对象内容一定不可变。
- 抽象类适合抽取共同属性、共同方法和必须由子类实现的行为。
- 接口适合定义能力规范和调用边界。
- 内部类是类内部的辅助类，新人阶段能看懂基本写法即可。
- 匿名内部类可以临时实现接口或继承抽象类，是理解 Lambda 表达式的重要基础。
