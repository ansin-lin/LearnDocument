# 第16章 Java 泛型

> 本章目标：理解泛型解决什么问题，掌握 `List<String>`、泛型类、泛型方法和常见通配符写法。

## 一、泛型是什么

泛型用于在编译期约束数据类型。

没有泛型时：

```java
List names = new ArrayList();
names.add("Tanaka");
names.add(100);
```

有泛型时：

```java
List<String> names = new ArrayList<>();
names.add("Tanaka");
```

`List<String>` 表示这个集合只能保存字符串。

## 二、为什么需要泛型

泛型的作用：

- 减少强制类型转换
- 提前发现类型错误
- 让代码含义更清楚
- 支撑通用容器结构和集合类型约束

Java 中常见：

```java
Box<String>
List<Employee>
Map<String, Object>
```

## 三、泛型类

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
Box<String> box = new Box<>();
box.setData("success");
```

## 四、泛型方法

```java
public class ResponseFactory {

    public static <T> Box<T> boxOf(T data) {
        Box<T> box = new Box<>();
        box.setData(data);
        return box;
    }
}
```

`<T>` 写在返回值类型前面，表示这是一个泛型方法。

## 五、通配符

`?` 表示未知类型。

```java
public void printList(List<?> values) {
    for (Object value : values) {
        System.out.println(value);
    }
}
```

新人阶段先掌握 `List<T>`、`Map<K, V>` 和简单 `?` 即可。

## 六、本章练习

请完成：

1. 创建 `List<String>` 保存员工姓名。
2. 创建 `Box<T>` 泛型类。
3. 使用 `Box<Employee>` 保存员工对象。
4. 说明 `List<String>` 和原始 `List` 的区别。
