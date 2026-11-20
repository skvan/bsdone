import { test, expect } from '@playwright/test';

// 强制换局的收尾必须和正常三出局一样「定案」这个半局（halfComplete=true），而且要在翻局之前
// 送出去：局中每个打席的推送都是预览、后端不落库，强制结束的半局若没有定案推送，库里就是
// 「记分栏有分、判定层零笔」（线上 game 261 的形态），之后每个半局的定案都会撞 409。
// 这里跑的是真按钮链路（局面纠偏 → 强制换局 → 确定换局），补上静态文字护栏证明不了的那一段。

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
  pitching: { ip: 0, np: 0, pitchH: 0, er: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0, wp: 0 },
  fielding: { po: 0, a: 0, e: 0 }
});
const lineup = (teamId) => players
  .filter((player) => player.teamId === teamId)
  .map((player, index) => ({ ...player, stats: zeroStats(), battingOrder: index + 1, listOrder: index }));
const stats = players.map((player, index) => ({
  teamId: player.teamId,
  playerId: player.id,
  position: player.position,
  number: player.number,
  battingOrder: index % positions.length + 1,
  listOrder: index,
  ...zeroStats()
}));

const hitEvent = (sequence) => ({
  id: `ab-1-top-${sequence}-e2e`,
  inning: 1,
  half: 'top',
  sequence,
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
  scoringRunnerIds: [],
  scoringRunners: [],
  recordedAt: Date.now() - 2000
});

/**
 * 云端现场快照：1 局上第 1 棒一垒安打（无得分）。
 * @param complete playEventsComplete 证书；false 时前端把本地记录当成不可信（#712 的缺口）
 * @param ledger 是否留一笔未落地的得分台账（走 flushPendingRunsToPlayEvent 补记那条路）
 */
function snapshot({ complete = true, ledger = false } = {}) {
  const home = lineup(1);
  const away = lineup(2);
  if (ledger) {
    away[0].stats.batting = { ...away[0].stats.batting, ab: 1, r: 1, h: 1 };
    home[0].stats.pitching = { ...home[0].stats.pitching, pitchR: 1, er: 1 };
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
    teamsData: ledger
      ? { away: { score: 1, innings: [1] }, home: { score: 0, innings: [0] } }
      : { away: { score: 0, innings: [0] }, home: { score: 0, innings: [0] } },
    eventLog: [],
    atBatSummaries: [],
    playEventsComplete: complete,
    playEvents: ledger
      ? [hitEvent(1)]
      : [hitEvent(1), { ...hitEvent(2), id: 'ab-1-top-2-e2e', batterPlayerId: 202, batterName: 'T2P2' }],
    runLedger: {
      boundary: 0,
      entries: ledger ? [{
        runs: 1,
        runnerId: 201,
        batterId: 202,
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
      const teamPlayer = player.teamId === 2 ? away.find((p) => p.id === player.id) : home.find((p) => p.id === player.id);
      return [String(player.id), {
        batting: teamPlayer?.stats.batting ?? row.batting,
        pitching: teamPlayer?.stats.pitching ?? row.pitching,
        fielding: teamPlayer?.stats.fielding ?? row.fielding
      }];
    })),
    gameState: {
      inning: 1,
      isTop: true,
      outs: 0,
      halfInningOuts: [],
      balls: 0,
      strikes: 0,
      batterIndex: { away: ledger ? 2 : 0, home: 0 },
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

async function installMock(page, { snapshotJson, reconstructBodies, snapshotPosts }) {
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
      gameMode: 'BASEBALL', inning: 1, topBottom: 'top', homeScore: 0, awayScore: 0
    };
    else if (path === '/game/10/stats') data = { list: stats, total: stats.length };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      snapshotPosts.push(request.postDataJSON().snapshotJson);
      data = { snapshotJson };
    } else if (path === '/game/10/earned-runs/reconstruct') {
      reconstructBodies.push(request.postDataJSON());
      data = { pitcherAbsoluteTotals: {} };
    } else if (path === '/game/10/save-live') data = { success: true };
    else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) data = { restricted: false, players: [] };
    else if (path === '/player/list') {
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

/** 局面纠偏弹层里的「强制换局」→ 确认框「确定换局」 */
async function clickForceHalf(page) {
  await page.getByRole('button', { name: '垒况纠偏、得分纠偏' }).click();
  const tile = page.getByRole('button', { name: '强制换局' });
  await expect(tile).toBeVisible();
  await tile.click();
  const box = page.locator('.el-message-box');
  await expect(box).toBeVisible();
  await box.getByRole('button', { name: '确定换局' }).click();
}

/**
 * 取指定半局的那一发定案推送。强制换局之后紧跟着的自动保存会再对账「换局后的新局」，
 * 所以不能拿最后一发当被测对象（实测会拿到 1 bottom 的空对账）。
 */
async function lastSettleFor(page, bodies, inning, half) {
  await expect
    .poll(() => bodies.filter((body) => body.inning === inning && body.half === half).length)
    .toBeGreaterThan(0);
  await page.waitForTimeout(300);
  return bodies.filter((body) => body.inning === inning && body.half === half).at(-1);
}

test('强制换局发出半局定案推送（halfComplete=true），且用的是翻局前那个半局', async ({ page }) => {
  const reconstructBodies = [];
  const snapshotPosts = [];
  await installMock(page, {
    snapshotJson: JSON.stringify(snapshot()),
    reconstructBodies,
    snapshotPosts
  });

  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '保存比赛' })).toBeVisible();

  await clickForceHalf(page);

  // 强制换局自己的定案推送是第一发；后面还会有一发「新一局」（1 bottom、plays 空）来自
  // 正常的 autoSaveHalfInning 对账，那是现行行为，不是本测试要钉的东西。
  await expect.poll(() => reconstructBodies.length).toBeGreaterThan(0);
  const body = await lastSettleFor(page, reconstructBodies, 1, 'top');
  expect(body.inning).toBe(1);
  // 'top' 而不是 'bottom'：定案要在 Qa(!0) 换局之前定半局，否则这个半局永远不会被重建
  expect(body.half).toBe('top');
  // 缺这一位就是局中预览：后端只算不写，该半局在库里留零笔判定
  expect(body.halfComplete).toBe(true);
  expect(body.plays.map((play) => play.sequence)).toEqual([1, 2]);

  // 定案走 reconstruct，快照走另一条端点；换局后的自动存档也不能把逐棒记录丢短。
  // 拍板后服务端已不再对条数设限（#713 放行），现场页自己没丢就是最后一道，所以钉在这里
  await expect.poll(() => snapshotPosts.length).toBeGreaterThan(0);
  const posted = JSON.parse(snapshotPosts.at(-1));
  expect(posted.playEvents.length).toBe(2);
  expect(posted.playEventsComplete, '现场页存快照时必须带上可信度证书，否则重进就落回 #712 的不可信').toBe(true);
});

test('台账里有未落地得分时，强制换局先补记再定案（#449 同一出口）', async ({ page }) => {
  const reconstructBodies = [];
  const snapshotPosts = [];
  await installMock(page, {
    snapshotJson: JSON.stringify(snapshot({ ledger: true })),
    reconstructBodies,
    snapshotPosts
  });

  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '保存比赛' })).toBeVisible();

  await clickForceHalf(page);

  const body = await lastSettleFor(page, reconstructBodies, 1, 'top');
  expect(body.half).toBe('top');
  expect(body.halfComplete).toBe(true);
  // 补记的那条独立 playEvent 也要进同一次定案推送，不然那一分在判定层不存在
  expect(body.plays.map((play) => play.sequence)).toEqual([1, 2]);
  expect(body.plays[1].scoringRunners).toEqual([
    expect.objectContaining({ runnerId: 201, responsiblePitcherId: 101 })
  ]);

  // 台账补记的那一条同时存在于两条通道：定案推送几条，快照里就得是几条逐棒记录
  // （条数那道守门已改成放行，两边对不上不会有人拦，只能靠这一例钉）
  await expect.poll(() => snapshotPosts.length).toBeGreaterThan(0);
  expect(JSON.parse(snapshotPosts.at(-1)).playEvents).toHaveLength(body.plays.length);
});

test('本地记录不可信（缺证书）时强制换局无法定案：当场提示而不是静默跳过（#712）', async ({ page }) => {
  const reconstructBodies = [];
  const snapshotPosts = [];
  await installMock(page, {
    snapshotJson: JSON.stringify(snapshot({ complete: false })),
    reconstructBodies,
    snapshotPosts
  });

  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '保存比赛' })).toBeVisible();

  await clickForceHalf(page);
  await expect(page.getByText('半局定案没有执行')).toBeVisible();
  // 缺口本身（本轮只让它可见，不改变行为）：没有可信证书就不送重建
  await page.waitForTimeout(500);
  expect(reconstructBodies).toHaveLength(0);
  // 证书不在场就得推 false（不是推 true 后期待别人担着）：重进时靠这一位判定可不可信
  await expect.poll(() => snapshotPosts.length).toBeGreaterThan(0);
  expect(JSON.parse(snapshotPosts.at(-1)).playEventsComplete).toBe(false);
});
