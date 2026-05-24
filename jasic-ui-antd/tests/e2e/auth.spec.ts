import process from 'node:process';
import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

/**
 * 登录页常用文案常量。
 * 这里直接复用当前页面中文文案做黑盒选择器，避免依赖内部组件结构或 class 命名。
 */
const LOGIN_PAGE_TEXT = {
  userNamePlaceholder: '请输入用户名或手机号',
  passwordPlaceholder: '请输入密码',
  confirm: /确\s*认/
} as const;

/**
 * 作用：访问密码登录页并等待核心输入框可见。
 * 这样可以确保后续断言基于用户真正可操作的页面状态，而不是路由切换中的中间态。
 */
async function openPasswordLoginPage(page: Page) {
  await page.goto('/login/pwd-login');
  await expect(page.getByPlaceholder(LOGIN_PAGE_TEXT.userNamePlaceholder)).toBeVisible();
  await expect(page.getByPlaceholder(LOGIN_PAGE_TEXT.passwordPlaceholder)).toBeVisible();
}

test.describe('Web 黑盒冒烟 - 登录入口', () => {
  test('未登录访问受保护页面时应跳转到登录页', async ({ page }) => {
    // 通过直接访问业务路由验证路由守卫，而不是依赖内部 store 或 token 注入。
    await page.goto('/work-order');

    await expect(page).toHaveURL(/\/login(?:\/pwd-login)?(?:\?.*redirect=.*work-order.*)?$/);
    await expect(page.getByPlaceholder(LOGIN_PAGE_TEXT.userNamePlaceholder)).toBeVisible();
  });

  test('密码登录页应展示用户名和密码输入框', async ({ page }) => {
    await openPasswordLoginPage(page);

    await expect(page.getByRole('button', { name: LOGIN_PAGE_TEXT.confirm })).toBeVisible();
  });

  test('配置测试账号后可执行真实账号密码登录', async ({ page }) => {
    const username = process.env.E2E_USERNAME?.trim();
    const password = process.env.E2E_PASSWORD ?? '';

    test.skip(!username || !password, '未配置 E2E_USERNAME / E2E_PASSWORD，跳过真实登录黑盒用例。');

    await openPasswordLoginPage(page);

    await page.getByPlaceholder(LOGIN_PAGE_TEXT.userNamePlaceholder).fill(username!);
    await page.getByPlaceholder(LOGIN_PAGE_TEXT.passwordPlaceholder).fill(password);

    // 登录成功后通常会进入首页或选公司页；黑盒场景只要求离开登录页。
    await page.getByRole('button', { name: LOGIN_PAGE_TEXT.confirm }).click();
    await expect(page).not.toHaveURL(/\/login/);
  });
});
