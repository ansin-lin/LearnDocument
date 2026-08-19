# 扩展专题1 文件、CSV 与 Excel

> 本章目标：掌握 FastAPI 中上传文件、下载文件、CSV 导出和 Excel 处理的基础写法。

文件与报表接口不改变员工核心模型。上传时限制大小和类型、使用服务器生成名称，并在下载时验证访问范围。

## 一、常见文件场景

| 场景 | 说明 |
| --- | --- |
| 文件上传 | 上传附件、图片、资料 |
| 文件下载 | 下载模板、下载处理结果 |
| CSV 导出 | 导出员工列表、统计结果 |
| Excel 处理 | 生成报表、读取业务表格 |

## 二、文件上传

文件：`app/routers/files.py`  
操作：新建  
代码类型：项目代码片段

```python
from fastapi import APIRouter, File, UploadFile  # 导入文件上传相关对象

router = APIRouter(prefix="/files", tags=["files"])  # 创建文件路由


@router.post("/upload")  # 注册上传接口
async def upload_file(file: UploadFile = File(...)):  # 接收上传文件
    return {  # 返回文件信息
        "filename": file.filename,  # 原始文件名
        "content_type": file.content_type,  # 文件类型
    }  # 完成当前调用或数据结构
```

`UploadFile` 常用内容：

| 内容 | 作用 |
| --- | --- |
| `filename` | 原始文件名 |
| `content_type` | 文件类型 |
| `file` | 底层文件对象 |
| `read()` | 读取文件内容 |

`File()` 用于声明上传文件参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `default` | `...`、默认值或 `None` | 本示例为 `...`，表示必填 | 设置文件是否必填以及缺省值 |
| `description` | 字符串或 `None` | 默认 `None` | 在 OpenAPI 文档中说明文件用途 |
| `media_type` | MIME 类型字符串 | 默认 `"multipart/form-data"` | 声明请求体的媒体类型 |

## 三、保存上传文件

文件：`app/services/file_service.py`  
操作：新建  
代码类型：项目代码片段

```python
from pathlib import Path  # 导入 Path
from uuid import uuid4  # 导入 uuid4
from fastapi import HTTPException, UploadFile  # 从fastapi模块导入HTTPException, UploadFile


async def save_upload_file(file: UploadFile):  # 定义保存上传文件函数
    upload_dir = Path("uploads")  # 设置上传目录
    upload_dir.mkdir(exist_ok=True)  # 如果目录不存在则创建
    suffix = Path(file.filename or "").suffix.lower()  # 设置或保存suffix的值
    if suffix not in {".pdf"}:  # 判断当前条件是否成立
        raise HTTPException(status_code=400, detail="只允许 PDF 文件")  # 抛出稳定的应用异常

    save_name = f"{uuid4().hex}{suffix}"  # 生成唯一文件名
    save_path = upload_dir / save_name  # 拼接保存路径
    total_size = 0  # 设置或保存total_size的值

    with save_path.open("wb") as output:  # 在上下文中管理当前资源
        while chunk := await file.read(1024 * 1024):  # 每次异步读取最多1 MB内容
            total_size += len(chunk)  # 累加已经读取的文件大小
            if total_size > 5 * 1024 * 1024:  # 判断当前条件是否成立
                output.close()  # 调用output.close()
                save_path.unlink(missing_ok=True)  # 调用save_path.unlink()
                raise HTTPException(status_code=413, detail="文件超过 5 MB")  # 抛出稳定的应用异常
            output.write(chunk)  # 调用output.write()

    return save_name  # 返回保存后的文件名
```

`await file.read(size)`异步读取上传内容并返回`bytes`。`size`接受非负整数或`-1`：正整数最多读取指定字节数，`-1`读取剩余全部内容；默认是`-1`。本例每次读取`1024 * 1024`字节，便于累计大小并限制为5 MB，不能用一次读取全部内容代替大小控制。

扩展名和客户端提供的 `content_type` 都不能单独证明文件内容安全。生产项目还应检查文件签名、恶意内容和访问权限。

## 四、文件下载

文件：`app/routers/files.py`  
操作：追加  
代码类型：项目代码片段

```python
from pathlib import Path  # 导入 Path
from fastapi import HTTPException  # 导入 HTTPException
from fastapi.responses import FileResponse  # 导入 FileResponse


@router.get("/download/{file_id}")  # 注册下载接口
def download_file(file_id: str):  # 定义download_file函数
    if not file_id.isalnum():  # 判断当前条件是否成立
        raise HTTPException(status_code=400, detail="文件编号不合法")  # 抛出稳定的应用异常

    upload_dir = Path("uploads").resolve()  # 设置或保存upload_dir的值
    file_path = (upload_dir / f"{file_id}.pdf").resolve()  # 设置或保存file_path的值
    if not file_path.is_relative_to(upload_dir):  # 判断当前条件是否成立
        raise HTTPException(status_code=400, detail="文件路径不合法")  # 抛出稳定的应用异常
    if not file_path.is_file():  # 判断当前条件是否成立
        raise HTTPException(status_code=404, detail="文件不存在")  # 不存在返回 404
    return FileResponse(file_path, filename=f"{file_id}.pdf")  # 返回当前处理结果
```

`FileResponse()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `path` | 文件路径字符串或路径对象 | 必填 | 指定要传输的服务器文件 |
| `filename` | 下载文件名字符串或 `None` | 默认 `None` | 设置浏览器保存文件时使用的名称 |
| `media_type` | MIME 类型字符串或 `None` | 默认根据文件名推断 | 设置响应的 Content-Type |
| `headers` | 响应头字典或 `None` | 默认 `None` | 添加额外响应头 |

## 五、CSV 导出

文件：`app/routers/files.py`  
操作：追加  
代码类型：项目代码片段

```python
import csv  # 导入 csv
from io import StringIO  # 导入 StringIO
from fastapi.responses import Response  # 导入 Response


@router.get("/employees.csv")  # 完整路径为 /api/files/employees.csv
def export_employees_csv():  # 定义导出函数
    output = StringIO()  # 创建内存文本缓冲区
    writer = csv.writer(output)  # 创建 CSV 写入对象
    writer.writerow(["员工编号", "姓名", "部门", "入职日期", "在职"])  # 调用writer.writerow()
    writer.writerow(["E001", "Tanaka", "开发部", "2024-04-01", "true"])  # 调用writer.writerow()
    content = "\ufeff" + output.getvalue()  # 添加 BOM，方便 Excel 打开中文和日文
    return Response(content=content, media_type="text/csv")  # 返回 CSV 响应
```

`csv.writer(output)` 创建把数据写入文本缓冲区的 CSV 写入器，`writerow()` 每次写入一行。`Response()` 的 `content` 接收响应正文，`media_type="text/csv"` 告诉调用方响应内容是 CSV。

## 六、Excel 基础

Excel 处理可以使用 `openpyxl`。

```powershell
pip install openpyxl  # 安装 Excel 处理库
```

文件：`app/report_demo.py`  
操作：新建并独立运行  
代码类型：完整实验文件

```python
from openpyxl import Workbook  # 导入 Workbook

workbook = Workbook()  # 创建工作簿
sheet = workbook.active  # 获取默认工作表
sheet.title = "员工列表"  # 设置工作表名称
sheet.append(["员工编号", "姓名", "部门"])  # 写入表头
sheet.append(["E001", "Tanaka", "開発部"])  # 写入数据
workbook.save("employees.xlsx")  # 保存 Excel 文件
```

`Workbook()` 创建一个新工作簿，`workbook.active` 取得当前工作表，`append()` 在末尾写入一行，`save()` 接收目标文件路径并保存。生成文件后应确认工作表名称、表头、数据行和字符显示正确。

## 七、基础练习

请完成：

1. 上传一个文件并返回文件名
2. 保存上传文件
3. 下载已保存文件
4. 导出与员工模型字段一致的 CSV
5. 生成员工 Excel
6. 上传超过 5 MB 或非 PDF 文件，确认被拒绝且没有残留半文件
7. 使用包含路径分隔符的下载参数，确认不能读取上传目录外文件

## 八、本章总结

- `UploadFile` 用于接收上传文件
- `FileResponse` 用于下载文件
- CSV 可使用标准库 `csv`
- Excel 可使用 `openpyxl`
- 文件功能必须考虑大小、类型、文件名和权限
