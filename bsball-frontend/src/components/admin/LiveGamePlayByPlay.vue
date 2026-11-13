<template>
  <section
    class="lg-pbp"
    :class="{ 'lg-pbp--side': isHorizontal }"
    :style="isHorizontal ? null : { height: `${height}px` }"
    :aria-label="lt('liveGame.pbp.aria')"
  >
    <div
      v-if="!isHorizontal"
      class="lg-pbp__resize"
      role="separator"
      aria-orientation="horizontal"
      :aria-valuemin="PBP_MIN_HEIGHT"
      :aria-valuemax="maxHeight"
      :aria-valuenow="height"
      :aria-label="lt('liveGame.pbp.resizeAria')"
      :title="lt('liveGame.pbp.resizeTitle')"
      tabindex="0"
      @pointerdown="startResize"
      @keydown="handleKeydown"
      @dblclick="resetHeight"
    >
      <span class="lg-pbp__grip" aria-hidden="true"></span>
    </div>

    <header class="lg-pbp__head">
      <span class="lg-pbp__title">PLAY BY PLAY</span>
      <span class="lg-pbp__hint">{{ lt('liveGame.pbp.hint') }}</span>
      <div class="lg-pbp__dock">
        <span class="lg-pbp__dock-label">吸附</span>
        <div class="lg-pbp__dock-switch" role="group" aria-label="日志吸附位置">
          <button
            v-for="item in visibleDocks"
            :key="item.key"
            type="button"
            class="lg-pbp__dock-btn"
            :class="{ 'is-active': dock === item.key }"
            :title="item.title"
            :aria-label="item.title"
            :aria-pressed="dock === item.key"
            @click="$emit('dock-change', item.key)"
          >
            <svg viewBox="0 0 18 18" fill="none" aria-hidden="true">
              <rect x="2.4" y="2.4" width="13.2" height="13.2" rx="2.6" stroke="currentColor" stroke-width="1.2" />
              <rect :x="item.bar.x" :y="item.bar.y" :width="item.bar.w" :height="item.bar.h" rx="1" fill="currentColor" />
            </svg>
          </button>
        </div>
        <span class="lg-pbp__count" :title="lt('liveGame.pbp.countTitle', { limit: PBP_LOG_LIMIT })">{{ lt('liveGame.pbp.count', { count: entries.length }) }}</span>
      </div>
    </header>
    <p class="lg-pbp__sr" aria-live="polite" aria-atomic="true">{{ latestAnnouncement }}</p>

    <div class="lg-pbp__list" :aria-label="lt('liveGame.pbp.listAria')" tabindex="0">
      <article
        v-for="(entry, index) in entries"
        :key="entry.key"
        :class="['lg-pbp__row', { 'is-latest': index === 0 }]"
      >
        <div class="lg-pbp__meta">
          <span v-if="entry.inning" class="lg-pbp__inning">{{ entry.inning }}</span>
          <time v-if="entry.time" class="lg-pbp__time">{{ entry.time }}</time>
        </div>
        <div class="lg-pbp__content">
          <p class="lg-pbp__text">
            <span>{{ entry.text }}</span>
            <span v-if="entry.tail" class="lg-pbp__tail">{{ entry.tail }}</span>
          </p>
          <p v-for="(line, lineIndex) in entry.lines" :key="lineIndex" class="lg-pbp__sub">{{ line }}</p>
        </div>
      </article>
      <p v-if="!entries.length" class="lg-pbp__empty">{{ lt('liveGame.pbp.empty') }}</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useLiveGameText } from '../../composables/useLiveGameText';
import {
  buildPbpEntries,
  clampPbpHeight,
  PBP_DEFAULT_RATIO,
  PBP_LOG_LIMIT,
  PBP_MIN_HEIGHT,
  PBP_STORAGE_KEY
} from '../../utils/liveGamePbp.js';
import { isHorizontalDock } from '../../utils/liveGamePbpDock.js';

const { text: lt } = useLiveGameText();

const props = defineProps({
  logs: { type: Array, default: () => [] },
  dock: { type: String, default: 'bottom' },
  allowHorizontal: { type: Boolean, default: true }
});
defineEmits(['dock-change']);

const DOCK_ITEMS = [
  { key: 'top', title: '吸附到上方', bar: { x: 4.6, y: 4.6, w: 8.8, h: 3.1 } },
  { key: 'bottom', title: '吸附到下方', bar: { x: 4.6, y: 10.3, w: 8.8, h: 3.1 } },
  { key: 'left', title: '吸附到左侧', bar: { x: 4.6, y: 4.6, w: 3.1, h: 8.8 } },
  { key: 'right', title: '吸附到右侧', bar: { x: 10.3, y: 4.6, w: 3.1, h: 8.8 } }
];

const entries = computed(() => buildPbpEntries(props.logs));
const latestAnnouncement = computed(() => {
  const latest = entries.value[0];
  if (!latest) return '';
  return latest.inning ? `${latest.inning} ${latest.text}` : latest.text;
});
const isHorizontal = computed(() => isHorizontalDock(props.dock));
const visibleDocks = computed(() => (
  props.allowHorizontal ? DOCK_ITEMS : DOCK_ITEMS.filter((item) => !isHorizontalDock(item.key))
));

const height = ref(initialHeight());
const maxHeight = ref(viewportMax());
let stopDrag = null;

function viewportHeight() {
  return typeof window === 'undefined' ? 0 : window.innerHeight;
}

function viewportMax() {
  // 传极大值复用 clamp 的上限计算，避免重复「屏占比」公式
  return clampPbpHeight(Number.MAX_SAFE_INTEGER, viewportHeight());
}

function readRatio() {
  try {
    const stored = Number(window.localStorage.getItem(PBP_STORAGE_KEY));
    return Number.isFinite(stored) && stored > 0 ? stored : PBP_DEFAULT_RATIO;
  } catch {
    return PBP_DEFAULT_RATIO;
  }
}

function initialHeight() {
  const viewport = viewportHeight();
  return clampPbpHeight(viewport * readRatio(), viewport);
}

function persist() {
  try {
    const viewport = viewportHeight();
    if (!viewport) return;
    window.localStorage.setItem(PBP_STORAGE_KEY, String(height.value / viewport));
  } catch {
    // 隐私模式等场景下无法写入，保持本次会话内的调整即可
  }
}

function resetHeight() {
  const viewport = viewportHeight();
  height.value = clampPbpHeight(viewport * PBP_DEFAULT_RATIO, viewport);
  persist();
}

function syncViewport() {
  maxHeight.value = viewportMax();
  height.value = clampPbpHeight(height.value, viewportHeight());
}

function stopResizing() {
  document.body.classList.remove('lg-pbp-resize-active');
  if (stopDrag) {
    stopDrag();
    stopDrag = null;
  }
  persist();
}

function startResize(event) {
  if (stopDrag || event.button !== 0) return;
  event.preventDefault();
  const startY = event.clientY;
  const startHeight = height.value;
  const move = (moveEvent) => {
    height.value = clampPbpHeight(startHeight + (startY - moveEvent.clientY), viewportHeight());
  };
  stopDrag = () => {
    window.removeEventListener('pointermove', move);
    window.removeEventListener('pointerup', stopResizing);
    window.removeEventListener('pointercancel', stopResizing);
  };
  document.body.classList.add('lg-pbp-resize-active');
  window.addEventListener('pointermove', move);
  window.addEventListener('pointerup', stopResizing);
  window.addEventListener('pointercancel', stopResizing);
}

function handleKeydown(event) {
  if (!['ArrowUp', 'ArrowDown', 'Home'].includes(event.key)) return;
  event.preventDefault();
  if (event.key === 'Home') {
    resetHeight();
    return;
  }
  const step = event.shiftKey ? 48 : 16;
  const delta = event.key === 'ArrowUp' ? step : -step;
  height.value = clampPbpHeight(height.value + delta, viewportHeight());
  persist();
}

onMounted(() => {
  syncViewport();
  window.addEventListener('resize', syncViewport);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport);
  stopResizing();
});
</script>

<style scoped>
.lg-pbp {
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  min-height: 0;
  position: relative;
  display: flex;
  flex-direction: column;
  margin: 0;
  box-sizing: border-box;
  background: var(--el-bg-color);
  overflow: hidden;
}

.lg-pbp:not(.lg-pbp--side) {
  flex: 0 0 auto;
  min-height: 120px;
  margin-top: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.lg-pbp__resize {
  flex: 0 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: row-resize;
  touch-action: none;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.lg-pbp__resize:hover,
.lg-pbp__resize:focus-visible {
  background: var(--el-color-primary-light-9);
}

.lg-pbp__resize:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: -2px;
}

.lg-pbp__grip {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: var(--el-border-color);
}

.lg-pbp__head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 6px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.lg-pbp__title {
  color: var(--el-text-color-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.lg-pbp__hint {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.lg-pbp__dock {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.lg-pbp__dock-label {
  color: var(--el-text-color-secondary);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.lg-pbp__dock-switch {
  display: flex;
  gap: 2px;
  padding: 2px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.lg-pbp__dock-btn {
  width: 26px;
  height: 24px;
  padding: 0;
  border: 0;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
}

.lg-pbp__dock-btn svg {
  width: 16px;
  height: 16px;
  display: block;
}

.lg-pbp__dock-btn:hover {
  color: var(--el-text-color-primary);
  background: var(--el-fill-color);
}

.lg-pbp__dock-btn.is-active {
  color: #e3c78f;
  background: #1d2636;
  box-shadow: 0 2px 6px rgba(29, 38, 54, 0.24);
}

.lg-pbp__count {
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.lg-pbp--side .lg-pbp__head {
  align-items: flex-start;
}

.lg-pbp--side .lg-pbp__dock {
  margin-left: 0;
  width: 100%;
  justify-content: space-between;
}

.lg-pbp--side .lg-pbp__count {
  margin-left: auto;
}

.lg-pbp__sr {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.lg-pbp__list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0 8px 8px;
}

.lg-pbp__list:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: -2px;
}

.lg-pbp__row {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 8px;
  padding: 5px 6px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  border-left: 3px solid transparent;
  border-radius: 4px;
}

.lg-pbp__row.is-latest {
  border-left-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.lg-pbp__meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.lg-pbp__inning {
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--el-fill-color);
  color: var(--el-text-color-regular);
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
}

.lg-pbp__time {
  color: var(--el-text-color-placeholder);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.lg-pbp__content {
  min-width: 0;
}

.lg-pbp__text {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.lg-pbp__tail {
  display: inline-block;
  margin-left: 6px;
  padding: 0 4px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  color: var(--el-text-color-regular);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
  line-height: 1.5;
  vertical-align: 1px;
}

.lg-pbp__sub {
  margin: 2px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.lg-pbp__empty {
  margin: 16px 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-align: center;
}

@media (max-width: 640px) {
  .lg-pbp__head {
    padding: 5px 9px;
  }

  .lg-pbp__hint {
    display: none;
  }

  .lg-pbp__row {
    grid-template-columns: 40px minmax(0, 1fr);
    gap: 6px;
    padding: 4px;
  }

  .lg-pbp__text {
    font-size: 12px;
  }
}
</style>
