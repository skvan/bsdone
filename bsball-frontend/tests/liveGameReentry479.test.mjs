// #479 垒球再上场：赛事开关、默认关闭、同棒次与实时录入接线（静态护栏）。
// 运行：node --test tests/liveGameReentry479.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
const LIVE = read('../src/views/admin/LiveGame.js');
const SUB = read('../src/utils/liveSubstitutionMode.js');
const DRAG = read('../src/utils/liveSubDrag.js');
const EVENTS = read('../src/views/admin/Events.js');
const RULE_FIELDS = read('../src/components/admin/EventSpecialRuleFields.vue');

function sliceFunction(source, header) {
  const at = source.indexOf(header);
  assert(at >= 0, `找不到 ${header}`);
  const open = source.indexOf('{', at);
  let depth = 0;
  for (let i = open; i < source.length; i++) {
    const c = source[i];
    if (c === '"' || c === "'" || c === '`') {
      for (i++; i < source.length; i++) {
        if (source[i] === '\\') i++;
        else if (source[i] === c) break;
      }
      continue;
    }
    if (c === '/' && source[i + 1] === '/') {
      while (i < source.length && source[i] !== '\n') i++;
      continue;
    }
    if (c === '/' && source[i + 1] === '*') {
      i = source.indexOf('*/', i) + 1;
      continue;
    }
    if (c === '{') depth++;
    else if (c === '}' && --depth === 0) return source.slice(at, i + 1);
  }
  throw new Error(`${header} 括号不闭合`);
}

test('赛事特殊规则：垒球赛事才显示再上场开关，默认关闭', () => {
  assert.ok(RULE_FIELDS.includes('v-if="gameMode === \'SOFTBALL\'"'), '缺少仅垒球显示的 v-if');
  assert.ok(RULE_FIELDS.includes('softballReentry'), '缺少 softballReentry 开关绑定');
  assert.ok(RULE_FIELDS.includes("gameMode: { type: String, default: 'BASEBALL' }"), '缺少 gameMode prop');
  assert.ok(EVENTS.includes('softballReentry:!1'), '表单初值应默认关闭');
  assert.ok(EVENTS.includes('softballReentry:t.softballReentry===!0'), '详情加载应映射 softballReentry');
  assert.ok(EVENTS.includes('softballReentry:o.specialRules.softballReentry===!0'), '保存应携带 softballReentry');
  assert.ok(EVENTS.includes('gameMode:o.gameMode'), '特殊规则组件应接收 gameMode');
});

test('实时录入：规则默认关闭，Ki/替补席/净状态统一走 reentryAllowed', () => {
  assert.ok(LIVE.includes('softballReentryOn=x(!1)'), '规则状态应默认关闭');
  assert.ok(LIVE.includes('softballReentryOn.value=ev?.specialRules?.softballReentry===!0'), '事件加载应读取 softballReentry');
  assert.ok(LIVE.includes('function softballReentryAllowed(){'), '缺少 softballReentryAllowed()');
  assert.ok(LIVE.includes('if(N.value==="SOFTBALL"&&softballReentryOn.value)return;'), 'Ki 应按开关放行再上');
  assert.ok(LIVE.includes('buildLiveBenchItems(bench,effectiveUnavailable(teamKey),N.value,softballReentryAllowed())'), '替补席应按开关灰显');
  assert.ok(LIVE.includes('filter(p=>pitcherId<=0||Number(p.id)!==pitcherId)'), '替补席应隐藏当前守备投手（#678）');
  assert.ok(/applyNetTeamState\(before\[side\],after\[side\],diffs\[index\],\{\s*sport:N\.value,reentryAllowed:softballReentryAllowed\(\)/.test(LIVE), '净状态应按开关落不可再上');
  assert.ok(LIVE.includes('if(N.value==="SOFTBALL"&&softballReentryOn.value)return t;'), '旧换人浮层替补/阵容过滤应按开关');
  assert.ok(SUB.includes("reentryAllowed = String(sport).toUpperCase() === 'SOFTBALL'"), '净状态应支持显式 reentryAllowed');
  assert.ok(DRAG.includes('!reentryAllowed && blocked.has(String(p.id))'), '替补灰显应使用 reentryAllowed');
  assert.ok(SUB.includes('export function effectiveUnavailableIds'), '缺少有效不可再上名单回填');
  assert.ok(SUB.includes('export function findReentryInTransaction'), '缺少同事务再上场拦截');
  assert.ok(LIVE.includes('function effectiveUnavailable(side){'), '缺少 effectiveUnavailable 页面接线');
  assert.ok(LIVE.includes('substitutionModeBefore.value?.[side]'), '模式内有效名单应取进入前状态（允许中间犹豫）');
  assert.ok(LIVE.includes('effectiveUnavailable(teamKey)'), '替补席应使用有效不可再上名单');
  assert.ok(LIVE.includes('findReentryInTransaction(diffs,{reentryAllowed:softballReentryAllowed()})'), '结束时缺少同事务再上场拦截');
});

test('同棒次：cc() 的替补上场路径必须调用 validateReentrySlot', () => {
  const cc = sliceFunction(LIVE, 'function cc(e){');
  assert.ok(cc.includes('validateReentrySlot({'), 'cc() 未接同棒次校验');
  assert.ok(cc.includes('targetSlot:y.findIndex(P=>Number(P.id)===Number(t?.id))'), '目标槽位应取被换下球员所在 lineup 槽位');
  assert.ok(cc.includes('mt.value==="fielder"&&!e.groupKey'), '仅替补上场路径校验（守备互换不校验）');
  assert.ok(cc.includes('effectiveUnavailable(a)'), '关闭态应使用有效不可再上名单拦截');
  assert.ok(SUB.includes('export function validateReentrySlot'), '缺少 validateReentrySlot');
  assert.ok(SUB.includes('export function originalLineupSlot'), '缺少 originalLineupSlot');
});
