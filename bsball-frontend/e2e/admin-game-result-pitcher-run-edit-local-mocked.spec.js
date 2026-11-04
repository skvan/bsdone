import { test, expect } from '@playwright/test';

// 结果录入页：引擎场次（final）允许直接改投手 R/ER，保存时带人工覆写标记。
const GAME_ID = 10;
const HOME_ID = 2;
const AWAY_ID = 1;
const BATTER_POSITIONS = ['C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'DH'];

const game = {
  id: GAME_ID,
  eventId: 1,
  homeTeamId: HOME_ID,
  awayTeamId: AWAY_ID,
  status: 'final',
  gameMode: 'BASEBALL',
  engineManaged: true,
  homeScore: 3,
  awayScore: 2,
  gameday: '2026-11-04',
  totalInnings: 9,
  venue: '测试球场'
};

function makePlayers(teamId, offset) {
  const batters = BATTER_POSITIONS.map((position, index) => ({
    id: offset + index + 1,
    name: `T${teamId}P${index + 1}`,
    number: String(index + 1),
    teamId,
    positions: [position],
    batHand: 'R',
    throwHand: 'R',
    status: 'active'
  }));
  const pitcher = {
    id: offset + 100,
    name: `T${teamId}PITCHER`,
    number: '99',
    teamId,
    positions: ['P'],
    batHand: 'R',
    throwHand: 'R',
    status: 'active'
  };
  return [...batters, pitcher];
}

function makeStats(teamId, players, statIdOffset) {
  return players.map((player, index) => {
    const isPitcher = player.positions[0] === 'P';
    const batterIndex = isPitcher ? null : index;
    return {
      id: statIdOffset + index, gameId: GAME_ID, teamId, playerId: player.id,
      isPitcher: isPitcher ? 1 : 0,
      battingOrder: isPitcher ? null : batterIndex + 1,
      listOrder: index,
      pitcherOrder: isPitcher ? 1 : null,
      position: player.positions[0], number: player.number,
      batHand: player.batHand, throwHand: player.throwHand,
      pa: 0, ab: 0, r: 0, h: 0, e: 0, bbHp: 0, sb: 0, so: 0, soSwing: 0, soLooking: 0,
      rbi: 0, hr: 0, insideParkHr: 0, doubles: 0, triples: 0, gdp: 0, sh: 0, sf: 0,
      fieldingGs: 1, defInn: 0, dp: 0, pb: 0, catcherSb: 0, catcherCs: 0,
      ip: 0, pitchR: 0, er: 0, pitchH: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0,
      pitchInsideParkHr: 0, pitchPa: 0, pitchBf: 0, np: 0, pitchBb: 0, pitchIbb: 0,
      pitchHbp: 0, wp: 0, bk: 0, go: 0, fo: 0, gs: 0, svo: 0, cg: 0, pg: 0,
      w: 0, l: 0, sv: 0, hld: 0
    };
  });
}

const awayPlayers = makePlayers(AWAY_ID, 1000);
const homePlayers = makePlayers(HOME_ID, 2000);
const awayStats = makeStats(AWAY_ID, awayPlayers, 3000);
const homeStats = makeStats(HOME_ID, homePlayers, 4000);

async function setup(page) {
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

    if (path === '/event/1') data = { id: 1, name: 'Result Pitcher Run E2E' };
    else if (path === `/game/${GAME_ID}`) data = game;
    else if (path === `/game/${GAME_ID}/stats`) {
      data = { list: [...awayStats, ...homeStats], total: awayStats.length + homeStats.length };
    } else if (path === `/game/${GAME_ID}/fielding-positions`) {
      data = { list: [], total: 0 };
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
    } else if (/^\/team\/[12]$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === AWAY_ID ? '客队A' : '主队B', shortName: id === AWAY_ID ? '客队A' : '主队B' };
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

test('final 引擎场次：结果录入页可直接改投手 R/ER 并带人工覆写标记', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);

  const tab = page.getByRole('tab', { name: '投手数据' });
  await expect(tab).toBeVisible();
  await tab.click();

  const awayTable = page.locator('.el-table').filter({ hasText: 'T1PITCHER' }).first();
  const pitcherRow = awayTable.locator('tbody tr', { hasText: 'T1PITCHER' }).first();
  await expect(pitcherRow).toBeVisible();

  const headers = awayTable.locator('.el-table__header th');
  const count = await headers.count();
  let rIndex = -1;
  let erIndex = -1;
  for (let i = 0; i < count; i += 1) {
    const text = (await headers.nth(i).innerText()).trim();
    if (text === 'R') rIndex = i;
    if (text === 'ER') erIndex = i;
  }
  expect(rIndex).toBeGreaterThan(-1);
  expect(erIndex).toBeGreaterThan(-1);

  const rInput = pitcherRow.locator('td').nth(rIndex).locator('input');
  const erInput = pitcherRow.locator('td').nth(erIndex).locator('input');
  await expect(rInput).toBeEditable();
  await expect(erInput).toBeEditable();
  await rInput.fill('5');
  await rInput.blur();
  await erInput.fill('4');
  await erInput.blur();

  const saveRequest = page.waitForRequest((request) => (
    request.method() === 'POST'
    && request.url().includes(`/game/${GAME_ID}/save-result`)
  ));
  await page.getByRole('button', { name: '保存比赛数据' }).click();
  const body = (await saveRequest).postDataJSON();
  const pitcherPart = body.stats.find((part) => (
    Number(part.playerId) === 1100 && Number(part.isPitcher) === 1
  ));

  expect(pitcherPart).toBeTruthy();
  expect(pitcherPart.manualPitcherRunOverride).toBe(true);
  expect(Number(pitcherPart.pitchR)).toBe(5);
  expect(Number(pitcherPart.er)).toBe(4);
});
