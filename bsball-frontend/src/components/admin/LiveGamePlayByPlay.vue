<template>
  <section class="lg-pbp" :class="{ 'lg-pbp--side': isHorizontal }" aria-label="Play by Play 实时动态">
    <header class="lg-pbp__head">
      <span class="lg-pbp__title">PLAY BY PLAY</span>
      <span class="lg-pbp__hint">实时动态 · 最新在上</span>
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
        <span class="lg-pbp__count" :title="`最多显示最近 ${PBP_LOG_LIMIT} 条`">{{ entries.length }} 条</span>
      </div>
    </header>
    <p class="lg-pbp__sr" aria-live="polite" aria-atomic="true">{{ latestAnnouncement }}</p>

    <div class="lg-pbp__list" aria-label="实时比赛日志" tabindex="0">
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
      <p v-if="!entries.length" class="lg-pbp__empty">暂无比赛日志</p>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue';
import { buildPbpEntries, PBP_LOG_LIMIT } from '../../utils/liveGamePbp.js';
import { isHorizontalDock } from '../../utils/liveGamePbpDock.js';

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
