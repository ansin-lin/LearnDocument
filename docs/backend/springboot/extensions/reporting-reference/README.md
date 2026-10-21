# 账票课程参考代码

本目录保存第01～06章从课堂正文中移出的完整参考实现。它用于教师演示、代码阅读和进阶学习，不属于新人必修内容。

## 使用前提

- Java 17
- Spring Boot 4.1.1
- MyBatis Spring Boot Starter/Test Starter 4.1.0
- MySQL 8.0.16 及以上
- Apache POI 5.5.1
- 已完成员工管理项目第15～16章的 JWT 认证与 `ROLE_ADMIN` 授权

## 文件

| 文件 | 内容 |
| --- | --- |
| [employee-list-reference.md](employee-list-reference.md) | 无模板従業員一覧表：CSV、POI、Mapper、Service、Controller |
| [invoice-reference.md](invoice-reference.md) | 有模板請求書：模板映射、动态插行、公式、Service、Controller |
| [delivery-reference.md](delivery-reference.md) | 服务器文件保存、临时文件、发布、哈希和下载检查 |

## 重要边界

这些代码按现有员工项目的包名和分层编写，但本目录不是独立 Maven 工程。請求書示例还依赖二进制模板 `src/main/resources/report-templates/invoice-v1.xlsx`。在把代码复制到项目后，仍需补充或对接项目已有的统一异常处理、配置类和测试设施。

当前仓库没有因为这些 Markdown 代码块自动得到可运行应用，也没有因此完成 MySQL 连接测试、模板生成测试或打印验证。
