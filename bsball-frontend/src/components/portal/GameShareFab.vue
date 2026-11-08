<template>
  <el-button
    circle
    class="game-share-fab"
    :title="t('gameShare.entry')"
    :aria-label="t('gameShare.entry')"
    @click="openShare"
  >
    <el-icon><Share /></el-icon>
  </el-button>

  <el-drawer
    v-model="visible"
    direction="btt"
    size="min(86vh, 640px)"
    :with-header="false"
    append-to-body
    destroy-on-close
    class="game-share-drawer"
    @closed="onClosed"
  >
    <div class="gs-sheet">
      <div class="gs-handle" aria-hidden="true"></div>
      <div class="gs-head">
        <div class="gs-head-main">
          <div class="gs-title">{{ t('gameShare.title') }}</div>
          <div class="gs-match">{{ shareTitle }}</div>
          <div v-if="shareMeta?.desc" class="gs-desc">{{ shareMeta.desc }}</div>
        </div>
        <button type="button" class="gs-close" :aria-label="t('gameShare.close')" @click="visible = false">×</button>
      </div>

      <div class="gs-actions">
        <button v-if="isWechat" type="button" class="gs-action" @click="showWechatGuide">
          <span class="gs-ico gs-ico--wechat">
            <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M9 4C5.7 4 3 6.2 3 9c0 1.6.9 3 2.3 3.9L5 15l2.2-1.1c.6.2 1.2.3 1.8.4-.1-.4-.2-.9-.2-1.3 0-3 2.9-5.4 6.4-5.4h.4C14.9 5.5 12.2 4 9 4zm-2.2 3.1c.5 0 .9.4.9.9s-.4.9-.9.9-.9-.4-.9-.9.4-.9.9-.9zm4.4 0c.5 0 .9.4.9.9s-.4.9-.9.9-.9-.4-.9-.9.4-.9.9-.9z" />
              <path d="M21 12.7c0-2.3-2.2-4.1-4.9-4.1s-4.9 1.8-4.9 4.1 2.2 4.1 4.9 4.1c.5 0 1.1-.1 1.6-.2L19 17l-.2-1.7c1.3-.7 2.2-1.6 2.2-2.6zm-6.5-1.2c.4 0 .7.3.7.7s-.3.7-.7.7-.7-.3-.7-.7.3-.7.7-.7zm3.2 0c.4 0 .7.3.7.7s-.3.7-.7.7-.7-.3-.7-.7.3-.7.7-.7z" />
            </svg>
          </span>
          <span>{{ t('gameShare.wechatFriend') }}</span>
        </button>
        <button v-if="isWechat" type="button" class="gs-action" @click="showWechatGuide">
          <span class="gs-ico gs-ico--wechat">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <circle cx="12" cy="12" r="3.2" />
              <circle cx="12" cy="4" r="1.6" />
              <circle cx="12" cy="20" r="1.6" />
              <circle cx="4" cy="8" r="1.6" />
              <circle cx="20" cy="8" r="1.6" />
              <circle cx="4" cy="16" r="1.6" />
              <circle cx="20" cy="16" r="1.6" />
            </svg>
          </span>
          <span>{{ t('gameShare.timeline') }}</span>
        </button>
        <button type="button" class="gs-action" @click="copyLink">
          <span class="gs-ico">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <rect x="8" y="8" width="11" height="11" rx="2" />
              <path d="M5 15V6a1 1 0 011-1h9" />
            </svg>
          </span>
          <span>{{ t('gameShare.copyLink') }}</span>
        </button>
        <button type="button" class="gs-action" :disabled="!dataReady || generating" @click="generatePoster">
          <span class="gs-ico gs-ico--gold">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <rect x="4" y="3" width="16" height="18" rx="2" />
              <path d="M8 8h8M8 12h5" />
              <circle cx="15.5" cy="16.5" r="2.5" />
            </svg>
          </span>
          <span>{{ t('gameShare.generatePoster') }}</span>
        </button>
      </div>

      <div v-if="!isWechat" class="gs-outside-tip">{{ t('gameShare.outsideWechatTip') }}</div>

      <div v-if="panelQrUrl" class="gs-qr-card">
        <img :src="panelQrUrl" class="gs-qr" :alt="t('gameShare.qrCaption')" />
        <div class="gs-qr-copy">
          <b>{{ t('gameShare.qrCaption') }}</b>
          <span>{{ displayHost }}</span>
        </div>
      </div>

      <div v-if="generating" class="gs-poster-loading">
        <span class="gs-spinner" aria-hidden="true"></span>
        {{ t('gameShare.posterGenerating') }}
      </div>
      <div v-else-if="posterPreview" class="gs-poster-result">
        <img :src="posterPreview" class="gs-poster-img" :alt="t('gameShare.posterAlt')" />
        <div class="gs-poster-btns">
          <el-button type="primary" size="small" @click="savePoster">{{ t('gameShare.savePoster') }}</el-button>
          <el-button size="small" plain @click="generatePoster">{{ t('gameShare.regeneratePoster') }}</el-button>
        </div>
        <p class="gs-longpress">{{ t('gameShare.longPressHint') }}</p>
      </div>

      <p class="gs-foot-hint">{{ t('gameShare.hint') }}</p>
    </div>
  </el-drawer>

  <!-- 战报海报离屏渲染区：尺寸 360x450，导出 3 倍图 -->
  <teleport to="body">
    <div ref="posterRef" class="gsp" aria-hidden="true">
      <div class="gsp-top">
        <span class="gsp-event">{{ eventName }}</span>
        <span class="gsp-game">{{ gameLabel }}</span>
      </div>
      <div class="gsp-score">
        <div class="gsp-team">
          <div class="gsp-badge">{{ awayInitial }}</div>
          <div class="gsp-team-name">{{ awayName }}</div>
          <div class="gsp-num">{{ awayScoreText }}</div>
        </div>
        <div class="gsp-colon">{{ scoreSeparator }}</div>
        <div class="gsp-team">
          <div class="gsp-badge">{{ homeInitial }}</div>
          <div class="gsp-team-name">{{ homeName }}</div>
          <div class="gsp-num">{{ homeScoreText }}</div>
        </div>
      </div>
      <div v-if="hasLineScore" class="gsp-line">
        <table>
          <thead>
            <tr>
              <th></th>
              <th v-for="inning in inningRows" :key="inning">{{ inning }}</th>
              <th>R</th>
              <th>H</th>
              <th>E</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="gsp-line-team">{{ awayName }}</td>
              <td v-for="(cell, i) in awayInningCells" :key="'a' + i">{{ cell }}</td>
              <td>{{ valueOrDash(game?.awayScore) }}</td>
              <td>{{ valueOrDash(game?.awayH) }}</td>
              <td>{{ valueOrDash(game?.awayE) }}</td>
            </tr>
            <tr>
              <td class="gsp-line-team">{{ homeName }}</td>
              <td v-for="(cell, i) in homeInningCells" :key="'h' + i">{{ cell }}</td>
              <td>{{ valueOrDash(game?.homeScore) }}</td>
              <td>{{ valueOrDash(game?.homeH) }}</td>
              <td>{{ valueOrDash(game?.homeE) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="gsp-meta">{{ posterDateTime }}<template v-if="venue"><br />{{ venue }}</template></div>
      <div class="gsp-bottom">
        <img v-if="qrUrl" :src="qrUrl" class="gsp-qr" alt="" />
        <div class="gsp-qr-cap">
          <b>{{ t('gameShare.qrCaption') }}</b>
          <span>{{ displayHost }}</span>
        </div>
      </div>
    </div>
  </teleport>

  <teleport to="body">
    <div v-if="showGuide" class="gs-guide" @click="showGuide = false">
      <div class="gs-guide-arrow" aria-hidden="true">↑</div>
      <div class="gs-guide-card">
        <div class="gs-guide-title">{{ t('gameShare.guideTitle') }}</div>
        <div class="gs-guide-body">{{ t('gameShare.guideBody') }}</div>
        <div class="gs-guide-ok">{{ t('gameShare.guideOk') }}</div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Share } from '@element-plus/icons-vue';
import QrCode from 'qrcode';
import html2canvas from 'html2canvas';
import { eventApi, gameApi, teamApi } from '../../api/business';
import { resolveAssetUrl } from '../../api/request';
import { buildGameShareMeta, buildGameShareUrl, gameDateTime } from '../../composables/useGameShare';
import { useWechatShare } from '../../composables/useWechatShare';
import { isWechatBrowser } from '../../utils/wechatJssdk';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const gameId = computed(() => Number(route.params.gameId) || 0);
const eventId = computed(() => Number(route.params.eventId) || 0);
const tenantCode = computed(() => String(route.params.tenantCode || ''));

const game = ref(null);
const homeTeam = ref(null);
const awayTeam = ref(null);
const event = ref(null);
const visible = ref(false);
const showGuide = ref(false);
const generating = ref(false);
const dataReady = ref(false);
const posterPreview = ref('');
const qrUrl = ref('');
const panelQrUrl = ref('');
const posterRef = ref(null);
const isWechat = computed(() => isWechatBrowser());
const displayHost = typeof window !== 'undefined' ? window.location.host : 'bsdone.com';
let loadSeq = 0;
let qrSeq = 0;
let posterObjectUrl = '';

async function loadData() {
  if (!gameId.value) return;
  const seq = ++loadSeq;
  dataReady.value = false;
  try {
    const data = await gameApi.get(gameId.value);
    if (seq !== loadSeq || !data) return;
    game.value = data;
    const [home, away, ev] = await Promise.all([
      data.homeTeamId ? teamApi.get(data.homeTeamId) : Promise.resolve(null),
      data.awayTeamId ? teamApi.get(data.awayTeamId) : Promise.resolve(null),
      data.eventId ? eventApi.get(data.eventId) : Promise.resolve(null)
    ]);
    if (seq !== loadSeq) return;
    homeTeam.value = home;
    awayTeam.value = away;
    event.value = ev;
    dataReady.value = true;
  } catch (e) {
    console.warn('[GameShare] 比赛分享数据加载失败', e);
  }
}

onMounted(() => {
  // 微信内需要尽早拿到比赛数据以配置右上角分享卡片；其他浏览器等用户打开面板再加载
  if (isWechat.value) loadData();
});
watch(gameId, () => {
  visible.value = false;
  showGuide.value = false;
  qrSeq += 1;
  qrUrl.value = '';
  panelQrUrl.value = '';
  posterPreview.value = '';
  revokePoster();
  game.value = null;
  homeTeam.value = null;
  awayTeam.value = null;
  event.value = null;
  dataReady.value = false;
  if (isWechat.value) loadData();
});

const shareUrl = computed(() =>
  gameId.value ? buildGameShareUrl(router, gameId.value, eventId.value, 'share', tenantCode.value) : ''
);
const posterShareUrl = computed(() =>
  gameId.value ? buildGameShareUrl(router, gameId.value, eventId.value, 'poster', tenantCode.value) : ''
);
const shareMeta = computed(() => {
  if (!shareUrl.value) return null;
  const meta = buildGameShareMeta(game.value, homeTeam.value, awayTeam.value, event.value, {
    resolveAsset: (url) => resolveAssetUrl(url) || '',
    fallbackHome: t('gameShare.fallbackHome'),
    fallbackAway: t('gameShare.fallbackAway')
  });
  if (!meta) return null;
  meta.link = shareUrl.value;
  return meta;
});
const shareTitle = computed(() => (dataReady.value ? shareMeta.value?.title : '') || t('gameShare.loading'));

useWechatShare(shareMeta);

const homeName = computed(() => teamLabel(homeTeam.value, t('gameShare.fallbackHome')));
const awayName = computed(() => teamLabel(awayTeam.value, t('gameShare.fallbackAway')));
const homeInitial = computed(() => firstChar(homeName.value));
const awayInitial = computed(() => firstChar(awayName.value));
const eventName = computed(() => event.value?.name || t('gameShare.fallbackEvent'));
const gameLabel = computed(() => (game.value?.gameNumber != null ? `GAME ${game.value.gameNumber}` : 'GAME'));
const hasScore = computed(() => {
  const status = String(game.value?.status || '');
  return (status === 'final' || status === 'live') && game.value?.homeScore != null && game.value?.awayScore != null;
});
const awayScoreText = computed(() => (hasScore.value ? game.value.awayScore : ''));
const homeScoreText = computed(() => (hasScore.value ? game.value.homeScore : ''));
const scoreSeparator = computed(() => (hasScore.value ? ':' : 'VS'));
const hasLineScore = computed(() => {
  const away = game.value?.awayScoreByInning;
  const home = game.value?.homeScoreByInning;
  return (Array.isArray(away) && away.length > 0) || (Array.isArray(home) && home.length > 0);
});
const inningRows = computed(() => {
  const away = Array.isArray(game.value?.awayScoreByInning) ? game.value.awayScoreByInning : [];
  const home = Array.isArray(game.value?.homeScoreByInning) ? game.value.homeScoreByInning : [];
  const total = Math.max(Number(game.value?.totalInnings) || 0, away.length, home.length, 1);
  return Array.from({ length: total }, (_, i) => i + 1);
});
const awayInningCells = computed(() => inningCells(game.value?.awayScoreByInning));
const homeInningCells = computed(() => inningCells(game.value?.homeScoreByInning));
const posterDateTime = computed(() => gameDateTime(game.value));
const venue = computed(() => String(game.value?.venue || '').trim());

function teamLabel(team, fallback) {
  return String(team?.name || team?.shortName || '').trim() || fallback;
}

function firstChar(name) {
  return Array.from(String(name || '?'))[0] || '?';
}

function valueOrDash(value) {
  return value == null || value === '' ? '–' : value;
}

function inningCells(values) {
  return inningRows.value.map((n) => {
    const v = Array.isArray(values) ? values[n - 1] : null;
    return v == null || v === '' ? '–' : v;
  });
}

async function ensureQr() {
  if (!shareUrl.value) return;
  if (qrUrl.value && panelQrUrl.value) return;
  const seq = ++qrSeq;
  const options = { width: 480, margin: 1, errorCorrectionLevel: 'M' };
  try {
    const [posterQr, panelQr] = await Promise.all([
      qrUrl.value || QrCode.toDataURL(posterShareUrl.value, options),
      panelQrUrl.value || QrCode.toDataURL(shareUrl.value, options)
    ]);
    if (seq === qrSeq) {
      qrUrl.value = posterQr;
      panelQrUrl.value = panelQr;
    }
  } catch {
    if (seq === qrSeq) {
      qrUrl.value = '';
      panelQrUrl.value = '';
    }
  }
}

function openShare() {
  if (!game.value) loadData();
  visible.value = true;
  ensureQr();
}

function showWechatGuide() {
  if (!isWechat.value) {
    ElMessage.info(t('gameShare.outsideWechatTip'));
    return;
  }
  showGuide.value = true;
}

async function copyLink() {
  const text = shareUrl.value;
  if (!text) return;
  try {
    if (navigator.clipboard && window.isSecureContext) {
      try {
        await navigator.clipboard.writeText(text);
      } catch {
        copyWithTextarea(text);
      }
    } else {
      copyWithTextarea(text);
    }
    ElMessage.success(t('gameShare.copySuccess'));
  } catch {
    ElMessage.error(t('gameShare.copyFail'));
  }
}

function copyWithTextarea(text) {
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.style.position = 'fixed';
  ta.style.left = '-9999px';
  document.body.appendChild(ta);
  ta.select();
  try {
    if (!document.execCommand('copy')) throw new Error('copy failed');
  } finally {
    ta.remove();
  }
}

async function generatePoster() {
  if (generating.value) return;
  if (!dataReady.value) {
    ElMessage.info(t('gameShare.dataLoading'));
    return;
  }
  generating.value = true;
  try {
    await ensureQr();
    await nextTick();
    const el = posterRef.value;
    if (!el) throw new Error('poster element missing');
    const canvas = await html2canvas(el, {
      scale: 3,
      useCORS: true,
      allowTaint: false,
      backgroundColor: '#0e2a47',
      logging: false
    });
    const blob = await new Promise((resolve, reject) => {
      canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('poster blob failed'))), 'image/png');
    });
    const url = URL.createObjectURL(blob);
    // 生成期间抽屉被关闭/切换比赛：丢弃结果，避免遗留 blob URL
    if (!visible.value) {
      URL.revokeObjectURL(url);
      return;
    }
    revokePoster();
    posterObjectUrl = url;
    posterPreview.value = url;
  } catch (e) {
    console.error('[GameShare] 海报生成失败', e);
    ElMessage.error(t('gameShare.posterFail'));
  } finally {
    generating.value = false;
  }
}

function savePoster() {
  if (!posterPreview.value) return;
  const link = document.createElement('a');
  link.href = posterPreview.value;
  link.download = `比赛战报-${gameId.value || 'game'}.png`;
  document.body.appendChild(link);
  link.click();
  link.remove();
}

function revokePoster() {
  if (posterObjectUrl) {
    URL.revokeObjectURL(posterObjectUrl);
    posterObjectUrl = '';
  }
}

function onClosed() {
  showGuide.value = false;
  generating.value = false;
  posterPreview.value = '';
  revokePoster();
}

onBeforeUnmount(() => {
  revokePoster();
  loadSeq += 1;
  qrSeq += 1;
});
</script>

<style scoped>
.game-share-fab {
  width: var(--portal-backtop-btn-size, 40px);
  height: var(--portal-backtop-btn-size, 40px);
  padding: 0;
  background: var(--el-bg-color-overlay, #fff);
  color: var(--el-text-color-primary);
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: var(--el-box-shadow-light, 0 2px 12px rgba(0, 0, 0, 0.12));
  box-sizing: border-box;
}

.game-share-fab:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

:global(.game-share-drawer .el-drawer__body) {
  padding: 0;
}

.gs-sheet {
  max-width: 560px;
  margin: 0 auto;
  padding: 8px 18px 24px;
  color: var(--el-text-color-primary);
}

.gs-handle {
  width: 38px;
  height: 4px;
  margin: 0 auto 12px;
  border-radius: 999px;
  background: var(--el-border-color);
}

.gs-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 6px;
}

.gs-head-main {
  flex: 1;
  min-width: 0;
}

.gs-title {
  font-size: 17px;
  font-weight: 700;
}

.gs-match {
  margin-top: 4px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.gs-desc {
  margin-top: 3px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.gs-close {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.gs-actions {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 18px;
}

.gs-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  width: 72px;
  padding: 2px 0;
  border: 0;
  background: none;
  color: var(--el-text-color-regular);
  font-size: 12px;
  cursor: pointer;
}

.gs-action:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.gs-ico {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
}

.gs-ico svg {
  width: 22px;
  height: 22px;
}

.gs-ico--wechat {
  background: #e8f8ee;
  color: #07c160;
}

.gs-ico--gold {
  background: #f7ecd2;
  color: #7a5a1f;
}

.gs-outside-tip {
  margin-top: 12px;
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.gs-qr-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.gs-qr {
  width: 64px;
  height: 64px;
  display: block;
}

.gs-qr-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.gs-qr-copy b {
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.gs-poster-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
  padding: 22px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.gs-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--el-border-color);
  border-top-color: var(--el-color-primary);
  border-radius: 50%;
  animation: gs-spin 0.8s linear infinite;
}

@keyframes gs-spin {
  to {
    transform: rotate(360deg);
  }
}

.gs-poster-result {
  margin-top: 16px;
  text-align: center;
}

.gs-poster-img {
  width: min(100%, 300px);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(9, 30, 51, 0.28);
  display: block;
  margin: 0 auto;
}

.gs-poster-btns {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
}

.gs-longpress {
  margin: 6px 0 0;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.gs-foot-hint {
  margin: 18px 0 0;
  font-size: 11px;
  line-height: 1.7;
  color: var(--el-text-color-placeholder);
}

.gs-guide {
  position: fixed;
  inset: 0;
  z-index: 4000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: 68px 16px 0;
  background: rgba(0, 0, 0, 0.78);
}

.gs-guide-arrow {
  margin-right: 18px;
  color: #fff;
  font-size: 30px;
  line-height: 1;
  animation: gs-bounce 1s ease-in-out infinite;
}

@keyframes gs-bounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

.gs-guide-card {
  max-width: 280px;
  margin-top: 6px;
  padding: 14px 16px;
  border-radius: 10px;
  background: rgba(17, 24, 39, 0.96);
  color: #fff;
  text-align: right;
}

.gs-guide-title {
  font-size: 14px;
  font-weight: 600;
}

.gs-guide-body {
  margin-top: 5px;
  font-size: 12px;
  color: #d1d5db;
  line-height: 1.6;
}

.gs-guide-ok {
  margin-top: 8px;
  color: #7dd3a8;
  font-size: 12px;
}

/* 战报海报：360x450，导出 3 倍图 */
.gsp {
  position: fixed;
  left: -10000px;
  top: 0;
  z-index: -1;
  width: 360px;
  height: 450px;
  padding: 22px 22px 16px;
  box-sizing: border-box;
  overflow: hidden;
  background: linear-gradient(165deg, #12395b 0%, #0e2a47 55%, #091e33 100%);
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.gsp-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.gsp-event {
  font-size: 12px;
  color: #e8dcc4;
  letter-spacing: 0.04em;
}

.gsp-game {
  flex: 0 0 auto;
  padding: 2px 7px;
  border: 1px solid rgba(201, 164, 92, 0.6);
  border-radius: 999px;
  color: #c9a45c;
  font-size: 9px;
}

.gsp-score {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
}

.gsp-team {
  width: 100px;
  text-align: center;
}

.gsp-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  margin: 0 auto 6px;
  border-radius: 50%;
  background: #fff;
  color: #0e2a47;
  font-size: 14px;
  font-weight: 800;
}

.gsp-team-name {
  font-size: 13px;
  font-weight: 600;
}

.gsp-num {
  margin-top: 4px;
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
}

.gsp-colon {
  color: #c9a45c;
  font-size: 20px;
}

.gsp-line {
  margin-top: 14px;
  padding: 7px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
  border-bottom: 1px solid rgba(255, 255, 255, 0.18);
  font-size: 9px;
  color: #d7dee8;
}

.gsp-line table {
  width: 100%;
  border-collapse: collapse;
}

.gsp-line th,
.gsp-line td {
  padding: 2px 2px;
  text-align: center;
  font-weight: 500;
}

.gsp-line th {
  color: #9fb0c3;
}

.gsp-line-team {
  max-width: 44px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gsp-meta {
  margin-top: 10px;
  color: #c7d2df;
  font-size: 10px;
  line-height: 1.7;
}

.gsp-bottom {
  position: absolute;
  right: 22px;
  bottom: 16px;
  left: 22px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.gsp-qr {
  width: 64px;
  height: 64px;
  padding: 4px;
  border-radius: 6px;
  background: #fff;
  display: block;
}

.gsp-qr-cap {
  display: flex;
  flex-direction: column;
  gap: 3px;
  color: #e8dcc4;
  font-size: 9px;
  text-align: right;
}

.gsp-qr-cap b {
  color: #fff;
  font-size: 11px;
}

@media (max-width: 480px) {
  .gs-actions {
    gap: 16px;
  }

  .gs-action {
    width: 66px;
  }

  .gs-ico {
    width: 42px;
    height: 42px;
  }

  .gs-sheet {
    padding: 8px 14px 20px;
  }
}

@media (max-width: 360px) {
  .gs-actions {
    gap: 6px;
  }

  .gs-action {
    width: 58px;
  }

  .gs-ico {
    width: 40px;
    height: 40px;
  }
}
</style>
