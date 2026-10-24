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

test('联赛编辑弹窗提供收费能力入口，旧行内入口下线', () => {
  const src = read('../src/views/admin/Leagues.js');
  assert.ok(src.includes('特殊规则能力'), '缺少收费能力入口文案');
  assert.ok(src.includes('business:league:specialRule'), '缺少收费能力权限门禁');
  assert.ok(src.includes('ruleForm.specialRulesEnabled'), '缺少能力开关绑定');
  assert.ok(!src.includes('openSpecialRules(c)'), '旧行内特殊规则入口应移除');
  assert.ok(!src.includes('extraBatter'), '联赛级旧实际规则 UI 应移除');
  assert.ok(!src.includes('forceHalf'), '联赛级旧实际规则 UI 应移除');
});

test('赛事特殊规则组件包含两个开关与未开放提示', () => {
  const src = read('../src/components/admin/EventSpecialRuleFields.vue');
  for (const need of ['extraBatter', 'forceHalf', 'available', 'hint', 'el-switch']) {
    assert.ok(src.includes(need), `缺少组件要素：${need}`);
  }
});
