/**
 * #387 投手触身球（HBP）往返验证：载入 → 旧数据兼容回填 → 存档 payload
 *
 * 与 liveGamePitcherRemap387.test.mjs 的「静态白名单比对」互补：本档是把 LiveGame.js 里
 * 三段**原始码文字**绑上变量后真的执行一遍，验的是行为与最终数字，不是字段清单。
 *   Do() 的 pitching/batting/fielding 区块（T = 服务端 GamePlayerStat 行）
 *   ue() 的旧数据兼容回填（p / b）
 *   Eo() 的存档 payload 表达式（r / s / p）
 * 三段在源码里的变量名本就一致，可直接复用原文，无需重新实现逻辑。
 *
 * WHIP 一栏依 boxscore 口径计算，公式出处：
 *   components/admin/GameDetailContent.js:1245
 *   const t=Number(e?.pitchH)||0,l=(e?.pitchBb??null)!=null?Number(e?.pitchBb)||0:Number(e?.pitchBbHp)||0,d=Number(e?.ip)||0
 *
 * 运行：cd bsball-frontend && node --test tests/liveGameHbpRoundTrip387.test.mjs
 * 判别力自检：LIVEGAME_PATH=<未含修复的 LiveGame.js> node --test 本档，第 2 项起应转红。
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const SRC_PATH = process.env.LIVEGAME_PATH
  || join(dirname(fileURLToPath(import.meta.url)), '../src/views/admin/LiveGame.js');
const src = readFileSync(SRC_PATH, 'utf8');

const PAYLOAD_ANCHOR = 'isPitcher:a.isPitcherRow?1:0';
const payloadLine = src.split('\n').find((l) => l.includes(PAYLOAD_ANCHOR));
assert(payloadLine, '找不到 Eo() 存档 payload 行，元件结构可能已变动');

/** 取 Do() 重建区块：必须从 payload 行之后搜，否则会误抓 Pt() 的零值模板 */
function doBlock(ns) {
  const start = src.indexOf(ns + ':{', src.indexOf(PAYLOAD_ANCHOR));
  if (start < 0) throw new Error('找不到 Do() 的 ' + ns + ' 区块');
  const open = start + ns.length + 1;
  for (let i = open, depth = 0; i < src.length; i++) {
    if (src[i] === '{') depth++;
    else if (src[i] === '}' && --depth === 0) return src.slice(open + 1, i);
  }
  throw new Error(ns + ' 区块括号不闭合');
}

/** 取 payload 中引用某本地变量的键→表达式原文（字面常数如 pitchR:0 会被排除） */
function payloadExprs(keys, varName) {
  const re = new RegExp('[,{](' + keys + '|[a-zA-Z_]+):([^,{}]*)', 'g');
  const out = [];
  for (const [, key, expr] of payloadLine.matchAll(re)) {
    if (new RegExp('^(' + keys + ')([A-Za-z_]*|)$').test(key) && expr.includes(varName + '.')) out.push(key + ':' + expr);
  }
  if (!out.length) throw new Error('payload 里找不到 ' + keys + ' 表达式');
  return out;
}
const asPayload = (pairs, arg) => new Function(arg, 'return {' + pairs.join(',') + '}');

/** 存档侧三个建构器：全部用源码原文 */
const savePitching = (r) => asPayload(payloadExprs('pitch[A-Za-z_]*|ip|np|go|fo|gs|wp|bk', 'r'), 'r')(r);
const saveBatting = (s) => asPayload(payloadExprs('bb|bbHp|hbp|ab|gdp|ibb|io|soSwing|soLooking', 's'), 's')(s);
const saveFielding = (p) => asPayload(payloadExprs('defInn|dp|pb|catcher[A-Za-z_]*', 'p'), 'p')(p);

/** ue() 的旧数据兼容回填（原文两行） */
const pitcherCompat = src.split('\n').find((l) => l.includes('p.pitchBb=Math.max'));
const batterCompat = src.split('\n').find((l) => l.includes('b.bb=Number(b.bbHp)'));
assert(pitcherCompat && batterCompat, '找不到 ue() 的旧数据兼容回填语句');
const normalize = new Function('p', 'b', [pitcherCompat, batterCompat].join('\n') + ';void 0');

const whip = (row) => {
  const h = Number(row.pitchH) || 0;
  const bb = (row.pitchBb ?? null) != null ? Number(row.pitchBb) || 0 : Number(row.pitchBbHp) || 0;
  const ip = Number(row.ip) || 0;
  return ip ? (h + bb) / ip : null;
};

/** 235 场那一行的等价数据：1 局、BF 6、AB 3、H 0、SO 1、四死 3（= 2 保送 + 1 触身） */
const SERVER_ROW = {
  ip: 1, np: 20, pitchH: 0, er: 0, pitchR: 0,
  pitchBb: 2, pitchIbb: 0, pitchHbp: 1, pitchBbHp: 3,
  pitchSo: 1, pitchHr: 0, pitchInsideParkHr: 0, wp: 0, bk: 0,
  pitchPa: 6, pitchBf: 6, pitchDoubles: 0, pitchTriples: 0,
  pitchSh: 0, pitchSf: 0, pitchIo: 0, go: 2, fo: 1, gs: 1,
  ab: 3, r: 0, h: 0, rbi: 0, bb: 2, hbp: 1, bbHp: 3, so: 1, soSwing: 1, soLooking: 0,
  sf: 0, sh: 0, io: 0, gdp: 0, ibb: 0, insideParkHr: 0, doubles: 0, triples: 0, hr: 0, sb: 0, cs: 0, e: 0,
  po: 3, a: 0, defInn: 1, dp: 0, pb: 0, catcherSb: 0, catcherCs: 0,
};

/** 未含修复的 Do() pitching 区块原文（origin/v2.2/dev），作负对照 */
const BROKEN_PITCHING_BLOCK = 'ip:T?.ip??0,np:T?.np??0,pitchH:T?.pitchH??0,er:T?.er??0,pitchBbHp:T?.pitchBbHp??0,pitchSo:T?.pitchSo??0,pitchHr:T?.pitchHr??0,pitchInsideParkHr:T.pitchInsideParkHr??0,wp:T?.wp??0,bk:T?.bk??0';

function roundTrip(T = SERVER_ROW) {
  const p = new Function('T', 'return {' + doBlock('pitching') + '}')(T);
  const b = new Function('T', 'return {' + doBlock('batting') + '}')(T);
  const f = new Function('T', 'return {' + doBlock('fielding') + '}')(T);
  normalize(p, b);
  return { p, b, f, savedPitching: savePitching(p), savedBatting: saveBatting(b), savedFielding: saveFielding(f) };
}

test('负对照：未含修复时，1 次触身球会被折进保送并反映到 WHIP', () => {
  const p = new Function('T', 'return {' + BROKEN_PITCHING_BLOCK + '}')(SERVER_ROW);
  normalize(p, {});
  const saved = savePitching(p);
  assert.equal(saved.pitchHbp ?? 0, 0, '破损版：payload 的触身球是 0');
  assert.equal(saved.pitchBb, 3, '破损版：三笔四死全记成保送');
  assert.equal(saved.pitchBbHp, 3, '合计仍正确 → 内部自洽，从资料本身看不出走样');
  assert.equal(whip(saved), 3, '破损版 WHIP=3，与 issue 截图指纹一致');
});

test('#387 主案：修复后往返，触身球保持 1、四坏保持 2、WHIP 回到 2', () => {
  const { savedPitching } = roundTrip();
  assert.equal(savedPitching.pitchHbp, 1, '投手 HB 必须仍为 1');
  assert.equal(savedPitching.pitchBb, 2, '四坏必须仍是 2，不能变成 3');
  assert.equal(savedPitching.pitchBbHp, 3, '四死合计勾稽不破：2 BB + 1 HBP = 3');
  assert.equal(whip(savedPitching), 2, '修复后 WHIP=2（破损版是 3）');
});

test('修复后往返：BF / PA / IP / NP 不再被清零', () => {
  const { savedPitching } = roundTrip();
  assert.equal(savedPitching.pitchBf, 6);
  assert.equal(savedPitching.pitchPa, 6);
  assert.equal(savedPitching.ip, 1);
  assert.equal(savedPitching.np, 20);
});

test('修复后往返：打者 hbp 与投手 pitchHbp 相等（boxscore 两侧不会各说各话）', () => {
  const { savedPitching, savedBatting } = roundTrip();
  assert.equal(savedBatting.hbp, 1);
  assert.equal(savedBatting.bb, 2);
  assert.equal(savedBatting.bbHp, 3);
  assert.equal(savedPitching.pitchHbp, savedBatting.hbp, '投手 HB 必须等于该打者群的 HBP 合计');
});

test('修复后往返：守备局数 defInn 由服务端字段正确带出（#226 正口数据）', () => {
  const { savedFielding } = roundTrip();
  assert.equal(savedFielding.defInn, 1);
});

test('修复后往返：故意四坏 / GDP / IO / 捕手相关新补栏位不被清零', () => {
  const row = { ...SERVER_ROW, pitchIbb: 1, ibb: 1, gdp: 1, io: 1, pb: 2, catcherSb: 3, catcherCs: 1 };
  const { savedPitching, savedBatting, savedFielding } = roundTrip(row);
  assert.equal(savedPitching.pitchIbb, 1);
  assert.equal(savedPitching.pitchHbp, 1, '加了 IBB 不能把 HBP 挤掉');
  assert.equal(savedBatting.gdp, 1);
  assert.equal(savedBatting.ibb, 1);
  assert.equal(savedBatting.io, 1);
  assert.equal(savedFielding.pb, 2);
  assert.equal(savedFielding.catcherSb, 3);
  assert.equal(savedFielding.catcherCs, 1);
});

test('真正的 legacy 投手行（只有合计、无拆分）仍按原逻辑兼容回填', () => {
  const legacy = { ...SERVER_ROW, pitchBb: 0, pitchHbp: 0, pitchBbHp: 3 };
  const { p } = roundTrip(legacy);
  assert.equal(p.pitchBb, 3, '只有合计的旧行仍回填成保送——本修复没有破坏 legacy 兼容');
  assert.equal(p.pitchHbp ?? 0, 0, '合计行不该凭空长出触身球');
});

test('真正的 legacy 打击行（只有 bbHp 合计）往返后不被清零', () => {
  const legacy = { ...SERVER_ROW, bb: 0, hbp: 0, bbHp: 3 };
  const { b, savedBatting } = roundTrip(legacy);
  assert.equal(b.bb, 3, '只有 bbHp 合计的旧打击行应回填成保送');
  assert.equal(savedBatting.bbHp, 3, 'bbHp 合计不能在「重进现场→再保存」后被清零');
  assert.equal(savedBatting.hbp ?? 0, 0, '合计行不该凭空长出触身球');
});

test('拆分已存在的行不会被兼容回填二次改写（歧义已消失）', () => {
  const { p } = roundTrip();
  assert.equal(p.pitchBb, 2, '回填条件 (bb=0 且 hbp=0) 不成立，不应触发');
  assert.equal(p.pitchHbp, 1);
});
