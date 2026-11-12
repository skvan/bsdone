// 赛前阵容提交：球队端保存 + 办赛方空侧自动载入（本地 mock，无后端）
// 运行：E2E_PORT=3102 npx playwright test e2e/team-pregame-lineup-local-mocked.spec.js
import { test, expect } from '@playwright/test';

const homePlayers = [
  { id: 101, name: '主队球员1', number: '1', teamId: 1 },
  { id: 102, name: '主队球员2', number: '2', teamId: 1 }
];
const awayPlayers = [
  { id: 201, name: '客队球员1', number: '1', teamId: 2 },
  { id: 202, name: '客队球员2', number: '2', teamId: 2 }
];

async function setupAuth(page) {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });
}

function gamePayload() {
  return {
    id: 10,
    eventId: 1,
    homeTeamId: 1,
    awayTeamId: 2,
    status: 'scheduled',
    gameMode: 'BASEBALL',
    gameNumber: 1,
    gameday: '2026-11-15',
    gameTime: '2026-11-15 14:00:00',
    venue: 'Test Field'
  };
}

function teamSubmission() {
  const lineup = [{ id: 201, name: '客队球员1', number: '1', teamId: 2, position: 'C' }];
  return {
    v: 1,
    gameId: 10,
    teamId: 2,
    gameMode: 'BASEBALL',
    complete: false,
    lineup,
    bench: [],
    fieldingPitcherId: null,
    lastDhAddedFromPoolId: null
  };
}

test('球队端：比赛列表 → 编辑赛前阵容 → 保存本队阵容', async ({ page }) => {
  await setupAuth(page);
  let savedBody = null;

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/team/1/lineup-games') {
      data = {
        eventId: 1,
        eventName: 'Local E2E Event',
        season: '2026',
        gameMode: 'BASEBALL',
        games: [{
          gameId: 10,
          gameNumber: 1,
          gameday: '2026-11-15',
          gameTime: '2026-11-15 14:00:00',
          venue: 'Test Field',
          status: 'scheduled',
          side: 'home',
          opponentTeamId: 2,
          opponentName: 'Away',
          hasSubmission: false,
          lineupUpdatedAt: null,
          complete: false
        }]
      };
    } else if (path === '/team/1/lineup-games/10' && request.method() === 'GET') {
      data = {
        gameId: 10,
        eventId: 1,
        eventName: 'Local E2E Event',
        season: '2026',
        gameMode: 'BASEBALL',
        specialRules: {},
        teamId: 1,
        teamName: 'Home',
        teamLogo: '',
        side: 'home',
        opponentTeamId: 2,
        opponentName: 'Away',
        gameNumber: 1,
        gameday: '2026-11-15',
        gameTime: '2026-11-15 14:00:00',
        venue: 'Test Field',
        status: 'scheduled',
        rosterRestricted: false,
        rosterCount: 2,
        lineupJson: null,
        lineupUpdatedAt: null,
        complete: false
      };
    } else if (path === '/team/1/lineup-games/10' && request.method() === 'PUT') {
      savedBody = request.postDataJSON();
      data = { saved: true };
    } else if (path === '/event/1/teams/1/roster-players') {
      data = { restricted: false, players: homePlayers };
    } else if (path === '/event/1/teams/2/roster-players') {
      data = { restricted: false, players: awayPlayers };
    } else if (path === '/stadium/list') {
      data = { list: [], total: 0 };
    } else if (path === '/event/1') {
      data = { id: 1, name: 'Local E2E Event', gameMode: 'BASEBALL', specialRules: {} };
    } else if (path === '/game/10') {
      data = gamePayload();
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.goto('/bs-ball/bs-ball/admin/teams/1/lineup-games/1');
  await expect(page.getByRole('button', { name: '编辑阵容' })).toBeVisible();
  await page.getByRole('button', { name: '编辑阵容' }).click();
  await expect(page.locator('.lineup-card--pool')).toHaveCount(2);

  await page.locator('.lineup-card--pool[data-player-id="101"]').click();
  await page.locator('.lineup-slot[data-slot-code="C"]').click();
  await page.getByRole('button', { name: '保存阵容' }).click();

  await expect.poll(() => savedBody?.lineupJson ?? '').toContain('"id":101');
  expect(JSON.parse(savedBody.lineupJson).teamId).toBe(1);
  expect(JSON.parse(savedBody.lineupJson).gameId).toBe(10);
});

test('办赛方：快照空侧自动载入球队赛前阵容', async ({ page }) => {
  await setupAuth(page);

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') {
      data = { id: 1, name: 'Local E2E Event', gameMode: 'BASEBALL', specialRules: {} };
    } else if (path === '/event/1/game-team-options') {
      data = [{ id: 1, name: 'Home' }, { id: 2, name: 'Away' }];
    } else if (path === '/stadium/list') {
      data = { list: [], total: 0 };
    } else if (path === '/event/1/teams/1/roster-players') {
      data = { restricted: false, players: homePlayers };
    } else if (path === '/event/1/teams/2/roster-players') {
      data = { restricted: false, players: awayPlayers };
    } else if (path === '/game/10') {
      data = gamePayload();
    } else if (path === '/game/10/live-snapshot') {
      data = { snapshotJson: '' };
    } else if (path === '/game/10/lineup-submissions') {
      data = [{
        teamId: 2,
        teamName: 'Away',
        teamLogo: '',
        side: 'away',
        lineupJson: JSON.stringify(teamSubmission()),
        updatedAt: '2026-11-11T18:20:00',
        complete: false
      }];
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/lineup');
  await expect(page.locator('.team-submission-banner')).toContainText('已载入客队赛前阵容');
  await expect(page.locator('.lineup-batting__row[data-player-id="201"]')).toBeVisible();
});
