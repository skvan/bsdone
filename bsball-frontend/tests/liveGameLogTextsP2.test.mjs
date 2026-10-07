// P2 批次1 回归断言（#223 接杀回传双杀 / #224 RBI 打点注记）
// 运行：node --test tests/liveGameLogTextsP2.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { advanceText, outText } from '../src/utils/liveGameLogTexts.js';

const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('advanceText：#224 打点注记（rbiNote 仅 to=4 得分分支生效）', () => {
  assert.equal(
    advanceText({ reasonCn: '安打得分', runner: { name: '梁志伟', number: 37 }, from: 3, to: 4, rbiNote: '麦政鸿：1打点' }),
    '安打得分：三垒跑者 梁志伟 (#37) 回本垒得分（麦政鸿：1打点）'
  );
  assert.equal(
    advanceText({ reasonCn: '盗垒成功 (SB)', runner: { name: 'Gytis', number: 8 }, from: 1, to: 2, rbiNote: '麦政鸿：1打点' }),
    '盗垒成功 (SB)：一垒跑者 Gytis (#8) 进二垒 — 安全'
  );
  assert.equal(
    advanceText({ reasonCn: '野选推进得分', runner: { name: '王一', number: 23 }, from: 3, to: 4, rbiNote: '麦政鸿：1打点' }),
    '野选推进得分：三垒跑者 王一 (#23) 回本垒得分（麦政鸿：1打点）'
  );
});

test('outText：#223 接杀回传双杀注记（链内融合 / 无链兜底 / 无 note 不回归）', () => {
  assert.equal(
    outText({ reasonCn: '离垒过远出局', runner: { name: '李四', number: 21 }, from: 2, to: 2, chain: ['1', '6'], note: '接杀回传双杀 DP', outsCum: 2, total: 2 }),
    '离垒过远出局：二垒跑者 李四 (#21) 于二垒出局（1-6，接杀回传双杀 DP），本棒次2人出局，已累计2人出局'
  );
  assert.equal(
    outText({ reasonCn: '离垒过远出局', runner: { name: '李四', number: 21 }, from: 2, to: 2, note: '接杀回传双杀 DP', outsCum: 1, total: 1 }),
    '离垒过远出局：二垒跑者 李四 (#21) 于二垒出局（接杀回传双杀 DP），本棒次1人出局，已累计1人出局'
  );
  assert.equal(
    outText({ reasonCn: '离垒过远出局', runner: { name: '李四', number: 21 }, from: 2, to: 2, outsCum: 1, total: 1 }),
    '离垒过远出局：二垒跑者 李四 (#21) 于二垒出局，本棒次1人出局，已累计1人出局'
  );
});

test('LiveGame.js：#223 CATCH_BACK_DP 接入点齐备（置位/清除/kp/Ro/mf/bf/提示）', () => {
  assert.match(src, /e!=="G"&&\(l\.abCatchRecorded=\{/, 'bt G/F/L/TAG 接杀置位（非 G）');
  assert.match(src, /Dn\(t\),l\.abCatchRecorded=\{\r?\n\s+pos:t\[0\]\?\?null/, 'bt SF 接杀置位');
  assert.match(src, /function bt\(e,t,a\)\{\r?\n\s+l\.abCatchRecorded=null;/, 'bt 开头清除');
  assert.match(src, /function Wt\(e\)\{\r?\n\s+l\.abCatchRecorded=null;/, 'Wt（投球）开头清除');
  assert.match(src, /function Hf\(\)\{\r?\n\s+l\.abCatchRecorded=null;/, 'Hf（故意四坏）开头清除');
  assert.match(src, /e\.id==="out_last"&&l\.abCatchRecorded&&l\.abCatchRecorded\.inning===l\.inning/, 'kp out_last 分支（含半局校验）');
  assert.match(src, /if\(a\?\.type==="CATCH_BACK_DP"\)\{/, 'Ro 完成分支');
  assert.ok(src.includes('"PICKOFF_OUT","CATCH_BACK_DP"].includes'), 'mf 集合含新类型');
  assert.ok(src.includes('"PICKOFF_OUT","CATCH_BACK_DP","G"'), 'bf 集合含新类型');
  assert.ok(src.includes('CATCH_BACK_DP")return"接杀回传双杀：点击回踩垒守备员"'), '流程提示文案');
  assert.match(src, /Dn\(cbChain\)/, 'Dn 记账（A=链首接杀者 / PO=链末踩垒者）');
  assert.match(src, /note:"接杀回传双杀 DP"/, 'outText note 注入');
});

test('LiveGame.js：#224 RBI 注记接入点（pu/fp/HR/IPHR/SF/上一棒/记账缺口）', () => {
  assert.match(src, /l\.addLog\(LgAdvance\(\{\r?\n\s+reasonCn:"安打得分",runner:N,from:3,to:4,rbiNote:Rb\?/, 'pu 安打得分注记（含计数）');
  assert.match(src, /reasonCn:"野选推进得分",runner:p,from:3,to:4,rbiNote:/, 'fp 野选推进得分注记');
  assert.match(src, /hn\("HR",b\)\}（\$\{r\.name\}：\$\{b\}打点）/, 'HR 注记');
  assert.match(src, /hn\("IPHR",b\)\}（\$\{r\.name\}：\$\{b\}打点）/, 'IPHR 注记');
  assert.match(src, /\$\{sfRbi\?`（\$\{r\.name\}：1打点）`:""\}/, 'SF 得分注记（条件化）');
  assert.match(src, /rbiWho=creditLastBatterRbi\(\)/, '上一棒内推进打点人捕获');
  assert.match(src, /wp\(a,s,r,e\.cn,e\.id,rbiWho\?/, 'wp 注记注入');
  assert.match(src, /fc_forced\[3\]\?\(D\(LgAdvance\(\{\r?\n\s+reasonCn:"野选推进得分",runner:fc_old\[3\],from:3,to:4,rbiNote:/, 'fcApply 被迫得分：先落文本（含打点注记）');
  assert.match(src, /\)\),Ct\(1\),r\.stats\.batting\.rbi=/, '随后 Ct(1)+RBI 记账');
});
