<template>
  <div class="game-fielding-table">
    <h4 class="game-fielding-table__title">{{ title }}</h4>
    <el-table
      :data="rows"
      size="small"
      max-height="520"
      class="compact-table"
      empty-text="暂无球员"
      :show-summary="rows.length > 0"
      :summary-method="summaryMethod"
      :row-key="rowKey"
      highlight-current-row
      :current-row-key="currentRowKey"
      @row-click="onRowClick"
    >
      <el-table-column prop="playerName" label="球员" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.name }}</template>
      </el-table-column>
      <el-table-column label="背号" width="70" align="center">
        <template #default="{ row }">{{ row.number ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="POS" width="90" align="center">
        <template #default="{ row }">{{ row._fieldPosition || row.position || '-' }}</template>
      </el-table-column>
      <el-table-column
        v-for="col in columns"
        :key="col.key"
        :prop="col.key"
        :label="col.label"
        :width="col.width"
        align="center"
      >
        <template #default="{ row }">
          <el-input-number
            :model-value="Number(statsFor(row)?.[col.key]) || 0"
            :min="0"
            :max="col.max"
            :step="col.step"
            :precision="col.precision"
            :controls="false"
            size="small"
            class="stat-input"
            @update:model-value="setValue(row, col.key, $event)"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import {
  buildFieldingSummaryCells,
  buildFieldingTotals
} from '../../utils/gameFieldingRows.js';

const props = defineProps({
  rows: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  statsFor: { type: Function, required: true }
});

const columns = [
  { key: 'fieldingGs', label: 'GS', width: 70, max: 1, step: 1, precision: 0 },
  { key: 'po', label: 'PO', width: 70, max: 99, step: 1, precision: 0 },
  { key: 'a', label: 'A', width: 70, max: 99, step: 1, precision: 0 },
  { key: 'e', label: 'E', width: 70, max: 99, step: 1, precision: 0 },
  { key: 'dp', label: 'DP', width: 70, max: 99, step: 1, precision: 0 },
  { key: 'defInn', label: 'INN', width: 80, max: 30, step: 0.1, precision: 1 },
  { key: 'pb', label: 'PB', width: 70, max: 99, step: 1, precision: 0 },
  { key: 'catcherSb', label: 'SB', width: 70, max: 99, step: 1, precision: 0 },
  { key: 'catcherCs', label: 'CS', width: 70, max: 99, step: 1, precision: 0 }
];

const currentRowKey = ref('');

const totals = computed(() => {
  const rows = props.rows.map((row) => props.statsFor(row) ?? {});
  return buildFieldingTotals(rows, columns);
});

function rowKey(row) {
  if (row?._rowId != null) return `row_${row._rowId}`;
  if (row?.supplementId) return `p_${row.id}_${row.supplementId}`;
  return `p_${row?.id ?? ''}`;
}

function onRowClick(row) {
  currentRowKey.value = rowKey(row);
}

function summaryMethod({ columns: tableColumns }) {
  return buildFieldingSummaryCells(tableColumns, totals.value);
}

function setValue(row, key, value) {
  const stats = props.statsFor(row);
  if (stats) stats[key] = Number(value) || 0;
}
</script>

<style scoped>
.game-fielding-table {
  margin-bottom: 20px;
}
.game-fielding-table__title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}
</style>
