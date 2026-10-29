// 门户统计「位置守备榜」本地无头冒烟（#478 P2）：
// 编译产物 PortalStats 挂载新 Tab → 组件请求 /stats/leaders/fielding-positions（minOuts=9）→ 渲染 GP/GS/INN
// 运行：npx playwright test e2e/portal-stats-fielding-positions-local-mocked.spec.js
import { test, expect } from '@playwright/test';

const TENANT = 'bs-ball';
const YEAR = new Date().getFullYear();

const events = [
  { id: 11, name: '春季联赛', season: '2026春', leagueId: 1, startDate: `${YEAR}-03-01`, endDate: `${YEAR}-05-30`, status: 'ongoing', gameMode: 'BASEBALL' }
];
const teams = [
  { id: 1, name: '甲队' },
  { id: 2, name: '乙队' }
];
const positionRows = [
  { playerId: 108, playerName: '王中外', teamId: 1, teamName: '甲队', position: 'CF', gp: 5, gs: 4, inn: 3.1, po: 6, a: 1, e: 0, tc: 7, tcPct: '100.0%' },
  { playerId: 109, playerName: '李捕手', teamId: 2, teamName: '乙队', position: 'C', gp: 4, gs: 4, inn: 3.0, po: 5, a: 0, e: 1, tc: 6, tcPct: '83.3%' }
];

async function mockApi(page, captured) {
  await page.addInitScript(() => localStorage.setItem('bs-ball-locale', 'zh-CN'));
  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    captured.push(`${request.method()} ${path}${url.search}`);
    let data = [];
    if (path === '/event/list') data = { list: events, total: events.length };
    else if (path === '/game/list') data = { list: [], total: 0 };
    else if (path === '/team/list') data = { list: teams, total: teams.length };
    else if (path === '/team/select-options') data = teams.map(({ id, name }) => ({ id, name }));
    else if (path === '/portal/settings') data = {};
    else if (path === '/stats/leaders/fielding-positions') data = { list: positionRows, total: positionRows.length, meta: { minOuts: 9 } };
    else if (path.startsWith('/stats/')) data = { list: [], total: 0, meta: {} };
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

test('位置守备榜：Tab 渲染、资格线请求与 GP/GS/INN/PO/A/E/TC% 展示', async ({ page }) => {
  const captured = [];
  await mockApi(page, captured);

  await page.goto(`/bs-ball/${TENANT}/stats`);
  await expect(page.getByRole('tab', { name: '位置守备榜' })).toBeVisible();

  await page.getByRole('tab', { name: '位置守备榜' }).click();
  await expect(page.getByRole('heading', { name: '按位置守备榜' })).toBeVisible();
  await expect(page.getByText('至少 9 个守备出局（3 局）方可上榜')).toBeVisible();
  await expect(page.getByText('王中外')).toBeVisible();
  await expect(page.getByText('李捕手')).toBeVisible();
  await expect(page.getByText('CF', { exact: true })).toBeVisible();
  await expect(page.getByText('3.1')).toBeVisible();
  await expect(page.getByText('100.0%')).toBeVisible();
  await expect(page.getByText('83.3%')).toBeVisible();

  const request = captured.find((entry) => entry.startsWith('GET /stats/leaders/fielding-positions'));
  expect(request, captured.join('\n')).toBeTruthy();
  expect(request).toContain('minOuts=9');

  // 主客场筛选在本 Tab 生效（此前筛选控件启用但请求未带 homeAway）
  await page.locator('.el-select').filter({ hasText: '按主客场筛选' }).click();
  await page.getByRole('option', { name: '客场', exact: true }).click();
  await expect
    .poll(() => captured.some((entry) => entry.startsWith('GET /stats/leaders/fielding-positions') && entry.includes('homeAway=away')))
    .toBe(true);
});

test('深链 ?tab=fieldingPositions 刷新后仍停留在位置守备榜', async ({ page }) => {
  const captured = [];
  await mockApi(page, captured);

  await page.goto(`/bs-ball/${TENANT}/stats?tab=fieldingPositions`);
  await expect(page.getByRole('heading', { name: '按位置守备榜' })).toBeVisible();
  await expect(page.getByText('王中外')).toBeVisible();
  await expect(page.getByRole('tab', { name: '位置守备榜' })).toHaveClass(/is-active/);
});
