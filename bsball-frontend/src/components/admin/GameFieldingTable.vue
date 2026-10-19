<template>
  <div class="game-fielding-table">
    <h4 class="game-fielding-table__title">{{ title }}</h4>
    <el-table :data="rows" size="small" max-height="520" class="compact-table" empty-text="暂无球员">
      <el-table-column label="球员" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.name }}</template>
      </el-table-column>
      <el-table-column label="背号" width="70" align="center">
        <template #default="{ row }">{{ row.number ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="POS" width="90" align="center">
        <template #default="{ row }">{{ row.position || '-' }}</template>
      </el-table-column>
      <el-table-column
        v-for="col in columns"
        :key="col.key"
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
  { key: 'defInn', label: 'INN', width: 80, max: 30, step: 0.1, precision: 1 }
];

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
