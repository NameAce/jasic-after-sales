# jasic-ui 全量对齐回归与验收结论

## 回归范围

- 模块：`system/org/notify/work-order/company-address/log/advanced-modules`
- 维度：查询、重置、分页、增改弹窗、必填拦截、状态展示、按钮权限、文案一致性

## 执行结果

- 页面入口与路由映射：通过
- `system/user/role/menu` 主干能力：通过
- `org/work-order/notify/company-address/log/advanced-modules`：通过
- i18n 文案一致性：核心业务文案通过；少量历史硬编码属于非阻断优化项

## 2026-04-27 P0/P1 专项回归

- `system/user`：通过  
  - 列表补齐 `ID` 列；操作位次已按基准调整（编辑/分配角色/绑定大区/重置密码/强制下线/删除）。
- `work-order`：通过  
  - 列顺序已按 jasic-ui 对齐；建单入口收敛为单主入口“新增工单”（下拉选择三类建单模式）。
- `system/menu`：通过  
  - 主体类型切换不再自动触发查询，改为显式“查询”按钮触发。
- `notify`：通过  
  - 保留“标记已读”扩展能力，但仅在待处理项显示，历史记录项不展示行内操作。

## 接口回归（本轮关联项）

- `system/user` 相关接口调用参数保持不变：`listUser/addUser/updateUser/deleteUser/assignUserRoles/resetPwd/kickoutUser/getUserRegions/assignUserRegions`。
- `work-order` 查询与统计接口参数保持不变：`listWorkOrder/countWorkOrderStatus`。
- `system/menu` 查询接口保持不变：`menuTree(subjectType)`，仅调整触发时机。
- `notify` 接口保持不变：`getNotifyTodoCount/getNotifyTodoPage/markNotifyMessageRead`。

## 自动化检查记录

- `pnpm -s build:test`：通过（可成功产物构建）
- `pnpm -s typecheck`：未通过（仓库既有类型问题）
  - 代表性错误：
    - `src/locales/langs/en-us.ts` / `src/locales/langs/zh-cn.ts` 路由 i18n key 类型约束不匹配
    - `src/router/elegant/routes.ts` 路由声明与类型不一致
    - `src/views/org/index.vue`、`src/views/advanced-modules/index.vue`、`src/views/system/role/index.vue` 等存在历史类型问题
  - 判定：非本轮新增文档改动引入，属于当前分支存量问题

## 验收结论

- 页面级验收：通过（本轮目标页面均形成对照并完成对齐）
- 交互级验收：通过（必填与核心校验行为与基准一致）
- 文案级验收：通过（关键业务文案已统一；可继续增量抽离长文本）
- 回归级验收：通过（核心流程无阻断）

## 差异说明（留档）

1. `system_dict-data` 合并进 `advanced-modules/dict`，未拆独立页面  
   - 原因：当前前端采用单路由聚合设计  
   - 影响：无功能缺失

2. `org` 采用单页多 Tab，而非 jasic-ui 多页面  
   - 原因：工程架构收敛  
   - 影响：功能与字段行为保持一致

3. 局部提示文案仍有硬编码（非关键路径）  
   - 原因：历史页面增量改造  
   - 处理：已纳入后续 i18n 精修清单
