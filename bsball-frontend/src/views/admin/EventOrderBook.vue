<template>
  <div class="order-book-page event-order-book-page">
    <el-card shadow="never" class="event-order-book-card">
      <div class="page-head no-print">
        <el-button link type="primary" class="back-btn" @click="goBack">{{ t('orderBook.backEventTeams') }}</el-button>
        <span class="page-title">{{ t('orderBook.bookTitle') }}</span>
        <span v-if="eventName" class="page-sub">· {{ eventName }}</span>
        <div class="head-actions">
          <el-button type="primary" @click="printBook">{{ t('orderBook.bookPrint') }}</el-button>
        </div>
      </div>

      <div v-loading="loading" class="book-body">
        <div class="book-header">
          <div class="book-title">
            {{ eventName || '—' }}<span v-if="eventSeason" class="book-season">· {{ eventSeason }}</span>
          </div>
          <div class="book-meta">
            <span>{{ t('orderBook.bookGeneratedAt') }}{{ generatedAt }}</span>
            <span>{{ t('orderBook.bookSummary', summary) }}</span>
          </div>
        </div>

        <section v-for="s in sections" :key="s.teamId" class="book-team">
          <div class="book-team-head">
            <el-avatar :size="26" shape="square" :src="resolveAssetUrl(s.teamLogo) || undefined">
              {{ (s.teamName || '?').slice(0, 1) }}
            </el-avatar>
            <span class="book-team-name">{{ s.teamName || `ID:${s.teamId}` }}</span>
            <el-tag v-if="s.rosterLocked" type="warning" effect="light">{{ t('orderBook.locked') }}</el-tag>
            <span class="book-team-count">{{ t('orderBook.colPlayerCount') }} · {{ s.playerCount }}</span>
          </div>
          <EventRosterTable
            v-if="s.submitted && !s.loadFailed"
            :rows="s.rows"
            :max-height="null"
            show-current-in-team
            :empty-text="t('orderBook.emptyRoster')"
          />
          <div v-else-if="s.loadFailed" class="book-placeholder">{{ t('orderBook.rosterLoadFailed') }}</div>
          <div v-else class="book-placeholder">{{ t('orderBook.bookNotSubmitted') }}</div>
        </section>

        <div v-if="!loading && !sections.length" class="book-empty">{{ t('orderBook.bookEmpty') }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { eventApi, eventRegistrationApi } from '../../api/business';
import { fetchResult, resolveAssetUrl } from '../../api/request';
import { buildOrderBookSections, summarizeOrderBook } from '../../utils/orderBook';
import EventRosterTable from '../../components/admin/EventRosterTable.vue';
import '../../styles/admin-page-head.css';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const eventId = computed(() => String(route.params.eventId ?? ''));
const eventName = ref('');
const eventSeason = ref('');
const generatedAt = ref('');
const loading = ref(false);
const sections = ref([]);
const summary = computed(() => summarizeOrderBook(sections.value));

function goBack() {
  router.push({ name: 'AdminEventTeams', params: { eventId: eventId.value } });
}

function printBook() {
  window.print();
}

function formatNow() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

async function load() {
  loading.value = true;
  try {
    const [event, teams] = await Promise.all([
      eventApi.get(eventId.value),
      eventRegistrationApi.listEventTeams(eventId.value)
    ]);
    eventName.value = event?.name ?? '';
    eventSeason.value = event?.season ?? '';
    generatedAt.value = formatNow();
    // 仅已提交名单的已确认球队才取名单；单队失败只占位、不弹提示、不阻断整册
    // （走 fetchResult：getData 会吞错返回 null 且弹全局提示，无法区分「加载失败」与「空名单」）
    const toFetch = (teams ?? []).filter((x) => x.status === 'confirmed' && x.rosterSubmittedAt != null);
    const rosterMap = {};
    await Promise.all(toFetch.map(async (x) => {
      const { data, error } = await fetchResult(`/api/event/${eventId.value}/teams/${x.teamId}/roster`);
      rosterMap[x.teamId] = error ? { failed: true } : { rows: data ?? [] };
    }));
    sections.value = buildOrderBookSections(teams, rosterMap);
  } catch (err) {
    ElMessage.error(err?.message ?? t('orderBook.bookLoadFailed'));
    sections.value = [];
  } finally {
    loading.value = false;
  }
}

load();
</script>

<!-- 打印（整册）：非 scoped 块——仅在整册页挂载时全局隐藏布局外壳（body:has 守卫，避免影响其它页面打印），仅输出册体，并保留颜色 -->
<style>
@media print {
  body:has(.event-order-book-page) * {
    visibility: hidden;
  }

  /* 显示规则须带 :has 前缀抬升特异性（0,2,1），否则会被 (0,1,1) 的隐藏规则压过 */
  body:has(.event-order-book-page) .event-order-book-page,
  body:has(.event-order-book-page) .event-order-book-page * {
    visibility: visible;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .event-order-book-page {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }

  /* 灰阶/色弱显示模式下 .admin-layout 的 filter 会成为绝对定位包含块并裁剪分页，打印时还原 */
  body:has(.event-order-book-page) .admin-layout {
    filter: none !important;
  }

  .event-order-book-page .no-print {
    display: none !important;
  }
}
</style>

<style scoped>
.event-order-book-page {
  padding: 12px;
}

.head-actions {
  margin-left: auto;
}

.book-header {
  margin-bottom: 12px;
}

.book-title {
  font-size: 18px;
  font-weight: 600;
}

.book-season {
  margin-left: 6px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.book-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.book-team {
  margin-top: 16px;
  break-inside: avoid;
  page-break-inside: avoid;
}

.book-team-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.book-team-name {
  font-weight: 600;
}

.book-team-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.book-placeholder {
  padding: 12px;
  color: var(--el-text-color-secondary);
  border: 1px dashed var(--el-border-color);
  border-radius: 4px;
}

.book-empty {
  padding: 24px;
  text-align: center;
  color: var(--el-text-color-secondary);
}
</style>
