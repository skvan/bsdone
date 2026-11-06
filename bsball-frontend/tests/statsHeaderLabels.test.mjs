// 统计表头三语一致性守护：列 → 术语键 → 全称/缩写/列宽
// 运行：node --test tests/statsHeaderLabels.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

import {
  STATS_BATTING_COLS,
  STATS_PITCHING_COLS,
  STATS_FIELDING_COLS,
  TEAM_STATS_BATTING_COLS,
  TEAM_STATS_PITCHING_COLS,
  TEAM_STATS_FIELDING_COLS
} from '../src/stores/statsColumns.js';
import {
  HEADER_KEY_BY_COLUMN,
  statsColumnWidth,
  statsHeaderKey
} from '../src/stores/statsHeaderKeys.js';

const readJson = (rel) => JSON.parse(fs.readFileSync(new URL(rel, import.meta.url), 'utf8'));
const messages = {
  'zh-CN': readJson('../src/locales/zh-CN.json'),
  'zh-TW': readJson('../src/locales/zh-TW.json'),
  en: readJson('../src/locales/en.json')
};

const ALL_COLS = [
  ...STATS_BATTING_COLS,
  ...STATS_PITCHING_COLS,
  ...STATS_FIELDING_COLS,
  ...TEAM_STATS_BATTING_COLS,
  ...TEAM_STATS_PITCHING_COLS,
  ...TEAM_STATS_FIELDING_COLS
];

const CJK = /[\u3400-\u9fff]/;
const SIMPLIFIED_HINT = /[门问题实发让给现机关这没样种数据队垒运动训练纪录影视众后进过将来还么须应与为见观贝页风飞马鸟鱼龙龟齿龄场击纯独牵终结长对时总记满触滚统获较联员职责备术单双败胜负]/;

function resolveHeader(locale, col) {
  const headerKey = statsHeaderKey(col.key, col.colKey);
  const msg = messages[locale];
  const term = msg.glossary?.term?.[headerKey];
  const fullName = msg.stats.col[headerKey] || term?.fullName || msg.stats.col[col.colKey] || msg.stats.colEn[col.colKey];
  const abbreviation = term?.shortLabel || msg.stats.colEn[col.colKey];
  return { headerKey, fullName, abbreviation };
}

function visualWidth(text) {
  let width = 0;
  for (const ch of String(text)) width += CJK.test(ch) ? 12 : 7;
  return width;
}

test('每个统计列都有显式术语键映射', () => {
  for (const col of ALL_COLS) {
    assert.ok(HEADER_KEY_BY_COLUMN[col.key], `列 ${col.key} 缺少术语键映射`);
  }
});

test('三语表头全称与英文缩写都存在', () => {
  for (const col of ALL_COLS) {
    for (const locale of ['zh-CN', 'zh-TW', 'en']) {
      const { headerKey, fullName, abbreviation } = resolveHeader(locale, col);
      assert.ok(messages[locale].glossary.term[headerKey], `${locale} ${col.key}（${headerKey}）缺少术语条目（tooltip）`);
      assert.ok(fullName, `${locale} ${col.key}（${headerKey}）缺少全称`);
      assert.ok(abbreviation, `${locale} ${col.key}（${headerKey}）缺少缩写`);
      assert.match(abbreviation, /^[A-Z0-9%+./-]+$/, `${col.key} 英文缩写格式不合法：${abbreviation}`);
      assert.ok(abbreviation.length <= 6, `${col.key} 英文缩写过长：${abbreviation}`);
      if (locale !== 'en') {
        assert.ok(CJK.test(fullName), `${locale} ${col.key} 全称不是中文：${fullName}`);
        assert.notEqual(fullName, abbreviation, `${locale} ${col.key} 全称与缩写相同：${fullName}`);
      }
    }
  }
});

test('繁中表头全称无简体残留', () => {
  for (const col of ALL_COLS) {
    const { headerKey, fullName } = resolveHeader('zh-TW', col);
    assert.ok(!SIMPLIFIED_HINT.test(fullName), `zh-TW ${col.key}（${headerKey}）全称含简体：${fullName}`);
  }
});

test('守备 GS 使用守备语境术语', () => {
  assert.equal(statsHeaderKey('gs', 'gs', 'fielding'), 'gsFielding');
  for (const locale of ['zh-CN', 'zh-TW', 'en']) {
    assert.ok(messages[locale].stats.col.gsFielding, `${locale} 缺少 stats.col.gsFielding`);
    assert.ok(messages[locale].glossary.term.gsFielding?.fullName, `${locale} 缺少 gsFielding 术语`);
  }
  for (const locale of ['zh-CN', 'zh-TW']) {
    const full = messages[locale].stats.col.gsFielding;
    assert.ok(visualWidth(full) + 24 + 8 <= statsColumnWidth('gs', 80), `${locale} gsFielding 列宽不足`);
  }
});

test('守备 GS 接线：PortalStats 传 fielding 语境，PortalLeadersTable 透传 domain', () => {
  const statsEvent = fs.readFileSync(new URL('../src/components/portal/statsEventCascade.js', import.meta.url), 'utf8');
  const portalStats = fs.readFileSync(new URL('../src/views/portal/PortalStats.js', import.meta.url), 'utf8');
  assert.ok(statsEvent.includes('shk(a.key,a.colKey,e.termDomain)'), 'PortalLeadersTable 未把 termDomain 传给术语映射');
  assert.ok(portalStats.includes('"term-domain":"fielding"'), 'PortalStats 守备榜未传 fielding 语境');
  assert.ok(portalStats.includes('"term-domain":"pitching"'), 'PortalStats 投手榜未传 pitching 语境');
});

test('中文表头宽度按全称 + 排序箭头 + 内边距估算足够', () => {
  for (const col of ALL_COLS) {
    const width = statsColumnWidth(col.key, col.width);
    for (const locale of ['zh-CN', 'zh-TW']) {
      const { headerKey, fullName } = resolveHeader(locale, col);
      const needed = visualWidth(fullName) + 24 + 8;
      assert.ok(
        needed <= width,
        `${locale} ${col.key}（${headerKey}）列宽 ${width} 不足以容纳「${fullName}」估算 ${needed}px`
      );
    }
  }
});

test('三语都有 stats.col 列标签', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en']) {
    for (const col of ALL_COLS) {
      const headerKey = statsHeaderKey(col.key, col.colKey);
      assert.ok(
        messages[locale].stats.col[headerKey],
        `${locale} ${col.key}（${headerKey}）缺少 stats.col 列标签`
      );
    }
  }
});

test('高置信度术语按官方口径落库', () => {
  const expected = {
    'zh-CN': {
      twoB: '二垒打',
      threeB: '三垒打',
      hr: '本垒打',
      hbp: '触身球',
      sh: '牺牲触击球',
      sf: '牺牲高飞球'
    },
    'zh-TW': {
      twoB: '二壘安打',
      threeB: '三壘安打',
      hr: '全壘打',
      hbp: '觸身球',
      sh: '犧牲觸擊',
      sf: '犧牲飛球',
      era: '防禦率'
    },
    en: {
      twoB: 'Doubles',
      hr: 'Home Runs',
      hbp: 'Hit by Pitch',
      sh: 'Sacrifice Bunts',
      sf: 'Sacrifice Flies',
      pg: 'Shutouts'
    }
  };
  for (const [locale, pairs] of Object.entries(expected)) {
    for (const [key, value] of Object.entries(pairs)) {
      assert.equal(messages[locale].glossary.term[key].fullName, value, `${locale} ${key}`);
    }
  }
  assert.equal(messages.en.glossary.term.pg.shortLabel, 'SHO');
  assert.equal(messages.en.glossary.term.tcPct.shortLabel, 'FPCT');
  assert.equal(messages.en.stats.colEn.pendingR, 'PEND');
});
