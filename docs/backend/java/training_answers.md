# Java 语法与逻辑综合练习100题参考答案

本文件对应 `training.md`。第1～50题是原有练习答案，第51～100题是新增练习答案。以下实现用于完成练习后的核对，不是唯一写法。

除特别给出完整类的题目外，代码块是对应 `ExerciseXX` 类中的核心方法；请自行在 `main` 方法中准备题目示例和边界数据进行调用。每个代码块相互独立，需要的导入已写在代码块内或正文中说明。

## 原有练习参考答案

### 1. 斐波那契数列——兔子繁殖问题

```java
static long[] rabbits(int n) {
    long[] result = new long[n];
    if (n > 0) result[0] = 1;
    if (n > 1) result[1] = 1;
    for (int i = 2; i < n; i++) result[i] = result[i - 1] + result[i - 2];
    return result;
}
```

### 2. 指定范围内的素数

```java
static boolean isPrime(int n) {
    if (n < 2) return false;
    for (int i = 2; i <= n / i; i++) if (n % i == 0) return false;
    return true;
}

static void printPrimes101To200() {
    int count = 0;
    for (int n = 101; n <= 200; n++) {
        if (isPrime(n)) { System.out.print(n + " "); count++; }
    }
    System.out.println("\n数量=" + count);
}
```

### 3. 水仙花数

```java
for (int n = 100; n <= 999; n++) {
    int a = n / 100, b = n / 10 % 10, c = n % 10;
    if (a * a * a + b * b * b + c * c * c == n) System.out.println(n);
}
```

### 4. 分解质因数

```java
static String factorize(int number) {
    int original = number;
    StringBuilder result = new StringBuilder(original + " = ");
    for (int factor = 2; factor <= number / factor; factor++) {
        while (number % factor == 0) {
            if (result.charAt(result.length() - 1) != ' ') result.append(" × ");
            result.append(factor);
            number /= factor;
        }
    }
    if (number > 1) {
        if (result.charAt(result.length() - 1) != ' ') result.append(" × ");
        result.append(number);
    }
    return result.toString();
}
```

### 5. 条件运算符

```java
String grade = score < 0 || score > 100 ? "成绩无效"
        : score >= 90 ? "A" : score >= 60 ? "B" : "C";
```

### 6. 最大公约数和最小公倍数

```java
static long gcd(long a, long b) {
    while (b != 0) { long r = a % b; a = b; b = r; }
    return a;
}
static long lcm(long a, long b) { return a / gcd(a, b) * b; }
```

### 7. 统计字符串中的字符类型

```java
static int[] countTypes(String text) {
    int[] count = new int[4];
    for (char ch : text.toCharArray()) {
        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) count[0]++;
        else if (ch >= '0' && ch <= '9') count[1]++;
        else if (ch == ' ') count[2]++;
        else count[3]++;
    }
    return count;
}
```

### 8. 求a + aa + aaa + ...的值

```java
static long seriesSum(int a, int n) {
    long term = 0, sum = 0;
    for (int i = 0; i < n; i++) { term = term * 10 + a; sum += term; }
    return sum;
}
```

### 9. 查找完数

```java
for (int n = 2; n < 1000; n++) {
    int sum = 1;
    for (int divisor = 2; divisor <= n / divisor; divisor++) {
        if (n % divisor == 0) {
            sum += divisor;
            if (divisor != n / divisor) sum += n / divisor;
        }
    }
    if (sum == n) System.out.println(n);
}
```

### 10. 小球反弹问题

```java
double height = 100.0;
double distance = 100.0;
for (int landing = 2; landing <= 10; landing++) {
    height /= 2.0;
    distance += height * 2.0;
}
double tenthBounce = height / 2.0;
```

### 11. 三位数排列组合

```java
int count = 0;
for (int a = 1; a <= 4; a++)
    for (int b = 1; b <= 4; b++)
        for (int c = 1; c <= 4; c++)
            if (a != b && a != c && b != c) {
                System.out.println(a * 100 + b * 10 + c); count++;
            }
```

### 12. 阶梯奖金计算

```java
static double bonus(double profit) {
    double[] limits = {10, 20, 40, 60, 100};
    double[] rates = {0.10, 0.075, 0.05, 0.03, 0.015, 0.01};
    double result = 0, previous = 0;
    for (int i = 0; i < limits.length; i++) {
        double part = Math.max(0, Math.min(profit, limits[i]) - previous);
        result += part * rates[i];
        previous = limits[i];
    }
    if (profit > 100) result += (profit - 100) * rates[5];
    return result;
}
```

### 13. 求满足条件的整数

```java
static boolean isSquare(int value) {
    int root = (int) Math.sqrt(value);
    return root * root == value;
}
for (int n = 0; n <= 100000; n++)
    if (isSquare(n + 100) && isSquare(n + 268)) System.out.println(n);
```

### 14. 日期计算

```java
import java.time.LocalDate;

int dayOfYear = LocalDate.of(year, month, day).getDayOfYear();
```

### 15. 三个整数排序

```java
if (x > y) { int t = x; x = y; y = t; }
if (x > z) { int t = x; x = z; z = t; }
if (y > z) { int t = y; y = z; z = t; }
```

### 16. 九九乘法表

```java
for (int row = 1; row <= 9; row++) {
    for (int column = 1; column <= row; column++)
        System.out.printf("%d×%d=%d\t", column, row, column * row);
    System.out.println();
}
```

### 17. 猴子吃桃——逆向计算

```java
int peaches = 1;
for (int day = 9; day >= 1; day--) peaches = (peaches + 1) * 2;
```

### 18. 乒乓球比赛配对

```java
String[] first = {"a", "b", "c"};
String[] second = {"x", "y", "z"};
for (String aOpponent : second)
    for (String bOpponent : second)
        for (String cOpponent : second)
            if (!aOpponent.equals(bOpponent) && !aOpponent.equals(cOpponent)
                    && !bOpponent.equals(cOpponent) && !aOpponent.equals("x")
                    && !cOpponent.equals("x") && !cOpponent.equals("z"))
                System.out.printf("a-%s, b-%s, c-%s%n", aOpponent, bOpponent, cOpponent);
```

### 19. 打印菱形

```java
for (int row = -3; row <= 3; row++) {
    int spaces = Math.abs(row), stars = 7 - spaces * 2;
    System.out.println(" ".repeat(spaces) + "*".repeat(stars));
}
```

### 20. 分数数列求和

```java
double numerator = 2, denominator = 1, sum = 0;
for (int i = 0; i < 20; i++) {
    sum += numerator / denominator;
    double next = numerator + denominator;
    denominator = numerator;
    numerator = next;
}
```

### 21. 阶乘求和

```java
long factorial = 1, sum = 0;
for (int i = 1; i <= 20; i++) { factorial *= i; sum += factorial; }
```

### 22. 递归计算阶乘

```java
static long factorial(int n) {
    if (n < 0) throw new IllegalArgumentException("n不能为负数");
    return n <= 1 ? 1 : n * factorial(n - 1);
}
```

### 23. 递归计算年龄

```java
static int age(int person) {
    if (person < 1) throw new IllegalArgumentException("编号必须为正数");
    return person == 1 ? 10 : age(person - 1) + 2;
}
```

### 24. 整数位数与倒序输出

```java
static void printDigits(int n) {
    System.out.println("位数=" + String.valueOf(n).length());
    while (n > 0) { System.out.print(n % 10 + " "); n /= 10; }
}
```

### 25. 判断回文数

```java
static boolean isPalindrome(int n) {
    int original = n, reversed = 0;
    while (n > 0) { reversed = reversed * 10 + n % 10; n /= 10; }
    return original == reversed;
}
```

### 26. 根据英文星期名称判断星期

```java
static String weekday(String input) {
    return switch (input.toLowerCase(java.util.Locale.ROOT)) {
        case "monday" -> "星期一"; case "tuesday" -> "星期二";
        case "wednesday" -> "星期三"; case "thursday" -> "星期四";
        case "friday" -> "星期五"; case "saturday" -> "星期六";
        case "sunday" -> "星期日"; default -> "无效星期";
    };
}
```

### 27. 求100以内的素数

复用第2题的 `isPrime()`，循环范围改为2～100。

### 28. 选择排序

```java
static void selectionSort(int[] values) {
    for (int i = 0; i < values.length - 1; i++) {
        int min = i;
        for (int j = i + 1; j < values.length; j++) if (values[j] < values[min]) min = j;
        int t = values[i]; values[i] = values[min]; values[min] = t;
    }
}
```

### 29. 3×3矩阵主对角线求和

```java
int sum = 0;
for (int i = 0; i < 3; i++) sum += matrix[i][i];
```

### 30. 有序数组插入

```java
static int[] insert(int[] source, int value) {
    int position = 0;
    while (position < source.length && source[position] <= value) position++;
    int[] result = new int[source.length + 1];
    System.arraycopy(source, 0, result, 0, position);
    result[position] = value;
    System.arraycopy(source, position, result, position + 1, source.length - position);
    return result;
}
```

### 31. 数组逆序

```java
for (int left = 0, right = values.length - 1; left < right; left++, right--) {
    int t = values[left]; values[left] = values[right]; values[right] = t;
}
```

### 32. 查找数组中第二大的不同数字

```java
static java.util.OptionalInt secondLargest(int[] values) {
    Integer max = null, second = null;
    for (int value : values) {
        if (max == null || value > max) { second = max; max = value; }
        else if (value < max && (second == null || value > second)) second = value;
    }
    return second == null ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(second);
}
```

### 33. 杨辉三角

```java
int[][] triangle = new int[10][];
for (int row = 0; row < triangle.length; row++) {
    triangle[row] = new int[row + 1];
    triangle[row][0] = triangle[row][row] = 1;
    for (int column = 1; column < row; column++)
        triangle[row][column] = triangle[row - 1][column - 1] + triangle[row - 1][column];
}
```

### 34. 三个数字排序——数组方式

对长度为3的数组复用第28题的 `selectionSort()`。

### 35. 数组最大值、最小值交换

```java
int maxIndex = 0;
for (int i = 1; i < values.length; i++) if (values[i] > values[maxIndex]) maxIndex = i;
int t = values[0]; values[0] = values[maxIndex]; values[maxIndex] = t;
int minIndex = 0;
for (int i = 1; i < values.length; i++) if (values[i] < values[minIndex]) minIndex = i;
t = values[values.length - 1]; values[values.length - 1] = values[minIndex]; values[minIndex] = t;
```

### 36. 数组循环右移

```java
static int[] rotateRight(int[] values, int m) {
    int[] result = new int[values.length];
    int shift = m % values.length;
    for (int i = 0; i < values.length; i++) result[(i + shift) % values.length] = values[i];
    return result;
}
```

### 37. 约瑟夫环问题

```java
static int josephus(int n) {
    int survivor = 0;
    for (int size = 2; size <= n; size++) survivor = (survivor + 3) % size;
    return survivor + 1;
}
```

### 38. 计算字符串长度

```java
int count = 0;
for (char ignored : text.toCharArray()) count++;
```

### 39. 奇偶不同规则的分数求和

```java
static double fractionSum(int n) {
    int start = n % 2 == 0 ? 2 : 1;
    double sum = 0;
    for (int denominator = start; denominator <= n; denominator += 2)
        sum += 1.0 / denominator;
    return sum;
}
```

### 40. 字符串排序

```java
for (int i = 0; i < values.length - 1; i++)
    for (int j = i + 1; j < values.length; j++)
        if (values[i].compareTo(values[j]) > 0) {
            String t = values[i]; values[i] = values[j]; values[j] = t;
        }
```

### 41. 五只猴子分桃

```java
int start = 1;
while (true) {
    int peaches = start, rounds = 0;
    while (rounds < 5 && peaches % 5 == 1) {
        peaches = (peaches - 1) / 5 * 4;
        rounds++;
    }
    if (rounds == 5) break;
    start++;
}
```

### 42. 条件枚举

```java
for (int x = 10; x <= 99; x++)
    if (8 * x >= 10 && 8 * x <= 99 && 9 * x >= 100 && 9 * x <= 999)
        System.out.printf("x=%d, 809×x=%d%n", x, 809 * x);
```

### 43. 使用0～7组成三位奇数

```java
int count = 0;
for (int a = 1; a <= 7; a++)
    for (int b = 0; b <= 7; b++)
        for (int c = 1; c <= 7; c += 2)
            if (a != b && a != c && b != c) { System.out.println(a * 100 + b * 10 + c); count++; }
```

### 44. 偶数分解为两个奇数

```java
for (int a = 1; a <= n - a; a += 2) {
    int b = n - a;
    if (b % 2 == 1) System.out.printf("%d + %d = %d%n", a, b, n);
}
```

### 45. 连续被9整除

```java
int count = 0;
while (n % 9 == 0) { n /= 9; count++; }
```

### 46. 字符串连接

```java
String result = new StringBuilder(first).append(second).toString();
```

### 47. 打印星号

```java
for (int value : values) System.out.println("*".repeat(value));
```

交互输入时使用循环，范围不在1～50就继续读取当前项。

### 48. 四位数字加密

```java
static String encrypt(int number) {
    int[] digits = {number / 1000, number / 100 % 10, number / 10 % 10, number % 10};
    for (int i = 0; i < digits.length; i++) digits[i] = (digits[i] + 5) % 10;
    int t = digits[0]; digits[0] = digits[3]; digits[3] = t;
    t = digits[1]; digits[1] = digits[2]; digits[2] = t;
    return String.format("%d%d%d%d", digits[0], digits[1], digits[2], digits[3]);
}
```

### 49. 统计子串出现次数

```java
static int count(String text, String target) {
    if (target.isEmpty()) throw new IllegalArgumentException("子串不能为空");
    int count = 0;
    for (int i = 0; i <= text.length() - target.length(); i++)
        if (text.startsWith(target, i)) count++;
    return count;
}
```

### 50. 学生成绩平均值

```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class Student {
    final String id, name;
    final int score1, score2, score3;
    Student(String id, String name, int score1, int score2, int score3) {
        this.id = id; this.name = name;
        this.score1 = score1; this.score2 = score2; this.score3 = score3;
    }
    double average() { return (score1 + score2 + score3) / 3.0; }
    String toLine() {
        return String.format("%s,%s,%d,%d,%d,%.2f", id, name, score1, score2, score3, average());
    }
}

static void writeStudents(List<Student> students) throws IOException {
    List<String> lines = new ArrayList<>();
    for (Student student : students) lines.add(student.toLine());
    Files.write(Path.of("stud.txt"), lines, StandardCharsets.UTF_8);
}
```

## 新增练习参考答案

## 第一阶段参考答案

### 51. 温度转换

```java
static double toFahrenheit(double celsius) {
    return celsius * 9.0 / 5.0 + 32.0;
}

// 输出时：System.out.printf("%.1f%n", toFahrenheit(25));
```

### 52. 分段电费

```java
static double calculateFee(int usage) {
    if (usage < 0) {
        throw new IllegalArgumentException("用电量不能为负数");
    }
    if (usage <= 100) {
        return usage * 0.50;
    }
    if (usage <= 200) {
        return 100 * 0.50 + (usage - 100) * 0.60;
    }
    return 100 * 0.50 + 100 * 0.60 + (usage - 200) * 0.80;
}
```

### 53. 闰年判断

```java
static boolean isLeapYear(int year) {
    return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0);
}
```

### 54. 成绩等级

```java
static String grade(int score) {
    if (score < 0 || score > 100) return "成绩无效";
    if (score >= 90) return "A";
    if (score >= 80) return "B";
    if (score >= 70) return "C";
    if (score >= 60) return "D";
    return "E";
}
```

### 55. 三个整数的中间值

```java
static int middle(int a, int b, int c) {
    if ((a >= b && a <= c) || (a >= c && a <= b)) return a;
    if ((b >= a && b <= c) || (b >= c && b <= a)) return b;
    return c;
}
```

### 56. 最大公约数与最小公倍数

```java
static long gcd(long a, long b) {
    while (b != 0) {
        long remainder = a % b;
        a = b;
        b = remainder;
    }
    return a;
}

static long lcm(long a, long b) {
    return a / gcd(a, b) * b;
}
```

### 57. 素数判断

```java
static boolean isPrime(int n) {
    if (n < 2) return false;
    for (int divisor = 2; divisor <= n / divisor; divisor++) {
        if (n % divisor == 0) return false;
    }
    return true;
}
```

使用 `divisor <= n / divisor` 可避免 `divisor * divisor` 在更大整数时溢出。

### 58. 斐波那契数列

```java
static long[] fibonacci(int n) {
    if (n < 1 || n > 50) {
        throw new IllegalArgumentException("n必须在1到50之间");
    }
    long[] result = new long[n];
    result[0] = 1;
    if (n > 1) result[1] = 1;
    for (int i = 2; i < n; i++) {
        result[i] = result[i - 1] + result[i - 2];
    }
    return result;
}
```

### 59. 数字拆分与反转

```java
static void printNumberInfo(int number) {
    int original = number;
    int digits = 0;
    int sum = 0;
    int reversed = 0;

    while (number > 0) {
        int digit = number % 10;
        digits++;
        sum += digit;
        reversed = reversed * 10 + digit;
        number /= 10;
    }
    System.out.printf("%d: 位数=%d，数字和=%d，反转=%d%n",
            original, digits, sum, reversed);
}
```

### 60. 打印空心矩形

```java
static void printRectangle(int height, int width) {
    for (int row = 0; row < height; row++) {
        for (int column = 0; column < width; column++) {
            boolean border = row == 0 || row == height - 1
                    || column == 0 || column == width - 1;
            System.out.print(border ? '*' : ' ');
        }
        System.out.println();
    }
}
```

## 第二阶段参考答案

### 61. 数组统计

```java
static void printStatistics(int[] values) {
    int min = values[0];
    int max = values[0];
    long sum = 0;
    for (int value : values) {
        if (value < min) min = value;
        if (value > max) max = value;
        sum += value;
    }
    double average = (double) sum / values.length;
    System.out.printf("最大值=%d，最小值=%d，总和=%d，平均值=%.2f%n",
            max, min, sum, average);
}
```

### 62. 原地逆序数组

```java
static void reverse(int[] values) {
    for (int left = 0, right = values.length - 1;
         left < right;
         left++, right--) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
    }
}
```

### 63. 第二大的不同数字

```java
import java.util.OptionalInt;

static OptionalInt secondLargest(int[] values) {
    Integer largest = null;
    Integer second = null;
    for (int value : values) {
        if (largest == null || value > largest) {
            if (largest != null) second = largest;
            largest = value;
        } else if (value < largest && (second == null || value > second)) {
            second = value;
        }
    }
    return second == null ? OptionalInt.empty() : OptionalInt.of(second);
}
```

### 64. 选择排序

```java
static void selectionSort(int[] values) {
    for (int i = 0; i < values.length - 1; i++) {
        int minIndex = i;
        for (int j = i + 1; j < values.length; j++) {
            if (values[j] < values[minIndex]) minIndex = j;
        }
        int temporary = values[i];
        values[i] = values[minIndex];
        values[minIndex] = temporary;
    }
}
```

### 65. 有序数组插入

```java
static int[] insertSorted(int[] values, int target) {
    int position = 0;
    while (position < values.length && values[position] <= target) {
        position++;
    }

    int[] result = new int[values.length + 1];
    for (int i = 0; i < position; i++) result[i] = values[i];
    result[position] = target;
    for (int i = position; i < values.length; i++) result[i + 1] = values[i];
    return result;
}
```

### 66. 矩阵对角线

```java
static int[] diagonalSums(int[][] matrix) {
    int main = 0;
    int secondary = 0;
    for (int i = 0; i < matrix.length; i++) {
        if (matrix[i].length != matrix.length) {
            throw new IllegalArgumentException("必须是正方形矩阵");
        }
        main += matrix[i][i];
        secondary += matrix[i][matrix.length - 1 - i];
    }
    return new int[]{main, secondary};
}
```

### 67. 字符类型统计

```java
static int[] countCharacterTypes(String text) {
    int letters = 0, digits = 0, whitespace = 0, others = 0;
    for (int i = 0; i < text.length(); i++) {
        char ch = text.charAt(i);
        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) letters++;
        else if (ch >= '0' && ch <= '9') digits++;
        else if (Character.isWhitespace(ch)) whitespace++;
        else others++;
    }
    return new int[]{letters, digits, whitespace, others};
}
```

### 68. 忽略格式判断回文

```java
static boolean isPalindrome(String text) {
    int left = 0;
    int right = text.length() - 1;
    while (left < right) {
        while (left < right && !Character.isLetterOrDigit(text.charAt(left))) left++;
        while (left < right && !Character.isLetterOrDigit(text.charAt(right))) right--;
        if (Character.toLowerCase(text.charAt(left))
                != Character.toLowerCase(text.charAt(right))) return false;
        left++;
        right--;
    }
    return true;
}
```

### 69. 重叠子串计数

```java
static int countOccurrences(String text, String target) {
    if (target.isEmpty()) {
        throw new IllegalArgumentException("子串不能为空");
    }
    int count = 0;
    for (int i = 0; i <= text.length() - target.length(); i++) {
        if (text.startsWith(target, i)) count++;
    }
    return count;
}
```

### 70. 简单字符串压缩

```java
static String compress(String text) {
    if (text.isEmpty()) return "";
    StringBuilder result = new StringBuilder();
    int count = 1;
    for (int i = 1; i <= text.length(); i++) {
        if (i < text.length() && text.charAt(i) == text.charAt(i - 1)) {
            count++;
        } else {
            result.append(text.charAt(i - 1)).append(count);
            count = 1;
        }
    }
    return result.toString();
}
```

## 第三阶段参考答案

### 71. 提取素数方法

```java
static boolean isPrime(int n) {
    if (n < 2) return false;
    for (int divisor = 2; divisor <= n / divisor; divisor++) {
        if (n % divisor == 0) return false;
    }
    return true;
}

static void printPrimes() {
    for (int n = 2; n <= 100; n++) {
        if (isPrime(n)) System.out.print(n + " ");
    }
}
```

### 72. 方法重载计算面积

```java
static double area(double radius) {
    if (radius <= 0) throw new IllegalArgumentException("半径必须大于0");
    return Math.PI * radius * radius;
}

static double area(double width, double height) {
    if (width <= 0 || height <= 0) {
        throw new IllegalArgumentException("宽和高必须大于0");
    }
    return width * height;
}
```

### 73. 递归阶乘

```java
static long factorial(int n) {
    if (n < 0 || n > 20) {
        throw new IllegalArgumentException("n必须在0到20之间");
    }
    if (n <= 1) return 1L;
    return n * factorial(n - 1);
}
```

### 74. 递归计算各位数字之和

```java
static int digitSum(int n) {
    if (n < 0) throw new IllegalArgumentException("n不能为负数");
    if (n < 10) return n;
    return n % 10 + digitSum(n / 10);
}
```

### 75. 可变参数统计

```java
static double average(int... values) {
    if (values.length == 0) throw new IllegalArgumentException("至少需要一个值");
    long sum = 0;
    for (int value : values) sum += value;
    return (double) sum / values.length;
}
```

### 76. Student类

```java
final class Student {
    private final String id;
    private final String name;
    private final int[] scores;

    Student(String id, String name, int score1, int score2, int score3) {
        validate(score1);
        validate(score2);
        validate(score3);
        this.id = id;
        this.name = name;
        this.scores = new int[]{score1, score2, score3};
    }

    private static void validate(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("成绩必须在0到100之间");
        }
    }

    double average() {
        return (scores[0] + scores[1] + scores[2]) / 3.0;
    }

    @Override
    public String toString() {
        return id + "," + name + "," + String.format("%.2f", average());
    }
}
```

### 77. BankAccount封装

```java
final class BankAccount {
    private final String accountNumber;
    private long balance;

    BankAccount(String accountNumber, long initialBalance) {
        if (initialBalance < 0) throw new IllegalArgumentException("初始余额不能为负数");
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }

    void deposit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("存款金额必须大于0");
        balance += amount;
    }

    void withdraw(long amount) {
        if (amount <= 0 || amount > balance) {
            throw new IllegalArgumentException("取款金额无效");
        }
        balance -= amount;
    }

    String getAccountNumber() { return accountNumber; }
    long getBalance() { return balance; }
}
```

### 78. static对象计数

```java
final class Employee {
    private static int count;
    private final long id;
    private final String name;

    Employee(long id, String name) {
        this.id = id;
        this.name = name;
        count++;
    }

    static int getCount() { return count; }
}
```

### 79. 继承与方法重写

```java
class Employee {
    private final String name;
    private final long baseSalary;

    Employee(String name, long baseSalary) {
        this.name = name;
        this.baseSalary = baseSalary;
    }

    long calculateSalary() { return baseSalary; }
    String getName() { return name; }
}

final class Manager extends Employee {
    private final long bonus;

    Manager(String name, long baseSalary, long bonus) {
        super(name, baseSalary);
        this.bonus = bonus;
    }

    @Override
    long calculateSalary() {
        return super.calculateSalary() + bonus;
    }
}
```

### 80. 接口与多态

```java
interface Shape {
    double area();
}

final class Circle implements Shape {
    private final double radius;

    Circle(double radius) {
        if (radius <= 0) throw new IllegalArgumentException("半径必须大于0");
        this.radius = radius;
    }

    public double area() { return Math.PI * radius * radius; }
}

final class Rectangle implements Shape {
    private final double width;
    private final double height;

    Rectangle(double width, double height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("尺寸必须大于0");
        this.width = width;
        this.height = height;
    }

    public double area() { return width * height; }
}

static double totalArea(Shape[] shapes) {
    double total = 0;
    for (Shape shape : shapes) total += shape.area();
    return total;
}
```

### 81. 枚举与状态判断

```java
enum OrderStatus {
    CREATED, PAID, SHIPPED, COMPLETED, CANCELLED
}

static boolean isFinished(OrderStatus status) {
    return status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED;
}
```

### 82. JavaBean商品对象

```java
import java.math.BigDecimal;

class Product {
    private Long id;
    private String name;
    private BigDecimal price;

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }

    public void setPrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("价格不能为null或负数");
        }
        this.price = price;
    }
}
```

## 第四阶段参考答案

### 83. 安全解析整数

```java
import java.util.OptionalInt;

static OptionalInt parseInt(String text) {
    if (text == null || text.isBlank()) return OptionalInt.empty();
    try {
        return OptionalInt.of(Integer.parseInt(text.trim()));
    } catch (NumberFormatException exception) {
        return OptionalInt.empty();
    }
}
```

### 84. 自定义年龄异常

```java
class InvalidAgeException extends Exception {
    InvalidAgeException(String message) {
        super(message);
    }
}

static void register(int age) throws InvalidAgeException {
    if (age < 18 || age > 65) {
        throw new InvalidAgeException("年龄必须在18到65之间");
    }
}
```

### 85. List去重并保持顺序

```java
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

static List<String> distinct(List<String> source) {
    return new ArrayList<>(new LinkedHashSet<>(source));
}
```

### 86. Set求交集

```java
import java.util.LinkedHashSet;
import java.util.Set;

static Set<Integer> intersection(Set<Integer> first, Set<Integer> second) {
    Set<Integer> result = new LinkedHashSet<>(first);
    result.retainAll(second);
    return result;
}
```

### 87. Map统计单词

```java
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

static Map<String, Integer> countWords(String text) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    if (text == null || text.isBlank()) return counts;
    for (String word : text.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
        counts.put(word, counts.getOrDefault(word, 0) + 1);
    }
    return counts;
}
```

### 88. Comparator排序员工

```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class Employee {
    private final String department;
    private final String name;

    Employee(String department, String name) {
        this.department = department;
        this.name = name;
    }

    String getDepartment() { return department; }
    String getName() { return name; }
}

static List<Employee> sortedEmployees(List<Employee> source) {
    List<Employee> result = new ArrayList<>(source);
    result.sort(Comparator.comparing(Employee::getDepartment)
            .thenComparing(Employee::getName));
    return result;
}
```

### 89. Queue窗口排队

```java
import java.util.ArrayDeque;
import java.util.Queue;

Queue<String> queue = new ArrayDeque<>();
queue.offer("A");
queue.offer("B");
queue.offer("C");
System.out.println("服务：" + queue.poll());
System.out.println("服务：" + queue.poll());
System.out.println("剩余：" + queue);
```

### 90. Deque括号匹配

```java
import java.util.ArrayDeque;
import java.util.Deque;

static boolean isBalanced(String text) {
    Deque<Character> stack = new ArrayDeque<>();
    for (char ch : text.toCharArray()) {
        if (ch == '(' || ch == '[' || ch == '{') {
            stack.push(ch);
        } else {
            if (stack.isEmpty()) return false;
            char open = stack.pop();
            if ((ch == ')' && open != '(')
                    || (ch == ']' && open != '[')
                    || (ch == '}' && open != '{')) return false;
        }
    }
    return stack.isEmpty();
}
```

### 91. 泛型Box

```java
final class Box<T> {
    private final T value;

    Box(T value) { this.value = value; }
    T getValue() { return value; }
}

Box<String> textBox = new Box<>("Java");
Box<Integer> numberBox = new Box<>(17);
```

### 92. 泛型方法获取最后一个元素

```java
import java.util.List;
import java.util.Optional;

static <T> Optional<T> last(List<T> list) {
    if (list.isEmpty()) return Optional.empty();
    return Optional.ofNullable(list.get(list.size() - 1));
}
```

## 第五阶段参考答案

### 93. BigDecimal订单金额

```java
import java.math.BigDecimal;
import java.math.RoundingMode;

static BigDecimal total(BigDecimal unitPrice,
                        BigDecimal quantity,
                        BigDecimal discountRate) {
    if (unitPrice.signum() < 0 || quantity.signum() < 0
            || discountRate.compareTo(BigDecimal.ZERO) < 0
            || discountRate.compareTo(BigDecimal.ONE) > 0) {
        throw new IllegalArgumentException("金额、数量或折扣率无效");
    }
    BigDecimal subtotal = unitPrice.multiply(quantity);
    return subtotal.multiply(BigDecimal.ONE.subtract(discountRate))
            .setScale(2, RoundingMode.HALF_UP);
}
```

### 94. LocalDate日期计算

```java
import java.time.LocalDate;
import java.time.Period;

static int age(LocalDate birthday, LocalDate baseDate) {
    if (birthday.isAfter(baseDate)) {
        throw new IllegalArgumentException("生日不能晚于基准日");
    }
    return Period.between(birthday, baseDate).getYears();
}
```

### 95. 日期范围内的工作日

```java
import java.time.DayOfWeek;
import java.time.LocalDate;

static int countWeekdays(LocalDate start, LocalDate end) {
    if (start.isAfter(end)) throw new IllegalArgumentException("日期范围无效");
    int count = 0;
    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
        DayOfWeek day = date.getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) count++;
    }
    return count;
}
```

### 96. 使用StringBuilder生成CSV行

```java
static String toCsvLine(String[] fields) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < fields.length; i++) {
        if (i > 0) result.append(',');
        result.append(fields[i]);
    }
    return result.toString();
}
```

### 97. 写入并读取UTF-8文件

```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

static void writeAndRead() throws IOException {
    Path path = Path.of("employees.txt");
    Files.write(path, List.of("Tanaka", "Suzuki", "Sato"), StandardCharsets.UTF_8);
    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
        System.out.println(line);
    }
}
```

### 98. 统计文本文件

```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

static void printFileStatistics(Path path) {
    if (Files.notExists(path)) {
        System.out.println("文件不存在：" + path);
        return;
    }
    try {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        int nonWhitespace = 0;
        int words = 0;
        for (String line : lines) {
            for (int i = 0; i < line.length(); i++) {
                if (!Character.isWhitespace(line.charAt(i))) nonWhitespace++;
            }
            if (!line.isBlank()) words += line.trim().split("\\s+").length;
        }
        System.out.printf("行数=%d，非空白字符=%d，单词数=%d%n",
                lines.size(), nonWhitespace, words);
    } catch (IOException exception) {
        System.out.println("读取失败：" + exception.getMessage());
    }
}
```

### 99. CSV成绩汇总

```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

static void summarize(Path path) throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    int count = 0;
    int sum = 0;
    for (int index = 0; index < lines.size(); index++) {
        String line = lines.get(index);
        if (line.isBlank()) continue;
        String[] fields = line.split(",", -1);
        if (fields.length != 3) {
            System.out.printf("第%d行：字段数错误%n", index + 1);
            continue;
        }
        try {
            int score = Integer.parseInt(fields[2].trim());
            if (score < 0 || score > 100) throw new NumberFormatException();
            sum += score;
            count++;
        } catch (NumberFormatException exception) {
            System.out.printf("第%d行：成绩无效%n", index + 1);
        }
    }
    if (count == 0) System.out.println("无有效数据");
    else System.out.printf("平均分=%.2f%n", (double) sum / count);
}
```

### 100. 学生成绩小程序

下面是完整的 `Exercise100.java`：

```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Exercise100 {
    public static void main(String[] args) {
        Path input = Path.of("scores.csv");
        Path output = Path.of("score-report.txt");
        try {
            List<Student> students = readStudents(input);
            students.sort(Comparator.comparingDouble(Student::average)
                    .reversed()
                    .thenComparing(Student::getId));

            List<String> report = new ArrayList<>();
            if (students.isEmpty()) {
                report.add("无有效数据");
            } else {
                for (Student student : students) {
                    report.add(String.format("%s,%s,%.2f",
                            student.getId(), student.getName(), student.average()));
                }
            }
            Files.write(output, report, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.out.println("文件处理失败：" + exception.getMessage());
        }
    }

    static List<Student> readStudents(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Student> result = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) continue;
            String[] fields = line.split(",", -1);
            if (fields.length != 5) {
                System.out.printf("第%d行：字段数错误%n", index + 1);
                continue;
            }
            try {
                int score1 = parseScore(fields[2]);
                int score2 = parseScore(fields[3]);
                int score3 = parseScore(fields[4]);
                result.add(new Student(fields[0].trim(), fields[1].trim(),
                        score1, score2, score3));
            } catch (IllegalArgumentException exception) {
                System.out.printf("第%d行：%s%n", index + 1, exception.getMessage());
            }
        }
        return result;
    }

    static int parseScore(String text) {
        try {
            int score = Integer.parseInt(text.trim());
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("成绩必须在0到100之间");
            }
            return score;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("成绩不是整数");
        }
    }

    static final class Student {
        private final String id;
        private final String name;
        private final int score1;
        private final int score2;
        private final int score3;

        Student(String id, String name, int score1, int score2, int score3) {
            this.id = id;
            this.name = name;
            this.score1 = score1;
            this.score2 = score2;
            this.score3 = score3;
        }

        String getId() { return id; }
        String getName() { return name; }

        double average() {
            return (score1 + score2 + score3) / 3.0;
        }
    }
}
```

## 核对建议

- 不只比较示例输出，还要检查空集合、重复值、边界数字和非法输入。
- 如果实现与答案不同但满足全部规格、没有隐藏异常，也可以判定为正确。
- 文件题完成核对后，删除自己生成的 `employees.txt`、`exam-scores.csv`、`scores.csv` 和 `score-report.txt`，避免影响下一次练习。
