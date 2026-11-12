// 删除投手行前的剩余数据诊断。与后端 GamePlayerStatZeros 的统计口径保持一致（含 PA）。
const STAT_FIELDS = [
  ['pa', 'PA'], ['ab', 'AB'], ['r', 'R'], ['h', 'H'], ['e', 'E'], ['bbHp', 'BB+HP'],
  ['sb', 'SB'], ['so', 'SO'], ['soSwing', '挥空'], ['soLooking', '看三振'], ['rbi', 'RBI'],
  ['doubles', '2B'], ['triples', '3B'], ['hr', 'HR'], ['insideParkHr', '场内本垒打'],
  ['gdp', 'GDP'], ['sh', 'SH'], ['sf', 'SF'], ['bb', 'BB'], ['ibb', 'IBB'], ['hbp', 'HBP'],
  ['io', 'IO'], ['cs', 'CS'], ['ip', 'IP'], ['outsRecorded', 'OUTS'], ['earnedR', '自责(旧)'], ['unearnedR', '非自责'],
  ['pendingR', '待定'], ['er', 'ER'], ['pitchH', 'H'], ['pitchDoubles', '2B'],
  ['pitchTriples', '3B'], ['pitchBbHp', 'BB+HP'], ['pitchSo', 'SO'], ['pitchHr', 'HR'],
  ['pitchInsideParkHr', '场内本垒打'], ['pitchSh', 'SH'], ['pitchSf', 'SF'], ['gs', 'GS'],
  ['svo', 'SVO'], ['cg', 'CG'], ['pg', 'PG'], ['w', 'W'], ['l', 'L'], ['sv', 'SV'],
  ['hld', 'HLD'], ['pitchBf', 'BF'], ['pitchPa', 'PA'], ['pitAb', 'AB'], ['np', 'NP'],
  ['pitchBb', 'BB'], ['pitchIbb', 'IBB'], ['pitchHbp', 'HBP'], ['pitchIo', 'IO'],
  ['wp', 'WP'], ['bk', 'BK'], ['pitchR', 'R'], ['go', 'GO'], ['fo', 'FO']
];

const FIELDING_FIELDS = [
  ['tc', 'TC'], ['po', 'PO'], ['a', 'A'], ['e', 'E'], ['fieldingGs', 'GS'], ['defInn', 'INN'],
  ['dp', 'DP'], ['pb', 'PB'], ['catcherSb', 'SB'], ['catcherCs', 'CS']
];

function valueOf(source, key) {
  const value = Number(source?.[key]);
  return Number.isFinite(value) ? value : 0;
}

/** 返回非零字段（去重，按统计/守备顺序）。 */
export function nonZeroPitcherFields(stats, fielding) {
  const out = [];
  const seen = new Set();
  for (const [key, label] of STAT_FIELDS) {
    const value = valueOf(stats, key);
    if (value === 0) continue;
    const dedupe = `${label}:${value}`;
    if (seen.has(dedupe)) continue;
    seen.add(dedupe);
    out.push({ key, label, value });
  }
  for (const [key, label] of FIELDING_FIELDS) {
    const value = valueOf(stats, key) || valueOf(fielding, key);
    if (value === 0) continue;
    const dedupe = `${label}:${value}`;
    if (seen.has(dedupe)) continue;
    seen.add(dedupe);
    out.push({ key, label, value });
  }
  return out;
}
