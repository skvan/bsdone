import { test, expect } from '@playwright/test';

// 生产 268 复核复现：DH 取消后 1B 守备行、投手守备行都要出现在结果录入守备页签。
// 运行：E2E_PORT=3030 npx playwright test e2e/admin-game-result-fielding-268-local-mocked.spec.js
const GAME_ID = 10;
const HOME_ID = 2;
const AWAY_ID = 1;

const game = {
  id: GAME_ID,
  eventId: 1,
  homeTeamId: HOME_ID,
  awayTeamId: AWAY_ID,
  status: 'final',
  gameMode: 'BASEBALL',
  homeScore: 1,
  awayScore: 0,
  gameday: '2026-11-01',
  totalInnings: 4,
  venue: '测试球场'
};

const awayPlayers = [
  { id: 164, teamId: AWAY_ID, name: '吴昌言', number: '83', positions: [] },
  { id: 44, teamId: AWAY_ID, name: '姚智鹏', number: '26', positions: ['P'] },
  { id: 21, teamId: AWAY_ID, name: '王译圣', number: '40', positions: ['3B', 'P'] },
  { id: 23, teamId: AWAY_ID, name: '邝逸舜', number: '47', positions: ['LF'] },
  { id: 31, teamId: AWAY_ID, name: '谭嘉民', number: '32', positions: ['1B'] }
];
const homePlayers = [
  { id: 101, teamId: HOME_ID, name: '主队投手', number: '1', positions: ['P'] },
  { id: 102, teamId: HOME_ID, name: '主队捕手', number: '2', positions: ['C'] }
];

const awayStats = [
  {
    id: 7001, teamId: AWAY_ID, playerId: 44, isPitcher: 1,
    position: 'P', firstPosition: 'P', startingPosition: 'P',
    battingOrder: 2, listOrder: 1, pitcherOrder: 1, ip: 4, np: 50,
    po: 0, a: 0, e: 0, dp: 0, defInn: 0
  },
  {
    id: 7002, teamId: AWAY_ID, playerId: 23, isPitcher: 0,
    position: 'LF', firstPosition: 'LF', startingPosition: 'LF',
    battingOrder: 3, listOrder: 2, po: 1, a: 0, e: 0, dp: 0, defInn: 4
  },
  {
    id: 7003, teamId: AWAY_ID, playerId: 31, isPitcher: 0,
    position: '1B', firstPosition: '1B', startingPosition: '1B',
    battingOrder: 4, listOrder: 3, po: 2, a: 0, e: 0, dp: 0, defInn: 4
  },
  {
    id: 7004, teamId: AWAY_ID, playerId: 21, isPitcher: 0,
    position: '3B', firstPosition: '3B', startingPosition: '3B',
    battingOrder: 5, listOrder: 4, po: 1, a: 2, e: 0, dp: 0, defInn: 4
  },
  {
    id: 7005, teamId: AWAY_ID, playerId: 21, isPitcher: 1,
    position: 'P', firstPosition: '3B', startingPosition: '3B',
    battingOrder: 5, listOrder: 4, pitcherOrder: 2, ip: 1, np: 12,
    po: 0, a: 0, e: 0, dp: 0, defInn: 0
  }
];
const homeStats = [
  {
    id: 7101, teamId: HOME_ID, playerId: 101, isPitcher: 0,
    position: 'P', firstPosition: 'P', startingPosition: 'P',
    battingOrder: 1, listOrder: 0, po: 0, a: 1, e: 0, dp: 0, defInn: 4
  },
  {
    id: 7102, teamId: HOME_ID, playerId: 102, isPitcher: 0,
    position: 'C', firstPosition: 'C', startingPosition: 'C',
    battingOrder: 2, listOrder: 1, po: 4, a: 0, e: 0, dp: 0, defInn: 4
  }
];

const fieldingPositions = [
  // 吴昌言只存在于逐位置守备表：先发 DH，5 局取消 DH 后守 1B
  { teamId: AWAY_ID, playerId: 164, position: '1B', outs: 12, po: 10, a: 1, e: 0, dp: 1, fieldingGs: true },
  { teamId: AWAY_ID, playerId: 44, position: 'P', outs: 12, po: 0, a: 2, e: 0, dp: 0, fieldingGs: true },
  { teamId: AWAY_ID, playerId: 23, position: 'LF', outs: 12, po: 1, a: 0, e: 0, dp: 0, fieldingGs: true },
  { teamId: AWAY_ID, playerId: 31, position: '1B', outs: 12, po: 2, a: 0, e: 0, dp: 0, fieldingGs: true },
  { teamId: AWAY_ID, playerId: 21, position: '3B', outs: 9, po: 1, a: 2, e: 0, dp: 0, fieldingGs: true },
  { teamId: AWAY_ID, playerId: 21, position: 'P', outs: 3, po: 0, a: 0, e: 0, dp: 0, fieldingGs: false },
  { teamId: HOME_ID, playerId: 101, position: 'P', outs: 12, po: 0, a: 1, e: 0, dp: 0, fieldingGs: true },
  { teamId: HOME_ID, playerId: 102, position: 'C', outs: 12, po: 4, a: 0, e: 0, dp: 0, fieldingGs: true }
];

async function mockApis(page) {
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
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];

    if (path === '/event/1') data = { id: 1, name: 'Production 268' };
    else if (path === `/game/${GAME_ID}`) data = game;
    else if (path === `/game/${GAME_ID}/stats`) {
      data = { list: [...awayStats, ...homeStats], total: awayStats.length + homeStats.length };
    } else if (path === `/game/${GAME_ID}/fielding-positions`) {
      data = { list: fieldingPositions, total: fieldingPositions.length };
    } else if (path === '/team/select-options' || path === '/event/1/game-team-options') {
      data = [
        { id: AWAY_ID, name: '客队A', teamId: AWAY_ID },
        { id: HOME_ID, name: '主队B', teamId: HOME_ID }
      ];
    } else if (path === '/game/list') {
      data = { list: [game], total: 1 };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 0);
      const list = teamId === AWAY_ID ? awayPlayers : teamId === HOME_ID ? homePlayers : [...awayPlayers, ...homePlayers];
      data = { list, total: list.length };
    } else if (/^\/team\/1$/.test(path) || /^\/team\/2$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === AWAY_ID ? '客队A' : '主队B', shortName: id === AWAY_ID ? '客队A' : '主队B' };
    } else if (path === `/game/${GAME_ID}/earned-runs/decisions`) {
      data = [];
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

async function openFieldingTab(page) {
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  const tab = page.getByRole('tab', { name: '守备数据' });
  await expect(tab).toBeVisible();
  await tab.click();
  return page.locator('.game-fielding-table', { hasText: '客队A 守备' });
}

test('结果录入守备表：DH 取消后 1B 行与投手 P 行都要显示', async ({ page }) => {
  await mockApis(page);
  const awayTable = await openFieldingTab(page);
  await expect(awayTable).toBeVisible();

  const wuRow = awayTable.locator('tbody tr', { hasText: '吴昌言' }).first();
  await expect(wuRow).toBeVisible();
  await expect(wuRow.locator('td').nth(2)).toHaveText('1B');
  await expect(wuRow.locator('.el-input-number input').nth(1)).toHaveValue('10');
  await expect(wuRow.locator('.el-input-number input').nth(2)).toHaveValue('1');
  await expect(wuRow.locator('.el-input-number input').nth(4)).toHaveValue('1');

  const pitcherRow = awayTable.locator('tbody tr', { hasText: '姚智鹏' }).first();
  await expect(pitcherRow).toBeVisible();
  await expect(pitcherRow.locator('td').nth(2)).toHaveText('P');
  await expect(pitcherRow.locator('.el-input-number input').nth(2)).toHaveValue('2');
  await expect(pitcherRow.locator('.el-input-number input').nth(2)).toBeEditable();

  const twoWayRow = awayTable.locator('tbody tr', { hasText: '王译圣' }).first();
  await expect(twoWayRow).toBeVisible();
  await expect(awayTable.locator('tbody tr', { hasText: '王译圣' })).toHaveCount(1);
  await expect(twoWayRow.locator('td').nth(2)).toHaveText('3B');
  await expect(twoWayRow.locator('.el-input-number input').nth(1)).toHaveValue('1');
  await expect(twoWayRow.locator('.el-input-number input').nth(2)).toHaveValue('2');

  const homeTable = page.locator('.game-fielding-table', { hasText: '主队B 守备' });
  const homePitcherRow = homeTable.locator('tbody tr', { hasText: '主队投手' }).first();
  await expect(homePitcherRow).toBeVisible();
  await expect(homePitcherRow.locator('td').nth(2)).toHaveText('P');
});
