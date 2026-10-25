// 静态门禁：联赛收费能力 + 赛事级特殊规则入口（Issue #275，2026-10-24 口径）
// 运行：node --test tests/eventSpecialRules.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');

test('赛事编辑弹窗接入特殊规则组件，并按能力/既有规则决定可用性', () => {
  const src = read('../src/views/admin/Events.js');
  for (const need of [
    'EventSpecialRuleFields',
    'hasEventRules',
    'leagueCapabilityEnabled',
    'specialRulesAvailable',
    'specialRulesHint',
    'E.get(n.id)',
    'Ke.specialRules(n)',
    'D.value&&specialRulesAvailable.value'
  ]) {
    assert.ok(src.includes(need), `缺少接线：${need}`);
  }
});

test('联赛编辑弹窗内联收费能力开关，旧二级弹窗/行内入口下线', () => {
  const src = read('../src/views/admin/Leagues.js');
  assert.ok(src.includes('特殊规则能力'), '缺少收费能力入口文案');
  assert.ok(src.includes('business:league:specialRule'), '缺少收费能力权限门禁');
  assert.ok(src.includes('specialRulesEnabled'), '缺少能力开关绑定');
  assert.ok(src.includes('loadSpecialRules'), '缺少打开编辑时加载能力');
  assert.ok(src.includes('specialRulesLoading'), '缺少加载中状态');
  assert.ok(src.includes('specialRulesRequestSeq'), '缺少请求序号防跨联赛覆盖');
  assert.ok(src.includes('updateSpecialRules'), '缺少保存时写入能力');
  assert.ok(!src.includes('openSpecialRules'), '旧二级弹窗入口应移除');
  assert.ok(!src.includes('ruleDlg'), '旧二级弹窗状态应移除');
  assert.ok(!src.includes('特殊规则能力：'), '旧二级弹窗标题应移除');
  assert.ok(!src.includes('extraBatter'), '联赛级旧实际规则 UI 应移除');
  assert.ok(!src.includes('forceHalf'), '联赛级旧实际规则 UI 应移除');
});

test('赛事特殊规则组件包含两个开关与未开放提示', () => {
  const src = read('../src/components/admin/EventSpecialRuleFields.vue');
  for (const need of ['extraBatter', 'forceHalf', 'available', 'hint', 'el-switch']) {
    assert.ok(src.includes(need), `缺少组件要素：${need}`);
  }
});
