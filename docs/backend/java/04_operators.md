# 第4章 Java 运算符

## 前言

在上一节中，我们介绍了 Java 的变量和数据类型。本节将学习如何对它们进行运算，这就涉及到 **运算符（Operator）**。

运算符就是 Java 中用于对变量或者字面量进行操作的符号。  

本文主要内容包括：

- 算术运算符  
- 关系运算符  
- 位运算符  
- 逻辑运算符  
- 赋值运算符  
- 条件运算符（三元运算符）  
- `instanceof`  
- 运算符优先级  
- `equals()` 和 `==` 的区别  

---

## 算术运算符

| 操作符 | 描述 |
|--------|------|
| +      | 加法：相加运算符两侧的值 |
| -      | 减法：左操作数减去右操作数 |
| *      | 乘法：相乘操作符两侧的值 |
| /      | 除法：左操作数除以右操作数 |
| %      | 取余：左操作数除以右操作数的余数 |
| ++     | 自增：操作数的值加 1 |
| --     | 自减：操作数的值减 1 |

⚠️ 注意：`++` 和 `--` 可以放在操作数前后：

- **前置**：先自增/减，再赋值
- **后置**：先赋值，再自增/减

### 示例

```java
public class Main {
    public static void main(String[] args) {
        int num1 = 10, num2 = 20, num3 = 30, num4 = 40;

        System.out.println("num1 + num2 = " + (num1 + num2));
        System.out.println("num1 - num2 = " + (num1 - num2));
        System.out.println("num1 * num2 = " + (num1 * num2));
        System.out.println("num2 / num1 = " + (num2 / num1));
        System.out.println("num2 % num1 = " + (num2 % num1));
        System.out.println("num3 % num1 = " + (num3 % num1));
        System.out.println("num1++ = " + (num1++));//num1++ = 10
        System.out.println("num1-- = " + (num2--));//num2-- = 20
        System.out.println("num4++ = " + (num4++));//++num3 = 31
        System.out.println("++num4 = " + (++num4));//--num4 = 39
    }
}
```

这里不难看出，无论是 ++ 还是 --，当它们单独写一行时，不管是放在变量前边还是后边，其最终结果都是一样的。但如果将它们参与运算，此时的效果就不一样了，这里需要注意。

```java
int a = 10;
int b = a++;
```

以上代码中，先进行了 b = a 的赋值操作，所以此时 b 的值是 10。

```java
int a = 10;
int b = ++a;
```

而此时，先要对 a 进行加一的操作之后，再将 a 的值赋予 b，所以此时 b 的值为 11。

---

## 关系运算符

关系运算符主要是指两个数据间的关系，两者之间的比较结果用逻辑值来表示，常用来比较判断两个变量或常量的大小。

常见的关系运算符及含义如下表：

| 运算符 | 描述 |
|--------|------|
| ==     | 判断是否相等 |
| !=     | 判断是否不相等 |
| >      | 左操作数是否大于右操作数 |
| <      | 左操作数是否小于右操作数 |
| >=     | 左操作数是否大于或等于右操作数 |
| <=     | 左操作数是否小于或等于右操作数 |

### 示例2

```java
public class Main {
    public static void main(String[] args) {
        int num1 = 100, num2 = 220;

        System.out.println("num1 == num2 = " + (num1 == num2));//false
        System.out.println("num1 != num2 = " + (num1 != num2));//true
        System.out.println("num1 > num2 = " + (num1 > num2));//false
        System.out.println("num2 < num1 = " + (num2 < num1));//false
        System.out.println("num2 <= num1 = " + (num2 <= num1));//false
        System.out.println("num2 >= num1 = " + (num2 >= num1));//true
    }
}
```

---

## 位运算符

| 操作符 | 描述 |
|--------|------|
| &      | 按位与：都为 1 结果为 1，否则为 0 |
| \|     | 按位或：都为 0 结果为 0，否则为 1 |
| ^      | 按位异或：相同为 0，不同为 1 |
| ~      | 按位取反：0 变 1，1 变 0 |
| <<     | 左移：按位左移 |
| >>     | 右移：按位右移 |
| >>>    | 无符号右移：高位补 0 |

### 示例3

```java
public class Main {
    public static void main(String[] args) {
        int num1 = 10, num2 = 20;

        System.out.println("num1 & num2 = " + (num1 & num2));
        System.out.println("num1 | num2 = " + (num1 | num2));
        System.out.println("num1 ^ num2 = " + (num1 ^ num2));
        System.out.println("~ num2 = " + (~num2));
        System.out.println("num1 << 2 = " + (num1 << 2));
        System.out.println("num1 >> 2 = " + (num1 >> 2));
        System.out.println("num1 >>> 2 = " + (num1 >>> 2));
    }
}
```

---

## 逻辑运算符

| 操作符 | 描述 |
|--------|------|
| &&     | 逻辑与：都为 true 时为 true |
| \|\|   | 逻辑或：有一个为 true 时为 true |
| !      | 逻辑非：取反 |

### 示例4

```java
public class Main {
    public static void main(String[] args) {
        boolean positive = true, negative = false;

        System.out.println("positive && negative = " + (positive && negative));//false
        System.out.println("positive || negative = " + (positive || negative));//true
        System.out.println("!(positive || negative) = " + !(positive || negative));//false
    }
}
```

---

## 赋值运算符

| 操作符 | 描述 |
|--------|------|
| =      | 赋值 |
| +=     | 加后赋值 |
| -=     | 减后赋值 |
| *=     | 乘后赋值 |
| /=     | 除后赋值 |
| %=     | 取模后赋值 |
| <<=    | 左移后赋值 |
| >>=    | 右移后赋值 |
| &=     | 按位与后赋值 |
| ^=     | 按位异或后赋值 |
| \|=    | 按位或后赋值 |

### 示例5

```java
public class Main {
    public static void main(String[] args) {
        int num1 = 100, num2 = 1000;

        System.out.println("num1 += num2 = " + (num1 += num2));
        System.out.println("num1 -= num2 = " + (num1 -= num2));
        System.out.println("num1 *= num2 = " + (num1 *= num2));
        System.out.println("num1 &= num2 = " + (num1 /= num2));
        System.out.println("num1 &= num2 = " + (num1 %= num2));
    }
}
```

---

## 条件运算符（三元运算符）

也叫作三元运算符，共有 3 个操作数，且需要判断布尔表达式的值，常用来取代某个 if-else 语句。

其语法结构如下所示：

```java
关系表达式 ? 表达式 1 : 表达式 2;
```

### 示例6

```java
public class Main {
    public static void main(String[] args) {
        int num1 = 30, num2 = 300;

        int result = num1 > num2 ? num1 : num2;
        System.out.println("The max is " + result);
    }
}
```

---

## instanceof 运算符

用于判断对象是否为某个类或接口的实例。结果为true或者false。

语法：

```java
object instanceof ClassName 
```

---

## 运算符优先级（由高到低）

| 优先级 | 运算符 |
|--------|--------|
| 1      | .、()、{} |
| 2      | !、~、++、-- |
| 3      | *、/、% |
| 4      | +、- |
| 5      | <<、>>、>>> |
| 6      | <、<=、>、>=、instanceof |
| 7      | ==、!= |
| 8      | & |
| 9      | ^ |
| 10     | \| |
| 11     | && |
| 12     | \|\| |
| 13     | ?: |
| 14     | =、+=、-=、*=、/=、%=、&=、^=、\|= |

💡 建议使用 `()` 明确运算顺序，避免歧义。  

---

## equals() 和 == 的区别

- **==**
    - 基本类型：比较值是否相等
    - 引用类型：比较内存地址是否相同（是否为同一对象）

- **equals()**
  
  作用也是判断两个对象是否相等，但是 不能用于基本数据类型变量的比较。存在于 Object() 类中，所以所有类都具有 equals() 方法存在两种使用情况：

    - 默认在 `Object` 中与 `==` 相同
    - 如果类重写了 `equals()`，则比较对象内容是否相等（如 `String`）

### 示例7

```java
public class Main {
    public static void main(String[] args) {
        int num1 = 10, num2 = 10, num3 = 20;
        String str1 = "安信株式会社";
        String str2 = new String("安信株式会社");

        System.out.println(num1 == num2); // true
        System.out.println(num2 == num3); // false
        System.out.println(str1 == str2); // false (不同对象)
        System.out.println(str1.equals(str2)); // true (内容相同)
    }
}
```

---

## 总结

本章介绍了 Java 的各种运算符，包括：

- 算术运算符  
- 关系运算符  
- 位运算符  
- 逻辑运算符  
- 赋值运算符  
- 条件运算符  
- `instanceof`  
- 运算符优先级  
- `equals()` 和 `==` 的区别  

这些运算符是 Java 程序设计的基础，必须熟练掌握。
