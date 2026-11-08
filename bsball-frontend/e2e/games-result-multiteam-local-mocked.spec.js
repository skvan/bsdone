import { test, expect } from '@playwright/test';

// 回归：#结果录入 多队球员（档案主队≠本场参赛队）保存时 teamId 必须取本场侧别。
// 场景取自生产比赛 268：球员 84 兼属球队 4（档案主队）与球队 1（本场客队），
// 客队统计行 teamId=1；旧逻辑取 player.teamId=4，触发秩序册校验 400。

const EVENT_ID = 18;
const GAME_ID = 268;
const HOME_TEAM_ID = 2;
const AWAY_TEAM_ID = 1;
const MULTI_TEAM_PLAYER_ID = 84;
const MULTI_TEAM_STAT_ID = 5741;

const BATTER_POSITIONS = ['C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'DH'];

function makePlayers(teamId, indexOffset) {
  const batters = BATTER_POSITIONS.map((position, index) => {
    const id = teamId === AWAY_TEAM_ID && index === 0
      ? MULTI_TEAM_PLAYER_ID
      : indexOffset + index + 1;
    return {
      id,
      name: `T${teamId}P${index + 1}`,
      number: String(index + 1),
      // 生产数据：球员 84 的档案主队是 4，但仍属于本场客队 1
      teamId: id === MULTI_TEAM_PLAYER_ID ? 4 : teamId,
      positions: [position],
      batHand: 'R',
      throwHand: 'R',
      status: 'active'
    };
  });
  const pitcher = {
    id: indexOffset + 100,
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
      id: player.id === MULTI_TEAM_PLAYER_ID ? MULTI_TEAM_STAT_ID : statIdOffset + index,
      gameId: GAME_ID,
      teamId,
      playerId: player.id,
      isPitcher: isPitcher ? 1 : 0,
      battingOrder: isPitcher ? null : batterIndex + 1,
      listOrder: index,
      pitcherOrder: isPitcher ? 1 : null,
      position: player.positions[0],
      number: player.number,
      batHand: player.batHand,
      throwHand: player.throwHand,
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

test('多队球员保存比赛结果时，统计行 teamId 取本场侧别而非档案主队', async ({ page }) => {
  const homePlayers = makePlayers(HOME_TEAM_ID, 2000);
  const awayPlayers = makePlayers(AWAY_TEAM_ID, 1000);
  const stats = [
    ...makeStats(HOME_TEAM_ID, homePlayers, 3000),
    ...makeStats(AWAY_TEAM_ID, awayPlayers, 4000)
  ];
  const game = {
    id: GAME_ID,
    eventId: EVENT_ID,
    homeTeamId: HOME_TEAM_ID,
    awayTeamId: AWAY_TEAM_ID,
    gameMode: 'BASEBALL',
    status: 'final',
    gameTime: '2026-11-01 15:29:00',
    gameday: '2026-11-01',
    gameNumber: 4,
    totalInnings: 9,
    homeScore: 0,
    awayScore: 0
  };
  const teams = [
    { id: AWAY_TEAM_ID, name: '暴力羊' },
    { id: HOME_TEAM_ID, name: '广州不死鸟' }
  ];

  await page.addInitScript(() => {
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });

  let savedBody = null;
  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = null;

    if (path === `/event/${EVENT_ID}`) {
      data = { id: EVENT_ID, name: '2026花城联盟冬季赛', gameMode: 'BASEBALL', season: '2026冬' };
    } else if (path === '/team/select-options' || path === `/event/${EVENT_ID}/game-team-options`) {
      data = teams;
    } else if (path === '/game/list') {
      data = { list: [game], total: 1 };
    } else if (path === `/game/${GAME_ID}`) {
      data = game;
    } else if (path === `/game/${GAME_ID}/stats`) {
      data = { list: stats, total: stats.length };
    } else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId'));
      const list = teamId === AWAY_TEAM_ID ? awayPlayers : homePlayers;
      data = { list, total: list.length };
    } else if (path === '/stadium/list') {
      data = { list: [], total: 0 };
    } else if (path === `/game/${GAME_ID}/save-result`) {
      savedBody = request.postDataJSON();
      data = null;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, msg: 'ok', data })
    });
  });

  await page.goto(`/bs-ball/bs-ball/admin/events/${EVENT_ID}/games/${GAME_ID}/edit`);
  await expect(page.getByRole('button', { name: '保存比赛数据' })).toBeVisible();
  await page.getByRole('button', { name: '保存比赛数据' }).click();

  await expect.poll(() => savedBody).toBeTruthy();
  const part = savedBody.stats.find((row) => row.playerId === MULTI_TEAM_PLAYER_ID);
  expect(part).toBeTruthy();
  expect(part.id).toBe(MULTI_TEAM_STAT_ID);
  expect(part.teamId).toBe(AWAY_TEAM_ID);

  // 其余三侧行仍需复用既有 stat id 并保持本侧 teamId
  const homeBatter = savedBody.stats.find((row) => row.playerId === 2001);
  expect(homeBatter.id).toBe(3000);
  expect(homeBatter.teamId).toBe(HOME_TEAM_ID);
  const awayPitcher = savedBody.stats.find((row) => row.playerId === 1100);
  expect(awayPitcher.id).toBe(4009);
  expect(awayPitcher.teamId).toBe(AWAY_TEAM_ID);
});
