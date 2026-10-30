// 门户报名名单 PDF：实时取数、球队聚合分页、HTML 安全与入口断言
// 运行：node --test tests/portalOrderBookPdf.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  ORDER_BOOK_PDF_GEOMETRY,
  buildOrderBookPdfPagesHtml,
  fitImageContainSquare,
  paginateOrderBookCards,
  sanitizePdfFileName,
  scaleImageToFit
} from '../src/utils/orderBookPdfLayout.js';
import { loadEventOrderBookData } from '../src/utils/eventOrderBookData.js';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

const team = (id, extra = {}) => ({
  teamId: id,
  teamName: `球队${id}`,
  teamLogo: null,
  status: 'confirmed',
  playerCount: 0,
  rosterSubmittedAt: '2026-10-30T10:00:00',
  rosterLocked: false,
  ...extra
});

const player = (id, extra = {}) => ({
  playerId: id,
  name: `球员${id}`,
  avatar: null,
  number: String(id),
  positions: ['P'],
  currentInTeam: true,
  ...extra
});

test('分页：A4 固定五列，卡行不拆，超长球队续页重复队头', () => {
  assert.equal(ORDER_BOOK_PDF_GEOMETRY.columns, 5);
  assert.ok(ORDER_BOOK_PDF_GEOMETRY.pageWidth > 0);
  assert.ok(ORDER_BOOK_PDF_GEOMETRY.pageHeight > 0);

  const rows = Array.from({ length: 22 }, (_, i) => player(i + 1));
  const pages = paginateOrderBookCards([{ ...team(1), rows, submitted: true, playerCount: 22 }]);
  assert.ok(pages.length >= 2, '22 人至少跨两页');
  assert.equal(pages[0].header, 'first');
  assert.ok(pages.slice(1).every((p) => p.header === 'continuation'));

  const chunks = pages.flatMap((p) => p.teams.flatMap((t) => t.rows));
  assert.deepEqual(chunks.flat().map((p) => p.playerId), rows.map((p) => p.playerId), '顺序不丢不乱');
  assert.ok(chunks.every((chunk) => chunk.length <= 5), '每行最多五张卡');
  assert.equal(pages[1].teams[0].continued, true, '续页重复队头并标记续');
  assert.ok(
    pages.every((p) => p.teams.every((t) => t.rows.length > 0 || t.placeholder)),
    '不得出现孤队头'
  );
  pages.forEach((page, index) => {
    const G = ORDER_BOOK_PDF_GEOMETRY;
    const capacity = G.pageHeight - G.padding * 2 - G.footerHeight - G.bodyPaddingTop
      - (index === 0 ? G.firstHeaderHeight : G.continuationHeaderHeight);
    const used = page.teams.reduce(
      (sum, t) => sum + G.teamHeaderHeight + (t.placeholder ? G.placeholderHeight : t.rows.length * G.rowHeight),
      0
    );
    assert.ok(used <= capacity, `第 ${index + 1} 页内容高度 ${used} 不得超过 ${capacity}`);
  });
});

test('分页：未提交、加载失败、无在役分别给出明确占位', () => {
  const pages = paginateOrderBookCards([
    { ...team(1, { rosterSubmittedAt: null }), rows: [], submitted: false, loadFailed: false },
    { ...team(2), rows: [], submitted: true, loadFailed: true },
    { ...team(3), rows: [], submitted: true, loadFailed: false }
  ]);
  const entries = pages.flatMap((p) => p.teams);
  assert.equal(entries[0].placeholder, 'notSubmitted');
  assert.equal(entries[1].placeholder, 'loadFailed');
  assert.equal(entries[2].placeholder, 'noActive');
});

test('页面 HTML：用户数据经过转义，头像/队徽使用预载图片或首字回退', () => {
  const section = {
    ...team(1),
    teamName: '<img src=x onerror=alert(1)>',
    rows: [player(1, { name: '<script>alert(1)</script>', positions: ['P', '1B'] })],
    submitted: true,
    playerCount: 1
  };
  const pages = paginateOrderBookCards([section]);
  const html = buildOrderBookPdfPagesHtml(pages, {
    title: '秩序册 · <报名名单>',
    eventName: '<赛事>',
    eventSeason: '2026',
    generatedAtLine: '生成时间：2026-10-30 10:00',
    summaryText: '1 支球队',
    images: {},
    labels: {
      locked: '已锁定',
      playerCount: '名单人数',
      notSubmitted: '名单未提交',
      loadFailed: '名单加载失败',
      noActive: '本队暂无在役球员',
      continued: '（续）'
    }
  });
  assert.ok(!html.includes('<script>'));
  assert.ok(!html.includes('<img src=x'));
  assert.ok(html.includes('&lt;script&gt;'));
  assert.ok(html.includes('P / 1B'));
  assert.ok(html.includes('&lt;赛事&gt;'));
  assert.match(html, /\.obp-title\{[^}]*text-overflow:ellipsis/);
  assert.match(html, /\.obp-event\{[^}]*white-space:nowrap/);
  assert.match(html, /\.obp-cont\{[^}]*text-overflow:ellipsis/);
  assert.match(html, /\.obp-meta span\{[^}]*white-space:nowrap/);

  const imageSection = {
    ...section,
    teamLogo: '/logo.png',
    rows: [{ ...section.rows[0], avatar: '/avatar.png' }]
  };
  const imagePages = paginateOrderBookCards([imageSection]);
  const withImages = buildOrderBookPdfPagesHtml(imagePages, {
    title: '秩序册',
    eventName: '赛事',
    generatedAtLine: '生成时间：2026-10-30 10:00',
    summaryText: '汇总',
    images: {
      '/avatar.png': 'data:image/jpeg;base64,AAA',
      '/logo.png': 'data:image/png;base64,BBB'
    },
    labels: {}
  });
  assert.match(withImages, /<span class="obp-avatar"><img src="data:image\/jpeg;base64,AAA" alt=""><\/span>/);
  assert.doesNotMatch(withImages, /data:image\/jpeg;base64,AAA[^<]*<i>/);
  assert.match(withImages, /<span class="obp-team-logo"><img src="data:image\/png;base64,BBB" alt=""><\/span>/);
});

test('页头长赛事名在生成 HTML 时显式截断，不依赖 html2canvas 不支持的 ellipsis', () => {
  const longPlayerName = '超长球员姓名测试甲乙丙丁';
  const longTeamName = '超长球队名称'.repeat(8);
  const pages = paginateOrderBookCards([{
    ...team(1, { teamName: longTeamName }),
    rows: [player(1, { name: longPlayerName })],
    submitted: true,
    playerCount: 1
  }]);
  const html = buildOrderBookPdfPagesHtml(pages, {
    title: '秩序册 · 报名名单',
    eventName: '超长赛事名称'.repeat(20),
    eventSeason: '2026',
    generatedAtLine: '生成时间：2026-10-30 10:00',
    summaryText: '1 支球队',
    images: {},
    labels: {}
  });
  assert.ok(html.includes('…'));
  assert.ok(!html.includes('超长赛事名称'.repeat(20)));
  assert.ok(!html.includes(longPlayerName), '卡片姓名按实际宽度截断，不能只靠 overflow 裁剪');
  assert.ok(!html.includes(longTeamName), '队名同样显式截断');

  const fitting = buildOrderBookPdfPagesHtml(
    paginateOrderBookCards([{
      ...team(2),
      rows: [
        player(1, { name: '张三李四王' }),
        player(2, { name: 'SCHOENEWEIS' })
      ],
      submitted: true,
      playerCount: 2
    }]),
    {
      title: '秩序册',
      eventName: '赛事',
      generatedAtLine: '生成时间：2026-10-30 10:00',
      summaryText: '汇总',
      images: {},
      labels: {}
    }
  );
  assert.ok(fitting.includes('张三李四王'), '可容纳的 5 个 CJK 姓名不得误截断');
  assert.ok(!fitting.includes('SCHOENEWEIS'), '全大写拉丁姓名按保守宽度截断');
});

test('图片预载按卡片目标尺寸降采样，避免原图常驻内存', () => {
  assert.deepEqual(scaleImageToFit(4000, 3000, 256), { width: 256, height: 192 });
  assert.deepEqual(scaleImageToFit(100, 80, 256), { width: 100, height: 80 });
  assert.deepEqual(scaleImageToFit(0, 80, 256), { width: 0, height: 0 });
  assert.deepEqual(scaleImageToFit('bad', 80, 256), { width: 0, height: 0 });
  assert.deepEqual(fitImageContainSquare(200, 100, 128), {
    side: 128,
    drawWidth: 128,
    drawHeight: 64,
    offsetX: 0,
    offsetY: 32
  });
  assert.deepEqual(fitImageContainSquare(0, 80, 128), {
    side: 0, drawWidth: 0, drawHeight: 0, offsetX: 0, offsetY: 0
  });
});

test('文件名清理非法字符并保留中文', () => {
  const name = sanitizePdfFileName('2026 秋季/联赛:*?"<>|');
  assert.match(name, /^2026 秋季_联赛_+$/);
  assert.equal((name.match(/_/g) || []).length, 8);
  assert.equal(sanitizePdfFileName('   '), 'order-book');
});

test('实时取数：每次调用重新请求，只拉已确认且已提交球队，第二次拿到新名单', async () => {
  const calls = [];
  let rosterVersion = 1;
  const request = async (path) => {
    calls.push(path);
    if (path.includes('/teams?')) {
      return {
        data: [
          team(10),
          team(20, { rosterSubmittedAt: null }),
          team(30, { status: 'invited' })
        ]
      };
    }
    if (path.includes('/teams/10/roster?')) {
      return { data: [player(rosterVersion)] };
    }
    return { data: [] };
  };

  const first = await loadEventOrderBookData(101, request, 1000);
  assert.equal(first.sections.length, 2, '已确认球队全部保留；未提交球队显示占位');
  assert.equal(first.sections[0].rows[0].playerId, 1);
  assert.equal(first.sections[1].submitted, false);
  assert.deepEqual(calls, [
    '/api/event/101/teams?_ts=1000',
    '/api/event/101/teams/10/roster?_ts=1000'
  ], '未提交球队不应请求名单');

  rosterVersion = 2;
  calls.length = 0;
  const second = await loadEventOrderBookData(101, request, 2000);
  assert.equal(second.sections[0].rows[0].playerId, 2, '第二次下载必须反映最新报名名单');
  assert.deepEqual(calls, [
    '/api/event/101/teams?_ts=2000',
    '/api/event/101/teams/10/roster?_ts=2000'
  ]);
});

test('实时取数：单队名单失败保留占位，赛事列表失败直接抛错', async () => {
  const partial = await loadEventOrderBookData(7, async (path) => {
    if (path.includes('/teams?')) return { data: [team(1), team(2)] };
    if (path.includes('/teams/1/roster?')) return { error: 'boom' };
    return { data: [player(8)] };
  }, 1);
  assert.equal(partial.sections[0].loadFailed, true);
  assert.equal(partial.sections[1].rows.length, 1);

  await assert.rejects(
    () => loadEventOrderBookData(8, async () => ({ error: '赛事不存在' }), 1),
    /赛事不存在/
  );
});

test('入口：报名名单按钮与赛程按钮并列、同样式，PDF 生成不走浏览器打印', () => {
  const portalEvents = read('../src/views/portal/PortalEvents.js');
  const button = read('../src/components/portal/EventRosterDownloadButton.vue');
  const css = read('../src/styles/legacy/portal-events.css');
  const pdf = read('../src/utils/eventOrderBookPdf.js');

  assert.match(portalEvents, /EventRosterDownloadButton/);
  assert.match(
    portalEvents,
    /class:"event-header-pdf-wrap"[\s\S]*?EventRosterDownloadButton[\s\S]*?:M\("",!0\)\]\),_:2/,
    '报名名单按钮必须与赛程按钮位于同一间隔容器'
  );
  assert.match(portalEvents, /eventSeason:n\.season/);
  assert.match(portalEvents, /onKeydown:Ke\(/, 'PDF 操作区需阻止键盘事件冒泡到赛程折叠头');
  assert.match(button, /event-header-pdf-btn/);
  assert.match(button, /downloadRosterPdf/);
  assert.match(button, /loadEventOrderBookData/);
  assert.match(css, /\.event-header-pdf-wrap\[data-v-ea6f7ff4\]\{[^}]*gap:/);
  assert.match(pdf, /loadPrintExport/);
  assert.match(pdf, /loadJsPdf/);
  assert.match(pdf, /maxSize: 128, mime: 'image\/png', containSquare: true/);
  assert.match(pdf, /maxSize: 256, mime: 'image\/jpeg', quality: 0\.85, cropSquare: true/);
  assert.doesNotMatch(pdf, /window\.print/);
});

test('五语种按钮与 PDF 文案齐全', () => {
  const files = ['zh-CN', 'zh-TW', 'en', 'ko', 'ja'];
  const keys = [
    'events.downloadRosterPdf',
    'events.rosterPdfTitle',
    'events.downloadRosterPdfSuccess',
    'events.downloadRosterPdfFail',
    'events.downloadRosterPdfEmpty',
    'events.rosterPdfContinued'
  ];
  for (const name of files) {
    const dict = JSON.parse(read(`../src/locales/${name}.json`));
    for (const key of keys) {
      const value = key.split('.').reduce((obj, part) => obj?.[part], dict);
      assert.equal(typeof value, 'string', `${name} 缺少 ${key}`);
      assert.ok(value.length > 0, `${name} 的 ${key} 不能为空`);
    }
  }
});
