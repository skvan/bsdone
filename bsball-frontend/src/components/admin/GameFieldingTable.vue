<template>
  <div class="game-fielding-table">
    <h4 class="game-fielding-table__title">{{ title }}</h4>
    <el-table
      :data="displayRows"
      size="small"
      border
      class="compact-table"
      empty-text="暂无球员"
      :show-summary="rows.length > 0"
      :summary-method="summaryMethod"
      :row-key="rowKey"
      :row-class-name="rowClassName"
      highlight-current-row
      :current-row-key="currentRowKey"
      @row-click="onRowClick"
    >
      <el-table-column prop="playerName" label="球员" min-width="150" resizable show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row._positionRow" class="game-fielding-table__sub-name">
            ↳ {{ row.position }}
          </span>
          <span v-else class="game-fielding-table__player">
            <button
              v-if="hasPlayerPositions(row)"
              type="button"
              class="game-fielding-table__expander"
              :class="{ 'is-open': expandedKeys.has(rowKey(row)) }"
              :aria-expanded="expandedKeys.has(rowKey(row)) ? 'true' : 'false'"
              @click.stop="toggle(row)"
            >{{ expandedKeys.has(rowKey(row)) ? '▾' : '▸' }}</button>
            <span>{{ row.name }}</span>
            <span v-if="hasPlayerPositions(row)" class="game-fielding-table__count">
              {{ t('gameResultFielding.positionsCount', { n: positionRowsFor(row).length }) }}
            </span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="背号" width="70" align="center" resizable>
        <template #default="{ row }">{{ row._positionRow ? '—' : (row.number ?? '-') }}</template>
      </el-table-column>
      <el-table-column label="POS" width="90" align="center" resizable>
        <template #default="{ row }">
          <el-select
            v-if="row._positionRow"
            :model-value="row.position"
            size="small"
            :disabled="!editable"
            @update:model-value="(value) => changePosition(row, value)"
          >
            <el-option
              v-for="option in positionOptionsWithCurrent(row, positionOptionsFor(row._parentRow))"
              :key="option"
              :label="option"
              :value="option"
            />
          </el-select>
          <template v-else>{{ row._fieldPosition || row.position || '-' }}</template>
        </template>
      </el-table-column>
      <el-table-column
        v-for="col in columns"
        :key="col.key"
        :prop="col.key"
        :label="col.label"
        :width="col.width"
        align="center"
        resizable
      >
        <template #default="{ row }">
          <template v-if="col.key === 'fieldingGs'">
            <template v-if="row._positionRow">—</template>
            <el-input-number
              v-else
              :model-value="Number(statsFor(row)?.fieldingGs) || 0"
              :min="0"
              :max="1"
              :controls="false"
              size="small"
              class="stat-input"
              @update:model-value="(value) => setValue(row, 'fieldingGs', value)"
            />
          </template>
          <el-input-number
            v-else-if="row._positionRow"
            :model-value="positionValue(row, col.key)"
            :min="0"
            :max="col.max"
            :step="col.step"
            :precision="col.precision"
            :controls="false"
            size="small"
            class="stat-input"
            :disabled="!editable || !positionFieldEnabled(row, col.key)"
            @update:model-value="(value) => setPositionValue(row, col.key, value)"
          />
          <el-input-number
            v-else-if="!hasPlayerPositions(row)"
            :model-value="Number(statsFor(row)?.[col.key]) || 0"
            :min="0"
            :max="col.max"
            :step="col.step"
            :precision="col.precision"
            :controls="false"
            size="small"
            class="stat-input"
            @update:model-value="(value) => setValue(row, col.key, value)"
          />
          <span v-else>{{ Number(statsFor(row)?.[col.key]) || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <template v-if="row._positionRow">
            <el-button v-if="editable" link type="danger" size="small" @click="emit('remove-position', row._parentRow, row.position)">
              {{ t('gameResultFielding.removePosition') }}
            </el-button>
          </template>
          <template v-else-if="editable">
            <el-dropdown
              v-if="positionOptionsFor(row).length"
              trigger="click"
              @command="(position) => emit('add-position', row, position)"
            >
              <el-button link type="primary" size="small">
                {{ t('gameResultFielding.addPosition') }}
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="option in positionOptionsFor(row)"
                    :key="option"
                    :command="option"
                  >
                    {{ option }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-else link type="info" size="small" disabled>守位已满</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <div class="game-fielding-table__hint">
      <template v-if="editable">{{ t('gameResultFielding.autoTotalHint') }}</template>
      <template v-else>{{ t('gameResultFielding.finalOnly') }}</template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  buildFieldingSummaryCells,
  buildFieldingTotals
} from '../../utils/gameFieldingRows.js';
import { inningsToOuts, outsToInnings } from '../../utils/inningsFormat.js';

const { t } = useI18n();

const props = defineProps({
  rows: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  statsFor: { type: Function, required: true },
  // 返回球员的逐位置明细（PositionRow[]）；旧调用不传时视为无逐位置数据
  positionRowsFor: { type: Function, default: () => [] },
  // 返回该球员尚可选择的守位；旧调用不传时下拉为空
  positionOptionsFor: { type: Function, default: () => [] },
  // 仅 final 为 true：控制逐位置子行与增删按钮
  editable: { type: Boolean, default: false }
});

const emit = defineEmits(['add-position', 'remove-position', 'positions-changed']);

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
const expandedKeys = ref(new Set());

// 球员行后按展开状态插入逐位置子行；子行仅为展示副本，修改回写 _source
const displayRows = computed(() => {
  const list = [];
  for (const row of props.rows ?? []) {
    list.push(row);
    if (!expandedKeys.value.has(rowKey(row))) continue;
    for (const positionRow of props.positionRowsFor(row) ?? []) {
      list.push({ ...positionRow, _positionRow: true, _parentRow: row, _source: positionRow });
    }
  }
  return list;
});

const totals = computed(() => {
  const rows = props.rows.map((row) => props.statsFor(row) ?? {});
  return buildFieldingTotals(rows, columns);
});

function rowKey(row) {
  if (row?._positionRow) return `pos-${rowKey(row._parentRow)}-${row.position}`;
  if (row?._rowId != null) return `row_${row._rowId}`;
  if (row?.supplementId) return `p_${row.id}_${row.supplementId}`;
  return `p_${row?.id ?? ''}`;
}

function rowClassName({ row }) {
  return row?._positionRow ? 'row-position' : '';
}

function onRowClick(row) {
  currentRowKey.value = rowKey(row);
}

function toggle(row) {
  const key = rowKey(row);
  const next = new Set(expandedKeys.value);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  expandedKeys.value = next;
}

// 仅捕手守位计入的计数据；切换离开 C 时需清零，避免跨守位残留
const CATCHER_ONLY_KEYS = ['pb', 'catcherSb', 'catcherCs'];

function hasPlayerPositions(row) {
  return (props.positionRowsFor(row) ?? []).length > 0;
}

function positionFieldEnabled(row, key) {
  if (CATCHER_ONLY_KEYS.includes(key)) return row.position === 'C';
  return true;
}

// 当前守位会被 positionOptionsFor（排除已用守位）过滤掉，下拉里合并回当前项
function positionOptionsWithCurrent(row, options) {
  const list = [row.position, ...(options ?? [])]
    .map((value) => String(value ?? '').trim().toUpperCase())
    .filter(Boolean);
  return [...new Set(list)];
}

function summaryMethod({ columns: tableColumns }) {
  return buildFieldingSummaryCells(tableColumns, totals.value);
}

function setValue(row, key, value) {
  const stats = props.statsFor(row);
  if (stats) stats[key] = Number(value) || 0;
}

// 逐位置行存 outs，INN 列按出局换算展示与回写
function positionValue(row, key) {
  if (key === 'defInn') return outsToInnings(Number(row.outs) || 0);
  return Number(row[key]) || 0;
}

function setPositionValue(row, key, value) {
  if (!row._source) return;
  if (key === 'defInn') row._source.outs = inningsToOuts(Number(value) || 0);
  else row._source[key] = Number(value) || 0;
  emit('positions-changed', row._parentRow);
}

function changePosition(row, value) {
  if (!row._source) return;
  const position = String(value || '').toUpperCase();
  row._source.position = position;
  // 离开捕手位后清零 PB/SB/CS（源对象与展示副本同步），避免被汇总/保存
  if (position !== 'C') {
    for (const key of CATCHER_ONLY_KEYS) {
      row._source[key] = 0;
      row[key] = 0;
    }
  }
  emit('positions-changed', row._parentRow);
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
.game-fielding-table__player {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.game-fielding-table__expander {
  width: 16px;
  height: 16px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
}
.game-fielding-table__expander:hover {
  background: var(--el-fill-color-light);
}
.game-fielding-table__expander.is-open {
  color: var(--el-color-primary);
}
.game-fielding-table__count {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.game-fielding-table__sub-name {
  padding-left: 8px;
  color: var(--el-text-color-secondary);
}
.game-fielding-table__hint {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.game-fielding-table :deep(.el-table__body tr.row-position > td.el-table__cell) {
  background: var(--el-fill-color-lighter);
}
</style>
