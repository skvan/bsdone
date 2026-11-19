<template>
  <div
    ref="dockRef"
    :class="['lg-scoreboard-dock', { 'is-dragging': dragging, 'is-resizing': resizing }]"
    :style="dockStyle"
    @pointermove="onPointerMove"
    @pointerup="endInteraction"
    @pointercancel="endInteraction"
    @lostpointercapture="endInteraction"
    @click.stop
    @contextmenu.prevent
  >
    <section
      ref="cardRef"
      class="lg-scoreboard"
      role="group"
      :aria-label="lt('liveGame.scoreboard.aria')"
      :style="cardStyle"
    >
      <div
        class="lg-scoreboard__grid lg-scoreboard__grid--top lg-scoreboard__drag"
        @pointerdown.stop="onDragPointerDown"
      >
        <div class="lg-scoreboard__teams">
          <div
            :class="['lg-team-row', { 'is-batting': isTop }]"
            :style="{ '--lg-team-logo': logoSrc(awayLogo) }"
          >
            <span class="lg-team-side">{{ lt('liveGame.team.awayShort') }}</span>
            <span class="lg-team-name">{{ awayName || lt('liveGame.team.away') }}</span>
            <span class="lg-team-score" aria-live="polite">{{ awayScore ?? 0 }}</span>
          </div>
          <div
            :class="['lg-team-row', { 'is-batting': !isTop }]"
            :style="{ '--lg-team-logo': logoSrc(homeLogo) }"
          >
            <span class="lg-team-side">{{ lt('liveGame.team.homeShort') }}</span>
            <span class="lg-team-name">{{ homeName || lt('liveGame.team.home') }}</span>
            <span class="lg-team-score" aria-live="polite">{{ homeScore ?? 0 }}</span>
          </div>
        </div>

        <div class="lg-scoreboard__situation">
          <div class="lg-bases" role="img" :aria-label="basesLabel">
            <span
              v-for="base in [3, 2, 1]"
              :key="base"
              :class="['lg-base', `lg-base--${base}`, { 'is-on': !!bases?.[base] }]"
            ></span>
          </div>

          <div class="lg-lights">
            <div class="lg-light-group" :aria-label="lt('liveGame.scoreboard.strikesAria', { n: strikes })">
              <span class="lg-light-label">S</span>
              <span
                v-for="i in 2"
                :key="`s-${i}`"
                :class="['lg-light', 'lg-light--strike', { 'is-on': strikes >= i }]"
              ></span>
            </div>
            <div class="lg-light-group" :aria-label="lt('liveGame.scoreboard.ballsAria', { n: balls })">
              <span class="lg-light-label">B</span>
              <span
                v-for="i in 3"
                :key="`b-${i}`"
                :class="['lg-light', 'lg-light--ball', { 'is-on': balls >= i }]"
              ></span>
            </div>
            <div class="lg-light-group" :aria-label="lt('liveGame.scoreboard.outsAria', { n: outs })">
              <span class="lg-light-label">O</span>
              <span
                v-for="i in 2"
                :key="`o-${i}`"
                :class="['lg-light', 'lg-light--out', { 'is-on': outs >= i }]"
              ></span>
            </div>
          </div>
        </div>
      </div>

      <div class="lg-scoreboard__grid lg-scoreboard__grid--bottom">
        <div class="lg-actor-main lg-actor-main--batter" :style="battingLogoStyle">
          <span class="lg-actor-label">{{ lt('liveGame.scoreboard.batter') }}</span>
          <span class="lg-actor-number">#{{ batter.number || '-' }}</span>
          <span class="lg-actor-name">{{ batter.name || '--' }}</span>
        </div>
        <span v-if="batter.order" class="lg-actor-extra lg-actor-extra--batter" :style="battingLogoStyle">{{ orderText }}</span>
        <div class="lg-inning-number" :aria-label="lt('liveGame.scoreboard.inningAria', { inning, half: isTop ? lt('liveGame.scoreboard.top') : lt('liveGame.scoreboard.bottom') })">{{ lt('liveGame.scoreboard.inning', { inning }) }}</div>
        <div class="lg-inning-arrows" aria-hidden="true">
          <span :class="['lg-inning-arrow', { 'is-on': isTop }]">▲</span>
          <span :class="['lg-inning-arrow', { 'is-on': !isTop }]">▼</span>
        </div>
        <div class="lg-actor-main lg-actor-main--pitcher" :style="fieldingLogoStyle">
          <span class="lg-actor-label">{{ lt('liveGame.scoreboard.pitcher') }}</span>
          <span class="lg-actor-number">#{{ pitcher.number || '-' }}</span>
          <span class="lg-actor-name">{{ pitcher.name || '--' }}</span>
        </div>
        <span class="lg-actor-extra lg-actor-extra--pitcher" :style="fieldingLogoStyle">NP {{ pitcher.np ?? 0 }}</span>
      </div>
      <span
        class="lg-scoreboard-grip"
        aria-hidden="true"
        @pointerdown.stop="onResizePointerDown($event, 'se')"
      ></span>
    </section>

    <span
      v-for="dir in RESIZE_DIRS"
      :key="dir"
      :class="['lg-scoreboard-resize', `lg-scoreboard-resize--${dir}`]"
      aria-hidden="true"
      @pointerdown="onResizePointerDown($event, dir)"
    ></span>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useLiveGameText } from '../../composables/useLiveGameText';

const { isEnglish, text: lt } = useLiveGameText();

const props = defineProps({
  awayName: { type: String, default: '' },
  homeName: { type: String, default: '' },
  awayLogo: { type: String, default: '' },
  homeLogo: { type: String, default: '' },
  awayScore: { type: [Number, String], default: 0 },
  homeScore: { type: [Number, String], default: 0 },
  inning: { type: [Number, String], default: 1 },
  isTop: { type: Boolean, default: true },
  bases: { type: Object, default: () => ({ 1: false, 2: false, 3: false }) },
  balls: { type: Number, default: 0 },
  strikes: { type: Number, default: 0 },
  outs: { type: Number, default: 0 },
  batter: { type: Object, default: () => ({}) },
  pitcher: { type: Object, default: () => ({}) }
});

// 记分牌基准尺寸：拖拽/缩放只改变整体比例，内部字号与灯位保持等比
const BASE_WIDTH = 270;
const MIN_WIDTH = 240;
const MAX_SCALE = 2.4;
const MIN_SCALE = MIN_WIDTH / BASE_WIDTH;
const EDGE_GAP = 16;
const STORAGE_KEY = 'bs-ball-live-scoreboard-layout';
const RESIZE_DIRS = ['n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw'];

const dockRef = ref(null);
const cardRef = ref(null);
const layout = reactive({ left: null, top: null, scale: 1 });
const baseHeight = ref(72);
const dragging = ref(false);
const resizing = ref(false);

let container = null;
let observer = null;
let gesture = null;

const dockStyle = computed(() => {
  const { width, height } = cardSize();
  const style = { width: `${width}px`, height: `${height}px` };
  // 未主动移动时交给 CSS 定位（含小屏 10px 内边距），移动后再用绝对坐标
  if (layout.left != null && layout.top != null) {
    style.left = `${layout.left}px`;
    style.top = `${layout.top}px`;
    style.right = 'auto';
  }
  return style;
});

const cardStyle = computed(() => ({
  '--lg-base-width': `${BASE_WIDTH}px`,
  transform: `scale(${layout.scale})`
}));

function containerSize() {
  if (!container) return { width: 0, height: 0 };
  const rect = container.getBoundingClientRect();
  return { width: rect.width, height: rect.height };
}

// 记分牌在给定缩放比例下的整体尺寸
function cardSize(scale = layout.scale) {
  return { width: BASE_WIDTH * scale, height: baseHeight.value * scale };
}

function maxScale() {
  const { width, height } = containerSize();
  let max = MAX_SCALE;
  if (width > 0) max = Math.min(max, (width - EDGE_GAP * 2) / BASE_WIDTH);
  if (height > 0) max = Math.min(max, (height - EDGE_GAP * 2) / baseHeight.value);
  return Math.max(MIN_SCALE, max);
}

function clampScale(value) {
  const numeric = Number(value);
  const scale = Number.isFinite(numeric) && numeric > 0 ? numeric : 1;
  return Math.min(Math.max(scale, MIN_SCALE), maxScale());
}

// 位置以球场容器左上角为原点；四边都留出 EDGE_GAP，保证外扩的缩放手柄不被裁掉
function clampPosition(left, top, scale = layout.scale) {
  const { width, height } = containerSize();
  const { width: cardWidth, height: cardHeight } = cardSize(scale);
  return {
    left: Math.min(Math.max(EDGE_GAP, left), Math.max(EDGE_GAP, width - cardWidth - EDGE_GAP)),
    top: Math.min(Math.max(EDGE_GAP, top), Math.max(EDGE_GAP, height - cardHeight - EDGE_GAP))
  };
}

function measureBaseHeight() {
  const height = cardRef.value?.offsetHeight;
  if (height && Math.abs(height - baseHeight.value) > 0.5) baseHeight.value = height;
}

function saveLayout() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      left: layout.left,
      top: layout.top,
      scale: layout.scale
    }));
  } catch {
    // 隐私模式等禁用 localStorage 时忽略，仅本次会话生效
  }
}

function restoreLayout() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
    if (!saved) return;
    layout.scale = clampScale(saved.scale);
    if (Number.isFinite(saved.left) && Number.isFinite(saved.top)) {
      const position = clampPosition(saved.left, saved.top, layout.scale);
      layout.left = position.left;
      layout.top = position.top;
    }
  } catch {
    // 记录损坏时回退到默认位置
  }
}

function settleInsideContainer() {
  layout.scale = clampScale(layout.scale);
  if (layout.left == null || layout.top == null) return;
  const position = clampPosition(layout.left, layout.top, layout.scale);
  layout.left = position.left;
  layout.top = position.top;
}

function beginGesture(event, mode, direction = '') {
  if (event.pointerType === 'mouse' && event.button !== 0) return;
  const dock = dockRef.value;
  if (!dock || !container) return;
  event.preventDefault();
  const dockRect = dock.getBoundingClientRect();
  const containerRect = container.getBoundingClientRect();
  layout.left = dockRect.left - containerRect.left;
  layout.top = dockRect.top - containerRect.top;
  gesture = {
    mode,
    direction,
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    left: layout.left,
    top: layout.top,
    scale: layout.scale,
    ...cardSize()
  };
  if (mode === 'drag') dragging.value = true;
  else resizing.value = true;
  try {
    event.currentTarget?.setPointerCapture?.(event.pointerId);
  } catch {
    gesture = null;
    dragging.value = false;
    resizing.value = false;
  }
}

function onDragPointerDown(event) {
  if (gesture) return;
  beginGesture(event, 'drag');
}

function onResizePointerDown(event, direction) {
  if (gesture) return;
  event.stopPropagation();
  beginGesture(event, 'resize', direction);
}

function moveDrag(event, state) {
  const position = clampPosition(
    state.left + (event.clientX - state.startX),
    state.top + (event.clientY - state.startY)
  );
  layout.left = position.left;
  layout.top = position.top;
}

// 四边/四角统一按等比缩放：边缘看单轴位移，角落取两轴平均
function resizeDelta(event, state) {
  const dx = event.clientX - state.startX;
  const dy = event.clientY - state.startY;
  const deltas = [];
  if (state.direction.includes('e')) deltas.push(dx / BASE_WIDTH);
  if (state.direction.includes('w')) deltas.push(-dx / BASE_WIDTH);
  if (state.direction.includes('s')) deltas.push(dy / baseHeight.value);
  if (state.direction.includes('n')) deltas.push(-dy / baseHeight.value);
  if (!deltas.length) return 0;
  return deltas.reduce((sum, delta) => sum + delta, 0) / deltas.length;
}

function moveResize(event, state) {
  const nextScale = clampScale(state.scale + resizeDelta(event, state));
  const { width: nextWidth, height: nextHeight } = cardSize(nextScale);
  // 向西/向北缩放时锚定对边，保证对角位置不动
  const left = state.direction.includes('w') ? state.left + state.width - nextWidth : state.left;
  const top = state.direction.includes('n') ? state.top + state.height - nextHeight : state.top;
  const position = clampPosition(left, top, nextScale);
  layout.scale = nextScale;
  layout.left = position.left;
  layout.top = position.top;
}

function onPointerMove(event) {
  if (!gesture || event.pointerId !== gesture.pointerId) return;
  if (gesture.mode === 'drag') moveDrag(event, gesture);
  else moveResize(event, gesture);
}

function endInteraction(event) {
  if (!gesture || (event && event.pointerId !== gesture.pointerId)) return;
  gesture = null;
  dragging.value = false;
  resizing.value = false;
  saveLayout();
}

onMounted(async () => {
  await nextTick();
  container = dockRef.value?.closest('.field-svg-wrap') || dockRef.value?.parentElement || null;
  measureBaseHeight();
  restoreLayout();
  if (!container) return;
  observer = new ResizeObserver(() => {
    measureBaseHeight();
    settleInsideContainer();
  });
  observer.observe(container);
  if (cardRef.value) observer.observe(cardRef.value);
});

onBeforeUnmount(() => {
  observer?.disconnect();
  observer = null;
});

const ordinal = (n) => {
  const value = Number(n);
  if (!Number.isFinite(value) || value <= 0) return '';
  const mod100 = value % 100;
  if (mod100 >= 11 && mod100 <= 13) return `${value}th`;
  return `${value}${['th', 'st', 'nd', 'rd'][value % 10] ?? 'th'}`;
};
const orderText = computed(() => (isEnglish.value ? ordinal(props.batter?.order) : lt('liveGame.scoreboard.order', { n: props.batter?.order })));

const basesLabel = computed(() => {
  const occupied = [1, 2, 3].filter((base) => props.bases?.[base]).map((base) => lt(`liveGame.scoreboard.base${base}`));
  return occupied.length
    ? lt('liveGame.scoreboard.basesOccupied', { bases: occupied.join(isEnglish.value ? ', ' : '、') })
    : lt('liveGame.scoreboard.basesEmpty');
});

const logoSrc = (logo) => (logo ? `url('${logo}')` : 'none');
const battingLogoStyle = computed(() => ({
  '--lg-team-logo': logoSrc(props.isTop ? props.awayLogo : props.homeLogo)
}));
const fieldingLogoStyle = computed(() => ({
  '--lg-team-logo': logoSrc(props.isTop ? props.homeLogo : props.awayLogo)
}));
</script>

<style scoped>
.lg-scoreboard-dock {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 6;
  pointer-events: none;
  touch-action: none;
  user-select: none;
  -webkit-touch-callout: none;
}

.lg-scoreboard-dock.is-dragging,
.lg-scoreboard-dock.is-resizing {
  z-index: 7;
}

.lg-scoreboard {
  --lg-line: rgba(255, 255, 255, 0.14);
  --lg-muted: #94a3b8;
  --lg-text: #f8fafc;
  --lg-gold: #fbbf24;
  position: absolute;
  top: 0;
  left: 0;
  box-sizing: border-box;
  width: var(--lg-base-width);
  padding: 8px 10px 7px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(10, 17, 31, 0.94), rgba(5, 10, 20, 0.96));
  color: var(--lg-text);
  box-shadow: 0 8px 24px rgba(2, 6, 23, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(4px);
  transform-origin: top left;
  pointer-events: none;
}

/* 顶部标题区是唯一拖动区，其余牌面让位给球场点击（守备员/落点） */
.lg-scoreboard__drag {
  position: relative;
  pointer-events: auto;
  touch-action: none;
}

.lg-scoreboard__drag::before {
  content: "";
  position: absolute;
  top: -5px;
  left: 50%;
  width: 22px;
  height: 3px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.55);
  transform: translateX(-50%);
  transition: background 0.15s;
}

@media (hover: hover) {
  .lg-scoreboard-dock:hover .lg-scoreboard {
    border-color: rgba(148, 163, 184, 0.5);
  }

  .lg-scoreboard__drag:hover::before {
    background: rgba(203, 213, 225, 0.85);
  }
}

/* 边框/四角拖动缩放：命中区贴在边框上，视觉只保留右下角一道细握把 */
.lg-scoreboard-resize {
  position: absolute;
  z-index: 2;
  pointer-events: auto;
  touch-action: none;
}

/* 命中区贴着边框外侧，只向牌内探 1px，避免盖住贴边的守备员/落点 */
.lg-scoreboard-resize--n,
.lg-scoreboard-resize--s {
  left: 12px;
  right: 12px;
  height: 12px;
}

.lg-scoreboard-resize--n {
  top: -11px;
  cursor: ns-resize;
}

.lg-scoreboard-resize--s {
  bottom: -11px;
  cursor: ns-resize;
}

.lg-scoreboard-resize--e,
.lg-scoreboard-resize--w {
  top: 12px;
  bottom: 12px;
  width: 12px;
}

.lg-scoreboard-resize--e {
  right: -11px;
  cursor: ew-resize;
}

.lg-scoreboard-resize--w {
  left: -11px;
  cursor: ew-resize;
}

.lg-scoreboard-resize--ne,
.lg-scoreboard-resize--nw,
.lg-scoreboard-resize--se,
.lg-scoreboard-resize--sw {
  width: 16px;
  height: 16px;
}

.lg-scoreboard-resize--ne {
  top: -15px;
  right: -15px;
  cursor: nesw-resize;
}

.lg-scoreboard-resize--nw {
  top: -15px;
  left: -15px;
  cursor: nwse-resize;
}

.lg-scoreboard-resize--se {
  right: -15px;
  bottom: -15px;
  cursor: nwse-resize;
}

.lg-scoreboard-resize--sw {
  bottom: -15px;
  left: -15px;
  cursor: nesw-resize;
}

/* 右下角可见握把同时也是可命中的缩放把手，避免“看得见点不到” */
.lg-scoreboard-grip {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 16px;
  height: 16px;
  pointer-events: auto;
  touch-action: none;
  cursor: nwse-resize;
}

.lg-scoreboard-grip::after {
  content: "";
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 6px;
  height: 6px;
  border-right: 1px solid rgba(203, 213, 225, 0.55);
  border-bottom: 1px solid rgba(203, 213, 225, 0.55);
  border-radius: 0 0 1px 0;
}

.lg-scoreboard__grid {
  display: grid;
  grid-template-columns: minmax(0, 11fr) minmax(0, 4fr) minmax(0, 3fr) minmax(0, 2fr);
  column-gap: 3px;
}

.lg-scoreboard__grid--top {
  align-items: center;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--lg-line);
}

.lg-scoreboard__grid--bottom {
  grid-template-rows: 13px 13px;
  row-gap: 2px;
  align-items: center;
  padding-top: 5px;
}

.lg-scoreboard__teams {
  grid-column: 1 / span 2;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.lg-actor-main,
.lg-actor-extra {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  border-radius: 4px;
}

.lg-actor-main::before,
.lg-actor-extra::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -1;
  background-image: var(--lg-team-logo, none);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  opacity: 0.16;
  filter: saturate(0.75) contrast(0.9);
  pointer-events: none;
}

.lg-actor-main::before {
  left: 30px;
}

.lg-actor-main {
  display: flex;
  align-items: baseline;
  gap: 5px;
  min-width: 0;
  line-height: 1.2;
}

.lg-actor-main--batter {
  grid-column: 1;
  grid-row: 2;
}

.lg-actor-main--pitcher {
  grid-column: 1;
  grid-row: 1;
}

.lg-actor-label {
  flex: 0 0 25px;
  color: var(--lg-muted);
  font-size: 9px;
  letter-spacing: 0.1em;
}

.lg-actor-number {
  flex: 0 0 auto;
  color: var(--lg-gold);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.lg-actor-name {
  min-width: 0;
  overflow: hidden;
  color: var(--lg-text);
  font-size: 11px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lg-actor-extra {
  justify-self: end;
  min-width: 0;
  color: var(--lg-muted);
  font-size: 9px;
  font-variant-numeric: tabular-nums;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lg-actor-extra--batter {
  grid-column: 2;
  grid-row: 2;
}

.lg-actor-extra--pitcher {
  grid-column: 2;
  grid-row: 1;
}

.lg-team-row {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr) auto;
  gap: 4px;
  align-items: baseline;
  min-width: 0;
  padding: 1px 4px;
  border-radius: 5px;
  line-height: 1.15;
  overflow: hidden;
}

.lg-team-row::before {
  content: "";
  position: absolute;
  inset: 0 0 0 22px;
  z-index: 0;
  background-image: var(--lg-team-logo, none);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  opacity: 0.16;
  filter: saturate(0.75) contrast(0.9);
  pointer-events: none;
}

.lg-team-row > * {
  position: relative;
  z-index: 1;
}

.lg-team-row.is-batting {
  background: rgba(251, 191, 36, 0.14);
  box-shadow: inset 2px 0 0 var(--lg-gold);
}

.lg-team-side {
  color: var(--lg-muted);
  font-size: 9px;
}

.lg-team-name {
  min-width: 0;
  overflow: hidden;
  color: var(--lg-text);
  font-size: 11px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lg-team-score {
  color: var(--lg-text);
  font-size: 16px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.lg-inning-number {
  grid-column: 3;
  grid-row: 1 / span 2;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  color: var(--lg-text);
  font-size: 15px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  text-align: right;
  white-space: nowrap;
}

.lg-inning-arrows {
  grid-column: 4;
  grid-row: 1 / span 2;
  display: grid;
  grid-template-rows: 13px 13px;
  row-gap: 2px;
  align-items: center;
  justify-items: center;
  align-self: stretch;
  line-height: 1;
}

.lg-inning-arrow {
  color: rgba(148, 163, 184, 0.35);
  font-size: 10px;
}

.lg-inning-arrow.is-on {
  color: var(--lg-gold);
  text-shadow: 0 0 6px rgba(251, 191, 36, 0.55);
}

.lg-scoreboard__situation {
  grid-column: 3 / span 2;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
  min-width: 0;
}

.lg-bases {
  position: relative;
  flex: 0 0 auto;
  width: 28px;
  height: 22px;
}

.lg-base {
  position: absolute;
  box-sizing: border-box;
  width: 9px;
  height: 9px;
  border: 1px solid rgba(148, 163, 184, 0.75);
  border-radius: 2px;
  background: transparent;
  transform: rotate(45deg);
  transition: background 0.12s, border-color 0.12s, box-shadow 0.12s;
}

.lg-base--2 {
  top: 0;
  left: 9px;
}

.lg-base--3 {
  bottom: 0;
  left: 1px;
}

.lg-base--1 {
  right: 1px;
  bottom: 0;
}

.lg-base.is-on {
  border: 2px solid var(--lg-gold);
  background: var(--lg-gold);
  box-shadow: 0 0 0 1px rgba(251, 191, 36, 0.22), 0 0 7px rgba(251, 191, 36, 0.55);
}

.lg-lights {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.lg-light-group {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  min-height: 8px;
}

.lg-light-label {
  width: 8px;
  color: var(--lg-muted);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.lg-light {
  box-sizing: border-box;
  width: 6px;
  height: 6px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}

.lg-light--ball.is-on {
  border-color: #22c55e;
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.65);
}

.lg-light--strike.is-on {
  border-color: #facc15;
  background: #facc15;
  box-shadow: 0 0 6px rgba(250, 204, 21, 0.65);
}

.lg-light--out.is-on {
  border-color: #ef4444;
  background: #ef4444;
  box-shadow: 0 0 6px rgba(239, 68, 68, 0.7);
}

</style>
