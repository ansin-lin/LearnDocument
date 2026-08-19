# 扩展专题4 Amazon S3 私有文件下载

> 本专题成果：为员工报告生成短期有效的S3预签名下载URL，保持Bucket私有，并明确认证、对象Key、费用和清理边界。

> 官方行为检查日期：2026-07-29。AWS控制台、价格、配额和安全建议可能变化，实际项目应重新核对官方文档。

## 一、前置条件

开始前需要：

- 完成第20章认证授权。
- 有一套允许练习的AWS账号和Region。
- 有一个专用测试Bucket和测试对象。
- 了解S3存储、请求和数据传输可能产生费用。

练习和验证必须使用独立测试 Bucket，不能使用生产 Bucket、管理员长期密钥或公开读权限。

## 二、为什么使用预签名URL

```text
前端请求FastAPI
→ FastAPI验证登录、权限和对象归属
→ 使用应用IAM身份生成短期URL
→ 前端在有效期内直接从S3下载
```

Bucket和对象保持私有。预签名URL是临时Bearer凭证，拿到URL的人在有效期内可能访问对象，因此不能记录完整URL或把有效期设置得过长。

## 三、安装和配置

安装AWS SDK：

```powershell
python -m pip install boto3
```

文件：`app/config.py`  
操作：向`Settings`追加字段  
代码类型：配置代码片段

```python
aws_region: str  # 保存AWS Region名称
s3_bucket: str  # 保存专用私有Bucket名称
```

`.env.example`只记录非秘密配置：

```text
AWS_REGION=ap-northeast-1
S3_BUCKET=replace-with-training-bucket
```

Boto3使用标准凭证链。部署环境优先使用IAM Role和临时凭证，不把`AWS_ACCESS_KEY_ID`、`AWS_SECRET_ACCESS_KEY`写进代码、镜像或Git。

## 四、创建S3服务

文件：`app/services/storage_service.py`  
操作：新建  
代码类型：完整文件

```python
import boto3  # 导入AWS Python SDK
from botocore.exceptions import BotoCoreError, ClientError  # 导入SDK基础错误和服务错误

from app.config import settings  # 导入Region和Bucket配置


class StorageUnavailableError(Exception):  # 定义稳定的应用层文件服务异常
    pass  # 当前异常不增加额外字段


class StorageService:  # 封装S3下载URL生成逻辑
    def __init__(self) -> None:  # 创建服务时初始化S3客户端
        self.client = boto3.client(  # 创建低级S3客户端
            "s3",  # 指定服务名称
            region_name=settings.aws_region,  # 指定请求Region
        )  # 完成客户端创建

    def create_download_url(  # 定义预签名下载URL生成方法
        self,  # 接收当前对象
        object_key: str,  # 接收已经由服务端确认的对象Key
    ) -> str:  # 返回临时下载URL
        try:  # 捕获SDK和AWS服务错误
            return self.client.generate_presigned_url(  # 生成不公开Bucket的临时URL
                ClientMethod="get_object",  # 允许执行下载对象操作
                Params={  # 提供get_object需要的参数
                    "Bucket": settings.s3_bucket,  # 使用配置中的Bucket
                    "Key": object_key,  # 指定对象Key
                },  # 结束操作参数
                ExpiresIn=300,  # 设置5分钟有效期
            )  # 返回生成的URL
        except (BotoCoreError, ClientError) as exc:  # 捕获本地SDK或远端服务错误
            raise StorageUnavailableError from exc  # 转换为应用稳定异常
```

`boto3.client()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `service_name` | AWS 服务名称字符串 | 必填；本示例为 `"s3"` | 创建指定 AWS 服务的低级客户端 |
| `region_name` | AWS Region 名称字符串或 `None` | 默认从环境和 AWS 配置读取 | 指定请求发送到的 Region |
| `endpoint_url` | HTTP(S) URL 或 `None` | 默认使用 AWS 服务端点 | 测试时可连接兼容 S3 的本地服务 |

`generate_presigned_url()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `ClientMethod` | S3 客户端方法名称字符串 | 必填 | 指定预签名 URL 要执行的操作 |
| `Params` | 与客户端方法参数一致的字典 | 默认 `None` | 提供 Bucket、Key 等操作参数 |
| `ExpiresIn` | 正整数秒数 | 默认 `3600` | 设置预签名 URL 的有效时间 |
| `HttpMethod` | HTTP 方法字符串或 `None` | 默认由客户端方法决定 | 必要时覆盖生成 URL 使用的 HTTP 方法 |

有效期使用300秒。生成URL不代表对象一定存在；真正下载时仍会检查对象、凭证有效期、IAM和Bucket策略。

## 五、创建受保护接口

文件：`app/schemas.py`  
操作：追加  
代码类型：项目代码片段

```python
from pydantic import BaseModel  # 导入Pydantic模型基类


class DownloadUrlResponse(BaseModel):  # 定义下载URL响应结构
    url: str  # 返回短期有效URL
    expires_in: int  # 返回有效秒数
```

文件：`app/routers/employees.py`  
操作：追加受保护接口  
代码类型：项目代码片段

```python
from fastapi import Depends, HTTPException  # 导入依赖和HTTP异常工具

from app.schemas import DownloadUrlResponse  # 从app.schemas模块导入DownloadUrlResponse
from app.services.storage_service import (  # 从存储服务模块导入服务类和异常类
    StorageService,  # 传入StorageService参数
    StorageUnavailableError,  # 传入StorageUnavailableError参数
)  # 完成当前调用或数据结构


@router.get(  # 为下面的函数注册框架行为
    "/{employee_number}/report-url",  # 组成当前文本内容
    response_model=DownloadUrlResponse,  # 设置或保存response_model的值
)  # 完成当前调用或数据结构
def get_employee_report_url(  # 定义get_employee_report_url函数
    employee_number: str,  # 接收employee_number参数并声明类型
    current_user=Depends(get_current_user),  # 设置或保存current_user的值
):  # 结束参数列表并开始生成下载地址
    object_key = (  # 设置或保存object_key的值
        f"employee-reports/{employee_number}.pdf"  # 组成当前文本内容
    )  # 完成当前调用或数据结构
    try:  # 开始执行可能失败的操作
        url = StorageService().create_download_url(  # 设置或保存url的值
            object_key  # 传入object_key参数
        )  # 完成当前调用或数据结构
    except StorageUnavailableError as exc:  # 捕获对象存储服务不可用异常
        raise HTTPException(  # 抛出稳定的应用异常
            status_code=503,  # 设置或保存status_code的值
            detail="文件服务暂时不可用",  # 设置或保存detail的值
        ) from exc  # 保留原异常链并返回稳定HTTP错误
    return DownloadUrlResponse(  # 返回当前处理结果
        url=url,  # 设置或保存url的值
        expires_in=300,  # 设置或保存expires_in的值
    )  # 完成当前调用或数据结构
```

正式项目必须在生成URL前查询员工与附件记录，并验证当前用户是否有权访问该对象。不能直接接受调用方提交的任意S3 Key。

## 六、最小权限

生成下载URL的应用角色只需要目标前缀的必要权限，例如只允许：

```text
s3:GetObject
arn:aws:s3:::example-training-bucket/employee-reports/*
```

不要授予 `s3:*`，不要公开Bucket，也不要让前端持有AWS凭证。URL拥有的能力不会超过生成它的IAM身份。

## 七、验证与清理

1. 向测试Bucket的`employee-reports/`前缀上传一个虚构PDF。
2. 未登录请求接口，确认返回`401`。
3. 有权限用户取得URL并在5分钟内下载。
4. 修改对象Key或等待过期，确认下载失败。
5. 确认应用日志不包含完整预签名URL。
6. 练习结束后删除测试对象；不再使用的测试Bucket也应删除，并确认没有版本、分段上传或生命周期资源残留。

云端步骤无法由本地文档审计证明成功，必须保留Region、IAM主体、对象Key、验证结果和清理结果。

参考：[AWS S3预签名URL](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html)、[AWS最小权限建议](https://docs.aws.amazon.com/prescriptive-guidance/latest/presigned-url-best-practices/foundational-best-practices.html)。
