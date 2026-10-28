// 个人驾驶舱：全部球队显示全部代表球队、选中球队只显示该队、野手不渲染投球雷达卡。
// 运行：npx playwright test e2e/portal-clubhouse-radar-teams.spec.js（playwright.config 自动起 dev:3000）
import { test, expect } from '@playwright/test';

const TENANT = 'bs-ball';
const ALL = 0;

const teams = [
  {
    id: 1,
    name: '广州猎豹',
    shortName: '猎豹',
    logo: '',
    events: [{ id: 11, name: '春季联赛', gameMode: 'BASEBALL', status: 'ongoing' }],
    primaryEventId: 11,
    primaryEventName: '春季联赛',
    standing: {}
  },
  {
    id: 2,
    name: '海珠蓝鸟',
    shortName: '蓝鸟',
    logo: '',
    events: [{ id: 12, name: '城市杯', gameMode: 'BASEBALL', status: 'ongoing' }],
    primaryEventId: 12,
    primaryEventName: '城市杯',
    standing: {}
  }
];

const AXES = {
  batting: ['AVG', 'OBP', 'SLG', 'OPS', 'RBI', 'SB'],
  fielding: ['FPCT', 'TC/G', 'PO/G', 'A/G', 'DP/G', 'RF/9'],
  pitching: ['ERA', 'WHIP', 'K/9', 'BB/9', 'HR/9', 'IP']
};

function card(domain) {
  const titles = { batting: '打击评价', fielding: '守备评价', pitching: '投球评价' };
  const units = { batting: 'PA', fielding: 'INN', pitching: 'IP' };
  return {
    domain,
    title: titles[domain],
    subtitle: AXES[domain].join(' · '),
    available: true,
    score: 78,
    grade: 'B+',
    sampleSize: 120,
    ownSample: domain === 'batting' ? 88 : 42.3,
    sampleUnit: units[domain],
    reliability: 1.0,
    confidenceLabel: '样本充足',
    axes: AXES[domain].map((abbr, index) => ({
      key: `${domain}-${index}`,
      label: abbr,
      abbr,
      value: `0.${300 + index}`,
      percentile: 60 + index * 5
    })),
    note: domain === 'batting' ? '参考 wOBA' : domain === 'fielding' ? '未按守位分组' : '参考 FIP',
    meta: domain === 'batting' ? { woba: 0.352 } : domain === 'pitching' ? { fip: 3.21, fipConstant: 3.1 } : {}
  };
}

/** 旧口径 payload：野手仍带回投球卡，用于验证前端兜底过滤。 */
function evaluation(role) {
  const cards = [card('batting'), card('fielding'), card('pitching')];
  return {
    available: true,
    comparison: '全租户生涯',
    minSample: { batting: 10, pitching: 3, fielding: 6 },
    cards,
    overall: {
      score: 78,
      grade: 'B+',
      reliability: 1.0,
      confidenceLabel: '样本充足',
      role,
      weights: role === 'POSITION_PLAYER'
        ? { batting: 0.55, fielding: 0.45 }
        : { batting: 0.15, pitching: 0.7, fielding: 0.15 },
      sampleSize: {
        batting: 88,
        pitching: role === 'POSITION_PLAYER' ? 0 : 42.3,
        fielding: 42.3
      }
    }
  };
}

function dashboard(teamId, role) {
  const team = teams.find((item) => item.id === teamId) || null;
  return {
    player: {
      id: 10,
      name: '陈一',
      nameEn: 'CHEN YI',
      number: '27',
      positions: ['CF'],
      throwHand: '右投',
      batHand: '右打',
      intro: '本地冒烟球员',
      height: 178,
      weight: 75
    },
    teams,
    scope: {
      teamId: team ? team.id : null,
      teamName: team ? team.name : '全部球队',
      eventId: null,
      eventName: '生涯',
      gameMode: 'BASEBALL',
      teamAll: !team,
      career: true
    },
    events: [],
    config: {
      version: 4,
      layout: 'v4-board',
      modules: [{
        key: 'evaluation', enabled: true, x: 0, y: 0, width: 1216, height: 700
      }]
    },
    stats: {
      batting: { gp: 20, pa: 88, ab: 70, h: 22, avg: '.314', obp: '.402', slg: '.457', ops: '.859', rbi: 18, sb: 6 },
      pitching: { ip: 0 },
      fielding: { gp: 20, inn: 42.3, tc: 55, po: 30, a: 22, e: 3 }
    },
    catalog: [],
    moduleCatalog: [{
      key: 'evaluation',
      label: '总体评价',
      defaultWidth: 1216,
      defaultHeight: 700,
      minWidth: 720,
      minHeight: 360,
      maxWidth: 1216,
      maxHeight: 1600
    }],
    milestoneCatalog: [],
    performance: { points: [], available: false },
    ranks: [],
    evaluation: evaluation(role),
    nextGame: null,
    schedule: [],
    recentGames: []
  };
}

async function mockClubhouse(page, role = 'POSITION_PLAYER') {
  await page.addInitScript(() => {
    localStorage.setItem('bs-ball-locale', 'zh-CN');
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: false, username: 'local-e2e'
    }));
  });
  await page.route('**/bsball-server/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = {};
    // `/api/...` 会被请求层重写为 `/bsball-server/...`
    if (path === '/account/clubhouse') {
      const raw = url.searchParams.get('teamId');
      const teamId = raw == null || raw === '' ? ALL : Number(raw);
      data = dashboard(Number.isFinite(teamId) ? teamId : ALL, role);
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

test('全部球队：hero 与球员卡显示全部代表球队，不显示「全部球队」', async ({ page }) => {
  await mockClubhouse(page);

  await page.goto(`/bs-ball/${TENANT}/me`);

  const heroTeam = page.locator('.hero-team');
  await expect(heroTeam.locator('.team-copy b')).toHaveText('广州猎豹 / 海珠蓝鸟');
  await expect(heroTeam.locator('.crest')).toHaveText('全');
  await expect(heroTeam).not.toContainText('全部球队');

  const cardTeam = page.locator('.card-team-name');
  await expect(cardTeam).toContainText('广州猎豹 / 海珠蓝鸟');
  await expect(page.getByText('代表 2 队 · 2 项赛事')).toBeVisible();
});

test('选择球队：hero 与球员卡只显示该队', async ({ page }) => {
  await mockClubhouse(page);
  await page.goto(`/bs-ball/${TENANT}/me`);

  await page.locator('.scope-team').click();
  await page.getByRole('option', { name: '海珠蓝鸟' }).click();

  const heroTeam = page.locator('.hero-team');
  await expect(heroTeam.locator('.team-copy b')).toHaveText('海珠蓝鸟');
  await expect(heroTeam.locator('.crest')).toHaveText('蓝鸟');
  await expect(heroTeam).not.toContainText('广州猎豹');

  const cardTeam = page.locator('.card-team-name');
  await expect(cardTeam).toContainText('海珠蓝鸟');
  await expect(cardTeam).not.toContainText('广州猎豹');
  await expect(page.getByText('代表 2 队 · 2 项赛事')).toBeVisible();
});

test('野手：不渲染投球雷达卡，也不展示公式说明', async ({ page }) => {
  await mockClubhouse(page);

  await page.goto(`/bs-ball/${TENANT}/me`);

  await expect(page.locator('.radar-card.batting')).toBeVisible();
  await expect(page.locator('.radar-card.fielding')).toBeVisible();
  await expect(page.locator('.radar-card.pitching')).toHaveCount(0);
  await expect(page.locator('.eval-note')).toHaveCount(0);
  await expect(page.locator('.evaluation-panel')).not.toContainText('角色权重');
  await expect(page.locator('.evaluation-panel')).not.toContainText('上限 500 人');
  await expect(page.getByLabel('评价口径')).toHaveCount(0);
});
