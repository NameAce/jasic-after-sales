# B端小程序工单列表“报修工单”Tab后端实施提示词

```text
项目：E:\桌面\jasic-after-sales
通用协作约定：docs/00-项目协作约定.md
本轮参考方案：docs/31-B端小程序工单列表报修工单Tab后端方案.md

本轮目标：
按既定方案完成 B 端小程序工单列表“报修工单”Tab 的后端支持，只做后端，不做前端、小程序页面和交互。

本轮范围：
1. 为工单列表新增 `viewScope=COMPANY_REPAIR`
2. 支持列表接口按“当前公司报修出去的工单”口径查询
3. 总部主体不支持该视图
4. `status-count` 暂不支持该视图，并返回明确业务提示
5. 沿用现有数据权限、操作权限、按钮权限与详情权限逻辑
6. 补齐必要注释、测试与验证说明

已确认业务口径：
1. `viewScope` 新增值：`COMPANY_REPAIR`
2. 仅包含建单入口：
   - `UPSTREAM_FIRST`
   - `UPSTREAM_HQ`
3. 固定筛选条件：
   - `w.report_subject_type = 'COMPANY'`
   - `w.report_company_id = 当前登录公司ID`
   - `w.create_entry_type IN ('UPSTREAM_FIRST', 'UPSTREAM_HQ')`
4. 不包含：
   - `PROXY_SELF`
   - `CUSTOMER_REPORT`
5. `SELF` 数据范围继续生效，按现有“本人参与事实”口径收紧，只能看自己的
6. 总部主体传入 `COMPANY_REPAIR` 时不扩语义，直接按空列表处理
7. 操作/按钮权限完全沿用现有逻辑，不新增专属动作规则
8. 旧数据不处理

实现约束：
1. 先阅读并理解以下代码后再改：
   - `jasic-system/src/main/java/com/jasic/aftersales/system/domain/query/WorkOrderQuery.java`
   - `jasic-system/src/main/java/com/jasic/aftersales/system/service/impl/WorkOrderServiceImpl.java`
   - `jasic-admin/src/main/resources/mapper/system/WorkOrderMapper.xml`
   - 与工单权限相关的 Service / AccessContext / Permission 代码
2. 不要改动既有 `CURRENT / HISTORY / ALL` 语义
3. 不要新建独立权限体系
4. 不要为了通过编译绕过权限规则
5. 注释统一使用简体中文，且新增/修改逻辑要补充完整注释

建议改造点：
1. `WorkOrderQuery`
   - 补充 `viewScope` 允许值说明，加入 `COMPANY_REPAIR`
2. `WorkOrderServiceImpl`
   - 在列表查询流程中识别 `viewScope=COMPANY_REPAIR`
   - 保持现有权限上下文补齐逻辑
   - 总部主体命中该视图时返回空列表
   - `status-count` 收到 `COMPANY_REPAIR` 时返回“当前视图暂不支持状态统计”之类的明确业务提示
3. `WorkOrderMapper.xml`
   - 在工单列表 SQL 的 `viewScope` 分支中新增 `COMPANY_REPAIR`
   - 条件严格使用已确认口径
   - `SELF` 数据范围仍沿用现有用户级收口条件，不放宽为同公司全员可见
4. 如有必要，补充单元测试或集成测试，覆盖新增视图口径

验证重点：
1. 二级网点创建 `UPSTREAM_FIRST` 工单后，可在 `COMPANY_REPAIR` 查到
2. 一级网点创建 `UPSTREAM_HQ` 工单后，可在 `COMPANY_REPAIR` 查到
3. `PROXY_SELF`、`CUSTOMER_REPORT` 工单不可进入 `COMPANY_REPAIR`
4. A 网点只能看到 A 网点报修出去的工单，不能看到 B 网点的
5. `SELF` 用户只能看到本人有参与事实的 `COMPANY_REPAIR` 工单
6. 总部主体查询 `COMPANY_REPAIR` 返回空列表
7. 列表能看到的工单，其详情权限口径与现有逻辑保持一致
8. `status-count` 对 `COMPANY_REPAIR` 返回明确不支持提示，而不是错误统计

交付要求：
1. 直接完成代码修改，不要只停留在分析
2. 完成后说明：
   - 改了哪些关键文件
   - 核心实现方式
   - 运行了哪些验证命令
   - 验证结果如何
   - 是否有未解决风险
3. 如果发现当前代码和既定方案冲突，先说明冲突点和影响范围，再处理
4. 不要扩展到前端、小程序页面或旧数据修复
```
