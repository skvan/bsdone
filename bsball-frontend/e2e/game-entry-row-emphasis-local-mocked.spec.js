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
const awayPitcher = { id: 211, teamId: AWAY_ID, name: '客投', number: '11' };
const homePitcher = { id: 111, teamId: HOME_ID, name: '林投', number: '11' };
const players = {
  [AWAY_ID]: [...awayPlayers, awayPitcher],
  [HOME_ID]: [...homePlayers, homePitcher]
};

const stats = [...awayPlayers, ...homePlayers].map((player, index) => {
  const position = LINEUP_POSITIONS[index % 9];
  const catcher = position === 'C';
  const first = index % 9 === 0;
  return {
    id: player.id,
    teamId: player.teamId,
    playerId: player.id,
    battingOrder: (index % 9) + 1,
    listOrder: index % 9,
    position,
    number: player.number,
    pa: 4,
    ab: 4,
    h: index % 3,
    po: catcher ? 8 : first ? 2 : 0,
    a: catcher ? 1 : 0,
    e: 0,
    dp: 0,
    defInn: 9,
    pb: catcher ? (player.teamId === AWAY_ID ? 1 : 2) : 0,
    catcherSb: catcher ? (player.teamId === AWAY_ID ? 2 : 1) : 0,
    catcherCs: catcher ? (player.teamId === AWAY_ID ? 1 : 0) : 0
  };
});
stats.push(
  {
    id: 211, teamId: AWAY_ID, playerId: 211, isPitcher: 1, pitcherOrder: 1, listOrder: 0,
    position: 'P', number: '11', ip: 9, np: 100, pitchR: 3, er: 2, pitchH: 6, pitchSo: 5, pitchBb: 2, gs: 1
  },
  {
    id: 111, teamId: HOME_ID, playerId: 111, isPitcher: 1, pitcherOrder: 1, listOrder: 0,
    position: 'P', number: '11', ip: 9, np: 100, pitchR: 5, er: 4, pitchH: 8, pitchSo: 4, pitchBb: 3, gs: 1
  }
);

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

  const firstNameCell = rows.first().locator('td').nth(1);
  await firstNameCell.click();
  await expect(rows.first()).toHaveClass(/current-row/);
  await firstNameCell.click();
  await expect(rows.first()).toHaveClass(/current-row/);
  await expect(rows.nth(1)).not.toHaveClass(/current-row/);

  await page.getByRole('tab', { name: '投手数据' }).click();
  const pitcherTable = page.locator('[data-game-result-nav="away-pitcher"]');
  await expect(pitcherTable).toBeVisible();
  const pitcherRow = pitcherTable.locator('.el-table__body-wrapper tbody tr').first();
  await expect(pitcherRow).toBeVisible();
  await pitcherRow.locator('.stat-input input').first().click();
  await expect(pitcherRow).toHaveClass(/current-row/);
  const pitcherAccent = await pitcherRow.locator('td').first().evaluate((el) => getComputedStyle(el).boxShadow);
  expect(pitcherAccent).toContain('rgb(64, 158, 255)');
  await pitcherRow.locator('td').nth(1).click();
  await expect(pitcherRow).toHaveClass(/current-row/);

  await page.getByRole('tab', { name: '守备数据' }).click();
  const fieldingTable = page.locator('.game-fielding-table', { hasText: '客队A 守备' });
  await expect(fieldingTable).toBeVisible();
  const fieldingRow = fieldingTable.locator('.el-table__body-wrapper tbody tr').first();
  await expect(fieldingRow).toBeVisible();
  await fieldingRow.locator('.stat-input input').first().click();
  await expect(fieldingRow).toHaveClass(/current-row/);
  const fieldingAccent = await fieldingRow.locator('td').first().evaluate((el) => getComputedStyle(el).boxShadow);
  expect(fieldingAccent).toContain('rgb(64, 158, 255)');
  await fieldingRow.locator('td').first().click();
  await expect(fieldingRow).toHaveClass(/current-row/);
}

for (const dark of [false, true]) {
  test(`后台补录页：编辑中的行保持当前行高亮（${dark ? '深色' : '浅色'}）`, async ({ page }) => {
    await mockEntryPage(page, { dark });
    await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/edit');
    await assertEditingRow(page);
  });
}

test('后台补录页：守备表显示 Totals 汇总行，捕手行保留在守备表中', async ({ page }) => {
  await mockEntryPage(page);
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/edit');
  await expect(page.locator('.game-entry-page')).toBeVisible();

  await expect(page.getByRole('tab', { name: '捕手数据' })).toHaveCount(0);
  await page.getByRole('tab', { name: '守备数据' }).click();
  const fieldingTable = page.locator('.game-fielding-table', { hasText: '客队A 守备' });
  await expect(fieldingTable).toBeVisible();
  const fieldingFooter = fieldingTable.locator('.el-table__footer-wrapper');
  await expect(fieldingFooter).toContainText('Totals');
  await expect(fieldingFooter.locator('td').nth(4)).toHaveText('10');

  const catcherRow = fieldingTable.locator('.el-table__body-wrapper tbody tr', { hasText: '客2' });
  await expect(catcherRow).toBeVisible();
  await expect(fieldingTable.locator('.el-table__header-wrapper thead')).toContainText('PB');
});

test('后台补录页：列宽可拖拽调整', async ({ page }) => {
  await mockEntryPage(page);
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/edit');
  const table = page.locator('[data-game-result-nav="away-batter"]');
  const header = table.locator('.el-table__header-wrapper thead th').filter({ hasText: 'PA' }).first();
  await expect(header).toBeVisible();
  await header.scrollIntoViewIfNeeded();
  const box = await header.boundingBox();
  const before = box.width;
  await page.mouse.move(box.x + box.width - 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width + 40, box.y + box.height / 2, { steps: 8 });
  await page.mouse.up();
  const after = await header.evaluate((el) => el.getBoundingClientRect().width);
  expect(Math.abs(after - before)).toBeGreaterThan(10);
});
