// 垒球换人模式·提交前阵容校验（回归）：Er 必须把赛制传给 validateRosterPositions。
// 背景：Er() 曾传 void 0，垒球 10/11 人阵容被按棒球 9 人规则校验，结束换人时报
// “未开放 EP 制”或“无效守备位置: XF”。本测试切片执行 LiveGame.js 里的 Er，
// 注入真实校验器与 mock 的 gameMode/extraBatter，确保调用方也显式传 N.value。
// 运行：node --test tests/liveGameSubstitutionSoftball.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateRosterPositions } from '../src/utils/starterFieldingValidation.js';

const SRC_PATH = process.env.LIVEGAME_PATH
  || join(dirname(fileURLToPath(import.meta.url)), '../src/views/admin/LiveGame.js');
const src = fs.readFileSync(SRC_PATH, 'utf8').replace(/\r\n/g, '\n');

/** 从 LiveGame.js 切出 Er 函数体（支持签名后续增加默认参数）。 */
function sliceEr() {
  const at = src.indexOf('function Er(');
  assert(at >= 0, '找不到 Er()，元件结构可能已变动');
  const open = src.indexOf('{', at);
  assert(open > at, 'Er() 缺少函数体');
  let depth = 0;
  for (let i = open; i < src.length; i++) {
    const c = src[i];
    if (c === '"' || c === "'" || c === '`') {
      for (i++; i < src.length; i++) {
        if (src[i] === '\\') i++;
        else if (src[i] === c) break;
      }
      continue;
    }
    if (c === '/' && src[i + 1] === '/') {
      while (i < src.length && src[i] !== '\n') i++;
      continue;
    }
    if (c === '/' && src[i + 1] === '*') {
      i = src.indexOf('*/', i) + 1;
      continue;
    }
    if (c === '{') depth++;
    else if (c === '}' && --depth === 0) return src.slice(at, i + 1);
  }
  throw new Error('Er() 括号不闭合');
}

const makeEr = (mode, extraBatter) => new Function(
  'sd', 'N', 'extraBatterOn',
  'return (function(){' + sliceEr() + '\nreturn Er;})()',
)(validateRosterPositions, { value: mode }, { value: extraBatter });

const rows = (positions) => positions.map((position, index) => ({
  position,
  battingOrder: index + 1,
}));
const SOF8 = ['C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'];

test('垒球 10 人（P+八守位+XF）应通过提交前校验', () => {
  const er = makeEr('SOFTBALL', false);
  assert.equal(er(rows([...SOF8, 'P', 'XF'])).ok, true);
});

test('垒球 10 人（DH+八守位+XF）应通过提交前校验', () => {
  const er = makeEr('SOFTBALL', false);
  assert.equal(er(rows([...SOF8, 'DH', 'XF'])).ok, true);
});

test('垒球 11 人（P+DH+八守位+XF）：开额外打击者通过，关时仍拒', () => {
  const lineup = rows([...SOF8, 'P', 'DH', 'XF']);
  assert.equal(makeEr('SOFTBALL', true)(lineup).ok, true);
  const closed = makeEr('SOFTBALL', false)(lineup);
  assert.equal(closed.ok, false);
  assert.match(closed.msg, /未开放 EP 制/);
});

test('历史快照 XF1/XF2：垒球按 XF 归一后通过', () => {
  const er = makeEr('SOFTBALL', false);
  assert.equal(er(rows([...SOF8, 'P', 'XF1'])).ok, true);
  assert.equal(er(rows([...SOF8, 'DH', 'XF2'])).ok, true);
});

test('棒球回归：9 人通过，10 人未开额外打击者仍拒', () => {
  assert.equal(makeEr('BASEBALL', false)(rows([...SOF8, 'P'])).ok, true);
  const ten = makeEr('BASEBALL', false)(rows([...SOF8, 'P', 'DH']));
  assert.equal(ten.ok, false);
  assert.match(ten.msg, /未开放 EP 制/);
});

test('结束换人调用点显式传赛制，避免只修 Er 内部漏接线', () => {
  assert.match(
    src,
    /const check=Er\(after\[side\]\.lineup,\s*N\.value\)/,
    'finishSubstitutionMode 的 Er 调用必须显式传 N.value',
  );
});
