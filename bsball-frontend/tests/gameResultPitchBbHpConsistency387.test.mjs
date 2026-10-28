/**
 * #387 衍生问题：比赛结果页的投手四死「合计栏」与「拆分栏」必须同一口径
 *
 * 后端（GameService.applyResultPartToStat）加上 `!= null` 守卫后，「缺字段」不再等于「清零」，
 * 而是「保留既有值」。于是结果页 payload 里的一处非对称就会变成脏数据：
 *   pitchBbHp:(Number(v?.pitchBb)||0)+(Number(v?.pitchHbp)||0)   ← 恒为数字，清空输入框即算出 0
 *   pitchBb:v?.pitchBb, pitchIbb:v?.pitchIbb, pitchHbp:v?.pitchHbp ← 原样透传，清空即缺字段
 * 用户把已记录 BB=5 的投手在结果页清空 BB 栏保存：拆分栏被守卫**保留 5**，合计栏被**覆写 0**，
 * DB 得到 pitch_bb=5 / pitch_bb_hp=0 的自相矛盾行，读侧派生（StatsService 的「拆分优先、否则 bb_hp-hbp」）
 * 会跟着错。改动前两者一起被抹平（显示 0，至少自洽），所以这条是本守卫**新引入**的风险。
 *
 * 本档钉的是「四栏同进同出」这条不变量：结果页既然把合计栏算成数字，拆分栏就必须同样是数字，
 * 让后端的守卫只作用于真正不带这些字段的老客户端 / 部分写入载荷。
 *
 * 运行：cd bsball-frontend && node --test tests/gameResultPitchBbHpConsistency387.test.mjs
 * 判别力自检：GAMES_PATH=<未含 ??0 的 Games.js> node --test 本档，第 1、2 项应转红。
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const SRC_PATH = process.env.GAMES_PATH
  || join(dirname(fileURLToPath(import.meta.url)), '../src/views/admin/Games.js');
const src = readFileSync(SRC_PATH, 'utf8');

/** 结果页保存 payload 的锚点：合计栏由两根拆分栏派生，故一定与它们同一段 */
const ANCHOR = 'pitchBbHp:(Number(v?.pitchBb)||0)+(Number(v?.pitchHbp)||0)';
const line = src.split('\n').find((l) => l.includes(ANCHOR));
assert(line, '找不到结果页保存 payload 的 pitchBbHp 派生表达式，Games.js 结构可能已变动');

/** 抽出投手列的「键:表达式」原文段，绑上 p/v 后真的求值——测的是 payload 成品，不是字段清单 */
const START = 'ip:p?.ip??0';
const END = ',wp:v?.wp';
const si = line.indexOf(START);
const ei = line.indexOf(END, si);
assert(si >= 0 && ei > si, '投手 payload 段落边界变动，请同步本测试的锚点');
const segment = line.slice(si, ei);
const buildPayload = new Function('p', 'v', `return {${segment}}`);

/** 四根投手四死栏：守卫之后「不送」＝保留，所以只有「送数字」才是用户想要的清零 */
const FOUR = ['pitchBb', 'pitchIbb', 'pitchHbp', 'pitchBbHp'];

test('1 结果页清空 BB/HBP 输入框：合计与拆分一起归零，不留 bb=5 / bbHp=0 的矛盾行', () => {
  const saved = buildPayload({}, { pitchBb: 5 });        // 先正常存一次
  assert.equal(saved.pitchBb, 5);
  assert.equal(saved.pitchBbHp, 5);

  const cleared = buildPayload({}, { pitchBb: null });   // 用户清空输入框后保存
  assert.equal(cleared.pitchBb, 0, '拆分栏 BB 必须送数字：后端守卫会把「缺字段」解释成保留既有值');
  assert.equal(cleared.pitchBbHp, 0, '合计栏与拆分栏必须同进同出');
  assert.equal(cleared.pitchBb + cleared.pitchHbp, cleared.pitchBbHp, '合计恒等于拆分之和');
});

test('2 四栏在 payload 里恒为数字，绝不会因输入框为空而缺席', () => {
  for (const v of [{}, { pitchBb: 3 }, { pitchBb: 3, pitchIbb: 1, pitchHbp: 2 }, { pitchHbp: 0 }]) {
    const payload = buildPayload({}, v);
    for (const key of FOUR) {
      assert.ok(key in payload, `${key} 不应缺席（缺席＝后端保留旧值）`);
      assert.equal(typeof payload[key], 'number', `${key} 应为数字，实际 ${JSON.stringify(payload[key])}`);
      assert.ok(Number.isFinite(payload[key]), `${key} 不应是 NaN`);
    }
  }
});

test('3 静态护栏：拆分栏与派生合计栏同用 ??0，不得再回到原样透传', () => {
  for (const needle of ['pitchBb:v?.pitchBb??0', 'pitchIbb:v?.pitchIbb??0', 'pitchHbp:v?.pitchHbp??0']) {
    assert.ok(src.includes(needle), `缺少 ${needle}（应与同排 pitchSh/pitchSf/pitchIo 的 ??0 一致）`);
  }
  assert.ok(
    !/pitchBb:v\?\.pitchBb(?!(\?\?|\s*\}))/.test(src),
    '结果页又出现 pitchBb:v?.pitchBb 的裸透传；它和恒为数字的合计栏混用会写出不一致的四死数据'
  );
});

test('4 相邻口径不被顺手改动：撤銷/归零仍是送 0，np/pitchPa/pitchBf 仍原样透传', () => {
  const payload = buildPayload({ ip: 2 }, { pitchBb: 2, pitchHbp: 1, pitchIo: 3, np: 40 });
  assert.equal(payload.pitchBbHp, 3, '合计 = BB + HBP（不含 IBB，沿用既有派生口径）');
  assert.equal(payload.pitchIo, 3);
  assert.equal(payload.np, 40);
  assert.ok(!('pitchPa' in payload) || payload.pitchPa === undefined, 'pitchPa 仍可为缺省，本 PR 不扩范围');
});
