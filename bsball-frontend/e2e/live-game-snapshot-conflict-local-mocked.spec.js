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
 * 后端 messages*.properties 三份词典的逐字转写（占位符保持 {0,number,#} 原形）。
 * 不要手改这一段：tests/snapshotGuardMessageParity.test.mjs 会拿它与后端三份文件逐字比对。
 * mock 说一套、线上说另一套，是这轮最难查的漂移方向。
 */
// SNAPSHOT_GUARD_MESSAGES:BEGIN
const SNAPSHOT_GUARD_MESSAGES = {
  "zh-CN": {
    "snapshot.guard.run.regress": "本场比赛已有逐棒记录，提交的快照得分只到 {0,number,#} 分，低于记分 {1,number,#} 分，已拒绝覆盖以免丢失逐棒记录；用「从云端恢复录入现场」可放弃本地这份回到服务器现状，确需从头重录请先清空比分或另开新场",
    "snapshot.guard.run.regressUnknown": "本场比赛已有逐棒记录，提交的快照读不出有效比分（payload 不可解析或不含 teamsData），低于记分 {0,number,#} 分，已拒绝覆盖以免丢失逐棒记录；用「从云端恢复录入现场」可放弃本地这份回到服务器现状，确需从头重录请先清空比分或另开新场",
    "snapshot.guard.events.wiped": "本场比赛云端已有 {0,number,#} 条逐棒记录，提交的快照里一条都没有，已拒绝覆盖以免证据被清空；若你刚撤销了全部记录，请从云端恢复录入现场后重新操作；确需从头重录请另开新场",
    "snapshot.guard.payload.unreadable": "本场比赛云端已有 {0,number,#} 条逐棒记录，提交的快照无法解析成有效内容，已拒绝覆盖以免证据被清空；请用「从云端恢复录入现场」回到服务器现状",
    "snapshot.guard.events.shrink": "本场比赛云端已有 {0,number,#} 条逐棒记录，提交的快照只有 {1,number,#} 条，且未带撤销/重做意图，已拒绝覆盖以免证据被截短；这份存档不是在本页面撤销/重做后保存的，请用「从云端恢复录入现场」回到服务器现状，或在本页面重新操作后再保存",
    "reconstruct.guard.partialDenominator": "本次重建后全场判定只有 {0,number,#} 笔，低于记分栏的 {1,number,#} 分，说明推上来的逐分资料没有涵盖已记的分数；而重建会拿这个残缺分母绝对重算全部投手的 R/ER，并作废相关投手的人工覆写（线上 261/235/248 就是这样丢数据的）。请先把其余有得分的半局一并重建补齐，或改用投手 R/ER 人工覆写；确认要做局部重建，可在缺口提示里选择局部定案"
  },
  "zh-TW": {
    "snapshot.guard.run.regress": "本場比賽已有逐棒記錄，提交的快照得分只到 {0,number,#} 分，低於記分 {1,number,#} 分，已拒絕覆蓋以免遺失逐棒記錄；用「從雲端恢復錄入現場」可放棄本地這份回到伺服器現況，確需從頭重錄請先清空比分或另開新場",
    "snapshot.guard.run.regressUnknown": "本場比賽已有逐棒記錄，提交的快照讀不出有效比分（payload 不可解析或不含 teamsData），低於記分 {0,number,#} 分，已拒絕覆蓋以免遺失逐棒記錄；用「從雲端恢復錄入現場」可放棄本地這份回到伺服器現況，確需從頭重錄請先清空比分或另開新場",
    "snapshot.guard.events.wiped": "本場比賽雲端已有 {0,number,#} 條逐棒記錄，提交的快照裡一條都沒有，已拒絕覆蓋以免證據被清空；若你剛撤銷了全部記錄，請從雲端恢復錄入現場後重新操作；確需從頭重錄請另開新場",
    "snapshot.guard.payload.unreadable": "本場比賽雲端已有 {0,number,#} 條逐棒記錄，提交的快照無法解析成有效內容，已拒絕覆蓋以免證據被清空；請用「從雲端恢復錄入現場」回到伺服器現況",
    "snapshot.guard.events.shrink": "本場比賽雲端已有 {0,number,#} 條逐棒記錄，提交的快照只有 {1,number,#} 條，且未帶撤銷/重做意圖，已拒絕覆蓋以免證據被截短；這份存檔不是在本頁面撤銷/重做後儲存的，請用「從雲端恢復錄入現場」回到伺服器現況，或在本頁面重新操作後再儲存",
    "reconstruct.guard.partialDenominator": "本次重建後全場判定只有 {0,number,#} 筆，低於記分欄的 {1,number,#} 分，說明推上來的逐分資料沒有涵蓋已記的分數；而重建會拿這個殘缺分母絕對重算全部投手的 R/ER，並作廢相關投手的人工覆寫（線上 261/235/248 就是這樣丟資料的）。請先把其餘有得分的半局一併重建補齊，或改用投手 R/ER 人工覆寫；確認要做局部重建，可在缺口提示裡選擇局部定案"
  },
  "en": {
    "snapshot.guard.run.regress": "This game already has play-by-play records, but the submitted snapshot only accounts for {0,number,#} runs, below the {1,number,#} on the scoreboard. The overwrite was rejected so the records survive. Use \"Restore Session from Cloud\" to drop the local copy and go back to the server state, or clear the score / start a new game if you truly need to re-enter from scratch.",
    "snapshot.guard.run.regressUnknown": "This game already has play-by-play records, but the submitted snapshot carries no readable score (unparsable payload or no teamsData), below the {0,number,#} on the scoreboard. The overwrite was rejected so the records survive. Use \"Restore Session from Cloud\" to drop the local copy, or clear the score / start a new game if you truly need to re-enter from scratch.",
    "snapshot.guard.events.wiped": "The cloud copy of this game holds {0,number,#} play-by-play records, while the submitted snapshot contains none, so the overwrite was rejected to keep the records. If you just undid every record, use \"Restore Session from Cloud\" to get back to the server state and start over; to truly re-enter from scratch, open a new game.",
    "snapshot.guard.payload.unreadable": "The cloud copy of this game holds {0,number,#} play-by-play records, but the submitted snapshot could not be parsed into readable content, so the overwrite was rejected. Use \"Restore Session from Cloud\" to go back to the server state.",
    "snapshot.guard.events.shrink": "This game already has {0,number,#} play-by-play records in the cloud, but the submitted snapshot only contains {1,number,#} and carries no undo/redo intent, so the overwrite was rejected to avoid truncating the records. This snapshot was not saved right after an undo/redo on this page; use \"Restore Session from Cloud\" to go back to the server state, or redo the action on this page and save again.",
    "reconstruct.guard.partialDenominator": "The rebuild would leave only {0,number,#} decisions for the whole game, below the {1,number,#} runs on the scoreboard, which means the submitted play data does not cover the runs already recorded. A rebuild would use this incomplete denominator to recompute every pitcher R/ER and void their manual overrides (this is how games 261/235/248 lost data in production). Rebuild the remaining scoring half-innings first, or use manual pitcher R/ER overrides. For a deliberate partial rebuild, choose the partial-settle option in the gap prompt."
  }
};
// SNAPSHOT_GUARD_MESSAGES:END

/** MessageFormat 的 {n} 与 {n,number,#} 在 JS 侧都只是插值；千分位已由后端模板关掉，两边才可比。 */
function formatGuardMessage(template, args) {
  return template.replace(/\{(\d)(,number,#)?\}/g, (_match, index) => String(args[Number(index)]));
}

/**
 * 后端 UiLocaleResolver 的语言判定在 mock 侧的等价实现：只认 X-UI-Locale，没带就用 WebConfig
 * 声明的默认（简体中文）。真后端在没有这个头时还会退回 Accept-Language，本 mock 不模拟那一层
 * ——前端现在恒带这个头，退不回；少这一层只会让 mock 比线上更保守，不会更宽。
 */
function guardLocale(headers) {
  const raw = String((headers && headers['x-ui-locale']) || '').trim().toLowerCase().replace(/_/g, '-');
  if (!raw) return 'zh-CN';
  if (raw.startsWith('zh')) {
    return raw.includes('tw') || raw.includes('hk') || raw.includes('hant') ? 'zh-TW' : 'zh-CN';
  }
  if (raw === 'en' || raw.startsWith('en-') || raw.startsWith('ko') || raw.startsWith('ja')) return 'en';
  return 'zh-CN';
}

/**
 * 后端现行快照守门的复刻（#713 收紧后：得分倒退；任何变短且未带撤销意图，清零与非清零各一条文案）：
 * 自带证据且总分落后于记分栏才 409；条数变短只有带 allowEvidenceShrink 才放行。记分栏必须与
 * /game/{id} 的响应同源（同一个 official），否则 mock 会跟线上相反（比如把该拒的那一发静默放行）。
 * 文案取自 SNAPSHOT_GUARD_MESSAGES，按请求的 X-UI-Locale 选语言——所以前端哪天忘了带那个头，
 * 英文界面那例会拿到中文而转红，而不是靠测试里手写一句英文。
 * 注：真后端还有一条「payload 解不开」（snapshot_payload_unreadable）与「比分读不出」
 * （run.regressUnknown）两格，画面上走不到（前端 pa() 恒发合法 JSON 且带 teamsData），
 * 那两格由后端测试钉，不往 mock 里塞够不到的分支。
 */
function makeServerGuard({ official, state }) {
  const officialRuns = official.home + official.away;
  return (json, { headers = {}, allowEvidenceShrink = false } = {}) => {
    const messages = SNAPSHOT_GUARD_MESSAGES[guardLocale(headers)];
    const body = JSON.parse(json);
    const claimed = (body.teamsData?.away?.score ?? 0) + (body.teamsData?.home?.score ?? 0);
    if (officialRuns > 0 && claimed < officialRuns) {
      return { msg: formatGuardMessage(messages['snapshot.guard.run.regress'], [claimed, officialRuns]) };
    }
    const incoming = body.playEvents?.length;
    // #713 收紧：任何变短（含清零）都要撤销/重做意图；旧设备/陈旧标签页的短档一律 409。
    // 文案仍从词典取：上游改成写死一句，词典与线上就会各说各话，而 parity 测试只对词典那份有牙。
    if (incoming != null && incoming < state.events && !allowEvidenceShrink) {
      if (incoming === 0) {
        return { msg: formatGuardMessage(messages['snapshot.guard.events.wiped'], [state.events]) };
      }
      return { msg: formatGuardMessage(messages['snapshot.guard.events.shrink'], [state.events, incoming]) };
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
async function installMock(page, { snapshotPosts, snapshotHeaders = null, replyFor, restoredEvents = 1, locale = null, official = { home: 0, away: 0 }, certificate = true, playKeys = [], reconstructBodies = [] }) {
  await page.addInitScript((uiLocale) => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    // 界面语言：不给就按 playwright.config 的 locale=zh-CN 探测（与线上一致的默认）
    if (uiLocale) localStorage.setItem('bs-ball-locale', uiLocale);
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  }, locale);

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
      if (snapshotHeaders) snapshotHeaders.push(request.headers());
      const reply = replyFor(json, {
        headers: request.headers(),
        allowEvidenceShrink: requestBody.allowEvidenceShrink === true
      }) ?? {};
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

// 「保存比赛」这个按钮名本身也在词典里（liveGame.top.save），界面切英文后它就不叫这个名字了。
// 所以按语言取名字，而不是假设中文按钮恒定存在——真实英文界面的用户点的就是另一个名字。
const SAVE_LABEL = { 'zh-CN': '保存比赛', en: 'Save Game' };

const saveButton = (page, locale = 'zh-CN') => page.getByRole('button', { name: SAVE_LABEL[locale] });

async function openLive(page, locale = 'zh-CN') {
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(saveButton(page, locale)).toBeVisible();
}

test('快照被服务器 409 拒收：显示服务器给的原因，并且不做无意义重试', async ({ page }) => {
  const snapshotPosts = [];
  // 记分栏 18:6、本地现场停在开局 0 分（fixture 里的 teamsData 就是 0/0）：这一发撞的是
  // 得分倒退那道 409，拒收文案由 makeServerGuard 算出来，不是手写的
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

test('撤销把逐棒条数推回更低：保存放行、云端跟着变短（#713 拍板）', async ({ page }) => {
  // 这一例不预设「撤销会不会把条数推低」（本轮之前只能靠读码推断），而是真的录一拍再撤销，
  // 看推上来的 payload 剩几条。mock 跑的是后端现行规则（makeServerGuard）：条数变短放行；
  // 本例记分栏与本地都是 0 分，撞不到得分那道，所以画面上出现的就是线上会发生的。
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
  // #713 收紧后仍要放行：撤销/重做那条路会带 allowEvidenceShrink，否则本例会停在 3 条。
  // 这里能到 2 条，本身就证明意图随请求上去了。
  await expect.poll(() => state.events).toBe(2);
  // 拖过重试窗口再断两句反向：否则「当时为 0」不能排除“晚一步又弹出提示”
  await page.waitForTimeout(1500);
  await expect(page.getByText('现场快照未能同步到服务器')).toHaveCount(0);
  await expect(page.getByText('已拒绝覆盖')).toHaveCount(0);
});

test('服务器按条数拒收（变短且未带意图）：原样显示原因、不做无意义重试', async ({ page }) => {
  // 规则本身由后端 GameServiceLiveSnapshotGuardTest 钉；这里只验前端把服务器那句原因原样抬出来，
  // 不再被「换人在场状态会丢失」的通用文案顶掉。mock 直接给 shrink 文案，不装作复刻守门。
  const snapshotPosts = [];
  const official = { home: 0, away: 0 };
  await installMock(page, {
    snapshotPosts,
    restoredEvents: 1,
    official,
    replyFor: () => ({
      msg: formatGuardMessage(SNAPSHOT_GUARD_MESSAGES['zh-CN']['snapshot.guard.events.shrink'], [3, 1])
    })
  });
  await openLive(page);

  await saveButton(page).click();

  await expect(page.getByText('提交的快照只有 1 条，且未带撤销/重做意图')).toBeVisible();
  await page.waitForTimeout(1500);
  expect(snapshotPosts).toHaveLength(1);
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

test('界面切英文后被 409 拒收：屏幕上是英文，请求确实带了 X-UI-Locale（#713 文案本地化）', async ({ page }) => {
  // 这一例测的是前端那一跳：界面语言有没有真的随请求上去。语言的「选择」由后端
  // SnapshotGuardMessageKeyTest 验（真 MessageSource + 真 LocaleResolver），这里验的是
  // 前端有没有把话说给后端听。mock 按头选文案，所以 request.js 里那句 uiLocaleHeaders()
  // 一旦被删，这里拿到的就是简体中文而转红，而不是因为测试里写死了英文句子而照旧绿。
  const snapshotPosts = [];
  const snapshotHeaders = [];
  const official = { home: 6, away: 18 };
  await installMock(page, {
    snapshotPosts,
    snapshotHeaders,
    locale: 'en',
    official,
    replyFor: makeServerGuard({ official, state: { events: 1 } })
  });
  await openLive(page, 'en');

  await saveButton(page, 'en').click();

  // 请求侧：界面语言随每一发快照 POST 带上（fetch 改不动 Accept-Language，只能自己带，实测）
  await expect.poll(() => snapshotHeaders.at(-1)?.['x-ui-locale']).toBe('en');
  // 画面侧：取英文词典里独有的一句，不用按钮名（按钮名在英文界面上本来就在那儿）
  await expect(page.getByText('The overwrite was rejected so the records survive')).toBeVisible();
  await expect(page.getByText('已拒绝覆盖')).toHaveCount(0);
  await page.waitForTimeout(1500);
  expect(snapshotPosts).toHaveLength(1);
});

/**
 * 阵容板（第 5 例）用的 mock：与上面 installMock 的分别是「页面不同、端点也不同」。
 * 端点集合是照 LiveGameLineupBoard.vue 的加载链读码列的（game → live-snapshot 草稿 →
 * roster → lineup-submissions），不是猜的；缺一个就会看到阵容板停在「还没选队伍」。
 *
 * 守门形态照后端现行规则（GameService.mergeAndGuardSnapshot）：
 * 阵容板那一发不提 playEvents → lineupOnly → 两道比对都不跑，并把服务器自己的
 * teamsData/playEvents 并回去（preserveUnmentionedSections）。所以这一发不该被拒，
 * 也不该把云端逐棒记录清空——「你改你的名单，我没提到的部分保持原样」。
 */
async function installBoardMock(page, { lineupPosts, livePosts, state, official }) {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });

  const ok = (route, data) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, data })
  });

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/bsball-server', '');
    const post = request.method() === 'POST';
    const messages = SNAPSHOT_GUARD_MESSAGES[guardLocale(request.headers())];

    if (post && path === '/game/10/lineup-snapshot') {
      const body = JSON.parse(request.postDataJSON().snapshotJson);
      lineupPosts.push(body);
      if (Object.prototype.hasOwnProperty.call(body, 'playEvents')) {
        // 不是阵容板的形态（自带证据）：照旧过得分一道，别让 mock 比线上宽
        const claimed = (body.teamsData?.away?.score ?? 0) + (body.teamsData?.home?.score ?? 0);
        const runs = official.home + official.away;
        if (runs > 0 && claimed < runs) {
          return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
            code: 409, msg: formatGuardMessage(messages['snapshot.guard.run.regress'], [claimed, runs])
          }) });
        }
        // 补齐条数那道：payload 自带证据时后端按 lineupOnly=false 照跑两道比对，而阵容板端点
        // 恒为 saveLineupSnapshot(g, json, true) → allowEvidenceShrink=false，没有意图放行那一格。
        // 少了这一段，这份 mock 就比后端宽，「mock 绿而线上拒」查不出来。
        if (body.playEvents.length < state.events) {
          const incoming = body.playEvents.length;
          return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
            code: 409,
            msg: incoming === 0
              ? formatGuardMessage(messages['snapshot.guard.events.wiped'], [state.events])
              : formatGuardMessage(messages['snapshot.guard.events.shrink'], [state.events, incoming])
          }) });
        }
        state.events = body.playEvents.length;
      }
      // 没提 playEvents：不比对、现有证据原样留着（并回服务器现值后落库）
      return ok(route, { success: true });
    }
    if (post && path === '/game/10/live-snapshot') {
      const body = JSON.parse(request.postDataJSON().snapshotJson);
      livePosts.push(body);
      const claimed = (body.teamsData?.away?.score ?? 0) + (body.teamsData?.home?.score ?? 0);
      const runs = official.home + official.away;
      if (runs > 0 && claimed < runs) {
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
          code: 409, msg: formatGuardMessage(messages['snapshot.guard.run.regress'], [claimed, runs])
        }) });
      }
      // 条数那道照上游收紧后的规则一并复刻：任何变短（含清零）默认拒，带撤销意图才放行。
      // 缺这一段，这份 mock 就比后端窄，「mock 绿而线上拒」那种漂移查不出来。
      const shrinkIntent = request.postDataJSON().allowEvidenceShrink === true;
      const incomingEvents = body.playEvents?.length;
      if (incomingEvents != null && incomingEvents < state.events && !shrinkIntent) {
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
          code: 409,
          msg: incomingEvents === 0
            ? formatGuardMessage(messages['snapshot.guard.events.wiped'], [state.events])
            : formatGuardMessage(messages['snapshot.guard.events.shrink'], [state.events, incomingEvents])
        }) });
      }
      state.events = incomingEvents ?? state.events;
      return ok(route, { snapshotJson: request.postDataJSON().snapshotJson });
    }
    if (post && path === '/game/10/save-result') return ok(route, { success: true });

    if (path === '/event/1') return ok(route, { id: 1, name: 'Local E2E Event', gameMode: 'BASEBALL', specialRules: {} });
    if (path === '/event/1/game-team-options') return ok(route, [{ id: 1, name: 'Home' }, { id: 2, name: 'Away' }]);
    if (path === '/team/select-options') return ok(route, [{ id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 }]);
    if (/^\/team\/[12]$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      return ok(route, { id, name: id === 1 ? 'Home' : 'Away' });
    }
    if (path === '/stadium/list') return ok(route, { list: [], total: 0 });
    if (path === '/game/10') {
      return ok(route, {
        id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
        gameMode: 'BASEBALL', inning: 1, topBottom: 'top',
        homeScore: official.home, awayScore: official.away
      });
    }
    if (path === '/game/10/stats') return ok(route, { list: stats, total: stats.length });
    if (path === '/game/10/lineup-submissions') return ok(route, []);
    if (path === '/game/10/live-snapshot') {
      return ok(route, { snapshotJson: JSON.stringify(snapshot({ events: state.events })) });
    }
    const roster = /^\/event\/1\/teams\/([12])\/roster-players$/.exec(path);
    if (roster) {
      const teamId = Number(roster[1]);
      return ok(route, { restricted: false, players: players.filter((x) => x.teamId === teamId) });
    }
    return ok(route, []);
  });
}

test('已记分场次从阵容板点「开始录入」：阵容板那一发不该被守门误伤，也不该清掉云端证据', async ({ page }) => {
  // 这一例补的是「放行阵容板」最容易带坏的那件事：打过比分的场次能不能从阵容板存名单。
  // 跑的是线上真的那条链：LiveGameLineupBoard.vue → /game/{id}/lineup-snapshot →
  // saveLineupSnapshot(g, json, true)。注意同名旧件 LiveGameLineup.js 没有任何路由 import，
  // 拿它做结论会测到不运行的代码（tests/liveGameLineupBoardGuard409.test.mjs 钉住谁是活件）。
  const lineupPosts = [];
  const livePosts = [];
  const official = { home: 6, away: 18 };
  const state = { events: 3 };
  await installBoardMock(page, { lineupPosts, livePosts, state, official });

  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/lineup');
  const start = page.getByRole('button', { name: '开始录入' });
  await expect(start).toBeEnabled();
  await start.click();

  await expect.poll(() => lineupPosts.length).toBe(1);
  // 前提坐实（不是照注释信）：阵容板那一发确实不主张逐棒证据，才有 lineupOnly 豁免
  expect(Object.prototype.hasOwnProperty.call(lineupPosts[0], 'playEvents')).toBe(false);
  // 不误伤：这一发不能被守门顶回（顶回就是「打过比分的场次不能从阵容板保存」那个老形状）
  await expect(page.getByText('已拒绝覆盖')).toHaveCount(0);
  await page.waitForTimeout(1200);
  expect(state.events).toBe(3);
  // 且真的进了录入页
  await expect(page).toHaveURL(/games\/10\/live/);
  // 进录入页这一步本身不会自动推 live 端点（实测：到此处 livePosts 仍为 0），
  // 所以「换页就把用户撞一个 409」这个形状在现行前端不存在。
  expect(livePosts).toHaveLength(0);

  // 放行阵容板不等于拆掉防线：用户在录入页点保存时推的是 live 端点（lineupOnly=false），
  // 实测那一发 claimed=0、events=0（bootstrap 现场把本地放在 0 分、没自动采纳云端证据），
  // 被得分那道顶回，屏幕上抬的是服务器给的原因，云端 3 条逐棒记录照旧留着。
  await saveButton(page).click();
  await expect.poll(() => livePosts.length).toBe(1);
  await expect(page.getByText('已拒绝覆盖')).toBeVisible();
  await page.waitForTimeout(1500);
  expect(state.events).toBe(3);
});
