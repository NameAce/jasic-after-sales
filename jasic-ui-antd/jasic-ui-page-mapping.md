# jasic-ui 页面一一映射清单

基准目录：`jasic-after-sales/jasic-ui/src/views`

目标目录：`soybean-admin-antd/src/views`

## 核心业务映射（本轮范围）

| jasic-ui 页面 | soybean 页面 | 对齐状态 | 说明 |
| --- | --- | --- | --- |
| `system/user/index.vue` | `system/user/index.vue` | 已对齐 | 搜索、列表、抽屉（新增/编辑/分配角色/重置密码/绑定大区）已覆盖 |
| `system/role/index.vue` | `system/role/index.vue` | 已对齐 | 搜索、列表、角色编辑、菜单分配与数据范围校验已覆盖 |
| `system/menu/index.vue` | `system/menu/index.vue` | 已对齐 | 主体类型切换、树形菜单、菜单拷贝、发布能力已覆盖 |
| `org/companyType/index.vue` | `org/index.vue`（`companyType` Tab） | 已对齐 | 合并为 Tab 化页面 |
| `org/company/index.vue` | `org/index.vue`（`company` Tab） | 已对齐 | 合并为 Tab 化页面 |
| `org/contract/index.vue` | `org/index.vue`（`hqFirst`/`firstSecond` Tab） | 已对齐 | 含 CRM 导入模式 |
| `org/region/index.vue` | `org/index.vue`（`area` Tab） | 已对齐 | 区域逐级下钻 |
| `notify/index.vue` | `notify/index.vue` | 已对齐 | 待办/历史、已读、跳转工单详情 |
| `workOrder/index.vue` | `work-order/index.vue` | 已对齐 | 查询、状态统计、详情抽屉、建单弹窗 |
| `log/operLog/index.vue` | `log/index.vue` | 已对齐 | 搜索、分页、详情、批删、清空 |
| `system/*`（dict/config/notifyTemplate/machineBarcode/syncTask/faultRepairConfig/roleTemplate/region） | `advanced-modules/index.vue` | 已对齐 | 多个 system 页在 soybean 汇聚为单页多 Tab |

## 账号与基础页映射（非本轮主任务）

| jasic-ui 页面 | soybean 页面 | 说明 |
| --- | --- | --- |
| `login/index.vue` | `_builtin/login/index.vue` | 登录流程已做兼容对齐 |
| `login/chooseCompany.vue` | `_builtin/choose-company/index.vue` | 选公司流程已对齐 |
| `dashboard/index.vue` | `home/index.vue` | 首页能力映射 |
| `account/profile.vue` | `user-center/index.vue` | 个人中心映射 |
| `error/404.vue` | `_builtin/404/index.vue` | 错误页映射 |

## 命名差异与路由映射规则

- `workOrder`（jasic-ui）映射为 `work-order`（soybean 命名风格）。
- `advanced-modules` 承载原 `system/*` 多页面能力，路由通过 `ROUTE_NAME_TO_MODULE_KEY` 自动定位 Tab。
- `org` 模块采用「单页面多 Tab」以收敛重复逻辑；与 jasic-ui 多页面能力保持功能等价。
