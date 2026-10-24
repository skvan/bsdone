<template>
  <section class="lg-scoreboard" role="group" aria-label="实时记分牌">
    <div class="lg-scoreboard__grid lg-scoreboard__grid--top">
      <div class="lg-scoreboard__teams">
        <div
          :class="['lg-team-row', { 'is-batting': isTop }]"
          :style="{ '--lg-team-logo': logoSrc(awayLogo) }"
        >
          <span class="lg-team-side">客</span>
          <span class="lg-team-name">{{ awayName || '客队' }}</span>
          <span class="lg-team-score" aria-live="polite">{{ awayScore ?? 0 }}</span>
        </div>
        <div
          :class="['lg-team-row', { 'is-batting': !isTop }]"
          :style="{ '--lg-team-logo': logoSrc(homeLogo) }"
        >
          <span class="lg-team-side">主</span>
          <span class="lg-team-name">{{ homeName || '主队' }}</span>
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
          <div class="lg-light-group" :aria-label="`好球 ${strikes}`">
            <span class="lg-light-label">S</span>
            <span
              v-for="i in 2"
              :key="`s-${i}`"
              :class="['lg-light', 'lg-light--strike', { 'is-on': strikes >= i }]"
            ></span>
          </div>
          <div class="lg-light-group" :aria-label="`坏球 ${balls}`">
            <span class="lg-light-label">B</span>
            <span
              v-for="i in 3"
              :key="`b-${i}`"
              :class="['lg-light', 'lg-light--ball', { 'is-on': balls >= i }]"
            ></span>
          </div>
          <div class="lg-light-group" :aria-label="`出局 ${outs}`">
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
        <span class="lg-actor-label">打者</span>
        <span class="lg-actor-number">#{{ batter.number || '-' }}</span>
        <span class="lg-actor-name">{{ batter.name || '--' }}</span>
      </div>
      <span v-if="batter.order" class="lg-actor-extra lg-actor-extra--batter" :style="battingLogoStyle">{{ batter.order }}棒</span>
      <div class="lg-inning-number" :aria-label="`第${inning}局${isTop ? '上半局' : '下半局'}`">{{ inning }}局</div>
      <div class="lg-inning-arrows" aria-hidden="true">
        <span :class="['lg-inning-arrow', { 'is-on': isTop }]">▲</span>
        <span :class="['lg-inning-arrow', { 'is-on': !isTop }]">▼</span>
      </div>
      <div class="lg-actor-main lg-actor-main--pitcher" :style="fieldingLogoStyle">
        <span class="lg-actor-label">投手</span>
        <span class="lg-actor-number">#{{ pitcher.number || '-' }}</span>
        <span class="lg-actor-name">{{ pitcher.name || '--' }}</span>
      </div>
      <span class="lg-actor-extra lg-actor-extra--pitcher" :style="fieldingLogoStyle">NP {{ pitcher.np ?? 0 }}</span>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue';

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

const basesLabel = computed(() => {
  const names = { 1: '一垒', 2: '二垒', 3: '三垒' };
  const occupied = [1, 2, 3].filter((base) => props.bases?.[base]).map((base) => names[base]);
  return occupied.length ? `垒包：${occupied.join('、')}有人` : '垒包：无人';
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
.lg-scoreboard {
  --lg-line: rgba(255, 255, 255, 0.14);
  --lg-muted: #94a3b8;
  --lg-text: #f8fafc;
  --lg-gold: #fbbf24;
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 6;
  box-sizing: border-box;
  width: min(270px, calc(100% - 24px));
  padding: 8px 10px 7px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(10, 17, 31, 0.94), rgba(5, 10, 20, 0.96));
  color: var(--lg-text);
  box-shadow: 0 8px 24px rgba(2, 6, 23, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(4px);
  pointer-events: none;
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
  border: 1px solid rgba(15, 23, 42, 0.4);
  border-radius: 2px;
  background: #f8fafc;
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
  background: #0b1220;
  box-shadow: 0 0 0 1px rgba(251, 191, 36, 0.22), 0 0 7px rgba(251, 191, 36, 0.18);
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
  border-color: #ef4444;
  background: #ef4444;
  box-shadow: 0 0 6px rgba(239, 68, 68, 0.7);
}

.lg-light--strike.is-on {
  border-color: #22c55e;
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.65);
}

.lg-light--out.is-on {
  border-color: var(--lg-gold);
  background: var(--lg-gold);
  box-shadow: 0 0 6px rgba(251, 191, 36, 0.65);
}

@media (max-width: 640px) {
  .lg-scoreboard {
    top: 10px;
    right: 10px;
    width: min(260px, calc(100% - 20px));
    padding: 7px 9px 6px;
  }
}
</style>
