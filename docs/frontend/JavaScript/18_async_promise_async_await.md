# 第 18 章 异步处理、Promise 与 async/await

网页经常需要等待：等待定时器结束、等待服务器返回数据、等待用户完成操作。如果 JavaScript 在等待期间什么都不能做，页面就会卡住。异步处理让程序能够先继续执行其他代码，等结果准备好后再处理它。

完成本章后，你应当能够：

- 根据输出顺序区分同步代码和异步任务。
- 说明回调函数在异步处理中的作用。
- 使用 `then()`、`catch()` 和 `finally()` 处理 Promise。
- 说明 Promise 的三种状态以及 `resolve()`、`reject()` 的作用。
- 使用 Promise 链连接有前后依赖的任务。
- 使用 `async`、`await` 和 `try...catch...finally` 编写异步流程。
- 区分顺序等待和 `Promise.all()` 并行等待。
- 在页面中处理加载中、成功、空数据和失败状态。

本章使用 `setTimeout()` 模拟耗时任务。下一章会把相同的处理方式应用到真实的 `fetch()` 请求中。

## 1. 为什么需要异步处理

### 1.1 同步代码按顺序执行

```js
console.log("第 1 步：开始");
console.log("第 2 步：处理数据");
console.log("第 3 步：结束");
```

输出：

```text
第 1 步：开始
第 2 步：处理数据
第 3 步：结束
```

前一行执行完成后，才会执行下一行。这种按顺序完成的执行方式称为**同步执行**。

如果某段同步代码需要很长时间才能完成，后面的 JavaScript 只能等待。页面可能暂时无法响应点击、输入等操作，这种现象称为**阻塞**。

### 1.2 异步任务不会让代码停在原地等待

```js
console.log("第 1 步：开始");

setTimeout(() => {
  console.log("第 2 步：延迟任务完成");
}, 1000);

console.log("第 3 步：继续执行");
```

输出：

```text
第 1 步：开始
第 3 步：继续执行
第 2 步：延迟任务完成
```

`setTimeout(handler, delay)` 让浏览器至少等待指定时间，再执行函数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `handler` | 函数 | 必填 | 等待结束后要执行的函数 |
| `delay` | 大于等于 `0` 的毫秒数 | 可选，省略时按 `0` 处理 | 指定至少等待多长时间 |

`setTimeout()` 会立即返回一个定时器 ID，不会停在原地等待一秒。因此，“第 3 步”先输出，定时器对应的函数稍后才执行。定时器 ID 可以传给 `clearTimeout(id)`，在回调执行前取消定时器。

### 1.3 一个够用的执行模型

零基础阶段先记住下面的过程：

```text
启动定时器、网络请求等异步任务
              ↓
JavaScript 继续执行后面的同步代码
              ↓
异步任务完成，安排对应的后续处理
              ↓
当前同步代码结束后，执行后续处理
```

JavaScript 代码仍然按顺序执行，只是等待工作由浏览器等运行环境管理。本章先学会正确组织异步结果，调用栈、任务队列和微任务等底层细节不影响本章的基本使用。

## 2. 回调函数如何接收异步结果

### 2.1 什么是回调函数

```js
function showMessage() {
  console.log("延迟任务完成");
}

setTimeout(showMessage, 1000);
```

`showMessage` 作为参数传给 `setTimeout()`，由定时器在等待结束后调用。这种“先交给其他代码，在适当时间由对方调用”的函数称为**回调函数**。

这里传入的是函数本身 `showMessage`，不能写成 `showMessage()`。后者会立即调用函数，并把它的返回值传给 `setTimeout()`。

### 2.2 异步结果不能直接使用普通 `return` 取得

下面的写法无法把定时器中的用户对象返回给调用者：

```js
function loadCurrentUserWrong() {
  setTimeout(() => {
    return { employeeNumber: "EMP-00001", name: "田中" };
  }, 500);
}

const user = loadCurrentUserWrong();
console.log(user); // undefined
```

`loadCurrentUserWrong()` 在启动定时器后就执行完了，因此外层函数返回 `undefined`。定时器中的 `return` 只结束定时器回调，不能回到已经结束的外层函数。

可以先通过回调函数接收结果：

```js
function loadCurrentUser(onSuccess) {
  setTimeout(() => {
    const user = { employeeNumber: "EMP-00001", name: "田中" };
    onSuccess(user);
  }, 500);
}

loadCurrentUser((user) => {
  console.log(user.name); // 田中
});
```

`loadCurrentUser(onSuccess)` 接收成功回调。用户数据准备好以后，它调用 `onSuccess(user)`，调用方的 `user` 参数便会收到该对象。

### 2.3 连续任务会产生回调嵌套

现在增加一个要求：先读取当前用户，再根据员工编号读取申请列表。

```js
function loadCurrentUser(onSuccess, onFailure) {
  setTimeout(() => {
    const user = { employeeNumber: "EMP-00001", name: "田中" };
    onSuccess(user);
  }, 500);
}

function loadApplications(employeeNumber, onSuccess, onFailure) {
  setTimeout(() => {
    if (employeeNumber === "EMP-00001") {
      onSuccess(["交通费申请", "休假申请"]);
      return;
    }

    onFailure(new Error("申请列表读取失败"));
  }, 500);
}

loadCurrentUser(
  (user) => {
    loadApplications(
      user.employeeNumber,
      (applications) => {
        console.log(`${user.name}：`, applications);
      },
      (error) => {
        console.error("读取申请失败", error);
      },
    );
  },
  (error) => {
    console.error("读取用户失败", error);
  },
);
```

这段代码能够工作，但已经出现三个具体问题：

1. 第二个任务依赖第一个结果，只能写进第一个成功回调，代码不断向右缩进。
2. 读取用户和读取申请分别需要失败回调，错误处理散落在不同层级。
3. 再增加“读取部门”“读取权限”等步骤时，任务顺序会被层层括号包围。

Promise 的目的不是让异步任务变成同步任务，而是统一表示异步任务的成功或失败，并让连续任务可以按顺序连接。

## 3. Promise 表示未来的结果

### 3.1 Promise 是什么

Promise 是一个表示“未来才会确定的结果”的对象。调用异步函数时，先得到 Promise；任务完成后，再从 Promise 中取得成功值或失败原因。

```text
调用异步函数
    ↓
立即得到 Promise
    ↓
等待任务完成
 ┌──────────────┐
成功值         失败原因
```

### 3.2 Promise 的三种状态

| 状态 | 含义 | 后续变化 |
| --- | --- | --- |
| `pending` | 任务仍在处理中 | 可以变为成功或失败 |
| `fulfilled` | 任务成功完成 | 状态不会再改变 |
| `rejected` | 任务执行失败 | 状态不会再改变 |

Promise 从 `pending` 变为 `fulfilled` 或 `rejected` 后，状态就确定了。成功和失败不会同时发生，也不能再次改变。

### 3.3 Promise 统一了哪些处理

| 需要处理的情况 | 使用的方法 |
| --- | --- |
| 任务成功 | `then()` |
| 任务失败 | `catch()` |
| 无论成功或失败都要执行的收尾 | `finally()` |
| 多个有依赖关系的任务 | Promise 链，或后面的 `async/await` |

接下来先站在调用方的角度使用已有 Promise，再学习怎样创建 Promise。

## 4. 先学会使用已有的 Promise

下面先假设项目中已经有一个 `loadApplications()` 函数。调用它会立即返回 Promise，约半秒后得到申请数组或错误。

### 4.1 `then()` 处理成功结果

```js
loadApplications("EMP-00001").then((applications) => {
  console.log(applications);
});
```

`then(onFulfilled)` 为 Promise 登记成功处理函数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `onFulfilled` | 函数 | 可选 | Promise 成功后接收成功值 |

`then()` 不会让当前代码停下来等待。它会立即返回一个新的 Promise，并在原 Promise 成功后调用传入的函数。示例中的 `applications` 会收到成功值。

### 4.2 `catch()` 处理失败

```js
loadApplications("UNKNOWN")
  .then((applications) => {
    console.log(applications);
  })
  .catch((error) => {
    console.error("读取申请失败", error.message);
  });
```

`catch(onRejected)` 为 Promise 登记失败处理函数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `onRejected` | 函数 | 可选 | 接收 Promise 的失败原因 |

原 Promise 失败，或者前面的 `then()` 中抛出错误时，流程会跳到能够处理该错误的 `catch()`。`catch()` 也会返回一个新的 Promise。

### 4.3 `finally()` 执行共同收尾

```js
console.log("显示加载中");

loadApplications("EMP-00001")
  .then((applications) => {
    console.log("读取成功", applications);
  })
  .catch((error) => {
    console.error("读取失败", error.message);
  })
  .finally(() => {
    console.log("关闭加载中");
  });
```

`finally(onFinally)` 登记共同收尾函数。无论前面的 Promise 成功还是失败都会执行，适合关闭加载提示、恢复按钮等操作。

`finally()` 的回调不接收业务成功值或失败原因，因为它不负责判断结果。它同样返回一个新的 Promise。

### 4.4 调用方只负责使用结果

```text
调用函数取得 Promise
        ↓
then() 处理成功值
catch() 处理失败原因
finally() 执行共同收尾
```

调用方不直接调用 `resolve()` 或 `reject()`。它们属于创建 Promise 的函数，下一节再打开 `loadApplications()` 的内部观察。

## 5. Promise 是怎样创建和完成的

### 5.1 使用 `new Promise()` 创建 Promise

```js
const task = new Promise((resolve, reject) => {
  console.log("开始准备申请数据");
});

console.log(task);
```

`new Promise(executor)` 创建一个初始状态为 `pending` 的 Promise，并立即执行 `executor` 函数。

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `executor` | 接收 `resolve`、`reject` 的函数 | 必填 | 启动任务，并在完成时报告成功或失败 |

JavaScript 会把两个函数交给 `executor`：

- `resolve(value)`：让 Promise 变为 `fulfilled`，并保存成功值。
- `reject(reason)`：让 Promise 变为 `rejected`，并保存失败原因。

`executor` 在创建 Promise 时立即执行，不是任务完成后才执行。真正需要等待的是其中的定时器、网络请求等操作。当前示例没有调用 `resolve()` 或 `reject()`，因此 `task` 会一直保持 `pending`。

### 5.2 用 `resolve()` 交付成功值

```js
const task = new Promise((resolve) => {
  setTimeout(() => {
    resolve(["交通费申请", "休假申请"]);
  }, 500);
});

task.then((applications) => {
  console.log(applications);
});
```

半秒后，`resolve(applications)` 把数组保存为 Promise 的成功值，`then()` 中的参数收到这个数组。`resolve()` 不是普通 `return`，它的作用是确定 Promise 状态并交付结果。

### 5.3 用 `reject()` 交付失败原因

```js
const task = new Promise((resolve, reject) => {
  setTimeout(() => {
    reject(new Error("申请列表读取失败"));
  }, 500);
});

task.catch((error) => {
  console.error(error.message);
});
```

`reject(reason)` 让 Promise 进入失败状态。失败原因理论上可以是任意值，但项目中应优先传入 `Error` 对象，以保留错误名称、消息和调用信息。

### 5.4 封装稳定的模拟业务函数

本章后续统一使用下面两个函数：

```js
function loadCurrentUser() {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ employeeNumber: "EMP-00001", name: "田中" });
    }, 500);
  });
}

function loadApplications(employeeNumber) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (employeeNumber === "EMP-00001") {
        resolve(["交通费申请", "休假申请"]);
        return;
      }

      reject(new Error("申请列表读取失败"));
    }, 500);
  });
}
```

- `loadCurrentUser()` 不接收参数，返回 Promise；成功值是当前用户对象。
- `loadApplications(employeeNumber)` 必须接收员工编号，返回 Promise；编号有效时成功值是申请数组，否则以 `Error` 失败。
- 两个函数都会先返回 `pending` Promise，约半秒后确定结果。

创建方和使用方的职责如下：

```text
创建方 loadApplications()
  ├─ 启动任务
  ├─ 成功时 resolve(applications)
  └─ 失败时 reject(error)

使用方
  ├─ 调用函数取得 Promise
  ├─ then() 处理成功值
  ├─ catch() 处理失败原因
  └─ finally() 执行共同收尾
```

实际项目中更常见的是使用 `fetch()` 等已经返回 Promise 的接口。能够看懂并编写简单的 Promise 包装即可，不要为了使用 Promise 而重复包装一个本来就返回 Promise 的函数。

## 6. 使用 Promise 链连接连续任务

### 6.1 `then()` 会把结果交给下一步

```js
loadApplications("EMP-00001")
  .then((applications) => {
    return applications.length;
  })
  .then((count) => {
    console.log(`申请件数：${count}`);
  });
```

每次调用 `then()` 都会返回新的 Promise。回调的处理结果决定新 Promise 的结果：

| 回调中的写法 | 下一步得到的结果 |
| --- | --- |
| `return 普通值` | 下一段 `then()` 收到该值 |
| `return Promise` | 下一段等待该 Promise，并接收它的成功值 |
| `throw error` | 后续成功处理被跳过，错误交给 `catch()` |
| 没有 `return` | 下一段收到 `undefined` |

### 6.2 返回另一个 Promise

```js
loadCurrentUser()
  .then((user) => {
    console.log(`当前用户：${user.name}`);
    return loadApplications(user.employeeNumber);
  })
  .then((applications) => {
    console.log("申请列表：", applications);
  })
  .catch((error) => {
    console.error("页面数据读取失败", error.message);
  });
```

第一段 `then()` 返回 `loadApplications()` 的 Promise，因此第二段会等待申请列表准备完成。读取用户或读取申请任意一步失败，都会沿着链向后传递到同一个 `catch()`。

### 6.3 忘记 `return` 会断开连接

```js
loadCurrentUser()
  .then((user) => {
    loadApplications(user.employeeNumber); // 错误：忘记 return
  })
  .then((applications) => {
    console.log(applications); // undefined
  });
```

第一段回调没有返回值，新 Promise 会以 `undefined` 成功，下一段不会等待 `loadApplications()`。修正方法是写成：

```js
return loadApplications(user.employeeNumber);
```

## 7. 使用 `async` 和 `await` 简化 Promise

Promise 链可以表达连续步骤，但步骤较多时会出现多个 `then()`。`async/await` 可以用更接近普通流程代码的方式使用同一个 Promise。

### 7.1 `async` 函数一定返回 Promise

```js
async function getMessage() {
  return "读取完成";
}

const result = getMessage();
console.log(result); // Promise 对象

result.then((message) => {
  console.log(message); // 读取完成
});
```

在函数前添加 `async` 后，该函数一定返回 Promise：

- `return value` 会让返回的 Promise 以 `value` 成功。
- `throw error` 会让返回的 Promise 以 `error` 失败。
- 没有 `return` 时，成功值是 `undefined`。

### 7.2 `await` 取得 Promise 的成功值

```js
async function showApplications() {
  const user = await loadCurrentUser();
  const applications = await loadApplications(user.employeeNumber);

  console.log(`${user.name}：`, applications);
}

showApplications();
```

`await expression` 等待表达式代表的 Promise：

- Promise 成功时，`await` 表达式得到成功值。
- Promise 失败时，`await` 抛出失败原因。
- 等待期间只暂停当前 `async` 函数中位于 `await` 后面的代码，不会冻结整个网页。

当前课程统一在 `async` 函数中使用 `await`。

### 7.3 看懂暂停位置

```js
async function showApplications() {
  console.log("函数内：开始读取");

  const user = await loadCurrentUser();

  console.log("函数内：读取完成", user);
}

console.log("函数外：调用前");
showApplications();
console.log("函数外：调用后");
```

输出顺序：

```text
函数外：调用前
函数内：开始读取
函数外：调用后
函数内：读取完成 ...
```

调用 `async` 函数时，它先执行到 `await`。遇到仍在等待的 Promise 后，外部同步代码继续执行；Promise 成功后，再恢复当前函数。

### 7.4 Promise 链和 `async/await` 是同一套机制

Promise 链：

```js
loadCurrentUser()
  .then((user) => {
    return loadApplications(user.employeeNumber);
  })
  .then((applications) => {
    console.log(applications);
  });
```

`async/await`：

```js
async function showApplications() {
  const user = await loadCurrentUser();
  const applications = await loadApplications(user.employeeNumber);
  console.log(applications);
}
```

二者处理的是同一种 Promise。新代码通常使用 `async/await` 表达连续步骤，同时也要能够阅读既有代码中的 Promise 链。

## 8. 处理失败和共同收尾

### 8.1 使用 `try...catch` 接收异步错误

```js
async function showApplications() {
  try {
    const applications = await loadApplications("UNKNOWN");
    console.log(applications);
  } catch (error) {
    console.error("申请列表读取失败", error.message);
  }
}

showApplications();
```

`try` 中的 Promise 失败时，`await` 会抛出失败原因，流程进入 `catch`。不要留下空的 `catch`，否则程序失败后既没有用户提示，也没有可供排查的信息。

### 8.2 使用 `finally` 恢复页面状态

```js
async function showApplications() {
  console.log("显示加载中");

  try {
    const applications = await loadApplications("EMP-00001");
    console.log(applications);
  } catch (error) {
    console.error(error);
  } finally {
    console.log("关闭加载中");
  }
}
```

`finally` 块无论成功还是失败都会执行，适合恢复按钮、隐藏加载提示。它不代替错误处理；错误仍应在 `catch` 中处理，或继续交给调用方。

### 8.3 调用方也要处理 `async` 函数返回的 Promise

```js
async function initializePage() {
  const user = await loadCurrentUser();
  return loadApplications(user.employeeNumber);
}

initializePage().catch((error) => {
  console.error("页面初始化失败", error);
});
```

调用 `async` 函数会立即得到 Promise。如果函数内部没有处理错误，调用方应使用 `await` 或 `catch()` 处理，避免出现未处理的 Promise 拒绝。

## 9. 顺序等待和并行等待

### 9.1 有依赖关系时顺序等待

```js
async function loadPageData() {
  const user = await loadCurrentUser();
  const applications = await loadApplications(user.employeeNumber);

  return { user, applications };
}
```

第二个任务需要第一个任务返回的员工编号，因此必须先读取用户，再读取申请。

### 9.2 互不依赖时使用 `Promise.all()`

```js
function loadDepartments() {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(["开发部", "品质管理部"]);
    }, 500);
  });
}

async function loadIndependentData() {
  const applicationsPromise = loadApplications("EMP-00001");
  const departmentsPromise = loadDepartments();

  const [applications, departments] = await Promise.all([
    applicationsPromise,
    departmentsPromise,
  ]);

  console.log(applications);
  console.log(departments);
}
```

`Promise.all(iterable)` 把多个结果组合成一个 Promise。本例传入 Promise 数组：

- 所有任务成功时，得到按照传入顺序排列的结果数组。
- 任意一个任务失败时，组合后的 Promise 立即进入失败状态。
- 任务在调用函数时已经启动，`Promise.all()` 负责等待它们全部完成。

只有互不依赖的任务才适合这样并行等待。有前后依赖的任务仍应顺序 `await`。

> **可选了解**：`Promise.allSettled()` 会等待所有任务结束，并分别记录成功或失败；`Promise.race()` 采用最先结束的结果；`Promise.any()` 采用最先成功的结果。基础项目先掌握 `Promise.all()` 即可。

## 10. 页面中的完整异步处理

下面是一份独立、可直接运行的完整 HTML。把代码保存为 `async-demo.html`，用浏览器打开后点击按钮。

示例通过员工编号输入框控制结果：

- 输入 `EMP-00001`：返回两条申请。
- 输入 `EMP-00002`：返回空数组。
- 输入其他内容：模拟读取失败。

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>异步读取申请列表</title>
  </head>
  <body>
    <label for="employeeNumber">员工编号</label>
    <input id="employeeNumber" value="EMP-00001">
    <button id="loadButton" type="button">读取申请</button>

    <p id="statusMessage" aria-live="polite">请输入员工编号。</p>
    <ul id="applicationList"></ul>

    <script>
      const employeeNumberInput = document.querySelector("#employeeNumber");
      const loadButton = document.querySelector("#loadButton");
      const statusMessage = document.querySelector("#statusMessage");
      const applicationList = document.querySelector("#applicationList");

      function loadApplications(employeeNumber) {
        return new Promise((resolve, reject) => {
          setTimeout(() => {
            if (employeeNumber === "EMP-00001") {
              resolve(["交通费申请", "休假申请"]);
              return;
            }

            if (employeeNumber === "EMP-00002") {
              resolve([]);
              return;
            }

            reject(new Error("找不到该员工的申请数据"));
          }, 800);
        });
      }

      function renderApplications(applications) {
        applicationList.replaceChildren();

        for (const application of applications) {
          const item = document.createElement("li");
          item.textContent = application;
          applicationList.append(item);
        }
      }

      async function handleLoad() {
        const employeeNumber = employeeNumberInput.value.trim();

        loadButton.disabled = true;
        statusMessage.textContent = "读取中……";
        applicationList.replaceChildren();

        try {
          const applications = await loadApplications(employeeNumber);

          if (applications.length === 0) {
            statusMessage.textContent = "没有申请数据";
            return;
          }

          renderApplications(applications);
          statusMessage.textContent = "读取完成";
        } catch (error) {
          console.error(error);
          statusMessage.textContent = "申请列表读取失败，请确认员工编号";
        } finally {
          loadButton.disabled = false;
        }
      }

      loadButton.addEventListener("click", () => {
        handleLoad().catch((error) => {
          console.error("未预期的页面错误", error);
        });
      });
    </script>
  </body>
</html>
```

本例中首次组合使用的方法如下：

- `trim()` 删除输入内容首尾的空白字符并返回新字符串。
- `replaceChildren()` 删除元素的现有子节点；传入新节点时也可以同时替换内容。
- `createElement(tagName)` 创建指定标签的元素节点。
- `append(node)` 把节点追加到元素末尾。
- `addEventListener(type, listener)` 登记事件处理函数；这里监听 `click` 事件。

点击按钮后，可以观察到完整状态变化：

```text
按钮可用
→ 点击后禁用按钮并显示“读取中”
→ 显示成功列表、空数据提示或失败提示
→ finally 恢复按钮
```

## 11. 常见错误与排查

### 11.1 把 Promise 当成最终数据

```js
const applications = loadApplications("EMP-00001");
console.log(applications.length); // 这里得到的不是申请数组长度
```

`loadApplications()` 返回 Promise，不是数组。应在 `then()` 中使用成功值，或者写成 `await loadApplications(...)`。

### 11.2 在异步结果准备前读取变量

```js
let applications = [];

loadApplications("EMP-00001").then((result) => {
  applications = result;
});

console.log(applications); // 此时通常仍是 []
```

最后一行同步代码不会等待 `then()`。需要数据的操作应放进后续 Promise 流程，或者由 `async` 函数继续 `await`。

### 11.3 Promise 链中忘记 `return`

下一段收到 `undefined` 或提前执行时，检查上一段 `then()` 是否返回了值或 Promise。

### 11.4 使用 `await` 却没有处理错误

网络、存储和数据转换都可能失败。明确在哪一层使用 `try...catch`，同时保留控制台错误和用户能够理解的页面提示。

### 11.5 误以为 `await` 会冻结网页

`await` 只暂停当前 `async` 函数后面的代码。浏览器仍然可以处理页面更新和其他事件。

### 11.6 无依赖任务全部顺序等待

互不依赖的任务逐个 `await` 会增加总等待时间。确认任务没有依赖关系后，可以同时启动并使用 `Promise.all()` 等待。

### 11.7 `forEach()` 不会等待异步回调

```js
async function processApplications(applications) {
  applications.forEach(async (application) => {
    await saveApplication(application);
  });

  console.log("全部保存完成"); // 可能过早执行
}
```

`forEach()` 不会等待回调返回的 Promise。需要逐项顺序处理时使用 `for...of`：

```js
async function processApplications(applications) {
  for (const application of applications) {
    await saveApplication(application);
  }

  console.log("全部保存完成");
}
```

这里假设项目中已经存在返回 Promise 的 `saveApplication(application)`。如果各项可以并行处理，可以使用 `applications.map(saveApplication)` 生成 Promise 数组，再交给 `Promise.all()`。

## 12. 本章综合练习

练习使用“通知列表”，不直接复制本章的申请列表示例。

### 12.1 初始状态

新建 `notification-demo.html`，准备以下元素：

- “读取通知”按钮。
- 用于显示加载、空数据、成功和失败消息的状态区域。
- 用于显示通知的列表。

使用 `setTimeout()` 模拟数据读取，本章不发送真实网络请求。

### 12.2 任务一：使用 Promise 处理结果

1. 编写 `loadNotifications(userId)`，让它立即返回 Promise。
2. 一秒后，根据 `userId` 产生成功数组、空数组或 `Error`。
3. 使用 `then()` 显示成功数据。
4. 使用 `catch()` 显示失败提示并在控制台记录错误。
5. 使用 `finally()` 恢复按钮。
6. 故意删除一处 `return`，观察下一段得到什么，再修正代码。

### 12.3 任务二：改写为 `async/await`

1. 把按钮处理函数声明为 `async` 函数。
2. 使用 `await` 取得通知数组。
3. 使用 `try...catch...finally` 处理成功、失败和收尾。
4. 点击按钮后立即禁用按钮并显示“读取中”。
5. 分别显示通知列表、空数据提示和失败提示。
6. 增加一个互不依赖的 `loadUserSettings()`，使用 `Promise.all()` 同时等待通知和设置。

### 12.4 验证标准

| 场景 | 预期结果 |
| --- | --- |
| 调用读取函数后立即打印返回值 | 看到 Promise，而不是通知数组 |
| 有通知 | 显示通知列表和成功消息 |
| 返回空数组 | 显示“没有新通知” |
| 读取失败 | 页面显示失败提示，控制台保留错误信息 |
| 成功或失败结束 | 按钮恢复可用 |
| 两个独立任务同时执行 | `Promise.all()` 按传入顺序返回两个结果 |

完成练习后，你应当能够解释：

- Promise 为什么有 `pending`、`fulfilled` 和 `rejected` 三种状态。
- `then()` 链中的 `return` 为什么不能随意省略。
- `async` 函数返回什么，`await` 暂停的是哪一部分代码。
- 什么情况下应顺序等待，什么情况下可以并行等待。
