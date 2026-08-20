# 第16章 Java 泛型

> 本章目标：理解泛型解决什么问题，掌握泛型类、泛型方法、泛型接口和常见通配符写法，能够看懂 `List<Employee>`、`Map<String, Object>`、`Response<T>` 这类项目代码。

## 一、泛型是什么

泛型用于在编译期约束数据类型。

没有泛型时：

```java
import java.util.ArrayList;
import java.util.List;

public class RawListDemo {

    public static void main(String[] args) {
        List names = new ArrayList();
        names.add("Tanaka");
        names.add(100);

        String firstName = (String) names.get(0);
        System.out.println(firstName); // 输出：Tanaka
    }
}
```

没有泛型时，集合中可以放入不同类型的数据，取出时还需要强制类型转换。

使用泛型：

```java
import java.util.ArrayList;
import java.util.List;

public class GenericListDemo {

    public static void main(String[] args) {
        List<String> names = new ArrayList<>();
        names.add("Tanaka");

        String firstName = names.get(0);
        System.out.println(firstName); // 输出：Tanaka
    }
}
```

`List<String>` 表示这个列表只能保存字符串。写错类型时，编译阶段就能发现问题。

## 二、为什么需要泛型

泛型的作用：

- 减少强制类型转换。
- 提前发现类型错误。
- 让代码含义更清楚。
- 支撑集合、返回结果、分页结果等通用结构。

项目中常见泛型写法：

```java
List<Employee>
Map<String, Object>
Response<Employee>
PageResult<Order>
```

## 三、泛型类

泛型类是在类名后面定义类型占位符。

```java
public class Box<T> {
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
```

`T` 是类型占位符，使用时再指定真实类型。

```java
public class BoxDemo {

    public static void main(String[] args) {
        Box<String> box = new Box<>();
        box.setData("success");

        String value = box.getData();
        System.out.println(value); // 输出：success
    }
}
```

## 四、泛型方法

泛型方法是在返回值类型前声明类型占位符。

```java
public class ResponseFactory {

    public static <T> Box<T> boxOf(T data) {
        Box<T> box = new Box<>();
        box.setData(data);
        return box;
    }
}
```

调用：

```java
public class ResponseFactoryDemo {

    public static void main(String[] args) {
        Box<String> result = ResponseFactory.boxOf("OK");
        System.out.println(result.getData()); // 输出：OK
    }
}
```

## 五、泛型接口

接口也可以使用泛型。

```java
public interface Repository<T> {
    T findById(Long id);

    void save(T data);
}
```

实现类指定具体类型：

```java
public class EmployeeRepository implements Repository<Employee> {

    @Override
    public Employee findById(Long id) {
        return new Employee(id, "Tanaka");
    }

    @Override
    public void save(Employee data) {
        System.out.println("保存员工：" + data.getName());
    }
}
```

泛型接口常用于定义通用的数据访问、转换、处理能力。

## 六、泛型约束

泛型可以限制类型范围。

```java
public class NumberBox<T extends Number> {
    private T value;

    public NumberBox(T value) {
        this.value = value;
    }

    public double toDouble() {
        return value.doubleValue();
    }
}
```

`T extends Number` 表示 `T` 必须是 `Number` 或 `Number` 的子类，例如 `Integer`、`Long`、`Double`。

## 七、通配符

`?` 表示未知类型。

```java
import java.util.List;

public class WildcardDemo {

    public void printList(List<?> values) {
        for (Object value : values) {
            System.out.println(value);
        }
    }
}
```

`List<?>` 可以接收 `List<String>`、`List<Integer>`、`List<Employee>` 等不同类型列表，适合只读取、不关心具体类型的场景。

## 八、extends 通配符

`? extends T` 表示接收 `T` 或 `T` 的子类，适合读取数据。

```java
import java.util.List;

public class ExtendsWildcardDemo {

    public double sum(List<? extends Number> numbers) {
        double total = 0;

        for (Number number : numbers) {
            total += number.doubleValue();
        }

        return total;
    }
}
```

`List<? extends Number>` 可以接收 `List<Integer>`、`List<Long>`、`List<Double>`。

## 九、super 通配符

`? super T` 表示接收 `T` 或 `T` 的父类，适合写入数据。

```java
import java.util.List;

public class SuperWildcardDemo {

    public void addEmployees(List<? super Employee> employees) {
        employees.add(new Employee(1001L, "Tanaka"));
        employees.add(new Employee(1002L, "Suzuki"));
    }
}
```

`List<? super Employee>` 可以接收 `List<Employee>` 或 `List<Object>`。

新人阶段先记住：

- `extends` 更适合读。
- `super` 更适合写。
- 不确定时先使用明确类型，例如 `List<Employee>`。

## 十、泛型擦除

Java 泛型主要在编译期发挥作用。编译后，很多泛型信息会被擦除，这叫泛型擦除。

```java
List<String> names = new ArrayList<>();
List<Integer> scores = new ArrayList<>();
```

运行时它们主要都是 `ArrayList` 对象。新人阶段只需要知道：泛型可以帮助编译器检查类型，但不要把泛型理解成运行时创建了全新的集合类型。

## 十一、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 使用原始 `List` | 失去类型检查 | 使用 `List<String>`、`List<Employee>` |
| 滥用 `Object` | 调用方不知道真实类型 | 使用泛型表达真实数据类型 |
| `List<String>` 赋值给 `List<Object>` | 泛型不支持这种直接赋值 | 使用通配符或明确类型 |
| 不理解 `? extends` 不能随意添加元素 | 子类型不确定 | 读数据用 `extends`，写数据用 `super` |
| 泛型类和泛型方法的 `<T>` 位置混淆 | 声明位置不同 | 泛型类写在类名后，泛型方法写在返回值前 |

## 十二、本章练习

请完成：

1. 创建 `List<String>` 保存员工姓名。
2. 创建 `Box<T>` 泛型类。
3. 使用 `Box<Employee>` 保存员工对象。
4. 创建 `Repository<T>` 泛型接口。
5. 创建 `Response<T>` 类，包含 `success`、`message`、`data` 三个字段。
6. 编写 `printList(List<?> values)` 方法。
7. 说明 `List<String>` 和原始 `List` 的区别。

## 十三、本章总结

- 泛型用于在编译期约束类型。
- 泛型可以减少强制类型转换，让代码含义更清楚。
- 泛型类、泛型方法、泛型接口都很常见。
- `?` 表示未知类型。
- `? extends T` 适合读取，`? super T` 适合写入。
- Java 泛型主要在编译期工作，存在类型擦除。
