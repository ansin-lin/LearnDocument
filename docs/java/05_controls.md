# Java 流程控制

## 前言

在上一节我们学习了各种运算符的使用技巧，本节将介绍 **流程控制**，主要内容包括：

- 输入输出  
- 顺序结构  
- 分支结构  
- 循环结构  

---

## 输入输出

### 输入（Scanner 类）

要从控制台读取数据，需要使用 `Scanner` 类。  

步骤：

1. 导入类：`import java.util.Scanner;`  
2. 创建对象：`Scanner scanner = new Scanner(System.in);`  
3. 调用方法读取输入。  

常用方法：

| 返回值 | 方法名       | 描述 |
|--------|--------------|------|
| boolean| hasNext()    | 如果还有输入则返回 true |
| String | next()       | 返回下一个字符串（遇空格结束） |
| String | nextLine()   | 返回一行内容（可包含空格） |
| int    | nextInt()    | 输入整型 |
| long   | nextLong()   | 输入长整型 |
| float  | nextFloat()  | 输入浮点型 |
| double | nextDouble() | 输入双精度浮点数 |

⚠️ 注意 `next()` 和 `nextLine()` 的区别：  

- `next()` 遇到空格、Tab、换行会停止读取；  
- `nextLine()` 会读取整行内容，包括空格。  

#### 示例

```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入整型");
        int num = scanner.nextInt();
        System.out.println("输入的整型：" + num);

        System.out.println("输入字符串");
        String name = scanner.next();
        System.out.println("输入的字符串：" + name);

        System.out.println("输入浮点型");
        float f = scanner.nextFloat();
        System.out.println("输入的浮点型：" + f);
    }
}
```

---

### 输出（System.out）

常用方法：

- `System.out.print()`：输出但不换行  
- `System.out.println()`：输出并换行  
- `System.out.printf()` / `System.out.format()`：格式化输出  

常用占位符：

| 占位符 | 描述 |
|--------|------|
| %d     | 整数 |
| %f     | 浮点数 |
| %s     | 字符串 |
| %x     | 十六进制整数 |
| %e     | 科学计数法浮点数 |

常见转义字符：

| 转义字符 | 描述 |
|----------|------|
| `\n`       | 换行 |
| `\t`       | 制表符 |
| `\\`     | 反斜杠 |
| `\'`      | 单引号 |
| `\"`     | 双引号 |

#### 示例2

```java
public class Main {
    public static void main(String[] args) {
        int num1 = 10;
        double num2 = 34.9;
        String name = "村雨遥";

        System.out.println("公众号：" + name);
        System.out.print("num1 = " + num1 + "\n");

        System.out.printf("num1 = %d\n", num1);
        System.out.format("num2 = %f\n", num2);
        System.out.printf("name = %s\n", name);
    }
}
```

---

## 顺序结构

**顺序结构** 是程序默认的执行方式：代码从上到下依次执行。  

---

## 分支结构

### if 语句（适合范围判断）

#### 单分支(单次判断)

当我们只进行一次判断时，可以使用一个 if 语句包含一个条件表达式，其语法格式如下；

```java
if (条件表达式) {
    执行语句;
}
```

如果条件表达式的值为 true，则执行 if 语句块中的执行语句，否则就执行 if 语句块后边的代码；

#### 多分支(多次判断)

要进行多次判断时，可以使用 if…else 的形式，其语法格式如下；

```java
if (条件1) {
    执行语句1;
} else if (条件2) {
    执行语句2;
} else {
    执行语句N;
}
```

如果条件表达式 1 为 true，则执行执行语句 1，否则接着判断条件表达式 2，若为 true，则执行执行语句 2，以此类推，直到完成最后一个条件表达式的判断。

#### 示例3

```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入分数：");
        double score = scanner.nextDouble();

        if (score < 0 || score > 100) {
            System.out.println("输入的分数不合法");
        } else if (score >= 90) {
            System.out.println("A");
        } else if (score >= 80) {
            System.out.println("B");
        } else if (score >= 60) {
            System.out.println("C");
        } else {
            System.out.println("D");
        }
    }
}
```

---

### switch 语句（适合离散值选择）

语法：

```java
switch (表达式) {
    case 值1:
        执行语句1;
        break;
    case 值2:
        执行语句2;
        break;
    default:
        执行语句;
}
```

通过判断表达式的值，然后执行对应值下的执行语句，而 default 下的执行语句表示如果 switch 表达式未匹配到对应的值时所执行的语句；

#### 示例4

```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("请输入周几：");
        int day = input.nextInt();

        switch (day) {
            case 1:
                System.out.println("星期一");
                break;
            case 2:
                System.out.println("星期二");
                break;
            case 3:
                System.out.println("星期三");
                break;
            case 4:
                System.out.println("星期四");
                break;
            case 5:
                System.out.println("星期五");
                break;
            case 6:
            case 7:
                System.out.println("周末");
                break;
            default:
                System.out.println("未知日期");
        }
    }
}
```

而在使用 switch 分支语法时，需要遵循一定的规则：

1. switch 中的变量类型可以是：byte、short、int、char、String （自 JDK 1.7 开始）；
2. switch 语句根据表达式的结果跳转到对应的 case 结果，然后执行其后跟着的语句，直到遇到 break 才结束执行；
3. 默认情况下，一般都会跟着一个 default 的分支，用于未匹配到对应情况时的执行情况；

---

## 循环结构

### while 循环

假设我们现在有一个题目，需要你计算 1 + 2 + 3 + …… + 50 的结果，你会怎么办呢？

这么写么：

```java
public class Main {
    public static void main(String[] args) {
        int sum = 1 + 2;
        sum += 3;
        sum += 4;
        ……
        sum += 50;
        System.out.println("1 + 2 + 3 + …… + 50 = " + sum);
    }
}
```

#### 示例5

```java
public class Main {
    public static void main(String[] args) {
        int sum = 0, num = 1;
        while (num <= 50) {
            sum += num;
            num++;
        }
        System.out.println("1+2+…+50 = " + sum);
    }
}
```

从上面的实例，利用 while 循环，我们就能轻易达成循环的效果。其语法格式如下：
语法：

```java
while (条件表达式) {
    执行语句;
}
```

只要表达式为 true，就会不断循环执行其中的执行语句，直到表达式为 false，此时便跳出循环，不再执行其中的执行语句。

---

### do…while 循环

语法：

```java
do {
    执行语句;
} while (条件表达式);
```

两者的最大区别在于：do……while 无论 表达式 是否为 true，都至少会执行一次。

#### 示例7

```java
public class Main {
    public static void main(String[] args) {
        int num = 10, sum = 0;
        do {
            sum += num;
            num++;
        } while (num < 10);
        System.out.println("sum = " + sum);//10

        int num1 = 10, sum1 = 0;
        while (num1 < 10){
            sum1 += num1;
            num1++;
        } ;
        System.out.println("sum = " + sum1);//0
    }
}
```

因此，当我们需要在 while 和 do……while 之间做出选择时，如果我们最少需要进行一次循环，则选择 do……while，其他情况下选用两者都可以。

---

### for 循环

#### 普通 for 循环

除开 while 和 do……while 之外，我们还有 for 循环来达成同样的结果，只是表达方法不一样。语法如下所述：

```java
for (初始化; 条件; 更新) {
    执行语句;
}
```

同样以上面计算 1 + 2 + …… + 50 为例，可以写成如下的形式：

#### 示例8

```java
public class Main {
    public static void main(String[] args) {
        int sum = 0;
        for (int i = 1; i <= 50; i++) {
            sum += i;
        }
        System.out.println("1+2+…+50 = " + sum);
    }
}
```

for 循环的执行步骤如下：

1. 首先执行初始条件，可以声明一种类型，但可以初始化一个或多个循环控制变量，甚至可以放空。
2. 接着判断终止条件，如果为 true，则进入循环体执行循环语句；如果为 false，则终止循环，执行循环体后面的语句。
3. 一次循环完成后，执行更新语句来更新循环控制变量。
4. 最后再次判断终止条件，循环以上三个步骤。
for 和 while 最大的区别就在于 for 循环一般都是事先知道需要循环的次数的，而 while 循环则不需要。

---

#### 增强 for 循环

自 Java 5 后，引入了一种增强型 for 循环，主要用于数组遍历，其语法格式如下：

语法：

```java
for (类型 变量 : 数组或集合) {
    执行语句;
}
```

#### 示例9

```java
public class Main {
    public static void main(String[] args) {
        int[] numbers = {1, 4, 5, 6, 9, 10};
        for (int num : numbers) {
            System.out.print(num + "\t");
        }
    }
}
```

---

## break 和 continue

### break

主要用在循环语句或者 switch 语句中，表示**跳出此循环，然后继续执行该循环下的语句**。

```java
for (int i = 1; i < 10; i++) {
    if (i == 5) break;
    System.out.println("i = " + i);
}
```

当 i == 5 时，我们执行了 break 语句，此时就直接跳出了 for 循环，而不再进行下一次的循环。

### continue

continue 也同样是应用在循环控制结构中，主要是让程序**跳出当次循环，进而进入下一次循环的迭代**。

在 for 循环中，执行 continue 语句后，直接跳转到更新语句，而不再执行 continue 后的语句。而在 while 或 do……while 循环中，执行 continue 语句后，直接跳转到表达式的判断。

```java
for (int i = 1; i < 10; i++) {
    if (i == 5) continue;
    System.out.println("i = " + i);
}
```

---

## 总结

本章介绍了 Java 流程控制的四大部分：

- 输入输出  
- 顺序结构  
- 分支结构（if、switch）  
- 循环结构（while、do…while、for、增强 for）  
- 跳转语句（break、continue）  

这些是构建 Java 程序逻辑的基本语法结构，必须熟练掌握。
