# Java 集合（Collection）框架详解

## 一、前言

集合（Collection）是 Java 中用于存储和操作一组对象的容器。  
相比数组，集合的长度可变、类型安全、支持泛型与丰富的操作方法。  
Java 集合框架（Java Collections Framework, JCF）为开发者提供了统一的接口和实现结构。

---

## 二、集合框架体系结构

Java 集合分为两大类：

1. **Collection 接口体系**（单值集合）  
   包含 `List`、`Set`、`Queue` 等接口。
2. **Map 接口体系**（键值对集合）  
   用于存储键值映射，如 `HashMap`、`TreeMap`。

### 集合框架类图概览

```markdown
Collection（接口）
├── List（接口）
│   ├── ArrayList
│   ├── LinkedList
│   └── Vector
│       └── Stack
│
├── Set（接口）
│   ├── HashSet
│   │   └── LinkedHashSet
│   └── TreeSet
│
└── Queue（接口）
    ├── PriorityQueue
    └── Deque（接口）
        ├── ArrayDeque
        └── LinkedList

Map（接口）
├── HashMap
│   └── LinkedHashMap
├── TreeMap
└── Hashtable
    └── Properties

```

---

## 三、Collection 接口总览

`Collection` 是最基础的集合接口，定义了集合的通用操作：

| 方法 | 说明 |
|------|------|
| `add(E e)` | 向集合添加元素 |
| `remove(Object o)` | 删除指定元素 |
| `clear()` | 清空集合 |
| `size()` | 返回元素数量 |
| `isEmpty()` | 判断集合是否为空 |
| `contains(Object o)` | 判断集合是否包含某个元素 |
| `iterator()` | 获取迭代器 |

---

## 四、List 接口（有序可重复）

### 1. 特点

- 元素有序（插入顺序）  
- 允许重复元素  
- 支持索引访问  

### 2. 常用实现类

| 类名 | 底层结构 | 线程安全 | 特点 |
|------|-----------|-----------|------|
| `ArrayList` | 动态数组 | 否 | 查询快，增删慢 |
| `LinkedList` | 双向链表 | 否 | 增删快，查询慢 |
| `Vector` | 动态数组 | 是 | 线程安全，较旧 |

### 示例

```java
import java.util.*;

public class ListDemo {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("Java");
        list.add("Python");
        list.add("Go");
        System.out.println(list.get(1)); // Python
        list.remove("Go");
        System.out.println(list);
    }
}
```

---

## 五、Set 接口（无序不重复）

### 1. 特点1

- 不允许重复元素  
- 元素无序（部分实现有序）  
- 通过 `equals()` 与 `hashCode()` 判断唯一性  

### 2. 常用实现类1

| 类名 | 底层结构 | 特点 |
|------|-----------|------|
| `HashSet` | 哈希表 | 无序，查找速度快 |
| `LinkedHashSet` | 哈希表 + 双向链表 | 有序（按插入顺序） |
| `TreeSet` | 红黑树 | 自动排序（可自定义 Comparator） |

### 示例1

```java
import java.util.*;

public class SetDemo {
    public static void main(String[] args) {
        Set<String> set = new HashSet<>();
        set.add("Java");
        set.add("Python");
        set.add("Java"); // 重复元素不会添加
        System.out.println(set);
    }
}
```

---

## 六、Queue 接口（队列）

### 1. 特点2

- 遵循 FIFO（先进先出）原则  
- 通常用于任务调度或消息队列  

### 2. 常用实现类2

| 类名 | 说明 |
|------|------|
| `PriorityQueue` | 基于堆的优先级队列 |
| `ArrayDeque` | 双端队列（Deque 的实现） |

### 示例2

```java
import java.util.*;

public class QueueDemo {
    public static void main(String[] args) {
        Queue<Integer> queue = new PriorityQueue<>();
        queue.offer(3);
        queue.offer(1);
        queue.offer(2);
        System.out.println(queue.poll()); // 1（最小优先）
    }
}
```

---

## 七、Map 接口（键值对集合）

### 1. 特点3

- 存储键值对（key-value）形式  
- key 唯一，value 可重复  
- 不继承 `Collection` 接口  

### 2. 常用实现类3

| 类名 | 底层结构 | 特点 |
|------|-----------|------|
| `HashMap` | 哈希表 | 无序，允许 null 键值 |
| `LinkedHashMap` | 哈希表 + 链表 | 按插入顺序保存 |
| `TreeMap` | 红黑树 | 按 key 排序 |
| `Hashtable` | 哈希表 | 线程安全，不允许 null |

### 示例3

```java
import java.util.*;

public class MapDemo {
    public static void main(String[] args) {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "Java");
        map.put(2, "Python");
        map.put(3, "Go");

        System.out.println(map.get(2)); // Python
        map.remove(3);

        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}
```

---

## 八、Collections 工具类

`Collections` 是集合的操作工具类，包含大量静态方法。

| 方法 | 说明 |
|------|------|
| `sort(List<T> list)` | 排序 |
| `reverse(List<T> list)` | 反转 |
| `shuffle(List<T> list)` | 随机打乱 |
| `max()/min()` | 获取最大/最小值 |
| `frequency(Collection, Object)` | 统计元素出现次数 |

### 示例4

```java
import java.util.*;

public class CollectionsDemo {
    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(3, 1, 2, 5, 4);
        Collections.sort(list);
        Collections.reverse(list);
        System.out.println(list);
    }
}
```

---

## 九、集合遍历与泛型

### 1. 使用增强 for 循环

```java
for (String item : list) {
    System.out.println(item);
}
```

### 2. 使用迭代器

```java
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    System.out.println(it.next());
}
```

### 3. 使用 Lambda

```java
list.forEach(item -> System.out.println(item));
```

---

## 十、性能与适用场景对比

| 类型 | 实现类 | 查询效率 | 增删效率 | 线程安全 | 特点 |
|------|---------|------------|------------|-------------|------|
| List | ArrayList | 高 | 低 | 否 | 顺序存储，访问快 |
| List | LinkedList | 低 | 高 | 否 | 链表结构 |
| Set | HashSet | 高 | 高 | 否 | 无序唯一 |
| Set | TreeSet | 中 | 中 | 否 | 自动排序 |
| Map | HashMap | 高 | 高 | 否 | 常用键值存储 |
| Map | TreeMap | 中 | 中 | 否 | 按 key 排序 |
| Map | Hashtable | 中 | 中 | 是 | 线程安全，老旧 |

---

## 十一、总结

- 集合是 Java 最核心的容器框架之一，是数据存储与处理的基础。  
- `List`：有序可重复；`Set`：无序不可重复；`Map`：键值对存储。  
- 选择集合实现类时，应根据**访问频率、线程安全、排序要求**来确定。  
- `Collections` 工具类提供了常用的集合操作与辅助方法。  

> 熟练掌握集合框架，是成为 Java 后端开发工程师的必备技能之一。

---
