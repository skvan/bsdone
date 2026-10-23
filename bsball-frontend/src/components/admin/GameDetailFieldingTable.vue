<template>
  <div class="gd-fielding-table">
    <h4 class="gd-fielding-table__title">{{ title }}</h4>
    <el-table :data="rows" size="small" border class="box-table box-table-fielding">
      <el-table-column label="球员" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ nameOf(row) }}</template>
      </el-table-column>
      <el-table-column label="POS" width="70" align="center">
        <template #default="{ row }">{{ row.position || '-' }}</template>
      </el-table-column>
      <el-table-column prop="po" label="PO" width="64" align="center" />
      <el-table-column prop="a" label="A" width="64" align="center" />
      <el-table-column prop="e" label="E" width="64" align="center" />
      <el-table-column prop="dp" label="DP" width="64" align="center" />
      <el-table-column label="INN" width="76" align="center">
        <template #default="{ row }">{{ innings(row.defInn) }}</template>
      </el-table-column>
      <el-table-column prop="pb" label="PB" width="64" align="center" />
      <el-table-column prop="catcherSb" label="SB" width="64" align="center" />
      <el-table-column prop="catcherCs" label="CS" width="64" align="center" />
    </el-table>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { buildFieldingDisplayRows } from '../../utils/gameFieldingRows.js';

const props = defineProps({
  stats: { type: Array, default: () => [] },
  teamId: { type: [Number, String], default: null },
  teamName: { type: String, default: '' },
  playersMap: { type: Object, default: () => ({}) }
});

const rows = computed(() => buildFieldingDisplayRows(props.stats, props.teamId));

const title = computed(() => `${props.teamName || ''} 守备`.trim());

function nameOf(row) {
  const composite = props.playersMap?.[`${props.teamId}:${row.playerId}`];
  const fallback = props.playersMap?.[row.playerId];
  const player = composite || fallback;
  return player?.name || player?.shortName || row.playerName || `#${row.playerId ?? '-'}`;
}

function innings(value) {
  const n = Number(value) || 0;
  if (n <= 0) return '0';
  const whole = Math.floor(n + 1e-9);
  const frac = Math.round((n - whole) * 10);
  return frac > 0 ? `${whole}.${frac}` : String(whole);
}
</script>

<style scoped>
.gd-fielding-table {
  margin: 16px 0;
}
.gd-fielding-table__title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}
</style>
