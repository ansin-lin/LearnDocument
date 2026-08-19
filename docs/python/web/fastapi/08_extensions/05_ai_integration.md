# 扩展专题5 AI员工资料摘要接口

> 本专题成果：通过OpenAI Responses API生成非敏感员工资料摘要，设置模型、超时、有限重试和错误边界，并用依赖替换完成无外网测试。

> 官方行为检查日期：2026-07-29。示例模型保持环境可配置；上线前应重新核对模型可用性、价格、限额和数据政策。

## 一、业务边界

本专题只使用虚构或经过批准的非敏感字段：

```text
员工编号
部门名称
在职状态
公开的工作说明
```

不要发送密码、Token、私人邮箱、身份证件、未公开人事评价或客户机密。AI输出需要作为建议展示，不能直接成为考勤、录用、解雇或权限决定。

## 二、安装和配置

安装官方Python SDK：

```powershell
python -m pip install openai
```

文件：`app/config.py`  
操作：向`Settings`追加字段  
代码类型：配置代码片段

```python
openai_api_key: str  # 保存服务端OpenAI API Key
openai_model: str = "gpt-5.6-sol"  # 设置可由环境变量覆盖的模型名称
```

环境配置：

```text
OPENAI_API_KEY=
OPENAI_MODEL=gpt-5.6-sol
```

API Key只保存在服务端环境或秘密管理服务中，不能返回前端、写入日志或提交Git。

## 三、封装AI服务

文件：`app/services/ai_summary_service.py`  
操作：新建  
代码类型：完整文件

```python
import openai  # 导入异常类型所在模块
from openai import OpenAI  # 导入同步API客户端

from app.config import settings  # 导入Key、模型和应用配置


class AiServiceUnavailableError(Exception):  # 定义稳定的应用层AI服务异常
    pass  # 当前异常不增加额外字段


class AiSummaryService:  # 定义AiSummaryService类
    def __init__(self) -> None:  # 定义__init__函数
        self.client = OpenAI(  # 设置或保存self.client的值
            api_key=settings.openai_api_key,  # 设置或保存api_key的值
            timeout=20.0,  # 设置或保存timeout的值
            max_retries=2,  # 设置或保存max_retries的值
        )  # 完成当前调用或数据结构

    def summarize(  # 定义summarize函数
        self,  # 接收当前对象
        employee_number: str,  # 接收employee_number参数并声明类型
        department_name: str,  # 接收department_name参数并声明类型
        is_active: bool,  # 接收is_active参数并声明类型
        job_notes: str,  # 接收job_notes参数并声明类型
    ) -> str:  # 声明函数返回值类型
        prompt = (  # 设置或保存prompt的值
            f"员工编号: {employee_number}\n"  # 组成当前文本内容
            f"部门: {department_name}\n"  # 组成当前文本内容
            f"在职: {is_active}\n"  # 组成当前文本内容
            f"工作说明: {job_notes}"  # 组成当前文本内容
        )  # 完成当前调用或数据结构
        try:  # 开始执行可能失败的操作
            response = self.client.responses.create(  # 设置或保存response的值
                model=settings.openai_model,  # 设置或保存model的值
                instructions=(  # 设置或保存instructions的值
                    "请根据输入生成不超过100字的中性工作摘要。"  # 组成当前文本内容
                    "不得推测年龄、健康、家庭、民族或其他敏感属性。"  # 组成当前文本内容
                ),  # 完成当前调用或数据结构
                input=prompt,  # 设置或保存input的值
            )  # 完成当前调用或数据结构
        except (  # 开始列出需要捕获的异常
            openai.APITimeoutError,  # 传入openai.APITimeoutError的值
            openai.APIConnectionError,  # 传入openai.APIConnectionError的值
            openai.RateLimitError,  # 传入openai.RateLimitError的值
            openai.APIStatusError,  # 传入openai.APIStatusError的值
        ) as exc:  # 将捕获的异常保存到变量
            raise AiServiceUnavailableError from exc  # 抛出稳定的应用异常
        return response.output_text  # 返回当前处理结果
```

`OpenAI()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `api_key` | API Key 字符串、返回字符串的函数或 `None` | 默认读取 `OPENAI_API_KEY` | 设置服务端调用 API 使用的密钥 |
| `timeout` | 秒数、`httpx.Timeout` 对象或 `None` | SDK 默认 10 分钟 | 限制单次请求等待时间；本示例设置为 20 秒 |
| `max_retries` | 非负整数 | SDK 默认 `2` | 设置连接错误、超时和部分可重试状态的最大自动重试次数 |

`responses.create()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `model` | 当前账号可使用的模型 ID 字符串 | 必填 | 指定生成摘要的模型 |
| `instructions` | 指令字符串或 `None` | 默认 `None` | 规定输出目标、限制和处理原则 |
| `input` | 字符串或结构化输入项列表 | 必填 | 提交需要处理的员工业务信息 |

SDK 默认会自动重试部分临时错误两次，本例显式保留该上限并把超时设为20秒。不要在 Router 外再写无限重试循环。

## 四、请求与响应模型

文件：`app/schemas.py`  
操作：追加  
代码类型：项目代码片段

```python
from pydantic import BaseModel, Field  # 导入模型基类和字段约束工具


class EmployeeSummaryRequest(BaseModel):  # 定义摘要请求体
    job_notes: str = Field(  # 接收公开的工作说明
        min_length=1,  # 至少输入1个字符
        max_length=1000,  # 最多输入1000个字符
    )  # 结束字段约束


class EmployeeSummaryResponse(BaseModel):  # 定义摘要响应结构
    summary: str  # 返回生成的摘要文本
    generated_by_ai: bool = True  # 明确标记内容由AI生成
```

限制输入长度可以控制费用、延迟和滥用范围，但不能代替权限、内容政策和用量限制。

## 五、创建接口

文件：`app/routers/employees.py`  
操作：追加受保护接口  
代码类型：项目代码片段

```python
from fastapi import Depends, HTTPException  # 导入依赖和HTTP异常工具
from sqlalchemy.orm import Session  # 导入Session类型

from app.dependencies import get_db  # 从app.dependencies模块导入get_db
from app.services.ai_summary_service import (  # 从AI摘要服务模块导入服务类和异常类
    AiServiceUnavailableError,  # 传入AiServiceUnavailableError参数
    AiSummaryService,  # 传入AiSummaryService参数
)  # 完成当前调用或数据结构


@router.post(  # 为下面的函数注册框架行为
    "/{employee_number}/ai-summary",  # 组成当前文本内容
    response_model=EmployeeSummaryResponse,  # 设置或保存response_model的值
)  # 完成当前调用或数据结构
def create_ai_summary(  # 定义create_ai_summary函数
    employee_number: str,  # 接收employee_number参数并声明类型
    payload: EmployeeSummaryRequest,  # 接收payload参数并声明类型
    db: Session = Depends(get_db),  # 接收db参数并声明类型
    current_user=Depends(require_admin),  # 设置或保存current_user的值
):  # 结束参数列表并开始生成员工摘要
    employee = EmployeeService(db).get_employee(  # 设置或保存employee的值
        employee_number  # 传入employee_number参数
    )  # 完成当前调用或数据结构
    try:  # 开始执行可能失败的操作
        summary = AiSummaryService().summarize(  # 设置或保存summary的值
            employee.employee_number,  # 传入employee.employee_number的值
            employee.department.name,  # 传入employee.department.name的值
            employee.is_active,  # 传入employee.is_active的值
            payload.job_notes,  # 传入payload.job_notes的值
        )  # 完成当前调用或数据结构
    except AiServiceUnavailableError as exc:  # 捕获应用层AI服务异常
        raise HTTPException(  # 抛出稳定的应用异常
            status_code=503,  # 设置或保存status_code的值
            detail="AI服务暂时不可用",  # 设置或保存detail的值
        ) from exc  # 保留原异常链并返回稳定HTTP错误
    return EmployeeSummaryResponse(summary=summary)  # 返回当前处理结果
```

接口先完成认证、授权和员工存在检查，再调用外部服务。不要把供应商异常、请求内容或API Key直接返回调用方。

## 六、测试替换

自动测试不调用真实付费API。把AI服务作为依赖或可替换对象，测试时返回固定摘要：

文件：`tests/fakes.py`  
操作：新建  
代码类型：测试替身片段

```python
class FakeAiSummaryService:  # 定义不访问网络的AI服务测试替身
    def summarize(  # 保持与真实服务相同的方法名称和参数
        self,  # 接收当前对象
        employee_number: str,  # 接收employee_number参数并声明类型
        department_name: str,  # 接收department_name参数并声明类型
        is_active: bool,  # 接收is_active参数并声明类型
        job_notes: str,  # 接收job_notes参数并声明类型
    ) -> str:  # 声明函数返回值类型
        return "开发部员工，当前在职。"  # 返回固定结果，保证测试稳定
```

测试替身保留真实`AiSummaryService.summarize()`的四个业务参数，使测试能发现调用契约变化；每个参数的类型、顺序和含义与真实服务一致。

至少验证：

- 未认证返回`401`。
- 普通角色返回`403`。
- 不存在员工返回`404`。
- 输入超过1000字符返回`422`。
- AI服务失败转换为`503`。
- 成功响应明确标记`generated_by_ai=true`。

## 七、上线检查

- 记录调用次数、延迟、失败率和估算用量，不记录完整敏感Prompt。
- 为用户、接口或组织设置速率和预算限制。
- 把模型名放在配置中，并用代表性样例验证升级。
- 明确AI输出可能不准确，保留人工确认和纠正入口。
- 根据项目所在地、合同和数据分类确认是否允许发送相关数据。

参考：[OpenAI API快速开始](https://platform.openai.com/docs/quickstart/make-your-first-api-request)、[OpenAI Python SDK](https://github.com/openai/openai-python)、[当前模型指南](https://developers.openai.com/api/docs/guides/latest-model)。
