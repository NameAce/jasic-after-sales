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
| component | `mp/aftersale/src/components/VoicePlaybackList/VoicePlaybackList.vue` | `mp/contractor/src/components/VoicePlaybackList/VoicePlaybackList.vue` | aftersale（仅 `themeColor` 引入路径随目录约定不同） |
| component | `mp/aftersale/src/components/BaseButton/BaseButton.vue` | `mp/contractor/src/components/BaseButton/BaseButton.vue` | aftersale |
| component | `mp/aftersale/src/components/ListEmpty/ListEmpty.vue` | `mp/contractor/src/components/ListEmpty/ListEmpty.vue` | aftersale |
| component | `mp/aftersale/src/components/ListNoMore/ListNoMore.vue` | `mp/contractor/src/components/ListNoMore/ListNoMore.vue` | contractor |
| page | `mp/aftersale/src/pages/historicalRecord/index.vue` | `mp/contractor/src/pages/historicalRecord/index.vue` | aftersale |
| page | `mp/aftersale/src/pages/address/edit.vue` | `mp/contractor/src/pages/address/edit.vue` | aftersale |
| page | `mp/aftersale/src/pages/address/index.vue` | `mp/contractor/src/pages/address/index.vue` | contractor |
| api/domain | `mp/aftersale/src/api/mapRepairsToFaultPointRecords.ts` | `mp/contractor/src/api/mapRepairsToFaultPointRecords.ts` | aftersale |
| constants | `mp/aftersale/src/constants/historicalRecord.ts` | `mp/contractor/src/constants/historicalRecord.ts` | contractor |
| constants | `mp/aftersale/src/constants/repairForm.ts` | `mp/contractor/src/constants/repairForm.ts` | aftersale |
| theme | `mp/aftersale/src/constants/theme.ts` | `mp/contractor/src/theme/colors.ts` | 双端并列（公共段一致，顶部注释互指） |
| styles | `mp/aftersale/src/styles/variables.scss` | `mp/contractor/src/styles/variables.scss` | 双端并列（公共段 + 扩展段模板一致） |
| config | `mp/aftersale/.gitignore` | `mp/contractor/.gitignore` | contractor |
| http | `mp/aftersale/src/utils/http.ts` | `mp/contractor/src/utils/http.ts` | aftersale（`handleResponseBody` 抽取写法 + 分支顺序） |
| constants | `mp/aftersale/src/constants/apiMessages.ts` | `mp/contractor/src/constants/apiMessages.ts` | 双端并列（6 条常量字面一致） |
| api/auth | `mp/aftersale/src/api/auth.ts` | `mp/contractor/src/api/auth.ts` | 双端并列（`login / chooseCompany / getUserInfo / logout` 对齐 jasic-ui，C 端 `mp-login-*` 入口保留） |
| models/user | `mp/aftersale/src/models/user.ts` | `mp/contractor/src/models/user.ts` | 双端并列（`SysUserInfo / CompanySimple / LoginResult` 同形，C 端 `perms / companies` 保留为 optional） |
| utils/format | `mp/aftersale/src/utils/format.ts` | `mp/contractor/src/utils/format.ts` | 双端并列（`formatIsoDateTime / formatAmount / maskMobile / maskAddress`） |
| utils/orderStatus | `mp/aftersale/src/utils/orderStatus.ts` | `mp/contractor/src/utils/orderStatus.ts` | contractor（`ORDER_STATUS_TEXT_MAP / getStatusDesc / getStatusIcon / getStepIndex / isOrderStatus / isPendingMainStatus`） |

## 待继续推进镜像对

_（空）本轮 ⑥R 完成后，原「VoicePlaybackList 功能互斥」阻塞已消除；暂无未对齐项。_

## 已对齐镜像对（阶段 6.x：VoicePlaybackList 全量化 / VoiceInputField 职责拆分）

- `VoicePlaybackList/VoicePlaybackList.vue`（aftersale / contractor 双端 1:1，仅 `themeColor` 引入路径因目录约定不同）：
  - 合并为「播放全量版」：同时具备远程 `http(s)` url 的 `downloadFile` 预下载 + 本地 `tempFilePath` 直通（原 aftersale 能力）与 `deletable` 删除按钮（原 contractor 能力）。
  - 新 props：`items: VoicePlaybackItem[]`、`deletable?: boolean = false`、`downloadable?: boolean = true`。
  - 新事件：`remove(index)`。组件内部不再弹删除确认弹窗，只负责停播兜底；确认与 `v-model` 变更交给父级（VoiceInputField）。
  - 播放态兜底：对 `props.items.length` 做 watcher，长度缩短时统一 `stopAndResetPlayback`，避免删除后样式错位或残留驱动。
  - 只读场景兼容：`aftersale/pages/order/detail.vue` 两处 `<VoicePlaybackList :items="faultVoicePlaybackItems" />` 保持不变（`deletable` 默认 false、`downloadable` 默认 true，语义与旧实现完全一致）。
- `VoiceInputField/VoiceInputField.vue`（aftersale / contractor 双端 1:1，仅上传 API 不同：`uploadCustomerFile` vs `uploadSystemFile`）：
  - 降级为「录制 + 列表编排」组件：删除内置 `<view class="voice-list">` 模板块、`innerAudioContext`、`playVoice / playingIndex / playProgress / ignoreNextAudioStop / lastVoicePlayPath / scheduleClearIgnoreStop / clearIgnoreAudioStop / formatDurationSec / formatDisplayDuration / getPlayTotalSec`、以及对应 `.voice-list / .voice-item / .play-btn / .progress-bar / .duration / .delete-btn` scoped 样式。
  - 保留：`RecorderManager` 全量录制流程（权限申请、按住开始、上滑取消、pending/start/stop/error 状态机、录音计时、上传后入库）、`.voice-placeholder`「暂无录音」占位（决策点 #1：录制场景专属，留在 `VoiceInputField` 自管）。
  - 新增：`playbackItems` 计算属性（`{ url: v.tempFilePath || v.url || '', duration: v.duration }`），在「本地 tempFilePath 可直接播」与「草稿恢复后仅剩 url 也能走下载播放」两种形态间自动选路。
  - 新增：`onRemoveVoice(index)`（决策点 #2：确认弹窗与 `update:modelValue` 统一由父级 `VoiceInputField` 承担）。
  - 移除：旧 `showRecordedList` prop（原本用于「拆分期临时关闭内置列表」，拆分完成后不再需要）。jasicRepair / otherRepair 两页调用点 `<VoiceInputField v-model="formData.voiceList" />` 未使用该 prop，移除无破坏性影响。
- 远程 url 下载策略（决策点 #3）：`VoicePlaybackList` 内部 `/^https?:\/\//i` 判别，非 http(s) 直通，本地 `tempFilePath` 零成本、远端语音下载后再播；支持手动关闭 `downloadable`。
- 同页多实例互斥（决策点 #4）：当前仓库未出现同页多 `VoicePlaybackList` 场景（`aftersale/pages/order/detail.vue` 两处 `<VoicePlaybackList>` 位于 `v-if` 互斥分支），本轮不引入跨实例全局停播总线，保留给将来真实需要时实现。

## 已对齐镜像对（阶段 5.x：FormItemAnchor / formFieldScrollFocus）

- `FormItemAnchor/FormItemAnchor.vue`（以 aftersale 为准）：contractor 的 `:id` 改为 `formFieldAnchorId(name)`（加 `ff-anchor-` 前缀），`class` 统一为 `form-field-anchor`，样式改为不占布局流的 `position: absolute; 1px × 1px`（父级 `.form-item` 已 `position: relative`）。
- `utils/formFieldScrollFocus.ts`（以 aftersale 为准）：contractor 从单工具 `triggerScrollIntoView` 扩展为与 aftersale 完全一致的六工具（`formFieldAnchorId / getFirstInvalidFieldKey / SCROLL_TOP_OFFSET_PX / scrollPageToFormFieldKey / scrollToFirstInvalidUniFormField / triggerScrollIntoView`）。
- 业务调用点：`pages/address/edit.vue` 保持不变（`triggerScrollIntoView` 签名未变，由工具内部自动把 `fieldKey` 映射为 `ff-anchor-{fieldKey}`，`scroll-view` 的 `scroll-into-view` 与 `FormItemAnchor.id` 同步加前缀，语义对称）。`TabBar.vue` / `pages/order/list.vue` 的 `scroll-into-view` 用于 Tab 键名而非表单字段锚点，不纳入本镜像。

## 本轮收尾验收记录（2026-04-17）

- `.gitignore`：已将 `contractor` 的源码误产物忽略策略同步到 `aftersale`，双端对 `src/**/*.js`、`src/pages/**/*.js` 等规则保持一致。
- 列表视觉（代码级对照）：`address/index.vue`、`historicalRecord/index.vue` 两端 DOM 结构与样式语义保持同源；`order/list.vue` 保持“视觉 token 与卡片语义一致、业务结构按角色分叉”的约束。
- 差异结论：当前仅保留白名单内差异（`my/login` 与角色域交互），未发现新增的非白名单漂移项。

## 本轮收尾验收记录（2026-04-21，阶段 2.3~2.4）

- 常量：`constants/historicalRecord.ts` 已将 aftersale 对齐为 contractor 全量（补 `WORK_ORDER_FLOWS_HISTORY_STORAGE_KEY`）；`constants/repairForm.ts` 顶部补 C 端专有注释（`JASIC_BRAND_CODE` / `REPAIR_TYPE_TO_SERVICE_MODE` / `CUSTOMER_WORK_ORDER_REPORT_BIZ_TYPE` 不镜像到 contractor）。
- 主题：`constants/theme.ts` 与 `theme/colors.ts` 公共段分节注释统一为「双端公共 token」；扩展段分节统一为「用户端 / 师傅端独有 · 扩展」模板。
- SCSS：`styles/variables.scss` 公共段保持字面一致；扩展段分节注释统一为「[端名] 端侧扩展」模板；`$surface-track` / `$voice-panel-track` 统一为「`$voice-panel-track` 真源 + `$surface-track` 别名」，双端同名并列。
- 组件镜像（新增已对齐）：`BaseButton/BaseButton.vue`（以 aftersale 为准，补 shadow + `constant/env(safe-area-inset-bottom)` 兜底）、`ListEmpty/ListEmpty.vue`（以 aftersale 为准，将空状态样式固化为 scoped 样式）、`ListNoMore/ListNoMore.vue`（以 contractor 为准，使用 `$text-slate-400` 与不带 trailing comma 的 props 默认值）。
- 组件镜像（待推进）：`FormItemAnchor/FormItemAnchor.vue`（依赖 `utils/formFieldScrollFocus.ts` 统一）与 `VoicePlaybackList/VoicePlaybackList.vue`（远程 url 下载 vs deletable 删除功能差异）已纳入「待继续推进镜像对」。

## 本轮收尾验收记录（2026-04-21，阶段 2.5：接口契约收口）

- 基线确认（白名单边界）：
  - 仅长期保留差异：`/customer/*`、`/auth/mp-*`、`my/login` 角色语境分叉。
  - 非白名单差异已收口到统一契约：`code/msg/data`、成功码 `00000`、`A0100/A0200`、分页字段、`mainStatus` 主枚举、权限字段 shape。
- aftersale `api/workOrder.ts`：
  - 列表查询入参从 `tabStatus` 回收为 `mainStatus`。
  - `isAsc` 语义改为 `'asc' | 'desc'`，不再使用 `boolean`。
  - `pageSize` 默认值由 `500` 回收为 `10`，与 `PageQuery` 基线一致。
- contractor `api/workOrder.ts`：
  - `mainStatus` 映射仅保留主枚举：`PENDING_ASSIGN | PENDING_TECH_ACCEPT | IN_PROGRESS | COMPLETED | CLOSED`。
  - 聚合展示态 `WAIT_ACCEPT`（= `PENDING_ASSIGN + PENDING_TECH_ACCEPT`）长期保留，降维为 `PENDING_TECH_ACCEPT`；
    依据真源 `jasic-common/.../WorkOrderStatusConstants.java` `DisplayStatus` + `WorkOrderListVO.displayStatus`
    `allowableValues = "WAIT_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED"`，属后端正式字段，不回收。
  - 前端自造历史别名 `PROCESSING / DONE / REPAIRING / IN_REPAIR / PENDING / WAITING / FINISHED / CLOSED_EVAL`
    已在契约统一阶段回收（aftersale `MAIN_STATUS_TO_UI` + contractor `mapMainStatusToOrderStatus` /
    `mapDisplayStatusToOrderStatus` / `if (u === 'WAIT_ACCEPT')` 分支注释），三端禁止再引入。
- API 重复成功码判断收口：
  - contractor `api/auth.ts` 删除重复 `res.code` 判定，统一回归 `utils/http.ts` 失败分支处理。
  - contractor `api/workOrder.ts` 删除列表、统计、详情、派单、接单、转单、关单、维修登记、复检登记等重复 `res.code` + toast 样板判断。
- 回归执行结果（本机）：
  - `corepack pnpm -C mp/aftersale lint`：通过（0 error，1 warning，历史文件 `VoicePlaybackList.vue`）。
  - `corepack pnpm -C mp/contractor lint`：通过（0 error，2 warnings，历史文件 `VoicePlaybackList.vue` / `SiteWorkbench.vue`）。
  - `corepack pnpm -C mp/aftersale type-check`：失败，`vue-tsc` 在 Node.js `v24.13.0` 环境报工具兼容错误（`Search string not found: /supportedTSExtensions`）。
  - `corepack pnpm -C mp/contractor type-check`：失败，存在仓库既有类型环境冲突（`@dcloudio/types` 与 `@uni-helper` 等定义重复）及既有组件类型告警，非本轮改动引入。

## 本轮收尾验收记录（2026-04-21，阶段 2.6：双端列表触发与动作收口）

- aftersale 列表参数收口：
  - `pages/order/list.vue` Tab 筛选已从 `tabStatus` 全量切换到 `mainStatus`（`待接单 -> PENDING_TECH_ACCEPT`、`维修中 -> IN_PROGRESS`、`已完成 -> COMPLETED`、`已关闭 -> CLOSED`）。
  - 注释明确区分“UI 文案展示（含 `displayStatus`）”与“接口筛选主枚举（`mainStatus`）”。
- contractor 评价页边界：
  - `pages.json` 与 `pages/order/list.vue` 无 `pages/evaluate/index` 路由与跳转残留。
  - `pages/order/detail.vue` 保持“客户评价”只读展示，不新增评价入口按钮。
- 按钮显隐策略：
  - contractor 列表固化为“`availableActions` 优先 + 前端权限二次过滤 + 状态兜底仅过渡期”。
  - aftersale 列表固化为“接口布尔字段（`canEvaluate / canUploadSendExpress`）控制显隐，页面仅渲染”。
- 长期差异边界补充：
  - contractor 不再维护独立评价页面，评价能力长期保留为详情只读展示（与 C 端评价提交链路形成职责边界）。

## 本轮收尾验收记录（阶段 6.x：语音播放 / 录制组件对齐）

- 双端 `VoicePlaybackList/VoicePlaybackList.vue` 重写为「播放全量版」（`items / deletable / downloadable` + `remove` 事件），两端除 `themeColor` 引入路径外字面 1:1。
- 双端 `VoiceInputField/VoiceInputField.vue` 降级为「录制 + 列表编排」组件（删除内嵌播放 UI、`innerAudioContext` 及对应样式），两端除 `uploadCustomerFile` / `uploadSystemFile` 外字面 1:1；原 `showRecordedList` prop 随拆分完成一并移除。
- 只读场景：`aftersale/pages/order/detail.vue` 的两处 `<VoicePlaybackList :items="faultVoicePlaybackItems" />` 保持原调用，`deletable` 默认 false、`downloadable` 默认 true，语义与旧实现一致。
- 决策点落地：#1 占位留在 VoiceInputField 自管；#2 删除确认弹窗交父级；#3 `downloadable` 默认 true，内部用 `/^https?:\/\//i` 判别走下载或直通；#4 同页多实例互斥暂不实现（当前仓库无真实场景）。
- 回归执行结果（本机）：
  - ReadLints 扫描四个核心文件 + `aftersale/pages/order/detail.vue`：0 error / 0 warning。
  - `lint` 中此前反复出现的“历史文件 `VoicePlaybackList.vue`”告警按阶段 2.5 记录应随重写一并消除，实际是否归零需在命令行 `corepack pnpm -C mp/{aftersale,contractor} lint` 下复核（本轮暂未再次运行 lint 脚本）。

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
- aftersale `api/workOrder.ts` 保留 `Customer*` 前缀函数（C 端专属 `/customer/work-order/*`），以 JSDoc 指向 jasic-ui 对照项 + 文件尾部 `export { getCustomerWorkOrder as getWorkOrder, ... }` re-export 桥弥合 grep 一致性；contractor 侧不得出现 `Customer*` 前缀函数。
- `mp/aftersale/src/constants/theme.ts` 与 `mp/contractor/src/theme/colors.ts` 目录命名不强制镜像，仅要求公共 token 字面一致；两份文件顶部互相注释指向。

### my/login 差异边界（专项）

- 允许差异：页面骨架、交互区块、文案语境、权限分支差异。
- 必须一致：接口语义、关键字段映射规则、错误处理级别、基础视觉 token 语义。
- 禁止漂移：无文档说明的接口字段漂移、同语义多套命名长期并存。
- 评审口径：先判断是否命中本专项白名单，再判断是否需要回收对齐。

## 主题 token 对齐约束

- 公共 token 以 `mp/aftersale/src/constants/theme.ts` 与 `mp/contractor/src/theme/colors.ts` 为准，要求键名与值保持一致；两份文件分节注释统一为「`// --- 双端公共 token ---`」与「`// --- [端]端独有 / 扩展 ---`」模板。
- 端侧扩展 token 允许存在，但必须放在各自文件的扩展区，并与公共 token 语义隔离；contractor 扩展 token 会在注释中明确标注「仅在 contractor 使用，不镜像到 aftersale」。
- SCSS 真源 `src/styles/variables.scss`：公共段字面一致；扩展段统一为「`// [端名] 端侧扩展` + 2~3 行说明」模板；如遇语义相同但命名漂移的 token（例 `$surface-track` / `$voice-panel-track`），统一为「一个真源 + 一个别名」双端同名并列。
- 目录命名和存放位置仅做建议，不做强制迁移；约束重点是公共 token 契约一致。

---

## 与 jasic-ui（PC 后台）对齐口径

mp 双端的 HTTP 契约层、登录流、工单状态枚举与权限模型以 jasic-ui 为真源。所有条目均标注基准文件路径与代码行号，改动 mp 侧 `utils/http.ts` 或 `api/*` 时必须同时核对此处。

### 响应体 shape

- 真源：[jasic-ui/src/utils/request.js](../jasic-ui/src/utils/request.js) L66-L83，响应拦截器只读 `res.code / res.msg / res.data`。
- 口径：`ApiResponse<T>` 必须为 `{ code: string; msg: string; data: T }`；禁止再出现 `result` / `message` 兼容字段。
- mp 侧约束：[mp/aftersale/src/utils/http.ts](aftersale/src/utils/http.ts) 与 [mp/contractor/src/utils/http.ts](contractor/src/utils/http.ts) 的 `ApiResponse<T>` 类型与导出形状保持同字面；`api/*` 调用点一律返回 / 透传 `.data`，不得再写 `res.result`、`res.message` 或 `unwrap(res)`。

### 成功码与业务错误码

- 真源：[jasic-ui/src/utils/request.js](../jasic-ui/src/utils/request.js) L71 `res.code !== '00000'`。
- 口径：
  - 成功码：严格等值 `'00000'`；禁止 `'0'` / `'200'` 等宽容判定。
  - A0100（登录过期）：清 token → 弹一次 modal 提示「登录已过期，请重新登录」→ `uni.reLaunch('/pages/login/index')`。
  - A0200（无权限）：`uni.showToast({ icon: 'none', title: '没有操作权限', duration: 1500 })`。
  - 其它非 `00000`：`uni.showToast({ icon: 'none', title: res.msg || '操作失败' })`。
  - HTTP 非 2xx：`res.data.msg || res.data.message || '网络错误'`（与 PC 端 axios 报错兜底同口径）。
- mp 侧约束：[mp/aftersale/src/utils/http.ts](aftersale/src/utils/http.ts) 与 [mp/contractor/src/utils/http.ts](contractor/src/utils/http.ts) 必须导出同名常量 `API_SUCCESS_CODE = '00000'`、`API_AUTH_EXPIRED = 'A0100'`、`API_NO_PERMISSION = 'A0200'`，业务层一律引用常量。

### 防重复弹框

- 真源：[jasic-ui/src/utils/request.js](../jasic-ui/src/utils/request.js) L7、L32-L49 的 `reloginPromptVisible` 门闩。
- 口径：mp 双端 `utils/http.ts` 使用同形的模块级 `authExpiredHandling` 标志位：A0100 / HTTP 401 短时间内仅弹一次 modal；`uni.reLaunch` 后复位。

### Token / 请求头

- 真源：[jasic-ui/src/utils/request.js](../jasic-ui/src/utils/request.js) L54-L57 仅设置 `config.headers.Authorization = token`（不带 `Bearer ` 前缀）。
- 口径：三端 Header 字面保持一致；mp 侧 `uni.setStorageSync` 的 token key 固定为 `token`（一次锁定，不再重命名）。

### 登录响应 shape

- 真源：[jasic-ui/src/store/modules/user.js](../jasic-ui/src/store/modules/user.js) L50-L67 解包 `token / needChooseCompany / companies / userInfo`，其中 `userInfo` 含 `perms / currentCompanyId / currentCompanyName / currentTypeCode / roles`。
- 口径：
  - contractor 的 `LoginResult / SysUserInfo`（见 [mp/contractor/src/utils/permissions.ts](contractor/src/utils/permissions.ts)）直接对齐同形。
  - aftersale 的 `UserInfo`（见 [mp/aftersale/src/stores/modules/user.ts](aftersale/src/stores/modules/user.ts)）即使 C 端暂不消费 `perms / companies`，也必须保留为 optional 字段，防止后端扩字段时解包出错。
  - 登录端点集合：`POST /api/auth/mp-login-*`（端侧专属）、`POST /api/auth/choose-company`、`GET /api/auth/user-info`、`POST /api/auth/logout` 双端必须齐全。

### 工单 `mainStatus` 枚举

- 真源：[jasic-ui/src/views/workOrder/index.vue](../jasic-ui/src/views/workOrder/index.vue) 与 [mp/contractor/src/api/order.ts](contractor/src/api/order.ts) 头部注释的字面枚举：`PENDING_ASSIGN | PENDING_TECH_ACCEPT | IN_PROGRESS | COMPLETED | CLOSED`。
- 口径：三端使用相同大写枚举；禁止 `pending / processing` 小写别名；[mp/contractor/src/utils/orderStatus.ts](contractor/src/utils/orderStatus.ts) 与 aftersale 对应常量文件 re-export 同一份枚举给 `api/order.ts` 使用。

### 权限码 `Perms`

- 真源：后端 `sys_menu.perms`（通过 `GET /api/auth/user-info` 下发到 `userInfo.perms`）。
- 口径：
  - [mp/contractor/src/utils/permissions.ts](contractor/src/utils/permissions.ts) 中 `Perms` 常量字符串必须与 `sys_menu.perms` 字面一致；未确认前保留 TODO 注释并沿用现值。
  - 权限判断统一使用 `userStore.hasPermission / canAny / canAll`，语义对标 jasic-ui 的 `v-hasPerms`。
  - aftersale（C 端）不消费 `Perms`，但 `UserInfo.perms` 字段形状保持一致。

### 字段布尔风格

- 真源：jasic-ui 与后端 DTO 习惯使用 0/1（如 `enabledFlag`、`isDefault`、`status`）。
- 口径：mp 双端 `*DTO / *VO` / 表单提交字段中的布尔语义一律使用 `number`（0/1），不得用 TypeScript 的 `boolean`。

### 错误文案字典

- 真源：[mp/aftersale/src/constants/apiMessages.ts](aftersale/src/constants/apiMessages.ts) 与 [mp/contractor/src/constants/apiMessages.ts](contractor/src/constants/apiMessages.ts)，两份文件字面完全一致。
- 口径：`utils/http.ts` 内部禁止出现硬编码中文文案，一律从 `@/constants/apiMessages` 引用同名常量。
- 常量清单（6 条，双端同名同值）：

  | 常量名                      | 字面值                   | 适用分支                              |
  | --------------------------- | ------------------------ | ------------------------------------- |
  | `API_MSG_AUTH_EXPIRED`      | `"登录已过期，请重新登录"` | A0100 / HTTP 401 modal 标题区         |
  | `API_MSG_NO_PERMISSION`     | `"没有操作权限"`          | A0200 业务码 toast                    |
  | `API_MSG_OPERATION_FAILED`  | `"操作失败"`              | 非 `00000` 其它业务码的兜底 toast     |
  | `API_MSG_NETWORK_ERROR`     | `"网络错误"`              | HTTP 非 2xx / fail 回调兜底 toast     |
  | `API_MSG_TIMEOUT`           | `"请求超时"`              | fail 回调 `errMsg.includes('timeout')` |
  | `API_MSG_BAD_RESPONSE`      | `"响应格式错误"`          | shape 校验失败（缺 `code` 字段等）    |

### 分页契约

- 真源：[jasic-ui/src/api](../jasic-ui/src/api) 的分页查询参数与后端 `PageQuery` / `PageResult`。
- 请求入参统一字段：`pageNum`（页码，从 1 开始）、`pageSize`（每页条数）、`orderByColumn`（排序字段，`camelCase`，映射到后端列）、`isAsc`（`"asc" | "desc"`，字面为字符串，禁止传 `boolean`）。
- 响应体统一形状：`{ pageNum: number; pageSize: number; total: number; records: T[] }`，禁止兼容 `list / rows / items / totalCount` 等旧命名。
- mp 侧约束：
  - contractor 列表接口（如 `listWorkOrder`）回传字面已对齐；aftersale 的 `listCustomerWorkOrder` 同口径，二次包装时也要保留四字段原样返回。
  - 本端空列表兜底允许 `records: []` + `total: 0`，但 `pageNum / pageSize` 必须回填请求值或后端下发值，不得为 `undefined`。

### DTO / VO / Model 后缀规约

- 契约层三层定义：
  - `*VO`（View Object）：**后端原样响应**，字段名与字面完全对齐 Java POJO；布尔语义字段（`isJasicProduct / canEvaluate / canUploadSendExpress / enabledFlag / isDefault` 等）一律 `number`（0/1）。
  - `*DTO`（Data Transfer Object）：**请求体**或分页查询体；字段名与字面对齐后端 `RequestBody / PageQuery`；布尔语义字段同样用 `number`。
  - `*Model`（无后缀或 `OrderDetail / OrderListItem` 等业务名）：**UI 展示模型**；允许将 VO 的 0/1 布尔归一为 TypeScript 的 `boolean`，以便模板 `v-if` 直接使用。
- 归属约束：类型声明出现在 `api/*.ts` 文件顶部或 `models/*.ts` 文件内；页面与组件不得自行 inline 重复声明同语义类型。
- 参考落地：`CustomerWorkOrderDetailVO` / `CustomerWorkOrderListVO`（VO 层）、`OrderDetail` / `OrderListItem`（UI Model 层）；contractor 已按此规约落地，aftersale 随阶段 C 对齐。

### 格式化函数名表

- 真源：[mp/aftersale/src/utils/format.ts](aftersale/src/utils/format.ts) 与 [mp/contractor/src/utils/format.ts](contractor/src/utils/format.ts)，两端函数名、签名、实现字面镜像。
- 统一导出清单：

  | 函数名               | 签名                                                 | 用途                                                               |
  | -------------------- | ---------------------------------------------------- | ------------------------------------------------------------------ |
  | `formatIsoDateTime`  | `(raw: unknown) => string`                           | 后端 `LocalDateTime` / ISO 字符串 → `YYYY-MM-DD HH:mm:ss` 展示形式 |
  | `formatAmount`       | `(raw: unknown, fallback?: string) => string`        | 金额 `number / string` → 两位小数字符串（`¥` 由调用点拼接）        |
  | `maskMobile`         | `(raw: unknown) => string`                           | 手机号 `138****1234` 脱敏                                          |
  | `maskAddress`        | `(raw: unknown) => string`                           | 详细地址 `XX省XX市***` 尾段脱敏                                    |

- 禁止漂移：页面与 `api/*.ts` 不得自行实现 `formatQuoteAmount / formatLatestSummaryTime / formatListRepairPriceText` 等同语义别名，一律从 `@/utils/format` 引用。

### Toast / Modal / Icon 字典

- Toast 参数字面（双端镜像，禁止漂移）：

  ```ts
  uni.showToast({ icon: 'none', title: <文案>, duration: 1500 })
  ```

  - 业务页面禁止使用 `icon: 'success' | 'error' | 'loading'`；成功态走页面内提示或导航，不走 toast。
  - 文案优先走 `@/constants/apiMessages` 常量或 `getApiMessage(res, fallback)`，禁止硬编码「成功」「失败」等 UI 文案。
- Modal 参数字面（仅用于登录失效 / 关键二次确认）：

  ```ts
  uni.showModal({ title: '提示', content: <文案>, showCancel: false })
  ```

- Material Icon slug 清单（工单主状态与业务入口，字面见 `utils/orderStatus.ts` 与 `pages/index/index.vue`）：

  | 用途                       | slug                   |
  | -------------------------- | ---------------------- |
  | `PENDING_ASSIGN`           | `pending_actions`      |
  | `PENDING_TECH_ACCEPT`      | `pending_actions`      |
  | `IN_PROGRESS`              | `build_circle`         |
  | `COMPLETED`                | `check_circle`         |
  | `CLOSED`                   | `task_alt`             |
  | 兜底（非合法状态）         | `info`                 |

  - 业务入口图标（本机维修 / 其它维修 / 客服 / 扫码 / 地址等）固化为 `mp/aftersale/src/assets/*.svg` 与 `mp/contractor/src/assets/*.svg` 中的文件名；新增入口图标需同时落盘两端并在 `MIRROR_FILE_PAIRS.md` 本节追加行。

### Customer 前缀桥接

- 背景：aftersale（C 端）调用的 `/customer/work-order/*` 系列是 mp 专属接口，函数名为历史约定的 `Customer*` 前缀；与 jasic-ui 的同语义函数（`getWorkOrder / listWorkOrder / countWorkOrderStatus` 等）语义对齐但路径不同。
- 保留原则：`mp/aftersale/src/api/workOrder.ts` 中 8 个 `Customer*` 函数（`getCustomerWorkOrder` / `listCustomerWorkOrder` / `countCustomerWorkOrderStatus` / `createCustomerWorkOrder` / `evaluateCustomerWorkOrder` / `getCustomerWorkOrderBarcodeInfo` / `getCustomerWorkOrderLatestSummary` / `updateCustomerWorkOrderSenderVoucher`）保留原名不迁；每个函数顶部必须补一行 JSDoc 指向 jasic-ui 对照项，便于三端 grep：

  ```ts
  /** 对应 jasic-ui listWorkOrder（C 端专属接口 `/customer/work-order/list`） */
  ```

- Re-export 桥（便于三端按 jasic-ui 命名 grep）：`mp/aftersale/src/api/workOrder.ts` 文件尾部追加不带 `Customer` 的别名桥，只 re-export、不重复实现：

  ```ts
  export {
    getCustomerWorkOrder as getWorkOrder,
    listCustomerWorkOrder as listWorkOrder,
    countCustomerWorkOrderStatus as countWorkOrderStatus,
  }
  ```

- 禁止漂移：contractor 侧不得出现 `Customer*` 前缀函数；aftersale 新增 C 端专属接口如继续使用 `Customer*` 前缀，必须同步追加 JSDoc 对照与 re-export 桥。

### http.ts 写法基准

- 真源：[mp/aftersale/src/utils/http.ts](aftersale/src/utils/http.ts) 与 [mp/contractor/src/utils/http.ts](contractor/src/utils/http.ts) 以 aftersale 的 `handleResponseBody` 抽取写法为基准，结构、分支顺序、常量引用字面镜像。
- 分支顺序（三端镜像，禁止调整）：

  ```text
  uni.request
    ├─ success 回调
    │   1. statusCode === 401            → handleAuthExpired → reject
    │   2. statusCode < 200 || >= 300    → pickHttpErrorMsg + toast → reject
    │   3. statusCode === 204            → resolve({ code: '00000', msg: '', data: null })
    │   4. shape 校验                    → 无 code 字段 → toast API_MSG_BAD_RESPONSE → reject
    │   5. handleResponseBody(code 分发)
    │        ├─ code === '00000'                 → resolve
    │        ├─ code === 'A0100' (AUTH_EXPIRED)  → handleAuthExpired → reject
    │        ├─ code === 'A0200' (NO_PERMISSION) → toast NO_PERMISSION → reject
    │        └─ else                             → toast msg || OPERATION_FAILED → reject
    └─ fail 回调
        1. errMsg.includes('timeout')    → toast API_MSG_TIMEOUT
        2. else                          → toast API_MSG_NETWORK_ERROR
  ```

- 强制约束：
  - 双端均须导出 `ApiResponse<T>`、`API_SUCCESS_CODE`、`API_AUTH_EXPIRED`、`API_NO_PERMISSION`、`resolveHttpUrl`、`http<T>(options)`、`getApiMessage(res, fallback)`。
  - 模块级 `authExpiredHandling` 门闩：A0100 / HTTP 401 短时间内仅弹一次 modal；modal `success` / `fail` 回调中复位。
  - 业务层一律用 `http<T>()` 调用 + `await res.data` 透传；禁止在页面里自行处理 `statusCode` 与业务码。

---

## API 命名与文件组织约定

### `/api` baseURL

- `utils/http.ts` 内部固定拼 `/api`：`API_BASE = (VITE_HTTP || '').replace(/\/$/, '') + '/api'`。
- `api/*.ts` 中业务路径字面 **不带** `/api` 前缀（如 `'/system/work-order/list'`），与 [jasic-ui/src/api](../jasic-ui/src/api) 保持一致，便于三端跨项目 grep。

### 函数命名动词表

| 动词          | 含义                         | jasic-ui 对照                          |
| ------------- | ---------------------------- | -------------------------------------- |
| `list`        | 分页 / 列表查询              | `listWorkOrder`                        |
| `get`         | 单条详情                     | `getWorkOrder`                         |
| `add`         | 新建                         | `addCompanyAddress`                    |
| `update`      | 全量 / 部分更新              | `updateWorkOrder`                      |
| `delete`      | 删除                         | `deleteCompanyAddress`                 |
| `setDefault`  | 设为默认                     | `setDefaultCompanyAddress`             |
| `upload`      | 文件上传                     | `uploadSystemFile`                     |
| `bind`        | 绑定文件 / 关联实体          | `bindBizFile`                          |
| `unbind`      | 解绑                         | `unbindBizFile`                        |
| `assign`      | 派单                         | `assignWorkOrder`                      |
| `accept`      | 技师接单                     | `techAcceptWorkOrder`                  |
| `transfer`    | 转单                         | `transferWorkOrder`                    |
| `repair`      | 维修登记                     | `repairWorkOrder`                      |
| `review`      | 复检                         | `reviewWorkOrder`                      |
| `close`       | 关单                         | `closeWorkOrder`                       |
| `count`       | 状态 / 数量统计              | `countWorkOrderStatus`                 |

- 统一去掉 `...API` 后缀：历史 `getCompanyAddressListAPI` → `listCompanyAddress`。
- 资源名使用后端原名：`CompanyAddress / CustomerAddress / WorkOrder / SystemFile / BizFile / Menu / Role / User / Config / DictType / DictData / RoleTemplate / Company / CompanyType / Contract / Region / MachineBarcode / SyncTask / FaultRepairConfig`。
- 禁止把 `Customer` 和 `Company` 语义混用（历史 `createCustomerAddressAPI` 误用在 contractor 的公司地址需一并纠正）。

### 文件划分

- 以 [jasic-ui/src/api](../jasic-ui/src/api) 的拆分为基准：
  - contractor `src/api/address.ts` → `src/api/companyAddress.ts`
  - contractor `src/api/order.ts` → `src/api/workOrder.ts`
  - aftersale `src/api/address.ts` → `src/api/customerAddress.ts`
  - aftersale `src/api/order.ts` → `src/api/workOrder.ts`
- 每个 API 文件内部使用分组注释模板：

  ```ts
  // --- User ---
  // --- Role ---
  // --- Menu ---
  ```

### 存储 key 前缀

- 历史 key `token` 保留（大量引用、破坏代价高，在 `utils/http.ts` 顶部 doc 锁定）。
- 新增 key 走 `jasic_*` 前缀：`jasic_company_id`、`jasic_user_info`，对齐 jasic-ui 的 `jasic_token / jasic_company_id` 风格。

---

## 允许长期差异白名单（PC 与 mp 之间）

以下条目**允许长期差异**，不强行拉齐；其余契约层条目必须与 jasic-ui 保持一致。

| 类别         | 具体差异                                                                       | 原因                                           |
| ------------ | ------------------------------------------------------------------------------ | ---------------------------------------------- |
| 前端框架     | jasic-ui: Vue 2 + Element UI；mp: Vue 3 + uni-app + @dcloudio/uni-ui            | 历史选型 + 端侧平台限制                        |
| 状态管理     | jasic-ui: Vuex；mp: Pinia + pinia-plugin-persistedstate                        | Vue 2 / Vue 3 生态差异                         |
| HTTP 库      | jasic-ui: axios；mp: `uni.request` 封装                                        | 小程序平台不支持 axios                         |
| 路由守卫     | jasic-ui: vue-router `beforeEach` + `permission.js`；mp: 页面级 `onLoad` + 白名单 | 小程序路由机制差异                             |
| Loading 指示 | jasic-ui: NProgress；mp: `uni.showLoading`                                     | 端侧原生能力                                   |
| 端侧专属接口 | `/api/customer/*`、`/api/auth/mp-*`                                            | 仅小程序消费，不在 jasic-ui 登录/业务链路      |
| 登录入口     | `pages/login/index.vue`、`pages/my/index.vue` 角色语境与文案                   | 用户端 / 师傅端 / PC 运营端身份与业务目标不同  |
| 扫码 / 录音  | `uni.scanCode`、`uni.getRecorderManager`                                       | 小程序专属 API，PC 无对应实现                  |

**注意**：端侧专属接口（`/api/customer/*`、`/api/auth/mp-*`）虽不与 jasic-ui 对齐业务语义，但其响应体、错误码、错误文案的处理**仍必须**走新 `utils/http.ts`（成功码 `'00000'`、A0100 / A0200 同口径、防重复弹框同口径）。

---

## 回归验证清单（阶段 6.4）

任何改动契约层（`utils/http.ts`、`api/*`、`stores/modules/user.ts`、`utils/permissions.ts`）或登录 / 工单 / 地址 / 文件上传主链路的 PR，必须在合并前按本清单过一遍；建议用 checkbox 形式贴到 PR 描述中。

### 登录链路（双端）

- [ ] 正常登录：输入账号密码 → 收到 `{ code: '00000', data: { token, userInfo, needChooseCompany, companies } }` → 落地 token → 跳首页。
- [ ] `needChooseCompany: true` 分支：contractor 调用 `POST /api/auth/choose-company`，aftersale 不触发此分支但不报错。
- [ ] onLaunch 刷新：应用启动时若存在 token，调用 `GET /api/auth/user-info` 刷新 `perms` / 用户信息。
- [ ] A0100 强登：手动使 token 失效 → 下次请求触发 modal「登录已过期，请重新登录」→ 点击确认后 `reLaunch('/pages/login/index')`；短时间连续请求仅弹一次（防重复弹框生效）。
- [ ] A0200 无权限：调用需要权限的接口 → toast「没有操作权限」；业务按钮按 `hasPermission` 隐藏或禁用。
- [ ] 登出：`POST /api/auth/logout` 成功 → 清 token / 用户信息 → `reLaunch('/pages/login/index')`。
- [ ] 成功码宽容判定：构造响应 `{ code: '0' }` / `{ code: '200' }` 应被判为失败（不再宽容通过）。

### 工单链路（contractor 为主，aftersale 覆盖 C 端可见部分）

- [ ] 列表 `listWorkOrder`：分页、筛选、下拉刷新、上拉加载更多。
- [ ] 状态统计 `countWorkOrderStatus`：各 tab 数字与列表过滤一致。
- [ ] 详情 `getWorkOrder`：`mainStatus` 字面为 `PENDING_ASSIGN / PENDING_TECH_ACCEPT / IN_PROGRESS / COMPLETED / CLOSED` 之一，UI 按大写枚举渲染。
- [ ] 派单 `assignWorkOrder`：派单成功后列表 / 详情 `mainStatus` 从 `PENDING_ASSIGN` 切换为 `PENDING_TECH_ACCEPT`。
- [ ] 接单 `techAcceptWorkOrder`：`PENDING_TECH_ACCEPT` → `IN_PROGRESS`。
- [ ] 转单 `transferWorkOrder`：`IN_PROGRESS` 保持，指派人改变。
- [ ] 维修登记 `repairWorkOrder`：多段故障点上报 + 配件使用 + 语音 + 图片上传。
- [ ] 复检 `reviewWorkOrder`：`IN_PROGRESS` → `COMPLETED`。
- [ ] 关单 `closeWorkOrder`：`COMPLETED` → `CLOSED`。
- [ ] aftersale 侧 C 端：下单、取消、评价 `pages/order/evaluate.vue` 链路正常。

### 地址链路（双端对称）

- [ ] 列表：aftersale `listCustomerAddress` / contractor `listCompanyAddress` 正常分页与下拉刷新。
- [ ] 新建：`addCustomerAddress` / `addCompanyAddress`，`fullAddress` 字段正确落库。
- [ ] 编辑：`updateCustomerAddress` / `updateCompanyAddress`，表单回填字段完整。
- [ ] 删除：`deleteCustomerAddress` / `deleteCompanyAddress`，列表即时刷新。
- [ ] 设为默认：`setDefaultCustomerAddress` / `setDefaultCompanyAddress`，同一租户下唯一默认项。
- [ ] 跨页回传选中：地址选择页 → 业务表单页，`SavedAddress.fullAddress` 与 `addressStorage` 存取一致。

### 文件上传链路（双端对称）

- [ ] 单图上传：`uploadSystemFile` 返回 `{ fileId, url }`，预览图即时出现。
- [ ] 多图 / 视频上传：`MediaUploadField` 并发 / 顺序上传，进度与失败重试表现一致。
- [ ] 绑定：`bindBizFile` 把业务单号 + fileId 成功绑定；`unbindBizFile` 可解绑。
- [ ] 预览：`mediaPreview.ts` 的 `resolvePreviewableUrl` 正确归一 `tmp/blob/file/wxfile/绝对域名/相对路径`；`previewVideo` 端侧行为正常。
- [ ] 错误码：上传返回 `code !== '00000'` 时走统一 toast，且不进入后续绑定流程。

### 静态守卫

- [ ] `pnpm -C mp/aftersale type-check` 与 `pnpm -C mp/contractor type-check` 均通过（CI 见 `.github/workflows/mp-type-check.yml`）。
- [ ] `pnpm -C mp/aftersale lint` 与 `pnpm -C mp/contractor lint` 均通过；`src/api/**` 与 `src/utils/http.ts` 下不得出现 `.result` 成员访问（ESLint `no-restricted-syntax` 规则生效）。

