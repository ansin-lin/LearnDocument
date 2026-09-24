# 有給休暇申請システム Vue 版

第22章の要件を、Vue 3、Vuetify、Vue Router、Pinia、Axiosで実装した研修用フロントエンドです。利用者登録、ログイン、ホーム集計、休暇申請、確認、完了、一覧検索、取消、ログアウトをNode.js + MySQL APIと接続して動かします。

## 1. 使用技術

- Vue 3とVite：画面と開発環境
- Vuetify：レイアウト、入力、カード、表、DialogなどのUI
- Vue Router：URLと画面遷移
- Pinia：ログイン利用者、申請草稿、申請一覧の共有状態
- Axios：Node.js APIとの通信

追加のフォーム校验库、日期库、CSS框架和图标库は使用していません。业务校验以后台响应为准，前端只处理输入状态、错误显示和请求中的重复点击。

## 2. 目录

```text
src/
├─ api/                 # Axios实例和API调用
├─ components/          # 共用消息、业务布局、申请表单和状态标签
├─ constants/           # 休假类型、状态和显示文字
├─ plugins/             # Vuetify注册和主题
├─ router/              # 页面路由和登录守卫
├─ stores/              # 登录状态、申请草稿和申请列表
├─ styles/              # 少量全局补充样式
├─ utils/               # 日期和ISO UTC时间显示
├─ views/               # 七个业务画面及404画面
├─ App.vue              # Vuetify应用根组件
└─ main.js              # 注册Router、Pinia和Vuetify
```

## 3. 启动

先启动同级目录的`backend`并确认：

```text
GET http://localhost:3000/api/health
```

然后在本目录执行：

```bash
npm ci
npm run dev
```

浏览器打开`http://localhost:5174`。前端和后台都使用`localhost`，不要把一方改成`127.0.0.1`。Vite 已固定使用`5174`；如果该端口被占用，先关闭占用端口的程序再重新启动。

默认API地址来自`.env.example`：

```dotenv
VITE_API_BASE_URL=http://localhost:3000/api
```

需要本机覆盖时，新建`.env.local`，不要把数据库密码或`SESSION_SECRET`写入任何`VITE_`变量。

## 4. 操作顺序

1. 进入注册画面，通过部门API取得选项并创建用户。
2. 使用注册账号登录，浏览器保存`HttpOnly`会话Cookie。
3. 首页读取员工资料和休假汇总。
4. 申请页在选择、失去焦点和进入确认页前执行前端基础校验；校验通过后把草稿保存到Pinia，确认后才发送新增API。
5. 完成页按申请ID重新读取，因此刷新不会重复新增。
6. 一览页可组合状态和关键字筛选；后台在新增时随机生成`pending`、`approved`或`returned`状态。
7. 退出时删除后台Session，并清除前端Store。

## 5. 校验与错误

申请表单的前端校验用于及时提示，后台仍会重新校验全部业务规则。API返回`VALIDATION_ERROR`或业务冲突时，页面回到申请输入，保留草稿并显示字段错误。网络错误、空数据和读取失败分别显示。

新增申请请求处理中按钮会被禁用。新增API没有幂等请求标识，因此不能配置自动重试；发生超时时先打开申请一览确认是否已保存。

## 6. 构建与自测

```bash
npm run build
npm run preview
```

Windows上建议将项目放在纯英文的短路径中，例如`C:\work\paid-leave-vue`。部分Node.js原生构建工具在包含中文、日文或过长的路径下可能异常结束；这不影响源代码，更换工作路径后重新执行`npm ci`即可。

至少检查：

- 注册、登录、刷新后会话恢复和退出；
- 无申请时首页显示`12、0、0`；
- 后台字段错误可以显示，失败后输入仍保留；
- 连续点击时只发送一次新增请求；
- 完成页刷新不新增记录；
- 状态与关键字组合筛选；
- 只有`pending`记录显示取消按钮；
- 360px宽度、键盘操作和直接打开各路由；
- Console没有未处理错误。

`pending`、`approved`和`returned`由后台新增申请时随机决定，不需要额外执行状态准备SQL。随机得到`pending`时可继续验证取消功能。

## 7. 研修范围

这是本地研修项目，不是生产系统。后台README列出的HTTPS、CSRF、限流、审计日志、正式权限和部署设计不在本练习实现范围内。
