<template>
  <div class="ghr-panel">
    <div class="ghr-head">
      <span class="ghr-title">打击落点回放</span>
      <span v-if="records.length" class="ghr-count">共 {{ records.length }} 次击球落点</span>
    </div>
    <el-empty
      v-if="!loading && !records.length"
      description="本场暂无击球落点记录"
      :image-size="72"
    />
    <template v-else>
      <div class="ghr-filters">
        <span class="ghr-filter-label">球队</span>
        <el-select v-model="teamKey" size="small" class="ghr-filter-team" @change="onFilterChange">
          <el-option label="全部球队" value="all" />
          <el-option v-for="t in teamOptions" :key="t.id" :label="t.name" :value="String(t.id)" />
        </el-select>
        <span class="ghr-filter-label">打者</span>
        <el-select v-model="batterKey" size="small" class="ghr-filter-batter" @change="onFilterChange">
          <el-option label="全部打者" value="all" />
          <el-option v-for="b in batterOptions" :key="b.key" :label="b.label" :value="b.key" />
        </el-select>
        <el-checkbox v-model="showTrails" size="small" class="ghr-trail-toggle">显示飞行轨迹</el-checkbox>
      </div>
      <div class="ghr-body">
        <div class="ghr-field">
          <svg viewBox="0 0 480 640" class="ghr-svg" role="img" aria-label="击球落点场地视图">
            <defs>
              <linearGradient id="ghr-grass" x1="0%" y1="100%" x2="0%" y2="0%">
                <stop offset="0%" stop-color="#2d7a32" />
                <stop offset="100%" stop-color="#248528" />
              </linearGradient>
              <linearGradient id="ghr-dirt" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#d98a4a" />
                <stop offset="100%" stop-color="#c06830" />
              </linearGradient>
            </defs>
            <g aria-hidden="true">
              <path d="M 240 590 L -179.5 165.8 A 572.2 572.2 0 0 1 659.5 165.8 Z" fill="url(#ghr-grass)" />
              <path d="M 240 575 L 415 410 C 445 410 355 270 240 270 C 120 270 40 410 65 410 Z" fill="url(#ghr-dirt)" />
              <polygon points="240,555 352.5,445.5 240,333 127.5,445.5" fill="url(#ghr-grass)" />
              <circle cx="240" cy="559" r="38.9" fill="url(#ghr-dirt)" />
              <circle cx="240" cy="448" r="16" fill="url(#ghr-dirt)" />
              <rect x="233" y="445" width="14" height="4" fill="#ffffff" rx="0.5" />
              <line x1="261" y1="545" x2="659.5" y2="165.8" stroke="#ffffff" />
              <line x1="218" y1="545" x2="-179.5" y2="165.8" stroke="#ffffff" />
              <path d="M -179.5 165.8 A 572.2 572.2 0 0 1 659.5 165.8" stroke="#ffffff" fill="none" />
              <polygon points="233,548 247,548 247,555 240,562 233,555" fill="#ffffff" />
              <rect x="218" y="545" width="10" height="22" fill="none" stroke="#ffffff" />
              <rect x="252" y="545" width="10" height="22" fill="none" stroke="#ffffff" />
              <g transform="rotate(45, 352.5, 445.5)">
                <rect x="347.5" y="440.5" width="10" height="10" fill="#ffffff" />
              </g>
              <g transform="rotate(45, 240, 333)">
                <rect x="235" y="328" width="10" height="10" fill="#ffffff" />
              </g>
              <g transform="rotate(45, 127.5, 445.5)">
                <rect x="122.5" y="440.5" width="10" height="10" fill="#ffffff" />
              </g>
            </g>
            <template v-if="showTrails">
              <path
                v-for="t in staticTrails"
                :key="'t' + t.id"
                :d="t.d"
                class="ghr-trail ghr-trail--static"
                :style="{ stroke: t.color }"
              />
            </template>
            <template v-for="(r, i) in visibleRecords" :key="i">
              <circle
                v-if="isHit(r)"
                :cx="r.sprayX"
                :cy="r.sprayY"
                :r="r === selected ? 9.5 : 6.5"
                class="ghr-dot ghr-dot--hit"
                :class="{ 'ghr-dot--on': r === selected }"
                :style="r === selected ? { fill: battedShapeColor(r.bipCode) } : null"
                @click="playSingle(r)"
              />
              <path
                v-else
                :d="crossD(r, r === selected ? 9.5 : 7)"
                class="ghr-dot ghr-dot--cross"
                :class="{ 'ghr-dot--on': r === selected }"
                @click="playSingle(r)"
              />
            </template>
            <template v-if="showTrails">
              <path
                v-for="(t, i) in animTrails"
                :key="'a' + i"
                :d="t.d"
                class="ghr-trail"
                :style="{ stroke: t.color }"
              />
            </template>
            <template v-for="(b, i) in animBalls" :key="'b' + i">
              <circle v-if="b" :cx="b.x" :cy="b.y" r="5" class="ghr-ball" />
            </template>
          </svg>
          <div class="ghr-legend">
            <span class="ghr-legend-item">
              <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true">
                <circle cx="7" cy="7" r="5.5" fill="#ffffff" stroke="#1f2d3d" />
              </svg>
              安打
            </span>
            <span class="ghr-legend-item">
              <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true">
                <path d="M 3 3 L 11 11 M 11 3 L 3 11" stroke="#f5222d" stroke-width="2.4" fill="none" stroke-linecap="round" />
              </svg>
              出局 / 非安打
            </span>
            <span class="ghr-legend-item ghr-legend-item--shapes" v-for="s in LEGEND_SHAPES" :key="s.label">
              <span class="ghr-legend-swatch" :style="{ background: s.color }"></span>
              {{ s.label }}
            </span>
          </div>
        </div>
        <ul class="ghr-list">
          <li
            v-for="(r, i) in visibleRecords"
            :key="i"
            class="ghr-item"
            :class="{ 'ghr-item--on': r === selected }"
            @click="playSingle(r)"
          >
            <span class="ghr-item-main">{{ inningLabel(r) }} · 第{{ r.batterOrder }}棒 {{ playerName(r) }}</span>
            <span class="ghr-item-sub">
              {{ resultLabel(r) }} · <span class="ghr-shape-dot" :style="{ background: battedShapeColor(r.bipCode) }"></span>{{ shapeLabel(r) }}<template v-if="zoneText(r)">（{{ zoneText(r) }}）</template><template v-if="r.rbi"> · {{ r.rbi }}打点</template>
            </span>
          </li>
        </ul>
      </div>
      <div class="ghr-actions">
        <el-button size="small" type="primary" plain :disabled="!visibleRecords.length" @click="playGroup()">同时重播</el-button>
        <span class="ghr-hint">点击场地圆点或左侧列表播放单球；「同时重播」同时播放当前分组全部击球的飞行</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { hitSprayApi } from '../../api/business';
import {
  BAT_SHAPE_COLORS,
  battedGeo,
  battedPathD,
  battedPointAt,
  battedShapeColor,
  battedShapeOf,
  bipCodeShape,
} from '../../utils/battedBallFlight';

const props = defineProps({
  gameId: { type: [Number, String], default: null },
  playersMap: { type: Object, default: () => ({}) },
  teams: { type: Array, default: () => [] },
  homeTeamId: { type: [Number, String], default: null },
  awayTeamId: { type: [Number, String], default: null },
});

const RESULT_LABELS = {
  H1: '一垒安打',
  H2: '二垒安打',
  H3: '三垒安打',
  HR: '全垒打',
  IPHR: '场内全垒打',
  OUT_G: '滚地出局',
  OUT_F: '飞球出局',
  OUT_L: '平飞出局',
  OUT_TAG: '触杀出局',
  SF: '高飞牺牲',
  SH: '牺牲触击',
  E: '失误上垒',
  FC: '野选',
  DP: '双杀出局',
  TP: '三杀出局',
};
const SHAPE_LABELS = {
  g: '滚地球',
  hg: '强袭滚地球',
  fly: '飞球',
  ld: '平飞球',
  b: '短打',
  pop: '内野高飞球',
};
const ZONE_LABELS = {
  P: '投手',
  C: '捕手',
  '1B': '一垒',
  '2B': '二垒',
  '3B': '三垒',
  SS: '游击',
  LF: '左外野',
  CF: '中外野',
  RF: '右外野',
  LCF: '左中外野',
  RCF: '右中外野',
  IF: '内野',
};
// 库中历史数据为英文枚举（shallow/medium/deep）
const DEPTH_LABELS = { shallow: '浅', medium: '中', deep: '深' };

// 图例（球种配色与轨迹同源）
const LEGEND_SHAPES = [
  { color: BAT_SHAPE_COLORS.g, label: '滚地球 / 强袭滚地球 / 短打' },
  { color: BAT_SHAPE_COLORS.ld, label: '平飞球' },
  { color: BAT_SHAPE_COLORS.fly, label: '飞球' },
  { color: BAT_SHAPE_COLORS.pop, label: '内野高飞球' },
];
const HIT_RESULTS = new Set(['H1', 'H2', 'H3', 'HR', 'IPHR']);

const loading = ref(false);
const records = ref([]);
const selected = ref(null);
const teamKey = ref('all');
const batterKey = ref('all');
const showTrails = ref(true);
const animIds = ref(new Set());
const animTrails = ref([]);
const animBalls = ref([]);
let animSeq = 0;
let rafId = 0;
let loadSeq = 0;

async function load(gameId) {
  if (!gameId) return;
  const seq = ++loadSeq;
  loading.value = true;
  try {
    const { data } = await hitSprayApi.getByGame(gameId);
    if (seq !== loadSeq) return;
    records.value = (Array.isArray(data) ? data : []).filter(
      (r) => r?.sprayX != null && r?.sprayY != null,
    );
    resetView();
  } catch (e) {
    if (seq !== loadSeq) return;
    console.error('加载落点数据失败:', e);
    records.value = [];
  } finally {
    if (seq === loadSeq) loading.value = false;
  }
}

watch(() => props.gameId, (id) => load(id), { immediate: true });

function resetView() {
  cancelAnim();
  selected.value = null;
  teamKey.value = 'all';
  batterKey.value = 'all';
}

function cancelAnim() {
  animSeq += 1;
  cancelAnimationFrame(rafId);
  animIds.value = new Set();
  animTrails.value = [];
  animBalls.value = [];
}

// 记录归属球队：优先用记录上的 teamId；历史数据为 0 时按上/下半局推出击球方（主/客队）
function teamIdOf(r) {
  if (r.teamId != null && Number(r.teamId) > 0) return Number(r.teamId);
  return Number(r.half === 'top' ? props.awayTeamId : props.homeTeamId);
}

function teamNameOf(teamId) {
  const team = (props.teams ?? []).find((t) => Number(t?.id) === Number(teamId));
  return team?.name ?? `球队#${teamId}`;
}

const teamOptions = computed(() => {
  const ids = [...new Set(records.value.map(teamIdOf))].filter((id) => Number.isFinite(id));
  return ids.map((id) => ({ id, name: teamNameOf(id) }));
});

const teamFiltered = computed(() => {
  if (teamKey.value === 'all') return records.value;
  const id = Number(teamKey.value);
  return records.value.filter((r) => teamIdOf(r) === id);
});

const batterOptions = computed(() => {
  const seen = new Map();
  for (const r of teamFiltered.value) {
    const key = `${teamIdOf(r)}:${r.playerId}`;
    if (!seen.has(key)) {
      seen.set(key, { key, label: `第${r.batterOrder}棒 ${playerName(r)}` });
    }
  }
  return [...seen.values()];
});

const visibleRecords = computed(() => {
  if (batterKey.value === 'all') return teamFiltered.value;
  const [teamId, playerId] = String(batterKey.value).split(':').map(Number);
  return teamFiltered.value.filter((r) => teamIdOf(r) === teamId && Number(r.playerId) === playerId);
});

function onFilterChange() {
  cancelAnim();
  selected.value = null;
  if (batterKey.value !== 'all') {
    const batterStillVisible = batterOptions.value.some((b) => b.key === batterKey.value);
    if (!batterStillVisible) batterKey.value = 'all';
  }
}

const staticTrails = computed(() =>
  visibleRecords.value
    .filter((r) => !animIds.value.has(r.id))
    .map((r) => ({
      id: r.id,
      d: battedPathD(r.sprayX, r.sprayY, battedShapeOf(r.bipCode).k),
      color: battedShapeColor(r.bipCode),
    })),
);

function playSingle(record) {
  selected.value = record;
  startFlight([record]);
}

function playGroup() {
  if (!visibleRecords.value.length) return;
  startFlight(visibleRecords.value);
}

// 生长中的轨迹 path（与录入页动画同法）：控制点随进度外移，球头停在 battedPointAt 处
function growingTrailD(meta, q, head) {
  if (meta.shape.k === 0) return `M 240 555 L ${head.x} ${head.y}`;
  const cx = 240 + (meta.geo.cx - 240) * q;
  const cy = 555 + (meta.geo.cy - 555) * q;
  return `M 240 555 Q ${cx} ${cy} ${head.x} ${head.y}`;
}

function startFlight(targets) {
  cancelAnim();
  const seq = animSeq;
  const metas = targets.map((r) => {
    const shape = battedShapeOf(r.bipCode);
    return { r, shape, geo: battedGeo(r.sprayX, r.sprayY, shape.k), color: battedShapeColor(r.bipCode) };
  });
  animIds.value = new Set(targets.map((t) => t.id));
  const start = performance.now();
  const frame = (now) => {
    if (seq !== animSeq) return;
    let allDone = true;
    const trails = new Array(metas.length);
    const balls = new Array(metas.length);
    metas.forEach((m, i) => {
      const x = Math.min((now - start) / m.shape.ms, 1);
      if (x < 1) allDone = false;
      const q = 1 - Math.pow(1 - x, 3);
      const pt = battedPointAt(m.r.sprayX, m.r.sprayY, m.shape.k, q);
      trails[i] = { d: growingTrailD(m, q, pt), color: m.color };
      balls[i] = x < 1 ? { x: pt.x, y: pt.y } : null; // 落到落点后隐藏球，落点只留标记（安打圆形/非安打×）
    });
    animTrails.value = trails;
    animBalls.value = balls;
    if (!allDone) rafId = requestAnimationFrame(frame);
  };
  rafId = requestAnimationFrame(frame);
}

onBeforeUnmount(() => {
  cancelAnim();
});

function inningLabel(r) {
  return `${r.inning}局${r.half === 'top' ? '上' : '下'}`;
}

function isHit(r) {
  return HIT_RESULTS.has(r.resultCode);
}

// 非安打落点用红色 ×（half = 半臂长）
function crossD(r, half) {
  const x = r.sprayX;
  const y = r.sprayY;
  return `M ${x - half} ${y - half} L ${x + half} ${y + half} M ${x - half} ${y + half} L ${x + half} ${y - half}`;
}

function playerName(r) {
  const map = props.playersMap ?? {};
  const player = map[`${r.teamId}:${r.playerId}`] ?? map[r.playerId];
  return player?.name ?? `球员#${r.playerId}`;
}

function resultLabel(r) {
  return RESULT_LABELS[r.resultCode] ?? r.resultCode ?? '击球';
}

function shapeLabel(r) {
  const shape = bipCodeShape(r.bipCode);
  return shape ? SHAPE_LABELS[shape] ?? shape : '击球';
}

function zoneText(r) {
  const zone = ZONE_LABELS[r.sprayZone];
  if (!zone) return '';
  const depth = DEPTH_LABELS[r.sprayDepth] ?? r.sprayDepth;
  return depth ? `${zone}·${depth}` : zone;
}
</script>

<style scoped>
.ghr-panel {
  margin-top: 16px;
}

.ghr-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
}

.ghr-title {
  font-size: 15px;
  font-weight: 600;
}

.ghr-count {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.ghr-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.ghr-filter-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.ghr-filter-team {
  width: 170px;
}

.ghr-filter-batter {
  width: 210px;
}

.ghr-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.ghr-field {
  flex: 0 0 320px;
  max-width: 320px;
}

.ghr-svg {
  width: 100%;
  height: auto;
  display: block;
  border-radius: 8px;
  background: #1d6a22;
}

.ghr-trail--static {
  stroke-width: 1.7;
  opacity: 0.85;
}

.ghr-dot {
  fill: #ffffff;
  opacity: 0.45;
  stroke: #1f2d3d;
  stroke-width: 1;
  cursor: pointer;
}

.ghr-dot--hit {
  opacity: 0.62;
}

.ghr-dot--cross {
  fill: none;
  stroke: #f5222d;
  stroke-width: 2.6;
  stroke-linecap: round;
  opacity: 0.8;
}

.ghr-dot--cross.ghr-dot--on {
  stroke: #f5222d;
  stroke-width: 3.4;
  opacity: 1;
}

.ghr-dot--on {
  opacity: 1;
  stroke: #ffffff;
  stroke-width: 2;
}

.ghr-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ghr-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.ghr-legend-swatch {
  display: inline-block;
  width: 16px;
  height: 3px;
  border-radius: 2px;
}

.ghr-trail {
  fill: none;
  stroke-width: 2.5;
  stroke-linecap: round;
  pointer-events: none;
}

.ghr-shape-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: 1px;
}

.ghr-ball {
  fill: #ffffff;
  stroke: #1f2d3d;
  stroke-width: 1.5;
  pointer-events: none;
}

.ghr-list {
  flex: 1 1 auto;
  min-width: 0;
  margin: 0;
  padding: 0;
  list-style: none;
  max-height: 420px;
  overflow-y: auto;
}

.ghr-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid transparent;
}

.ghr-item:hover {
  background: var(--el-fill-color-light);
}

.ghr-item--on {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.ghr-item-main {
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.ghr-item-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ghr-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.ghr-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

@media (max-width: 768px) {
  .ghr-body {
    flex-direction: column;
  }

  .ghr-field {
    flex: 0 0 auto;
    max-width: 420px;
    width: 100%;
  }

  .ghr-list {
    max-height: 320px;
    width: 100%;
  }
}
</style>
