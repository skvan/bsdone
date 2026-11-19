import { test, expect } from '@playwright/test';

// Issue #701 后续：结果录入守备表逐位置编辑（Task 7 E2E）。
// 覆盖：展开两守位球员、逐位置 PO 改动驱动球员合计（只读文本）联动、
// 下拉「添加守位」/子行「删除守位」增删、保存 payload 只带 dirty 球员、
// 非 final 子行只读 + finalOnly 提示，以及「无逐位置行的旧球员」走差额回写且不带 parts。
// 保存前置：结果页保存会跑先发守备校验，主客两队都要有 9 名先发打者（打序 1–9、守位 P+C+7 野位各一）。
// 运行：E2E_PORT=3038 npx playwright test e2e/admin-game-result-fielding-positions-local-mocked.spec.js
const GAME_ID = 711;
const HOME_ID = 2;
const AWAY_ID = 1;
// 被测试的两守位球员（王译圣）：3B 先发 + P 后援
const TWO_WAY_PLAYER_ID = 21;

const TEAMS = [
  { id: AWAY_ID, name: '客队A', teamId: AWAY_ID },
  { id: HOME_ID, name: '主队B', teamId: HOME_ID }
];

// 主客各 9 名先发打者：3B 的王译圣有两条逐位置行（3B+P），其余球员各一条。
const AWAY_PLAYERS = [
  { id: 21, name: '王译圣', number: '40', position: '3B' },
  { id: 22, name: '陈大文', number: '7', position: 'P' },
  { id: 23, name: '林小虎', number: '18', position: 'C' },
  { id: 24, name: '张伟', number: '3', position: '1B' },
  { id: 25, name: '黄强', number: '6', position: '2B' },
  { id: 26, name: '刘洋', number: '11', position: 'SS' },
  { id: 27, name: '赵磊', number: '24', position: 'LF' },
  { id: 28, name: '周杰', number: '8', position: 'CF' },
  { id: 29, name: '吴迪', number: '9', position: 'RF' }
];
const HOME_PLAYERS = [
  { id: 101, name: '主队一', number: '1', position: 'P' },
  { id: 102, name: '主队二', number: '2', position: 'C' },
  { id: 103, name: '主队三', number: '3', position: '1B' },
  { id: 104, name: '主队四', number: '4', position: '2B' },
  { id: 105, name: '主队五', number: '5', position: '3B' },
  { id: 106, name: '主队六', number: '6', position: 'SS' },
  { id: 107, name: '主队七', number: '7', position: 'LF' },
  { id: 108, name: '主队八', number: '8', position: 'CF' },
  { id: 109, name: '主队九', number: '9', position: 'RF' }
];

function toPlayerOption(teamId, player) {
  return {
    id: player.id,
    teamId,
    name: player.name,
    number: player.number,
    positions: [player.position],
    status: 'active'
  };
}

function toStat(teamId, player, battingOrder, statId) {
  const isTwoWay = player.id === TWO_WAY_PLAYER_ID;
  return {
    id: statId,
    teamId,
    playerId: player.id,
    isPitcher: 0,
    position: player.position,
    firstPosition: player.position,
    startingPosition: player.position,
    battingOrder,
    listOrder: battingOrder - 1,
    po: 1,
    a: isTwoWay ? 2 : 0,
    e: 0,
    dp: 0,
    defInn: 4,
    fieldingGs: 1
  };
}

function toPositionRows(teamId, player) {
  if (player.id === TWO_WAY_PLAYER_ID) {
    return [
      { teamId, playerId: player.id, position: '3B', outs: 9, po: 1, a: 2, e: 0, dp: 0, fieldingGs: true },
      { teamId, playerId: player.id, position: 'P', outs: 3, po: 0, a: 0, e: 0, dp: 0, fieldingGs: false }
    ];
  }
  return [
    {
      teamId,
      playerId: player.id,
      position: player.position,
      outs: 12,
      po: 1,
      a: 0,
      e: 0,
      dp: 0,
      fieldingGs: true
    }
  ];
}

function buildFixture(options = {}) {
  const status = options.status ?? 'final';
  const legacyPlayerId = options.legacyPlayerId ?? null;
  const game = {
    id: GAME_ID,
    eventId: 1,
    homeTeamId: HOME_ID,
    awayTeamId: AWAY_ID,
    status,
    gameMode: 'BASEBALL',
    homeScore: 1,
    awayScore: 0,
    gameday: '2026-11-19',
    gameTime: '2026-11-19 10:00:00',
    gameNumber: 1,
    totalInnings: 4,
    venue: '测试球场'
  };
  const awayStats = AWAY_PLAYERS.map((player, index) => toStat(AWAY_ID, player, index + 1, 9101 + index));
  const homeStats = HOME_PLAYERS.map((player, index) => toStat(HOME_ID, player, index + 1, 9201 + index));
  const fieldingPositions = [
    ...AWAY_PLAYERS.filter((player) => player.id !== legacyPlayerId)
      .flatMap((player) => toPositionRows(AWAY_ID, player)),
    ...HOME_PLAYERS.flatMap((player) => toPositionRows(HOME_ID, player))
  ];
  return {
    game,
    awayPlayers: AWAY_PLAYERS.map((player) => toPlayerOption(AWAY_ID, player)),
    homePlayers: HOME_PLAYERS.map((player) => toPlayerOption(HOME_ID, player)),
    stats: [...awayStats, ...homeStats],
    fieldingPositions
  };
}

async function mockApis(page, fixture, onSave) {
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

    if (path === '/event/1') data = { id: 1, name: 'Issue 711' };
    else if (path === `/game/${GAME_ID}`) data = fixture.game;
    else if (path === `/game/${GAME_ID}/stats`) {
      data = { list: fixture.stats, total: fixture.stats.length };
    } else if (path === `/game/${GAME_ID}/fielding-positions`) {
      data = { list: fixture.fieldingPositions, total: fixture.fieldingPositions.length };
    } else if (path === '/team/select-options' || path === '/event/1/game-team-options') {
      data = TEAMS;
    } else if (path === '/game/list') {
      data = { list: [fixture.game], total: 1 };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 0);
      const list = teamId === AWAY_ID
        ? fixture.awayPlayers
        : teamId === HOME_ID
          ? fixture.homePlayers
          : [...fixture.awayPlayers, ...fixture.homePlayers];
      data = { list, total: list.length };
    } else if (/^\/team\/[12]$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === AWAY_ID ? '客队A' : '主队B', shortName: id === AWAY_ID ? '客队A' : '主队B' };
    } else if (path === `/game/${GAME_ID}/earned-runs/decisions`) {
      data = [];
    } else if (path === `/game/${GAME_ID}/save-result`) {
      onSave?.(request.postDataJSON());
      data = null;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, msg: 'ok', data })
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

test('逐位置编辑：展开、合计联动、增删与保存 payload', async ({ page }) => {
  let saved = null;
  await mockApis(page, buildFixture(), (body) => { saved = body; });
  const table = await openFieldingTab(page);
  await expect(table).toBeVisible();

  const playerRow = table.locator('tbody tr', { hasText: '王译圣' }).first();
  await expect(playerRow).toBeVisible();
  // 有逐位置行：球员级合计为只读文本（不是输入框）
  await expect(playerRow.locator('td').nth(4).locator('input')).toHaveCount(0);
  await expect(playerRow.locator('td').nth(4)).toHaveText('1');
  await expect(playerRow.locator('td').nth(5)).toHaveText('2');

  await playerRow.locator('.game-fielding-table__expander').click();
  const subRows = table.locator('tbody tr.row-position');
  await expect(subRows).toHaveCount(2);
  await expect(subRows.nth(0).locator('.game-fielding-table__sub-name')).toHaveText('↳ 3B');
  await expect(subRows.nth(0).locator('td').nth(4).locator('input')).toHaveValue('1');
  await expect(subRows.nth(0).locator('td').nth(5).locator('input')).toHaveValue('2');
  await expect(subRows.nth(1).locator('.game-fielding-table__sub-name')).toHaveText('↳ P');
  await expect(subRows.nth(1).locator('td').nth(4).locator('input')).toHaveValue('0');

  // 改 3B 的 PO：球员合计只读文本立即按逐位置汇总变化（PO 1+0 → 3+0）
  const poInput = subRows.nth(0).locator('td').nth(4).locator('input');
  await poInput.fill('3');
  await poInput.blur();
  await expect(playerRow.locator('td').nth(4)).toHaveText('3');

  // 下拉「添加守位」→ 1B：行数 +1，新增行默认计数据为 0
  await playerRow.getByRole('button', { name: '添加守位' }).click();
  const dropdown = page.locator('.el-dropdown__popper:visible');
  await expect(dropdown).toBeVisible();
  await dropdown.locator('.el-dropdown-menu__item', { hasText: /^1B$/ }).click();
  await expect(subRows).toHaveCount(3);
  await expect(subRows.last().locator('.game-fielding-table__sub-name')).toHaveText('↳ 1B');
  await expect(subRows.last().locator('td').nth(4).locator('input')).toHaveValue('0');

  // 删除刚加的 1B：二次确认后行数 -1，合计回到两条守位
  await subRows.last().getByRole('button', { name: '删除守位' }).click();
  const removeConfirm = page.locator('.el-message-box:visible');
  await expect(removeConfirm).toContainText('确定删除该守位行？');
  await removeConfirm.getByRole('button', { name: '确定' }).click();
  await expect(removeConfirm).toBeHidden();
  await expect(subRows).toHaveCount(2);
  await expect(playerRow.locator('td').nth(4)).toHaveText('3');

  await page.getByRole('button', { name: '保存比赛数据' }).click();
  await expect.poll(() => saved).toBeTruthy();

  // 只带 dirty 球员（王译圣）；rows 为该球员的两条守位，3B.po 为新值
  expect(saved.fieldingPlayerParts).toHaveLength(1);
  const part = saved.fieldingPlayerParts[0];
  expect(Number(part.teamId)).toBe(AWAY_ID);
  expect(Number(part.playerId)).toBe(21);
  expect(part.rows).toHaveLength(2);
  expect(part.rows).toEqual(expect.arrayContaining([
    expect.objectContaining({
      position: '3B', outs: 9, po: 3, a: 2, e: 0, dp: 0, pb: 0, catcherSb: 0, catcherCs: 0
    }),
    expect.objectContaining({ position: 'P', outs: 3, po: 0 })
  ]));
  // 未改动的球员不带 parts
  expect(saved.fieldingPlayerParts.filter((item) => Number(item.playerId) !== 21)).toEqual([]);
  // 球员级合计已按逐位置重算后随 stats 提交
  const statPart = saved.stats.find((row) => Number(row.playerId) === 21 && Number(row.teamId) === AWAY_ID);
  expect(Number(statPart.po)).toBe(3);
});

test('无逐位置行的旧球员：合计可编辑，保存不含 parts 走后端补行', async ({ page }) => {
  let saved = null;
  // 刘洋（26）没有逐位置行：沿用球员级守备合计，编辑后由后端差额回写补行
  await mockApis(page, buildFixture({ legacyPlayerId: 26 }), (body) => { saved = body; });
  const table = await openFieldingTab(page);
  await expect(table).toBeVisible();

  const legacyRow = table.locator('tbody tr', { hasText: '刘洋' }).first();
  await expect(legacyRow).toBeVisible();
  await expect(legacyRow.locator('.game-fielding-table__expander')).toHaveCount(0);

  const poInput = legacyRow.locator('td').nth(4).locator('input');
  await expect(poInput).toBeEditable();
  await expect(poInput).toHaveValue('1');
  await poInput.fill('5');
  await poInput.blur();
  await expect(poInput).toHaveValue('5');

  await page.getByRole('button', { name: '保存比赛数据' }).click();
  await expect.poll(() => saved).toBeTruthy();

  // 未编辑任何逐位置行：parts 为空，旧球员由后端按差额回写
  expect(saved.fieldingPlayerParts).toEqual([]);
  const statPart = saved.stats.find((row) => Number(row.playerId) === 26 && Number(row.teamId) === AWAY_ID);
  expect(Number(statPart.po)).toBe(5);
});

test('非 final：子行输入只读并提示比赛结束后才能编辑', async ({ page }) => {
  await mockApis(page, buildFixture({ status: 'live' }));
  const table = await openFieldingTab(page);
  await expect(table).toBeVisible();

  const playerRow = table.locator('tbody tr', { hasText: '王译圣' }).first();
  await expect(playerRow).toBeVisible();
  await playerRow.locator('.game-fielding-table__expander').click();

  const subRows = table.locator('tbody tr.row-position');
  await expect(subRows).toHaveCount(2);
  await expect(subRows.first().locator('.el-input-number input').first()).toBeDisabled();

  await expect(table.locator('.game-fielding-table__hint'))
    .toHaveText('比赛结束后才能编辑逐位置守备');
  await expect(playerRow.getByRole('button', { name: '添加守位' })).toHaveCount(0);
  await expect(table.getByRole('button', { name: '删除守位' })).toHaveCount(0);
});
