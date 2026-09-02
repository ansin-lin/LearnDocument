# 第 22 章 ES 模块与项目脚本组织

## 本章目标

学完本章后，你应当能够：

- 说明为什么要把 JavaScript 拆分成模块；
- 使用 `export` 和 `import` 导出、导入代码；
- 区分命名导出与默认导出；
- 正确编写浏览器模块路径；
- 使用本地服务器运行模块项目；
- 按照职责组织页面、校验、存储和工具代码；
- 识别模块项目中常见的路径、服务器和循环依赖问题。

---

## 第一部分：认识模块

### 1. 为什么要使用模块

学习初期，可以把全部代码写在一个文件中：

```html
<script src="./app.js"></script>
```

当项目逐渐变大，一个文件可能同时包含：

- 页面元素获取；
- 表单事件处理；
- 数据校验；
- 本地存储；
- 日期格式化；
- 状态文本转换；
- 请求后端接口。

这些代码互相混在一起后，会出现以下问题：

1. 文件过长，难以找到需要修改的位置；
2. 多个页面重复编写相同工具函数；
3. 全局变量可能重名；
4. 修改一个功能时，不容易判断影响范围；
5. 多人协作时容易修改同一个文件。

模块化的基本思路是：

> 按职责把代码拆成多个文件，只公开其他文件需要使用的内容。

```text
页面交互
   ├─ 调用校验模块
   ├─ 调用存储模块
   └─ 调用格式化模块
```

---

### 2. 什么是 ES 模块

ES 模块是 JavaScript 语言内置的模块系统，也称为 ESM。

一个 JavaScript 文件只要使用了 `export` 或 `import`，就可以作为模块使用。

```js
// format.js
export function formatApplicationDate(dateText) {
  return dateText.replaceAll("-", "/");
}
```

```js
// app.js
import { formatApplicationDate } from "./format.js";

console.log(formatApplicationDate("2026-09-01"));
```

- `export`：把本文件中的内容提供给其他模块；
- `import`：把其他模块公开的内容引入当前模块。

---

### 3. 在 HTML 中加载模块

浏览器通过 `type="module"` 识别模块入口。

```html
<script type="module" src="./js/app.js"></script>
```

`app.js` 是入口模块，它可以继续导入其他模块。

#### 3.1 type="module" 带来的行为

模块脚本具有以下特点：

- 模块中的变量默认只在当前模块内有效，不会自动成为全局变量；
- 模块代码自动使用严格模式；
- 外部模块脚本默认延迟执行，通常会在 HTML 解析完成后执行；
- 同一个模块在同一页面中通常只执行一次；
- 可以使用 `import` 和 `export`。

```js
// config.js
const applicationName = "休假申请";

// 不会自动成为 window.applicationName
console.log(window.applicationName); // undefined
```

#### 3.2 为什么不能直接双击 HTML

模块加载受到浏览器安全策略和资源地址规则限制。直接用 `file://` 打开页面时，模块可能无法正常加载。

应使用本地服务器，例如：

```bash
python -m http.server 8000
```

然后访问：

```text
http://localhost:8000/
```

也可以使用编辑器的本地服务器插件或项目开发服务器。

---

## 第二部分：命名导出与导入

### 4. 命名导出

命名导出允许一个模块公开多个内容。

#### 4.1 定义时直接导出

```js
// application-validation.js
export const MAX_REASON_LENGTH = 200;

export function isDateSelected(dateText) {
  return dateText.trim() !== "";
}

export function isReasonValid(reason) {
  return reason.trim().length <= MAX_REASON_LENGTH;
}
```

其他模块必须使用相同名称导入：

```js
import {
  MAX_REASON_LENGTH,
  isDateSelected,
  isReasonValid
} from "./application-validation.js";
```

#### 4.2 先定义，再统一导出

```js
const MAX_REASON_LENGTH = 200;

function isDateSelected(dateText) {
  return dateText.trim() !== "";
}

function isReasonValid(reason) {
  return reason.trim().length <= MAX_REASON_LENGTH;
}

export {
  MAX_REASON_LENGTH,
  isDateSelected,
  isReasonValid
};
```

两种写法作用相同。一个文件内选择一致的风格即可。

#### 4.3 使用 as 设置别名

导入名称与当前文件中的名称冲突，或原名称过长时，可以设置别名。

```js
import {
  isDateSelected as hasSelectedDate
} from "./application-validation.js";

console.log(hasSelectedDate("2026-09-01"));
```

导出时也可以设置别名：

```js
function formatDate(dateText) {
  return dateText.replaceAll("-", "/");
}

export {
  formatDate as formatApplicationDate
};
```

---

### 5. 默认导出

一个模块最多只能有一个默认导出。

```js
// application-storage.js
const STORAGE_KEY = "leave-applications";

function saveApplications(applications) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify(applications)
  );
}

export default saveApplications;
```

默认导入时，名称可以由导入方决定：

```js
import saveApplications from "./application-storage.js";
```

下面也能工作，但随意改名会降低可读性：

```js
import save from "./application-storage.js";
```

#### 5.1 命名导出与默认导出的区别

| 比较项 | 命名导出 | 默认导出 |
| --- | --- | --- |
| 每个模块数量 | 可以有多个 | 最多一个 |
| 导入时是否使用 `{}` | 使用 | 不使用 |
| 导入名称 | 默认与导出名一致 | 可自行命名 |
| 重构时是否容易追踪 | 较容易 | 名称可能不统一 |
| 建议用途 | 工具函数、常量、多个业务函数 | 模块只有一个主要职责时 |

教学项目中优先使用命名导出。它能让导入名称保持一致，也更容易通过搜索找到来源。

#### 5.2 同时使用默认导出和命名导出

```js
// logger.js
export const LOG_LEVEL = "info";

export default function log(message) {
  console.log(`[${LOG_LEVEL}] ${message}`);
}
```

```js
import log, { LOG_LEVEL } from "./logger.js";
```

这种语法需要能够阅读，但同一模块导出方式过多时会增加理解成本，不要为了展示语法而混用。

---

### 6. 导入全部命名内容

`import * as name` 会把模块的命名导出组织到一个模块对象中。

```js
import * as validation from "./application-validation.js";

console.log(validation.MAX_REASON_LENGTH);
console.log(validation.isDateSelected("2026-09-01"));
```

内容较少时，明确列出需要的名称通常更清楚：

```js
import {
  isDateSelected,
  isReasonValid
} from "./application-validation.js";
```

---

### 7. 只执行模块，不接收导出值

有些模块的作用是执行初始化代码，例如注册事件或加载全局样式，不需要接收它的导出内容。

```js
import "./initialize-logging.js";
```

这种写法称为“副作用导入”。

业务代码中应谨慎使用，因为只看导入语句不容易知道它改变了什么。初始化入口可以使用，普通工具模块应优先导出明确的函数。

---

## 第三部分：模块路径

### 8. 浏览器如何解析模块路径

#### 8.1 当前目录

```js
import { formatDate } from "./format.js";
```

`./` 表示相对于当前 JavaScript 文件所在目录。

#### 8.2 上一级目录

```js
import { statusTextMap } from "../config/status.js";
```

`../` 表示当前目录的上一级。

#### 8.3 站点根路径

```js
import { formatDate } from "/js/utils/format.js";
```

以 `/` 开头表示从当前网站根路径查找。项目部署在子目录时，这种写法可能需要额外配置。

#### 8.4 完整 URL

```js
import { helper } from "https://example.com/modules/helper.js";
```

浏览器可以导入符合跨域规则的完整 URL，但项目主线通常使用本地模块或包管理工具。

#### 8.5 裸模块名称

```js
// 浏览器通常不能直接理解
// import axios from "axios";
```

`"axios"` 没有 `./`、`../` 或完整 URL，称为裸模块名称。它通常需要 Vite 等构建工具或 import map 帮助解析。

> 在不使用构建工具的浏览器练习中，路径通常要以 `./`、`../`、`/` 或完整 URL 开头，并写出 `.js` 扩展名。

---

### 9. 相对路径以谁为基准

导入路径相对于**当前写 import 的 JavaScript 文件**，不是相对于 HTML 文件。

```text
project/
├─ index.html
└─ js/
   ├─ app.js
   └─ utils/
      └─ format.js
```

在 `app.js` 中导入：

```js
import { formatDate } from "./utils/format.js";
```

在 `format.js` 中导入与 `app.js` 同级的文件时，需要先返回上一级：

```js
import { something } from "../something.js";
```

判断步骤：

1. 找到写有 `import` 的文件；
2. 从该文件所在目录出发；
3. 按照 `./` 和 `../` 移动；
4. 确认文件名、扩展名和大小写。

---

## 第四部分：模块的运行规则

### 10. 模块只执行一次

多个模块导入同一个模块时，该模块通常只初始化一次。

```js
// counter.js
console.log("counter 模块开始执行");

export const counter = {
  value: 0
};
```

其他文件多次导入时，会共享同一个模块实例。

```js
import { counter } from "./counter.js";

counter.value += 1;
```

这也称为模块缓存。它适合共享配置或单例状态，但不要把所有业务状态都放进全局共享模块，否则测试和维护会变困难。

---

### 11. 导入的是实时绑定

ES 模块导入的值与导出模块保持关联，称为实时绑定。

```js
// application-state.js
export let applicationCount = 0;

export function addApplication() {
  applicationCount += 1;
}
```

```js
// app.js
import {
  applicationCount,
  addApplication
} from "./application-state.js";

console.log(applicationCount); // 0

addApplication();

console.log(applicationCount); // 1
```

导入方可以读取最新值，但不能直接重新赋值：

```js
// applicationCount = 10;
// TypeError：不能给导入绑定重新赋值
```

需要修改时，应调用导出模块提供的函数。

---

### 12. 模块的执行顺序

浏览器会先加载并执行依赖模块，再执行导入它们的模块。

```text
app.js
  ├─ 导入 validation.js
  └─ 导入 storage.js
```

通常顺序是：

1. 分析 `app.js` 的导入；
2. 加载依赖模块；
3. 先执行依赖模块；
4. 最后执行 `app.js`。

不要依赖多个模块的隐式执行顺序完成业务逻辑。应通过函数调用明确表达流程。

---

### 13. 动态导入 import()

普通 `import` 会在模块加载阶段处理依赖。`import()` 可以在运行过程中按需加载模块。

```js
const reportButton = document.querySelector("#report-button");

reportButton.addEventListener("click", async () => {
  const reportModule = await import("./report.js");
  reportModule.showReport();
});
```

`import(path)` 返回 Promise，因此可以使用 `await`。它适合：

- 用户打开某个功能时再加载代码；
- 大型页面按功能拆分；
- 非常用功能延迟加载。

初学项目应先掌握普通静态导入。不要把所有导入都改成动态导入。

---

### 14. 重新导出与入口文件

一个目录可以使用入口文件统一重新导出内容。

```js
// utils/index.js
export { formatApplicationDate } from "./format.js";
export { escapeHtml } from "./escape-html.js";
```

使用方可以从一个入口导入：

```js
import {
  formatApplicationDate,
  escapeHtml
} from "./utils/index.js";
```

这种入口文件有时称为 barrel file。模块较多时可以减少路径数量，但过度集中也会隐藏依赖来源，并可能增加循环依赖。小项目不必强制使用。

---

## 第五部分：完整可运行示例

### 15. 示例目标

制作一个简单的“申请日期管理”页面：

- 输入日期；
- 拒绝空日期和重复日期；
- 把日期保存到 `localStorage`；
- 刷新页面后恢复数据；
- 显示格式化后的日期；
- 可以清空全部日期。

本章只使用本地数据。实际项目接入后端时，可以根据第19章的HTTP请求知识，把存储模块替换为接口模块。

---

### 16. 项目目录

```text
leave-application/
├─ index.html
└─ js/
   ├─ app.js
   ├─ config/
   │  └─ status.js
   ├─ storage/
   │  └─ application-storage.js
   ├─ utils/
   │  └─ format.js
   └─ validation/
      └─ application-validation.js
```

职责如下：

| 文件 | 职责 |
| --- | --- |
| `index.html` | 页面结构和模块入口 |
| `app.js` | 页面元素、事件和整体流程 |
| `status.js` | 状态常量与显示文本 |
| `application-storage.js` | 本地存储读写 |
| `format.js` | 日期和显示文本格式化 |
| `application-validation.js` | 输入值校验 |

---

### 17. 编写 HTML

```html
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta
    name="viewport"
    content="width=device-width, initial-scale=1.0"
  >
  <title>申请日期管理</title>
</head>
<body>
  <h1>申请日期管理</h1>

  <form id="application-form">
    <label for="application-date">申请日期</label>
    <input id="application-date" type="date">
    <button type="submit">添加</button>
  </form>

  <p id="message" role="status"></p>
  <ul id="application-list"></ul>
  <button id="clear-button" type="button">全部清空</button>

  <script type="module" src="./js/app.js"></script>
</body>
</html>
```

页面中只加载入口模块 `app.js`。其他模块由 `app.js` 继续导入。

---

### 18. 状态配置模块

```js
// js/config/status.js
export const APPLICATION_STATUS = {
  DRAFT: "draft"
};

export const statusTextMap = new Map([
  [APPLICATION_STATUS.DRAFT, "草稿"]
]);
```

常量集中定义后，其他模块不必重复手写 `"draft"`。

---

### 19. 校验模块

```js
// js/validation/application-validation.js
export function validateApplicationDate(dateText, selectedDates) {
  if (dateText === "") {
    return {
      valid: false,
      message: "请选择申请日期"
    };
  }

  if (selectedDates.has(dateText)) {
    return {
      valid: false,
      message: "该日期已经添加"
    };
  }

  return {
    valid: true,
    message: ""
  };
}
```

`validateApplicationDate()` 接收输入日期和已选择日期集合，返回统一的校验结果对象。

它只负责判断，不读取页面元素，也不直接显示错误。

---

### 20. 格式化模块

```js
// js/utils/format.js
export function formatApplicationDate(dateText) {
  return dateText.replaceAll("-", "/");
}

export function formatApplicationLabel(application, statusTextMap) {
  const statusText =
    statusTextMap.get(application.status) ?? "未知状态";

  return `${formatApplicationDate(application.date)}（${statusText}）`;
}
```

`formatApplicationDate()` 把 `2026-09-01` 转成 `2026/09/01`。

`formatApplicationLabel()` 组合日期和状态显示文本。

---

### 21. 存储模块

```js
// js/storage/application-storage.js
const STORAGE_KEY = "leave-applications";

export function loadApplications() {
  const jsonText = localStorage.getItem(STORAGE_KEY);

  if (jsonText === null) {
    return [];
  }

  try {
    const parsedValue = JSON.parse(jsonText);
    return Array.isArray(parsedValue) ? parsedValue : [];
  } catch (error) {
    console.error("申请数据解析失败", error);
    return [];
  }
}

export function saveApplications(applications) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify(applications)
  );
}

export function clearApplications() {
  localStorage.removeItem(STORAGE_KEY);
}
```

三个函数分别负责读取、保存和清空。页面模块不需要知道存储键名。

---

### 22. 页面入口模块

```js
// js/app.js
import {
  APPLICATION_STATUS,
  statusTextMap
} from "./config/status.js";

import {
  clearApplications,
  loadApplications,
  saveApplications
} from "./storage/application-storage.js";

import {
  formatApplicationLabel
} from "./utils/format.js";

import {
  validateApplicationDate
} from "./validation/application-validation.js";

const form = document.querySelector("#application-form");
const dateInput = document.querySelector("#application-date");
const message = document.querySelector("#message");
const list = document.querySelector("#application-list");
const clearButton = document.querySelector("#clear-button");

let applications = loadApplications();
const selectedDates = new Set(
  applications.map((application) => application.date)
);

function showMessage(text) {
  message.textContent = text;
}

function renderApplications() {
  list.replaceChildren();

  for (const application of applications) {
    const listItem = document.createElement("li");
    listItem.textContent = formatApplicationLabel(
      application,
      statusTextMap
    );
    list.append(listItem);
  }
}

form.addEventListener("submit", (event) => {
  event.preventDefault();

  const date = dateInput.value;
  const result = validateApplicationDate(
    date,
    selectedDates
  );

  if (!result.valid) {
    showMessage(result.message);
    return;
  }

  const application = {
    id: crypto.randomUUID(),
    date,
    status: APPLICATION_STATUS.DRAFT
  };

  applications.push(application);
  selectedDates.add(date);
  saveApplications(applications);

  renderApplications();
  form.reset();
  showMessage("申请日期已添加");
});

clearButton.addEventListener("click", () => {
  applications = [];
  selectedDates.clear();
  clearApplications();

  renderApplications();
  showMessage("申请日期已全部清空");
});

renderApplications();
```

入口模块负责连接各个模块：

```text
用户提交表单
      ↓
校验模块判断输入
      ↓
更新数组和 Set
      ↓
存储模块保存数据
      ↓
格式化模块生成显示文本
      ↓
入口模块更新 DOM
```

各模块之间通过参数和返回值协作，不直接修改彼此内部变量。

---

### 23. 运行和验证

在 `leave-application` 目录启动本地服务器：

```bash
python -m http.server 8000
```

访问：

```text
http://localhost:8000/
```

依次验证：

1. 不选日期直接提交，显示错误；
2. 添加一个日期，列表出现数据；
3. 重复添加相同日期，显示重复提示；
4. 刷新页面，数据仍然存在；
5. 点击“全部清空”，列表和本地存储都被清除；
6. 打开浏览器开发者工具，确认 Console 没有错误。

---

## 第六部分：组织原则与常见错误

### 24. 如何决定代码放在哪个模块

可以先问下面的问题：

- 是否直接读取或修改 DOM？放在页面模块；
- 是否只判断数据是否合法？放在校验模块；
- 是否只处理数据保存和读取？放在存储或接口模块；
- 是否是多个页面可复用的纯转换？放在工具模块；
- 是否是状态代码、固定选项？放在配置模块。

不要仅因为文件变长就随意拆分。模块应围绕明确职责，而不是把每几个函数机械地分到一个新文件。

---

### 25. 避免循环依赖

下面的结构形成循环：

```text
app.js → validation.js → app.js
```

例如，校验模块为了显示错误又导入页面模块，页面模块同时导入校验模块。

解决方法是让校验模块返回结果，由页面模块负责显示：

```js
export function validateName(name) {
  return name.trim() !== "";
}
```

依赖方向保持清晰：

```text
页面模块
   ↓
业务、校验、存储、工具模块
```

底层模块通常不应反过来导入页面模块。

---

### 26. 常见错误排查

#### 26.1 忘记 type="module"

```html
<script type="module" src="./js/app.js"></script>
```

没有 `type="module"` 时，浏览器无法在该脚本中使用静态 `import`。

#### 26.2 路径缺少 ./

```js
// 错误：浏览器会把它当作裸模块名称
// import { formatDate } from "format.js";

// 正确
import { formatDate } from "./format.js";
```

#### 26.3 文件扩展名或大小写错误

```js
import { formatDate } from "./utils/format.js";
```

确认：

- 文件确实存在；
- `.js` 扩展名已写；
- `format.js` 的大小写与磁盘文件一致；
- 部署到 Linux 服务器后大小写仍然正确。

#### 26.4 混淆默认导入和命名导入

```js
// 命名导出必须使用大括号
import { loadApplications } from "./application-storage.js";

// 默认导出不使用大括号
import saveApplications from "./save-applications.js";
```

#### 26.5 相对路径基准判断错误

路径以当前 JavaScript 文件为基准，不是以 `index.html` 为基准。

#### 26.6 直接通过 file:// 打开

出现跨域或模块加载错误时，先确认是否通过 `http://localhost` 访问。

#### 26.7 服务器返回了 HTML

控制台出现 MIME 类型错误或 `Unexpected token '<'` 时，常见原因是 JavaScript 路径错误，服务器返回了 404 HTML 页面。

在 Network 面板中检查：

- 请求 URL；
- HTTP 状态码；
- Response 内容；
- `Content-Type`。

#### 26.8 浏览器无法解析裸模块名称

```js
// 需要构建工具或 import map
// import axios from "axios";
```

`import axios from "axios"` 这类裸模块名称需要npm依赖和构建工具，或浏览器import map。第19章只要求理解Axios基础请求；具体项目应采用框架或构建工具提供的配置。

---

### 27. 本章练习

#### 练习 1：完成模块项目

按照本章目录创建文件并运行，提交以下验证结果：

1. 正常添加日期的页面截图；
2. 重复日期提示截图；
3. 刷新后数据仍存在；
4. Console 无错误。

#### 练习 2：增加删除单条数据功能

要求：

1. 每条日期后增加“删除”按钮；
2. 点击后从 `applications` 数组删除；
3. 同时从 `selectedDates` 删除日期；
4. 保存最新数组；
5. 重新渲染列表；
6. 删除后允许再次添加该日期。

思考每一步应放在入口模块、存储模块还是工具模块。

#### 扩展练习：按需加载统计模块

创建 `report.js`，导出统计申请数量的函数。只有用户点击“显示统计”按钮时才通过 `import()` 加载。

---

### 28. 本章小结

本章完成了从“一个脚本文件”到“按职责组织项目”的转换。

必须掌握：

- `type="module"`；
- 命名导出与命名导入；
- 默认导出的基本识别；
- `./`、`../` 和文件扩展名；
- 使用本地服务器运行；
- 页面、校验、存储和工具模块的职责划分。

需要理解：

- 模块作用域、严格模式与执行顺序；
- 模块缓存和实时绑定；
- 动态导入；
- 重新导出和循环依赖。

实际项目可以保留本章的模块结构，把本地存储替换为`fetch`或Axios接口模块。统一客户端、认证和拦截器应在具体Vue、React或项目课程中实现。
