import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright 黑盒测试配置。
 * 设计约束：
 * 1. 默认复用当前本地已启动的前端开发服务，避免在测试命令里额外拉起长期驻留进程。
 * 2. 允许通过环境变量覆盖基础地址，便于后续切换到测试环境或预发布环境。
 * 3. 保留失败截图、录像和 trace，方便定位前端交互与联调问题。
 */
const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:9527';

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30 * 1000,
  expect: {
    timeout: 5 * 1000
  },
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    headless: true,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome']
      }
    }
  ]
});
