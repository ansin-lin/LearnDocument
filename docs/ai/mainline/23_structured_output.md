# 第 23 章 Structured Output

> 本章成果：让模型按 Schema 返回工单字段，并完成格式、结构和业务三层校验。

Structured Output 用 Schema 约束输出结构，比“请返回 JSON”更适合程序消费。但结构正确不等于事实正确，也不等于业务允许。

## 1. JSON、JSON Mode 与 Schema

普通提示“返回 JSON”只是自然语言要求；JSON 模式侧重语法可解析；Structured Output 进一步约束字段和类型。具体支持范围依模型与 SDK 而定。即使 Schema 通过，证据仍可能虚构、优先级仍可能违反业务规则。

三层检查必须分开：JSON 能否解析；字段、类型、枚举是否符合 Pydantic；摘要、证据、优先级和权限是否满足业务规则。模型拒绝、截断或响应不完整也要作为正常分支处理。

```python
class TicketExtraction(BaseModel):
    summary: str
    category: Literal["ACCOUNT", "REPORT", "ACCESS", "OTHER"]
    priority_suggestion: Literal["HIGH", "MEDIUM", "LOW"]
    evidence: list[str]
    missing_fields: list[str]
```

实际 SDK 的结构化输出入口按[官方 Structured Outputs 指南](https://developers.openai.com/api/docs/guides/structured-outputs)实现并锁定版本。

## 2. Schema 设计

字段名表达业务含义；枚举限制合法值；必填与可空分开；长度和嵌套不过度复杂。无法判断的情况用明确状态或 `missing_fields` 表达，不让模型用空字符串掩盖缺失。

## 3. 三层校验

格式层检查完整与可解析；结构层由 Pydantic 检查字段类型和枚举；业务层核对摘要长度、evidence 是否来自原文、分类规则和人工确认。失败响应要区分模型拒绝、输出截断和校验错误。

## 4. 失败处理

区分模型拒绝、输出截断、JSON 解析失败、Schema 失败和业务校验失败。可以在总预算内做一次有明确原因的修复请求，连续失败则转人工或返回受控错误。

## 5. 为什么“提示它输出 JSON”还不够

模型可能输出 Markdown 代码块、缺少字段、把数字写成字符串或使用未约定的枚举。JSON Mode 主要保证语法上是 JSON；基于 Schema 的结构化输出还能约束字段、类型和必填项，但仍不能保证业务含义正确。是否支持某种约束取决于所选模型和接口能力。

## 6. Schema 的设计原则

以工单分析结果为例，可定义 `category`、`priority`、`summary`、`needs_human_review` 和 `reason`。字段名应表达业务含义；枚举只允许系统真正支持的值；必填与可空要分清；数组要限制元素结构；说明文字解释判断标准，而不是重复字段名。

不要让一个字段同时表示多件事，例如 `result: "high-network"` 混合了优先级和类别。拆分后更容易验证、查询和修改。

## 7. 三层校验分别做什么

1. **语法与 Schema 校验**：是不是可解析对象，字段和类型是否符合定义；
2. **业务校验**：工单状态、枚举组合、长度和关联编号是否允许；
3. **事实校验**：摘要和理由能否在输入中找到依据。

通过第一层不代表后两层通过。例如 `priority = "urgent"` 可能符合枚举，但如果正文只是密码重置请求，就需要业务复核。

## 8. 失败时如何处理

Schema 不匹配可记录原始响应的安全摘要，并进行一次受控重试；连续失败则转为明确错误或人工处理。不要用正则从破损 JSON 中“尽量抠字段”后直接写库，这会把不确定数据变成正式业务数据。

本章练习要求设计工单分类 Schema，并用缺字段、错误类型、非法枚举、逻辑矛盾和无依据结论五组数据验证处理结果。

不要无限要求模型“修复 JSON”。可以有限地使用明确错误重新请求，但总预算受限；高风险业务应返回人工处理。保存失败样例并加入评估集。

实践：使用[项目规格](../project_spec.md)的固定输入，验证合法结果、缺字段、无依据 evidence 和超长 summary。只有前三层都通过才返回成功。
