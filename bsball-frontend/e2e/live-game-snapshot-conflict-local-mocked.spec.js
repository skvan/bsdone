import { test, expect } from '@playwright/test';

// #713（可见性那一半）：现场快照被服务器明确拒收时，记录员必须看见「为什么被拒、该怎么做」。
//
// 修法前的形态（读码＋本测试实测）：pushLiveSnapshot 对所有失败一律重试一次再放弃，调用方
// 只按真假弹一句「现场快照未能同步到服务器……换人在场状态会丢失」。快照覆盖守门的 409 就这么
// 被吞成一句无关的提醒：存档根本没进云端，人却以为只是丢了换人在场状态，于是继续录下去
// （game 261 那场的证据就是在「以为已经存进去了」的状态下被覆盖掉的）。
//
// 同时钉住反向：可被重试救回来的失败（网络/5xx，以及 RateLimitFilter 对这个端点真会回的
// 429「请稍后再试」）仍走原文案与原重试，别把原来的兜底一起砍掉——所以只有 409 当拒收。
//
// 第三条钉的是 #713 拍板后的另一面：条数变短不再拒收（撤销过的本地状态得存得进去），
// 得分落后于记分栏那道仍拒。下面两个拒收/放行用例跑的是同一个 makeServerGuard——
// 它复刻的是后端现行规则，不是手写应答，所以 mock 不会偷偷比线上宽。

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

/**
 * 开局现场：1 局上、无人出局、指定条数的逐棒记录。
 * certificate=false 用来复刻「服务器没给证书」的重进形态（#712 校验的输入）。
 */
function snapshot({ events = 1, certificate = true } = {}) {
  const home = lineup(1);
  const away = lineup(2);
  const plays = Array.from({ length: events }, (_, index) => ({
    id: `ab-1-top-${index + 1}-e2e`,
    inning: 1,
    half: 'top',
    sequence: index + 1,
    batterPlayerId: 201 + index,
    batterName: `T2P${index + 1}`,
    pitcherPlayerId: 101,
    pitcherName: 'T1P1',
    resultCode: '1B',
    resultText: `T2P${index + 1}: 一垒安打`,
    outsBefore: 0,
    outsAfter: 0,
    basesBefore: { 1: null, 2: null, 3: null },
    basesAfter: { 1: null, 2: null, 3: null },
    scoringRunnerIds: [],
    scoringRunners: [],
    recordedAt: Date.now() - 2000
  }));
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
    teamsData: { away: { score: 0, innings: [0] }, home: { score: 0, innings: [0] } },
    eventLog: [],
    atBatSummaries: [],
    playEventsComplete: certificate,
    playEvents: plays,
    runLedger: { boundary: 0, entries: [] },
    playerStatsById: Object.fromEntries(players.map((player) => [String(player.id), zeroStats()])),
    gameState: {
      inning: 1,
      isTop: true,
      outs: 0,
      halfInningOuts: [],
      balls: 0,
      strikes: 0,
      batterIndex: { away: events, home: 0 },
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

/** 一份现场快照正文里的 playEvents 条数 */
function eventCount(snapshotJson) {
  try {
    return JSON.parse(snapshotJson).playEvents?.length ?? 0;
  }
  catch {
    return 0;
  }
}

/**
 * 后端快照守门的复刻（#713）：得分落后于记分栏 409；任何变短（含清零）都必须带撤销/重做意图
 * （allowEvidenceShrink）才放行，否则 409。记分栏必须与 /game/{id} 的响应同源（同一个 official），
 * 否则 mock 会跟线上相反（比如把该拒的那一发静默放行）。
 */
function makeServerGuard({ official, state }) {
  const officialRuns = official.home + official.away;
  return (json, allowEvidenceShrink) => {
    const body = JSON.parse(json);
    const claimed = (body.teamsData?.away?.score ?? 0) + (body.teamsData?.home?.score ?? 0);
    if (officialRuns > 0 && claimed < officialRuns) {
      return { msg: `本场比赛已有逐棒记录，提交的快照得分只到 ${claimed} 分，低于记分 ${officialRuns} 分，已拒绝覆盖` };
    }
    const incoming = body.playEvents?.length;
    if (incoming != null && incoming < state.events && !allowEvidenceShrink) {
      return {
        msg: `本场比赛云端已有 ${state.events} 条逐棒记录，提交的快照只有 ${incoming} 条，且未带撤销/重做意图，已拒绝覆盖以免证据被截短`
      };
    }
    state.events = incoming ?? state.events;
    return {};
  };
}

/**
 * @param snapshotPosts 收集每次快照 POST 的正文（含被拒的）
 * @param replyFor 快照 POST 的应答：返回 {status} 走 HTTP 层，返回 {msg} 走 Result 包体的
 *                 code 409（后端守门就是这个形态：HTTP 200 + body.code 409），返回 {} 放行
 *                 注：这里只模拟「payload 自带 playEvents」的现场页形态；阵容板那种不带
 *                 playEvents 的 payload 在真后端是「不参与比对、并回现有证据」（靠
 *                 mentionsEvidence/mentionsScore 分流），本 spec 不交叉那一块（由后端测试钉）
 */
async function installMock(page, { snapshotPosts, replyFor, restoredEvents = 1, official = { home: 0, away: 0 }, certificate = true, playKeys = [], reconstructBodies = [] }) {
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
    if (path === '/game/10/earned-runs/reconstruct' && request.method() === 'POST') {
      try {
        reconstructBodies.push(request.postDataJSON());
      } catch {
        reconstructBodies.push(null);
      }
    }
    if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      const requestBody = request.postDataJSON();
      const json = requestBody.snapshotJson;
      snapshotPosts.push(json);
      const reply = replyFor(json, requestBody.allowEvidenceShrink === true) ?? {};
      if (reply.status) {
        await route.fulfill({ status: reply.status, contentType: 'text/plain', body: 'gateway down' });
        return;
      }
      if (reply.msg) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 409, msg: reply.msg })
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { snapshotJson: json } })
      });
      return;
    }
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event' };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 }
    ];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 1, topBottom: 'top', homeScore: official.home, awayScore: official.away
    };
    else if (path === '/game/10/stats') data = { list: stats, total: stats.length };
    else if (path === '/game/10/live-snapshot') {
      data = { snapshotJson: JSON.stringify(snapshot({ events: restoredEvents, certificate })) };
    } else if (path === '/game/10/earned-runs/play-keys') data = { keys: playKeys };
    else if (path === '/game/10/earned-runs/reconstruct') data = { pitcherAbsoluteTotals: {} };
    else if (path === '/game/10/save-live') data = { success: true };
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

const saveButton = (page) => page.getByRole('button', { name: '保存比赛' });

async function openLive(page) {
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(saveButton(page)).toBeVisible();
}

test('快照被服务器 409 拒收：显示服务器给的原因，并且不做无意义重试', async ({ page }) => {
  const snapshotPosts = [];
  // 记分栏 18:6、本地现场停在开局 0 分（fixture 里的 teamsData 就是 0/0）：那一发存档发的就是
  // 后端现行唯一还会拒的形状，拒收文案由 makeServerGuard 算出来，不是手写的
  const official = { home: 6, away: 18 };
  await installMock(page, {
    snapshotPosts,
    official,
    replyFor: makeServerGuard({ official, state: { events: 1 } })
  });
  await openLive(page);

  await saveButton(page).click();

  // 服务器那句话要原样到人眼里（它带着可执行的下一步）
  await expect(page.getByText('低于记分 24 分，已拒绝覆盖')).toBeVisible();
  // 不能再被通用文案顶掉：那句讲的是「换人在场状态会丢失」，与本次被拒收无关
  // （拖过重试窗口再断：否则「当时为 0」不能排除“晚一步又被通用文案顶替”）

  // 409 重试不会改变结果，只会白花 800ms：等过重试窗口后仍应只有 1 次
  await page.waitForTimeout(1500);
  expect(snapshotPosts).toHaveLength(1);
  await expect(page.getByText('现场快照未能同步到服务器')).toHaveCount(0);
});

test('快照 POST 撞网络/5xx：仍按原设计重试一次，并走通用提示文案', async ({ page }) => {
  const snapshotPosts = [];
  await installMock(page, { snapshotPosts, replyFor: () => ({ status: 500 }) });
  await openLive(page);

  await saveButton(page).click();

  // 5xx 是可能被重试救回来的，这一路不能跟着 4xx 一起砍掉
  await expect.poll(() => snapshotPosts.length).toBeGreaterThanOrEqual(2);
  await expect(page.getByText('现场快照未能同步到服务器')).toBeVisible();
  expect(snapshotPosts.length).toBeLessThanOrEqual(2);
});

test('本地短档未带撤销意图：服务器 409 拒收并显示原因（#713）', async ({ page }) => {
  const snapshotPosts = [];
  const official = { home: 0, away: 0 };
  // 旧设备/陈旧标签页形状：云端 3 条，本地恢复出来的现场只有 1 条，普通保存不带撤销意图
  const state = { events: 3 };
  await installMock(page, {
    snapshotPosts,
    restoredEvents: 1,
    official,
    replyFor: makeServerGuard({ official, state })
  });
  await openLive(page);

  await saveButton(page).click();

  await expect(page.getByText('提交的快照只有 1 条，且未带撤销/重做意图')).toBeVisible();
  // 4xx 不重试：等过重试窗口后仍应只有 1 次，云端条数不变
  await page.waitForTimeout(1500);
  expect(snapshotPosts).toHaveLength(1);
  expect(state.events).toBe(3);
});

test('撤销把逐棒条数推回更低：带意图保存放行、云端跟着变短（#713）', async ({ page }) => {
  // 这一例不预设「撤销会不会把条数推低」（本轮之前只能靠读码推断），而是真的录一拍再撤销，
  // 看推上来的 payload 剩几条。mock 跑的是后端现行规则（makeServerGuard）：无意图变短拒收，
  // 撤销/重做意图下放行。本例记分栏与本地都是 0 分，撞不到得分那道。
  const posted = [];
  const official = { home: 0, away: 0 };
  const state = { events: 2 };
  await installMock(page, {
    snapshotPosts: posted,
    restoredEvents: 2,
    official,
    replyFor: makeServerGuard({ official, state })
  });
  await openLive(page);

  // 录一个三振（只有 1 出局，不会触发攻守交换弹框）：本地逐棒记录 2 → 3 条
  const strike = page.locator('button[title="挥空（计好球）Swing & Miss"]');
  const flowBar = page.locator('.les-flow-card');
  await strike.click();
  await strike.click();
  await strike.click();
  await expect(flowBar).toBeVisible();
  await page.waitForTimeout(350);
  await flowBar.getByRole('button', { name: '挥空三振' }).click();
  await expect(flowBar).toBeHidden();

  // 先存一次，让服务器现状追到 3 条
  await saveButton(page).click();
  await expect.poll(() => Math.max(...posted.map(eventCount))).toBe(3);
  expect(state.events).toBe(3);

  // 撤销那一拍：本地记录回到 2 条，比服务器现有的 3 条短。
  // 「撤销会不会把条数推低」本轮之前只是读码推断，现在用这一例坐实了：撤销后的保存
  // 确实推上较短的那一份（把 ns() 里恢复 playEvents 的那句改成 no-op，本例就转红）。
  await page.getByRole('button', { name: '撤销' }).first().click();
  await page.waitForTimeout(300);
  await saveButton(page).click();

  // 前提坐实：推上来的确实是被算短的那一份
  await expect.poll(() => eventCount(posted.at(-1)), { message: '撤销后保存推上来的条数' }).toBe(2);
  // #713 拍板：条数变短放行。云端跟着回到 2 条，而不是把这场锁死在「只能放弃本地、
  // 从云端恢复」——那等于让撤销这一步变成不可保存
  await expect.poll(() => state.events).toBe(2);
  // 拖过重试窗口再断两句反向：否则「当时为 0」不能排除“晚一步又弹出提示”
  await page.waitForTimeout(1500);
  await expect(page.getByText('现场快照未能同步到服务器')).toHaveCount(0);
  await expect(page.getByText('已拒绝覆盖')).toHaveCount(0);
});

test('#712 重进：本地快照覆盖云端定案键 → 恢复信任，保存写回 playEventsComplete=true', async ({ page }) => {
  const snapshotPosts = [];
  const official = { home: 0, away: 0 };
  await installMock(page, {
    snapshotPosts,
    official,
    restoredEvents: 2,
    certificate: false,
    // 后端 sourceEventId = toEarnedRunPlay.playId（1 局上 seq 1/2 → 1000001/1000002）
    playKeys: [
      { inning: 1, half: 'top', sequence: 1, sourceEventId: '1000001' },
      { inning: 1, half: 'top', sequence: 2, sourceEventId: '1000002' }
    ],
    replyFor: () => ({})
  });
  await openLive(page);

  // 校验通过会播一条 info；等它出现再保存，避免抢在 play-keys 响应之前
  await expect(page.getByText('已按云端定案记录校验本地快照')).toBeVisible();
  await saveButton(page).click();

  await expect.poll(() => {
    const last = snapshotPosts.at(-1);
    return last ? JSON.parse(last).playEventsComplete === true : null;
  }).toBe(true);
});

test('#712 重进：本地缺云端定案键 → 列出缺口，可按当前半局局部定案（confirmPartialRebuild）', async ({ page }) => {
  const snapshotPosts = [];
  const reconstructBodies = [];
  const official = { home: 0, away: 0 };
  await installMock(page, {
    snapshotPosts,
    official,
    restoredEvents: 2,
    certificate: false,
    playKeys: [
      { inning: 1, half: 'top', sequence: 1, sourceEventId: '1000001' },
      { inning: 1, half: 'top', sequence: 2, sourceEventId: '1000002' },
      { inning: 1, half: 'top', sequence: 3, sourceEventId: '1000003' }
    ],
    reconstructBodies,
    replyFor: () => ({})
  });
  await openLive(page);

  await expect(page.getByText('本地快照与云端定案不一致')).toBeVisible();
  await expect(page.getByText('第 1 局上，缺 1 条')).toBeVisible();

  await page.getByRole('button', { name: '按本地记录局部定案' }).click();
  await expect(page.getByText('将以本地不完整记录重建当前半局')).toBeVisible();
  await page.getByRole('button', { name: '继续定案' }).click();

  await expect.poll(() => reconstructBodies.some((body) => body?.confirmPartialRebuild === true)).toBe(true);
  await expect(page.getByText('已按本地记录定案当前半局')).toBeVisible();
});

test('#712 重进缺口：选「从云端恢复录入现场」→ 重新拉取快照，且不再弹第二个缺口', async ({ page }) => {
  const snapshotPosts = [];
  const official = { home: 0, away: 0 };
  await installMock(page, {
    snapshotPosts,
    official,
    restoredEvents: 2,
    certificate: false,
    playKeys: [
      { inning: 1, half: 'top', sequence: 1, sourceEventId: '1000001' },
      { inning: 1, half: 'top', sequence: 2, sourceEventId: '1000002' },
      { inning: 1, half: 'top', sequence: 3, sourceEventId: '1000003' }
    ],
    replyFor: () => ({})
  });
  await openLive(page);

  await expect(page.getByText('本地快照与云端定案不一致')).toBeVisible();
  // 缺口提示的 confirm 走 bc({skipConfirm:true})：等恢复成功提示，坐实重新拉取并还原了快照
  await page.getByRole('button', { name: '从云端恢复录入现场' }).click();
  await expect(page.getByText('已从云端录入快照恢复现场')).toBeVisible();
  // 恢复后校验会再跑一次，但本次入场已经提示过缺口，不能递归再弹
  await expect(page.getByText('本地快照与云端定案不一致')).toHaveCount(0);
  await page.waitForTimeout(500);
  expect(snapshotPosts).toHaveLength(0);
});
