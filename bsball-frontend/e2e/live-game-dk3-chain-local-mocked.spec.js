import { test, expect } from '@playwright/test';

// #696 不死三振（漏接三振）在一垒出局：恢复传球链
// - 直接完成记录：捕手触杀 PO，投手三振
// - 捕手传一垒：捕手 A、一垒 PO，投手三振
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
  pitching: { ip: 0, np: 0, pitchH: 0, er: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0, wp: 0, pitchPa: 0, pitchBf: 0 },
  fielding: { po: 0, a: 0, e: 0, pb: 0 }
});

const stats = players.map((player, index) => ({
  teamId: player.teamId,
  playerId: player.id,
  position: player.position,
  number: player.number,
  battingOrder: index % positions.length + 1,
  listOrder: index,
  ...zeroStats()
}));

async function setupLiveGame(page) {
  const savedSnapshots = [];
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
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') {
      data = { snapshotJson: savedSnapshots.at(-1) ?? '' };
    } else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      const { snapshotJson } = request.postDataJSON();
      savedSnapshots.push(snapshotJson);
      data = { snapshotJson };
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

  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();
  return { savedSnapshots };
}

async function openDk3OutAtFirst(page) {
  const strike = page.locator('button[title="挥空（计好球）Swing & Miss"]');
  await strike.click();
  await strike.click();
  await strike.click();

  const flowBar = page.locator('.les-flow-card');
  await expect(flowBar).toBeVisible();
  await flowBar.getByRole('button', { name: '漏接三振' }).click();

  const menu = page.locator('.lgpm__panel');
  await expect(menu).toBeVisible();
  // 浮层打开后 240ms 内会拦截点击（blockClickMsAfterOpen）
  await page.waitForTimeout(350);
  await menu.getByRole('button', { name: /挥空三振/ }).click();
  await menu.getByRole('button', { name: /在一垒出局/ }).click();
  return flowBar;
}

async function clickFielder(page, pos) {
  await page.locator(`g.defender-hit[data-pos="${pos}"]:visible`).first().click();
  await page.waitForTimeout(250);
}

async function saveSnapshot(page, snapshots) {
  const before = snapshots.length;
  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => snapshots.length).toBeGreaterThan(before);
  return JSON.parse(snapshots.at(-1));
}

function logText(snap) {
  return snap.eventLog.map((entry) => (typeof entry === 'string' ? entry : JSON.stringify(entry))).join('\n');
}

test('漏接三振在一垒出局：直接完成记录=捕手触杀 PO，投手三振', async ({ page }) => {
  const { savedSnapshots } = await setupLiveGame(page);
  const flowBar = await openDk3OutAtFirst(page);

  await expect(flowBar).toContainText('捕手已接球');
  await expect(flowBar).toContainText('直接点「完成记录」记捕手触杀');
  await page.getByRole('button', { name: '完成记录' }).click();
  await expect(flowBar).toHaveCount(0);

  const snap = await saveSnapshot(page, savedSnapshots);
  expect(snap.gameState.outs).toBe(1);
  expect(snap.playerStatsById['102'].fielding).toMatchObject({ po: 1, a: 0 });
  expect(snap.playerStatsById['103'].fielding).toMatchObject({ po: 0, a: 0 });
  expect(snap.playerStatsById['101'].pitching.pitchSo).toBe(1);
  expect(snap.playerStatsById['201'].batting).toMatchObject({ so: 1, ab: 1 });
  const text = logText(snap);
  expect(text).toContain('捕手 T1P2 (#2) 触杀出局 (K)');
});

test('漏接三振在一垒出局：捕手传一垒=捕手 A、一垒 PO，投手三振', async ({ page }) => {
  const { savedSnapshots } = await setupLiveGame(page);
  const flowBar = await openDk3OutAtFirst(page);

  await expect(flowBar).toContainText('捕手已接球');
  await clickFielder(page, '1B');
  await expect(flowBar).toContainText('2-3');
  await expect(flowBar).toContainText('已可完成');
  await page.getByRole('button', { name: '完成记录' }).click();
  await expect(flowBar).toHaveCount(0);

  const snap = await saveSnapshot(page, savedSnapshots);
  expect(snap.gameState.outs).toBe(1);
  expect(snap.playerStatsById['102'].fielding).toMatchObject({ po: 0, a: 1 });
  expect(snap.playerStatsById['103'].fielding).toMatchObject({ po: 1, a: 0 });
  expect(snap.playerStatsById['101'].pitching.pitchSo).toBe(1);
  expect(snap.playerStatsById['201'].batting).toMatchObject({ so: 1, ab: 1 });
  const text = logText(snap);
  expect(text).toContain('捕手 T1P2 (#2)→一垒 T1P3 (#3) 传杀 一垒出局 (K)');
});
