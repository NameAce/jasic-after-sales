# Dynamic Route Regression & Rollback Checklist

## Contract Baseline

- Dynamic menu source uses `GET /auth/menus`.
- Backend `SysMenuVO` field mapping:
  - `menuName -> meta.title`
  - `orderNum -> meta.order`
  - `isVisible -> meta.hideInMenu` (`0 => true`, `1 => false`)
  - `icon -> meta.icon`
  - `path -> path`
  - `component -> component`
  - `children -> children`
- Compatible response shape: either plain route tree array or `{ home, routes }`.
- Menu title priority: backend title first, i18n translation only when `VITE_BACKEND_MENU_USE_I18N=Y`.

## Regression Scope

- [x] Menu labels no longer display raw `route.xxx` key when backend provides title.
- [x] Menu order follows backend `meta.order` (or mapped `orderNum`).
- [x] Backend-removed route is not visible in sidebar.
- [x] Hidden route can be navigated but not shown in sidebar.
- [x] Breadcrumb and active menu keep expected behavior for `activeMenu`.
- [x] Refresh page keeps dynamic route access and menu state.
- [x] Static business menu is not injected in dynamic mode.

## Rollback Path

1. Change `.env.test` `VITE_AUTH_ROUTE_MODE=dynamic` back to `static`.
2. Keep `VITE_BACKEND_MENU_USE_I18N` as needed (`N` or `Y`).
3. Rebuild/restart and verify static route home and menu are restored.
4. Record missing backend fields (especially `meta.title`, `meta.hideInMenu`) before re-enabling dynamic mode.

## System Menu Regression (2026-04-27)

`system` 相关动态菜单已统一别名到 `advanced-modules`，并通过路由名自动定位到对应 Tab。以下为代码回归基线（首个业务请求以 `loadByModule` 对应接口为准）：

| Backend route name | 默认模块 Tab | 首个业务请求 |
|---|---|---|
| `system_role-template` | `roleTemplate` | `GET /system/role-template/list` |
| `system_config` | `config` | `GET /system/config/list` |
| `system_dict-type` | `dict` | `GET /system/dict/type/list` |
| `system_dict-data` | `dict` | `GET /system/dict/type/list` |
| `system_notify-template` | `notifyTemplate` | `GET /system/notify/template/list` |
| `system_machine-barcode` | `barcode` | `GET /system/machine-barcode/list` |
| `system_sync-task` | `syncTask` | `GET /system/sync-task/list` |
| `system_fault-repair-config` | `fault` | `GET /system/fault-repair-config/list` |
| `system_region` | `region` | `GET /system/region/list`（前置会先请求 `GET /org/company/list` 获取总部选项） |

### 差异记录

- `system_dict-data` 当前按计划并入 `dict` Tab，首请求仍为 `dict type` 列表，而非独立 `dict data` 列表。
- `region` 模块为保证总部上下文，会先拉取总部公司选项，再请求大区列表。
