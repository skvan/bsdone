<template>
  <div class="portal-fp">
    <div class="portal-fp__head">
      <h3 class="portal-fp__title">{{ t('stats.fieldingPositionsTitle') }}</h3>
      <span class="portal-fp__hint">{{ t('stats.fieldingPositionsHint', { n: minOuts }) }}</span>
    </div>
    <el-table
      v-loading="loading"
      :data="rows"
      stripe
      class="leaders-table stats-data-table portal-fp__table"
      :empty-text="t('stats.noData')"
      size="small"
    >
      <el-table-column type="index" :index="indexMethod" label="#" width="56" align="center" />
      <el-table-column prop="playerName" :label="t('stats.positionBoardPlayer')" min-width="140" show-overflow-tooltip />
      <el-table-column prop="teamName" :label="t('stats.positionBoardTeam')" min-width="120" show-overflow-tooltip />
      <el-table-column :label="t('stats.positionBoardPosition')" width="90" align="center">
        <template #default="{ row }">{{ formatPositionDisplay(row) || '-' }}</template>
      </el-table-column>
      <el-table-column prop="gp" :label="t('stats.positionBoardGp')" width="72" align="center" />
      <el-table-column prop="gs" :label="t('stats.positionBoardGs')" width="72" align="center" />
      <el-table-column prop="inn" :label="t('stats.positionBoardInn')" width="90" align="center" />
    </el-table>
    <div class="portal-fp__pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        small
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { statsApi } from '../../api/business';
import { formatPositionDisplay } from '../../utils/positionFormat';

const props = defineProps({
  years: { type: Array, default: () => [] },
  eventIds: { type: Array, default: () => [] },
  teamIds: { type: Array, default: () => [] },
  position: { type: String, default: '' },
  homeAway: { type: String, default: '' },
  gameMode: { type: String, default: '' },
  playerName: { type: String, default: '' },
  scope: { type: String, default: 'player' },
});

const { t } = useI18n();
const minOuts = 9;
const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    const res = await statsApi.getFieldingPositions({
      years: props.years,
      eventIds: props.eventIds,
      teamIds: props.teamIds,
      position: props.position,
      homeAway: props.homeAway,
      gameMode: props.gameMode,
      playerName: props.playerName,
      minOuts,
      page: page.value,
      pageSize: pageSize.value,
    });
    rows.value = Array.isArray(res?.list) ? res.list : [];
    total.value = Number(res?.total) || 0;
  } catch {
    rows.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function onSizeChange() {
  page.value = 1;
  load();
}

function indexMethod(index) {
  return (page.value - 1) * pageSize.value + index + 1;
}

watch(
  () => [props.years, props.eventIds, props.teamIds, props.position, props.homeAway, props.gameMode, props.playerName, props.scope],
  () => {
    page.value = 1;
    load();
  },
  { deep: true }
);

onMounted(load);
</script>

<style scoped>
.portal-fp {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.portal-fp__head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.portal-fp__title {
  margin: 0;
  font-size: 15px;
}
.portal-fp__hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.portal-fp__table {
  width: 100%;
}
.portal-fp__pager {
  display: flex;
  justify-content: flex-end;
}
</style>
