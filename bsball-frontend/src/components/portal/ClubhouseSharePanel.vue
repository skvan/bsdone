<template>
  <el-drawer
    v-model="visible"
    direction="btt"
    size="min(86vh, 640px)"
    :with-header="false"
    append-to-body
    destroy-on-close
    class="clubhouse-share-drawer"
  >
    <div class="csp-sheet">
      <div class="csp-handle" aria-hidden="true"></div>
      <div class="csp-head">
        <div>
          <span class="csp-kicker">SHARE CLUBHOUSE</span>
          <h3>{{ t('clubhouse.share.title') }}</h3>
          <p v-if="readonlyView" class="csp-sub">{{ t('clubhouse.share.publicBadge') }}</p>
        </div>
        <button type="button" class="csp-close" :aria-label="t('common.cancel')" @click="visible = false">×</button>
      </div>

      <div v-if="!readonlyView" class="csp-switch-row">
        <div>
          <b>{{ t('clubhouse.share.publicLabel') }}</b>
          <span>{{ shareEnabled ? t('clubhouse.share.publicOn') : t('clubhouse.share.publicOff') }}</span>
        </div>
        <el-switch
          :model-value="shareEnabled"
          :loading="saving"
          :disabled="saving"
          :aria-label="t('clubhouse.share.publicLabel')"
          @change="(value) => emit('toggle', value)"
        />
      </div>
      <p class="csp-hint">{{ t('clubhouse.share.publicHint') }}</p>

      <div class="csp-scope">
        <span class="csp-scope-label">{{ t('clubhouse.share.scopeLabel') }}</span>
        <span v-for="(chip, index) in scopeChips" :key="`${index}-${chip}`" class="csp-chip">{{ chip }}</span>
      </div>

      <div class="csp-card" :class="{ 'is-disabled': !canShare }">
        <div class="csp-thumb">
          <img v-if="preview.imgUrl" :src="preview.imgUrl" alt="" crossorigin="anonymous" />
          <span v-else>BSD</span>
        </div>
        <div class="csp-meta">
          <b>{{ preview.title || t('clubhouse.share.previewTitle') }}</b>
          <span>{{ preview.desc || t('clubhouse.share.previewDesc') }}</span>
        </div>
      </div>

      <div class="csp-link-line" :title="shareUrl">
        <span>{{ shareUrl || t('clubhouse.share.linkPending') }}</span>
      </div>

      <div v-if="guideVisible" class="csp-guide" role="status">
        <b>{{ t('clubhouse.share.wechatGuideTitle') }}</b>
        <span>{{ t('clubhouse.share.wechatGuide') }}</span>
      </div>

      <div class="csp-actions">
        <button
          v-for="action in wechatActions"
          :key="action.label"
          type="button"
          class="csp-action"
          :disabled="!canShare || !isWechat"
          @click="showWechatGuide"
        >
          <span class="csp-action-icon">{{ action.icon }}</span>
          <span>{{ action.label }}</span>
        </button>
        <button type="button" class="csp-action" :disabled="!canShare" @click="copyLink">
          <span class="csp-action-icon">链</span>
          <span>{{ t('clubhouse.share.copyLink') }}</span>
        </button>
      </div>
      <p v-if="!isWechat" class="csp-hint">{{ t('clubhouse.share.outsideWechat') }}</p>
      <p v-else-if="!shareEnabled && !readonlyView" class="csp-hint">{{ t('clubhouse.share.enableFirst') }}</p>

      <slot name="poster" />
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { isWechatBrowser } from '../../utils/wechatJssdk';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  readonlyView: { type: Boolean, default: false },
  shareEnabled: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  shareUrl: { type: String, default: '' },
  preview: { type: Object, default: () => ({}) },
  scopeChips: { type: Array, default: () => [] }
});
const emit = defineEmits(['update:modelValue', 'toggle']);
const { t } = useI18n();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});
const guideVisible = ref(false);
const isWechat = computed(() => isWechatBrowser());
const canShare = computed(() => Boolean(props.shareEnabled && props.shareUrl && props.preview?.title));
const wechatActions = computed(() => [
  { icon: '微', label: t('clubhouse.share.wechatFriend') },
  { icon: '圈', label: t('clubhouse.share.timeline') }
]);
watch(visible, (open) => {
  if (!open) guideVisible.value = false;
});

function showWechatGuide() {
  if (!canShare.value || !isWechat.value) return;
  guideVisible.value = true;
}

async function copyLink() {
  if (!canShare.value) return;
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(props.shareUrl);
    } else {
      const textarea = document.createElement('textarea');
      textarea.value = props.shareUrl;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      const copied = document.execCommand('copy');
      textarea.remove();
      if (!copied) throw new Error('copy failed');
    }
    ElMessage.success(t('clubhouse.share.copySuccess'));
  } catch {
    ElMessage.error(t('clubhouse.share.copyFailed'));
  }
}
</script>

<style scoped>
.clubhouse-share-drawer :deep(.el-drawer__body) { padding: 0; background: var(--bs-canvas); }
.csp-sheet { max-width: 640px; margin: 0 auto; padding: 10px 22px 28px; color: var(--bs-text); background: var(--bs-canvas); font-family: var(--bs-font-sans); }
.csp-handle { width: 38px; height: 4px; margin: 0 auto 12px; border-radius: 2px; background: var(--bs-hairline); }
.csp-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 12px; }
.csp-kicker { display: block; font-size: 10px; font-weight: 800; letter-spacing: .12em; color: var(--bs-accent-ink); }
.csp-head h3 { margin: 4px 0 0; font-size: 20px; line-height: 1.2; color: var(--bs-ink); }
.csp-sub { margin: 4px 0 0; font-size: 11px; color: var(--bs-text-muted); }
.csp-close { width: 34px; height: 34px; border: 1px solid var(--bs-hairline); border-radius: 50%; background: var(--bs-surface); color: var(--bs-text-muted); font-size: 20px; line-height: 1; cursor: pointer; }
.csp-close:hover { border-color: var(--bs-accent); color: var(--bs-accent-ink); }
.csp-switch-row { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 12px 14px; border: 1px solid var(--bs-hairline-soft); border-radius: 12px; background: var(--bs-surface); }
.csp-switch-row b { display: block; font-size: 13px; color: var(--bs-ink); }
.csp-switch-row span { display: block; margin-top: 2px; font-size: 11px; color: var(--bs-text-muted); }
.csp-hint { margin: 8px 0 0; font-size: 11px; line-height: 1.6; color: var(--bs-text-muted); }
.csp-scope { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin: 12px 0 10px; }
.csp-scope-label { font-size: 10px; font-weight: 700; letter-spacing: .08em; color: var(--bs-accent-ink); }
.csp-chip { padding: 2px 8px; border: 1px solid var(--bs-hairline); border-radius: 999px; background: var(--bs-surface-soft); color: var(--bs-text); font-size: 10px; }
.csp-card { display: flex; gap: 11px; align-items: center; padding: 10px; border: 1px solid var(--bs-hairline-soft); border-radius: 12px; background: var(--bs-surface); }
.csp-card.is-disabled { opacity: .55; }
.csp-thumb { width: 52px; height: 52px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; overflow: hidden; border: 1px solid var(--bs-hairline); border-radius: 9px; background: var(--bs-surface-soft); color: var(--bs-accent-ink); font-size: 12px; font-weight: 800; }
.csp-thumb img { width: 100%; height: 100%; object-fit: cover; }
.csp-meta { min-width: 0; }
.csp-meta b { display: block; overflow: hidden; font-size: 13px; color: var(--bs-ink); text-overflow: ellipsis; white-space: nowrap; }
.csp-meta span { display: block; margin-top: 3px; overflow: hidden; font-size: 11px; line-height: 1.5; color: var(--bs-text-muted); text-overflow: ellipsis; white-space: nowrap; }
.csp-link-line { margin: 8px 0 10px; padding: 6px 9px; overflow: hidden; border: 1px dashed var(--bs-hairline); border-radius: 7px; color: var(--bs-text-faint); font-family: var(--bs-font-num); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.csp-guide { display: grid; gap: 3px; margin-bottom: 10px; padding: 9px 11px; border-left: 3px solid var(--bs-accent); border-radius: 6px; background: var(--bs-accent-soft); color: var(--bs-accent-ink); font-size: 11px; line-height: 1.6; }
.csp-guide b { font-size: 12px; }
.csp-actions { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.csp-action { display: flex; flex-direction: column; align-items: center; gap: 5px; min-height: 64px; padding: 9px 5px; border: 1px solid var(--bs-hairline); border-radius: 10px; background: var(--bs-surface); color: var(--bs-text); font: inherit; font-size: 11px; cursor: pointer; }
.csp-action:hover:not(:disabled) { border-color: var(--bs-accent); background: var(--bs-accent-soft); }
.csp-action:disabled { opacity: .4; cursor: not-allowed; }
.csp-action-icon { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 50%; background: var(--bs-accent-soft); color: var(--bs-accent-ink); font-size: 12px; font-weight: 800; }
@media (max-width: 560px) {
  .csp-sheet { padding: 10px 16px 24px; }
  .csp-actions { grid-template-columns: 1fr; }
  .csp-action { flex-direction: row; justify-content: flex-start; min-height: 48px; padding: 8px 11px; }
}
</style>
