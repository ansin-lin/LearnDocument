# Java 程序结构

> 本文件讲解 Java 程序的组成与 **项目目录结构**。配套示例可直接复制使用。

---

## 项目目录结构

### 纯 JDK 手工项目

```java
hello-java/
├─ src/
│  └─ com/
│     └─ example/
│        └─ app/
│           └─ Main.java
├─ lib/                 # 依赖 jar（可选）
├─ out/                 # 编译输出目录（建议）
└─ README.md
```

## 源文件基本结构

一个最小可运行的 Java 程序：

```java
// 文件路径：src/main/java/com/example/app/Main.java
package com.example.app;        // ① 包声明（必须与目录匹配）

import java.time.LocalDate;     // ② import 语句（可选）

/**
 * 程序入口类
 */
public class Main {             // ③ 顶级 public 类名与文件名一致
    public static void main(String[] args) { // ④ 程序入口
        System.out.println("Hello, Java! " + LocalDate.now());
    }
}
```

**要点**  

- **编码统一用 UTF‑8**（JDK 18+ 默认 UTF‑8；旧版本建议 `javac -encoding UTF-8`）。  
- 每个 `.java` 文件**至多一个** `public` 顶级类，且文件名必须与该类同名。  
- `package` 必须与 **物理目录**一致（例如 `com.example.app` → `com/example/app`）。

---

## 最小可运行项目

### 10.1 纯 JDK 版本

```java
mini/
├─ src/
│  └─ com/example/app/Main.java
└─ out/
```

`Main.java`：

```java
package com.example.app;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Mini Java!");
    }
}
```

## 常见错误排查

| 现象 | 可能原因 | 解决方案 |
|---|---|---|
| `Error: Could not find or load main class ...` | 类路径错误；包名与目录不一致；缺少 `Main` | 检查 `-cp` 与目录结构；确认 `package` 与路径一致 |
| `package xxx does not exist` | 依赖未加入类路径；导入包名写错 | 添加 jar 到 `-cp` 或构建依赖；检查 `import` |
| `NoSuchMethodError: main` | `main` 签名不对 | 必须是 `public static void main(String[] args)` |
| 中文输出乱码 | 编码不一致 | 统一 UTF‑8；编译时 `-encoding UTF-8`；IDE 配置 |
| 运行 jar 找不到依赖 | 不是 fat‑jar；类路径缺依赖 | 使用打包插件（如 Maven shade、Spring Boot）或手动 `-cp lib/*` |

---
