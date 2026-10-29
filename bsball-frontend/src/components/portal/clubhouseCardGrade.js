// 卡面基准尺寸 px：组件根槽 = 基准 * scale，缩放浮层布局遵循同一契约
export const CARD_BASE_WIDTH = 204;
export const CARD_BASE_HEIGHT = 286;

// 球星卡八级材质色板，与确认稿「混合材质系列」一一对应 overall.grade（D/C/C+/B/B+/A/A+/S）
// width=边框粗细 sheen=光泽强度 sparkles=星芒数；越高档越炫丽
export const CARD_GRADE_MATERIALS = [
  { key: 'D', name: '深黑铁', width: 3.5, sheen: 0.14, sparkles: 0, color: '#2e343c', light: '#8f99a6', dark: '#07090c',
    glow: 'rgba(10,13,17,.55)', glow2: 'rgba(0,0,0,0)',
    stops: [['#4a525c', 0], ['#0b0e12', 24], ['#343b44', 50], ['#080a0d', 74], ['#404852', 100]] },
  { key: 'C', name: '铜', width: 4, sheen: 0.36, sparkles: 0, color: '#9c6534', light: '#d9a06b', dark: '#38200c',
    glow: 'rgba(156,101,52,.34)', glow2: 'rgba(156,101,52,.12)',
    stops: [['#c07b45', 0], ['#5c3315', 26], ['#8f5a2c', 50], ['#3a1f0c', 74], ['#7a4a22', 100]] },
  { key: 'C+', name: '白银', width: 4, sheen: 0.5, sparkles: 1, color: '#c3cdd8', light: '#ffffff', dark: '#66727e',
    glow: 'rgba(214,226,238,.38)', glow2: 'rgba(214,226,238,.12)',
    stops: [['#ffffff', 0], ['#96a3b1', 24], ['#eef3f8', 50], ['#6d7985', 76], ['#e2e9f0', 100]] },
  { key: 'B', name: '黄金', width: 4.5, sheen: 0.5, sparkles: 2, color: '#d4af37', light: '#ffe9a8', dark: '#7a5a12',
    glow: 'rgba(212,175,55,.42)', glow2: 'rgba(212,175,55,.18)',
    stops: [['#fff4c2', 0], ['#c9992e', 22], ['#ffe9a0', 46], ['#8a6414', 72], ['#f0d072', 100]] },
  { key: 'B+', name: '绿宝石', width: 4.5, sheen: 0.58, sparkles: 3, color: '#17b877', light: '#a5ffd6', dark: '#065f36',
    glow: 'rgba(23,184,119,.48)', glow2: 'rgba(23,184,119,.24)',
    stops: [['#c4ffe1', 0], ['#0d9152', 20], ['#6ff2b0', 42], ['#065f36', 66], ['#34d98c', 86], ['#b0ffdd', 100]] },
  { key: 'A', name: '钻石蓝', width: 5, sheen: 0.68, sparkles: 4, color: '#59c4f2', light: '#ffffff', dark: '#1c6f9e',
    glow: 'rgba(120,210,255,.52)', glow2: 'rgba(120,210,255,.3)',
    stops: [['#ffffff', 0], ['#7fd4ff', 14], ['#eaf9ff', 30], ['#3aa8e8', 46], ['#ffffff', 60], ['#9fe4ff', 78], ['#ffffff', 100]] },
  { key: 'A+', name: '尊贵紫', width: 5, sheen: 0.64, sparkles: 4, color: '#9b45e0', light: '#e6b8ff', dark: '#3d1160',
    glow: 'rgba(155,69,224,.55)', glow2: 'rgba(155,69,224,.32)',
    stops: [['#f0dcff', 0], ['#7a2bb8', 18], ['#d9a4ff', 36], ['#4a1470', 54], ['#b464e8', 72], ['#e6c2ff', 88], ['#8a2fd0', 100]] },
  { key: 'S', name: '史诗橙', width: 5.5, sheen: 0.34, sparkles: 6, color: '#ff6a00', light: '#ffb35c', dark: '#a33000',
    glow: 'rgba(255,106,0,.62)', glow2: 'rgba(255,60,0,.45)',
    stops: [['#ffc266', 0], ['#ff7a00', 16], ['#ff9e2e', 32], ['#d63c00', 48], ['#ff8c1a', 64], ['#ff5400', 82], ['#ffb35c', 100]] }
];

// 星芒位置（卡面宽高百分比）与尺寸 px，顺序与 sparkles 数量对应：取前 N 个
export const CARD_GRADE_SPARK_SPOTS = [
  [4.9, 2.1, 7],
  [95.1, 3.5, 8],
  [99, 42, 6.5],
  [96.1, 97.2, 8.5],
  [49, 99.3, 7.5],
  [3.9, 95.1, 8]
];

const FALLBACK_MATERIAL = CARD_GRADE_MATERIALS[0];

export function cardGradeMaterial(grade) {
  return CARD_GRADE_MATERIALS.find((item) => item.key === grade) || FALLBACK_MATERIAL;
}

export function gradeFrameGradient(material) {
  return `linear-gradient(160deg, ${material.stops.map(([color, offset]) => `${color} ${offset}%`).join(', ')})`;
}

export function gradeSheenGradient(material) {
  return `linear-gradient(115deg, transparent 42%, rgba(255,255,255,${material.sheen}) 50%, transparent 58%)`;
}

export function gradeSparkSpots(material) {
  return CARD_GRADE_SPARK_SPOTS.slice(0, material.sparkles).map(([x, y, size], index) => ({
    id: index,
    style: { left: `${x}%`, top: `${y}%`, width: `${size}px`, height: `${size}px` }
  }));
}
