# 第 38 章 MCP 基础

> 本章成果：解释 MCP Host、Client、Server、Tool 和 Resource 的职责，并设计一个限定范围的只读接入。

MCP 是应用与外部能力、上下文交互的协议，不是模型、Agent 或权限系统。Host 承载用户体验和安全边界；Client 与 Server 建立连接；Server 暴露 Tools、Resources 等能力。

## 1. 核心角色

Host 管理用户、模型和整体授权；Client 代表 Host 与一个 Server 通信；Server 提供 Tool、Resource 等能力。Tool 通常执行动作，Resource 提供可读取内容；具体支持以协议版本为准。

## 2. 与普通 API 的关系

MCP 统一能力发现和调用方式，但底层仍可能访问 API、文件或数据库。它不能代替业务鉴权、网络隔离、输入校验和审计，也不要求应用一定采用 Agent。

接入前审查 Server 来源、传输方式、凭据、暴露数据、工具副作用和日志。工具描述和远端返回都不能取代宿主授权。先从只读、少量、虚构数据开始验证成功、拒绝、超时和断连。

## 3. 接入检查

锁定协议和 Server 版本；确认启动与传输；限制可用工具；检查凭据范围；记录调用；验证断连和超时；审查供应链。远端 Tool/Resource 的名称与内容都按不可信输入处理。

## 4. 最小案例

先接入一个只读的测试工单工具，验证能力发现、参数 Schema、权限、结构化结果和离线错误。最小案例不同时引入写工具和多个 Server。

## 5. Host、Client、Server 怎样协作

Host 是承载 AI 应用与用户交互的程序；Client 在 Host 内与某个 MCP Server 建立连接；Server 暴露可发现的能力。一个 Host 可以连接多个 Server，每个连接的权限和生命周期应独立管理。

MCP 统一的是能力描述与交互方式，不等于替业务系统自动增加安全。身份认证、用户授权、网络隔离、参数校验和审计仍由应用与 Server 负责。

## 6. Tool、Resource 与 Prompt

- **Tool** 表示可执行动作，可能有副作用；
- **Resource** 表示可读取的上下文或数据；
- **Prompt** 表示 Server 提供的可复用提示模板或工作流入口。

不是所有数据读取都应该做成 Tool，也不是所有能力都必须暴露给模型。根据能力是否执行动作、是否需要参数和是否适合用户选择来设计。

## 7. 接入一个 Server 前检查什么

确认来源与版本、运行位置、它能访问的文件和网络、需要的凭据、暴露的能力、数据会发送到哪里、是否有写操作以及怎样停用。对第三方 Server 按供应链组件管理，不能因为协议统一就默认可信。

## 8. 最小演练的验收

让工单助手通过 MCP 读取一条测试工单：Host 只授予测试数据范围；Client 列出 Server 能力；模型选择读取工具；Server 校验编号并返回最小字段；日志记录调用。随后测试不存在编号、越权编号、Server 离线和恶意返回内容，证明系统能拒绝或降级。

ServiceDesk MCP 只暴露 `get_my_ticket`，输入工单 ID，身份由 Host 的认证上下文传递，Server 再检查归属。不得让模型把 `user_id` 参数改成他人账号。

实践：设计一个只返回本人虚构工单的 MCP Server 契约，画出调用链和信任边界。具体实现以[MCP 官方文档](https://modelcontextprotocol.io/docs/learn/architecture)为准。
