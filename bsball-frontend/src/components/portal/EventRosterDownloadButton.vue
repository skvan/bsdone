<template>
  <el-button
    type="primary"
    link
    class="event-header-pdf-btn"
    :class="{ 'event-header-pdf-btn--icon-only': iconOnly }"
    :loading="loading"
    :aria-label="t('events.downloadRosterPdf')"
    @click.stop="download"
  >
    <el-icon><Tickets /></el-icon>
    <span v-if="!iconOnly">{{ t('events.downloadRosterPdf') }}</span>
  </el-button>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Tickets } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import { fetchResult } from '../../api/request';
import { loadEventOrderBookData } from '../../utils/eventOrderBookData';
import { downloadEventOrderBookPdf } from '../../utils/eventOrderBookPdf';
import { summarizeOrderBook } from '../../utils/orderBook';

const props = defineProps({
  eventId: { type: [String, Number], required: true },
  eventName: { type: String, default: '' },
  eventSeason: { type: String, default: '' },
  iconOnly: { type: Boolean, default: false }
});

const { t } = useI18n();
const loading = ref(false);

function formatNow() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

async function download() {
  if (loading.value) return;
  loading.value = true;
  try {
    const { sections } = await loadEventOrderBookData(props.eventId, fetchResult);
    if (!sections.length) {
      ElMessage.warning(t('events.downloadRosterPdfEmpty'));
      return;
    }
    if (sections.every((s) => s.loadFailed)) throw new Error('all rosters failed');

    const summary = summarizeOrderBook(sections);
    await downloadEventOrderBookPdf({
      title: `${t('orderBook.bookTitle')} · ${t('events.rosterPdfTitle')}`,
      eventName: props.eventName,
      eventSeason: props.eventSeason,
      generatedAtLine: `${t('orderBook.bookGeneratedAt')}${formatNow()}`,
      summaryText: t('orderBook.bookSummary', summary),
      fileName: `${props.eventName}_${t('events.rosterPdfTitle')}`,
      labels: {
        locked: t('orderBook.locked'),
        playerCount: t('orderBook.colPlayerCount'),
        notSubmitted: t('orderBook.bookNotSubmitted'),
        loadFailed: t('orderBook.rosterLoadFailed'),
        noActive: t('orderBook.noPlayers'),
        continued: t('events.rosterPdfContinued')
      },
      sections
    });
    ElMessage.success(t('events.downloadRosterPdfSuccess'));
  } catch {
    ElMessage.error(t('events.downloadRosterPdfFail'));
  } finally {
    loading.value = false;
  }
}
</script>
