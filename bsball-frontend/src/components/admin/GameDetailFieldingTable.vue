<template>
  <div class="gd-fielding-table">
    <h4 class="gd-fielding-table__title">{{ title }}</h4>
    <el-table
      :data="rows"
      row-key="_rowKey"
      size="small"
      border
      :show-summary="rows.length > 0"
      :summary-method="summaryMethod"
      :row-class-name="rowClassName"
      :class="['box-table', 'box-table-fielding', sideClass]"
      @row-click="toggle"
    >
      <el-table-column prop="playerName" label="球员" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span
            class="gd-fielding-table__player"
            :class="{ 'is-sub': row.subRow, 'is-bd': row.kind === 'breakdown' }"
          >
            <template v-if="row.kind === 'breakdown'">
              <span class="gd-fielding-table__bd-mark">└</span>
              <span class="gd-fielding-table__bd-pos">{{ row.position }}</span>
            </template>
            <template v-else>
              <button
                v-if="row.multiPosition"
                type="button"
                class="gd-fielding-table__expander"
                :class="{ 'is-open': row.open }"
                :aria-expanded="row.open ? 'true' : 'false'"
                :aria-label="`${row.open ? '收起' : '展开'} ${nameOf(row)} 的逐位置守备数据`"
                @click.stop="toggle(row)"
              >
                <span class="gd-fielding-table__expander-h"></span>
                <span class="gd-fielding-table__expander-v"></span>
              </button>
              <span v-else class="gd-fielding-table__expander-spacer"></span>
              <span v-if="row.subRow" class="gd-fielding-table__sub-mark">└</span>
              <span class="gd-fielding-table__name" :class="{ 'is-open': row.open }">{{ nameOf(row) }}</span>
            </template>
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="fieldPosition" label="POS" width="70" align="center">
        <template #default="{ row }">{{ posLabel(row) }}</template>
      </el-table-column>
      <el-table-column
        v-for="col in visibleFields"
        :key="col.key"
        :prop="col.key"
        :label="col.label"
        :width="columnWidth(col)"
        align="center"
      >
        <template #default="{ row }">
          <span :class="{ 'gd-fielding-table__stat--dim': isDimStat(col, row) }">{{ cellValue(col, row) }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import {
  buildFieldingDisplayRows,
  buildFieldingSummaryCells,
  buildFieldingTotals,
  expandFieldingRows
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
const expandedPlayerIds = ref(new Set());
const baseRows = computed(() => buildFieldingDisplayRows(props.stats, props.teamId));
const rows = computed(() => expandFieldingRows(baseRows.value, expandedPlayerIds.value));
const visibleFields = computed(() => {
  const fields = props.fields?.length ? props.fields : DEFAULT_FIELDING_FIELDS;
  return fields.filter((field) => field.visible !== false);
});

const totals = computed(() => buildFieldingTotals(baseRows.value, visibleFields.value));
const title = computed(() => `${props.teamName || ''} 守备`.trim());

function toggle(row) {
  if (!row?.multiPosition) return;
  const id = String(row.playerId);
  const next = new Set(expandedPlayerIds.value);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  expandedPlayerIds.value = next;
}

function rowClassName({ row }) {
  if (row?.kind === 'breakdown') return 'gd-fielding-table__row--bd';
  return row?.multiPosition ? 'gd-fielding-table__row--multi' : '';
}

function posLabel(row) {
  if (row?.kind === 'breakdown') return row.position;
  if (row?.multiPosition) return '';
  return row?.fieldPosition || '-';
}

function isDimStat(col, row) {
  return row?.kind === 'breakdown'
    && ['pb', 'catcherSb', 'catcherCs'].includes(col.key)
    && row.position !== 'C';
}

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
  --gd-expander-bar: #737c8b;
  --gd-expander-hover: rgba(7, 26, 51, 0.06);
  --gd-bd-accent: #8a6d1f;
  --gd-bd-accent-soft: rgba(201, 164, 91, 0.12);
  --gd-bd-accent-border: rgba(201, 164, 91, 0.28);
  --gd-bd-line: rgba(201, 164, 91, 0.55);
  --gd-bd-row-bg: #fbf7ea;
  --gd-bd-row-hover: #f8f2e2;
  --gd-bd-row-border: #f1ead9;
  --gd-multi-row-bg: #fffdf7;
  --gd-multi-row-hover: #faf7ee;
}
html.dark .gd-fielding-table,
[data-admin-theme='dark'] .gd-fielding-table {
  --gd-expander-bar: #8b95a5;
  --gd-expander-hover: rgba(255, 255, 255, 0.08);
  --gd-bd-accent: #d9b45a;
  --gd-bd-accent-soft: rgba(255, 192, 0, 0.08);
  --gd-bd-accent-border: rgba(217, 180, 90, 0.24);
  --gd-bd-line: rgba(217, 180, 90, 0.5);
  --gd-bd-row-bg: #141a24;
  --gd-bd-row-hover: #161e2a;
  --gd-bd-row-border: #222b39;
  --gd-multi-row-bg: #121a26;
  --gd-multi-row-hover: #16202e;
}
.gd-fielding-table__title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}
.gd-fielding-table__player {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 19.9px;
  min-width: 0;
}
/* 同守位组的替补缩进在首个上场者下一行 */
.gd-fielding-table__player.is-sub {
  padding-left: 18px;
  color: var(--el-text-color-regular);
}
.gd-fielding-table__player.is-bd {
  padding-left: 28px;
  gap: 4px;
}
.gd-fielding-table__sub-mark {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
.gd-fielding-table__name {
  color: var(--el-text-color-primary);
}
.gd-fielding-table__name.is-open {
  font-weight: 600;
}
.gd-fielding-table__expander-spacer {
  width: 16px;
  height: 16px;
  flex: none;
}
.gd-fielding-table__expander {
  position: relative;
  box-sizing: border-box;
  width: 16px;
  height: 16px;
  flex: none;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease;
}
.gd-fielding-table__expander-h,
.gd-fielding-table__expander-v {
  position: absolute;
  background: var(--gd-expander-bar);
  border-radius: 1px;
  transition: background 0.15s ease, opacity 0.15s ease;
}
.gd-fielding-table__expander-h {
  width: 8px;
  height: 1.5px;
}
.gd-fielding-table__expander-v {
  width: 1.5px;
  height: 8px;
}
.gd-fielding-table__expander:hover {
  background: var(--gd-expander-hover);
}
.gd-fielding-table__expander:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 1px;
}
.gd-fielding-table__expander.is-open .gd-fielding-table__expander-h,
.gd-fielding-table__expander.is-open .gd-fielding-table__expander-v {
  background: var(--gd-bd-accent);
}
.gd-fielding-table__expander.is-open .gd-fielding-table__expander-v {
  opacity: 0;
}
.gd-fielding-table__bd-mark {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
.gd-fielding-table__bd-pos {
  display: inline-flex;
  align-items: center;
  height: 16px;
  padding: 0 4px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--gd-bd-accent);
  background: var(--gd-bd-accent-soft);
  border: 1px solid var(--gd-bd-accent-border);
}
.gd-fielding-table__stat--dim {
  color: var(--el-text-color-placeholder);
}
/* 多守位主行（合计行）与展开明细行的差异样式 */
.gd-fielding-table :deep(.el-table__body tr.gd-fielding-table__row--multi > td.el-table__cell) {
  background: var(--gd-multi-row-bg);
}
.gd-fielding-table :deep(.el-table__body tr.gd-fielding-table__row--multi:hover > td.el-table__cell) {
  background: var(--gd-multi-row-hover);
}
.gd-fielding-table :deep(.el-table__body tr.gd-fielding-table__row--bd > td.el-table__cell) {
  background: var(--gd-bd-row-bg) !important;
  border-bottom-color: var(--gd-bd-row-border);
  font-size: 12px;
}
.gd-fielding-table :deep(.el-table__body tr.gd-fielding-table__row--bd:hover > td.el-table__cell) {
  background: var(--gd-bd-row-hover) !important;
}
.gd-fielding-table :deep(.el-table__body tr.gd-fielding-table__row--bd > td.el-table__cell:first-child) {
  box-shadow: inset 3px 0 0 var(--gd-bd-line);
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
  background: var(--el-fill-color-light, #262727) !important;
  font-weight: 600;
}
html.dark .gd-fielding-table :deep(.el-table thead),
[data-admin-theme='dark'] .gd-fielding-table :deep(.el-table thead) {
  background: var(--el-fill-color-light, #262727) !important;
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
  .gd-fielding-table__player {
    gap: 5px;
  }
  .gd-fielding-table__player.is-sub {
    padding-left: 14px;
  }
  .gd-fielding-table__player.is-bd {
    padding-left: 20px;
  }
  .gd-fielding-table__expander-spacer,
  .gd-fielding-table__expander {
    width: 14px;
    height: 14px;
  }
  .gd-fielding-table__expander-h {
    width: 7px;
  }
  .gd-fielding-table__expander-v {
    height: 7px;
  }
  .gd-fielding-table__bd-pos {
    height: 15px;
    padding: 0 3px;
    font-size: 10px;
  }
  .gd-fielding-table :deep(.el-table__body tr.gd-fielding-table__row--bd > td.el-table__cell) {
    font-size: 11px;
  }
}
</style>
