import { test, expect } from '@playwright/test';

const GAME_ID = 10;
const HOME_ID = 1;
const AWAY_ID = 2;

const game = {
  id: GAME_ID,
  eventId: 1,
  homeTeamId: HOME_ID,
  awayTeamId: AWAY_ID,
  status: 'final',
  gameMode: 'BASEBALL',
  homeScore: 5,
  awayScore: 3,
  scheduledTime: '2026-10-18T14:00:00',
  venue: '测试球场',
  homeTeamName: '暴力羊',
  awayTeamName: '客队A'
};

const homePlayers = [
  { id: 101, teamId: HOME_ID, name: '林一', shortName: '林一', number: '1' },
  { id: 102, teamId: HOME_ID, name: '吴二', shortName: '吴二', number: '2' },
  { id: 103, teamId: HOME_ID, name: '周捕', shortName: '周捕', number: '3' },
  { id: 104, teamId: HOME_ID, name: '李投', shortName: '李投', number: '4' }
];
const awayPlayers = [
  { id: 201, teamId: AWAY_ID, name: '客一', shortName: '客一', number: '1' },
  { id: 202, teamId: AWAY_ID, name: '客二', shortName: '客二', number: '2' }
];

const homeStats = [
  {
    teamId: HOME_ID, playerId: 101, position: 'CF', firstPosition: 'CF', startingPosition: 'CF',
    battingOrder: 1, listOrder: 0, fieldingGs: 1, po: 2, a: 0, e: 0, dp: 0, defInn: 9,
    pb: 0, catcherSb: 0, catcherCs: 0
  },
  {
    teamId: HOME_ID, playerId: 102, position: 'P', firstPosition: '3B', startingPosition: '3B',
    battingOrder: 2, listOrder: 1, fieldingGs: 1, po: 1, a: 3, e: 1, dp: 1, defInn: 9,
    pb: 0, catcherSb: 0, catcherCs: 0
  },
  {
    teamId: HOME_ID, playerId: 103, position: 'C', firstPosition: 'C', startingPosition: 'C',
    battingOrder: 3, listOrder: 2, fieldingGs: 1, po: 8, a: 1, e: 0, dp: 0, defInn: 9,
    pb: 1, catcherSb: 2, catcherCs: 1
  },
  {
    teamId: HOME_ID, playerId: 104, position: 'P', firstPosition: 'P', startingPosition: 'P',
    battingOrder: 4, listOrder: 3, fieldingGs: 1, isPitcher: 1, po: 0, a: 1, e: 0, dp: 0,
    defInn: 9, pb: 0, catcherSb: 0, catcherCs: 0
  }
];

const awayStats = [
  {
    teamId: AWAY_ID, playerId: 201, position: 'P', firstPosition: 'P', startingPosition: 'P',
    battingOrder: 1, listOrder: 0, fieldingGs: 1, isPitcher: 1, po: 0, a: 1, e: 0, dp: 0,
    defInn: 9, pb: 0, catcherSb: 0, catcherCs: 0
  },
  {
    teamId: AWAY_ID, playerId: 202, position: 'C', firstPosition: 'C', startingPosition: 'C',
    battingOrder: 2, listOrder: 1, fieldingGs: 1, po: 6, a: 0, e: 0, dp: 0, defInn: 9,
    pb: 0, catcherSb: 1, catcherCs: 0
  }
];

const fieldingPositions = [
  { teamId: HOME_ID, playerId: 102, position: '3B', outs: 15, po: 1, a: 2, e: 1, dp: 1, fieldingGs: true },
  { teamId: HOME_ID, playerId: 102, position: 'P', outs: 12, po: 0, a: 1, e: 0, dp: 0, fieldingGs: false }
];

async function mockGameDetailApis(page) {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
    localStorage.setItem('bsball.portal.fieldSettingsGuide.v1', JSON.stringify({
      v: 1, byTenant: { 'bs-ball': true }, legacyPortalMigrated: true
    }));
  });

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];

    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event' };
    else if (path === `/game/${GAME_ID}`) data = game;
    else if (path === `/game/${GAME_ID}/stats`) data = { list: [...awayStats, ...homeStats], total: awayStats.length + homeStats.length };
    else if (path === `/game/${GAME_ID}/fielding-positions`) data = { list: fieldingPositions, total: fieldingPositions.length };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 0);
      const list = teamId === HOME_ID ? homePlayers : teamId === AWAY_ID ? awayPlayers : [...homePlayers, ...awayPlayers];
      data = { list, total: list.length };
    } else if (/^\/team\/(1|2)$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === HOME_ID ? '暴力羊' : '客队A', shortName: id === HOME_ID ? '暴力羊' : '客队A' };
    } else if (path.endsWith('/earned-runs/decisions') || path.endsWith('/hit-spray/records')) {
      data = { list: [], total: 0 };
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

async function assertFieldingBreakdown(page, path) {
  await page.goto(path);
  const fieldingTab = page.getByRole('button', { name: /^(Fielding|防守)$/ });
  await expect(fieldingTab).toBeVisible();
  await fieldingTab.click();

  const homeTable = page.locator('.gd-fielding-table', { hasText: '暴力羊 守备' });
  await expect(homeTable).toBeVisible();

  const singleRow = homeTable.locator('tbody tr', { hasText: '林一' }).first();
  await expect(singleRow.getByRole('button')).toHaveCount(0);

  const playerRow = homeTable.locator('tbody tr', { hasText: '吴二' }).first();
  await expect(playerRow).toBeVisible();
  const expander = playerRow.getByRole('button', { name: /展开 吴二/ });
  await expect(expander).toBeVisible();

  const cells = playerRow.locator('td');
  await expect(cells.nth(1)).toHaveText(/^\s*$/);

  await expander.click();
  await expect(playerRow.getByRole('button', { name: /收起 吴二/ })).toHaveAttribute('aria-expanded', 'true');
  const breakdownRows = homeTable.locator('tbody tr.gd-fielding-table__row--bd');
  await expect(breakdownRows).toHaveCount(2);

  const breakdown3B = breakdownRows.nth(0);
  await expect(breakdown3B).toContainText('3B');
  await expect(breakdown3B.locator('td').nth(1)).toHaveText('3B');
  await expect(breakdown3B.locator('td').nth(3)).toHaveText('1');
  await expect(breakdown3B.locator('td').nth(4)).toHaveText('2');
  await expect(breakdown3B.locator('td').nth(7)).toHaveText('5');
  await expect(breakdown3B.locator('.gd-fielding-table__stat--dim')).toHaveCount(3);

  const breakdownP = breakdownRows.nth(1);
  await expect(breakdownP).toContainText('P');
  await expect(breakdownP.locator('td').nth(7)).toHaveText('4');

  const totalsRow = homeTable.locator('.el-table__footer-wrapper tr').filter({ hasText: 'Totals' });
  await expect(totalsRow.locator('td').nth(3)).toHaveText('11');
  await expect(totalsRow.locator('td').nth(3)).not.toHaveText('12');

  await cells.nth(0).click();
  await expect(breakdownRows).toHaveCount(0);
  await expect(playerRow.getByRole('button', { name: /展开 吴二/ })).toHaveAttribute('aria-expanded', 'false');
}

test('后台比赛详情守备表：多守位球员可展开逐位置明细，TOTALS 不重复统计', async ({ page }) => {
  await mockGameDetailApis(page);
  await assertFieldingBreakdown(page, `/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}`);
});

test('门户比赛详情守备表：多守位球员可展开逐位置明细，TOTALS 不重复统计', async ({ page }) => {
  await mockGameDetailApis(page);
  await assertFieldingBreakdown(page, `/bs-ball/bs-ball/events/1/games/${GAME_ID}`);
});
