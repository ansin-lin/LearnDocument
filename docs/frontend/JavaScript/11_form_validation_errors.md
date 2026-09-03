# 第十一章 表单校验与错误提示

表单校验就是按规则检查用户输入，并告诉用户哪里需要修改。例如，“姓名不能为空”需要判断文本，“必须同意规则”需要判断复选框。

完成本章后，你应能独立检查一个字段，再把多个判断组合到一次提交中。需要用到[第十章](10_events_forms.md)的事件绑定与 `preventDefault()`，但不需要沿用第十章的 HTML 或脚本。

## 示例运行约定

各节示例默认相互独立，只复制当前小节配套的 HTML 和 JavaScript，不把不同示例追加到同一个脚本。

创建 `validation-demo/index.html` 和 `validation-demo/js/app.js`。页面外壳如下：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>校验实验</title>
  <script src="js/app.js" defer></script>
</head>
<body>
  <!-- 放入当前实验的 HTML -->
</body>
</html>
```

每次实验，替换 `body` 中的内容和整个 `js/app.js`，保存后刷新。第 2～8 节的结果直接显示在页面上，不需要到 Console 手动调用函数。第 9 节为第 8 节补充 CSS，第 10 节练习使用单独的文件。

## 1. 表单校验解决什么问题

常见规则与判断对象：

| 规则 | 检查什么 |
| --- | --- |
| 姓名不能为空 | 输入框的 `value` 是否为空字符串 |
| 备注不能只有空格 | `trim()` 处理后的字符串是否为空 |
| 必须选择类型 | 下拉框是否仍处于空值选项 |
| 必须同意规则 | 复选框的 `checked` 是否为 `true` |
| 结束日期不能早于开始日期 | 两个有效日期的先后关系 |

这些规则可以用已学过的 `if`、字符串处理和布尔值实现。校验应读取用户**本次操作时**的值，不能只在页面加载时读取一次。

HTML 的 `required` 等属性也能提供浏览器原生校验。下面的自定义校验实验不设置这些约束，让点击提交后能进入 JavaScript 并显示自己的提示。如果同时使用原生约束，不满足约束时浏览器可能先拦截提交，不会触发 `submit`。

前端校验用于及时提示、减少误操作，不能代替服务端校验。下面的“通过”只表示输入符合当前规则，不表示已保存或已发送请求。

## 2. 最小校验：输入不能为空

只用一个姓名字段观察“提交 → 判断 → 显示结果”。

HTML：

```html
<form id="nameForm">
  <label for="nameInput">姓名</label>
  <input id="nameInput" name="name" type="text">
  <button type="submit">检查姓名</button>
</form>
<p id="nameMessage"></p>
```

JavaScript：

```js
const nameForm = document.getElementById("nameForm");
const nameInput = document.getElementById("nameInput");
const nameMessage = document.getElementById("nameMessage");

nameForm.addEventListener("submit", event => {
  event.preventDefault();

  if (nameInput.value === "") {
    nameMessage.textContent = "姓名不能为空";
    return;
  }

  nameMessage.textContent = "姓名检查通过";
});
```

1. 点击提交或在输入框按回车，触发 `submit`。
2. `preventDefault()` 阻止默认提交，留在当前页面。
3. `nameInput.value` 读取当前输入。空值时显示错误。
4. `return` 结束这一次回调，下面的成功提示不再执行。
5. 非空时显示“姓名检查通过”，覆盖上次的提示。

依次验证：空输入 → 输入“田中” → 删除文字。每次提交后，应分别显示错误、通过、错误。

注意：只输入空格也会通过，因为 `"   "` 不等于 `""`。下一节单独解决这个问题。

## 3. 文本整理：排除空格和过短内容

`trim()` 返回去掉首尾空白后的新字符串，不会改变原字符串，也不会自动改写输入框。字符串的 `length` 表示长度；例如 `"会议准备".length` 为 `4`。

独立实验：备注去除首尾空格后至少 5 个字符。

```html
<label for="memoInput">备注（至少 5 个字符）</label>
<textarea id="memoInput" name="memo"></textarea>
<button id="checkButton" type="button">检查备注</button>
<p id="memoMessage"></p>
```

```js
const memoInput = document.getElementById("memoInput");
const checkButton = document.getElementById("checkButton");
const memoMessage = document.getElementById("memoMessage");

checkButton.addEventListener("click", () => {
  const memo = memoInput.value.trim();

  if (memo === "") {
    memoMessage.textContent = "备注不能为空，也不能只有空格";
    return;
  }

  if (memo.length < 5) {
    memoMessage.textContent = "备注至少需要 5 个字符";
    return;
  }

  memoMessage.textContent = "检查通过，整理后的备注：" + memo;
});
```

这里使用普通按钮，只检查一个控件，不提交表单。关键是把 `value.trim()` 放在点击回调里，每点击一次都重新读取。

| 输入 | 点击后的结果 |
| --- | --- |
| 空值或全是空格 | 提示不能为空 |
| `开会` | 提示至少 5 个字符 |
| `  准备会议资料  ` | 显示“检查通过，整理后的备注：准备会议资料” |

最后一项通过后，输入框仍保留原来的首尾空格，因为只整理了局部变量 `memo`。需要回写时，可以在校验通过后执行 `memoInput.value = memo`。

本例按 JavaScript 的 `length` 规则计数。表情符号等内容可能占多个长度单位，业务上的“字数”规则需要另行确认；先用普通中文、英文字母和数字验证。

## 4. 下拉框：检查是否选择有效选项

单选下拉框的 `value` 是当前选中 `option` 的 `value`，不是显示文字。把提示选项的值设为 `""`，就可以判断用户是否仍未选择。

HTML：

```html
<label for="deliveryType">收取方式</label>
<select id="deliveryType" name="deliveryType">
  <option value="">请选择</option>
  <option value="pickup">现场领取</option>
  <option value="post">邮寄</option>
</select>
<button id="checkButton" type="button">检查选择</button>
<p id="typeMessage"></p>
```

JavaScript：

```js
const deliveryType = document.getElementById("deliveryType");
const checkButton = document.getElementById("checkButton");
const typeMessage = document.getElementById("typeMessage");

checkButton.addEventListener("click", () => {
  if (deliveryType.value === "") {
    typeMessage.textContent = "请选择收取方式";
    return;
  }

  typeMessage.textContent = "选择检查通过：" + deliveryType.value;
});
```

初始状态点击按钮显示错误；选择“邮寄”后再点击显示 `post`；切回“请选择”再点击，错误重新出现。这里依据页面给出的选项检查是否选择，不等于服务端已经验证字段值合法。

## 5. 复选框：检查是否勾选

复选框用 `checked` 判断选中状态，不使用 `value` 判断。`checked` 是布尔值：勾选为 `true`，未勾选为 `false`。

```html
<label>
  <input id="agree" name="agree" type="checkbox">
  我已阅读使用规则
</label>
<button id="checkButton" type="button">检查确认项</button>
<p id="agreeMessage"></p>
```

```js
const agree = document.getElementById("agree");
const checkButton = document.getElementById("checkButton");
const agreeMessage = document.getElementById("agreeMessage");

checkButton.addEventListener("click", () => {
  if (!agree.checked) {
    agreeMessage.textContent = "请先确认使用规则";
    return;
  }

  agreeMessage.textContent = "确认项检查通过";
});
```

`!agree.checked` 表示“没有勾选”。分别在未勾选、勾选、取消勾选后点击按钮，应得到错误、通过、错误。

复选框的 `value` 表示选中后可提交的字段值，并不随勾选状态自动变成 `true` 或 `false`。

## 6. 日期范围：先检查空值，再比较先后

这里检查“设备借用开始日期”和“归还日期”。规则是两个日期都要填写，归还日期可以等于开始日期，但不能更早。

`input type="date"` 的 `value` 在未填写时为 `""`；选定普通四位年份日期后，得到 `YYYY-MM-DD` 格式的字符串，例如 `"2026-09-01"`，与页面显示的本地日期样式可能不同。

```html
<label for="startDate">借用开始日期</label>
<input id="startDate" name="startDate" type="date">
<label for="endDate">归还日期</label>
<input id="endDate" name="endDate" type="date">
<button id="checkButton" type="button">检查日期</button>
<p id="dateMessage"></p>
```

```js
const startDate = document.getElementById("startDate");
const endDate = document.getElementById("endDate");
const checkButton = document.getElementById("checkButton");
const dateMessage = document.getElementById("dateMessage");

checkButton.addEventListener("click", () => {
  const start = startDate.value;
  const end = endDate.value;

  if (start === "") {
    dateMessage.textContent = "请选择借用开始日期";
    return;
  }

  if (end === "") {
    dateMessage.textContent = "请选择归还日期";
    return;
  }

  if (end < start) {
    dateMessage.textContent = "归还日期不能早于借用开始日期";
    return;
  }

  dateMessage.textContent = "日期范围检查通过";
});
```

为什么能比较字符串？固定四位年份、两位月份、两位日期的字符串从较大时间单位排到较小单位，字符串顺序与日期先后一致。例如 `"2026-09-02" < "2026-09-10"` 为 `true`。

这不是任意日期文本的通用比较方法。不要直接套用于 `"9/2/2026"`、不同位数的年份或带有时间、时区的字符串。这里用日期控件选择 2026 年附近的日期进行实验。

依次验证：都不填、只填开始日期、归还早于开始、同一天、归还晚于开始。前面三种应报错，后面两种应通过。

## 7. 组合校验：多个字段通过后才继续

单项判断清楚后，再组合一个只有“姓名”和“确认项”的小表单。这里不沿用第 2～6 节的页面，也不需要拼接之前的脚本。

### 7.1 HTML（替换页面的 body 内容）

```html
<form id="confirmForm">
  <label for="nameInput">姓名</label>
  <input id="nameInput" name="name" type="text">
  <p id="nameError"></p>
  <label>
    <input id="agree" name="agree" type="checkbox">
    我已核对填写内容
  </label>
  <p id="agreeError"></p>
  <button type="submit">检查表单</button>
</form>
<p id="formMessage"></p>
```

### 7.2 完整 JavaScript

每个校验函数约定：不通过时显示错误并返回 `false`；通过时返回 `true`。函数返回值供调用者判断，不代表自动提交。

```js
const confirmForm = document.getElementById("confirmForm");
const nameInput = document.getElementById("nameInput");
const agree = document.getElementById("agree");
const nameError = document.getElementById("nameError");
const agreeError = document.getElementById("agreeError");
const formMessage = document.getElementById("formMessage");

function validateName() {
  if (nameInput.value.trim() === "") {
    nameError.textContent = "姓名不能为空";
    return false;
  }
  return true;
}

function validateAgreement() {
  if (!agree.checked) {
    agreeError.textContent = "请先核对填写内容";
    return false;
  }
  return true;
}

confirmForm.addEventListener("submit", event => {
  event.preventDefault();
  nameError.textContent = "";
  agreeError.textContent = "";
  formMessage.textContent = "";

  if (!validateName()) {
    return;
  }

  if (!validateAgreement()) {
    return;
  }

  formMessage.textContent = "全部检查通过，可以继续下一步";
});
```

### 7.3 跟踪一次执行

以“姓名已填写，但未勾选”为例：

1. 清空上次所有提示，防止旧成功或旧错误残留。
2. `validateName()` 返回 `true`，`!true` 为 `false`，不进入第一个 `if`。
3. `validateAgreement()` 显示错误并返回 `false`。
4. `!false` 为 `true`，进入第二个 `if`，其中的 `return` 结束提交回调。
5. 最后的通过提示不会执行。

注意两种 `return` 的位置：校验函数里的 `return false` 把结果交回调用者；回调里的 `return` 才停止本次提交处理。

这个版本按字段顺序检查，发现第一处错误就停止，不会一次显示所有错误。可以先保持这种方式，不必立即增加统一规则对象或通用校验框架。

验证：空表单提交 → 填姓名再提交 → 勾选后提交 → 删除姓名再提交。应依次提示姓名错误、确认项错误、全部通过、姓名错误；最后一次不应保留旧的成功文字。

## 8. 错误状态：提示文字、样式标记和焦点

文字能说明原因，错误样式帮助定位；仅改变颜色不能让用户知道该怎么改。这一节回到一个姓名字段，单独观察错误状态的设置与清理。

### 8.1 HTML

`aria-describedby="nameError"` 把输入框与提示区域关联；`aria-invalid` 用于表达该字段当前是否被判定为无效。它们是可访问性属性，不会自行执行校验或显示红色边框。

```html
<form id="nameForm">
  <label for="nameInput">姓名</label>
  <input id="nameInput" name="name" type="text"
         aria-describedby="nameError" aria-invalid="false">
  <p id="nameError" class="error-message"></p>
  <button type="submit">检查姓名</button>
</form>
<p id="formMessage"></p>
```

### 8.2 JavaScript

```js
const nameForm = document.getElementById("nameForm");
const nameInput = document.getElementById("nameInput");
const nameError = document.getElementById("nameError");
const formMessage = document.getElementById("formMessage");

nameForm.addEventListener("submit", event => {
  event.preventDefault();

  nameError.textContent = "";
  formMessage.textContent = "";
  nameInput.classList.remove("is-error");
  nameInput.setAttribute("aria-invalid", "false");

  if (nameInput.value.trim() === "") {
    nameError.textContent = "请输入姓名，不能只填写空格";
    nameInput.classList.add("is-error");
    nameInput.setAttribute("aria-invalid", "true");
    nameInput.focus();
    return;
  }

  formMessage.textContent = "姓名检查通过";
});
```

- `classList.add("is-error")` 添加错误 class；`classList.remove("is-error")` 移除这个 class。样式由第 9 节 CSS 定义，没有 CSS 时仍能看到文字提示。
- `setAttribute("aria-invalid", "true")` 标记字段无效；`"false"` 清除无效标记。这里传入的是 HTML 属性的字符串值。
- `nameInput.focus()` 把键盘焦点移到输入框。本例不传参数，调用后用户可以直接输入；它不会自动清空、选中文字或校验内容。

每次提交先清理旧状态，再按当前输入设置新状态。空值提交后，输入框应获得焦点；填写姓名再次提交，错误文字和错误标记应清除。再次清空提交，错误应重新出现。

本例在提交时更新校验结果，不在每次输入时更新。用户刚开始修改但尚未再次提交时，旧错误暂时保留是本例的行为，不是 `input` 监听失效。

## 9. 错误提示的样式配合

保留第 8 节的 HTML 和 JavaScript，新建 `css/style.css`，并在 `index.html` 的 `head` 中追加以下引用：

```html
<link rel="stylesheet" href="css/style.css">
```

`css/style.css` 的完整内容：

```css
.error-message {
  color: #b91c1c;
  font-size: 14px;
}

.is-error {
  border: 2px solid #b91c1c;
  background-color: #fff1f2;
}
```

空值提交后应同时看到错误文字、红色边框和浅色背景；填写姓名再次提交后，恢复正常样式。CSS 负责外观，JavaScript 负责何时添加或移除 class，不必在脚本里逐项修改颜色。

如果文字出现但边框没变，先检查样式文件的相对路径，再查看输入框是否已有 `is-error` class。不要为了显示错误而去掉浏览器的焦点轮廓。

## 10. 本章练习

使用下面的培训报名页面，把本章的单项判断组合起来。请独立编写脚本，不直接复制某个正文示例的整段处理逻辑。

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
9. 校验全部通过后，使用 `console.log()` 分别输出姓名、课程、日期和学习目标。

完成标准：

- 每条规则都有可观察的错误提示。
- 学员不需要修改练习提供的 HTML 即可完成全部 JavaScript。
- 校验通过时控制台分别输出整理后的报名字段。
- JS 与 CSS 职责清楚，不在 JS 中写大段样式。

## 参考资料

- [MDN：日期输入框与 value 格式](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input/date)
