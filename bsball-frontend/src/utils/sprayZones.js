// 打击落点分区几何与统计（驾驶舱热力模块与比赛回放面板共用）
// 场地坐标系与实时录入一致：viewBox -300 -150 1080 780，本垒约 (240,555)，
// 两条边线约正负 47 度；外野 7 条扇带对应 Issue #526 附图的 LL/LS/LC/MI/RC/RS/RL。
export const SPRAY_HOME = Object.freeze({ x: 240, y: 555 });
export const SPRAY_RADII = Object.freeze({ infield: 150, fence: 575 });

export const SPRAY_LANES = Object.freeze([
  { code: 'LL', label: '左边线', from: -47, to: -38 },
  { code: 'LS', label: '左路穿越', from: -38, to: -27 },
  { code: 'LC', label: '左中路', from: -27, to: -15 },
  { code: 'MI', label: '中路', from: -15, to: 15 },
  { code: 'RC', label: '右中路', from: 15, to: 27 },
  { code: 'RS', label: '右路穿越', from: 27, to: 38 },
  { code: 'RL', label: '右边线', from: 38, to: 47 }
]);

export const SPRAY_INFIELD_CODES = Object.freeze(['P', 'C', '1B', '2B', '3B', 'SS', 'IF']);

const INFIELD_SET = new Set(SPRAY_INFIELD_CODES);
const HIT_CODES = new Set(['H1', 'H2', 'H3', 'HR', 'IPHR']);

export const SPRAY_TYPE_LABELS = Object.freeze({
  ld: '平飞球',
  fly: '飞球',
  g: '滚地球',
  hg: '强袭滚地',
  b: '短打',
  pop: '内野高飞',
  other: '未知'
});
export const SPRAY_TYPE_ORDER = Object.freeze(['ld', 'fly', 'g', 'hg', 'b', 'pop', 'other']);

export function isSprayHit(resultCode) {
  return HIT_CODES.has(String(resultCode ?? '').trim().toUpperCase());
}

/** 球种取 bip:xxx:yyy 的第二段；未知归 other。 */
export function sprayBipShape(bipCode) {
  const shape = String(bipCode ?? '').trim().toLowerCase().split(':')[1];
  return shape && Object.prototype.hasOwnProperty.call(SPRAY_TYPE_LABELS, shape) && shape !== 'other'
    ? shape
    : 'other';
}

/** 落点分区：内野按记录守位优先；外野按极角扇带，越界贴边归入两侧边线。 */
export function classifySprayZone(x, y, storedZone) {
  const zone = String(storedZone ?? '').trim().toUpperCase();
  if (INFIELD_SET.has(zone)) return 'IF';
  const angle = Math.atan2(Number(x) - SPRAY_HOME.x, SPRAY_HOME.y - Number(y)) * 180 / Math.PI;
  const clamped = Math.max(-47, Math.min(47, angle));
  const lane = SPRAY_LANES.find((item) => clamped >= item.from && clamped < item.to);
  return lane ? lane.code : clamped >= 0 ? 'RL' : 'LL';
}

export function sprayPolar(angle, radius) {
  const rad = (angle * Math.PI) / 180;
  return {
    x: Number((SPRAY_HOME.x + Math.sin(rad) * radius).toFixed(1)),
    y: Number((SPRAY_HOME.y - Math.cos(rad) * radius).toFixed(1))
  };
}

export function sprayWedgePath(from, to, inner, outer) {
  const innerFrom = sprayPolar(from, inner);
  const outerFrom = sprayPolar(from, outer);
  const outerTo = sprayPolar(to, outer);
  const innerTo = sprayPolar(to, inner);
  return `M ${innerFrom.x} ${innerFrom.y} L ${outerFrom.x} ${outerFrom.y} `
    + `A ${outer} ${outer} 0 0 1 ${outerTo.x} ${outerTo.y} `
    + `L ${innerTo.x} ${innerTo.y} A ${inner} ${inner} 0 0 0 ${innerFrom.x} ${innerFrom.y} Z`;
}

export function sprayPiePath(from, to, radius) {
  const start = sprayPolar(from, radius);
  const end = sprayPolar(to, radius);
  return `M ${SPRAY_HOME.x} ${SPRAY_HOME.y} L ${start.x} ${start.y} `
    + `A ${radius} ${radius} 0 0 1 ${end.x} ${end.y} Z`;
}

// 热区配色（叠在真实球场底色上）：低→浅黄，中→橙，高→红橙；alpha 随强度递增
const HEAT_STOPS = Object.freeze([
  { at: 0, rgb: [255, 226, 120] },
  { at: 0.55, rgb: [255, 176, 40] },
  { at: 1, rgb: [255, 108, 0] }
]);

export function sprayHeatFill(ratio) {
  const t = Math.max(0, Math.min(1, Number(ratio) || 0));
  const upper = HEAT_STOPS.find((stop) => t <= stop.at) ?? HEAT_STOPS[HEAT_STOPS.length - 1];
  const lower = [...HEAT_STOPS].reverse().find((stop) => stop.at <= t) ?? HEAT_STOPS[0];
  const span = upper.at - lower.at || 1;
  const k = (t - lower.at) / span;
  const rgb = lower.rgb.map((channel, index) => Math.round(channel + (upper.rgb[index] - channel) * k));
  const alpha = (0.22 + 0.5 * t).toFixed(2);
  return `rgba(${rgb[0]}, ${rgb[1]}, ${rgb[2]}, ${alpha})`;
}

/** 分区视角几何：把服务端 rows（code/hits/balls）补上扇带路径、角标位置与热力透明度。 */
export function sprayZoneViews(rows) {
  const list = Array.isArray(rows) ? rows : [];
  const maxHits = Math.max(1, ...list.map((zone) => Number(zone.hits) || 0));
  return list.map((zone) => {
    const hits = Number(zone.hits) || 0;
    const balls = Number(zone.balls) || 0;
    const isInfield = zone.code === 'IF';
    const lane = SPRAY_LANES.find((item) => item.code === zone.code);
    const from = isInfield ? -47 : lane?.from ?? -47;
    const to = isInfield ? 47 : lane?.to ?? 47;
    const path = isInfield
      ? sprayPiePath(from, to, SPRAY_RADII.infield)
      : sprayWedgePath(from, to, SPRAY_RADII.infield, SPRAY_RADII.fence);
    const label = isInfield
      ? sprayPolar(0, 96)
      : sprayPolar((from + to) / 2, SPRAY_RADII.fence * 0.74);
    return {
      ...zone,
      hits,
      balls,
      path,
      labelX: label.x,
      labelY: label.y,
      heat: sprayHeatFill(hits / maxHits)
    };
  });
}

/** 从落点列表统计分区：points 需含 x/y/zone(storedZone)/resultCode。 */
export function buildSprayZoneRows(points) {
  const tally = new Map([['IF', [0, 0]], ...SPRAY_LANES.map((lane) => [lane.code, [0, 0]])]);
  (points || []).forEach((point) => {
    const code = classifySprayZone(point.x, point.y, point.zone);
    const counts = tally.get(code);
    counts[1] += 1;
    if (isSprayHit(point.resultCode)) counts[0] += 1;
  });
  return [...tally.entries()].map(([code, [hits, balls]]) => ({
    code,
    label: code === 'IF' ? '内野' : SPRAY_LANES.find((lane) => lane.code === code)?.label || code,
    hits,
    balls
  }));
}

/** 从落点列表统计球种：points 需含 bipCode/resultCode。 */
export function buildSprayTypeRows(points) {
  const tally = new Map(SPRAY_TYPE_ORDER.map((code) => [code, [0, 0]]));
  (points || []).forEach((point) => {
    const counts = tally.get(sprayBipShape(point.bipCode));
    counts[1] += 1;
    if (isSprayHit(point.resultCode)) counts[0] += 1;
  });
  return [...tally.entries()].map(([code, [hits, balls]]) => ({
    code,
    label: SPRAY_TYPE_LABELS[code],
    hits,
    balls
  }));
}
