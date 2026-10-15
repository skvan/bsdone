<template>
  <div class="order-book-page event-order-book-page">
    <el-card shadow="never" class="event-order-book-card">
      <div class="page-head no-print">
        <el-button link type="primary" class="back-btn" @click="goBack">{{ t('orderBook.backEventTeams') }}</el-button>
        <span class="page-title">{{ t('orderBook.bookTitle') }}</span>
        <span v-if="eventName" class="page-sub">· {{ eventName }}</span>
        <div class="head-actions">
          <el-radio-group v-model="viewMode">
            <el-radio-button value="cards">
              <el-icon><Grid /></el-icon>
              <span class="view-toggle-text">{{ t('orderBook.viewCards') }}</span>
            </el-radio-button>
            <el-radio-button value="list">
              <el-icon><List /></el-icon>
              <span class="view-toggle-text">{{ t('orderBook.viewList') }}</span>
            </el-radio-button>
          </el-radio-group>
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

        <template v-if="viewMode === 'cards'">
          <section v-for="s in sections" :key="s.teamId" class="book-team">
            <div class="book-team-head">
              <el-avatar :size="26" shape="square" :src="resolveAssetUrl(s.teamLogo) || undefined">
                {{ (s.teamName || '?').slice(0, 1) }}
              </el-avatar>
              <span class="book-team-name">{{ s.teamName || `ID:${s.teamId}` }}</span>
              <el-tag v-if="s.rosterLocked" type="warning" effect="light">{{ t('orderBook.locked') }}</el-tag>
              <span v-if="!s.loadFailed" class="book-team-count">{{ t('orderBook.colPlayerCount') }} · {{ s.playerCount }}</span>
            </div>
            <div v-if="s.submitted && !s.loadFailed && s.rows.length" class="book-cards">
              <div v-for="p in s.rows" :key="p.playerId" class="ob-player-card">
                <el-avatar :size="96" shape="square" :src="resolveAssetUrl(p.avatar) || undefined" class="ob-player-avatar">
                  {{ (p.name || '球').slice(0, 1) }}
                </el-avatar>
                <div class="ob-player-position">{{ (p.positions || []).join(' / ') || ' ' }}</div>
                <div class="ob-player-name" :title="p.name">{{ p.name }}</div>
                <div class="ob-player-number">{{ p.number || '—' }}</div>
              </div>
            </div>
            <div v-else-if="s.loadFailed" class="book-placeholder">{{ t('orderBook.rosterLoadFailed') }}</div>
            <div v-else-if="s.submitted" class="book-placeholder">{{ t('orderBook.noPlayers') }}</div>
            <div v-else class="book-placeholder">{{ t('orderBook.bookNotSubmitted') }}</div>
          </section>
          <div v-if="!loading && !sections.length" class="book-empty">{{ t('orderBook.bookEmpty') }}</div>
        </template>

        <template v-else>
          <div v-if="flatExclusionsText" class="book-placeholder ob-list-notice">{{ t('orderBook.listExcluded') }}{{ flatExclusionsText }}</div>
          <el-table :data="flatPlayers">
            <el-table-column :label="t('orderBook.colPhoto')" width="72" align="center">
              <template #default="{ row }">
                <el-avatar :size="32" shape="square" :src="resolveAssetUrl(row.avatar) || undefined">
                  {{ (row.name || '?').slice(0, 1) }}
                </el-avatar>
              </template>
            </el-table-column>
            <el-table-column prop="name" :label="t('orderBook.colPlayer')" min-width="150" />
            <el-table-column :label="t('orderBook.colNumber')" width="110">
              <template #default="{ row }">{{ row.numberText || '—' }}</template>
            </el-table-column>
            <el-table-column :label="t('orderBook.colPositions')" min-width="130">
              <template #default="{ row }">{{ row.positionText || '—' }}</template>
            </el-table-column>
            <el-table-column :label="t('orderBook.colRegTeams')" min-width="280">
              <template #default="{ row }">
                <span class="ob-reg-teams">{{ row.teamText }}</span>
              </template>
            </el-table-column>
            <template #empty>{{ sections.length ? t('orderBook.listNoActive') : t('orderBook.bookEmpty') }}</template>
          </el-table>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Grid, List } from '@element-plus/icons-vue';
import { eventApi, eventRegistrationApi } from '../../api/business';
import { fetchResult, resolveAssetUrl } from '../../api/request';
import { buildOrderBookSections, summarizeOrderBook, buildFlatOrderBookPlayers } from '../../utils/orderBook';
import '../../styles/admin-page-head.css';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const eventId = computed(() => String(route.params.eventId ?? ''));
const eventName = ref('');
const eventSeason = ref('');
const generatedAt = ref('');
const loading = ref(false);
const viewMode = ref('cards');
const sections = ref([]);
const summary = computed(() => summarizeOrderBook(sections.value));
const flatPlayers = computed(() => buildFlatOrderBookPlayers(sections.value));
// 列表视图为扁平行，未提交/加载失败的球队无对应行——列在表头提示，避免静默缺失
const flatExclusionsText = computed(() =>
  sections.value
    .filter((s) => s.loadFailed || !s.submitted)
    .map((s) => `${s.teamName || `ID:${s.teamId}`}（${s.loadFailed ? t('orderBook.rosterLoadFailed') : t('orderBook.bookNotSubmitted')}）`)
    .join('、')
);

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
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.view-toggle-text {
  margin-left: 4px;
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

/* 卡片视图：对齐门户阵容页信息结构（照片/守备位置/姓名/背号），浅色主题 */
.book-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(116px, 1fr));
  gap: 12px;
}

.ob-player-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 8px 8px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  break-inside: avoid;
  page-break-inside: avoid;
}

.ob-player-avatar {
  border-radius: 4px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 28px;
}

.ob-player-position {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ob-player-name {
  margin-top: 2px;
  font-weight: 600;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ob-player-number {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.book-empty {
  padding: 24px;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.book-placeholder {
  padding: 12px;
  color: var(--el-text-color-secondary);
  border: 1px dashed var(--el-border-color);
  border-radius: 4px;
}

/* 列表视图：报名球队列完整换行展示（同一球员多队时列出全部队名） */
.ob-reg-teams {
  white-space: normal;
  word-break: break-all;
  line-height: 1.5;
}

.ob-list-notice {
  margin-bottom: 12px;
}
</style>
