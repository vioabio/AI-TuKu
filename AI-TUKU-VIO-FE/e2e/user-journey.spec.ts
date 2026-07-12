import { test, expect } from '@playwright/test';

const TEST_ACCOUNT = `e2e_${Date.now()}`;
const TEST_PASSWORD = 'Test123456';

test.describe('AI-TuKu 用户核心旅程', () => {

  test('注册 → 登录 → 首页加载', async ({ page }) => {
    // 1. 访问注册页面
    await page.goto('/user/register');
    await expect(page.locator('h2, h1, .title')).toBeVisible();

    // 2. 填写注册表单
    await page.fill('input[placeholder*="账号"], #userAccount', TEST_ACCOUNT);
    await page.fill('input[placeholder*="密码"], #userPassword', TEST_PASSWORD);
    await page.fill('input[placeholder*="确认"], #checkPassword', TEST_PASSWORD);

    // 3. 提交注册
    await page.click('button:has-text("注册")');

    // 4. 应跳转到登录页
    await page.waitForURL('**/user/login', { timeout: 10000 });
    expect(page.url()).toContain('/user/login');
  });

  test('登录 → 成功进入首页', async ({ page }) => {
    await page.goto('/user/login');
    await expect(page.locator('input[placeholder*="账号"]')).toBeVisible();

    // 使用已知测试账号登录
    await page.fill('input[placeholder*="账号"], #userAccount', TEST_ACCOUNT);
    await page.fill('input[placeholder*="密码"], #userPassword', TEST_PASSWORD);
    await page.click('button:has-text("登录")');

    // 应跳转到首页
    await page.waitForURL('**/', { timeout: 15000 });
  });

  test('未登录访问受保护页面 → 应重定向', async ({ page }) => {
    await page.goto('/admin/userManage');

    // 应被重定向到登录页
    await page.waitForURL('**/user/login', { timeout: 10000 });
    expect(page.url()).toContain('/user/login');
  });

  test('首页应正常加载', async ({ page }) => {
    await page.goto('/');

    // 页面应有内容
    await expect(page.locator('body')).toBeVisible();
    // 不应显示错误信息
    await expect(page.locator('.ant-result-error')).toHaveCount(0);
  });
});
