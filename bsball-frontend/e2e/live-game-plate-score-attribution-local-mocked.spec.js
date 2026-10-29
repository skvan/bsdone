import { test, expect } from '@playwright/test';

// #457：本垒得分确认浮层延后落定时，「回本垒得分/推进」行必须留在原打席（第N棒标注），
// 不能被标到下一棒标题之下。回归覆盖 up()→pp() 的延后写入通道与 logBatterOverride 冻结打者机制。
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

async function installMock(page, savedSnapshots) {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });

  let savedSnapshot = '';
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
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson: savedSnapshot };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      savedSnapshot = request.postDataJSON().snapshotJson;
      savedSnapshots.push(JSON.parse(savedSnapshot));
      data = { snapshotJson: savedSnapshot };
    }
    else if (path === '/game/10/save-live') data = { success: true };
    else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) data = { restricted: false, players: [] };
    else if (path === '/player/list' || path === '/api/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 1);
      const list = players.filter((player) => player.teamId === teamId);
      data = { list, total: list.length };
    }
    else if (path === '/stadium/list') data = { list: [], total: 0 };
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

const headText = (entry) => (typeof entry === 'string' ? entry : entry?.head ?? '');
const tailText = (entry) => (entry && typeof entry === 'object' ? entry.tail ?? '' : '');
// eventLog 是 unshift（index 0 最新）；转成时间正序断言分组归属
const chronological = (eventLog) => [...eventLog].reverse().map(headText);
const chronologicalTails = (eventLog) => [...eventLog].reverse().map(tailText);

test('满垒保送经得分确认浮层落定：推进/得分行归属原打席，不落到下一棒（#457）', async ({ page }) => {
  const savedSnapshots = [];
  await installMock(page, savedSnapshots);
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();

  // 每球以计分板计数落定为准（B/S/O 分节点渲染，用容器 innerText 断言），避免点击快于状态更新导致吞球
  const scoreboard = page.getByLabel('当前比分与球数');
  const countShouldBe = async (balls) => {
    await expect
      .poll(async () => (await scoreboard.innerText()).replace(/\s+/g, ' ').includes(`B ${balls} · S 0`), { message: `计数应为 B${balls}` })
      .toBe(true);
  };
  // 计分板「击球员：T2Pn #n 第n棒」才是当前打者的可靠来源（打线侧栏恒含所有第N棒文本）
  const batterShouldBe = async (order) => {
    await expect
      .poll(async () => (await scoreboard.innerText()).includes(`击球员：T2P${order} #${order} 第${order}棒`), { message: `击球员应为第${order}棒` })
      .toBe(true);
  };
  const ballButton = page.locator('button[title="坏球 Ball"]');
  // 快速点击可能被前端过渡态吞掉（禁用/淡出），统一用「点击直到状态落地」的轮询模式
  const clickUntil = async (action, reached, message) => {
    await expect(async () => {
      if (await reached()) return;
      await action();
      expect(await reached(), message).toBe(true);
    }).toPass({ timeout: 20000, intervals: [200, 500, 1000] });
  };
  const recordBall = async (balls) => {
    await clickUntil(() => ballButton.click(), async () => (await scoreboard.innerText()).replace(/\s+/g, ' ').includes(`B ${balls} · S 0`), `计数应为 B${balls}`);
  };
  const walkOnce = async (nextBatterOrder) => {
    for (const balls of [1, 2, 3]) await recordBall(balls);
    // 第 4 坏：保送落定，打者推进
    await clickUntil(() => ballButton.click(), async () => (await scoreboard.innerText()).includes(`击球员：T2P${nextBatterOrder} #${nextBatterOrder}`), `击球员应推进到第${nextBatterOrder}棒`);
    await batterShouldBe(nextBatterOrder);
  };

  // 第 1~3 棒保送上垒 → 满垒（无人被迫进本垒，无判决浮层）
  for (let ab = 2; ab <= 4; ab++) await walkOnce(ab);

  // 第 4 棒第 4 坏保送：满垒挤回三垒跑者 → 弹出本垒得分确认浮层（延后落定通道 up()→pp()）
  await clickUntil(() => ballButton.click(), async () => (await page.locator('button.hsc-inline__btn--safe').count()) > 0, '满垒保送应弹出本垒得分确认浮层');
  // 浮层经跑垒动画延时后才可点（ir() 延迟置位），用类选择器等「安全」按钮出现
  const safeButton = page.locator('button.hsc-inline__btn--safe');
  await expect(safeButton).toBeVisible({ timeout: 15000 });
  await safeButton.click();
  await expect(safeButton).toHaveCount(0);

  // 第 5 棒记一球坏球，产生「第5棒」标题行
  await recordBall(1);

  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => savedSnapshots.length).toBeGreaterThan(0);
  const snapshot = savedSnapshots.at(-1);
  const lines = chronological(snapshot.eventLog);

  // [1] 四坏行按棒次齐整：第1~4棒各有保送行，且浮层落定后的推进/得分尾注留在第4棒行内
  const walkHeads = lines.filter((text) => /第\d+棒 .*: 四坏 \(BB\)/.test(text));
  expect(walkHeads.map((text) => text.match(/第(\d+)棒/)[1])).toEqual(['1', '2', '3', '4']);
  expect(snapshot.teamsData.away.score).toBe(1);
  const tails = chronologicalTails(snapshot.eventLog);
  const ab4HeadIdx = lines.findIndex((text) => /第4棒 .*: 四坏 \(BB\)/.test(text));
  expect(tails[ab4HeadIdx]).toContain('满垒挤回本垒得一分');

  // [2] #457 核心断言：第4棒打席标题写入后（含得分浮层延后落定），日志不得再出现更晚棒次的
  //     保送/得分行——即浮层落定的推进尾注必须随第4棒标题一起落下，不能漂到下一棒之后
  expect(lines.findIndex((text) => /第4棒 .*: 四坏 \(BB\)/.test(text)))
    .toBeLessThan(lines.length - 1);
  const lateAdvanceTails = tails.slice(ab4HeadIdx + 1).filter((text) => /进二垒|进三垒|回本垒|挤回/.test(text));
  expect(lateAdvanceTails).toEqual([]);

  // [4] playEvents：得分随第4棒（batterPlayerId=204）打席落定，sequence 连续无插队
  const ab4 = snapshot.playEvents.find((play) => play.batterPlayerId === 204);
  expect(ab4).toBeTruthy();
  expect(ab4.scoringRunners.map((runner) => runner.runnerId)).toEqual([201]);
  expect(ab4.scoreDelta.away).toBe(1);
  expect(snapshot.playEvents.map((play) => play.sequence)).toEqual([1, 2, 3, 4]);
});
