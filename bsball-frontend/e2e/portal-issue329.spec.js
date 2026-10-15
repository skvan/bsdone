// 门户页优化（Issue #329）本地无头冒烟：
//   1) 菜单「联盟球队」→「球队」；2) 联盟页点卡进详情（仅本联盟下辖赛事，可定位赛程）；
//   3) 球队页末位「自由球员」卡 → 列表（teamId=0 口径）→ 球员详情。
// 运行：npx playwright test e2e/portal-issue329.spec.js（playwright.config 自动起 dev:3000；/bsball-server 由本文件拦截）
import { test, expect } from '@playwright/test';

const TENANT = 'bs-ball';
const YEAR = new Date().getFullYear();

const league = { id: 1, name: '测试联盟', nameEn: 'Test League', description: '联盟描述' };
const events = [
  { id: 11, name: '春季联赛', season: '2026春', leagueId: 1, startDate: `${YEAR}-03-01`, endDate: `${YEAR}-05-30`, status: 'ongoing', gameMode: 'BASEBALL' },
  { id: 12, name: '秋季联赛', season: '2026秋', leagueId: 1, startDate: `${YEAR}-09-01`, endDate: `${YEAR}-11-30`, status: 'draft', gameMode: 'BASEBALL' },
  { id: 13, name: '他盟赛事', season: '2026', leagueId: 2, startDate: `${YEAR}-01-01`, endDate: `${YEAR}-02-01`, status: 'ended', gameMode: 'BASEBALL' }
];
const games = [
  { id: 101, eventId: 11, homeTeamId: 1, awayTeamId: 2, homeScore: 3, awayScore: 2, status: 'final', gameday: `${YEAR}-03-10 10:00` }
];
const teams = [
  { id: 1, name: '甲队' },
  { id: 2, name: '乙队' }
];
const freeAgents = [
  { id: 21, name: '自由人甲', positions: ['P', '1B'], avatar: '' },
  { id: 22, name: '自由人乙', positions: ['C'], avatar: '', status: 'retired' }
];

async function mockApi(page, captured) {
  // headless 浏览器 navigator.language=en-US 会令应用切英文，固定 localStorage 语言为 zh-CN（键同 i18n/index.js）
  await page.addInitScript(() => localStorage.setItem('bs-ball-locale', 'zh-CN'));
  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    captured.push(`${request.method()} ${path}${url.search}`);
    let data = [];
    if (path === '/league/list') data = { list: [league], total: 1 };
    else if (path === '/league/1') data = league;
    else if (path === '/league/3') data = { id: 3, name: '无赛事联盟' };
    else if (path === '/league/999') data = null;
    else if (path === '/event/list') data = { list: events, total: events.length };
    else if (path === '/game/list') data = { list: games, total: games.length };
    else if (path === '/team/list') data = { list: teams, total: teams.length };
    else if (path === '/team/select-options') data = teams.map(({ id, name }) => ({ id, name }));
    else if (path === '/player/list') {
      data = url.searchParams.get('teamId') === '0'
        ? { list: freeAgents, total: freeAgents.length }
        : { list: [], total: 0 };
    } else if (path === '/portal/settings') data = {};
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

test('菜单「球队」更名生效；联盟页点卡进详情，仅显示本联盟下辖赛事并可定位赛程', async ({ page }) => {
  const captured = [];
  await mockApi(page, captured);

  await page.goto(`/bs-ball/${TENANT}/leagues`);
  await expect(page.locator('nav.nav').getByRole('link', { name: '球队', exact: true })).toBeVisible();
  // 注意：nav 内「联盟」「球队」两链接文本相邻，元素拼接文本会出现“联盟球队”假命中，必须按链接精确断言
  await expect(page.locator('nav.nav').getByRole('link', { name: '联盟球队', exact: true })).toHaveCount(0);

  await page.getByRole('link', { name: /测试联盟/ }).click();
  await expect(page).toHaveURL(new RegExp(`/${TENANT}/leagues/1$`));
  await expect(page).toHaveTitle(/联盟详情/);
  await expect(page.getByRole('heading', { name: '测试联盟' })).toBeVisible();
  await expect(page.getByText('春季联赛')).toBeVisible();
  await expect(page.getByText('秋季联赛')).toBeVisible();
  await expect(page.getByText('他盟赛事')).toHaveCount(0);

  await page.getByRole('link', { name: /春季联赛/ }).click();
  await expect(page).toHaveURL(/events\?eventId=11/);
  await expect(page.getByText('春季联赛').first()).toBeVisible();
});

test('联盟详情边界：不存在联盟显示空态；无赛事联盟显示赛事空态', async ({ page }) => {
  const captured = [];
  await mockApi(page, captured);

  await page.goto(`/bs-ball/${TENANT}/leagues/999`);
  await expect(page.getByText('联盟不存在或已删除')).toBeVisible();

  await page.goto(`/bs-ball/${TENANT}/leagues/3`);
  await expect(page.getByRole('heading', { name: '无赛事联盟' })).toBeVisible();
  await expect(page.getByText('该联盟暂无赛事')).toBeVisible();
});

test('球队页末位「自由球员」卡进入列表：显示无现属球队球员（含退役）并可进详情', async ({ page }) => {
  const captured = [];
  await mockApi(page, captured);

  await page.goto(`/bs-ball/${TENANT}/teams`);
  await expect(page.getByText('甲队')).toBeVisible();
  await expect(page).toHaveTitle(/球队/);

  await page.getByRole('link', { name: /自由球员/ }).click();
  await expect(page).toHaveURL(new RegExp(`/${TENANT}/teams/free-agents$`));
  await expect(page.getByRole('heading', { name: '自由球员' })).toBeVisible();
  await expect(page.getByText('自由人甲')).toBeVisible();
  await expect(page.getByText('自由人乙')).toBeVisible();
  expect(captured.some((entry) => entry.startsWith('GET /player/list') && entry.includes('teamId=0'))).toBe(true);

  await page.getByRole('link', { name: /自由人甲/ }).click();
  await expect(page).toHaveURL(new RegExp(`/${TENANT}/players/21$`));
});
