# Playwright 黑盒测试说明

## 目标

- 使用 Playwright 对 PC Web 端执行黑盒冒烟测试。
- 默认覆盖登录入口、未登录拦截、模块切换等无需固定账号即可执行的场景。
- 支持通过环境变量补充真实账号，扩展为联调级黑盒测试。

## 前置条件

- 前端开发服务已启动，默认地址为 `http://127.0.0.1:9527`。
- 后端联调服务如需参与真实登录，请确保相关接口可访问。

## 常用命令

```powershell
pnpm test:e2e
pnpm test:e2e:headed
pnpm test:e2e:ui
```

## 可选环境变量

- `PLAYWRIGHT_BASE_URL`：覆盖默认测试地址。
- `E2E_USERNAME`：真实登录测试账号。
- `E2E_PASSWORD`：真实登录测试密码。

## 示例

```powershell
$env:E2E_USERNAME="hq_admin"
$env:E2E_PASSWORD="your-password"
pnpm test:e2e
```
