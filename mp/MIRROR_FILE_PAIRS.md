# aftersale 与 contractor 镜像文件对清单

## 基准约定

- 目标：双端保留各自文件，优先做到同路径、同 props 签名、同 DOM 结构、同样式语义。
- 基准标记：`contractor` / `aftersale` 表示该对文件的统一实现主参考端。
- 允许差异：仅限白名单中的差异类型；其余镜像文件应保持同源组织与实现策略。

## 已对齐镜像对

| 分类 | aftersale | contractor | 统一基准 |
| --- | --- | --- | --- |
| component | `mp/aftersale/src/components/CustomNavBar/CustomNavBar.vue` | `mp/contractor/src/components/CustomNavBar/CustomNavBar.vue` | contractor |
| component | `mp/aftersale/src/components/RepairTypeSelector/RepairTypeSelector.vue` | `mp/contractor/src/components/RepairTypeSelector/RepairTypeSelector.vue` | aftersale |
| component | `mp/aftersale/src/components/MediaUploadField/MediaUploadField.vue` | `mp/contractor/src/components/MediaUploadField/MediaUploadField.vue` | aftersale |
| component | `mp/aftersale/src/components/VoiceInputField/VoiceInputField.vue` | `mp/contractor/src/components/VoiceInputField/VoiceInputField.vue` | aftersale |
| page | `mp/aftersale/src/pages/historicalRecord/index.vue` | `mp/contractor/src/pages/historicalRecord/index.vue` | aftersale |
| page | `mp/aftersale/src/pages/address/edit.vue` | `mp/contractor/src/pages/address/edit.vue` | aftersale |
| page | `mp/aftersale/src/pages/address/index.vue` | `mp/contractor/src/pages/address/index.vue` | contractor |
| api/domain | `mp/aftersale/src/api/mapRepairsToFaultPointRecords.ts` | `mp/contractor/src/api/mapRepairsToFaultPointRecords.ts` | aftersale |
| config | `mp/aftersale/.gitignore` | `mp/contractor/.gitignore` | contractor |

## 待继续推进镜像对

| 分类 | aftersale | contractor | 建议基准 |
| --- | --- | --- | --- |
| - | - | - | - |

## 本轮收尾验收记录（2026-04-17）

- `.gitignore`：已将 `contractor` 的源码误产物忽略策略同步到 `aftersale`，双端对 `src/**/*.js`、`src/pages/**/*.js` 等规则保持一致。
- 列表视觉（代码级对照）：`address/index.vue`、`historicalRecord/index.vue` 两端 DOM 结构与样式语义保持同源；`order/list.vue` 保持“视觉 token 与卡片语义一致、业务结构按角色分叉”的约束。
- 差异结论：当前仅保留白名单内差异（`my/login` 与角色域交互），未发现新增的非白名单漂移项。

## 长期允许差异页面

| 分类 | aftersale | contractor | 差异原因 |
| --- | --- | --- | --- |
| page | `mp/aftersale/src/pages/my/index.vue` | `mp/contractor/src/pages/my/index.vue` | 角色域与业务目标不同，交互区块和权限流程长期分叉 |
| page | `mp/aftersale/src/pages/login/index.vue` | `mp/contractor/src/pages/login/index.vue` | 鉴权入口、身份校验和文案语境不同，强行镜像会降低可维护性 |

## 允许差异白名单

- API 名称与路由前缀差异（如 `/api/system/work-order/*` 与历史兼容接口别名）。
- DTO 字段映射差异（同语义字段在双端命名不同，但需在文件注释中说明映射关系）。
- 角色权限分支差异（师傅端/用户端权限判断、按钮显隐条件不同）。
- 端侧专有页面或模块（只在单端存在的页面、组件、主题扩展 token）。
- 文案与交互微差异（不改变业务语义，仅做端侧角色语境适配）。

### my/login 差异边界（专项）

- 允许差异：页面骨架、交互区块、文案语境、权限分支差异。
- 必须一致：接口语义、关键字段映射规则、错误处理级别、基础视觉 token 语义。
- 禁止漂移：无文档说明的接口字段漂移、同语义多套命名长期并存。
- 评审口径：先判断是否命中本专项白名单，再判断是否需要回收对齐。

## 主题 token 对齐约束

- 公共 token 以 `mp/aftersale/src/constants/theme.ts` 与 `mp/contractor/src/theme/colors.ts` 为准，要求键名与值保持一致。
- 端侧扩展 token 允许存在，但必须放在各自文件的扩展区，并与公共 token 语义隔离。
- 目录命名和存放位置仅做建议，不做强制迁移；约束重点是公共 token 契约一致。

