# 第11章 文件、CSV 与 Excel

> 本章目标：掌握 FastAPI 中上传文件、下载文件、CSV 导出和 Excel 处理的基础写法。

## 一、常见文件场景

| 场景 | 说明 |
| --- | --- |
| 文件上传 | 上传附件、图片、资料 |
| 文件下载 | 下载模板、下载处理结果 |
| CSV 导出 | 导出员工列表、统计结果 |
| Excel 处理 | 生成报表、读取业务表格 |

## 二、文件上传

```python
from fastapi import APIRouter, File, UploadFile  # 导入文件上传相关对象

router = APIRouter(prefix="/files", tags=["files"])  # 创建文件路由


@router.post("/upload")  # 注册上传接口
async def upload_file(file: UploadFile = File(...)):  # 接收上传文件
    return {  # 返回文件信息
        "filename": file.filename,  # 原始文件名
        "content_type": file.content_type,  # 文件类型
    }
```

`UploadFile` 常用内容：

| 内容 | 作用 |
| --- | --- |
| `filename` | 原始文件名 |
| `content_type` | 文件类型 |
| `file` | 底层文件对象 |
| `read()` | 读取文件内容 |

## 三、保存上传文件

```python
from pathlib import Path  # 导入 Path
from uuid import uuid4  # 导入 uuid4
from fastapi import UploadFile  # 导入 UploadFile


async def save_upload_file(file: UploadFile):  # 定义保存上传文件函数
    upload_dir = Path("uploads")  # 设置上传目录
    upload_dir.mkdir(exist_ok=True)  # 如果目录不存在则创建
    suffix = Path(file.filename).suffix  # 获取文件后缀
    save_name = f"{uuid4().hex}{suffix}"  # 生成唯一文件名
    save_path = upload_dir / save_name  # 拼接保存路径
    content = await file.read()  # 读取文件内容
    save_path.write_bytes(content)  # 写入文件
    return save_name  # 返回保存后的文件名
```

真实项目需要限制文件大小、类型和访问权限。

## 四、文件下载

```python
from pathlib import Path  # 导入 Path
from fastapi import HTTPException  # 导入 HTTPException
from fastapi.responses import FileResponse  # 导入 FileResponse


@router.get("/download/{file_name}")  # 注册下载接口
def download_file(file_name: str):  # 接收文件名
    file_path = Path("uploads") / file_name  # 拼接文件路径
    if not file_path.exists():  # 判断文件是否存在
        raise HTTPException(status_code=404, detail="文件不存在")  # 不存在返回 404
    return FileResponse(file_path, filename=file_name)  # 返回文件下载响应
```

## 五、CSV 导出

```python
import csv  # 导入 csv
from io import StringIO  # 导入 StringIO
from fastapi.responses import Response  # 导入 Response


@router.get("/employees.csv")  # 注册员工 CSV 导出接口
def export_employees_csv():  # 定义导出函数
    output = StringIO()  # 创建内存文本缓冲区
    writer = csv.writer(output)  # 创建 CSV 写入对象
    writer.writerow(["员工编号", "姓名", "部门"])  # 写入表头
    writer.writerow(["E001", "Tanaka", "開発部"])  # 写入示例数据
    content = "\ufeff" + output.getvalue()  # 添加 BOM，方便 Excel 打开中文和日文
    return Response(content=content, media_type="text/csv")  # 返回 CSV 响应
```

## 六、Excel 基础

Excel 处理可以使用 `openpyxl`。

```powershell
pip install openpyxl  # 安装 Excel 处理库
```

生成 Excel：

```python
from openpyxl import Workbook  # 导入 Workbook

workbook = Workbook()  # 创建工作簿
sheet = workbook.active  # 获取默认工作表
sheet.title = "员工列表"  # 设置工作表名称
sheet.append(["员工编号", "姓名", "部门"])  # 写入表头
sheet.append(["E001", "Tanaka", "開発部"])  # 写入数据
workbook.save("employees.xlsx")  # 保存 Excel 文件
```

## 七、基础练习

请完成：

1. 上传一个文件并返回文件名
2. 保存上传文件
3. 下载已保存文件
4. 导出员工 CSV
5. 生成员工 Excel

## 八、本章总结

- `UploadFile` 用于接收上传文件
- `FileResponse` 用于下载文件
- CSV 可使用标准库 `csv`
- Excel 可使用 `openpyxl`
- 文件功能必须考虑大小、类型、文件名和权限
