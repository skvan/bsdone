// 实时观赛旧快照兜底：投手统计对象缺失时，由逐打席记录回算可由 PBP 推导的字段。
// 只回算击球结果/四坏/触身/牺牲等可判定项；R/ER 不臆造。
export function derivePitcherStatsFromSummary(pitcherId, summaries) {
  let outs = 0;
  let h = 0;
  let doubles = 0;
  let triples = 0;
  let hr = 0;
  let iphr = 0;
  let bb = 0;
  let ibb = 0;
  let hbp = 0;
  let sh = 0;
  let sf = 0;
  let so = 0;
  let pa = 0;
  let found = false;
  for (const row of summaries ?? []) {
    if (Number(row?.pitcherPlayerId) !== Number(pitcherId)) continue;
    found = true;
    pa += 1;
    outs += Math.max(0, (row.outsAfter ?? 0) - (row.outsBefore ?? 0));
    const code = String(row.resultCode ?? '');
    if (code === 'H1' || code === 'H2' || code === 'H3' || code === 'HR' || code === 'IPHR') h += 1;
    if (code === 'H2') doubles += 1;
    if (code === 'H3') triples += 1;
    if (code === 'HR') hr += 1;
    if (code === 'IPHR') iphr += 1;
    if (code === 'BB' || code === 'IBB') bb += 1;
    if (code === 'IBB') ibb += 1;
    if (code === 'HBP') hbp += 1;
    if (code === 'SH') sh += 1;
    if (code === 'SF') sf += 1;
    if (/^K/.test(code) || /^DK3/.test(code)) so += 1;
  }
  if (!found) return null;
  return {
    ip: Math.floor(outs / 3) + (outs % 3) / 10,
    pitchH: h,
    pitchDoubles: doubles,
    pitchTriples: triples,
    pitchHr: hr,
    pitchInsideParkHr: iphr,
    pitchBb: bb,
    pitchIbb: ibb,
    pitchHbp: hbp,
    pitchBbHp: bb + hbp,
    pitchSh: sh,
    pitchSf: sf,
    pitchSo: so,
    pitchPa: pa,
    pitchBf: pa
  };
}

const SUMMARY_FILL_KEYS = [
  'ip',
  'pitchH',
  'pitchDoubles',
  'pitchTriples',
  'pitchHr',
  'pitchInsideParkHr',
  'pitchSo',
  'pitchPa',
  'pitchBf',
  'pitchSh',
  'pitchSf',
  'pitchBb',
  'pitchIbb',
  'pitchHbp'
];

/** 用逐打席回算补齐实时投手统计中缺失/为 0 的字段；R/ER/WP 等现场专属字段保持原值。 */
export function mergePitcherStatsWithSummary(livePitching, derived) {
  const merged = { ...(livePitching ?? {}) };
  if (!derived) return merged;
  // #387 旧数据兼容：历史加载会把 BB 回填成 pitchBbHp - pitchHbp（常见为 BB=合计、HBP=0）。
  // 若按零值补 HBP 会把 BB 留大，必须同时用 PBP 回算的 BB/IBB/HBP 覆盖回填形态。
  const backfilledAggregate =
    Number(merged.pitchBbHp) > 0 &&
    !Number(merged.pitchHbp) &&
    Number(merged.pitchBb) === Number(merged.pitchBbHp);
  for (const key of SUMMARY_FILL_KEYS) {
    if (!Number(merged[key]) && Number(derived[key])) merged[key] = derived[key];
  }
  if (backfilledAggregate) {
    merged.pitchBb = derived.pitchBb;
    merged.pitchIbb = derived.pitchIbb;
    merged.pitchHbp = derived.pitchHbp;
  }
  if (merged.pitchBb != null || merged.pitchHbp != null) {
    merged.pitchBbHp = (Number(merged.pitchBb) || 0) + (Number(merged.pitchHbp) || 0);
  }
  return merged;
}
