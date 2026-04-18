# 列表视觉对齐（aftersale ↔ contractor）

## 协作约定（维护顺序）

1. **先改 contractor（真源）**：列表壳层、搜索条、`list-container` 边距、`.list-empty`、`.order-card`（白底 + `2rpx solid $bg-hover`、无重阴影）等以 `mp/contractor/src/styles/order-pages.scss` 与对应页面为准。
2. **再同步 aftersale**：在 `mp/aftersale` 内用本端 `variables.scss` 扩展段与页面/组件 scoped 样式对齐同等数值与语义，**不**抽公共包、不跨目录 `@use`。

令牌扩展见：`mp/aftersale/src/styles/variables.scss` 文件末尾「列表/卡片对齐 contractor」注释段。

## 真机 / 模拟器验收（发版前）

在同一机型宽度下对比截图：

- 工单列表：页面灰底、搜索条高度与圆角、Tab 下划线、空状态插图区与标题/说明字级、卡片描边与列表上下边距。
- 地址列表：空状态字色、地址卡片描边与间距。
- 历史记录：记录卡片与列表区边距。

对照端：contractor 对应页；问题以先改 contractor 再同步 aftersale 的方式闭环。
