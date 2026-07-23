# 第3章 Flask Blueprint 与项目分层

> 项目增量：使用 Blueprint 拆分员工和部门模块，并建立 Service、Repository 调用方向。

## 一、主要内容

1. 单文件应用的问题
2. Blueprint 的作用和注册
3. URL 前缀
4. 按业务模块组织代码
5. Application Factory 与扩展初始化
6. Router、Service、Repository 职责
7. 避免循环导入
8. 员工和部门模块拆分

## 二、完成后的结构

~~~text
app/
├── __init__.py
├── extensions.py
├── employees/
├── departments/
├── services/
└── repositories/
~~~

## 三、练习

将健康检查和员工管理分别拆分为 Blueprint，并验证 URL 前缀。
