// 统计列 → 术语表键 / 列宽覆盖
// 按列 key 映射（避免打击与投手共用 colKey 时取错语境，如 r / pitchR、h / pitchH）。
// 列定义仍以 stores/statsColumns.js 为准；本文件只补充表头语义与显示宽度。

const HEADER_KEY_BY_COLUMN = {
  // 打击
  gp: 'gp',
  pa: 'pa',
  ab: 'ab',
  avg: 'avg',
  obp: 'obp',
  slg: 'slg',
  ops: 'ops',
  r: 'r',
  h: 'h',
  singles: 'oneB',
  doubles: 'twoB',
  triples: 'threeB',
  hr: 'hr',
  gdp: 'gdp',
  sh: 'sh',
  sf: 'sf',
  insideParkHr: 'iphr',
  rbi: 'rbi',
  bb: 'bb',
  ibb: 'ibb',
  hbp: 'hbp',
  io: 'io',
  so: 'so',
  sb: 'sb',
  cs: 'cs',
  tb: 'tb',
  // 投手
  gs: 'gs',
  ip: 'ip',
  w: 'w',
  l: 'l',
  era: 'era',
  cg: 'cg',
  sv: 'sv',
  svo: 'svo',
  pitchBf: 'pitchBf',
  pitchH: 'pitchH',
  pitchDoubles: 'pitchDoubles',
  pitchTriples: 'pitchTriples',
  pitchR: 'pitchR',
  er: 'er',
  unearnedR: 'unearnedR',
  pendingR: 'pendingR',
  pitchHr: 'pitchHr',
  pitchInsideParkHr: 'pitchInsideParkHr',
  pitchSh: 'pitchSh',
  pitchSf: 'pitchSf',
  // 术语表只有打击语境的 hbp / ibb，投手列复用（与 normalizeGlossaryKey 的别名一致）
  pitchHbp: 'hbp',
  pitchBb: 'pitchBb',
  pitchIbb: 'ibb',
  pitchIo: 'pitchIo',
  pitchSo: 'pitchSo',
  pitchBbHp: 'bbHp',
  pitAb: 'pitchAb',
  pitchAb: 'pitchAb',
  pitE: 'e',
  pitchPa: 'pa',
  whip: 'whip',
  wp: 'wp',
  bk: 'bk',
  // 守备
  inn: 'inn',
  tc: 'tc',
  po: 'po',
  a: 'a',
  e: 'e',
  dp: 'dp',
  pb: 'pb',
  catcherCs: 'catcherCs',
  catcherSb: 'catcherSb',
  tcPct: 'tcPct'
};

// 中文全称变长后的最小列宽（px）。按 12px 字体 + 24px 排序箭头 + 8px 内边距估算，
// 取简中/繁中较长者；仅放大，不缩小原有宽度。
const WIDTH_OVERRIDES = {
  doubles: 80,
  triples: 80,
  sh: 92,
  sf: 92,
  ibb: 92,
  io: 80,
  cs: 80,
  tc: 80,
  catcherCs: 80,
  pitchDoubles: 92,
  pitchTriples: 92,
  pitchSh: 104,
  pitchSf: 104,
  pitchIo: 92,
  pendingR: 92,
  pitchBf: 80,
  pitchIbb: 92,
  whip: 104,
  gs: 80
};

export function statsHeaderKey(columnKey, fallbackKey, domain) {
  // 守备位置榜的 GS 是「以该守位先发」，不是投手的先发场次。
  if (domain === 'fielding' && columnKey === 'gs') return 'gsFielding';
  return HEADER_KEY_BY_COLUMN[columnKey] || fallbackKey || columnKey;
}

// 编译产物中个别列沿用旧缩写（GG / PG / TC% / BB+HP），表头英文统一按 MLB 口径。
const HEADER_ABBR_OVERRIDES = {
  cg: 'CG',
  pg: 'SHO',
  tcPct: 'FPCT',
  bbHp: 'BB+HBP',
  pitchBbHp: 'BB+HBP'
};

export function statsHeaderAbbr(columnKey, fallbackAbbr) {
  return HEADER_ABBR_OVERRIDES[columnKey] || fallbackAbbr;
}

function maxWithOverride(overrides, columnKey, fallbackWidth) {
  const base = Number(fallbackWidth) || 0;
  const override = overrides[columnKey] || 0;
  return Math.max(base, override);
}

export function statsColumnWidth(columnKey, fallbackWidth) {
  return maxWithOverride(WIDTH_OVERRIDES, columnKey, fallbackWidth);
}

// 球员详情赛季表与比赛详情技术统计表：表头没有排序箭头，单元格内边距也更小；
// 按中文全称给最小宽度（同样只放大，不缩小）。
const PLAYER_WIDTH_OVERRIDES = {
  singles: 56,
  doubles: 56,
  triples: 56,
  hr: 56,
  gdp: 56,
  insideParkHr: 84,
  sh: 72,
  sf: 72,
  cs: 60,
  bb: 56,
  ibb: 72,
  hbp: 56,
  io: 64,
  so: 56,
  sb: 56,
  rbi: 56,
  avg: 56,
  obp: 56,
  slg: 56,
  ops: 68,
  e: 56,
  whip: 84,
  era: 68,
  ip: 64,
  svo: 64,
  pitchR: 56,
  er: 56,
  pitchH: 56,
  pitchAb: 64,
  pitchBf: 64,
  pitchDoubles: 84,
  pitchTriples: 84,
  pitchHr: 64,
  pitchInsideParkHr: 84,
  pitchSh: 84,
  pitchSf: 84,
  pitchIbb: 72,
  pitchIo: 72,
  pitchHbp: 56,
  pitchBb: 56,
  pitchSo: 56,
  pitAb: 64,
  pitE: 56,
  pitchPa: 64,
  wp: 56,
  bk: 64,
  inn: 64,
  tc: 64,
  po: 56,
  a: 56,
  dp: 56,
  pb: 56,
  catcherCs: 68,
  catcherSb: 68,
  tcPct: 68
};

export function playerColumnWidth(columnKey, fallbackWidth) {
  return maxWithOverride(PLAYER_WIDTH_OVERRIDES, columnKey, fallbackWidth);
}

export { HEADER_KEY_BY_COLUMN };
