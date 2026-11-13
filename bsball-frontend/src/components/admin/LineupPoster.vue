<template>
  <div class="lineup-poster-shell">
    <div ref="posterRef" class="lineup-poster" :class="{ 'lineup-poster--compact': battingRows.length > 9 }">
      <img
        v-if="teamMarkUrl || watermarkUrl"
        class="lineup-poster__bg-logo"
        :class="{ 'is-keyed': !watermarkSilhouetteUrl && (teamMarkUrl || watermarkKeyed) }"
        :src="watermarkSilhouetteUrl || teamMarkUrl || watermarkUrl"
        alt=""
      />
      <div v-else class="lineup-poster__bg-fallback">{{ teamInitial }}</div>
      <div class="lineup-poster__ring"></div>
      <div class="lineup-poster__vignette"></div>
      <div class="lineup-poster__frame"></div>

      <div class="lineup-poster__content">
        <div class="lineup-poster__topline">
          <span class="lineup-poster__pill">GAME DAY / STARTING LINEUP</span>
          <span class="lineup-poster__top-meta">{{ context?.season || 'LINEUP CARD' }}</span>
        </div>

        <div class="lineup-poster__header">
          <div class="lineup-poster__logo-card">
            <img v-if="teamMarkUrl || teamLogoRaw" :src="teamMarkUrl || watermarkUrl || teamLogoRaw" alt="" />
            <span v-else>{{ teamInitial }}</span>
          </div>
          <div class="lineup-poster__team-block">
            <div class="lineup-poster__team-name">{{ teamName }}</div>
            <div class="lineup-poster__event">{{ gameLabel }}</div>
            <div class="lineup-poster__meta">{{ dateText }}<template v-if="context?.venue"> · {{ context.venue }}</template></div>
          </div>
        </div>

        <div class="lineup-poster__matchup">
          <span class="lineup-poster__vs">VS</span>
          <span class="lineup-poster__opponent">{{ opponentText }}</span>
          <span class="lineup-poster__side-tag">{{ sideText }}</span>
        </div>

        <div class="lineup-poster__list-head">
          <span>棒次</span>
          <span>球员 / 背号</span>
          <span>守位</span>
        </div>

        <div class="lineup-poster__rows">
          <div v-for="row in battingRows" :key="row.id" class="lineup-poster__row">
            <span class="lineup-poster__order">{{ row.order }}</span>
            <div class="lineup-poster__player">
              <span class="lineup-poster__name">{{ row.name }}</span>
              <span class="lineup-poster__number">#{{ row.number || '-' }}</span>
            </div>
            <span class="lineup-poster__position">{{ row.position }}</span>
          </div>
        </div>

        <div v-if="pitcherRow" class="lineup-poster__pitcher">
          <span class="lineup-poster__pitcher-label">先发投手</span>
          <div class="lineup-poster__player">
            <span class="lineup-poster__name">{{ pitcherRow.name }}</span>
            <span class="lineup-poster__number">#{{ pitcherRow.number || '-' }}</span>
          </div>
          <span class="lineup-poster__position">投手</span>
        </div>

        <div class="lineup-poster__footer">
          <div class="lineup-poster__qr">
            <img v-if="qrUrl" :src="qrUrl" alt="" />
          </div>
          <div class="lineup-poster__qr-text">
            <b>扫码查看比赛</b>
            <span>赛程 · 比分 · 数据</span>
          </div>
          <div class="lineup-poster__brand">
            <img v-if="platformLogo" class="lineup-poster__brand-logo" :src="platformLogo" alt="" />
            <div>
              <b>BSD</b>
              <span>棒垒球数据平台</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import html2canvas from 'html2canvas';
import QrCode from 'qrcode';
import { resolveAssetUrl } from '../../api/request';
import { buildGameShareUrl } from '../../composables/useGameShare';
import { isPlaceholder, posOf } from '../../utils/lineupBoardRules';
import { keyTeamLogo } from '../portal/logoBackgroundKey';
import { useAppConfigStore } from '../../stores/appConfig';
import { currentTenantCodeFromUrl } from '../../utils/tenantRoute';
import { formatLineupDateTime } from '../../utils/teamLineupDraft';
import { waitForImages } from '../../utils/gameDetailCollage';

const props = defineProps({
  context: { type: Object, default: null },
  rows: { type: Array, default: () => [] },
  roster: { type: Array, default: () => [] },
  bench: { type: Array, default: () => [] },
  fieldingPitcherId: { type: [Number, String], default: null }
});

const route = useRoute();
const router = useRouter();
const appConfig = useAppConfigStore();
const posterRef = ref(null);
const qrUrl = ref('');
const watermarkUrl = ref('');
const watermarkKeyed = ref(false);
const watermarkSilhouetteUrl = ref('');

const POSITION_LABELS = {
  P: '投手', C: '捕手', '1B': '一垒手', '2B': '二垒手', '3B': '三垒手',
  SS: '游击手', LF: '左外野', CF: '中外野', RF: '右外野',
  DH: '指定打击', PH: '代打', PR: '代跑', XF: '自由人', XF1: '自由人①', XF2: '自由人②', EP: '额外打击者'
};

// 与球星卡一致：野钻上传件为红底位图，统一使用重绘的黑白矢量队标（海报用透明 PNG，html2canvas 可稳定导出）
const TEAM_MARKS = {
  '野钻': new URL('../portal/wild-diamond-mark.png', import.meta.url).href
};

function playerName(player, fallbackId) {
  return player?.name || `球员${fallbackId}`;
}

const teamName = computed(() => props.context?.teamName || '本队');
const teamInitial = computed(() => (teamName.value || '?').slice(0, 1));
const teamLogoRaw = computed(() => resolveAssetUrl(props.context?.teamLogo));
const teamMarkUrl = computed(() => {
  const names = String(teamName.value || '').split(' / ').map((name) => name.trim());
  const markTeam = names.find((name) => TEAM_MARKS[name]);
  return markTeam ? TEAM_MARKS[markTeam] : '';
});
const platformLogo = computed(() =>
  appConfig.resolveAssetUrl(appConfig.adminLogoUrl || appConfig.logoUrl || '/bs-ball-logo.png')
);
const battingRows = computed(() =>
  (props.rows ?? [])
    .filter((row) => !isPlaceholder(row))
    .map((row, index) => ({
      id: row.id,
      order: index + 1,
      name: playerName(row, row.id),
      number: row.number,
      position: POSITION_LABELS[posOf(row)] || row.position || '待定'
    }))
);
const pitcherRow = computed(() => {
  const pitcherId = Number(props.fieldingPitcherId);
  if (!Number.isFinite(pitcherId) || pitcherId <= 0) return null;
  if (battingRows.value.some((row) => String(row.id) === String(pitcherId))) return null;
  const player =
    (props.bench ?? []).find((row) => String(row.id) === String(pitcherId)) ??
    (props.roster ?? []).find((row) => String(row.id) === String(pitcherId));
  return player ? { name: playerName(player, pitcherId), number: player.number } : null;
});
const gameLabel = computed(() => {
  const parts = [props.context?.eventName, props.context?.gameNumber != null ? `第 ${props.context.gameNumber} 场` : ''];
  return parts.filter(Boolean).join(' · ') || '赛前阵容';
});
const dateText = computed(() => formatLineupDateTime(props.context?.gameTime || props.context?.gameday, '时间待定'));
const opponentText = computed(() => props.context?.opponentName || '对手待定');
const sideText = computed(() => (props.context?.side === 'home' ? '主场' : '客场'));
const tenantCode = computed(() => String(route.params.tenantCode || currentTenantCodeFromUrl() || ''));
const shareUrl = computed(() => {
  if (!props.context?.gameId || !props.context?.eventId) return '';
  return buildGameShareUrl(router, props.context.gameId, props.context.eventId, 'poster', tenantCode.value);
});

let logoTask = Promise.resolve();

/** html2canvas 不解析 CSS filter，白色剪影必须预渲染成图片再插入海报。 */
async function toWhiteSilhouette(url) {
  const image = new Image();
  image.decoding = 'async';
  image.src = new URL(url, window.location.origin).href;
  await image.decode();
  const canvas = document.createElement('canvas');
  canvas.width = image.naturalWidth;
  canvas.height = image.naturalHeight;
  const context = canvas.getContext('2d');
  if (!context) return '';
  context.drawImage(image, 0, 0);
  context.globalCompositeOperation = 'source-in';
  context.fillStyle = '#ffffff';
  context.fillRect(0, 0, canvas.width, canvas.height);
  return canvas.toDataURL('image/png');
}

watch([teamLogoRaw, teamMarkUrl], ([url, mark]) => {
  logoTask = (async () => {
    watermarkSilhouetteUrl.value = '';
    if (!url && !mark) {
      watermarkUrl.value = '';
      watermarkKeyed.value = false;
      return;
    }
    if (mark) {
      watermarkUrl.value = mark;
      watermarkKeyed.value = false;
      try {
        watermarkSilhouetteUrl.value = await toWhiteSilhouette(mark);
      } catch {
        // 剪影失败时退回 CSS filter 兜底
      }
      return;
    }
    try {
      const result = await keyTeamLogo(url);
      watermarkUrl.value = result?.url || url;
      watermarkKeyed.value = result?.keyed === true;
      if (watermarkKeyed.value) {
        try {
          watermarkSilhouetteUrl.value = await toWhiteSilhouette(watermarkUrl.value);
        } catch {
          // 剪影失败时退回 CSS filter 兜底
        }
      }
    } catch {
      watermarkUrl.value = url;
      watermarkKeyed.value = false;
    }
  })();
}, { immediate: true });

async function ensureQr() {
  if (qrUrl.value || !shareUrl.value) return;
  qrUrl.value = await QrCode.toDataURL(shareUrl.value, {
    width: 360,
    margin: 1,
    errorCorrectionLevel: 'M'
  });
}

async function generate() {
  await logoTask;
  await ensureQr();
  await nextTick();
  const el = posterRef.value;
  if (!el) throw new Error('poster element missing');
  await waitForImages(el);
  const canvas = await html2canvas(el, {
    scale: 3,
    useCORS: true,
    allowTaint: false,
    backgroundColor: '#071521',
    logging: false
  });
  const blob = await new Promise((resolve, reject) => {
    canvas.toBlob((value) => (value ? resolve(value) : reject(new Error('poster blob failed'))), 'image/png');
  });
  return { url: URL.createObjectURL(blob), blob };
}

defineExpose({ generate });
</script>

<style scoped>
.lineup-poster,
.lineup-poster * {
  box-sizing: border-box;
}

.lineup-poster-shell {
  position: fixed;
  left: -10000px;
  top: 0;
  z-index: -1;
  width: 900px;
  height: 1200px;
  overflow: hidden;
}
.lineup-poster {
  position: relative;
  width: 900px;
  height: 1200px;
  overflow: hidden;
  padding: 50px 54px 38px;
  display: flex;
  flex-direction: column;
  color: #f5f7fa;
  background:
    radial-gradient(900px 620px at 82% 8%, rgba(230, 184, 92, .16), transparent 62%),
    radial-gradient(780px 900px at 12% 108%, rgba(45, 143, 131, .14), transparent 60%),
    linear-gradient(160deg, #0a2237 0%, #071927 46%, #040d16 100%);
  font-family: "Microsoft YaHei", "PingFang SC", "Noto Sans SC", sans-serif;
}
.lineup-poster__bg-logo {
  position: absolute;
  width: 640px;
  left: 50%;
  top: 45%;
  transform: translate(-50%, -50%);
  opacity: .16;
  filter: blur(.5px) saturate(.85);
  pointer-events: none;
}
.lineup-poster__bg-logo.is-keyed {
  opacity: .18;
  filter: brightness(0) invert(1) blur(.4px);
}
.lineup-poster__bg-fallback {
  position: absolute;
  left: 50%;
  top: 45%;
  transform: translate(-50%, -50%);
  color: rgba(230, 184, 92, .1);
  font-size: 520px;
  font-weight: 900;
  line-height: 1;
  pointer-events: none;
}
.lineup-poster__ring {
  position: absolute;
  width: 760px;
  height: 760px;
  left: 50%;
  top: 45%;
  transform: translate(-50%, -50%);
  border: 1px solid rgba(230, 184, 92, .12);
  border-radius: 50%;
  box-shadow: 0 0 0 78px rgba(255, 255, 255, .012), 0 0 0 156px rgba(255, 255, 255, .008);
  pointer-events: none;
}
.lineup-poster__vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 42%, transparent 28%, rgba(2, 8, 14, .58) 100%);
  pointer-events: none;
}
.lineup-poster__frame {
  position: absolute;
  inset: 20px;
  border: 1px solid rgba(230, 184, 92, .3);
  border-radius: 4px;
  pointer-events: none;
}
.lineup-poster__frame::before,
.lineup-poster__frame::after {
  content: "";
  position: absolute;
  width: 84px;
  height: 2px;
  background: linear-gradient(90deg, rgba(230, 184, 92, .85), transparent);
  top: -1px;
  left: 26px;
}
.lineup-poster__frame::after {
  top: auto;
  bottom: -1px;
  left: auto;
  right: 26px;
  transform: rotate(180deg);
}
.lineup-poster__content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.lineup-poster__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.lineup-poster__pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border: 1px solid rgba(230, 184, 92, .6);
  border-radius: 999px;
  color: #e6b85c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .16em;
}
.lineup-poster__top-meta {
  color: #7f94a8;
  font-size: 12px;
  letter-spacing: .18em;
}
.lineup-poster__header {
  display: flex;
  align-items: center;
  gap: 22px;
  margin-top: 20px;
}
.lineup-poster__logo-card {
  flex: 0 0 auto;
  width: 116px;
  height: 116px;
  padding: 11px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(0, 0, 0, .34), inset 0 0 0 1px rgba(7, 21, 33, .08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #071521;
  font-size: 42px;
  font-weight: 900;
}
.lineup-poster__logo-card img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.lineup-poster__team-block { min-width: 0; }
.lineup-poster__team-name {
  font-size: 46px;
  line-height: 1.08;
  font-weight: 900;
  letter-spacing: .04em;
}
.lineup-poster__event {
  margin-top: 8px;
  color: #e6b85c;
  font-size: 19px;
  font-weight: 700;
  letter-spacing: .03em;
}
.lineup-poster__meta {
  margin-top: 5px;
  color: #9fb0c3;
  font-size: 15px;
  letter-spacing: .02em;
}
.lineup-poster__matchup {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-top: 24px;
  padding: 13px 18px;
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 12px;
  background: linear-gradient(90deg, rgba(255, 255, 255, .07), rgba(255, 255, 255, .018));
}
.lineup-poster__vs {
  color: #e6b85c;
  font-size: 19px;
  font-weight: 900;
  letter-spacing: .08em;
}
.lineup-poster__opponent {
  font-size: 25px;
  font-weight: 800;
  letter-spacing: .04em;
}
.lineup-poster__side-tag {
  margin-left: auto;
  padding: 5px 11px;
  border-radius: 999px;
  background: rgba(230, 184, 92, .12);
  color: #e6b85c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .08em;
}
.lineup-poster__list-head {
  display: grid;
  grid-template-columns: 62px 1fr 128px;
  align-items: end;
  margin-top: 18px;
  padding: 0 14px 8px 12px;
  border-bottom: 1px solid rgba(230, 184, 92, .28);
  color: #8298ad;
  font-size: 11px;
  letter-spacing: .14em;
}
.lineup-poster__list-head span:last-child { text-align: right; }
.lineup-poster__rows {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 7px;
  margin-top: 9px;
}
.lineup-poster__row {
  display: grid;
  grid-template-columns: 58px 1fr 128px;
  align-items: center;
  flex: 0 1 58px;
  min-height: 42px;
  padding: 0 14px 0 10px;
  border: 1px solid rgba(255, 255, 255, .055);
  border-radius: 10px;
  background: linear-gradient(90deg, rgba(255, 255, 255, .072), rgba(255, 255, 255, .024));
}
.lineup-poster__row:nth-child(even) {
  background: linear-gradient(90deg, rgba(255, 255, 255, .045), rgba(255, 255, 255, .014));
}
.lineup-poster--compact .lineup-poster__row { flex-basis: 50px; min-height: 38px; }
.lineup-poster--compact .lineup-poster__name { font-size: 22px; }
.lineup-poster__order {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  color: #071521;
  font-size: 19px;
  font-weight: 900;
  background: linear-gradient(135deg, #f4d58b, #c9953c);
  box-shadow: 0 4px 12px rgba(201, 149, 60, .22);
}
.lineup-poster__player {
  display: flex;
  align-items: baseline;
  gap: 13px;
  min-width: 0;
  padding-left: 3px;
}
.lineup-poster__name {
  overflow: hidden;
  color: #f6f9fc;
  font-size: 25px;
  font-weight: 800;
  letter-spacing: .03em;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.lineup-poster__number {
  flex: 0 0 auto;
  color: #e6b85c;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: .02em;
}
.lineup-poster__position {
  justify-self: end;
  min-width: 88px;
  padding: 6px 13px;
  border: 1px solid rgba(139, 233, 220, .5);
  border-radius: 999px;
  color: #9cefe3;
  background: rgba(45, 143, 131, .12);
  font-size: 14px;
  font-weight: 600;
  text-align: center;
  letter-spacing: .04em;
}
.lineup-poster__pitcher {
  display: grid;
  grid-template-columns: 104px 1fr 128px;
  align-items: center;
  flex: 0 0 58px;
  min-height: 44px;
  margin-top: 9px;
  padding: 0 14px;
  border: 1px solid rgba(230, 184, 92, .38);
  border-radius: 10px;
  background: linear-gradient(90deg, rgba(230, 184, 92, .19), rgba(230, 184, 92, .045));
}
.lineup-poster--compact .lineup-poster__pitcher { flex-basis: 50px; min-height: 40px; }
.lineup-poster__pitcher-label {
  color: #e6b85c;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: .08em;
}
.lineup-poster__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, .1);
}
.lineup-poster__qr {
  width: 84px;
  height: 84px;
  padding: 5px;
  border-radius: 8px;
  background: #fff;
  flex: 0 0 auto;
}
.lineup-poster__qr img { display: block; width: 100%; height: 100%; }
.lineup-poster__qr-text b {
  display: block;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: .04em;
}
.lineup-poster__qr-text span {
  display: block;
  margin-top: 3px;
  color: #8298ad;
  font-size: 12px;
}
.lineup-poster__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: right;
}
.lineup-poster__brand-logo {
  width: 34px;
  height: 34px;
  padding: 3px;
  border-radius: 8px;
  background: #fff;
  object-fit: contain;
}
.lineup-poster__brand b {
  display: block;
  color: #e6b85c;
  font-size: 16px;
  font-weight: 900;
  letter-spacing: .08em;
}
.lineup-poster__brand span {
  display: block;
  margin-top: 3px;
  color: #71879c;
  font-size: 11px;
  letter-spacing: .08em;
}
</style>
