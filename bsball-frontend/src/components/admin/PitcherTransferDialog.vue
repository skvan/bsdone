<template>
  <el-dialog
    :model-value="modelValue"
    title="转记投手数据"
    width="390px"
    append-to-body
    destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-form label-width="72px" size="default">
      <el-form-item label="原投手">
        <span class="strong">{{ sourceLabel }}</span>
      </el-form-item>
      <el-form-item label="目标投手">
        <el-select v-model="targetId" placeholder="选择同队投手" filterable style="width: 100%">
          <el-option
            v-for="item in targets"
            :key="item.playerId"
            :label="item.label"
            :value="item.playerId"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <table v-if="previewRows.length" class="transfer-preview">
      <thead>
        <tr><th>项目</th><th>原</th><th>目标现有</th><th>转记后</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in previewRows" :key="row.key">
          <td>{{ row.label }}</td>
          <td>{{ row.source }}</td>
          <td>{{ row.target }}</td>
          <td class="after">{{ row.after }}</td>
        </tr>
      </tbody>
    </table>

    <p class="transfer-note">
      目标已有值优先；只补入原投手非零、目标为零的投球/守备字段。R/ER 由 ER 引擎管理，不参与转记。
      预览基于当前页面；确认转记以已保存数据为准，未保存的修改会丢失。
    </p>
    <div class="transfer-steps">
      <span class="step first">① 转记</span>
      <span class="step">② 单独确认删除</span>
      <span class="step">③ 写审计</span>
    </div>
    <p class="transfer-hint">确认转记只执行转记，不会删除源行；完成后会单独询问是否删除源行。</p>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :disabled="!targetId" @click="submit">确认转记</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { mergePitcherStats } from '../../utils/pitcherStatTransfer';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  source: { type: Object, default: null },
  targets: { type: Array, default: () => [] }
});
const emit = defineEmits(['update:modelValue', 'confirm']);
const targetId = ref(null);

const sourceLabel = computed(() => {
  const source = props.source;
  if (!source) return '—';
  const number = source.number != null && String(source.number).trim() !== ''
    ? `${source.number}号 ` : '';
  return `${number}${source.name || `#${source.playerId}`}（${source.position || 'P'}）`;
});

const selectedTarget = computed(() => (
  props.targets.find((item) => String(item.playerId) === String(targetId.value)) || null
));

const previewRows = computed(() => {
  const source = props.source?.stats;
  const target = selectedTarget.value?.stats;
  if (!source || !target) return [];
  // 只用于预览：在副本上合并，不影响真实表单项。
  const sourceCopy = { ...source };
  const targetCopy = { ...target };
  mergePitcherStats(sourceCopy, targetCopy);
  const value = (v) => (v == null || v === '' ? '0' : String(v));
  return [
    { key: 'ip', label: 'IP', source: value(source.ip), target: value(target.ip), after: value(targetCopy.ip) },
    { key: 're', label: 'R / ER', source: `${value(source.pitchR)} / ${value(source.er)}`, target: `${value(target.pitchR)} / ${value(target.er)}`, after: `${value(targetCopy.pitchR)} / ${value(targetCopy.er)}` },
    { key: 'np', label: 'NP', source: value(source.np), target: value(target.np), after: value(targetCopy.np) },
    { key: 'pa', label: 'PA', source: value(source.pitchPa), target: value(target.pitchPa), after: value(targetCopy.pitchPa) },
    { key: 'bf', label: 'BF', source: value(source.pitchBf), target: value(target.pitchBf), after: value(targetCopy.pitchBf) },
    { key: 'h', label: 'H', source: value(source.pitchH), target: value(target.pitchH), after: value(targetCopy.pitchH) },
    { key: 'so', label: 'SO', source: value(source.pitchSo), target: value(target.pitchSo), after: value(targetCopy.pitchSo) },
    { key: 'bb', label: 'BB', source: value(source.pitchBb), target: value(target.pitchBb), after: value(targetCopy.pitchBb) },
    { key: 'fo', label: 'FO', source: value(source.fo), target: value(target.fo), after: value(targetCopy.fo) }
  ];
});

watch(
  () => [props.modelValue, props.targets],
  () => {
    if (!props.modelValue) return;
    const exists = props.targets.some((item) => String(item.playerId) === String(targetId.value));
    if (!exists) targetId.value = props.targets[0]?.playerId ?? null;
  },
  { immediate: true, deep: true }
);

function submit() {
  if (!targetId.value) return;
  emit('confirm', targetId.value);
}
</script>

<style scoped>
.strong {
  font-weight: 600;
}
.transfer-preview {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  margin: 2px 0 8px;
}
.transfer-preview th {
  background: #f5f7fa;
  color: #6b7480;
  font-weight: 500;
  padding: 5px 4px;
  border: 1px solid #ebeef5;
  text-align: center;
}
.transfer-preview td {
  padding: 5px 4px;
  border: 1px solid #ebeef5;
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.transfer-preview td:first-child {
  text-align: left;
  color: #606266;
}
.transfer-preview .after {
  color: #2f7d32;
  font-weight: 600;
}
.transfer-note {
  margin: 0 0 7px;
  padding: 7px 9px;
  border: 1px solid #faecd8;
  border-radius: 5px;
  background: #fdf6ec;
  color: #966b28;
  font-size: 11px;
  line-height: 1.55;
}
.transfer-steps {
  display: flex;
  gap: 4px;
  justify-content: center;
  flex-wrap: wrap;
  margin: 2px 0 6px;
}
.step {
  padding: 2px 5px;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  background: #f5f7fa;
  color: #7a8494;
  font-size: 10px;
}
.step.first {
  border-color: #d9ecff;
  background: #ecf5ff;
  color: #2a7fd0;
}
.transfer-hint {
  margin: 0;
  color: #909399;
  font-size: 11px;
  line-height: 1.5;
}
</style>
