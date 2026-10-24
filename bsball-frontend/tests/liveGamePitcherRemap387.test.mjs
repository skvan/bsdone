// 前端单测：实时录入「重新进入现场」的统计映射完整性（#387）
// 运行：在 bsball-frontend 目录执行  node --test tests/liveGamePitcherRemap387.test.mjs
//
// 背景：Do() 把服务端 game_player_stat 行重建为本地对象，Eo() 再把本地对象组装成保存 payload。
// 只要 Eo() 以 `??0` 直读某字段而 Do() 没还原它，「重新进入录入现场 → 再保存」就会把该字段
// 静默清零回写数据库（后端是 `if (part.getX() != null) set` 或无条件覆盖，0 不是 null）。
// #387 的实测症状：投手 pitchHbp 未被还原，ue() 的旧数据兼容回填
// （pitchBb = pitchBbHp - pitchHbp）于是把整笔四死合计判给保送——打者 hbp=1 而投手 pitch_hbp=0。
//
// 组件是 11k 行闭包无法直接单测，这里对源码做结构不变量校验：三侧（打击/投球/守备）全覆盖。
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const src = readFileSync(resolve(here, '../src/views/admin/LiveGame.js'), 'utf8');

// Eo() 的 payload 行：用投手行标记锁定，避免误抓其他组装式
const payloadLine = src.split('\n').find((l) => l.includes('isPitcher:a.isPitcherRow?1:0'));
assert.ok(payloadLine, '找不到 Eo() 的保存 payload 行，测试锚点已失效');

// 本地键 ↔ 服务端键的既有别名，按侧分开：打击侧是 snake/camel 兼容；守备侧的局数
// 落库列名与投手的 ip 不同名（#226）。注意 ip 在投手掌对应服务端 ip、在守备手掌
// 对应 defInn，混用一张表会互相误伤，故必须分侧。
const ALIAS = {
  batting: { so_swing: 'soSwing', so_looking: 'soLooking' },
  pitching: {},
  fielding: { ip: 'defInn' },
};

const SIDES = [
  { side: 'batting', ns: 's' },
  { side: 'pitching', ns: 'r' },
  { side: 'fielding', ns: 'p' },
];

const anchor = src.indexOf('ab:T?.ab??0');

/** Eo() 以 `payloadKey:ns.localKey??0` 直读的字段 → Map<localKey, payloadKey> */
function consumedByPayload(ns) {
  const out = new Map();
  // ns 不包捕获组，避免组序号错位
  const re = new RegExp('([a-zA-Z_]+):' + ns + '\\.([a-zA-Z_]+)\\?\\?0', 'g');
  for (const [, payload, local] of payloadLine.matchAll(re)) out.set(local, payload);
  return out;
}

/** Do() 的某一侧重建映射 → Map<localKey, serverKey> */
function remappedByLoad(side) {
  const from = side === 'batting' ? anchor - 200 : anchor;
  const start = src.indexOf(side + ':{', from);
  assert.ok(start > -1, `找不到 Do() 的 ${side} 映射块`);
  const block = src.slice(start, src.indexOf('}', start));
  const out = new Map();
  // 不要求紧跟 ??0：存在 so_swing:T?.soSwing??T?.so_swing??0 这类双重退回写法，
  // 取首个服务端键（权威列名）即可
  for (const [, local, server] of block.matchAll(/([a-zA-Z_]+):T\??\.([a-zA-Z_]+)/g)) {
    if (!out.has(local)) out.set(local, server);
  }
  return out;
}

test('#387 投手 BB/HBP 拆分必须在载入映射中还原（不得只留合计）', () => {
  const mapped = remappedByLoad('pitching');
  for (const k of ['pitchBb', 'pitchHbp', 'pitchIbb']) {
    assert.ok(mapped.has(k), 'Do() 未映射 ' + k + '：重载后 ue() 会把四死合计折成保送，触身球被改判为四坏');
  }
});

for (const { side, ns } of SIDES) {
  test(`${side} 侧：Eo() 直读的字段必须全部被 Do() 还原`, () => {
    const consumed = consumedByPayload(ns);
    const mapped = remappedByLoad(side);
    assert.ok(consumed.size >= 3, `${side} 侧 payload 字段解析异常，仅取到 ${consumed.size} 个`);
    const missing = [...consumed.keys()].filter((k) => !mapped.has(k));
    assert.deepEqual(missing, [], `这些 ${side} 字段会被 Eo() 以 ??0 送出，但 Do() 未从服务端还原：${missing.join(', ')}`);
  });
}

test('载入映射必须同名对应（防止把合计塞进拆分栏这类错键映射）', () => {
  const offenders = [];
  for (const { side } of SIDES) {
    for (const [local, server] of remappedByLoad(side)) {
      if (server !== ((ALIAS[side] ?? {})[local] ?? local)) offenders.push(`${side}.${local}←${server}`);
    }
  }
  assert.deepEqual(offenders, [], '非别名的错键映射会让数据落到错误的统计栏：' + offenders.join(', '));
});

test('守备局数与故意四坏/妨碍捕手等易漏项有显式锚点（回归即此 PR 扩到的字段）', () => {
  const fielding = remappedByLoad('fielding');
  assert.equal(fielding.get('ip'), 'defInn', '守备局数落库列是 defInn，漏还原会抹掉 #226 的每出局累计');
  const batting = remappedByLoad('batting');
  for (const k of ['gdp', 'ibb', 'io']) assert.ok(batting.has(k), `Do() 未映射打击侧 ${k}（io 还会连带把派生 pa 算小）`);
});

test('打击侧 bbHp 合计也必须还原（ue() legacy 回填依赖）', () => {
  assert.ok(remappedByLoad('batting').has('bbHp'), 'Do() 未映射 bbHp：只有合计的旧打击行在往返后会被清零');
});

test('打者侧 hbp 与投手侧 pitchHbp 同时在场（半边丢失是本缺陷的指纹）', () => {
  assert.match(src.slice(anchor, src.indexOf('}', anchor)), /hbp:T\?\.hbp\?\?0/, '打者侧 hbp 映射被移除，本测试的对照前提失效');
  assert.ok(remappedByLoad('pitching').has('pitchHbp'), '投手侧 pitchHbp 缺失，会出现「打者有触身、投手无触身」的半边数据');
});
