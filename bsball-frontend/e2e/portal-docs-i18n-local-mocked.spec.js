// 门户公告文档错误态/不存在态多语言冒烟（B2b）
// 运行：npx playwright test e2e/portal-docs-i18n-local-mocked.spec.js
import { test, expect } from '@playwright/test';

const TENANT = 'bs-ball';

async function mock(page, locale, mode) {
  await page.addInitScript((l) => localStorage.setItem('bs-ball-locale', l), locale);
  await page.route('**/bsball-server/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/bsball-server', '');
    if (path === '/sys/notice/999') {
      const body = mode === 'empty'
        ? { code: 200, data: null }
        : { code: 500, message: 'mock notice error' };
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
      return;
    }
    const data = path === '/portal/settings' ? {} : [];
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data }) });
  });
}

test('公告不存在态：简中/英文正常渲染且不白屏', async ({ page }) => {
  await mock(page, 'zh-CN', 'empty');
  await page.goto(`/bs-ball/${TENANT}/docs/999`);
  await expect(page.getByText('公告不存在或已删除', { exact: true })).toBeVisible();

  await page.addInitScript(() => localStorage.setItem('bs-ball-locale', 'en'));
  await page.reload();
  await expect(page.getByText('Announcement not found or deleted', { exact: true })).toBeVisible();
});

test('公告加载失败态：重试按钮可见且可点击', async ({ page }) => {
  await mock(page, 'zh-CN', 'error');
  await page.goto(`/bs-ball/${TENANT}/docs/999`);
  const retry = page.getByRole('button', { name: '重试' });
  await expect(retry).toBeVisible();
  await retry.click();
  await expect(page.getByText('服务器繁忙，请稍后重试')).toBeVisible();
});
