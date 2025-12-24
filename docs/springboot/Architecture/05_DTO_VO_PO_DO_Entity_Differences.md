# 05 DTO / VO / PO / DO / Entity 的区别（教学版）

> 本章节用于 **Spring Boot 项目教学 / 面试准备 / 架构规范说明**  
> 重点解决：**为什么要分这些对象、各自用在什么地方、实际项目如何落地**

---

## 一、先给结论（教学必背版）

| 对象 | 全称 | 主要职责 | 所在层 | 是否暴露给前端 |
| --- | --- | --- | --- | --- |
| PO | Persistent Object | 数据库表映射 | Mapper / Repository | 否 |
| Entity | 实体 | 业务核心对象 | Service / Domain | 一般不直接 |
| DO | Domain Object | 领域业务对象 | Service / Domain | 否 |
| DTO | Data Transfer Object | 数据传输 | Controller ↔ Service | 是（常见） |
| VO | View Object | 前端展示对象 | Controller / API | 是 |

一句话总结（教学用）：

> **PO 面向数据库，DO / Entity 面向业务，DTO / VO 面向接口与前端。**

---

## 二、为什么要区分这么多对象？

在企业级项目中，如果 **一个类既当数据库对象、又当接口对象、又写业务逻辑**，会带来以下问题：

- 数据库字段直接暴露给前端（安全风险）
- 表结构变更导致接口大面积修改
- 类字段越来越多，职责混乱
- 接口难以版本升级
- 业务规则难以维护与测试

👉 因此，引入 **对象分层** 是企业项目的基本规范。

---

## 三、PO（Persistent Object）

### 1. 定义

PO 是**持久化对象**，与数据库表字段 **一一对应**。

### 2. 特点

- 字段基本等同于表字段
- 不包含业务逻辑
- 仅用于数据库读写

### 3. 示例

```java
public class ProductPO {
    private Long id;
    private String name;
    private Integer price;
    private Integer stock;
    private Integer deletedFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 4. 使用位置

- MyBatis Mapper
- Repository 层

---

## 四、Entity / DO（业务对象）

> 在很多项目中 **DO 与 Entity 概念相同**，可视为业务领域对象。

### 1. 定义

Entity / DO 用于表示**业务中的核心概念**，不单纯等同于数据库表。

### 2. 特点

- 关注业务含义
- 可以包含业务规则
- 不直接暴露给前端

### 3. 示例

```java
public class ProductDO {
    private Long id;
    private String name;
    private Integer price;
    private Integer stock;

    public boolean canPurchase(int quantity) {
        return stock != null && stock >= quantity;
    }
}
```

### 4. 使用位置

- Service 层
- Domain 层

---

## 五、DTO（Data Transfer Object）

### 1. 定义

DTO 用于 **层与层之间、系统与系统之间的数据传输**。

### 2. 特点

- 面向接口
- 字段按“输入需要”设计
- 不包含业务逻辑

### 3. 示例（请求参数）

```java
public class ProductCreateDTO {
    private String name;
    private Integer price;
    private Integer initialStock;
}
```

### 4. 使用位置

- Controller 入参
- RPC / API 传输

---

## 六、VO（View Object）

### 1. 定义

VO 是**返回给前端展示的数据对象**。

### 2. 特点

- 字段贴合 UI 展示
- 可包含格式化字段
- 不等同于数据库结构

### 3. 示例（返回结果）

```java
public class ProductDetailVO {
    private Long id;
    private String name;
    private Integer price;
    private String priceLabel;
    private Boolean inStock;
}
```

### 4. 使用位置

- Controller 返回值
- REST API 响应体

---

## 七、对象转换流程（教学重点）

```text
请求 JSON
  ↓
DTO（接口入参）
  ↓
DO / Entity（业务处理）
  ↓
PO（数据库持久化）
  ↓
数据库
  ↑
PO（查询结果）
  ↑
DO / Entity（业务加工）
  ↑
VO（接口返回）
  ↑
响应 JSON
```

教学总结一句话：

> **DTO / VO 稳定接口，DO / Entity 承载业务，PO 绑定数据库。**

---

## 八、常见误区说明（课堂重点）

### 误区 1：Entity 直接作为接口返回

- 风险：数据库字段泄露
- 结果：接口不可控

### 误区 2：DTO 写业务逻辑

- DTO 应保持“纯数据结构”
- 业务逻辑应放在 Service / DO 中

### 误区 3：PO 到处传

- PO 只应存在于数据访问层附近

---

## 九、推荐包结构（教学规范）

```text
com.example.demo
 ├─ controller
 │   ├─ dto
 │   └─ vo
 ├─ service
 │   └─ domain   // DO / Entity
 ├─ mapper
 │   └─ po
 └─ config
```

---

## 十、面试 / 教学总结语

> 在 Spring Boot 项目中，通过区分 PO、DO、DTO、VO、Entity，  
> 可以降低耦合、保护数据库结构、提高接口稳定性和系统可维护性，  
> 是企业级开发中的通用设计规范。
