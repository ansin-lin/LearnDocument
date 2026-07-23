# 7. 数组

## 前言

前边已经讲过了 Java 中的 8 大基本数据类型，这篇文章主要讲解引用类型中的 **数组**。  

主要内容：

- 数组简介
- 遍历  
- 排序  
- 常用方法  

---

## 数组简介

数组是多个相同数据类型的元素按一定顺序排列而成的集合。

- 索引位置从 **0** 开始计数。  
- 一经创建，大小不可改变。  
- 数组元素有默认值：整型是 0，浮点型是 0.0，布尔型是 false。  

### 初始化方式

#### 静态初始化

```java
int[] arr1 = {1, 3, 5, 8, 10};
int[] arr2 = new int[]{1, 3, 5, 8, 10};
```

#### 动态初始化

```java
int[] arr = new int[5];
arr[0] = 1;
arr[1] = 3;
arr[2] = 5;
arr[3] = 8;
arr[4] = 10;
```

### 求数组长度

```java
int[] arr = new int[10];
int size = arr.length; // 10
```

---

## 二维数组

### 初始化

#### 静态初始化方法

```java
int[][] arr1 = {{1, 2, 4}, {5, 7, 9}, {19, 12, 18}};
```

#### 动态初始化方法

```java
int[][] arr1 = new int[3][3];
arr1 = new int[][]{{1, 2, 4}, {5, 7, 9}, {19, 12, 18}};

int[][] arr2 = new int[3][];
arr2 = new int[][]{{1, 2, 4}, {5, 7, 9}, {19, 12, 18}};
```

### 求二维数组长度

```java
int[][] arr = new int[10][20];
int row = arr.length;     // 行数
int col = arr[0].length;  // 列数
```

---

## 数组遍历

### 标准 for 循环

```java
public class TraverseTest {
    public static void main(String[] args) {
        String[] arr = {"安信株式会社", "海贼王", "进击的巨人", "鬼灭之刃", "斗罗大陆"};
        int size = arr.length;
        for (int i = 0; i < size; i++) {
            System.out.println("第 " + (i + 1) + " 个元素：" + arr[i]);
        }
    }
}
```

### 增强 for 循环

```java
public class EnforceTraverseTest {
    public static void main(String[] args) {
        String[] arr = {"安信株式会社", "海贼王", "进击的巨人", "鬼灭之刃", "斗罗大陆"};
        int index = 0;
        for (String name : arr) {
            System.out.println("第 " + (index + 1) + " 个元素：" + name);
            index++;
        }
    }
}
```

### 标准库遍历

```java
import java.util.Arrays;

public class StandardLibraryTest {
    public static void main(String[] args) {
        String[] arr = {"安信株式会社", "海贼王", "进击的巨人", "鬼灭之刃", "斗罗大陆"};
        System.out.println(Arrays.toString(arr));
    }
}
```

⚠️ 对于二维数组，可以使用 `Arrays.deepToString()`。

---

## 数组排序

### 冒泡排序

```java
import java.util.Arrays;

public class BubbleSort {
    public static void main(String[] args) {
        char[] chArray = {'c', 'u', 'n', 'y', 'u'};
        System.out.println(Arrays.toString(chArray)); // 排序前

        for (int i = 0; i < chArray.length - 1; i++) {
            for (int j = 0; j < chArray.length - 1 - i; j++) {
                if (chArray[j] < chArray[j + 1]) {
                    char temp = chArray[j];
                    chArray[j] = chArray[j + 1];
                    chArray[j + 1] = temp;
                }
            }
        }
        System.out.println(Arrays.toString(chArray)); // 排序后
    }
}
```

### 标准库排序

```java
import java.util.Arrays;

public class StandardLibrarySortTest {
    public static void main(String[] args) {
        String[] arr = {"安信株式会社", "海贼王", "进击的巨人", "鬼灭之刃", "斗罗大陆"};
        System.out.println(Arrays.toString(arr)); // 排序前
        Arrays.sort(arr);
        System.out.println(Arrays.toString(arr)); // 排序后
    }
}
```

---

## 常用方法

Java `Arrays` 工具类提供了丰富的方法：

- public static String toString(Object[] a)：将数组转换为字符串形式输出。
- public static <T> List<T> asList(T... a)：将数组转换为 List（固定长度）。
- public static boolean equals(Object[] a, Object[] b)：判断两个数组是否内容相同。
- public static boolean deepEquals(Object[] a1, Object[] a2)：深度比较多维数组是否相等。
- public static void sort(int[] a)：对整型数组进行升序排序。
- public static int binarySearch(int[] a, int key)：使用二分法查找指定元素的索引（需已排序）。
- public static void fill(int[] a, int val)：将数组中所有元素设置为指定值。
- public static <T> void fill(T[] a, T val)：将对象数组填充为指定值。
- public static <T> T[] copyOf(T[] original, int newLength)：复制数组到指定长度的新数组。
- public static <T> T[] copyOfRange(T[] original, int from, int to)：复制数组的指定范围。
- public static <T> boolean mismatch(T[] a, T[] b)【JDK 9】：返回第一个不同元素的索引。
- public static <T> T[] copyOfRange(T[] original, int from, int to, Class<? extends T[]> newType)【JDK 1.6】：复制指定范围并指定类型的新数组。

### 示例

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonMethodsTest {
    public static void main(String[] args) {
        String[] arr = {"安信株式会社", "海贼王", "进击的巨人", "鬼灭之刃", "斗罗大陆"};

        System.out.println(Arrays.toString(arr)); // 数组转字符串

        List<String> list = new ArrayList<>(Arrays.asList(arr));
        System.out.println(list); // 数组转 List

        list.add("镇魂街");
        String[] newArr = new String[list.size()];
        list.toArray(newArr);
        System.out.println(Arrays.toString(newArr)); // List 转数组

        System.out.println(Arrays.asList(newArr).contains("进击的巨人")); // true
        System.out.println(Arrays.asList(newArr).contains("网球王子")); // false
    }
}
```

---

## 总结

数组相关内容总结：  

- 一维、二维数组初始化方式（静态、动态）  
- 数组的长度和默认值  
- 三种常见遍历方式（for、增强 for、标准库）  
- 排序方法（冒泡排序、`Arrays.sort`）  
- 常用库方法（`toString`、`asList` 等）  

数组是 Java 编程中最常见的数据结构之一，必须熟练掌握。
