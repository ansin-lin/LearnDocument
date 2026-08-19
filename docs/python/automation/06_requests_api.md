# 接口调用自动化

这一章不讲 Web 开发，只讲自动化脚本如何调用已有接口。

在日本项目中，自动化脚本经常需要和其他系统联动：

- 从外部系统拉取数据。
- 查询批处理状态。
- 上传处理结果。
- 发送通知。
- 调用内部系统的业务接口。

学完本章后，你要能使用 `requests` 完成 GET、POST、请求头、参数、JSON 解析、超时、异常处理和有限重试。

## 一、安装和导入

```powershell
pip install requests
```

```python
import requests
```

`requests` 是 HTTP 客户端库。自动化脚本调用接口时必须设置 `timeout`，不能让请求无限等待。

## 二、GET 请求：调用查询接口

```python
import requests

# 作用：调用 GET 查询接口
# 使用场景：从其他系统获取指定日期的数据
response = requests.get(
    url="https://api.example.com/orders",
    params={"target_date": "2026-07-26"},
    timeout=10,
)

print(response.status_code)  # 例如：200 表示请求成功
```

常用参数：

| 参数 | 含义 | 使用场景 |
| --- | --- | --- |
| `url` | 接口地址 | 指定调用哪个接口 |
| `params` | 查询参数 | GET 请求条件 |
| `headers` | 请求头 | Token、Content-Type |
| `timeout` | 超时时间 | 自动化脚本必须设置 |

## 三、`response.json()`：读取 JSON 响应

```python
# 作用：把接口返回的 JSON 转成 Python 对象
# 使用场景：接口返回订单列表、处理状态、错误信息时
data = response.json()

print(type(data))  # 例如：<class 'dict'>
```

如果接口返回：

```json
{
  "status": "success",
  "items": [
    {"order_id": "O001", "amount": 1000}
  ]
}
```

可以这样读取：

```python
print(data["status"])  # 例如：success
print(len(data["items"]))  # 例如：1
```

## 四、POST 请求：提交处理结果

```python
# 作用：调用 POST 接口提交处理结果
# 使用场景：自动化脚本执行完成后，把状态回传给其他系统
payload = {
    "job_name": "daily_sales_report",
    "target_date": "2026-07-26",
    "status": "success",
    "row_count": 56090,
}

response = requests.post(
    url="https://api.example.com/job-results",
    json=payload,
    timeout=10,
)

print(response.status_code)  # 例如：201 表示创建成功
```

POST 请求常用参数：

| 参数 | 含义 | 使用场景 |
| --- | --- | --- |
| `json` | JSON 请求体 | 提交处理结果 |
| `data` | 表单或原始数据 | 对方接口不是 JSON 时 |
| `files` | 文件上传 | 上传报表或附件 |
| `timeout` | 超时时间 | 防止脚本卡死 |

## 五、请求头和 Token

```python
# 作用：设置请求头
# 使用场景：接口需要认证 Token 或指定 JSON 格式时
headers = {
    "Authorization": "Bearer YOUR_TOKEN",
    "Content-Type": "application/json",
}

response = requests.get(
    url="https://api.example.com/orders",
    headers=headers,
    params={"target_date": "2026-07-26"},
    timeout=10,
)

print(response.status_code)  # 例如：200
```

注意：不要把真实 Token 写进代码或提交到 Git。正式项目通常从环境变量、配置文件或安全管理工具读取。

## 六、`raise_for_status()`：检查 HTTP 状态

```python
# 作用：HTTP 状态码不是 2xx 时抛出异常
# 使用场景：接口失败时停止后续处理，避免使用错误数据
response.raise_for_status()

data = response.json()

print(data.keys())  # 例如：dict_keys(['status', 'items'])
```

常见状态码：

| 状态码 | 含义 | 自动化脚本处理 |
| --- | --- | --- |
| `200` | 成功 | 继续处理 |
| `201` | 创建成功 | 记录成功 |
| `400` | 参数错误 | 检查请求参数 |
| `401` | 认证失败 | 检查 Token |
| `404` | 地址或资源不存在 | 检查 URL |
| `500` | 服务端错误 | 记录日志，必要时重试 |

## 七、异常处理

```python
try:
    # 作用：调用接口并设置超时
    # 使用场景：避免接口无响应导致脚本一直卡住
    response = requests.get(
        url="https://api.example.com/orders",
        params={"target_date": "2026-07-26"},
        timeout=10,
    )
    response.raise_for_status()
    data = response.json()

except requests.Timeout:
    print("request timeout")  # 例如：接口超时

except requests.HTTPError as e:
    print(f"http error: {e}")  # 例如：401 Unauthorized

except requests.RequestException as e:
    print(f"request failed: {e}")  # 例如：网络连接失败
```

## 八、有限重试

接口偶发超时或 500 错误时，可以有限重试。重试不能无限循环。

```python
import time


def get_with_retry(url: str, params: dict, max_retries: int = 3):
    # 作用：带有限重试的 GET 请求
    # 使用场景：接口偶发失败时自动重试
    for attempt in range(1, max_retries + 1):
        try:
            response = requests.get(url=url, params=params, timeout=10)
            response.raise_for_status()
            return response

        except requests.RequestException as e:
            print(f"attempt={attempt} error={e}")

            if attempt == max_retries:
                raise

            time.sleep(2)


response = get_with_retry(
    "https://api.example.com/orders",
    {"target_date": "2026-07-26"},
)

print(response.status_code)  # 例如：200
```

## 九、本章完整案例

下面代码把接口调用封装成函数。真实接口地址需要替换成项目中的测试接口。

```python
import time
import requests


def call_get_api(url: str, params: dict, headers: dict | None = None) -> dict:
    # 作用：调用 GET 接口并返回 JSON
    # 使用场景：自动化脚本从外部系统拉取数据
    response = requests.get(
        url=url,
        params=params,
        headers=headers,
        timeout=10,
    )
    response.raise_for_status()
    return response.json()


def post_job_result(url: str, payload: dict, headers: dict | None = None) -> dict:
    # 作用：调用 POST 接口提交处理结果
    # 使用场景：自动化脚本执行完成后通知其他系统
    response = requests.post(
        url=url,
        json=payload,
        headers=headers,
        timeout=10,
    )
    response.raise_for_status()
    return response.json()


def call_with_retry(func, max_retries: int = 3):
    # 作用：给接口调用增加有限重试
    # 使用场景：处理偶发网络失败
    for attempt in range(1, max_retries + 1):
        try:
            return func()

        except requests.RequestException as e:
            print(f"attempt={attempt} failed: {e}")

            if attempt == max_retries:
                raise

            time.sleep(2)


headers = {"Authorization": "Bearer YOUR_TOKEN"}

orders_data = call_with_retry(
    lambda: call_get_api(
        "https://api.example.com/orders",
        {"target_date": "2026-07-26"},
        headers,
    )
)

result_data = call_with_retry(
    lambda: post_job_result(
        "https://api.example.com/job-results",
        {
            "job_name": "daily_sales_report",
            "target_date": "2026-07-26",
            "status": "success",
        },
        headers,
    )
)

print(type(orders_data))  # 例如：<class 'dict'>
print(type(result_data))  # 例如：<class 'dict'>
```

## 十、方法总结表

| 方法 | 作用 | 常用参数 | 使用场景 |
| --- | --- | --- | --- |
| GET 请求 | 调用查询接口 | `url`、`params`、`headers`、`timeout` | 查询数据 |
| POST 请求 | 调用提交接口 | `url`、`json`、`headers`、`timeout` | 提交结果、发送通知 |
| `response.status_code` | 状态码 | 无 | 判断接口返回状态 |
| `response.json()` | 解析 JSON | 无 | 读取接口返回内容 |
| `raise_for_status()` | 异常检查 | 无 | 非 2xx 状态直接报错 |
| `requests.Timeout` | 超时异常 | 无 | 捕获接口超时 |
| `requests.HTTPError` | HTTP 异常 | 无 | 捕获 4xx / 5xx |
| `requests.RequestException` | 请求异常基类 | 无 | 捕获网络请求异常 |

## 十一、本章练习

1. 使用 GET 请求调用一个测试接口，并设置 `timeout=10`。
2. 给 GET 请求增加 `params`。
3. 使用 `response.json()` 读取 JSON。
4. 使用 POST 请求提交一段处理结果。
5. 增加 `headers`，模拟 Token 请求。
6. 使用 `raise_for_status()` 检查状态码。
7. 增加 `try except` 处理超时和 HTTP 错误。
8. 给 GET 请求增加最多 3 次重试。
