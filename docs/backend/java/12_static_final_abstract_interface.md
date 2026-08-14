# 第12章 Java static、final、抽象类与接口

> 本章目标：理解属于类本身的成员、常量设计、抽象类和接口的基本用法。

## 一、static 是什么

`static` 表示成员属于类本身，而不是属于某一个对象。

```java
public class EmployeeCounter {
    public static int count = 0;

    public EmployeeCounter() {
        count++;
    }
}
```

使用：

```java
new EmployeeCounter();
new EmployeeCounter();

System.out.println(EmployeeCounter.count); // 输出：2
```

`count` 属于 `EmployeeCounter` 类，所有对象共享同一个值。

## 二、static 方法

`static` 方法可以通过类名直接调用。

```java
public class StringUtil {

    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
```

调用：

```java
System.out.println(StringUtil.isBlank("")); // 输出：true
```

工具类方法经常写成 `static`。

## 三、final 是什么

`final` 表示不可再次改变。

修饰变量：

```java
final int maxCount = 100;
```

修饰常量：

```java
public class EmployeeStatus {
    public static final String ACTIVE = "ACTIVE";
    public static final String RETIRED = "RETIRED";
}
```

常量命名使用大写加下划线。

## 四、final 修饰类和方法

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

## 五、抽象类

抽象类用于抽取共同属性和共同方法，同时允许子类补充具体实现。

```java
public abstract class Employee {
    protected String name;

    public Employee(String name) {
        this.name = name;
    }

    public abstract String getEmployeeType();
}
```

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
}
```

抽象类不能直接 `new`。

## 六、接口

接口用于定义一组能力或规范。

```java
public interface EmployeeRepository {
    Employee findById(Long id);
    void save(Employee employee);
}
```

实现类：

```java
public class MysqlEmployeeRepository implements EmployeeRepository {

    @Override
    public Employee findById(Long id) {
        return new Employee();
    }

    @Override
    public void save(Employee employee) {
        System.out.println("save employee");
    }
}
```

接口变量可以接收实现类对象：

```java
EmployeeRepository repository = new MysqlEmployeeRepository();
```

接口变量接收实现类对象时，也会体现多态。

## 七、抽象类和接口的区别

| 对比 | 抽象类 | 接口 |
| --- | --- | --- |
| 关系 | “是什么”的父子关系 | “能做什么”的能力规范 |
| 继承数量 | 只能继承一个类 | 可以实现多个接口 |
| 构造方法 | 可以有 | 没有普通构造方法 |
| 成员变量 | 可以有普通字段 | 主要是常量 |
| 常见场景 | 抽取共同代码 | 定义能力和规范 |

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
|  |
| 常量命名小写 | 不符合规范 | 使用 `UPPER_SNAKE_CASE` |
| 抽象类直接 new | 抽象类不能实例化 | new 子类对象 |
| 接口中写大量实现细节 | 接口职责不清 | 接口只表达核心能力 |

## 九、本章练习

请完成：

1. 创建 `EmployeeStatus` 常量类。
2. 创建抽象类 `Employee`，定义抽象方法 `getEmployeeType()`。
3. 创建接口 `EmployeeRepository`。
4. 创建实现类 `MemoryEmployeeRepository`。

## 十、本章总结

- `static` 表示属于类本身。
- `final` 表示不可再次改变。
- 抽象类适合抽取共同属性和行为。
- 接口适合定义能力规范。
- 接口适合定义一组能力规范。
