# API 返回码、业务枚举与字段取值说明

## 1. 返回码说明

### 1.1 通用返回结构

所有接口统一返回 `Result<T>`：

| 字段 | 说明 |
| --- | --- |
| `code` | 业务状态码，`00000` 表示成功 |
| `msg` | 提示信息，失败时可直接用于前端展示 |
| `data` | 业务数据，分页接口通常为 `PageResult<T>` |

### 1.2 标准返回码

| 返回码 | 含义 | 说明 |
| --- | --- | --- |
| `00000` | 操作成功 | 正常成功返回 |
| `A0001` | 用户端业务错误 | 业务校验失败默认使用该码，具体原因看 `msg` |
| `A0100` | 未登录 | 登录失效、未携带有效登录态 |
| `A0110` | 登录错误 | 用户名或密码错误、当前密码错误 |
| `A0120` | 账号已停用 | 用户或客户账号不可用 |
| `A0200` | 无权限 | 无菜单权限、无公司权限或无数据权限 |
| `A0400` | 参数校验失败 | `@RequestBody` / 查询参数校验失败 |
| `A0410` | 数据不存在 | 用户、公司、工单等记录不存在 |
| `A0420` | 数据已存在 | 编码、键名、关系等重复 |
| `B0001` | 系统内部错误 | 未捕获异常或系统异常 |
| `C0001` | 第三方服务错误 | 第三方接口或外部服务异常 |

### 1.3 异常映射规则

| 异常来源 | 返回码 | 说明 |
| --- | --- | --- |
| `NotLoginException` | `A0100` | 统一返回“未登录或登录已过期，请重新登录” |
| `NotPermissionException` | `A0200` | 统一返回“没有操作权限” |
| `MethodArgumentNotValidException` | `A0400` | 取首个字段校验错误 |
| `BindException` | `A0400` | 取首个参数绑定错误 |
| `HttpRequestMethodNotSupportedException` | `A0001` | 返回“不支持 xxx 请求” |
| `ServiceException(code, msg)` | 指定 `code` | 少量场景会显式指定错误码 |
| `ServiceException(msg)` | `A0001` | 大部分业务失败都走这个分支 |
| 其他未捕获异常 | `B0001` | 返回“系统内部错误，请联系管理员” |

## 2. 业务枚举说明

### 2.1 工单相关

#### `BrandTypeEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `JASIC` | 佳士品牌 | 佳士品牌设备报修 |
| `NON_JASIC` | 非佳士品牌 | 非佳士设备报修 |

#### 工单主状态 `mainStatus`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `PENDING_ASSIGN` | 待派单 | 已建单，等待管理人员派单 |
| `PENDING_TECH_ACCEPT` | 待接单 | 已派单，等待维修员接单 |
| `IN_PROGRESS` | 维修中 | 维修处理中 |
| `COMPLETED` | 已完成 | 维修完成，待复检/关闭 |
| `CLOSED` | 已关闭 | 工单已关闭 |

#### 工单展示状态 `displayStatus`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `WAIT_ACCEPT` | 待接单 | 对外聚合 `PENDING_ASSIGN` 与 `PENDING_TECH_ACCEPT` |
| `IN_PROGRESS` | 维修中 | 同主状态 |
| `COMPLETED` | 已完成 | 同主状态 |
| `CLOSED` | 已关闭 | 同主状态 |

#### 工单评价状态 `evaluateStatus`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `NOT_OPEN` | 未开启评价 | 当前还不能评价 |
| `PENDING_EVALUATE` | 待评价 | 工单关闭后待客户评价 |
| `EVALUATED` | 已评价 | 已完成评价 |

#### 工单建单入口 `createEntryType`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `PROXY_SELF` | 代客户填写 | 当前公司代客户建单并自行受理 |
| `UPSTREAM_FIRST` | 二级报修一级 | 二级网点报修，由上游一级受理 |
| `UPSTREAM_HQ` | 一级报修佳士 | 一级网点报修，由上游总部受理 |
| `CUSTOMER_REPORT` | 客户报修 | 客户在 C 端提交报修，由目标服务网点受理 |

### 2.2 主体与权限

#### `SubjectTypeEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `PLATFORM` | 平台 | 平台侧主体 |
| `HQ` | 总部 | 总部公司主体 |
| `SERVICE` | 服务网点 | 一级/二级服务网点主体 |

#### `DataScopeEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `ALL` | 全部数据 | 平台侧表示全部；一级网点可表示本公司及下级网点 |
| `COMPANY` | 当前公司数据 | 服务网点常用，表示仅本公司 |
| `REGION` | 大区数据 | 总部按大区查看数据 |
| `SELF` | 仅本人数据 | 仅查看本人相关数据 |

#### `CompanyCategoryEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `HQ` | 总部 | 查询总部公司 |
| `FIRST_LEVEL` | 一级网点 | 查询一级服务网点 |
| `SECOND_LEVEL` | 二级网点 | 查询二级服务网点 |

### 2.3 文件相关

#### `SysFileBizTypeEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `WORK_ORDER_FAULT_IMAGE` | 工单故障图片 | 工单故障图片文件 |
| `WORK_ORDER_FAULT_VIDEO` | 工单故障视频 | 工单故障视频文件 |
| `WORK_ORDER_FAULT_VOICE` | 工单故障语音 | 工单故障语音文件 |
| `WORK_ORDER_SENDER_VOUCHER` | 工单寄件凭证 | 客户寄件凭证 |
| `WORK_ORDER_RETURN_VOUCHER` | 工单回寄凭证 | 关单回寄凭证 |

#### `SysFileUploadUserTypeEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `SYSTEM` | 系统用户 | B 端用户上传 |
| `CUSTOMER` | 客户用户 | C 端客户上传 |

#### `SysFileStorageTypeEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `OSS` | 阿里云 OSS | 当前仅支持 OSS 存储 |

#### `SysFileAccessLevelEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `PRIVATE` | 私有文件 | 默认私有访问级别 |

#### `SysFileStatusEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `ACTIVE` | 有效 | 当前有效文件状态 |

#### `SysFileMediaTypeEnum`

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `IMAGE` | 图片 | 图片文件 |
| `VIDEO` | 视频 | 视频文件 |
| `VOICE` | 语音 | 语音文件 |

## 3. 常见字段取值说明

### 3.1 工单常用字段

| 字段 | 可选值 | 说明 |
| --- | --- | --- |
| `brandType` | `JASIC`、`NON_JASIC` | 品牌类型 |
| `serviceMode` | `MAIL`、`STORE` | 建单服务方式编码；分别表示寄修、到店维修 |
| `serviceModeLabel` | `寄修`、`到店维修` | 服务方式展示名称 |
| `warrantyStatus` | `IN_WARRANTY`、`OUT_OF_WARRANTY` | 质保状态；分别表示保内、保外 |
| `returnMethod` | `回寄`、`自提` | 关单时机器返还方式 |
| `viewScope` | `CURRENT`、`HISTORY`、`ALL` | 当前参与、历史参与、全部可见 |
| `currentAcceptSubjectType` | `SERVICE`、`HQ` | 当前受理主体类型 |
| `subjectType` | `PLATFORM`、`HQ`、`SERVICE` | 当前登录主体/菜单归属主体 |
| `dataScope` | `ALL`、`COMPANY`、`REGION`、`SELF` | 当前有效数据范围 |
| `category` | `HQ`、`FIRST_LEVEL`、`SECOND_LEVEL` | 公司业务分类 |

### 3.2 工单动作字段

`WorkOrderDetailVO.availableActions` 涉及下列业务编码：

#### `availableActions`

| 编码 | 说明 |
| --- | --- |
| `ASSIGN` | 派单 |
| `UPLOAD_SEND_EXPRESS` | 上传寄件快递单号 |
| `TRANSFER` | 转单 |
| `REVIEW` | 复检 |
| `RETURN_METHOD` | 选择返还方式 |
| `CLOSE` | 关闭工单 |
| `TECH_ACCEPT` | 维修员接单 |
| `QUOTE` | 报价 |
| `REPAIR_FINISH` | 维修登记 |

### 3.3 质保状态补充说明

`warrantyStatus` 当前主要由 `MachineBarcodeWarrantyResolver` 统一计算：

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `IN_WARRANTY` | 保内 | 条码在质保期内 |
| `OUT_OF_WARRANTY` | 保外 | 条码超出质保期，或无码场景默认保外 |

## 4. 维护约定

- 新增返回码时，先补充 `ResultCode`，再同步更新本文档。
- 新增枚举或受限字段值时，优先补代码注解，再同步更新本文档。
- 同一字段如果既有“编码字段”又有“名称字段”，接口侧优先返回编码和名称两份，避免前端再做映射。
