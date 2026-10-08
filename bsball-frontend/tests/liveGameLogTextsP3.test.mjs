// P3 回归断言（#225 垒况数据链 / #226 守备 IP 结清 / F 系统类归口）
// 运行：node --test tests/liveGameLogTextsP3.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { sysText, errorAdvanceText, stealSingleText } from '../src/utils/liveGameLogTexts.js';

const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('sysText：F 系统类逐字（纠偏为 F2 统一前缀）', () => {
  assert.equal(sysText({ kind: 'adjust_score', detail: '暴力羊 第2局 -1分' }), '纠偏（得分）：暴力羊 第2局 -1分');
  assert.equal(sysText({ kind: 'adjust_bases', detail: '一垒 甲；二垒 乙；三垒 丙；本垒 丁' }), '纠偏（垒况）：一垒 甲；二垒 乙；三垒 丙；本垒 丁');
  assert.equal(sysText({ kind: 'third_out_note', runs: 2 }), '第三出局为封杀：本 play 2 分不计入');
  assert.equal(sysText({ kind: 'inning_header', inning: 5, isTop: false }), '--- 5局下 ---');
  assert.equal(sysText({ kind: 'switch_sides', team: 'Proguys', nextBatter: '陈志佳' }), '攻守交换，进攻方：Proguys，下一棒：陈志佳');
  assert.equal(sysText({ kind: 'np_backfill', who: 'Lee ju won' }), 'Lee ju won: 先前未记该球球数，已为本次击出/守备补记 1 球（NP）');
  assert.equal(sysText({ kind: 'dk3_guard', code: 'dk3:xx' }), '漏接三振：未识别代码 dk3:xx');
});

test('sysText：P3 续（系统行/占位/不合法投球逐字；未知 kind 空串显式断言）', () => {
  assert.equal(sysText({ kind: 'game_start' }), '比赛开始');
  assert.equal(sysText({ kind: 'game_end' }), '比赛已结束');
  assert.equal(sysText({ kind: 'force_half' }), '强制换局：本半局提前结束（特殊换局规则）');
  assert.equal(sysText({ kind: 'extend_inning' }), '进入延长局');
  assert.equal(sysText({ kind: 'tp_placeholder' }), '滚地球：三杀（示意）');
  assert.equal(sysText({ kind: 'illegal_pitch' }), '不合法投球：垒上跑者各推进一垒');
  assert.equal(sysText({ kind: 'illegal_pitch', detail: 'walk_ab_end' }), '不合法投球：已记为坏球；当前打席因四坏结束。');
  assert.equal(sysText({ kind: 'illegal_pitch', detail: 'walk_no_runner' }), '不合法投球：记为坏球（垒上无跑者，未推进）');
  assert.equal(sysText({ kind: 'illegal_pitch', detail: 'walk_no_advance' }), '不合法投球：记为坏球；跑者不推进（示意记录）');
  assert.equal(sysText({ kind: 'unknown_kind' }), '', '未知 kind 返回空串（default 行为显式断言）');
});

test('特例归口（纯迁移逐字）：失误推进 / 盗垒成功单选', () => {
  assert.equal(errorAdvanceText({ reasonCn: '牵制失误：投手 失误', runnerName: '赵子轩', to: '本垒得分' }), '牵制失误：投手 失误，赵子轩 进本垒得分');
  assert.equal(errorAdvanceText({ reasonCn: '守备失误：三垒手 失误', runnerName: '王一', to: '二垒' }), '守备失误：三垒手 失误，王一 进二垒');
  assert.equal(stealSingleText({ name: 'Gytis' }), 'Gytis: 盗垒成功');
  assert.equal(stealSingleText({ name: 'Gytis', home: true }), 'Gytis: 盗本垒成功');
});

test('#225：Zi() 打席上下文含 basesBefore 布尔快照（历史组垒包数据链）', () => {
  assert.match(src, /basesBefore:\{1:!!l\.runners\[1\],2:!!l\.runners\[2\],3:!!l\.runners\[3\]\},runnerIdsBefore:\{/, 'Zi 补 basesBefore');
  assert.match(src, /basesBefore:\{\s*\r?\n\s*\.\.\.m\.basesBefore/, 'Qr 继续从 m.basesBefore 展开');
});

test('#226：fielding.ip + Ipy 全员累进 + 换人 4 处注记', () => {
  assert.ok(src.includes('po:0,a:0,e:0,ip:0'), 'Pt().fielding 含 ip');
  assert.ok(src.includes('const addIp=(o,key)=>{'), 'Ipy addIp 重构');
  assert.ok(src.includes('addIp(Se.value,"fielding")'), '投手活体守备 IP（DH 制深拷贝规避）');
  assert.ok(src.includes('for(const m of Hn.value)if(m!==Se.value)addIp(m,"fielding")'), '守备全员按引用去重累进');
  assert.ok(src.includes('function fieldingIpNote(p){'), '结清注记 helper');
  assert.ok(src.includes('D(LgSub({kind:"run",inName:P.name,outName:t.name})+fieldingIpNote(t))'), '代跑注记');
  assert.ok(src.includes('D(LgSub({kind:"hit",inName:w.name,outName:t.name})+fieldingIpNote(t))'), '代打注记');
  assert.ok(src.includes('syncFieldingPitcher(a,t,P,fieldingIpNote(t))'), '换投注记');
  assert.ok(src.includes('+(b==="P"?"":fieldingIpNote(t)):'), '非 DH P 位替换：换人行不带注记（注记仅随换投行，评审 Major 1）');
});
