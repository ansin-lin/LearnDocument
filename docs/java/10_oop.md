# 类的深入讲解

## 前言

学习 Java 的过程中，一开始很容易被各种变量类型绕晕。本文将深入讲解 Java 中的 **成员变量、实例变量、静态变量、局部变量** 的区别与联系，并进一步延伸讲解 **static、final**、以及 **抽象类、接口、内部类** 等相关概念。

---

## Java 中的变量分类

### 示例代码1

```java
package com.cunyu.demo;

public class Demo {

    private String name;  // 成员变量、实例变量
    private int age;      // 成员变量、实例变量
    private int ID;       // 成员变量、实例变量

    public static final String school = "卡塞尔学院"; // 成员变量、静态变量(类变量)
    public static String level = "SSS";              // 成员变量、静态变量(类变量)

    public int getAge() {
        return age;
    }

    public int getId() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setId(int ID) {
        this.ID = ID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void study() {
        String subject1 = "屠龙";   // 局部变量
        String subject2 = "炼金术"; // 局部变量
        System.out.println("学习科目：" + subject1 + "、" + subject2);
    }

    public static void main(String[] args) {
        Demo demo = new Demo();
        demo.setAge(23);
        demo.setId(14000001);
        demo.setName("楚子航");

        System.out.println("ID: " + demo.getId() + " Age: " + demo.getAge() + " Name: " + demo.getName());
        System.out.print("主修科目：");
        demo.study();

        System.out.println("学院：" + Demo.school);
        System.out.println("等级：" + Demo.level);
    }
}
```

---

### 各变量的联系与区别

| 类型 | 定义位置 | 是否有默认值 | 存储位置 | 生命周期 | 特点 |
|------|-----------|---------------|------------|------------|------|
| **成员变量** | 类中、方法外 | 有默认值 | 堆内存 | 与对象共存 | 包括实例变量和静态变量 |
| **实例变量** | 类中、方法外、无 static 修饰 | 有默认值 | 堆内存 | 对象创建→销毁 | 每个对象独立一份 |
| **静态变量（类变量）** | 类中、用 static 修饰 | 有默认值 | 方法区（静态区） | 类加载→卸载 | 所有对象共享一份 |
| **局部变量** | 方法或语句块中 | 无默认值 | 栈内存 | 方法调用→结束 | 必须先初始化后使用 |

> 注意：**静态变量** 通常用 `类名.变量名` 调用，也可用对象调用但不推荐。  

---

## static 与 final

### static 的作用

`static` 表示“静态”，用于修饰变量、方法、代码块、内部类。

#### 静态变量（类变量）

- 所有对象共享同一份内存。
- 生命周期与类相同。
- 可通过类名直接访问。

```java
public class Hero {
    private String name;           // 成员变量
    public static String game;     // 静态变量

    public static void main(String[] args) {
        Hero.game = "王者荣耀";
        System.out.println(Hero.game);
    }
}
```

#### 静态方法

- 不依赖实例即可调用。
- **只能访问静态成员**。
- 不存在 `this`。

```java
public class Util {
    private Util() {}

    public static void attack() {
        System.out.println("攻击！");
    }
}

public class Main {
    public static void main(String[] args) {
        Util.attack();  // 推荐方式
    }
}
```

#### 静态代码块

- 在类加载时执行一次。
- 一般用于静态数据初始化。

```java
public class Main {
    static String password;
    static {
        password = "123456";
    }
}
```

---

### final 的作用

`final` 表示“最终”，可修饰变量、方法、类。

#### 修饰变量

- 表示常量，只能赋值一次。  
- 命名规范：全大写，多个单词用 `_` 连接。  

```java
final int SIZE = 5;     // 常量
final String NAME = "Cunyu"; // 常量
```

> 如果修饰引用类型：引用地址不可变，但对象内容可变。

#### 修饰方法

- 该方法不能被子类重写。

```java
public class Person {
    public final void walk() {
        System.out.println("行走");
    }
}
```

#### 修饰类

- 该类不能被继承。

```java
public final class Person { }
```

---

## 抽象类（abstract）

### 抽象方法

抽象方法没有方法体，只定义方法格式：

```java
public abstract void welcome(String name);
```

### 抽象类的定义

- 含有抽象方法的类必须声明为 `abstract`。  
- 抽象类不能实例化。  
- 抽象类中可以包含普通方法与构造器。  

```java
public abstract class Person {
    public abstract void speak();
    public void eat() {
        System.out.println("吃饭");
    }
}
```

子类必须重写父类的抽象方法：

```java
public class Student extends Person {
    @Override
    public void speak() {
        System.out.println("学生在发言");
    }
}
```

---

## 接口（interface）

### 接口定义

```java
[public] interface 接口名 [extends  父接口名列表]  //接口声明
{                                  //接口体开始
    //常量数据成员的声明及定义
    数据类型   常量名=常数值;
    ……………
    //声明抽象方法
    返回值类型  方法名([参数列表]) [throw 异常列表] ;
    …………………
}//接口体结束
```

对接口定义说明如下：

1. 接口的访问限定只有public和缺省的。
2. interface是声明接口的关键字，与class类似。
3. 接口的命名必须符合标识符的规定，并且接口名必须与文件名相同。
4. 允许接口的多重继承，通过“extends 父接口名列表”可以继承多个接口。
5. 对接口体中定义的常量，系统默认为是“static final”修饰的，不需要指定。
6. 对接口体中声明的方法，系统默认为是“abstract”的，也不需要指定；对于一些特殊用途的接口，在处理过程中会遇到某些异常，可以在声明方法时加上“throw 异常列表”，以便捕捉出现在异常列表中的异常。有关异常的概念将在后边的章节讨论。

### 接口特性

| 成员类型 | 默认修饰符 | 说明 |
|-----------|--------------|------|
| 变量 | public static final | 常量 |
| 方法（JDK7 之前） | public abstract | 抽象方法 |
| 默认方法（JDK8+） | public default | 可带方法体 |
| 静态方法（JDK8+） | public static | 只能接口名调用 |
| 私有方法（JDK9+） | private / private static | 服务于默认或静态方法 |

### 实现接口

```java
public class Main implements Inter {
    @Override
    public void method1() {
        System.out.println("实现接口方法");
    }
}
```

### 多接口实现冲突

若实现多个接口且默认方法同名，必须重写：

```java
public class Main implements InterOne, InterTwo {
    @Override
    public void method2() {
        System.out.println("重写冲突方法");
    }
}
```

### 静态与私有方法示例

```java
public interface Inter {
    static void method() {
        System.out.println("接口静态方法");
    }
}
public class Main {
    public static void main(String[] args) {
        Inter.method();
    }
}
```

```java
public interface Inter {
    private void helper() {
        System.out.println("私有方法");
    }
    default void show() {
        helper();
    }
}
```

---

## 六、类与接口关系总结

| 关系 | 关键字 | 特性 |
|------|----------|------|
| 类 ↔ 类 | extends | 只能单继承 |
| 类 ↔ 接口 | implements | 可多实现 |
| 接口 ↔ 接口 | extends | 可多继承 |

---

## 内部类（Inner Class）

### 内部类的定义

内部类是定义在类中的类。

```java
public class Outer {
    public class Inner { }
}
```

### 分类与特点

| 类型 | 关键字 | 特点 |
|------|--------|------|
| 成员内部类 | 无 | 属于外部类实例，可访问外部类所有成员 |
| 静态内部类 | static | 只能访问外部类静态成员 |
| 局部内部类 | 定义在方法中 | 仅在方法内使用 |
| 匿名内部类 | new 父类/接口(){...} | 无类名、仅使用一次 |

### 示例代码

#### 成员内部类

```java
public class Outer {
    private String name = "外部类";
    public class Inner {
        public void show() {
            System.out.println("访问：" + name);
        }
    }
}
Outer.Inner inner = new Outer().new Inner();
inner.show();
```

#### 静态内部类

```java
public class Outer {
    static String birth = "2022";
    static class Inner {
        static void show() { System.out.println("静态内部类：" + birth); }
    }
}
Outer.Inner.show();
```

#### 局部内部类

```java
public class Outer {
    public void show() {
        class Inner {
            void print() {
                System.out.println("局部内部类方法");
            }
        }
        new Inner().print();
    }
}
```

#### 匿名内部类

```java
interface Inter {
    void run();
}
public class Main {
    public static void main(String[] args) {
        new Inter() {
            @Override
            public void run() {
                System.out.println("匿名内部类执行");
            }
        }.run();
    }
}
```

---

## 八、总结

- **变量层面**：区分实例变量、静态变量、局部变量的作用范围与生命周期。  
- **修饰符层面**：`static` 实现共享、`final` 实现不可变。  
- **抽象与接口层面**：抽象定义规范、接口定义契约。  
- **内部类层面**：灵活嵌套、封装逻辑。  

> 掌握这些基础，是深入理解 Java 面向对象思想的关键一步。
