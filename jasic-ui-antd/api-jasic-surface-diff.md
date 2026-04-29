# jasic-ui 与 soybean-admin-antd API 面对照表

对照基准：jasic-ui `src/api/*.js`（2026-04 快照）与本仓库 `src/service/api/*.ts`。分页字段以各接口实际入参/返回为准；本仓库 `createFlatRequest` 已解包为业务体 `data`。

## 模块清单

| jasic-ui 文件 | 本仓库文件 | 说明 |
|---------------|------------|------|
| auth.js | auth.ts | 已对齐微信拆分为 status / qrcode / unbind |
| org.js | org.ts | 外部公司、区域、CRM 导入路径已按 jasic 对齐 |
| workOrder.js | work-order.ts | URL/Method 已与 jasic 对齐 |
| system.js | system.ts + system-manage.ts | 本仓库 system 为子集；reset-pwd 等与 jasic 对齐 |
| notify.js | notify.ts | 路径一致；分页返回含 `rows`（与部分后端 Page 一致） |
| log.js | log.ts | 一致 |
| companyAddress.js | company-address.ts | 一致 |
| file.js | file.ts | `POST/GET` 上传与预览等已在本仓库封装（见 `src/service/api/file.ts`） |

## 分页与列表字段

| 场景 | jasic-ui / 后端常见 | 本仓库类型/页面 |
|------|---------------------|-----------------|
| MyBatis-Plus 分页请求 | `current`, `size` | `WorkOrderQuery`、`SysCompanyQuery`、`OperLogQuery` 等使用 `current`/`size` |
| 通知待办分页返回 | 视后端 | `NotifyMessagePageResultVO` 使用 `rows` + `total` |
| 操作日志分页 | 视后端 | `OperLogPageResult` 使用 `records` + `total` |
| 高级模块列表 | 视后端 | `advanced-modules/index.vue` 分页 `current`/`size`，列表兼容 `records`/`rows`/`list` |

## URL / Method 差异摘要（已在本仓库修正的项）

| 模块 | jasic-ui | 原 soybean（修正前） | 结论 |
|------|----------|----------------------|------|
| org | `GET /org/company/external/list` | `GET /org/external-company/list` | 已改本仓库 |
| org | `GET /org/company/external/:id/import-preview` | 无 | 已新增 |
| org | `GET /org/area/options`、`GET /org/area/:code` | `GET /org/region/tree` | 已改为 jasic 区域接口 |
| org | HQ/一级 CRM `.../crm-import/list`、`.../crm-import` | 曾用单一路径 `.../import-crm`（`importExternalCompanyByCrm`） | 已拆为 jasic 两套，旧函数已删除 |
| org | 一级二级命名 `listFirstSecondRelation` 等 | `*FirstSecondContract*` | 已增加 jasic 同名导出；补充 `PUT /org/contract/first-second`（`updateFirstSecondRelation`） |
| auth | 微信 `status` / `qrcode` / `unbind` | 单 `POST /auth/wechat-bind` | 已拆分为三接口 |
| auth | `PUT /auth/profile`（updateProfile） | 曾用 `GET /auth/profile`（fetchGetProfile） | 已改为 `fetchUpdateProfile` |
| system | `PUT /system/user/reset-pwd` + body | `PUT /system/user/:id/reset-password` | 已改本仓库 |
| work-order | 见 jasic `workOrder.js` 全表 | 多条路径/Method 不一致 | 已按 jasic 重写 |

## 环境与代理

| 项 | jasic-ui | 本仓库 |
|----|----------|--------|
| 开发 baseURL | `/api`（axios） | `getServiceBaseURL`：`VITE_HTTP_PROXY=Y` 时为 `/api` |
| 代理 | `vue.config.js` `/api` → target | `build/config/proxy.ts`：`proxyPattern` `/api` → `VITE_SERVICE_BASE_URL` |
| 构建 | Vue CLI | `vite build --mode prod` / `build:test` |

## 请求层行为（非 URL）

| 项 | jasic-ui | 本仓库 |
|----|----------|--------|
| 成功码 | `00000` | `VITE_SERVICE_SUCCESS_CODE`（默认 `00000`） |
| 登录失效 | `A0100` MessageBox | `VITE_SERVICE_LOGOUT_CODES` 含 `A0100` |
| 无权限 | `A0200` 固定文案 + warning | `VITE_SERVICE_FORBIDDEN_CODES`（默认 `A0200`）+ 同等提示 |
| apifoxToken | 无 | 仅当配置 `VITE_APIFOX_TOKEN` 时发送 |
| 超时 | 30000ms | 已与 jasic 对齐 `timeout: 30000` |
| refresh token | 无 | 保留 `handleExpiredRequest` |

## 页面完成度（与 jasic-ui 能力对照）

| 模块 | 说明 |
|------|------|
| 组织 `views/org/index.vue` | 公司类型/公司分页与 CRUD、总部-一级签约与 CRM 导入列表、一级二级关系与 CRM、外部公司列表与导入预览、`listAreaOptions`/`getAreaDetail` 行政区域逐级查看；与 `org.ts` 路径一致 |
| 高级模块 `views/advanced-modules/index.vue` | **采用单路由深化**（未拆多 `view.*`）：字典/参数/通知模板/条码/故障配置/角色模板的列表分页与增删改及字典与参数缓存刷新；同步任务行内「执行」调用 `runSyncTask`；角色模板「从平台同步」调用 `syncRoleTemplateFromPlatform`。**说明**：`POST /system/sync-task/:id/run`、`POST /system/role-template/sync` 及通知/条码等待删改路径若与贵司后端不一致，请在联调时按 Network 调整 `system.ts`。 |
| 区域（系统） | `listRegion` 仍为只读列表；组织侧行政区域以 `org` 的 `/org/area/*` 为准 |

## 菜单页 -> API 函数 -> 后端 URL（本轮基线）

| 菜单页 | 页面文件 | API函数（service/api） | URL + Method | 与 jasic 对照 |
|------|----------|------------------------|--------------|----------------|
| system/user | `src/views/system/user/index.vue` | `listUser` `addUser` `updateUser` `deleteUser` `assignUserRoles` `resetPwd` `kickoutUser` `listRegion` `getUserRegions` `assignUserRegions` | `/system/user/list` `GET`；`/system/user` `POST/PUT`；`/system/user/:id` `DELETE`；`/system/user/:id/roles` `PUT`；`/system/user/reset-pwd` `PUT`；`/system/user/:id/kickout` `POST`；`/system/region/list` `GET`；`/system/region/:id/regions` `GET/PUT` | 已对齐 |
| org | `src/views/org/index.vue` | `listCompanyType` `addCompanyType` `updateCompanyType` `deleteCompanyType`；`listCompany` `addCompany` `updateCompany` `deleteCompany`；`listHqFirstContract` `addHqFirstContract` `updateHqFirstContract` `deleteHqFirstContract`；`listFirstSecondRelation` `addFirstSecondRelation` `updateFirstSecondRelation` `deleteFirstSecondRelation`；`listCrmHqFirstContractImport` `importCrmHqFirstContract`；`listCrmFirstSecondRelationImport` `importCrmFirstSecondRelation`；`listExternalCompany` `getExternalCompanyImportPreview`；`listAreaOptions` `getAreaDetail` | `/org/company-type/*`；`/org/company*`；`/org/contract/hq-first*`；`/org/contract/first-second*`；`/org/contract/*/crm-import*`；`/org/company/external/*`；`/org/area/options` `/org/area/:code` | 已对齐；`/org/region/tree` 已下线 |
| notify | `src/views/notify/index.vue` | `getNotifyTodoCount` `getNotifyTodoPage` `markNotifyMessageRead` | `/system/notify/todo/count` `GET`；`/system/notify/todo/page` `GET`；`/system/notify/message/:id/read` `POST` | 已对齐 |
| advanced-modules | `src/views/advanced-modules/index.vue` | `listDictType` `addDictType` `updateDictType` `deleteDictType` `refreshDictTypeCache`；`listSystemConfig` `addConfig` `updateConfig` `deleteConfig` `refreshConfigCache`；`listNotifyTemplate` `getNotifyTemplate` `listNotifyTemplateChannels` `saveNotifyTemplateChannels` `addNotifyTemplateCustom` `updateNotifyTemplateCustom` `deleteNotifyTemplateCustom` `previewNotifyTemplate` `refreshNotifyTemplateCache`；`listMachineBarcode` `getMachineBarcode` `fullSyncMachineBarcode`；`listSyncTask` `getSyncTask` `listSyncTaskHandlerOptions` `addSyncTask` `updateSyncTask` `executeSyncTask` `listSyncTaskLog`；`listFaultRepairConfig` `getFaultRepairConfig` `addFaultRepairConfig` `updateFaultRepairConfig` `listFaultRepairConfigCompanyOptions`；`listRoleTemplate` `getRoleTemplate` `addRoleTemplate` `updateRoleTemplate` `deleteRoleTemplate` `syncRoleTemplate` `roleTemplateDataScopeOptions` `roleTemplateDataScopeOptionMap` `typeCodeMenuTree` `typeCodeMenuIds` `assignTypeCodeMenus`；`listRegion` `addRegion` `updateRegion` `deleteRegion` | `/system/dict/*`；`/system/config/*`；`/system/notify/template*`；`/system/machine-barcode*`；`/system/sync-task*`；`/system/fault-repair-config*`；`/system/role-template*`；`/system/menu/type-code-*`；`/system/region*` | 已对齐 |
| company-address | `src/views/company-address/index.vue` | `listCompanyAddress` `addCompanyAddress` `updateCompanyAddress` `deleteCompanyAddress` | `/company-address/*` | 已对齐 |
| log | `src/views/log/index.vue` | `listOperLog` | `/system/oper-log/list` `GET` | 已对齐 |
| work-order | `src/views/work-order/index.vue` | `listWorkOrder` 及相关工单流程接口 | `/work-order/*` | 已对齐 |

## 2026-04 本轮对齐补充

- 组织 CRM 导入已从“直接触发”改为“列表勾选 `snapshotIds` 后提交”，与 jasic 导入交互一致。
- 通知中心“标记已读”按钮已阻断行点击冒泡，避免误跳转工单详情。
- 动态路由层增加 legacy `manage/systemManage` 入口过滤，避免误入旧接口联调链路。
- 系统用户页补齐：新增/编辑/删除、分配角色、重置密码、强制下线、绑定大区。
- 角色模板补齐：模板菜单分配、数据范围选项联动及必填校验。
- 同步任务日志补齐：状态筛选、处理器/任务编码字段展示。
- 工单详情与建单弹窗补齐关键校验：手机号格式、故障信息最小填写、退回方式/快递单号联动、条码信息预填。

### 2026-04-27 system 菜单对齐补记

- `system_config/system_dict-type/system_dict-data/system_notify-template/system_machine-barcode/system_sync-task/system_fault-repair-config/system_role-template/system_region` 动态路由别名统一落到 `advanced-modules`，避免误触发 `system_menu` 或 `notify` 页面链路。
- `advanced-modules` 新增“路由名 -> Tab”自动同步：菜单直达时自动定位到目标模块并触发对应列表加载。
- `roleTemplate` 对齐 jasic 口径：文案改为“分配菜单/全量同步到公司”，新增“管理员模板”开关及“数据范围必须属于当前类型”校验。
- 其他 system 子模块对齐项：通知模板列头与开关文案、条码档案补齐 `custId/salesOrg/productName` 展示、配置/角色模板列表补充备注列。
- 已知差异：`system_dict-data` 按当前计划仍并入 `dict` 模块（首请求仍为 `/system/dict/type/list`），未拆独立数据项页。

## 联调清单验证记录（自动化部分）

- `pnpm build:test`（Vite `--mode test`）已通过。
- `pnpm typecheck` 未通过；当前主要为仓库既有类型问题（如路由类型、部分 SFC 组件类型推断、i18n route key 扩展），不由本轮新增改动引入。
- 本轮新增改动（动态菜单 legacy 过滤 + `org.ts` 下线 `/org/region/tree`）已通过 `pnpm run build:test` 编译。
- 登录、菜单、各列表与 A0200/A0100 提示需在连真实后端时做人工 spot-check（参见 `after-sales-uat-checklist.md`）。
