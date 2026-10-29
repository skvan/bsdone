<template>
  <div class="gd-fielding-table">
    <h4 class="gd-fielding-table__title">{{ title }}</h4>
    <el-table
      :data="rows"
      size="small"
      border
      :show-summary="rows.length > 0"
      :summary-method="summaryMethod"
      :class="['box-table', 'box-table-fielding', sideClass]"
    >
      <el-table-column prop="playerName" label="球员" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span :class="['gd-fielding-table__player', { 'is-sub': row.subRow }]">{{ nameOf(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="fieldPosition" label="POS" width="70" align="center">
        <template #default="{ row }">{{ row.fieldPosition || '-' }}</template>
      </el-table-column>
      <el-table-column
        v-for="col in visibleFields"
        :key="col.key"
        :prop="col.key"
        :label="col.label"
        :width="columnWidth(col)"
        align="center"
      >
        <template #default="{ row }">{{ cellValue(col, row) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import {
  buildFieldingDisplayRows,
  buildFieldingSummaryCells,
  buildFieldingTotals
} from '../../utils/gameFieldingRows.js';
import { formatInningsPitched } from '../../utils/inningsFormat.js';
import { DEFAULT_FIELDING_FIELDS } from '../../utils/gameDetailFieldSettingsStorage.js';

const props = defineProps({
  stats: { type: Array, default: () => [] },
  teamId: { type: [Number, String], default: null },
  teamName: { type: String, default: '' },
  playersMap: { type: Object, default: () => ({}) },
  fields: { type: Array, default: () => DEFAULT_FIELDING_FIELDS },
  side: { type: String, default: '' }
});

const sideClass = computed(() =>
  props.side === 'away' || props.side === 'home' ? `box-table-${props.side}` : ''
);
const rows = computed(() => buildFieldingDisplayRows(props.stats, props.teamId));
const visibleFields = computed(() => {
  const fields = props.fields?.length ? props.fields : DEFAULT_FIELDING_FIELDS;
  return fields.filter((field) => field.visible !== false);
});

const totals = computed(() => buildFieldingTotals(rows.value, visibleFields.value));
const title = computed(() => `${props.teamName || ''} 守备`.trim());

function columnWidth(col) {
  return col.key === 'defInn' ? 76 : 64;
}

function cellValue(col, row) {
  return col.key === 'defInn' ? formatInningsPitched(row.defInn) : (row[col.key] ?? 0);
}

function summaryMethod({ columns }) {
  return buildFieldingSummaryCells(columns, totals.value);
}

function nameOf(row) {
  const composite = props.playersMap?.[`${props.teamId}:${row.playerId}`];
  const fallback = props.playersMap?.[row.playerId];
  const player = composite || fallback;
  return player?.name || player?.shortName || row.playerName || `#${row.playerId ?? '-'}`;
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
/* 同守位组的替补缩进在首个上场者下一行 */
.gd-fielding-table__player.is-sub {
  padding-left: 18px;
  color: var(--el-text-color-regular);
}
.gd-fielding-table__player.is-sub::before {
  content: '└';
  margin-right: 4px;
  color: var(--el-text-color-placeholder);
}
/* Totals 汇总行：与打击/投手表合计行一致的灰底、加粗、上边框 */
.gd-fielding-table :deep(.el-table__footer-wrapper td.el-table__cell) {
  background: var(--bsball-gray-50, #fafafa) !important;
  border-top: 1px solid var(--el-border-color-lighter, var(--el-border-color)) !important;
  font-weight: 700;
}
.gd-fielding-table :deep(.el-table__footer-wrapper td.el-table__cell .cell) {
  color: var(--el-text-color-primary) !important;
  font-weight: 700;
}
html.dark .gd-fielding-table :deep(.el-table__footer-wrapper td.el-table__cell),
[data-admin-theme='dark'] .gd-fielding-table :deep(.el-table__footer-wrapper td.el-table__cell) {
  background: var(--el-fill-color, var(--el-fill-color-light)) !important;
}

/* 守备表是子组件，父组件 GameDetailContent 的 .box-table 样式带父 scope，无法命中这里的 el-table；
   在此用 :deep 补齐与击球 / 投手表一致的表头与单元格样式（#417） */
.gd-fielding-table :deep(.el-table) {
  --el-table-border-color: var(--el-border-color);
  --el-table-text-color: var(--el-text-color-primary);
  font-size: 13px;
}
.gd-fielding-table :deep(.el-table__cell) {
  text-overflow: clip;
  white-space: nowrap;
  border-right: none;
  line-height: 1.3;
  overflow: visible;
}
.gd-fielding-table :deep(td.el-table__cell) {
  box-sizing: border-box;
  vertical-align: middle;
  min-height: 28px;
  padding: 4px 3px;
}
.gd-fielding-table :deep(th.el-table__cell) {
  vertical-align: middle;
  box-sizing: border-box;
  min-height: 39px;
  padding: 6px 4px;
}
.gd-fielding-table :deep(.el-table__body-wrapper tbody td.el-table__cell:first-child .cell) {
  box-sizing: border-box;
  align-items: center;
  min-height: 19.9px;
  display: flex;
}
.gd-fielding-table :deep(.el-table__header th) {
  border-right: none;
  background: var(--bsball-gray-100, #f0f0f0) !important;
  font-size: 12px;
  white-space: nowrap;
  overflow: visible;
}
.gd-fielding-table :deep(.el-table thead) {
  background: var(--bsball-gray-100, #f0f0f0) !important;
}
.gd-fielding-table :deep(.el-table__header th .cell),
.gd-fielding-table :deep(.el-table__body-wrapper tbody td.el-table__cell .cell) {
  word-break: normal;
  white-space: nowrap !important;
  text-overflow: clip !important;
  line-height: 1.3 !important;
  overflow: visible !important;
}
.gd-fielding-table :deep(.el-table__body .el-table__cell .cell) {
  color: var(--el-text-color-primary);
}
html:not(.dark) .gd-fielding-table :deep(.box-table-away .el-table__header th),
html:not(.dark) .gd-fielding-table :deep(.box-table-away .el-table__fixed-header-wrapper th) {
  color: #000 !important;
  background: #ff9595 !important;
}
html:not(.dark) .gd-fielding-table :deep(.box-table-away .el-table__header th .cell),
html:not(.dark) .gd-fielding-table :deep(.box-table-away .el-table__fixed-header-wrapper th .cell) {
  color: #000 !important;
}
html:not(.dark) .gd-fielding-table :deep(.box-table-away .el-table thead) {
  background: #ff9595 !important;
}
html:not(.dark) .gd-fielding-table :deep(.box-table-home .el-table__header th),
html:not(.dark) .gd-fielding-table :deep(.box-table-home .el-table__fixed-header-wrapper th) {
  color: #000 !important;
  background: #ffc000 !important;
}
html:not(.dark) .gd-fielding-table :deep(.box-table-home .el-table__header th .cell),
html:not(.dark) .gd-fielding-table :deep(.box-table-home .el-table__fixed-header-wrapper th .cell) {
  color: #000 !important;
}
html:not(.dark) .gd-fielding-table :deep(.box-table-home .el-table thead) {
  background: #ffc000 !important;
}
html.dark .gd-fielding-table :deep(.el-table),
[data-admin-theme='dark'] .gd-fielding-table :deep(.el-table) {
  --el-table-text-color: var(--el-text-color-regular);
  --el-table-header-text-color: var(--el-text-color-regular);
}
html.dark .gd-fielding-table :deep(.el-table__header th),
[data-admin-theme='dark'] .gd-fielding-table :deep(.el-table__header th) {
  color: var(--el-text-color-regular) !important;
  font-weight: 600;
}
html.dark .gd-fielding-table :deep(.el-table__body .el-table__cell .cell),
[data-admin-theme='dark'] .gd-fielding-table :deep(.el-table__body .el-table__cell .cell) {
  color: var(--el-text-color-regular) !important;
}
@media (max-width: 768px) {
  .gd-fielding-table :deep(.el-table) {
    font-size: 11px;
  }
  .gd-fielding-table :deep(td.el-table__cell) {
    min-height: 26px;
    padding: 3px;
  }
  .gd-fielding-table :deep(th.el-table__cell) {
    min-height: 36px;
    padding: 5px 3px;
  }
  .gd-fielding-table :deep(.el-table__header th) {
    font-size: 11px;
  }
}
</style>
