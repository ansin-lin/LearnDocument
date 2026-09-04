# AI 教学路线与阶段验收

课程按“认识与判断 → AI 辅助工作 → API 应用 → RAG → Agent → 生产化 → 综合项目”推进。每个阶段都必须留下可观察成果；只看完概念或得到一次漂亮回答不算完成。

## 前置能力

第 1～16 章不要求特定编程语言。进入第 17 章前，应能够独立完成以下操作：

| 能力 | 检查方式 | 课程入口 |
| --- | --- | --- |
| Python | 能阅读类型提示、异常、模块、JSON 和异步函数 | [Python 基础](../python/index.md) |
| FastAPI | 能实现路由、Pydantic 模型、依赖注入、异常响应和接口测试 | [FastAPI 路线](../python/web/fastapi/index.md) |
| Web | 能解释 HTTP 请求、响应、状态码、认证与授权 | [Web 开发基础](../web_basics/00_frontend_backend_request_response.md) |
| SQL | 能写参数化查询并理解事务与索引 | [SQL 课程](../database/sql/00_database_introduction.md) |
| React | 能实现表单、请求、加载和错误状态；仅前端练习需要 | [React/Vue 常用库教程](../frontend/libraries/react-vue-common-libraries-full-tutorial.md) |
| Docker/Git | 能构建已有项目、查看差异并安全提交 | [Git](../tools/git/index.md) |

未通过前置检查时，先回到对应课程；AI 课程不会用一章重新讲完框架基础。

## 阶段一：AI 基础与使用边界（01～07，全员）

| 章 | 内容 | 可检查成果 |
| --- | --- | --- |
| [01](mainline/01_ai_foundations.md) | AI、ML、DL、生成式 AI、LLM | 对五个概念做专业分类，并判断三个业务场景 |
| [02](mainline/02_llm_mechanics.md) | Token、Embedding、Transformer、预测生成 | 解释一次输入如何变为输出，不把生成等同检索 |
| [03](mainline/03_llm_core_concepts.md) | Context、Temperature、幻觉、知识边界 | 完成参数对照与证据核对 |
| [04](mainline/04_prompt_basics.md) | Prompt 基本结构 | 写出含任务、约束、依据和输出格式的提示 |
| [05](mainline/05_prompt_practice.md) | Coding、SQL、测试、翻译和文档 Prompt | 改写五类含糊任务并核对结果 |
| [06](mainline/06_context_engineering.md) | 代码、需求、日志和文档上下文 | 制作最小上下文包并处理冲突信息 |
| [07](mainline/07_ai_risks.md) | 幻觉、错误代码、敏感数据和权限 | 完成一次脱敏和风险分级 |

阶段验收：面对一份陌生业务需求，能够说明是否适合用 AI、需要提供什么依据、结果怎样核对、哪些内容不得提交。

## 阶段二：AI 辅助软件工程（08～16，全员）

| 章 | 内容 | 可检查成果 |
| --- | --- | --- |
| [08](mainline/08_requirements_analysis.md) | 需求整理、拆分、疑问和异常场景 | 需求表与 QA 清单 |
| [09](mainline/09_ai_coding.md) | 生成、补全、解释和小步修改 | 受限差异与自测记录 |
| [10](mainline/10_code_review.md) | Bug、可读性、性能、安全和重构 | 有证据的 Review 指摘表 |
| [11](mainline/11_ai_debug.md) | Exception、Log、Stack Trace 和定位 | 复现—假设—证据—修复记录 |
| [12](mainline/12_ai_sql.md) | SQL 生成、分析、优化和测试数据 | 参数化 SQL 与执行计划核对 |
| [13](mainline/13_test_design.md) | 正常、异常、边界和组合测试 | 可追踪测试观点表 |
| [14](mainline/14_test_code.md) | pytest、Mock 和 API Test | 可重复测试与失败演示 |
| [15](mainline/15_documentation.md) | 设计、接口和代码说明 | 与实际实现一致的文档差异 |
| [16](mainline/16_japanese_projects.md) | 日文需求、QA、障害调查和证迹 | 一份日文现场交付包 |

阶段验收：使用 AI 辅助完成一个小改修，但每条结论都能回到规格、代码或实际测试结果。

## 阶段三：AI 业务应用基础（17～24，开发者）

| 章 | 内容 | 可检查成果 |
| --- | --- | --- |
| [17](mainline/17_ai_architecture.md) | React → FastAPI → LLM API 架构 | 数据流、信任边界和失败路径图 |
| [18](mainline/18_project_configuration.md) | SDK、环境变量和模型配置 | 不含密钥的可重建配置 |
| [19](mainline/19_ai_service_integration.md) | 在既有 FastAPI 项目接入 AI Service | 可替换客户端和假实现测试 |
| [20](mainline/20_llm_api.md) | Model、Instructions、Input、Response | 第一次真实或替身模型调用 |
| [21](mainline/21_chat_application.md) | 对话状态与历史 | 有长度边界的会话接口 |
| [22](mainline/22_streaming.md) | Streaming、SSE 和前端状态 | 能结束、取消和报错的流式页面 |
| [23](mainline/23_structured_output.md) | JSON Schema、Pydantic 和业务校验 | 工单结构化提取接口 |
| [24](mainline/24_tool_calling.md) | Function/Tool Calling | 一个只读工单查询工具 |

阶段验收：完成工单提取和只读查询；格式错误、模型超时、工具失败均返回可识别错误，不泄露密钥。

## 阶段四：企业知识库 RAG（25～33，开发者/进阶）

第 28 章先给出 RAG 全貌；第 25～27 章先建立组成能力。教师可在第 24 章结束时先用项目规格中的 RAG 流程图做动机导入。

| 章 | 内容 | 可检查成果 |
| --- | --- | --- |
| [25](mainline/25_embedding.md) | 文本向量化与相似度 | 语义相似度实验 |
| [26](mainline/26_vector_search.md) | Cosine、Top-K 和召回 | 可解释的检索结果 |
| [27](mainline/27_vector_database.md) | PostgreSQL + pgvector | 带元数据过滤的查询 |
| [28](mainline/28_rag_basics.md) | RAG 原理与边界 | 检索、生成分层验收表 |
| [29](mainline/29_document_processing.md) | PDF/Word、解析、切分和清洗 | 可追踪文档块 |
| [30](mainline/30_rag_retrieval.md) | Query、Retrieval、Context | 返回来源的检索接口 |
| [31](mainline/31_rag_generation.md) | 基于证据回答 | 带引用及拒答的回答接口 |
| [32](mainline/32_rag_improvement.md) | Chunk、Top-K、Metadata 和评价 | 一次受控参数实验 |
| [33](mainline/33_enterprise_rag.md) | 权限、版本和企业知识库 | 分用户权限的知识库验收 |

阶段验收：固定问题集分别计算检索命中和回答支持情况；没有证据时拒答，用户不能检索无权访问的文档。

## 阶段五：Agent 与协议（34～39，进阶/选修）

| 章 | 内容 | 可检查成果 |
| --- | --- | --- |
| [34](mainline/34_agent_basics.md) | Agent、Chat、RAG 和 Workflow 的区别 | 为场景选择最小方案 |
| [35](mainline/35_agent_workflow.md) | 判断—工具—结果—再判断 | 有步数和预算上限的循环 |
| [36](mainline/36_agent_tools.md) | DB、API、文件和搜索工具 | 权限分级工具目录 |
| [37](mainline/37_memory.md) | 对话、业务状态和长期记忆 | 数据生命周期设计 |
| [38](mainline/38_mcp.md) | MCP Client、Server、Tool、Resource | 一个只读 MCP 接入设计 |
| [39](mainline/39_multi_agent.md) | 分工、协调和工作流 | 与单 Agent 的评价对照 |

阶段验收：Agent 无法绕过业务权限；写操作需要确认；达到步数、成本或错误阈值时能够停止。

## 阶段六：生产化（40～44，进阶）

| 章 | 内容 | 可检查成果 |
| --- | --- | --- |
| [40](mainline/40_ai_security.md) | Prompt Injection、权限和数据泄漏 | 威胁模型与攻击用例 |
| [41](mainline/41_cost_management.md) | Token、模型、缓存和预算 | 单请求与月度成本估算 |
| [42](mainline/42_observability.md) | Prompt、Response、Token、Error、Latency | 可脱敏的追踪日志 |
| [43](mainline/43_evaluation.md) | 正确率、质量和 RAG Evaluation | 固定评估集与发布门槛 |
| [44](mainline/44_deployment.md) | Docker、配置、部署和回滚 | 可复现部署与恢复演练 |

阶段验收：在无真实密钥的测试环境完成攻击、超时、限流、预算和回滚演练，并留下真实结果。

## 阶段七：综合项目（45～50）

| 章 | 项目 | 核心验收 |
| --- | --- | --- |
| [45](mainline/45_chat_project.md) | AI Chat System | 会话、错误、取消和使用量 |
| [46](mainline/46_rag_project.md) | 企业知识库 RAG | 来源、拒答、权限和评价 |
| [47](mainline/47_document_analysis_project.md) | AI 文档分析系统 | 结构化提取、证据定位和人工确认 |
| [48](mainline/48_matching_project.md) | 案件 × 技术者 AI 匹配 | 可解释评分、偏差检查和人工决策 |
| [49](mainline/49_agent_project.md) | Agent 自动业务处理 | 受控工具、幂等、审计和停止条件 |
| [50](mainline/50_project_review.md) | 项目 Review | 架构、代码、Prompt、RAG、成本和安全总验收 |

其中 45～49 是递进项目包，不要求五套完全独立代码库。建议在同一 ServiceDesk AI 工程中建立功能分支，最终由第 50 章统一验收。
