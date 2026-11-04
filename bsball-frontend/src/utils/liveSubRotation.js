// 换人模式·守备轮转速记：解析闭环数字串，校验位置占用，生成位置置换表。
// 纯函数（不触碰 DOM/Vue），供 LiveGame 与 node --test 共用。

// 标准守位号 → 位置码；软垒额外守位 XF（10）不参与轮转
export const ROTATION_POSITION_BY_DIGIT = {
  1: 'P',
  2: 'C',
  3: '1B',
  4: '2B',
  5: '3B',
  6: 'SS',
  7: 'LF',
  8: 'CF',
  9: 'RF',
};

const ROTATION_POSITION_LABELS = {
  P: '投手',
  C: '捕手',
  '1B': '一垒手',
  '2B': '二垒手',
  '3B': '三垒手',
  SS: '游击手',
  LF: '左外野',
  CF: '中外野',
  RF: '右外野',
};

const labelOf = (code) => ROTATION_POSITION_LABELS[code] ?? code;
const describe = (digit, code) => `${digit}号位（${labelOf(code)}）`;

/**
 * 解析速记串：如 "1341" → digits ['1','3','4']（去掉收尾重复位）。
 * 规则：仅 1-9；至少 3 位；首尾相同；除收尾外同一位置只能出现一次。
 * @returns {{ok:true,digits:string[],codes:string[]} | {ok:false,error:string}}
 */
export function parseFieldRotation(input) {
  const raw = String(input ?? '').replace(/\s+/g, '');
  if (!raw) return { ok: false, error: '请输入守备轮转，如 1341（首尾相同形成闭环）' };
  if (!/^[1-9]+$/.test(raw)) return { ok: false, error: '只能输入 1-9 的守位数字，如 1341' };
  if (raw.length < 3) return { ok: false, error: '轮转至少需要 3 位数字，如 121 = 1 与 2 互换' };
  const digits = raw.split('');
  if (digits[0] !== digits[digits.length - 1]) {
    return { ok: false, error: '首尾数字必须相同，形成闭环，如 1341' };
  }
  const loop = digits.slice(0, -1);
  if (new Set(loop).size !== loop.length) {
    return { ok: false, error: '同一位置在轮转中只能出现一次（收尾闭环除外）' };
  }
  return { ok: true, digits: loop, codes: loop.map((d) => ROTATION_POSITION_BY_DIGIT[d]) };
}

/**
 * 生成位置置换表：entries 为当前守备阵容 [{id, position}]（position 已归一化为位置码）。
 * 每个被轮转的位置必须恰好对应一名球员，否则返回错误。
 * @returns {{ok:true,moves:{id:number,from:string,to:string}[],codes:string[]} | {ok:false,error:string}}
 */
export function buildFieldRotationMoves(digits, entries) {
  const pairs = (Array.isArray(digits) ? digits : []).map((d) => [String(d), ROTATION_POSITION_BY_DIGIT[d]]);
  if (pairs.length < 2 || pairs.some(([, code]) => !code)) {
    return { ok: false, error: '轮转格式无效，请重新输入' };
  }
  if (new Set(pairs.map(([digit]) => digit)).size !== pairs.length) {
    return { ok: false, error: '同一位置在轮转中只能出现一次' };
  }
  const codes = pairs.map(([, code]) => code);
  const byPosition = new Map();
  const duplicates = new Set();
  for (const entry of Array.isArray(entries) ? entries : []) {
    const pos = String(entry?.position ?? '').trim().toUpperCase();
    if (!pos) continue;
    if (byPosition.has(pos)) duplicates.add(pos);
    else byPosition.set(pos, entry);
  }
  const missing = pairs.filter(([, code]) => !byPosition.has(code));
  if (missing.length) {
    return { ok: false, error: `以下位置当前没有守备员：${missing.map(([digit, code]) => describe(digit, code)).join('、')}` };
  }
  const duplicated = pairs.filter(([, code]) => duplicates.has(code));
  if (duplicated.length) {
    return { ok: false, error: `阵容中以下位置有多名球员，请先修正阵容：${duplicated.map(([digit, code]) => describe(digit, code)).join('、')}` };
  }
  const moves = pairs.map(([, from], index) => ({
    id: Number(byPosition.get(from).id),
    from,
    to: codes[(index + 1) % codes.length],
  }));
  return { ok: true, moves, codes };
}

/** 轮转路径文案（如 "投手 → 一垒手 → 二垒手 → 投手"）。 */
export function formatFieldRotation(codes) {
  const list = Array.isArray(codes) ? codes : [];
  if (!list.length) return '';
  return [...list, list[0]].map((code) => labelOf(code)).join(' → ');
}
