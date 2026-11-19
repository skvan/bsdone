<template>
  <el-dialog
    v-model="visible"
    :title="t('clubhouse.share.imageTitle')"
    width="min(92vw, 720px)"
    append-to-body
    destroy-on-close
    class="cip-dialog"
    @closed="emit('closed')"
  >
    <div class="cip-body">
      <div v-if="result?.src" class="cip-frame">
        <img :src="result.src" :alt="t('clubhouse.share.imageTitle')" />
      </div>
      <p class="cip-meta">
        {{ t('clubhouse.share.imageMeta', {
          width: result?.width || 0,
          height: result?.height || 0,
          size: formatImageSize(result?.size || 0)
        }) }}
      </p>
      <p v-if="wechat" class="cip-hint">{{ t('clubhouse.share.imageLongPress') }}</p>
      <p v-else class="cip-hint">{{ t('clubhouse.share.imageSaveHint') }}</p>
    </div>
    <template #footer>
      <el-button @click="visible = false">{{ t('clubhouse.share.imageClose') }}</el-button>
      <el-button v-if="!wechat && result?.blob" type="primary" @click="emit('save')">
        {{ t('clubhouse.share.imageSave') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { formatImageSize } from '../../utils/clubhouseLongImage.js';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  result: { type: Object, default: null },
  wechat: { type: Boolean, default: false }
});
const emit = defineEmits(['update:modelValue', 'save', 'closed']);
const { t } = useI18n();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});
</script>

<style scoped>
.cip-dialog :deep(.el-dialog__body) { padding-top: 6px; }
.cip-body { display: grid; gap: 8px; }
.cip-frame { max-height: 70vh; overflow: auto; border: 1px solid var(--bs-hairline-soft); border-radius: 10px; background: var(--bs-surface-soft); }
.cip-frame img { display: block; width: 100%; height: auto; }
.cip-meta { margin: 0; color: var(--bs-text-muted); font-family: var(--bs-font-num); font-size: 11px; }
.cip-hint { margin: 0; color: var(--bs-accent-ink); font-size: 11px; line-height: 1.6; }
</style>
