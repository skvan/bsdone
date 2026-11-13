import { test, expect } from '@playwright/test';

const EVENT_ID = 1;

const event = {
  id: EVENT_ID,
  name: '2026 花城联盟春季赛',
  season: '2026春',
  status: 'ongoing',
  formatType: 'GROUP_KNOCKOUT',
  groupCount: 2,
  advancePerGroup: 2,
  thirdPlace: true,
  bracketSize: 4,
  startDate: '2026-03-07',
  endDate: '2026-06-14'
};

const teams = [
  { id: 1, name: '广州暴力羊' },
  { id: 2, name: '深圳野钻' },
  { id: 3, name: '东莞 Lancers' },
  { id: 4, name: '中山猎鹰' },
  { id: 5, name: '广州不死鸟' },
  { id: 6, name: '佛山闪电' }
];

const groupA = {
  stage: 'GROUP',
  groupCode: 'A',
  groupName: 'A 组',
  advanceCount: 2,
  rows: [
    { rank: 1, teamId: 1, teamName: '广州暴力羊', teamLogo: null, played: 2, wins: 2, losses: 0, draws: 0, points: 4, winPct: 1, gamesBehind: 0, runsFor: 10, runsAgainst: 3, runDiff: 7, qualified: true },
    { rank: 2, teamId: 2, teamName: '深圳野钻', teamLogo: null, played: 2, wins: 1, losses: 1, draws: 0, points: 2, winPct: 0.5, gamesBehind: 1, runsFor: 5, runsAgainst: 8, runDiff: -3, qualified: true },
    { rank: 3, teamId: 3, teamName: '东莞 Lancers', teamLogo: null, played: 2, wins: 1, losses: 1, draws: 0, points: 2, winPct: 0.5, gamesBehind: 1, runsFor: 4, runsAgainst: 6, runDiff: -2, qualified: false },
    { rank: 4, teamId: 4, teamName: '中山猎鹰', teamLogo: null, played: 2, wins: 0, losses: 2, draws: 0, points: 0, winPct: 0, gamesBehind: 2, runsFor: 2, runsAgainst: 4, runDiff: -2, qualified: false }
  ],
  matches: [
    { gameId: 11, homeTeamId: 1, homeScore: 5, awayTeamId: 2, awayScore: 2 },
    { gameId: 12, homeTeamId: 1, homeScore: 5, awayTeamId: 3, awayScore: 1 },
    { gameId: 13, homeTeamId: 2, homeScore: 3, awayTeamId: 3, awayScore: 2 },
    { gameId: 14, homeTeamId: 4, homeScore: 1, awayTeamId: 2, awayScore: 2 },
    { gameId: 15, homeTeamId: 3, homeScore: 2, awayTeamId: 4, awayScore: 1 }
  ]
};

const groupB = {
  stage: 'GROUP',
  groupCode: 'B',
  groupName: 'B 组',
  advanceCount: 2,
  rows: [
    { rank: 1, teamId: 5, teamName: '广州不死鸟', teamLogo: null, played: 2, wins: 2, losses: 0, draws: 0, points: 4, winPct: 1, gamesBehind: 0, runsFor: 9, runsAgainst: 4, runDiff: 5, qualified: true },
    { rank: 2, teamId: 6, teamName: '佛山闪电', teamLogo: null, played: 2, wins: 1, losses: 1, draws: 0, points: 2, winPct: 0.5, gamesBehind: 1, runsFor: 6, runsAgainst: 6, runDiff: 0, qualified: true }
  ],
  matches: [
    { gameId: 21, homeTeamId: 5, homeScore: 5, awayTeamId: 6, awayScore: 2 },
    { gameId: 22, homeTeamId: 6, homeScore: 4, awayTeamId: 5, awayScore: 4 }
  ]
};

const bracket = {
  rounds: [
    {
      code: 'SF',
      name: '半决赛',
      matches: [
        { slot: 1, gameId: 31, gameNumber: 31, homeTeamId: 1, homeTeamName: '广州暴力羊', homeScore: 5, awayTeamId: 6, awayTeamName: '佛山闪电', awayScore: 2, status: 'final', winnerTeamId: 1 },
        { slot: 2, gameId: 32, gameNumber: 32, homeTeamId: 5, homeTeamName: '广州不死鸟', homeScore: 3, awayTeamId: 2, awayTeamName: '深圳野钻', awayScore: 4, status: 'final', winnerTeamId: 2 }
      ]
    },
    {
      code: 'FINAL',
      name: '决赛',
      matches: [
        { slot: 1, gameId: 33, gameNumber: 33, homeTeamId: 1, homeTeamName: '广州暴力羊', homeScore: 5, awayTeamId: 2, awayTeamName: '深圳野钻', awayScore: 3, status: 'final', winnerTeamId: 1 }
      ]
    }
  ],
  championTeamId: 1,
  thirdPlace: {
    slot: 1,
    gameId: 34,
    gameNumber: 34,
    homeTeamId: 6,
    homeTeamName: '佛山闪电',
    homeScore: 4,
    awayTeamId: 5,
    awayTeamName: '广州不死鸟',
    awayScore: 3,
    status: 'final',
    winnerTeamId: 6
  }
};

const games = [
  { id: 11, eventId: EVENT_ID, homeTeamId: 1, awayTeamId: 2, homeScore: 5, awayScore: 2, status: 'final', gameNumber: 1, gameday: '2026-03-07', venue: '花城球场' },
  { id: 31, eventId: EVENT_ID, homeTeamId: 1, awayTeamId: 6, homeScore: 5, awayScore: 2, status: 'final', gameNumber: 2, gameday: '2026-05-01', venue: '花城球场' },
  { id: 33, eventId: EVENT_ID, homeTeamId: 1, awayTeamId: 2, homeScore: 5, awayScore: 3, status: 'final', gameNumber: 3, gameday: '2026-06-01', venue: '花城球场' }
];

async function mockApis(page) {
  await page.route('**/bsball-server/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];
    if (path === `/event/${EVENT_ID}`) data = event;
    else if (path === `/event/${EVENT_ID}/tournament`) {
      data = {
        eventId: EVENT_ID,
        eventName: event.name,
        season: event.season,
        formatType: event.formatType,
        pointsRule: { win: 2, draw: 1, loss: 0 },
        standings: [groupA, groupB],
        bracket,
        warnings: []
      };
    } else if (path === '/game/list') {
      data = { list: games, total: games.length };
    } else if (path === '/team/select-options') {
      data = teams;
    } else if (path === '/portal/settings') {
      data = {};
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

test('门户赛事详情：积分榜赛果合并、对角划线与淘汰赛对阵图', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await mockApis(page);
  await page.goto(`/bs-ball/bs-ball/events/${EVENT_ID}`);

  await expect(page.getByRole('heading', { name: event.name })).toBeVisible();
  await expect(page.getByRole('tab', { name: '赛程与结果' })).toBeVisible();

  await page.getByRole('tab', { name: '积分榜' }).click();
  await expect(page.locator('.tournament-standings')).toBeVisible();
  await expect(page.locator('.combined-table tbody tr')).toHaveCount(4);
  await expect(page.locator('.diag-cell')).toHaveCount(4);
  await expect(page.locator('.combined-table')).toContainText('广州暴力羊');
  await expect(page.locator('.score-win').first()).toBeVisible();
  await expect(page.locator('.score-loss').first()).toBeVisible();

  await page.locator('.group-switch .el-radio-button').nth(1).click();
  await expect(page.locator('.combined-table')).toContainText('佛山闪电');

  await page.getByRole('tab', { name: '对阵图' }).click();
  await expect(page.locator('.portal-bracket')).toBeVisible();
  await expect(page.locator('.champion-card')).toContainText('广州暴力羊');
  await expect(page.locator('.third-place-card')).toContainText('佛山闪电');
});
