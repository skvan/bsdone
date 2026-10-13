// 击出球飞行轨迹参数与几何（实时录入页与赛后回放共用，单一来源）
// 2026-10-10（issue #279）：从 LiveGame.js 内联表提取；hg/b/pop 由共用参数改为各自独立参数
// k：弧线凸起系数（0=贴地直线，值越大弧线越高）；ms：飞行时长（毫秒）
export const BAT_SHAPES = {
  g: [0, 450], // 滚地球：贴地直线
  hg: [0, 300], // 强袭滚地球：贴地直线，更快
  b: [0, 650], // 短打：贴地直线，更慢
  ld: [0.14, 600], // 平飞球：低弧
  fly: [0.45, 1000], // 飞球：高弧、飞行更缓（2026-10-10 用户调优：更慢、曲率更大）
  pop: [0.6, 1100], // 内野高飞球：最高弧、滞空最久
};

// 未知/缺失球种回退参数（同录入页原兜底：按平飞球）
const DEFAULT_SHAPE = [0.14, 600];

// 轨迹配色（2026-10-10 用户裁定）：滚地系（g/hg/b）统一黄色，以速度区分；
// 平飞/飞球/内野高飞各自独立颜色
export const BAT_SHAPE_COLORS = {
  g: '#FFD700', // 滚地球：黄
  hg: '#FFD700', // 强袭滚地球：黄（速度更快区分）
  b: '#FFD700', // 短打：黄（速度更慢区分）
  ld: '#38BDF8', // 平飞球：天蓝低弧
  fly: '#FF6B35', // 飞球：橙红高弧
  pop: '#B388FF', // 内野高飞球：紫（高弧滞空）
};

/** 球种 -> 轨迹颜色（未知回退平飞色） */
export function battedShapeColor(bipCode) {
  return BAT_SHAPE_COLORS[bipCodeShape(bipCode)] ?? BAT_SHAPE_COLORS.ld;
}

/** 从 bip:xxx:yyy 形式的 bipCode 提取球种段（g/hg/fly/ld/b/pop） */
export function bipCodeShape(bipCode) {
  return String(bipCode ?? "").match(/^bip:([^:]+):/)?.[1] ?? null;
}

/** 球种 -> { k, ms }（含兜底，行为与录入页 battedShapeOf 一致） */
export function battedShapeOf(bipCode) {
  const t = BAT_SHAPES[bipCodeShape(bipCode)] ?? DEFAULT_SHAPE;
  return { k: t[0], ms: t[1] };
}

/** 二次贝塞尔控制点：中点沿弦法线抬高 k*弦长（k<=0 时为弦中点，退化为直线） */
export function battedGeo(x, y, k) {
  return {
    cx: (240 + x) / 2,
    cy: (555 + y) / 2 - Math.max(0, k) * Math.hypot(x - 240, y - 555),
  };
}

/** 本垒(240,555) -> 落点 的轨迹采样点（progress 0-1，k=0 时退化为直线） */
export function battedPointAt(x, y, k, progress) {
  const cp = battedGeo(x, y, k);
  const q = progress;
  return {
    x: (1 - q) * (1 - q) * 240 + 2 * (1 - q) * q * cp.cx + q * q * x,
    y: (1 - q) * (1 - q) * 555 + 2 * (1 - q) * q * cp.cy + q * q * y,
  };
}

/** 轨迹 SVG path d（与录入页画法一致：k=0 直线，否则二次贝塞尔） */
export function battedPathD(x, y, k) {
  const cp = battedGeo(x, y, k);
  return k === 0 ? `M 240 555 L ${x} ${y}` : `M 240 555 Q ${cp.cx} ${cp.cy} ${x} ${y}`;
}
