# 侧边栏目标结构与顺序基线（排除首页）

## 基线来源

- 参考 `jasic-ui` 的权限路由生成链路（`jasic-ui/src/store/modules/permission.js`）与当前后端菜单域模型。
- 本基线仅约束“结构与顺序”，不约束图标与文案。
- 首页 `home` 不纳入对齐判断。

## 一级菜单目标顺序（不含首页）

1. `system`
2. `org`
3. `log`
4. `work-order`
5. `notify`
6. `company-address`
7. `advanced-modules`

## 二级菜单目标顺序

### `system`

1. `system_user`
2. `system_role`
3. `system_menu`

### `org`

- 当前版本无二级菜单（单页面）

### `log`

1. `log_oper-log`

### `work-order`

- 当前版本无二级菜单（单页面）

### `notify`

- 当前版本无二级菜单（单页面）

### `company-address`

- 当前版本无二级菜单（单页面）

### `advanced-modules`

- 当前版本无二级菜单（单页面）

## route key / path 映射（摘录）

- `system` -> `/system`
- `org` -> `/org`
- `log` -> `/log`
- `work-order` -> `/work-order`
- `notify` -> `/notify`
- `company-address` -> `/company-address`
- `advanced-modules` -> `/advanced-modules`
