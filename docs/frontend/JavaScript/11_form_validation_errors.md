# 第十一章 表单校验与错误提示

本章目标是让学员掌握前端项目中最常见的业务逻辑：检查用户输入是否符合要求，并把错误清楚地显示在页面上。

表单校验不是为了“让页面看起来复杂”，而是为了减少无效数据进入后续处理。以后学习接口请求、Vue、React 时，表单校验仍然是高频任务。

## 示例运行约定

- 第 2 节提供本章第 2～8 节共用的完整 HTML 页面。
- 各节 JavaScript 保存到页面加载的 `js/form-validation.js`。
- 标明“替代”的代码用于替换上一版本，不要同时保留多个表单 `submit` 监听器。

## 1. 表单校验解决什么问题

请假申请系统中常见规则：

- 姓名不能为空。
- 请假类型必须选择。
- 开始日期不能为空。
- 结束日期不能为空。
- 结束日期不能早于开始日期。
- 请假理由不能为空，且不能太短。
- 必须勾选确认项后才能提交。

这些规则本质上都是 `if` 判断。

## 2. 最小校验结构

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>表单校验练习</title>
  <link rel="stylesheet" href="css/style.css">
  <script src="js/form-validation.js" defer></script>
</head>
<body>
<form id="applyForm">
  <label for="employeeName">姓名</label>
  <input id="employeeName" name="employeeName" type="text">
  <p id="nameError" class="error-message"></p>

  <label for="leaveType">请假类型</label>
  <select id="leaveType" name="leaveType">
    <option value="">選択してください</option>
    <option value="paid">有给休假</option>
    <option value="half-am">上午休</option>
  </select>
  <p id="leaveTypeError" class="error-message"></p>

  <label for="startDate">开始日期</label>
  <input id="startDate" name="startDate" type="date">

  <label for="endDate">结束日期</label>
  <input id="endDate" name="endDate" type="date">
  <p id="dateError" class="error-message"></p>

  <label>
    <input id="agree" name="agree" type="checkbox">
    确认申请内容
  </label>
  <p id="agreeError" class="error-message"></p>

  <button type="submit">提交</button>
</form>
</body>
</html>
```

第 2～8 节都使用这份 HTML。单独实验某一节时，直接复制这份表单，不需要另外补写元素。

```js
const applyForm = document.querySelector("#applyForm");
const employeeName = document.querySelector("#employeeName");
const nameError = document.querySelector("#nameError");

applyForm.addEventListener("submit", (event) => {
  event.preventDefault();

  if (employeeName.value === "") {
    nameError.textContent = "姓名不能为空";
    employeeName.classList.add("is-error");
    return;
  }

  nameError.textContent = "";
  employeeName.classList.remove("is-error");
  console.log("可以提交");
});
```

核心流程：

1. 阻止默认提交。
2. 读取输入值。
3. 用 `if` 判断。
4. 错误时显示错误信息并停止后续处理。
5. 正确时清空错误信息并继续。

## 3. 去除首尾空格

用户可能只输入空格。校验前通常要使用 `trim()`。

```js
const employeeName = document.querySelector("#employeeName");
const nameError = document.querySelector("#nameError");

const name = employeeName.value.trim();

if (name === "") {
  nameError.textContent = "姓名不能为空";
}
```

常见字符串处理：

| 写法 | 作用 |
| --- | --- |
| `value.trim()` | 去掉首尾空格 |
| `value.length` | 获取字符长度 |
| `value.includes("文字")` | 判断是否包含某段文字 |
| `value.startsWith("文字")` | 判断是否以某段文字开头 |
| `value.endsWith("文字")` | 判断是否以某段文字结尾 |

## 4. 校验下拉框

```js
const leaveType = document.querySelector("#leaveType");
const leaveTypeError = document.querySelector("#leaveTypeError");

function validateLeaveType() {
  if (leaveType.value === "") {
    leaveTypeError.textContent = "请选择请假类型";
    return false;
  }

  leaveTypeError.textContent = "";
  return true;
}

console.log(validateLeaveType());
```

HTML 中通常把第一个选项设置为空值。第 2 节的完整表单已经包含下面的选项：

```html
<option value="">選択してください</option>
```

这样 JavaScript 判断会更清楚。

## 5. 校验复选框

复选框使用 `checked`。

```js
const agree = document.querySelector("#agree");
const agreeError = document.querySelector("#agreeError");

function validateAgreement() {
  if (!agree.checked) {
    agreeError.textContent = "请确认申请内容";
    return false;
  }

  agreeError.textContent = "";
  return true;
}

console.log(validateAgreement());
```

`!agree.checked` 表示“没有勾选”。

## 6. 校验日期范围

日期输入框的值通常是字符串，例如 `"2026-09-01"`。如果格式是 `YYYY-MM-DD`，可以直接比较大小。

比较范围前要先确认两个日期都不为空：

```js
const startDate = document.querySelector("#startDate");
const endDate = document.querySelector("#endDate");
const dateError = document.querySelector("#dateError");

function validateDateRange() {
  if (startDate.value === "") {
    dateError.textContent = "请选择开始日期";
    return false;
  }

  if (endDate.value === "") {
    dateError.textContent = "请选择结束日期";
    return false;
  }

  if (endDate.value < startDate.value) {
    dateError.textContent = "结束日期不能早于开始日期";
    return false;
  }

  dateError.textContent = "";
  return true;
}

console.log(validateDateRange());
```

## 7. 多个错误如何处理

新人阶段建议使用“逐项校验，遇到错误立即停止”的写法。

```js
const applyForm = document.querySelector("#applyForm");
const employeeName = document.querySelector("#employeeName");
const nameError = document.querySelector("#nameError");
const leaveType = document.querySelector("#leaveType");
const leaveTypeError = document.querySelector("#leaveTypeError");

function validateForm() {
  if (employeeName.value.trim() === "") {
    nameError.textContent = "姓名不能为空";
    return false;
  }

  if (leaveType.value === "") {
    leaveTypeError.textContent = "请选择请假类型";
    return false;
  }

  return true;
}

applyForm.addEventListener("submit", (event) => {
  event.preventDefault();

  if (!validateForm()) {
    return;
  }

  console.log("提交处理");
});
```

这样逻辑清楚，适合教学和代码 Review。

## 8. 清空错误信息

每次重新校验前，先清空旧错误。

下面是本节可直接复制到独立 JavaScript 文件运行的完整版本。它使用第 2 节的 HTML，并替代第 7 节的 JavaScript，不要同时保留两套 `submit` 监听器。

```js
const applyForm = document.querySelector("#applyForm");
const employeeName = document.querySelector("#employeeName");
const nameError = document.querySelector("#nameError");
const leaveType = document.querySelector("#leaveType");
const leaveTypeError = document.querySelector("#leaveTypeError");

function clearErrors() {
  nameError.textContent = "";
  leaveTypeError.textContent = "";

  employeeName.classList.remove("is-error");
  leaveType.classList.remove("is-error");
}

function validateForm() {
  if (employeeName.value.trim() === "") {
    nameError.textContent = "姓名不能为空";
    employeeName.classList.add("is-error");
    return false;
  }

  if (leaveType.value === "") {
    leaveTypeError.textContent = "请选择请假类型";
    leaveType.classList.add("is-error");
    return false;
  }

  return true;
}

applyForm.addEventListener("submit", (event) => {
  event.preventDefault();
  clearErrors();

  if (!validateForm()) {
    return;
  }

  console.log("提交处理");
});
```

## 9. 错误提示的样式配合

CSS 负责错误样式，JavaScript 只负责添加或移除 class。

```css
.error-message {
  color: #dc2626;
  font-size: 14px;
}

.is-error {
  border-color: #dc2626;
  background-color: #fff1f2;
}
```

这种分工以后在 Vue 和 React 中也一样重要。

## 10. 本章练习

正文使用了请假申请表。本练习改为培训报名场景，请根据下面的新 HTML 独立完成校验，不直接复制正文中的字段查询和错误设置代码。

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>培训报名</title>
  <link rel="stylesheet" href="css/training-form.css">
  <script src="js/training-form.js" defer></script>
</head>
<body>
  <form id="trainingForm">
    <label for="traineeName">学员姓名</label>
    <input id="traineeName" name="traineeName" type="text">
    <p id="traineeNameError" class="error-message"></p>

    <label for="courseType">课程类型</label>
    <select id="courseType" name="courseType">
      <option value="">请选择</option>
      <option value="frontend">前端</option>
      <option value="backend">后端</option>
    </select>
    <p id="courseTypeError" class="error-message"></p>

    <label for="trainingStart">开始日期</label>
    <input id="trainingStart" name="trainingStart" type="date">

    <label for="trainingEnd">结束日期</label>
    <input id="trainingEnd" name="trainingEnd" type="date">
    <p id="trainingDateError" class="error-message"></p>

    <label for="learningGoal">学习目标</label>
    <textarea id="learningGoal" name="learningGoal"></textarea>
    <p id="learningGoalError" class="error-message"></p>

    <label>
      <input id="acceptRules" name="acceptRules" type="checkbox">
      已确认培训规则
    </label>
    <p id="acceptRulesError" class="error-message"></p>

    <button type="submit">提交报名</button>
  </form>
</body>
</html>
```

在 `js/training-form.js` 中完成：

1. 学员姓名去除首尾空格后不能为空。
2. 课程类型必须选择。
3. 开始日期和结束日期必须填写。
4. 结束日期不能早于开始日期。
5. 学习目标去除首尾空格后至少有 20 个字符。
6. 未勾选培训规则时不能提交。
7. 每次提交前清空旧错误，再显示本次发现的错误。
8. 校验失败时给对应控件添加 `is-error`，修正后应移除。
9. 校验全部通过后，输出包含姓名、课程、日期和学习目标的报名对象。

完成标准：

- 每条规则都有可观察的错误提示。
- 学员不需要修改练习提供的 HTML 即可完成全部 JavaScript。
- 校验通过时控制台输出整理后的培训报名对象。
- JS 与 CSS 职责清楚，不在 JS 中写大段样式。
