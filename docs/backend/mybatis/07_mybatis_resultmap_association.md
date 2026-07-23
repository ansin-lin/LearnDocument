# 第7章 MyBatis ResultMap 与关联关系

> 目标：让你**一眼就能分清** 一对一、一对多、多对多 三种关系，知道：  
>
> - 它们在**业务上**是什么关系  
> - 在**数据库表结构**上怎么设计  
> - 在 **Java 实体类** 中怎么建模  
> - 在 **MyBatis ResultMap** 中怎么写 `<association>`、`<collection>`

---

## 一、三个关系先

- 一对一：  
    - 一个用户只有一张身份证，一张身份证只属于一个用户。  
- 一对多：  
    - 一个部门有多个员工，但每个员工只属于一个部门。  
- 多对多：  
    - 一个学生可以选多门课程，一门课程可以被多个学生选。  

下面所有示例都会围绕这三个非常常见的场景来展开。

---

## 二、一对一关系（User - UserDetail）

### 1️⃣ 业务含义

- 一个用户（User）对应一条详细信息（UserDetail）。
- 一个 UserDetail 只属于一个 User。

### 2️⃣ 数据库表设计

- 表 `user`：用户基本信息
- 表 `user_detail`：用户详细信息，**一行对应一个 user**

```sql
CREATE TABLE user (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50),
  age INT
);

CREATE TABLE user_detail (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT UNIQUE,           -- 关键：加 UNIQUE，确保一对一
  address VARCHAR(100),
  phone VARCHAR(20),
  CONSTRAINT fk_user_detail_user FOREIGN KEY(user_id) REFERENCES user(id)
);
```

**关系解释：**

- user.id ←→ user_detail.user_id（唯一外键）  
- 一个 user 只能在 user_detail 中出现 0 或 1 次 → 一对一。

### 3️⃣ Java 实体类

```java
public class User {
    private Integer id;
    private String username;
    private Integer age;
    private UserDetail detail; // 一对一
    // getter/setter...
}

public class UserDetail {
    private Integer id;
    private String address;
    private String phone;
    // getter/setter...
}
```

### 4️⃣ 一对一 ResultMap（嵌套查询方式）

#### 4.1 查询用户基本信息时，顺便把详情查出来（懒加载）

```xml
<resultMap id="UserWithDetailMap" type="com.example.entity.User">
  <id property="id" column="id"/>
  <result property="username" column="username"/>
  <result property="age" column="age"/>

  <association property="detail"
               javaType="com.example.entity.UserDetail"
               column="id"
               select="com.example.mapper.UserDetailMapper.selectByUserId"
               fetchType="lazy"/>
</resultMap>

<select id="selectUserWithDetail" resultMap="UserWithDetailMap">
  SELECT id, username, age FROM user WHERE id = #{id}
</select>

<!-- 子查询 -->
<select id="selectByUserId" resultType="com.example.entity.UserDetail">
  SELECT id, user_id, address, phone FROM user_detail WHERE user_id = #{userId}
</select>
```

**关键说明（无序列表）：**

- `<association>` 表示一对一。  
- `property="detail"`：映射到 User.detail 属性。  
- `javaType`：关联对象的类型。  
- `column="id"`：当前主表（user）的 id，作为参数传给子查询。  
- `select="...UserDetailMapper.selectByUserId"`：调用另一个 mapper 的方法。  
- `fetchType="lazy"`：detail 用到时才查，避免无意义的 JOIN。  

### 5️⃣ 一对一 ResultMap（单 SQL JOIN + 嵌套结果）

```xml
<resultMap id="UserWithDetailJoinMap" type="com.example.entity.User">
  <id property="id" column="uid"/>
  <result property="username" column="username"/>
  <result property="age" column="age"/>

  <association property="detail" javaType="com.example.entity.UserDetail">
    <id property="id" column="did"/>
    <result property="address" column="address"/>
    <result property="phone" column="phone"/>
  </association>
</resultMap>

<select id="selectUserWithDetailJoin" resultMap="UserWithDetailJoinMap">
  SELECT u.id   AS uid,
         u.username,
         u.age,
         d.id   AS did,
         d.address,
         d.phone
  FROM user u
  LEFT JOIN user_detail d 
  ON u.id = d.user_id
  WHERE u.id = #{id}
</select>
```

**对比：**

- 嵌套查询：2 次 SQL，但结构简单、懒加载方便。  
- JOIN 嵌套结果：1 条 SQL，把所有字段一次查回。

---

## 三、一对多关系（Department - Employee）

### 1️⃣ 业务含义

- 一个部门（Department）有多个员工（Employee）。  
- 每个员工只属于一个部门。  

### 2️⃣ 数据库表设计

```sql
CREATE TABLE department (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50)
);

CREATE TABLE employee (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50),
  dept_id INT,
  CONSTRAINT fk_emp_dept FOREIGN KEY(dept_id) REFERENCES department(id)
);
```

**关系解释：**

- department.id ←→ employee.dept_id  
- 一个部门：可以在 employee 表中对应多行 → 一对多。  
- 一个员工：dept_id 只指向一个部门 → 多对一。  

### 3️⃣ Java 实体类

```java
public class Department {
    private Integer id;
    private String name;
    private List<Employee> employees; // 一对多
    // getter/setter...
}

public class Employee {
    private Integer id;
    private String name;
    private Department department; // 多对一（可选）
    // getter/setter...
}
```

### 4️⃣ 一对多 ResultMap（嵌套查询）

```xml
<resultMap id="DeptWithEmpsMap" type="com.example.entity.Department">
  <id property="id" column="id"/>
  <result property="name" column="name"/>
  <collection property="employees"
              ofType="com.example.entity.Employee"
              column="id"
              select="com.example.mapper.EmployeeMapper.selectByDeptId"
              fetchType="lazy"/>
</resultMap>

<select id="selectDeptWithEmps" resultMap="DeptWithEmpsMap">
  SELECT id, name FROM department WHERE id = #{id}
</select>

<select id="selectByDeptId" resultType="com.example.entity.Employee">
  SELECT id, name, dept_id FROM employee WHERE dept_id = #{deptId}
</select>
```

**关键说明：**

- `<collection>` 表示一对多。  
- `property="employees"`：对应 Department.employees 列表。  
- `ofType="Employee"`：集合中每个元素的类型。  
- `column="id"`：部门 id 传给子查询（deptId）。  

### 5️⃣ 一对多 ResultMap（JOIN + 嵌套结果）

```xml
<resultMap id="DeptWithEmpsJoinMap" type="Department">
  <id property="id" column="did"/>
  <result property="name" column="dname"/>

  <collection property="employees" ofType="Employee">
    <id property="id" column="eid"/>
    <result property="name" column="ename"/>
  </collection>
</resultMap>

<select id="selectDeptWithEmpsJoin" resultMap="DeptWithEmpsJoinMap">
  SELECT d.id AS did, d.name AS dname,
         e.id AS eid, e.name AS ename
  FROM department d
  LEFT JOIN employee e ON d.id = e.dept_id
  WHERE d.id = #{id}
</select>
```

**注意点：**

- 同一个部门会在结果集中出现多行（因为每个员工一行）。  
- MyBatis 会根据 `<id>` 标签去重，把重复部门合并到同一个 Department 实例。  

---

## 四、多对多关系（Student - Course）

### 1️⃣ 业务含义

- 一个学生可以选择多门课程。  
- 一门课程也可以被多个学生选择。  

### 2️⃣ 数据库表设计

```sql
CREATE TABLE student (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50)
);

CREATE TABLE course (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100)
);

CREATE TABLE student_course (
  student_id INT,
  course_id INT,
  PRIMARY KEY(student_id, course_id),
  CONSTRAINT fk_sc_student FOREIGN KEY(student_id) REFERENCES student(id),
  CONSTRAINT fk_sc_course FOREIGN KEY(course_id) REFERENCES course(id)
);
```

**关系解释：**

- student : student_course = 1 : 多  
- course : student_course = 1 : 多  
- 合起来：student 和 course 是 多对多。  

### 3️⃣ Java 实体类

```java
public class Student {
    private Integer id;
    private String name;
    private List<Course> courses; // 多对多之一
    // getter/setter...
}

public class Course {
    private Integer id;
    private String title;
    private List<Student> students; // 反向多对多（可选）
    // getter/setter...
}
```

### 4️⃣ 多对多：从 “学生 → 选课列表” 方向查询（嵌套查询）

```xml
<resultMap id="StudentWithCoursesMap" type="Student">
  <id property="id" column="sid"/>
  <result property="name" column="sname"/>

  <collection property="courses"
              ofType="Course"
              column="sid"
              select="com.example.mapper.CourseMapper.selectByStudentId"/>
</resultMap>

<select id="selectStudentWithCourses" resultMap="StudentWithCoursesMap">
  SELECT id AS sid, name AS sname FROM student WHERE id = #{id}
</select>

<select id="selectByStudentId" resultType="Course">
  SELECT c.id, c.title
  FROM course c
  JOIN student_course sc ON c.id = sc.course_id
  WHERE sc.student_id = #{sid}
</select>
```

**理解方式：**

- Student 视角：我有很多 Course → 一对多。  
- 站在 Course 视角，同样可以通过中间表查到很多 Student。  
- 本质多对多 = 两个“一对多 + 中间表”。  

### 5️⃣ 多对多：JOIN + 嵌套结果（一次查出所有）

```xml
<resultMap id="StudentCoursesJoinMap" type="Student">
  <id property="id" column="sid"/>
  <result property="name" column="sname"/>

  <collection property="courses" ofType="Course">
    <id property="id" column="cid"/>
    <result property="title" column="ctitle"/>
  </collection>
</resultMap>

<select id="selectStudentWithCoursesJoin" resultMap="StudentCoursesJoinMap">
  SELECT s.id AS sid, s.name AS sname,
         c.id AS cid, c.title AS ctitle
  FROM student s
  LEFT JOIN student_course sc ON s.id = sc.student_id
  LEFT JOIN course c ON sc.course_id = c.id
  WHERE s.id = #{id}
</select>
```

**特点：**

- 一条 SQL 查出学生和所有课程。  
- 如果课程很多，结果集会比较大。  

---

## 五、association 与 collection 的属性说明

### 1️⃣ `<association>` 常用属性

- `property`：Java 实体中的属性名，例如 `user.detail`、`order.user`。  
- `javaType`：属性类型，例如 `UserDetail.class`。  
- `column`：当前结果集中的列名，作为参数传给子查询。  
- `select`：要调用的子查询语句 ID（命名空间 + id）。  
- `resultMap`：也可以直接引用另一个 `<resultMap>`。  
- `fetchType`：`lazy` / `eager`，控制懒加载。  

### 2️⃣ `<collection>` 常用属性

- `property`：集合属性名，例如 `department.employees`、`student.courses`。  
- `ofType`：集合中单个元素的类型，例如 `Employee.class`。  
- `column`：传给子查询的列，如主表主键。  
- `select`：引用子查询语句 ID。  
- `resultMap`：可引用另一个 ResultMap。  
- `fetchType`：懒加载还是立即加载。  

---

## 六：应该选哪种写法？

- **数据量小、结构简单**：推荐 JOIN + 嵌套结果（一次查完）。  
- **关系复杂、容易变化**：推荐嵌套查询（结构清晰、改动更小）。  
- **读多写少、频繁访问**：考虑配合二级缓存。  
- **数据量大且层级多**：结合分页 + 懒加载。  

---

## 七、用一句话总结三种关系

- 一对一：**一个对象里面嵌一个对象** → 用 `<association>`。  
- 一对多：**一个对象里面嵌一个 List** → 用 `<collection>`。  
- 多对多：**一个对象里面嵌一个 List，对面那个对象也可以有个 List**，中间再加一张关联表。  

只要你能在业务上用“一个 / 多个”说清楚关系，  
在 MyBatis 里就是选 `<association>` 还是 `<collection>` + 有没有中间表的区别。  
