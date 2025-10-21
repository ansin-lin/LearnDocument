# Java 包与 Jar 文件的创建

## 前言

在之前的文章中，讲到了面向的 3 大特性（封装、继承、多态）和面向对象设计的 5 大原则（SRP、OCP、LSP、DIP、ISP）。此外，我们还讲了如何创建一个类，并且在创建类后如何构造一个对象。然后还介绍了类中的属性和方法，并对构造方法和引用也做了简单的讲解。

有了上面的基础之后，今天我们来继续学习面向对象的相关知识，主要内容如下：

- 包
- jar 文件的创建

---

## 包（Package）

假设现在有这么一种情况，诸葛亮、周瑜、曹操共同开发一款程序。其中，周瑜和曹操均在自己代码模块中写了一个 `PublicUtil` 类，现在诸葛亮要调用他们的模块代码，需要同时用到 `PublicUtil` 类，这时候就会产生冲突。

为了解决这个问题，Java 引入了 **包机制（package）**，它提供了命名空间，可以避免类名冲突。

### 包名规则

- 一般规则：公司域名反写 + 包的作用。
- 包名全部使用小写字母。

示例：

```java
// 曹操的 PublicUtil 类
package caocao;
public class PublicUtil {
    ...
}

// 周瑜的 PublicUtil 类
package zhouyu;
public class PublicUtil {
    ...
}

// 诸葛亮使用两者的类
package zhugeliang;
import caocao.*;
import zhouyu.*;

public class Util {
    public static void main(String[] args) {
        // 使用周瑜的工具类
        zhouyu.PublicUtil.xxx();
        // 使用曹操的工具类
        caocao.PublicUtil.xxx();
    }
}
```

### 包的作用

1. 将功能类似的类和接口组织在一起，方便查找和使用。
2. 避免命名冲突，不同包中可以有同名的类。
3. 提供访问权限控制。

---

## JAR 文件的创建

JAR（Java ARchive）文件是一种压缩包格式，用于将多个 `.class` 文件、资源文件等打包在一起，方便分发和使用。

### 创建 JAR 文件

1. 编译 Java 文件：

   ```bash
   javac HelloWorld.java
   ```

2. 使用 `jar` 工具打包：

   ```bash
   jar cvf HelloWorld.jar HelloWorld.class
   ```

   - `c` 创建 jar 文件
   - `v` 显示打包过程
   - `f` 指定输出文件名

3. 运行 jar 文件（如果包含 `Main-Class`）：

   ```bash
   java -jar HelloWorld.jar
   ```

### 在 Manifest 文件中指定主类

创建 `manifest.txt` 文件：

```bash
Main-Class: HelloWorld
```

再执行：

```bash
jar cvfm HelloWorld.jar manifest.txt HelloWorld.class
```

这样就可以直接用 `java -jar HelloWorld.jar` 运行程序。

---

## 小结

- 包（package）用于管理命名空间和访问权限。
- 注释有三种：单行、多行、文档注释。
- Javadoc 可生成 HTML 文档。
- JAR 文件用于打包和分发 Java 应用程序。
