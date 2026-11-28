// 静态断言：Issue #347 纸质表单字段对齐（PR A）
// 运行：node --test tests/formFields347.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { resolveAtBats } from '../src/utils/gamePlayerStat.js';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const games = read('../src/views/admin/Games.js');
const detail = read('../src/components/admin/GameDetailContent.js');
const player = read('../src/components/admin/PlayerDetailContent.js');
const statsCols = read('../src/stores/statsColumns.js');
const storage = read('../src/utils/gameDetailFieldSettingsStorage.js');
const fieldingTable = read('../src/components/admin/GameFieldingTable.vue');
const fieldingMerge = read('../src/utils/gameResultFieldingMerge.js');
const defaults = read('../src/utils/gamePlayerStat.js');
const tableMeta = read('../src/composables/useStatsTableMeta.js');

const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en', 'ja', 'ko'].map((name) => [name, JSON.parse(read(`../src/locales/${name}.json`))])
);

test('管理端打者列拆分：GDP/SH/SF/BB/IBB/HB/IO', () => {
  for (const key of ['gdp', 'sh', 'sf', 'bb', 'ibb', 'hbp', 'io']) {
    assert.ok(games.includes(`key:"${key}"`), `打者列缺少 ${key}`);
  }
  assert.ok(games.includes('key:"decision"'), '投手列缺少 W/L/SV 三选一');
  assert.ok(games.includes('function decisionValue'), '缺少 decisionValue');
  assert.ok(games.includes('function applyDecision'), '缺少 applyDecision');
});

test('管理端守备页签与字段组件已挂载', () => {
  assert.ok(games.includes("import GameFieldingTable"), '未导入 GameFieldingTable');
  assert.ok(games.includes('label:"守备数据"'), '缺少守备数据页签');
  assert.ok(games.includes('fieldingStatsAway'), '缺少客队守备 statsFor');
  assert.ok(games.includes('fieldingStatsHome'), '缺少主队守备 statsFor');
});

test('结果录入守备表合并逐位置数据并纳入投手/DH取消球员', () => {
  for (const needle of [
    'il.listFieldingPositions(pe.value)',
    'fieldingPositionRows',
    'function mergeFieldingRows',
    'resultFieldingRows(le.value)',
    'resultFieldingRows(te.value)',
    '!t._fieldingOnly',
    'fo=le.value.filter(f=>f._fieldingOnly)',
    'fo=te.value.filter(f=>f._fieldingOnly)'
  ]) {
    assert.ok(games.includes(needle), `结果录入守备表缺少 ${needle}`);
  }
  // 合并口径已抽到 gameResultFieldingMerge.js（Issue #701），相关行为在工具内断言
  for (const needle of [
    'target._fieldPosition',
    'stats.battingOrder == null && stats.listOrder == null',
    'row._fieldingOnly = true'
  ]) {
    assert.ok(fieldingMerge.includes(needle), `守备合并工具缺少 ${needle}`);
  }
  assert.ok(fieldingTable.includes('row._fieldPosition || row.position'), '守备表 POS 未使用合并后的守位');
});

test('管理端保存/导入导出包含新字段', () => {
  for (const needle of [
    'io:S?.io',
    'pitchDoubles:v?.pitchDoubles??0',
    'pitchTriples:v?.pitchTriples??0',
    'pitchSh:v?.pitchSh??0',
    'pitchSf:v?.pitchSf??0',
    'pitchIo:v?.pitchIo??0',
    'pitchBbHp:(Number(v?.pitchBb)||0)+(Number(v?.pitchHbp)||0)',
    'bbHp:(Number(u?.bb)||0)+(Number(u?.hbp)||0)'
  ]) {
    assert.ok(games.includes(needle), `保存/导入导出缺少 ${needle}`);
  }
});

test('门户比赛详情打者/投手列已拆并补新字段', () => {
  for (const key of ['gdp', 'sh', 'sf', 'bb', 'ibb', 'hbp', 'io']) {
    assert.ok(detail.includes(`key:"${key}"`), `详情打者列缺少 ${key}`);
  }
  for (const key of ['pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo']) {
    assert.ok(storage.includes(`key: '${key}'`), `详情投手列缺少 ${key}`);
  }
  assert.ok(storage.includes("key: 'pitchPa', label: 'PA', visible: false"), 'PA 应默认隐藏但保留设置项（#211）');
  assert.ok(!storage.includes("key: 'pitchBbHp'"), '投手表仍保留 BB+HP 合并列');
});

test('球员详情/数据排行列为新字段', () => {
  for (const prop of ['gdp', 'sh', 'sf', 'bb', 'ibb', 'hbp', 'io', 'pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo']) {
    assert.ok(player.includes(`prop:"${prop}"`), `球员详情缺少 ${prop}`);
  }
  for (const key of ['io', 'pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo']) {
    assert.ok(statsCols.includes(`key:"${key}"`), `数据排行缺少 ${key}`);
  }
});

test('投手 AB 回退不重复扣 IBB，并兼容旧 BBHP 合计', () => {
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 2, pitchIbb: 1, pitchHbp: 1 }), 7);
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 0, pitchHbp: 0, pitchBbHp: 3 }), 7);
  assert.equal(resolveAtBats({ pitchBf: 10, pitchAb: 8 }), 8);
});

test('投手 AB 扣除 SH/SF（#702）', () => {
  // BF10/BB2/HBP1/SH1/SF1 → 10−2−1−1−1 = 5
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 2, pitchHbp: 1, pitchSh: 1, pitchSf: 1 }), 5);
  // 仅 SH：10−2−1−1 = 6
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 2, pitchHbp: 1, pitchSh: 1 }), 6);
  // 仅 SF：10−2−1−1 = 6
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 2, pitchHbp: 1, pitchSf: 1 }), 6);
  // 旧 BBHP 合计回退 + SH/SF：10−3−0−1−1 = 5
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 0, pitchHbp: 0, pitchBbHp: 3, pitchSh: 1, pitchSf: 1 }), 5);
  // 缺省/空值按 0 处理，不影响原口径
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 2, pitchHbp: 1, pitchSh: null, pitchSf: undefined }), 7);
  // 下限截断为 0
  assert.equal(resolveAtBats({ pitchBf: 2, pitchBb: 2, pitchHbp: 1, pitchSh: 1, pitchSf: 1 }), 0);
  // 有 pitchAb 时仍优先（人工覆写）
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 2, pitchHbp: 1, pitchSh: 1, pitchSf: 1, pitchAb: 6 }), 6);
});

test('投手 AB 公式全链路保持 SH/SF 一致（#702）', () => {
  const mapperXml = read('../../bsball-backend/src/main/resources/mapper/PlayerStatsMapper.xml');
  const from = mapperXml.indexOf('id="pitchAbEffectiveCol"');
  assert.ok(from > 0, 'Mapper 缺少 pitchAbEffectiveCol');
  const abFragment = mapperXml.slice(from, mapperXml.indexOf('</sql>', from));
  assert.ok(abFragment.includes('s.pitch_sh'), 'Mapper AB 回退未减 pitch_sh');
  assert.ok(abFragment.includes('s.pitch_sf'), 'Mapper AB 回退未减 pitch_sf');

  const drill = read('../../bsball-backend/src/main/java/com/bsball/stats/PlayerStatDrillPitchingMetric.java');
  const abLine = drill.split('\n').find((line) => line.includes('PITCH_AB(')) || '';
  assert.ok(abLine.includes('s.pitch_sh') && abLine.includes('s.pitch_sf'), '下钻 PITCH_AB 未减 SH/SF');
  // #702 历史库值动态修正：空值→新公式；库值=旧公式且 SH/SF>0→新公式；其他→保留
  for (const marker of ['s.pitch_ab IS NULL', 's.pitch_ab =', 'ELSE s.pitch_ab END']) {
    assert.ok(abFragment.includes(marker), `Mapper AB 缺少历史修正分支：${marker}`);
    assert.ok(abLine.includes(marker), `下钻 PITCH_AB 缺少历史修正分支：${marker}`);
  }
  // #702 回填同步：0 值兜底分支，SQL 与 Java 钻取必须同样具备
  assert.ok(abFragment.includes('s.pitch_ab = 0 AND'), 'Mapper AB 缺少 0 值兜底分支');
  assert.ok(abLine.includes('s.pitch_ab = 0 AND'), '下钻 PITCH_AB 缺少 0 值兜底分支');

  assert.ok(player.includes('resolvePitchAb'), '球员详情应复用共享 resolveAtBats');
  assert.ok(!player.includes('const a=Number(e.pitchBf)||0'), '球员详情不应保留内联 AB 公式');
  assert.ok(games.includes('BF−BB−HBP−SH−SF'), '结果录入 AB 列提示应为新口径');
  // D：BF 缺失时由 AB 反推，BB 已含 IBB，不再单加 pitchIbb
  const taBody = (games.match(/function ta\(t\)\{[^}]*\}/) || [''])[0];
  assert.ok(taBody.includes('pitchSh') && taBody.includes('pitchSf'), 'ta() 未计入 SH/SF');
  assert.ok(!taBody.includes('pitchIbb'), 'ta() 不应再加 IBB（BB 已含）');
  // C：保存走 toSavePitchAb，禁止默认 0
  assert.ok(games.includes('pitchAb:p?savePitchAb(v)'), '保存载荷应使用 toSavePitchAb');
  assert.ok(!games.includes('Math.floor(Number(v?.pitAb??0))'), '不应再直接写 pitAb（默认 0）');

  const sacrifice = { 'zh-CN': /牺牲/, 'zh-TW': /犧牲/, en: /sacrifice/i, ja: /犠(打|飛)/, ko: /희생/ };
  // 按 key 收集所有 AB 说明；纯列名（无公式线索）除外，避免 ja/ko 的意译公式被漏检
  const FORMULA_HINT = /BF|[−-]|minus|扣[除掉]|から|에서|빼|faced|타자수/i;
  const collectAbTips = (node, out = []) => {
    if (!node || typeof node !== 'object') return out;
    for (const [key, value] of Object.entries(node)) {
      if ((key === 'pitAb' || key === 'pitchAb') && typeof value === 'string' && FORMULA_HINT.test(value)) out.push(value);
      else collectAbTips(value, out);
    }
    return out;
  };
  for (const [name, dict] of Object.entries(locales)) {
    const tips = collectAbTips(dict);
    assert.ok(tips.length >= 3, `${name} 缺少 AB 公式文案`);
    for (const tip of tips) {
      assert.ok(/SH/.test(tip) || sacrifice[name].test(tip), `${name} AB 文案未含牺牲打：${tip}`);
      assert.ok(!/BF\s*[−-]\s*BB\s*[−-]\s*IBB/.test(tip), `${name} AB 文案残留旧口径：${tip}`);
      assert.ok(!/minus walks\/IBB\/HBP/.test(tip), `${name} AB 文案残留旧口径：${tip}`);
    }
  }
});

test('统计表列名：pitchIbb/pitchHbp 映射到 IBB/HBP 词典', () => {
  assert.ok(tableMeta.includes("pitchIbb: 'ibb'"));
  assert.ok(tableMeta.includes("pitchHbp: 'hbp'"));
});

test('默认值与五语种词条', () => {
  assert.ok(defaults.includes('gdp:0'));
  assert.ok(defaults.includes('io:0'));
  assert.ok(defaults.includes('pitchDoubles:0'));
  assert.ok(defaults.includes('pitchIo:0'));
  for (const [name, dict] of Object.entries(locales)) {
    for (const key of ['io', 'pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo']) {
      assert.ok(dict.stats?.col?.[key], `${name} stats.col 缺少 ${key}`);
    }
    for (const key of ['sh', 'sf', 'ibb', 'hbp', 'io', 'pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIbb', 'pitchIo']) {
      assert.ok(dict.player?.seasonColTip?.[key], `${name} seasonColTip 缺少 ${key}`);
    }
  }
});

test('Excel 导入投手 AB 显式标记人工，保存不被公式覆盖（#702 跟修）', () => {
  const marker = '...b.pitAb!=null&&{pitAb:b.pitAb,abManual:!0}';
  assert.equal(games.split(marker).length - 1, 2, '主/客投手导入带 pitAb 时都应标记 abManual');
  assert.ok(!games.includes('...b.pitAb!=null&&{pitAb:b.pitAb}'), '存在未标记 abManual 的导入 AB 行');
  const mapperXml = read('../../bsball-backend/src/main/resources/mapper/PlayerStatsMapper.xml');
  assert.ok(mapperXml.includes('无法完全区分'), 'Mapper CASE 注释应声明无持久人工标记的局限');
  const state = read('../src/utils/pitchAbState.js');
  assert.ok(state.includes('无法完全区分'), 'pitchAbState 注释应声明无持久人工标记的局限');
});
