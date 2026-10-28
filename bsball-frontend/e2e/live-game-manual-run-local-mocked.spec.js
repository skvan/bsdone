import { test, expect } from '@playwright/test';

// #449：现场快照恢复 playEvents + 未落定台账；保存时补记 MANUAL_RUN 并同步 ER 引擎
const positions = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'];
const players = [1, 2].flatMap((teamId) => positions.map((position, index) => ({
  id: teamId * 100 + index + 1,
  name: `T${teamId}P${index + 1}`,
  number: String(index + 1),
  teamId,
  position,
  positions: [position]
})));
const zeroStats = () => ({
  batting: { ab: 0, r: 0, h: 0, rbi: 0, bb: 0, hbp: 0, so: 0, sf: 0, sh: 0, sb: 0, cs: 0 },
  pitching: { ip: 0, np: 0, pitchH: 0, er: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0 },
  fielding: { po: 0, a: 0, e: 0 }
});
const lineup = (teamId) => players
  .filter((player) => player.teamId === teamId)
  .map((player, index) => ({
    ...player,
    stats: zeroStats(),
    battingOrder: index + 1,
    listOrder: index
  }));
const stats = players.map((player, index) => ({
  teamId: player.teamId,
  playerId: player.id,
  position: player.position,
  number: player.number,
  battingOrder: index % positions.length + 1,
  listOrder: index,
  ...zeroStats()
}));

function snapshot({ pendingRun = true } = {}) {
  const away = lineup(2);
  const home = lineup(1);
  if (pendingRun) {
    away[0].stats.batting = { ...away[0].stats.batting, ab: 1, r: 1, h: 1 };
    away[1].stats.batting = { ...away[1].stats.batting, r: 1 };
    home[0].stats.pitching = { ...home[0].stats.pitching, pitchR: 2, er: 2 };
  }
  return {
    v: 1,
    savedAt: Date.now() - 1000,
    gameMode: 'BASEBALL',
    setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '' },
    gameId: 10,
    gameStarted: true,
    homeLineup: home,
    awayLineup: away,
    homeBench: [],
    awayBench: [],
    awayFieldingPitcherId: 201,
    homeFieldingPitcherId: 101,
    awayFieldingPitcherRow: null,
    homeFieldingPitcherRow: null,
    awayLastDhAddedFromPoolId: null,
    homeLastDhAddedFromPoolId: null,
    awayUnavailablePlayerIds: [],
    homeUnavailablePlayerIds: [],
    awayLineupStarterIds: away.map((player) => player.id),
    homeLineupStarterIds: home.map((player) => player.id),
    awaySlotSubHistory: Array.from({ length: 12 }, () => []),
    homeSlotSubHistory: Array.from({ length: 12 }, () => []),
    teamsData: pendingRun ? {
      away: { score: 2, innings: [2] },
      home: { score: 0, innings: [0] }
    } : {
      away: { score: 0, innings: [0] },
      home: { score: 0, innings: [0] }
    },
    eventLog: [],
    atBatSummaries: [],
    playEventsComplete: true,
    playEvents: pendingRun ? [{
      id: 'ab-1-top-1-snapshot',
      inning: 1,
      half: 'top',
      sequence: 1,
      batterPlayerId: 201,
      batterName: 'T2P1',
      pitcherPlayerId: 101,
      pitcherName: 'T1P1',
      resultCode: '1B',
      resultText: 'T2P1: 一垒安打',
      outsBefore: 0,
      outsAfter: 0,
      basesBefore: { 1: null, 2: null, 3: null },
      basesAfter: { 1: null, 2: null, 3: null },
      scoringRunnerIds: [201],
      scoringRunners: [{ runnerId: 201, responsiblePitcherId: 101, origin: 'NORMAL' }],
      recordedAt: Date.now() - 2000
    }] : [],
    runLedger: {
      boundary: 0,
      entries: pendingRun ? [{
        runs: 1,
        runnerId: 202,
        batterId: 201,
        pitcherId: 101,
        responsiblePitcherId: 101,
        origin: 'NORMAL',
        runCause: 'BATTER_ACTION',
        rbiDelta: 1,
        rbiCredit: true,
        rbiReason: 'BATTER_ACTION'
      }] : []
    },
    playerStatsById: Object.fromEntries(players.map((player) => {
      const row = stats.find((stat) => stat.playerId === player.id);
      const awayPlayer = player.teamId === 2 ? away.find((p) => p.id === player.id) : null;
      const homePlayer = player.teamId === 1 ? home.find((p) => p.id === player.id) : null;
      return [String(player.id), {
        batting: (awayPlayer ?? homePlayer)?.stats.batting ?? row.batting,
        pitching: (homePlayer ?? awayPlayer)?.stats.pitching ?? row.pitching,
        fielding: (awayPlayer ?? homePlayer)?.stats.fielding ?? row.fielding
      }];
    })),
    gameState: {
      inning: 1,
      isTop: true,
      outs: 0,
      halfInningOuts: [],
      balls: 0,
      strikes: 0,
      batterIndex: { away: pendingRun ? 2 : 0, home: 0 },
      runners: { 1: null, 2: null, 3: null },
      flow: null,
      isGameEnd: false,
      hasPitchThisAB: false,
      needNextBatter: false,
      mvp: null,
      svp: null,
      runnerDragCiRunnerId: null,
      playErrorCredited: false,
      playWpCredited: false,
      playPbCredited: false
    }
  };
}

async function installMock(page, { snapshotJson, reconstructBodies, savedLiveBody, engineManaged }) {
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
    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event' };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 }
    ];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 1, topBottom: 'top', homeScore: 0, awayScore: 2,
      engineManaged
    };
    else if (path === '/game/10/stats') data = { list: stats, total: stats.length };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') data = { snapshotJson: '' };
    else if (path === '/game/10/earned-runs/reconstruct') {
      reconstructBodies.push(request.postDataJSON());
      data = {
        pitcherAbsoluteTotals: {
          101: { runs: 2, earnedRuns: 2, unearnedRuns: 0, pendingRuns: 0 }
        }
      };
    } else if (path === '/game/10/save-live') {
      savedLiveBody.push(request.postDataJSON());
      data = { success: true };
    } else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) data = { restricted: false, players: [] };
    else if (path === '/player/list' || path === '/api/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 1);
      const list = players.filter((player) => player.teamId === teamId);
      data = { list, total: list.length };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (/^\/team\/[12]$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === 1 ? 'Home' : 'Away' };
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

test('现场恢复后保存：先补记未落定得分再对账当前半局（#449）', async ({ page }) => {
  const reconstructBodies = [];
  const savedLiveBody = [];
  await installMock(page, {
    snapshotJson: JSON.stringify(snapshot()),
    reconstructBodies,
    savedLiveBody,
    engineManaged: true
  });

  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '保存比赛' })).toBeVisible();

  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => reconstructBodies.length).toBeGreaterThan(0);

  const body = reconstructBodies.at(-1);
  expect(body.inning).toBe(1);
  expect(body.half).toBe('top');
  expect(body.plays.map((play) => play.sequence)).toEqual([1, 2]);
  expect(body.plays[1].scoringRunners).toEqual([
    expect.objectContaining({ runnerId: 202, responsiblePitcherId: 101, origin: 'NORMAL' })
  ]);

  await expect.poll(() => savedLiveBody.length).toBeGreaterThan(0);
  expect(savedLiveBody.at(-1).game.awayScore).toBe(2);
  expect(savedLiveBody.at(-1).game.homeScore).toBe(0);
});

test('撤销掉半局全部事件后保存：对账请求允许空 plays，清掉后端残留判定（#449）', async ({ page }) => {
  const reconstructBodies = [];
  const savedLiveBody = [];
  await installMock(page, {
    snapshotJson: JSON.stringify(snapshot({ pendingRun: false })),
    reconstructBodies,
    savedLiveBody,
    engineManaged: true
  });

  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '保存比赛' })).toBeVisible();

  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => reconstructBodies.length).toBeGreaterThan(0);

  const body = reconstructBodies.at(-1);
  expect(body.inning).toBe(1);
  expect(body.half).toBe('top');
  expect(body.plays).toEqual([]);

  await expect.poll(() => savedLiveBody.length).toBeGreaterThan(0);
  expect(savedLiveBody.at(-1).game.awayScore).toBe(0);
});
