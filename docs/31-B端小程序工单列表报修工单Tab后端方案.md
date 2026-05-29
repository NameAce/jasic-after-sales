# B端小程序工单列表新增“报修工单”Tab后端方案

## 1. 背景

B端小程序工单列表需要新增一个“报修工单”Tab，用于展示“当前网点报修出去的工单”。

本次只讨论后端处理方案，不涉及前端、小程序页面实现、交互和状态统计扩展方案。

## 2. 本次目标

为工单列表接口新增一个新的查询视图：

- `viewScope = COMPANY_REPAIR`

用于查询“当前登录公司作为报修发起方”的工单列表。

## 3. 业务定义

### 3.1 视图名称

新增 `viewScope` 枚举值：

- `COMPANY_REPAIR`

### 3.2 视图语义

`COMPANY_REPAIR` 表示：

- 当前登录公司是该工单的“报修发起公司”
- 该视图展示的是“当前公司报修出去的工单”
- 不表示“当前公司正在承接的工单”
- 不表示“当前用户所属公司的全部工单”
- 不表示“当前用户个人创建的工单”

## 4. 查询范围

### 4.1 包含范围

仅包含以下建单入口类型：

- `UPSTREAM_FIRST`
- `UPSTREAM_HQ`

对应业务理解：

- 报修一级
- 报修佳士

### 4.2 不包含范围

以下类型不进入 `COMPANY_REPAIR`：

- `PROXY_SELF`
- `CUSTOMER_REPORT`

### 4.3 固定筛选条件

`viewScope = COMPANY_REPAIR` 时，后端固定附加以下条件：

- `w.report_subject_type = 'COMPANY'`
- `w.report_company_id = 当前登录公司ID`
- `w.create_entry_type IN ('UPSTREAM_FIRST', 'UPSTREAM_HQ')`

## 5. 数据权限规则

### 5.1 总体原则

`COMPANY_REPAIR` 只是一个新的列表视图，不新增独立权限体系。

仍然沿用现有：

- 数据权限逻辑
- 操作权限逻辑
- 按钮权限逻辑
- 详情权限校验逻辑

### 5.2 SELF 数据范围

当当前用户数据范围为 `SELF` 时，仍然按现有“本人参与事实”口径收紧。

也就是：

- 维修员如果数据范围是 `SELF`
- 在 `COMPANY_REPAIR` 视图下也只能看到“自己有参与事实”的工单
- 不能因为该工单属于“当前公司报修出去的工单”就放大为可看全公司

本次不为 `COMPANY_REPAIR` 单独创造新的 `SELF` 规则，直接复用现有“本人参与事实”判断。

### 5.3 总部主体

总部账号不支持 `COMPANY_REPAIR` 视图。

建议处理方式：

- 总部主体传入 `viewScope=COMPANY_REPAIR` 时，直接返回空列表
- 不扩展总部视角下的“报修工单”语义

## 6. 操作/按钮权限

### 6.1 原则

`COMPANY_REPAIR` 不新增任何专属动作规则。

仍然沿用当前：

- `availableActions`
- 行内操作权限判断
- 详情页操作权限判断
- 后端实例级权限兜底校验

### 6.2 说明

该视图只是“列表可见性入口”变化，不改变工单实例本身的可操作性判断。

也就是说：

- 能否派单，仍按原有派单权限和实例条件判断
- 能否转单，仍按原有转单权限和实例条件判断
- 能否上传寄件单号，仍按原有例外动作规则判断
- 后端权限兜底逻辑不变

## 7. 状态统计

本次**暂不支持** `COMPANY_REPAIR` 的状态统计。

即：

- 列表接口支持 `viewScope=COMPANY_REPAIR`
- `status-count` 暂不支持该视图

建议后端处理方式：

- 当 `status-count` 接口收到 `viewScope=COMPANY_REPAIR` 时
- 明确返回“不支持该视图统计”之类的业务提示
- 不返回错误口径的统计数据
- 不临时复用 `CURRENT / HISTORY / ALL` 的统计逻辑

后续如需支持，再单独规划。

## 8. 旧数据处理

本次**不处理旧数据回填**。

说明：

- 本次实现默认基于现有字段口径生效
- 不补历史数据
- 如果历史上存在 `report_company_id`、`report_subject_type` 缺失或不规范的数据，暂不在本次范围内处理

## 9. 后端改造建议

### 9.1 查询对象

文件建议关注：

- `jasic-system/src/main/java/com/jasic/aftersales/system/domain/query/WorkOrderQuery.java`

改造建议：

- 在 `viewScope` 注释和允许值说明中补充 `COMPANY_REPAIR`

### 9.2 Service 层

文件建议关注：

- `jasic-system/src/main/java/com/jasic/aftersales/system/service/impl/WorkOrderServiceImpl.java`

改造建议：

- 在列表查询口径中识别 `viewScope=COMPANY_REPAIR`
- 保持现有权限上下文补齐逻辑
- 总部主体命中该视图时直接返回空列表
- `status-count` 收到 `COMPANY_REPAIR` 时返回“不支持该视图统计”

注意：

- 不要把 `COMPANY_REPAIR` 混进 `CURRENT`
- 不要把 `COMPANY_REPAIR` 混进 `HISTORY`
- 不要改动既有 `CURRENT / HISTORY / ALL` 语义

### 9.3 Mapper / SQL

文件建议关注：

- `jasic-admin/src/main/resources/mapper/system/WorkOrderMapper.xml`

改造建议：

在工单列表 SQL 的 `viewScope` 分支中新增：

- `COMPANY_REPAIR`

对应条件：

- `w.report_subject_type = 'COMPANY'`
- `w.report_company_id = #{query.accessContext.currentCompanyId}`
- `w.create_entry_type IN ('UPSTREAM_FIRST', 'UPSTREAM_HQ')`

同时：

- `SELF` 数据范围仍然沿用现有用户级收口条件
- 不放宽为“同公司所有人可见”

## 10. 接口行为约定

### 10.1 列表接口

接口：

- `GET /system/work-order/list`

新增支持：

- `viewScope=COMPANY_REPAIR`

示例：

```http
GET /system/work-order/list?viewScope=COMPANY_REPAIR&pageNum=1&pageSize=10
```

### 10.2 状态统计接口

接口：

- `GET /system/work-order/status-count`

本次约定：

- 不支持 `viewScope=COMPANY_REPAIR`

建议行为：

- 返回明确业务提示，例如“当前视图暂不支持状态统计”

## 11. 验证重点

建议至少覆盖以下场景：

### 11.1 列表可见性

- 二级网点创建 `UPSTREAM_FIRST` 工单后，可在 `COMPANY_REPAIR` 查到
- 一级网点创建 `UPSTREAM_HQ` 工单后，可在 `COMPANY_REPAIR` 查到
- `PROXY_SELF` 工单不可进入 `COMPANY_REPAIR`
- `CUSTOMER_REPORT` 工单不可进入 `COMPANY_REPAIR`

### 11.2 公司边界

- A 网点只能查到 A 网点报修出去的工单
- 不能看到 B 网点报修出去的工单

### 11.3 SELF 收口

- `SELF` 用户只能看到本人有参与事实的 `COMPANY_REPAIR` 工单
- 同公司但未参与的工单不可见

### 11.4 总部限制

- 总部主体查询 `COMPANY_REPAIR` 返回空列表

### 11.5 权限一致性

- 列表能看到的工单，详情权限口径应与现有逻辑一致
- 行内 `availableActions` 仍按既有规则输出
- 不因新视图而产生额外越权按钮

## 12. 本次明确不做

本次不包含以下内容：

- 前端 Tab 实现
- 小程序页面改造
- `status-count` 对 `COMPANY_REPAIR` 的统计支持
- 旧数据回填
- 新增专属权限点
- 新增专属按钮规则
- 调整既有 `CURRENT / HISTORY / ALL` 语义

## 13. 最终确认口径

本次方案最终确认如下：

- 新增 `viewScope = COMPANY_REPAIR`
- 仅包含 `UPSTREAM_FIRST`、`UPSTREAM_HQ`
- 固定按 `report_subject_type='COMPANY' + report_company_id=当前登录公司` 过滤
- `SELF` 数据范围继续生效，且沿用现有“本人参与事实”口径
- 总部不支持该视图
- 操作/按钮权限完全沿用现有逻辑
- 状态统计本次暂不支持
- 旧数据本次不处理
