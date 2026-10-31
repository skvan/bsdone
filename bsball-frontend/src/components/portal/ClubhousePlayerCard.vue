<template>
  <div class="pcard" :style="slotStyle">
    <div class="pcard-inner" :style="{ transform: `scale(${scale})` }">
      <div class="pcard-frame">
        <div class="pcard-face">
          <img v-if="player.avatar" class="pcard-photo" :src="avatarUrl" :alt="player.name || '球员头像'" />
          <span v-else class="pcard-photo-fallback">{{ (player.name || '?').slice(0, 1) }}</span>
          <div class="pcard-tone"></div>
          <div class="pcard-shade"></div>
          <div class="pcard-brand"><i></i>BSD PLAYER CARD</div>
          <div v-if="score != null" class="pcard-rating">
            <div class="pcard-gem">
              <div class="pcard-gem-core"><b>{{ score }}</b><small>OVR</small></div>
            </div>
            <span class="pcard-grade">{{ gradeLabel }}</span>
          </div>
          <div class="pcard-strip"></div>
          <div class="pcard-strip-content">
            <div class="pcard-pos"><b>{{ positionLabel || '—' }}</b><span>{{ throwBatLabel }}</span></div>
            <span class="pcard-name">{{ player.name }}</span>
          </div>
        </div>
        <span v-for="spot in sparks" :key="spot.id" class="pcard-spark" :style="spot.style"></span>
        <div class="pcard-logo" :title="teamName">
          <img v-if="teamLogo && logoSrc" :src="logoSrc" :alt="teamName" />
          <span v-else-if="!teamLogo" class="pcard-logo-crest">{{ teamCrest }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { resolveAssetUrl } from '../../api/request';
import { whiteKeyedLogo } from './logoWhiteKey.js';
import {
  CARD_BASE_WIDTH,
  CARD_BASE_HEIGHT,
  cardGradeMaterial,
  gradeFrameGradient,
  gradeSheenGradient,
  gradeSparkSpots
} from './clubhouseCardGrade.js';

const props = defineProps({
  player: { type: Object, default: () => ({}) },
  grade: { type: String, default: '' },
  score: { type: Number, default: null },
  positionLabel: { type: String, default: '' },
  throwBatLabel: { type: String, default: '' },
  teamName: { type: String, default: '' },
  teamLogo: { type: String, default: '' },
  teamCrest: { type: String, default: '—' },
  scale: { type: Number, default: 1 }
});

const material = computed(() => cardGradeMaterial(props.grade));
const gradeLabel = computed(() => props.grade || material.value.key);
const avatarUrl = computed(() => resolveAssetUrl(props.player.avatar));
const sparks = computed(() => gradeSparkSpots(material.value));

const logoSrc = ref('');
let logoRequest = 0;
watch(() => props.teamLogo, async (url) => {
  const request = ++logoRequest;
  logoSrc.value = '';
  if (!url) {
    return;
  }
  const keyed = await whiteKeyedLogo(url);
  if (request === logoRequest) {
    logoSrc.value = keyed;
  }
}, { immediate: true });

const slotStyle = computed(() => ({
  width: `${Math.round(CARD_BASE_WIDTH * props.scale)}px`,
  height: `${Math.round(CARD_BASE_HEIGHT * props.scale)}px`,
  '--grade': material.value.color,
  '--grade-light': material.value.light,
  '--grade-dark': material.value.dark,
  '--grade-glow': material.value.glow,
  '--grade-glow2': material.value.glow2,
  '--bw': `${material.value.width}px`,
  '--frame-bg': gradeFrameGradient(material.value),
  '--sheen-bg': gradeSheenGradient(material.value)
}));
</script>

<style scoped>
.pcard { position: relative; filter: drop-shadow(0 9px 14px rgba(0, 0, 0, .42)) drop-shadow(0 0 9px var(--grade-glow)) drop-shadow(0 0 20px var(--grade-glow2)); }
.pcard-inner { position: relative; width: 204px; height: 286px; transform-origin: top left; }
.pcard-frame { position: absolute; inset: 0; padding: var(--bw); border-radius: 16px; background: var(--sheen-bg), var(--frame-bg); }
.pcard-face { position: relative; width: 100%; height: 100%; border-radius: calc(16px - var(--bw)); overflow: hidden; background: #0b1626; }
.pcard-photo { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; object-position: 50% 42%; filter: saturate(.92) contrast(1.03); }
.pcard-photo-fallback { position: absolute; inset: 0; display: grid; place-items: center; font-family: var(--bs-font-serif, Georgia, serif); font-size: 96px; color: #f7ebd4; background: radial-gradient(circle at 35% 22%, rgba(255, 255, 255, .16), transparent 38%), linear-gradient(145deg, #214a74, #0a1c33); }
.pcard-tone { position: absolute; inset: 0; background: linear-gradient(145deg, var(--grade-dark), transparent 35%, transparent 70%, var(--grade-dark)); opacity: .18; mix-blend-mode: color; }
.pcard-shade { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(3, 9, 17, .28) 0, transparent 25%, transparent 48%, rgba(3, 9, 17, .2) 60%, rgba(3, 9, 17, .94) 100%); }
.pcard-brand { position: absolute; left: 14px; top: 14px; display: flex; align-items: center; gap: 5px; font-size: 5.5px; letter-spacing: .17em; color: rgba(255, 255, 255, .82); text-shadow: 0 1px 4px #000; z-index: 2; }
.pcard-brand i { width: 5px; height: 5px; transform: rotate(45deg); background: var(--grade-light); }
.pcard-rating { position: absolute; right: 12px; top: 11px; width: 47px; height: 45px; z-index: 2; color: #fff; text-shadow: 0 2px 6px rgba(0, 0, 0, .75); }
.pcard-gem { position: relative; height: 100%; padding: 1.5px; background: linear-gradient(160deg, var(--grade-light), var(--grade), var(--grade-dark)); clip-path: polygon(18% 0, 82% 0, 100% 34%, 50% 100%, 0 34%); filter: drop-shadow(0 3px 6px rgba(0, 0, 0, .45)); }
.pcard-gem-core { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; padding-bottom: 3px; background: linear-gradient(165deg, rgba(34, 46, 62, .96), rgba(4, 10, 18, .95)), linear-gradient(115deg, transparent 44%, rgba(255, 255, 255, .16) 44% 56%, transparent 56%), linear-gradient(65deg, transparent 44%, rgba(255, 255, 255, .1) 44% 56%, transparent 56%); clip-path: polygon(17% 0, 83% 0, 100% 34%, 50% 100%, 0 34%); }
.pcard-gem-core b { font: 750 19px/.86 "Bahnschrift", "Segoe UI", sans-serif; letter-spacing: -.05em; }
.pcard-gem-core small { margin-top: 2px; font: 600 4.5px/1 "Bahnschrift", "Segoe UI", sans-serif; letter-spacing: .18em; color: var(--grade-light); }
.pcard-gem::after { content: ""; position: absolute; inset: 1.5px; background: linear-gradient(125deg, rgba(255, 255, 255, .3), transparent 34%); clip-path: polygon(18% 0, 82% 0, 100% 34%, 50% 100%, 0 34%); pointer-events: none; }
.pcard-grade { position: absolute; left: 50%; bottom: -5px; transform: translateX(-50%); min-width: 23px; height: 13px; padding: 0 4px; display: grid; place-items: center; border: 1px solid rgba(255, 255, 255, .42); border-radius: 3px; background: linear-gradient(145deg, var(--grade), var(--grade-dark)); font: 750 8px/1 "Bahnschrift", "Segoe UI", sans-serif; box-shadow: 0 2px 4px rgba(0, 0, 0, .38); }
.pcard-strip { position: absolute; left: -10px; right: -10px; bottom: -10px; height: 54px; transform: rotate(-4.5deg); background: repeating-linear-gradient(45deg, rgba(255, 255, 255, .11) 0 3px, transparent 3px 9px), linear-gradient(180deg, rgba(6, 10, 18, .42), rgba(4, 8, 14, .82)), linear-gradient(180deg, var(--grade-dark), var(--grade-dark)); border-top: 2px solid var(--grade); box-shadow: 0 -5px 14px rgba(0, 0, 0, .5); z-index: 2; }
.pcard-strip-content { position: absolute; left: 9px; right: 52px; bottom: 13px; height: 21px; display: flex; align-items: center; gap: 8px; padding: 0 10px; background: rgba(4, 8, 14, .2); border: 1px solid rgba(255, 255, 255, .16); z-index: 3; }
.pcard-pos { display: flex; flex-direction: column; line-height: 1; }
.pcard-pos b { font: italic 750 16px/1 "Bahnschrift", "Segoe UI", sans-serif; transform: skewX(-8deg); color: var(--grade-light); text-shadow: 0 1px 3px #000, 0 0 8px var(--grade-glow); }
.pcard-pos span { margin-top: 2px; font-size: 6px; line-height: 1; color: rgba(255, 255, 255, .75); letter-spacing: .08em; }
.pcard-name { flex: 1; text-align: center; font: 700 14px/1 "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; color: #fff; letter-spacing: .12em; text-shadow: 0 1px 4px #000; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pcard-spark { position: absolute; z-index: 4; background: #fff; opacity: .92; clip-path: polygon(50% 0, 62% 38%, 100% 50%, 62% 62%, 50% 100%, 38% 62%, 0 50%, 38% 38%); pointer-events: none; }
.pcard-logo { position: absolute; right: 2px; bottom: 2px; width: 72px; height: 72px; z-index: 5; filter: drop-shadow(0 3px 8px rgba(0, 0, 0, .6)); }
.pcard-logo img { width: 100%; height: 100%; object-fit: contain; }
.pcard-logo-crest { width: 100%; height: 100%; display: grid; place-items: center; border-radius: 12px; font-family: var(--bs-font-num, "Bahnschrift", sans-serif); font-size: 22px; font-weight: 800; color: #071a33; background: linear-gradient(145deg, var(--grade-light), var(--grade)); }
</style>
