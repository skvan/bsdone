import { test, expect } from '@playwright/test';

const GAME_ID = 10;
const HOME_ID = 1;
const AWAY_ID = 2;

const LINEUP_POSITIONS = ['CF', 'C', 'SS', '1B', '2B', '3B', 'LF', 'RF', 'DH'];
const awayPlayers = Array.from({ length: 9 }, (_, i) => ({
  id: 201 + i, teamId: AWAY_ID, name: `客${i + 1}`, number: String(i + 1)
}));
const homePlayers = Array.from({ length: 9 }, (_, i) => ({
  id: 101 + i, teamId: HOME_ID, name: `林${i + 1}`, number: String(i + 1)
}));
const players = { [AWAY_ID]: awayPlayers, [HOME_ID]: homePlayers };

const stats = [...awayPlayers, ...homePlayers].map((player, index) => ({
  id: player.id,
  teamId: player.teamId,
  playerId: player.id,
  battingOrder: (index % 9) + 1,
  listOrder: index % 9,
  position: LINEUP_POSITIONS[index % 9],
  number: player.number,
  pa: 4,
  ab: 4,
  h: index % 3
}));

const game = {
  id: GAME_ID,
  eventId: 1,
  homeTeamId: HOME_ID,
  awayTeamId: AWAY_ID,
  status: 'final',
  gameMode: 'BASEBALL',
  homeScore: 5,
  awayScore: 3,
  totalInnings: 9,
  homeScoreByInning: ['0', '1', '0', '0', '0', '0', '0', '0', ''],
  awayScoreByInning: ['1', '0', '0', '0', '0', '0', '0', '0', '']
};

async function mockEntryPage(page, { dark = false } = {}) {
  await page.addInitScript((isDark) => {
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
    localStorage.setItem('bs-ball-admin-settings', JSON.stringify({ theme: isDark ? 'dark' : 'light' }));
  }, dark);

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];

    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event', gameMode: 'BASEBALL' };
    else if (path === `/game/${GAME_ID}`) data = game;
    else if (path === `/game/${GAME_ID}/stats`) data = { list: stats, total: stats.length };
    else if (path === `/game/${GAME_ID}/substitutions`) data = { list: [], total: 0 };
    else if (path === '/team/select-options' || path === '/event/1/game-team-options') data = [
      { id: HOME_ID, name: '暴力羊' },
      { id: AWAY_ID, name: '客队A' }
    ];
    else if (path === '/game/list') data = { list: [], total: 0 };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 0);
      const list = players[teamId] ?? [];
      data = { list, total: list.length };
    } else if (/^\/team\/(1|2)$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      const name = id === HOME_ID ? '暴力羊' : '客队A';
      data = { id, name, shortName: name };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/player/select-options') data = [];

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

async function assertEditingRow(page) {
  await expect(page.locator('.game-entry-page')).toBeVisible();

  const awayTable = page.locator('[data-game-result-nav="away-batter"]');
  await expect(awayTable).toBeVisible();
  const rows = awayTable.locator('.el-table__body-wrapper tbody tr').filter({ hasNot: page.locator('.row-totals') });
  await expect(rows.first()).toBeVisible();

  const firstInput = rows.first().locator('.stat-input input').first();
  await firstInput.click();
  await expect(rows.first()).toHaveClass(/current-row/);

  await firstInput.click();
  await expect(rows.first()).toHaveClass(/current-row/);

  const secondInput = rows.nth(1).locator('.stat-input input').first();
  await secondInput.focus();
  await expect(rows.nth(1)).toHaveClass(/current-row/);
  await expect(rows.first()).not.toHaveClass(/current-row/);

  const accent = await rows.nth(1).locator('td').first().evaluate((el) => getComputedStyle(el).boxShadow);
  expect(accent).toContain('rgb(64, 158, 255)');
}

for (const dark of [false, true]) {
  test(`后台补录页：编辑中的行保持当前行高亮（${dark ? '深色' : '浅色'}）`, async ({ page }) => {
    await mockEntryPage(page, { dark });
    await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/edit');
    await assertEditingRow(page);
  });
}
