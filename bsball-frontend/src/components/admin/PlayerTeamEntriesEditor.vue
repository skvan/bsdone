<template>
  <div class="pte-editor">
    <p v-if="!entries.length" class="pte-empty">暂无球队经历，点击下方「添加球队经历」为球员登记球队。</p>
    <div v-for="(seg, idx) in entries" :key="idx" class="pte-seg">
      <div class="pte-seg-head">
        <span class="pte-seg-title">球队经历 {{ idx + 1 }}</span>
        <el-tag v-if="seg.current" type="success" size="small" effect="plain">当前球队</el-tag>
        <span class="pte-seg-spacer"></span>
        <el-button link type="danger" @click="removeSegment(idx)">删除</el-button>
      </div>
      <div class="pte-seg-grid">
        <div class="pte-field">
          <span class="pte-label">球队</span>
          <el-select v-model="seg.teamId" placeholder="选择球队" filterable clearable style="width: 100%">
            <el-option v-for="item in teams" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </div>
        <div class="pte-field">
          <span class="pte-label">背号</span>
          <el-input
            v-model="seg.number"
            maxlength="4"
            placeholder="如 00、1、01"
            @input="onNumberInput(seg)"
          />
        </div>
        <div class="pte-field pte-field--positions">
          <span class="pte-label">守备位置</span>
          <el-select
            v-model="seg.positions"
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            placeholder="可多选"
            style="width: 100%"
          >
            <el-option v-for="code in positionCodes" :key="code" :label="formatPosition(code)" :value="code" />
          </el-select>
        </div>
        <div class="pte-field pte-field--current">
          <span class="pte-label">&nbsp;</span>
          <el-checkbox v-model="seg.current">当前球队</el-checkbox>
        </div>
      </div>
    </div>
    <div class="pte-actions">
      <el-button plain @click="addSegment">添加球队经历</el-button>
      <span class="pte-hint">勾选「当前球队」表示目前在队（可多选）；未勾选为历史经历</span>
    </div>
  </div>
</template>

<script setup>
import { POSITION_LABELS, formatPosition } from '../../utils/playerOptions';

// 经历数组由父页面（PlayerList 编辑表单的响应式状态 l.teamEntries）直接传入并共享：
// 段内字段编辑与增删直接作用于该响应式数组，无需额外事件同步。
const props = defineProps({
  entries: { type: Array, default: () => [] },
  teams: { type: Array, default: () => [] }
});

const positionCodes = Object.keys(POSITION_LABELS);

function addSegment() {
  props.entries.push({ teamId: null, number: '', positions: [], current: false });
}

function removeSegment(idx) {
  props.entries.splice(idx, 1);
}

function onNumberInput(seg) {
  seg.number = String(seg.number ?? '').replace(/\D/g, '').slice(0, 4);
}
</script>

<style scoped>
.pte-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pte-empty {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.pte-seg {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px 12px 8px;
  background: var(--el-fill-color-lighter);
}

.pte-seg-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.pte-seg-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.pte-seg-spacer {
  flex: 1;
}

.pte-seg-grid {
  display: grid;
  grid-template-columns: minmax(150px, 1.2fr) 110px minmax(170px, 1.4fr) auto;
  gap: 12px;
  align-items: start;
}

.pte-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.pte-field--current {
  display: flex;
  flex-direction: column;
}

.pte-field--current :deep(.el-checkbox) {
  height: 32px;
}

.pte-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pte-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

@media (max-width: 768px) {
  .pte-seg-grid {
    grid-template-columns: 1fr 1fr;
  }

  .pte-field--positions {
    grid-column: 1 / -1;
  }
}
</style>
