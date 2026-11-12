// 批量添加替补：行前 checkbox + 表头全选/半选（本地 mock，无后端）
// 运行：E2E_PORT=3101 npx playwright test e2e/lineup-bench-batch-checkbox-local-mocked.spec.js
import { test, expect } from '@playwright/test';

test.use({ viewport: { width: 1400, height: 950 } });

const awayPlayers = Array.from({ length: 12 }, (_, i) => ({
  id: 201 + i,
  name: `客队球员${i + 1}`,
  number: String(i + 1),
  teamId: 2
}));
const homePlayers = [
  { id: 101, name: '主队球员1', number: '1', teamId: 1 },
  { id: 102, name: '主队球员2', number: '2', teamId: 1 }
];

test('批量添加替补：表头全选/取消，行 checkbox 保留单独勾选', async ({ page }) => {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event', gameMode: 'BASEBALL', specialRules: {} };
    else if (path === '/event/1/game-team-options') data = [{ id: 1, name: 'Home' }, { id: 2, name: 'Away' }];
    else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/event/1/teams/1/roster-players') data = { restricted: false, players: homePlayers };
    else if (path === '/event/1/teams/2/roster-players') data = { restricted: false, players: awayPlayers };

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.goto('/bs-ball/bs-ball/admin/events/1/games/live?homeTeamId=1&awayTeamId=2');
  await expect(page.locator('.lineup-card--pool')).toHaveCount(12);

  const openBatch = async () => {
    await page.getByRole('button', { name: '批量添加' }).click();
    const dialog = page.locator('.el-dialog').filter({ hasText: '批量添加替补' });
    await expect(dialog).toBeVisible();
    return dialog;
  };

  let dialog = await openBatch();
  const rows = dialog.locator('.lineup-batch__row');
  const head = dialog.locator('.lineup-batch__head .el-checkbox');
  const headInput = head.locator('input[type="checkbox"]');
  const count = dialog.locator('.lineup-batch__count');

  await expect(rows).toHaveCount(12);
  await expect(count).toHaveText('已选 0 / 12');
  await expect(headInput).not.toBeChecked();

  // 表头一键全选
  await head.click();
  await expect(dialog.locator('.lineup-batch__row.is-checked')).toHaveCount(12);
  await expect(count).toHaveText('已选 12 / 12');
  await expect(headInput).toBeChecked();
  await dialog.getByRole('button', { name: '确定' }).click();
  await expect(dialog).toBeHidden();
  await expect(page.locator('.lineup-card--bench')).toHaveCount(12);
  await expect(page.locator('.lineup-card--pool')).toHaveCount(0);

  // 再次打开：默认勾选当前替补；取消一人 → 表头半选
  dialog = await openBatch();
  await expect(dialog.locator('.lineup-batch__row.is-checked')).toHaveCount(12);
  await rows.first().click();
  await expect(count).toHaveText('已选 11 / 12');
  expect(await headInput.evaluate((el) => el.indeterminate)).toBe(true);
  await dialog.getByRole('button', { name: '确定' }).click();
  await expect(page.locator('.lineup-card--bench')).toHaveCount(11);

  // 半选态点表头 → 全选；再点一次 → 全部取消
  dialog = await openBatch();
  expect(await headInput.evaluate((el) => el.indeterminate)).toBe(true);
  await head.click();
  await expect(count).toHaveText('已选 12 / 12');
  await expect(headInput).toBeChecked();
  await head.click();
  await expect(count).toHaveText('已选 0 / 12');
  await expect(headInput).not.toBeChecked();
  await dialog.getByRole('button', { name: '确定' }).click();
  await expect(page.locator('.lineup-card--bench')).toHaveCount(0);

  // 单独勾选一名未选球员 → 确定后新增到替补席
  dialog = await openBatch();
  await expect(count).toHaveText('已选 0 / 12');
  await rows.first().click();
  await expect(count).toHaveText('已选 1 / 12');
  expect(await headInput.evaluate((el) => el.indeterminate)).toBe(true);
  await dialog.getByRole('button', { name: '确定' }).click();
  await expect(page.locator('.lineup-card--bench')).toHaveCount(1);
});
