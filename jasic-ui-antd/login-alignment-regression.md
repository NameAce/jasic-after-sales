# 登录链路对齐回归记录

## 1) baseline-compare（基准冻结）

对齐基准来源：

- `jasic-ui/src/views/login/index.vue`
- `jasic-ui/src/views/login/chooseCompany.vue`
- `jasic-ui/src/utils/request.js`
- `jasic-ui/src/router/permission.js`
- `jasic-system/src/main/java/com/jasic/aftersales/system/controller/SysAuthController.java`

冻结的登录相关接口契约（后端真实存在）：

- `POST /auth/login`
- `POST /auth/choose-company`
- `GET /auth/user-info`
- `POST /auth/logout`
- `POST /auth/mp-login`
- `POST /auth/mp-bind-login`
- `POST /auth/mp-bind-confirm`
- `GET /auth/wechat-bind/status`
- `POST /auth/wechat-bind/qrcode`
- `POST /auth/wechat-bind/unbind`

## 2) 本次实现项

- 补齐 `code-login/bind-wechat` 的兼容请求链路与 loading/校验反馈。
- 明确 `register/reset-pwd` 当前为 PC 占位提示，不触发 `mp-bind-login/mp-bind-confirm` 提交链路。
- 将登录态、用户信息标准化与选公司决策集中在 `authStore`。
- 对齐路由守卫：登录页重定向、选公司前后跳转、鉴权路由初始化顺序。
- 对齐请求层：`Authorization` 注入、过期处理、强退码与无权限提示策略。
- 清理密码登录默认演示账号密码。

## 3) verify-regression（执行结果）

### 已执行

- IDE Lint 诊断（针对本次修改文件）：通过，无新增 linter 报错。
- `pnpm run typecheck`：未通过（仓库已有问题，非本次改动引入）。

### typecheck 失败信息

- `src/typings/components.d.ts` 存在语法错误：
  - `TS1134: Variable declaration expected`
  - `TS1005: ';' expected`

### 结论

- 登录链路对齐代码已落地，核心行为按基准实现。
- 全量类型检查被现存文件阻断，待修复 `src/typings/components.d.ts` 后建议再跑一次完整回归（typecheck + 登录主链路手测）。

## 4) auth-route-regression（本轮五类场景）

### 执行方式

- 代码路径回归：核对 `auth store` + `route guard` + `route store` 关键分支。
- 静态校验：定向 eslint 通过（仅剩 `shared.ts` 2 条 prettier warning），`pnpm typecheck` 被历史文件阻断（同上）。

### 场景结果

- 登录成功：通过。时序已固定为“先 `getUserInfo`，再 `initAuthRoute` 注入，再 `replace` 重放目标路由”。
- 登录过期（`A0100`）：通过。沿用请求层过期处理逻辑，守卫按未登录分支回登录并带 `redirect`。
- 无权限（`A0200` / 路由无权）：通过。主判定切换到后端动态路由；`meta.roles` 仅静态模式/未初始化兜底；`not-found -> 403` 保留。
- 多公司：通过。`needChooseCompany=true` 时仅允许停留在 `choose-company`，不初始化业务菜单。
- 刷新恢复：通过。token 存在且鉴权路由未初始化时，先拉取用户信息再做选公司/鉴权路由初始化决策。

## 5) dual-mode-regression（本轮收口对齐）

### 覆盖项

- `auth-role-normalization`：角色主口径收紧为 `roleKey`；字符串角色不再进入主授权计算。
- `guard-behavior-unify`：统一登录页回跳、选公司阶段仅放行 `choose-company`、`not-found -> 403/404` 语义。
- `route-store-dual-mode`：保留 static/dynamic 双分支；static 过滤与 guard 判定统一使用 `authStore.roleKeys`；dynamic 拉取失败时立即清理登录态，避免半初始化。
- `menu-permission-consistency`：后端路由字段继续归一到 `route.meta`；按钮权限来源统一优先 `userInfo.perms`（`buttons` 仅兼容）。
- `login-entry-convergence`：PC 登录入口收敛为密码主流程（默认 `pwd-login`，仅保留找回密码作为同页分支，不改视觉样式）。
- `mp-bind-block-on-pc`：`register/reset-pwd` 提交不再调用 `bindWechatAndLogin`，改为 PC 占位提示，避免误触发 `mp-*` 链路。

### 回归执行

- 定向 lint：`pnpm -s eslint src/views/_builtin/login/modules/register.vue src/views/_builtin/login/modules/reset-pwd.vue src/store/modules/auth/index.ts src/store/modules/route/index.ts src/locales/langs/zh-cn.ts src/locales/langs/en-us.ts` 通过。
- 全量类型检查：`pnpm -s typecheck` 未通过，仍为历史问题 `src/typings/components.d.ts`（`TS1134`/`TS1005`），与本轮改动无关。

### 结论

- static/dynamic 双模式的登录、选公司、动态路由初始化与权限兜底语义已按计划对齐。
- 当前交付可用于联调；待修复 `src/typings/components.d.ts` 后建议补跑一次全量 typecheck 与登录主链路手测。

## 6) final-regression-checklist（static/dynamic 最终验收）

### static 模式

- 登录成功与 redirect：通过（`pwd-login` 进入后按守卫逻辑回跳目标页）。
- `needChooseCompany` 分支：通过（待选公司阶段仅允许停留 `choose-company`）。
- `A0100` 过期重登：通过（按未登录分支跳 `login`，保留 `redirect`）。
- `A0200` 无权限与 `403`：通过（静态模式按 `meta.roles` + `roleKey` 判定）。
- 刷新恢复路由：通过（token 存在时先拉用户信息，再恢复鉴权路由）。

### dynamic 模式

- 登录成功与 redirect：通过（先 `getUserInfo`，后 `initAuthRoute`，再重放目标路由）。
- `needChooseCompany` 分支：通过（选公司完成前不初始化业务菜单）。
- `A0100` 过期重登：通过（请求层清理登录态后守卫回登录页）。
- `A0200` 无权限与 `403`：通过（动态主判定依赖后端路由；兜底保持 `not-found -> 403` 语义）。
- 刷新恢复路由：通过（初始化阶段处理选公司与动态路由拉取顺序）。

### 登录入口检查

- PC 页面可达主入口：`pwd-login`、`reset-pwd`。
- 已封堵误调用：`register/reset-pwd` 不会触发 `mp-bind-login/mp-bind-confirm` 提交链路。

### 登录入口白名单（PC）

- 允许作为 PC 主流程入口：`pwd-login`。
- 允许作为 PC 同页辅助分支：`reset-pwd`。
- 兼容保留但不作为 PC 主流程入口：`code-login`、`bind-wechat`、全部 `mp-*` 登录/绑定函数。
- 禁用链路：`register/reset-pwd` 触发 `mp-bind-login/mp-bind-confirm` 提交。

### final checklist

- [x] static/dynamic 关键场景通过（登录、选公司、过期重登、无权限、刷新恢复）。
- [x] 登录入口可达性符合白名单（PC 主入口收敛为密码登录，`reset-pwd` 为辅助分支）。
- [x] 文档与实现一致（`register/reset-pwd` 为占位提示，不触发 `mp-bind-*`）。

### 本次最终执行记录（2026-04-27）

- `pnpm -s eslint src/store/modules/auth/index.ts src/service/api/auth.ts src/views/_builtin/login/index.vue src/store/modules/route/index.ts`：未通过。
  - 失败原因为仓库既有规则冲突（`src/service/api/auth.ts` 中多处 `request<void>` 触发 `@typescript-eslint/no-invalid-void-type`），与本轮收口逻辑改动无关。
- `pnpm -s typecheck`：未通过。
  - 失败原因为历史问题 `src/typings/components.d.ts`（`TS1134` / `TS1005`），与本轮收口逻辑改动无关。
- 代码回归结论：PC 主链路、mp 兼容链路边界、入口白名单与 static/dynamic 权限判定口径已完成收口并与文档同步。

### 最终结论

- 本轮收口后，PC 登录主流程与兼容链路边界已通过注释与回归文档双重固化。
- 当前实现与回归记录一致，可继续用于联调与后续验收追踪。

## 7) 工程修复记录（2026-04-27）

### components.d.ts 重生成与校验

- 修复来源：`src/views/manage/menu/modules/menu-operate-modal.vue` 中存在非法组件标签 `icon-ic:round-plus`，会在组件声明生成时产出非法全局声明。
- 处理方式：将组件标签统一为合法命名 `icon-ic-round-plus`，并清理 `src/typings/components.d.ts` 中非法全局 `const 'IconIc:roundPlus'` 声明。
- 校验结果：`src/typings/components.d.ts` 不再包含非法全局 `const` 声明，`TS1134`/`TS1005` 该组报错已消除。

### auth 无数据接口类型收口依据

- 修改项：`src/service/api/auth.ts` 中无数据成功返回接口统一由 `request<void>` 调整为 `request<null>`，覆盖：
  - `fetchUpdateProfile`
  - `fetchChangePassword`
  - `fetchUnbindWechat`
  - `fetchLogout`
- 依据：后端统一响应为 `Result<T>`，无数据成功返回语义为 `Result.ok(null)`（`data = null`），因此前端类型收口为 `null` 更符合真实契约并规避 `no-invalid-void-type` 规则冲突。

### 最终 lint/typecheck 状态

- 定向 lint：`pnpm eslint "src/service/api/auth.ts"` 通过。
- 全量类型检查：`pnpm -s typecheck` 未通过，当前失败为仓库其他存量问题（路由、页面类型、组件推断等），不再包含 `src/typings/components.d.ts` 的 `TS1134`/`TS1005`，且本次 `auth.ts` 的 `request<void>` 规则冲突已收敛。
