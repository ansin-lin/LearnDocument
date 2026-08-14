# 第10章 Java 构造方法、封装与 JavaBean

> 本章目标：理解对象创建和初始化过程，掌握构造方法、`this`、封装、访问修饰符和 JavaBean 写法。

## 一、对象为什么需要初始化

创建对象后，通常需要给对象设置初始值。

```java
Employee employee = new Employee();
employee.name = "Tanaka";
employee.department = "Sales";
```

如果每次创建对象都手动设置属性，容易遗漏。构造方法可以在创建对象时完成初始化。

## 二、构造方法是什么

构造方法是在 `new` 对象时自动执行的方法。

```java
public class Employee {
    String name;
    String department;

    public Employee(String name, String department) {
        this.name = name;
        this.department = department;
    }
}
```

使用：

```java
Employee employee = new Employee("Tanaka", "Sales");
System.out.println(employee.name); // 输出：Tanaka
```

构造方法特点：

- 方法名必须和类名相同
- 没有返回值类型
- 创建对象时自动调用

## 三、默认构造方法

如果类中没有写任何构造方法，Java 会自动提供无参构造方法。

```java
public class Employee {
}
```

等价于：

```java
public class Employee {
    public Employee() {
    }
}
```

如果手动写了有参构造方法，Java 不会自动再提供无参构造方法。

## 四、this 的作用

`this` 表示当前对象。

```java
public class Employee {
    String name;

    public Employee(String name) {
        this.name = name;
    }
}
```

`this.name` 表示成员变量，右侧 `name` 表示构造方法参数。

## 五、封装是什么

封装是把对象内部属性保护起来，通过方法控制访问。

不推荐：

```java
public class Employee {
    public String name;
}
```

推荐：

```java
public class Employee {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("员工姓名不能为空");
        }
        this.name = name;
    }
}
```

`private` 可以防止外部直接修改属性，`setName()` 可以加入校验逻辑。

## 六、访问修饰符

| 修饰符 | 可访问范围 | 常见用途 |
| --- | --- | --- |
| `public` | 所有地方 | 对外公开类和方法 |
| `protected` | 同包和子类 | 继承扩展 |
| 默认 | 同包 | 包内使用 |
| `private` | 当前类内部 | 保护成员变量 |

## 七、JavaBean

JavaBean 是 Java 中常见的数据类写法。

```java
public class Employee {
    private Long id;
    private String name;
    private String department;

    public Employee() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
```

JavaBean 常用于封装一组相关数据。

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 构造方法写了返回值 | 构造方法不能写返回值类型 | 删除返回值类型 |
| 有参构造后忘记无参构造 | 只能使用有参方式创建对象 | 根据需要补充无参构造 |
| 属性全部 public | 破坏封装 | 使用 private 和 getter/setter |
| setter 不做校验 | 非法数据进入对象 | 在 setter 中加入必要校验 |

## 九、本章练习

请完成：

1. 创建 `Employee` 类，包含 `id`、`name`、`department`。
2. 添加无参构造方法和有参构造方法。
3. 给字段添加 getter 和 setter。
4. 在 `setName()` 中校验姓名不能为空。
