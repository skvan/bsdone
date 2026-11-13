<template>
  <template v-if="isDouble">
    <el-divider content-position="left">赛程阶段</el-divider>
    <el-form-item label="回合">
      <el-input-number v-model="roundNo" :min="1" :max="2" :controls="false" style="width: 120px" />
      <span class="stage-hint">双循环的第 1 / 2 回合</span>
    </el-form-item>
  </template>

  <template v-else-if="showStage">
    <el-divider content-position="left">赛程阶段</el-divider>
    <el-form-item label="阶段">
      <el-select v-model="stageCode" clearable placeholder="未设置" style="width: 170px">
        <el-option v-for="option in stageOptions" :key="option.value" :label="option.label" :value="option.value" />
      </el-select>
      <el-select
        v-if="stageCode === 'GROUP'"
        v-model="groupCode"
        clearable
        placeholder="组别"
        style="width: 110px; margin-left: 8px"
      >
        <el-option v-for="group in groupOptions" :key="group" :label="`${group} 组`" :value="group" />
      </el-select>
    </el-form-item>
    <el-form-item v-if="stageCode === 'GROUP'" label="轮次">
      <el-input-number v-model="roundNo" :min="1" :max="99" :controls="false" style="width: 120px" />
      <span class="stage-hint">小组赛第几轮（可选）</span>
    </el-form-item>
    <el-form-item v-if="isBracket" label="对阵位">
      <el-input-number v-model="bracketSlot" :min="1" :max="64" :controls="false" style="width: 120px" />
      <span class="stage-hint">同一轮次内的位置，从 1 开始</span>
    </el-form-item>
  </template>

  <el-form-item v-else-if="formatType" label="赛程阶段">
    <span class="stage-hint">单循环无需阶段设置</span>
  </el-form-item>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  modelValue: { type: Object, required: true },
  formatType: { type: String, default: '' },
  groupCount: { type: Number, default: 2 }
});
const emit = defineEmits(['update:modelValue']);

const isDouble = computed(() => props.formatType === 'DOUBLE_ROUND_ROBIN');
const showStage = computed(() => props.formatType === 'KNOCKOUT' || props.formatType === 'GROUP_KNOCKOUT');
const isBracket = computed(() =>
  ['R16', 'QF', 'SF', 'FINAL', 'THIRD_PLACE'].includes(stageCode.value)
);
const stageOptions = computed(() => {
  const options = [
    { value: 'GROUP', label: '小组赛' },
    { value: 'R16', label: '1/8 决赛' },
    { value: 'QF', label: '1/4 决赛' },
    { value: 'SF', label: '半决赛' },
    { value: 'FINAL', label: '决赛' },
    { value: 'THIRD_PLACE', label: '三四名决赛' }
  ];
  return props.formatType === 'KNOCKOUT' ? options.filter((option) => option.value !== 'GROUP') : options;
});
const groupOptions = computed(() =>
  Array.from({ length: props.groupCount || 2 }, (_, index) => String.fromCharCode(65 + index))
);
const stageCode = computed({
  get: () => props.modelValue.stageCode || '',
  set: (value) => emit('update:modelValue', {
    ...props.modelValue,
    stageCode: value || null,
    groupCode: value === 'GROUP' ? (props.modelValue.groupCode || null) : null
  })
});
const groupCode = computed({
  get: () => props.modelValue.groupCode || '',
  set: (value) => update('groupCode', value || null)
});
const roundNo = computed({
  get: () => props.modelValue.roundNo ?? undefined,
  set: (value) => update('roundNo', value ?? null)
});
const bracketSlot = computed({
  get: () => props.modelValue.bracketSlot ?? undefined,
  set: (value) => update('bracketSlot', value ?? null)
});

function update(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value });
}
</script>

<style scoped>
.stage-hint {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
