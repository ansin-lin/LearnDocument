# 第13章: 文件格式、办公文档与自动化常用包详解

> 🎯 本章目标  
>
> - 了解用于 CSV、Excel、Word、PDF、图像、邮件处理的常用 Python 包
> - 掌握每个包的功能、常用方法、参数说明与 Web 系统应用场景

---

## 🟢 一、CSV 文件处理（csv 模块）

### 功能简介

- 读写 `.csv` 文件（表格型文本文件）
- 内置模块，无需额外安装
- 与 Excel 兼容

### 常用函数与类

| 函数 / 类 | 功能 |
|------------|------|
| `csv.reader(file)` | 读取 CSV 文件为列表 |
| `csv.writer(file)` | 写入 CSV 文件 |
| `csv.DictReader(file)` | 按列名读取为字典 |
| `csv.DictWriter(file, fieldnames)` | 按列名写入数据 |

### 常用参数

| 参数 | 说明 |
|------|------|
| `delimiter` | 分隔符，默认`,` |
| `newline` | 防止写入空行 |
| `encoding` | 文件编码 |

### 示例

```python
import csv

with open("users.csv", "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=["id", "name", "email"])
    writer.writeheader()
    writer.writerow({"id": 1, "name": "Tom", "email": "tom@mail.com"})

with open("users.csv", "r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        print(row["name"], row["email"])
```

✅ Web 应用场景：批量导入用户、导出订单报表。

---

## 🟣 二、Excel 文件处理（openpyxl 与 pandas）

### openpyxl

#### 功能

- 读写 `.xlsx` 文件（Excel 2007+）
- 可创建、修改、格式化单元格

#### 常用类与方法

| 类 / 方法 | 功能 |
|-------------|------|
| `Workbook()` | 创建新工作簿 |
| `load_workbook()` | 读取已有文件 |
| `wb.active` | 获取当前表 |
| `ws.append(list)` | 追加行数据 |
| `ws.cell(row, column, value)` | 按行列访问单元格 |
| `wb.save()` | 保存文件 |

#### 示例

```python
from openpyxl import Workbook

wb = Workbook()
ws = wb.active
ws.title = "Scores"
ws.append(["姓名", "分数"])
ws.append(["Tom", 90])
wb.save("scores.xlsx")
```

#### 样式设置

```python
from openpyxl.styles import Font, PatternFill
ws["A1"].font = Font(bold=True, color="FFFFFF")
ws["A1"].fill = PatternFill(start_color="000000", end_color="000000", fill_type="solid")
```

✅ Web 应用：生成 Excel 报表、财务导出。

---

### pandas

#### 功能

- 高级表格数据分析与导入导出
- 支持 CSV、Excel、JSON、SQL 等格式

#### 常用方法

| 方法 | 功能 |
|------|------|
| `pd.read_csv()` | 读取 CSV |
| `pd.read_excel()` | 读取 Excel |
| `df.to_csv()` | 导出 CSV |
| `df.to_excel()` | 导出 Excel |
| `df.describe()` | 获取统计信息 |

#### 示例

```python
import pandas as pd

df = pd.read_excel("scores.xlsx")
df["平均分"] = df["分数"].mean()
df.to_excel("summary.xlsx", index=False)
```

✅ Web 应用：后台管理系统导入导出、数据统计。

---

## 🟡 三、Word 文档处理（python-docx）

### 功能简介

- 创建和编辑 `.docx` 文件
- 支持段落、表格、图片等元素

### 常用类与方法

| 方法 | 功能 |
|------|------|
| `Document()` | 创建文档对象 |
| `doc.add_paragraph()` | 添加段落 |
| `doc.add_heading()` | 添加标题 |
| `doc.add_table(rows, cols)` | 添加表格 |
| `doc.add_picture(path, width)` | 添加图片 |
| `doc.save()` | 保存文件 |

### 示例

```python
from docx import Document

doc = Document()
doc.add_heading("项目周报", 0)
doc.add_paragraph("完成模块：用户登录、数据导出")
doc.save("weekly_report.docx")
```

✅ Web 应用：自动生成合同、报告、模板。

---

## 🔵 四、PDF 文件处理（reportlab 与 pdfplumber）

### reportlab

#### 功能

- 生成 PDF 文件
- 支持文本、图像、图表、绘图

#### 常用方法

| 方法 | 功能 |
|------|------|
| `canvas.Canvas(file)` | 创建 PDF 文件对象 |
| `drawString(x, y, text)` | 写入文字 |
| `drawImage()` | 插入图片 |
| `save()` | 保存 |

#### 示例

```python
from reportlab.pdfgen import canvas
c = canvas.Canvas("output.pdf")
c.drawString(100, 750, "销售报告")
c.save()
```

✅ Web 应用：自动生成发票、分析报告。

---

### pdfplumber

#### 功能

- 提取 PDF 文本与表格

#### 常用方法

| 方法 | 功能 |
|------|------|
| `pdfplumber.open(file)` | 打开 PDF |
| `page.extract_text()` | 提取文本 |
| `page.extract_tables()` | 提取表格 |

#### 示例

```python
import pdfplumber

with pdfplumber.open("output.pdf") as pdf:
    text = pdf.pages[0].extract_text()
    print(text)
```

✅ Web 应用：解析上传的合同、简历内容。

---

## 🟠 五、图像处理（Pillow）

### 功能简介

- 图像读写、裁剪、压缩、添加水印等
- 常用于图片上传与展示

### 常用方法

| 方法 | 功能 |
|------|------|
| `Image.open(path)` | 打开图片 |
| `img.resize(size)` | 调整大小 |
| `img.crop(box)` | 裁剪 |
| `img.convert(mode)` | 转换模式 |
| `img.save(path)` | 保存 |
| `ImageDraw.Draw()` | 绘图对象 |

#### 示例

```python
from PIL import Image, ImageDraw

img = Image.open("avatar.jpg").resize((200, 200))
draw = ImageDraw.Draw(img)
draw.text((10, 10), "Demo", fill="red")
img.save("result.jpg")
```

✅ Web 应用：头像处理、缩略图、水印。

---

## 🔴 六、邮件发送（smtplib + email.mime）

### 功能

- 通过 SMTP 协议发送邮件
- 支持 HTML、附件、多收件人

### 常用类

| 类 | 功能 |
|------|------|
| `SMTP()` | 创建 SMTP 客户端 |
| `MIMEText()` | 创建文本邮件 |
| `MIMEMultipart()` | 多部分邮件 |
| `send_message()` | 发送邮件 |

#### 示例

```python
import smtplib
from email.mime.text import MIMEText

msg = MIMEText("系统通知：任务完成", "plain", "utf-8")
msg["From"] = "sender@example.com"
msg["To"] = "receiver@example.com"
msg["Subject"] = "通知"

server = smtplib.SMTP("smtp.example.com", 25)
server.login("sender@example.com", "password")
server.send_message(msg)
server.quit()
```

✅ Web 应用：系统通知、用户验证、报表推送。

---

## 🧮 七、常用包汇总表

| 模块 | 用途 | 是否标准库 | 安装命令 |
|------|------|-------------|----------|
| csv | 读写 CSV 文件 | ✅ | 内置 |
| openpyxl | 读写 Excel | ❌ | `pip install openpyxl` |
| pandas | 高级表格处理 | ❌ | `pip install pandas` |
| python-docx | Word 文档 | ❌ | `pip install python-docx` |
| reportlab | 生成 PDF | ❌ | `pip install reportlab` |
| pdfplumber | 解析 PDF | ❌ | `pip install pdfplumber` |
| Pillow | 图像处理 | ❌ | `pip install pillow` |
| smtplib/email | 邮件发送 | ✅ | 内置 |

---

## ✅ 小结

| 应用场景 | 推荐模块 | 功能说明 |
|-----------|-----------|-----------|
| 批量导入导出 | csv, openpyxl, pandas | 表格数据操作 |
| 报告生成 | python-docx, reportlab | 自动生成报告 |
| 图像管理 | Pillow | 上传与水印 |
| 通知系统 | smtplib, email.mime | 邮件发送 |
| 数据分析 | pandas | 后端统计接口 |

---
