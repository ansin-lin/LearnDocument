# HTML 表单验证属性

## 本章目标

完成本章后，你可以：

- 使用 `required` 设置必填项
- 使用长度和数值属性限制输入范围
- 区分 `placeholder`、`readonly`、`disabled`
- 使用 `autocomplete` 提示自动填充用途
- 测试浏览器的 HTML 表单验证

## 1. 浏览器可以进行基础检查

HTML 属性可以告诉浏览器：

- 这一项必须填写
- 文本长度不能太短或太长
- 数值必须在某个范围内
- 内容应符合邮箱等输入类型

这些检查能帮助用户及时发现遗漏，但不能保证提交的数据一定正确。实际保存数据的一方仍然需要再次检查。

## 2. `required`：必填

```html
<label for="employee-name">姓名</label>
<input id="employee-name" name="employeeName" type="text" required>
```

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `required` | 布尔属性 | 选填；默认不是必填 | 提交前要求控件具有有效值或选择 |

`required="false"` 仍然表示必填。取消必填必须删除整个属性。

`required` 常用于文本框、单选框、复选框和下拉框，但是否应该必填取决于业务要求。

## 3. 文本长度

```html
<input
    id="employee-id"
    name="employeeId"
    type="text"
    minlength="4"
    maxlength="10"
    required
>
```

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `minlength` | 大于等于 0 的整数 | 选填；默认没有最小长度 | 设置最少字符数 |
| `maxlength` | 大于等于 0 的整数 | 选填；默认没有最大长度 | 设置最多字符数 |

`minlength` 不会自动让空输入变成必填。需要必填时应同时写 `required`。

## 4. 数值范围

```html
<label for="experience-years">开发经验年数</label>
<input
    id="experience-years"
    name="experienceYears"
    type="number"
    min="0"
    max="50"
    step="1"
>
```

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `min` | 与输入类型匹配的最小值 | 选填；默认没有下限 | 设置最小允许值 |
| `max` | 与输入类型匹配的最大值 | 选填；默认没有上限 | 设置最大允许值 |
| `step` | 正数或 `any` | 选填；不同类型有各自默认步长 | 设置允许的数值间隔 |

当前示例只允许 0 到 50 的整数。

## 5. `placeholder`：输入提示

```html
<label for="email">联系邮箱</label>
<input
    id="email"
    name="email"
    type="email"
    placeholder="name@example.com"
>
```

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `placeholder` | 简短提示文本 | 选填；默认没有 | 控件为空时显示输入提示 |

`placeholder` 会在用户输入后消失，因此不能代替 `label`。也不要把必须阅读的说明只放进 `placeholder`。

## 6. `readonly` 与 `disabled`

### `readonly`

```html
<input name="eventName" type="text" value="公司技术交流会" readonly>
```

用户不能修改，但该值仍会作为表单数据提交。`readonly` 主要适用于部分文字输入控件。

### `disabled`

```html
<input name="closedSession" type="text" value="已满员场次" disabled>
```

用户不能操作，并且该控件通常不会作为表单数据提交。

| 属性 | 可接受的值 | 默认行为 | 是否提交 |
| --- | --- | --- | --- |
| `readonly` | 布尔属性 | 默认可修改 | 通常提交 |
| `disabled` | 布尔属性 | 默认可操作 | 通常不提交 |

不要使用它们保存可信身份或价格等重要数据，因为访问者可以修改 HTML。

## 7. 自动填充属性

`autocomplete` 可以提示浏览器输入内容的用途：

```html
<input
    id="employee-name"
    name="employeeName"
    type="text"
    autocomplete="name"
>

<input
    id="email"
    name="email"
    type="email"
    autocomplete="email"
>
```

| 属性 | 常见值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `autocomplete` | `name`、`email`、`tel`、`organization`、`on`、`off` | 选填；默认取决于表单和浏览器 | 提示浏览器如何自动填写 |

浏览器是否实际自动填充，还会受到用户设置和已有记录影响。

`autofocus` 可以让一个控件在页面打开时自动获得焦点：

```html
<input id="employee-name" name="employeeName" type="text" autofocus>
```

一个页面最多给一个控件设置 `autofocus`。自动移动焦点可能让用户困惑，本项目不使用，认识即可。

## 8. `pattern`：当前只需了解

`pattern` 可以用模式检查文本格式：

```html
<input name="employeeId" type="text" pattern="[A-Z][0-9]{4}">
```

模式写法本身不是 HTML 基础内容，因此本课程不要求编写。员工编号只使用长度限制，不使用 `pattern`。

## 9. 完成报名表的验证规则

为 `register.html` 中的控件增加以下属性。

### 姓名

```html
<input
    id="employee-name"
    name="employeeName"
    type="text"
    minlength="2"
    maxlength="40"
    autocomplete="name"
    required
>
```

### 员工编号

```html
<input
    id="employee-id"
    name="employeeId"
    type="text"
    minlength="4"
    maxlength="10"
    required
>
```

### 邮箱

```html
<input
    id="email"
    name="email"
    type="email"
    placeholder="name@example.com"
    autocomplete="email"
    required
>
```

### 部门

```html
<select id="department" name="department" required>
    <option value="">请选择</option>
    <option value="development">开发部</option>
    <option value="quality">品质管理部</option>
    <option value="operations">运营部</option>
</select>
```

空值选项让浏览器可以判断用户是否真正选择了部门。

### 参加方式

在默认选中的“现场参加”上保留：

```html
<input id="onsite" name="attendanceType" type="radio" value="onsite" checked required>
```

同名单选框中有一个被选中即可满足必填要求。

## 10. 验证测试

按照以下顺序测试：

1. 不填写任何内容，直接提交。
2. 浏览器应阻止提交，并提示第一个未满足条件的控件。
3. 姓名只输入一个字符，再提交。
4. 员工编号输入超过十个字符，观察输入限制。
5. 邮箱输入 `abc`，确认邮箱格式检查。
6. 正确填写所有必填项并提交。
7. 应打开 `success.html`，地址栏中包含已提交数据。

提示文字的具体语言和外观取决于浏览器和系统。

