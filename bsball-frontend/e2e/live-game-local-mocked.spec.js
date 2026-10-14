import { test, expect } from '@playwright/test';

const scenarios = ['BASEBALL', 'SOFTBALL'].flatMap((gameMode) =>
  [false, true].map((walk) => ({ gameMode, walk }))
);

for (const { gameMode, walk } of scenarios) {
  const positions = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'];
  if (gameMode === 'SOFTBALL') positions.push('XF');
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

  const stats = players.map((player, index) => ({
    teamId: player.teamId,
    playerId: player.id,
    position: player.position,
    number: player.number,
    battingOrder: index % positions.length + 1,
    listOrder: index,
    ...zeroStats()
  }));

  test(`${gameMode}: ${walk ? 'walk' : 'ball'} survives undo, save and server restore`, async ({ page }) => {
    let savedSnapshot = '';
    const savedSnapshots = [];
    let savedLiveBody;
    const intercepted = [];

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
      intercepted.push(`${request.method()} ${path}`);
      let data = [];

      if (path === '/event/1') data = { id: 1, name: 'Local E2E Event' };
      else if (path === '/team/select-options') data = [
        { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 }
      ];
      else if (path === '/game/10') data = {
        id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
        gameMode, inning: 1, topBottom: 'top', homeScore: 0, awayScore: 0
      };
      else if (path === '/game/10/stats') data = { list: stats, total: stats.length };
      else if (path === '/game/10/live-snapshot' && request.method() === 'GET') {
        data = { snapshotJson: savedSnapshot };
      } else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
        savedSnapshot = request.postDataJSON().snapshotJson;
        savedSnapshots.push(savedSnapshot);
        data = { snapshotJson: savedSnapshot };
      } else if (path === '/game/10/save-live') {
        savedLiveBody = request.postDataJSON();
        data = { success: true };
      } else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) {
        data = { restricted: false, players: [] };
      } else if (path === '/player/list') {
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

    const recordBall = async () => {
      await page.locator('button[title="坏球 Ball"]').click();
      await expect(page.locator('button[title="坏球 Ball"]')).toBeVisible();
    };

    const initialCount = gameMode === 'SOFTBALL' ? 1 : 0;
    const pitchCount = walk ? 4 - initialCount : 1;
    for (let pitch = 0; pitch < pitchCount; pitch++) await recordBall();
    await page.getByRole('button', { name: '保存比赛' }).click();
    await expect.poll(() => savedSnapshots.length).toBeGreaterThan(0);
    const firstSnapshot = JSON.parse(savedSnapshots.at(-1));
    expect(firstSnapshot.eventLog.length).toBeGreaterThan(0);
    expect(JSON.stringify(firstSnapshot.eventLog)).toContain('坏球');
    expect(firstSnapshot.gameMode).toBe(gameMode);
    expect(firstSnapshot.gameState).toMatchObject({
      inning: 1, isTop: true, outs: 0,
      balls: walk ? initialCount : initialCount + 1,
      strikes: initialCount,
      batterIndex: { away: walk ? 1 : 0, home: 0 },
      runners: { 1: walk ? 201 : null, 2: null, 3: null }
    });
    expect(firstSnapshot.teamsData.away.score).toBe(0);
    expect(firstSnapshot.teamsData.home.score).toBe(0);
    expect(firstSnapshot.playerStatsById['201'].batting).toMatchObject({
      bb: walk ? 1 : 0, ab: 0, h: 0, r: 0, rbi: 0
    });
    expect(firstSnapshot.playerStatsById['101'].pitching).toMatchObject({
      np: pitchCount, pitchBbHp: walk ? 1 : 0, ip: 0, er: 0
    });
    expect(firstSnapshot.playerStatsById['101'].fielding).toMatchObject({ po: 0, a: 0, e: 0 });
    expect(firstSnapshot.atBatSummaries).toHaveLength(walk ? 1 : 0);
    if (walk) expect(firstSnapshot.atBatSummaries[0].resultCode).toBe('BB');

    await page.getByRole('button', { name: /撤销/ }).click();
    await recordBall();
    await page.getByRole('button', { name: '保存比赛' }).click();
    await expect.poll(() => savedLiveBody).toBeTruthy();
    await expect.poll(() => savedSnapshots.length).toBeGreaterThan(1);
    expect(savedLiveBody.game).toMatchObject({ inning: 1, topBottom: 'top' });
    expect(Array.isArray(savedLiveBody.stats)).toBe(true);

    const snapshot = JSON.parse(savedSnapshots.at(-1));
    expect(snapshot.eventLog.length).toBeGreaterThan(0);
    expect(JSON.stringify(snapshot.eventLog)).toContain('坏球');
    expect(snapshot.eventLog).toEqual(firstSnapshot.eventLog);
    expect(snapshot.gameState).toEqual(firstSnapshot.gameState);
    expect(snapshot.playerStatsById).toEqual(firstSnapshot.playerStatsById);
    expect(snapshot.activeAbContext).toEqual(firstSnapshot.activeAbContext);
    const withoutGeneratedIds = (summaries) => summaries.map(({ id, ...summary }) => summary);
    expect(withoutGeneratedIds(snapshot.atBatSummaries)).toEqual(withoutGeneratedIds(firstSnapshot.atBatSummaries));

    await page.getByRole('button', { name: /撤销/ }).click();
    await page.getByRole('button', { name: /重做/ }).click();
    const beforeRedoSave = savedSnapshots.length;
    await page.getByRole('button', { name: '保存比赛' }).click();
    await expect.poll(() => savedSnapshots.length).toBeGreaterThan(beforeRedoSave);
    const redoneSnapshot = JSON.parse(savedSnapshots.at(-1));
    expect(redoneSnapshot.eventLog).toEqual(snapshot.eventLog);
    expect(redoneSnapshot.gameState).toEqual(snapshot.gameState);
    expect(redoneSnapshot.activeAbContext).toEqual(snapshot.activeAbContext);
    expect(redoneSnapshot.playerStatsById).toEqual(snapshot.playerStatsById);
    expect(redoneSnapshot.atBatSummaries).toEqual(snapshot.atBatSummaries);

    await page.reload();
    await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();
    await expect.poll(() => intercepted.filter((entry) => entry === 'GET /game/10/live-snapshot').length).toBeGreaterThan(1);
    expect(intercepted.some((entry) => entry.includes('/game/10/save-live'))).toBe(true);
    const saveCount = savedSnapshots.length;
    await page.getByRole('button', { name: '保存比赛' }).click();
    await expect.poll(() => savedSnapshots.length).toBeGreaterThan(saveCount);
    const restoredSnapshot = JSON.parse(savedSnapshots.at(-1));
    expect(restoredSnapshot.eventLog).toEqual(firstSnapshot.eventLog);
    expect(restoredSnapshot.gameState).toEqual(firstSnapshot.gameState);
    expect(restoredSnapshot.playerStatsById).toEqual(firstSnapshot.playerStatsById);
    expect(restoredSnapshot.activeAbContext).toEqual(snapshot.activeAbContext);
    expect(restoredSnapshot.atBatSummaries).toEqual(snapshot.atBatSummaries);
    if (!walk) {
      const remainingPitches = 3 - initialCount;
      for (let pitch = 0; pitch < remainingPitches; pitch++) await recordBall();
      const beforeFinalSave = savedSnapshots.length;
      await page.getByRole('button', { name: '保存比赛' }).click();
      await expect.poll(() => savedSnapshots.length).toBeGreaterThan(beforeFinalSave);
      const completed = JSON.parse(savedSnapshots.at(-1));
      expect(completed.atBatSummaries).toHaveLength(1);
      expect(completed.atBatSummaries[0]).toMatchObject({
        resultCode: 'BB', batterPlayerId: 201, batterOrder: 1,
        ballsBefore: initialCount,
        strikesBefore: initialCount,
        basesBefore: { 1: false, 2: false, 3: false }, outsBefore: 0, outsAfter: 0
      });
      expect(completed.playerStatsById['201'].batting).toMatchObject({ bb: 1, ab: 0, rbi: 0 });
      expect(completed.gameState.runners).toEqual({ 1: 201, 2: null, 3: null });
    }
  });
}
