# 08 接口设计规范（REST + DTO / VO 教学版）

> 本章节用于 **Spring Boot 项目接口设计教学 / 面试准备 / 架构规范说明**  
> 目标：讲清楚 **什么是 REST 接口、如何设计 URL / Method、DTO / VO 如何配合使用**

---

## 一、为什么接口设计很重要？

接口是：
- 前后端的契约
- 系统与系统之间的边界

设计不合理会导致：
- 前端难以使用
- 接口频繁变更
- 维护成本高
- 面试时难以讲清

👉 企业级项目必须有 **统一接口设计规范**。

---

## 二、什么是 REST（RESTful）？

REST 是一种 **面向资源的接口设计风格**。

核心思想：
- 一切皆资源
- 使用 HTTP Method 表达行为

---

## 三、URL 设计规范（教学重点）

### 1. 使用名词，不用动词

❌ 错误示例：
```text
/getUser
/createOrder
```

✅ 正确示例：
```text
/users
/orders
```

---

### 2. 使用复数形式

```text
/users
/orders
/products
```

---

### 3. 使用层级表示资源关系

```text
/users/{userId}/orders
```

---

## 四、HTTP Method 使用规范

| Method | 含义 | 示例 |
| --- | --- | --- |
| GET | 查询 | GET /products |
| POST | 新建 | POST /products |
| PUT | 整体更新 | PUT /products/{id} |
| PATCH | 局部更新 | PATCH /products/{id} |
| DELETE | 删除 | DELETE /products/{id} |

面试一句话：

> REST 通过 HTTP Method 表达对资源的操作语义。

---

## 五、请求参数设计（DTO）

### 1. 查询参数（Query）

```http
GET /products?name=apple&page=1
```

对应 DTO：
```java
public class ProductQueryDTO {
    private String name;
    private Integer page;
}
```

---

### 2. 请求体参数（Body）

```http
POST /products
```

```java
public class ProductCreateDTO {
    private String name;
    private Integer price;
}
```

---

## 六、响应结果设计（VO）

### 1. 单对象返回

```java
public class ProductVO {
    private Long id;
    private String name;
    private Integer price;
}
```

---

### 2. 列表返回

```json
{
  "list": [],
  "total": 100
}
```

---

## 七、统一响应结构（企业级推荐）

```java
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;
}
```

成功示例：
```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

---

## 八、接口设计与异常处理的配合

- 正常流程 → 返回 SUCCESS
- 业务异常 → 返回业务错误码
- 系统异常 → 统一 SYSTEM_ERROR

👉 接口不直接返回 Exception

---

## 九、接口设计常见误区（面试高频）

### 1. 一个接口做所有事
❌ /order/doAll

### 2. Entity 直接作为接口返回
❌ 数据库字段泄露

### 3. URL 中包含动词
❌ /createUser

---

## 十、推荐包结构

```text
com.example.demo
 ├─ controller
 │   ├─ dto
 │   └─ vo
 ├─ service
 ├─ exception
 └─ common
     └─ ApiResponse.java
```

---

## 十一、面试总结用语（可直接背）

> 在 Spring Boot 项目中，接口通常遵循 RESTful 设计原则，  
> 通过 DTO 作为请求参数、VO 作为响应对象，  
> 并结合统一响应结构和异常处理，  
> 提高接口的可读性、稳定性和可维护性。
