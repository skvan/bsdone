<template>
  <el-table
    ref="tableRef"
    v-loading="loading"
    :data="rows"
    max-height="420"
    @selection-change="onSelectionChange"
  >
    <el-table-column v-if="selectable" type="selection" width="44" />
    <el-table-column prop="name" :label="t('orderBook.colPlayer')" min-width="110" />
    <el-table-column :label="t('orderBook.colNumber')" width="80">
      <template #default="{ row }">{{ row.number || '-' }}</template>
    </el-table-column>
    <el-table-column :label="t('orderBook.colPositions')" min-width="120">
      <template #default="{ row }">{{ (row.positions || []).join(' / ') || '-' }}</template>
    </el-table-column>
    <el-table-column v-if="showCurrentInTeam" :label="t('orderBook.colInTeam')" width="90">
      <template #default="{ row }">
        <el-tag v-if="!row.currentInTeam" type="danger" effect="light">{{ t('orderBook.leftTeam') }}</el-tag>
        <span v-else>{{ t('orderBook.inTeam') }}</span>
      </template>
    </el-table-column>
    <template #empty>{{ emptyText }}</template>
  </el-table>
</template>

<script setup>
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  /** 显示多选列（编辑态） */
  selectable: { type: Boolean, default: false },
  /** 显示"在队/已离队"列（查看态） */
  showCurrentInTeam: { type: Boolean, default: false },
  emptyText: { type: String, default: '' }
});

const emit = defineEmits(['selection-change']);
const { t } = useI18n();

const tableRef = ref(null);

function onSelectionChange(selection) {
  emit('selection-change', selection);
}

// 供父组件恢复勾选（el-table 勾选态受控性差，用 toggleRowSelection 回填）
defineExpose({
  toggleRowSelection: (row, selected) => tableRef.value?.toggleRowSelection(row, selected)
});
</script>
