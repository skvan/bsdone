import { test, expect } from '@playwright/test';

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
const stats = players.map((player, index) => ({
  teamId: player.teamId,
  playerId: player.id,
  position: player.position,
  number: player.number,
  battingOrder: index % positions.length + 1,
  listOrder: index,
  ...zeroStats()
}));

async function openLive(page, locale) {
  await page.addInitScript((loc) => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
    localStorage.setItem('bs-ball-locale', loc);
  }, locale);

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
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson: '' };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') data = { snapshotJson: request.postDataJSON().snapshotJson };
    else if (path === '/game/10/save-live') data = { success: true };
    else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) data = { restricted: false, players: [] };
    else if (path === '/player/list') data = { list: players.filter((p) => p.teamId === Number(url.searchParams.get('teamId') ?? 1)), total: 9 };
    else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (/^\/team\/[12]$/.test(path)) data = { id: Number(path.split('/').at(-1)), name: path.endsWith('1') ? 'Home' : 'Away' };
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
}

test('English UI: top bar, scoreboard, corrections menu and fix-runners dialog', async ({ page }) => {
  await openLive(page, 'en');
  await expect(page.getByRole('button', { name: 'Save Game' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Substitution Mode' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Fullscreen' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'In Play' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Ball', exact: true })).toBeVisible();
  await expect(page.locator('.lg-scoreboard')).toContainText('BAT');
  await expect(page.locator('.lg-scoreboard')).toContainText('PIT');
  const text = await page.locator('.live-game-page').innerText();
  for (const zh of ['实时比赛录入', '保存比赛', '换人模式', '全屏显示', '坏球', '好球', '界外球', '界内球', '打线', '板凳席无可用球员', '替补']) {
    expect(text, `English UI still shows ${zh}`).not.toContain(zh);
  }
  await page.locator('button[title="Runner & Score Corrections"]').click();
  await expect(page.locator('.live-field-bench-ops-popover')).toBeVisible();
  await expect(page.getByText('Game State Corrections')).toBeVisible();
  await page.getByRole('button', { name: 'Fix Runners', exact: true }).click();
  await expect(page.getByRole('dialog')).toContainText('Fix Runners');
  await expect(page.getByRole('dialog')).toContainText('At-Bat');
  const labelBox = await page.locator('.runner-manual-fix__label').first().boundingBox();
  expect(labelBox.height).toBeLessThan(24);
  await page.getByRole('dialog').getByRole('button', { name: 'Cancel', exact: true }).click();

  await page.getByRole('button', { name: 'More' }).click();
  await expect(page.getByText('Restore Session from Cloud')).toBeVisible();
  await expect(page.getByText('End Game (mark final and save)')).toBeVisible();
  await page.keyboard.press('Escape');

  const quick = page.locator('.field-quick-pitch');
  await expect(quick).toContainText('Ball');
  await expect(quick).toContainText('Strike');
  await expect(quick).toContainText('Swing');
  await expect(quick).toContainText('Foul');
  await expect(quick).toContainText('In Play');

  await page.getByRole('button', { name: 'Save Game' }).click();
  await expect(page.locator('.el-message').filter({ hasText: 'Saved' }).last()).toBeVisible();

  await page.getByRole('button', { name: 'More' }).click();
  await page.getByText('End Game (mark final and save)').click();
  const endBox = page.locator('.el-message-box');
  await expect(endBox).toContainText('End this game?');
  await endBox.getByRole('button', { name: 'Cancel' }).click();

  await page.getByRole('button', { name: 'More' }).click();
  await page.getByText('Restore Session from Cloud').click();
  await expect(endBox).toContainText('Cloud data will overwrite the current scoring page');
  await expect(endBox).toContainText('Continue');
  await endBox.getByRole('button', { name: 'Cancel' }).click();

  await page.getByRole('link', { name: '工作台' }).click();
  const leaveBox = page.locator('.el-message-box');
  await expect(leaveBox).toContainText('Leave Confirmation');
  await expect(leaveBox).toContainText('Save & Leave');
  await leaveBox.getByRole('button', { name: 'Stay' }).click();
});
