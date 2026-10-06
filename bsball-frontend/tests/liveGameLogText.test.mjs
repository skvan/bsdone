// LiveGame 日志文案标准（位置+名字(背号)+场面）——静态回归断言
// 标准（负责人确认）：每行要交代清楚位置和名字以及场面——安全/出局/推进到几垒/得分
// 运行：node --test tests/liveGameLogText.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
const layout = fs.readFileSync(new URL('../src/utils/liveGameFieldLayout.js', import.meta.url), 'utf8');

test('统一助手 logPlayerName()：名字 + 背号（(#N)，缺号安全降级）', () => {
  const i = src.indexOf('function logPlayerName(e){');
  assert.ok(i >= 0, 'nb 存在');
  const body = src.slice(i, src.indexOf('\n}', i) + 1);
  assert.match(body, /e\?\.name\?\?"跑者"/, '名字缺省跑者');
  assert.match(body, /\(#\$\{e\.number\}\)/, '背号渲染');
});

test('跑者行 Ru/Bs：位置前缀 + 名(背号)；安全进垒显式「— 安全」', () => {
  assert.match(src, /function Ru\(u,f\)\{\r?\n\s*return`\$\{jv\[u\]\} \$\{logPlayerName\(f\)\}`/, 'Ru 用 logPlayerName');
  assert.match(src, /function Bs\(u,f,l\)\{\r?\n\s*return`\$\{Ru\(u,f\)\} 进\$\{l\} — 安全`/, 'Bs 含 — 安全');
});

test('工具 buildRunnerAdvanceText：安全进垒统一「— 安全」（留在/得分分支不变）', () => {
  assert.match(layout, /进\$\{C\[n\]\} — 安全`/, '进垒分支含 — 安全');
  assert.doesNotMatch(layout, /回本垒得分\} — 安全/, '得分分支不加（得分已明确）');
});

test('打席行自动注入：第N棒 名 (#背号):', () => {
  assert.match(src, /第\$\{M\}棒 \$\{R\.name\}\$\{V\}:/, '棒次注入含背号');
  assert.match(src, /R\.number!=null&&R\.number!==""\?` \(#\$\{R\.number\}\)`:""/, '背号拼装');
});

test('拖拽出局行：垒位全程 + 名(背号) + 出局原因', () => {
  assert.match(src, /拖拽进垒 · 出局 · \$\{Ru\(s,a\)\} 试图进/, '拖拽出局含 from/目标垒与名(背号)');
});

test('换投/互换/补位行：名字带背号（logPlayerName 统一）', () => {
  assert.match(src, /pc\(Y,logPlayerName\(P\)\)/, '换投日志含背号');
  assert.match(src, /fc\(b,logPlayerName\(t\),Y,Y,logPlayerName\(T\),b\)/, '互换日志含背号');
  assert.match(src, /`\$\{b\} \$\{logPlayerName\(T\)\} 补位`/, '补位日志含背号');
});

test('击球出局行：守备位名 + 守备员名(背号)（Un2 注入名字串）', () => {
  assert.match(src, /function Un2\(e,t\)\{/);
  const i2 = src.indexOf('function Un2(e,t){');
  const body2 = src.slice(i2, src.indexOf('\n    }', i2));
  assert.match(body2, /Vt\(V\)/, '按位置解析守备员');
  assert.match(body2, /`\$\{ya\[V\]\?\?V\} \$\{logPlayerName\(Q\)\}`/, '守位名+名(背号)');
  assert.match(src, /Un2\(e,t\)\|\|/, '出局行调用 Un2');
  assert.match(src, /function Un\(u,f,g\)\{[\s\S]*?Array\.isArray\(g\)&&g\.length===l\.length\?g\.join\("→"\)/, 'Un 支持名字串（长度对齐保护）');
});

test('触身/触杀挂接：ac() 行含跑者名(背号)与守备员名(背号)', () => {
  const i = src.indexOf('function ac(e){');
  const body = src.slice(i, src.indexOf('D(b)', i));
  assert.match(body, /logPlayerName\(a\)/, '跑者名(背号)');
  assert.match(body, /— \$\{logPlayerName\(m\)\}/, '守备员名(背号)');
});

test('触身/四坏推进尾串：强制进垒规则（一垒无人时二垒跑者不生成 2B→3B）', () => {
  const i = src.indexOf('function ei(){');
  const body = src.slice(i, src.indexOf('function ca(){', i));
  assert.match(body, /e&&t&&s\.push\(bn\(2,/, '2B→3B 仅在 1B 有人时生成');
  assert.doesNotMatch(body, /,t&&s\.push\(bn\(2,/, '旧的无条件 2B→3B 形态不得残留');
});

test('评审回归：棒次注入带背号后，_f 匹配与 Watch 解析先归一 (#N)（防「上一棒」合并路径失效）', () => {
  assert.ok(src.includes('const _=m.batterName?.replace(/\\s*\\(#\\d+\\)$/,"")'), '_f 先剥离背号再比较');
  assert.ok(src.includes('_&&_!==e.batterName'), '_f 使用归一后名字比较');
  assert.ok(!src.includes('!(e.batterName&&m.batterName&&m.batterName!==e.batterName)'), '旧严格相等守卫不得残留');
  const watch = fs.readFileSync(new URL('../src/views/admin/LiveGameWatch.js', import.meta.url), 'utf8');
  assert.ok(watch.includes('(a.batterName??"").replace(/\\s*\\(#\\d+\\)$/,"")'), 'Watch $e 比较前同样剥离背号');
});

test('评审采纳：ac() 统一 logPlayerName；TAG 出局可用名字串', () => {
  assert.ok(src.includes('${logPlayerName(a)}: ${y}出局'), 'ac 行统一名(背号)与跑者降级');
  assert.ok(src.includes('o||nu([S])'), 'TAG 分支优先名字串（缺省回退数字形态）');
});
