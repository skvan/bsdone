<template>
  <el-divider content-position="left">赛制设置</el-divider>
  <el-form-item label="赛制">
    <el-radio-group v-model="formatType" class="format-grid">
      <el-radio
        v-for="option in formatOptions"
        :key="option.value"
        :value="option.value"
        border
        class="format-radio"
      >
        <span class="format-title">{{ option.title }}</span>
        <span class="format-desc">{{ option.desc }}</span>
      </el-radio>
    </el-radio-group>
  </el-form-item>

  <template v-if="showPoints">
    <el-form-item label="积分制">
      <el-radio-group v-model="pointsSystem" class="points-grid">
        <el-radio v-for="option in pointsOptions" :key="option.value" :value="option.value" border class="points-radio">
          <span class="points-code">{{ option.code }}</span>
          <span class="points-desc">{{ option.desc }}</span>
        </el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <div class="format-hint">积分只影响排名；胜率仍按 W / (W + L) 计算，创建后可在赛事设置中修改。</div>
    </el-form-item>
  </template>

  <template v-if="isGroup">
    <el-divider content-position="left">小组与晋级</el-divider>
    <el-form-item label="小组数">
      <el-select v-model="groupCount" style="width: 120px">
        <el-option v-for="n in [1, 2, 4, 8]" :key="n" :label="`${n} 组`" :value="n" />
      </el-select>
    </el-form-item>
    <el-form-item label="每组晋级">
      <el-select v-model="advancePerGroup" style="width: 120px">
        <el-option v-for="n in [1, 2, 3, 4]" :key="n" :label="`${n} 名`" :value="n" />
      </el-select>
    </el-form-item>
    <el-form-item label="三四名">
      <el-checkbox v-model="thirdPlace">设置三四名决赛</el-checkbox>
    </el-form-item>
    <el-form-item>
      <div class="format-hint">淘汰赛签表：{{ groupCount }} 组 × 每组 {{ advancePerGroup }} 名 = {{ groupCount * advancePerGroup }} 队。</div>
    </el-form-item>
  </template>

  <template v-if="isKnockout">
    <el-divider content-position="left">淘汰赛设置</el-divider>
    <el-form-item label="签表规模">
      <el-select v-model="bracketSize" style="width: 120px">
        <el-option v-for="n in [2, 4, 8, 16, 32]" :key="n" :label="`${n} 队`" :value="n" />
      </el-select>
    </el-form-item>
    <el-form-item label="三四名">
      <el-checkbox v-model="thirdPlace">设置三四名决赛</el-checkbox>
    </el-form-item>
    <el-form-item>
      <div class="format-hint">单败淘汰不计算积分，直接按对阵图晋级。</div>
    </el-form-item>
  </template>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  modelValue: { type: Object, required: true }
});
const emit = defineEmits(['update:modelValue']);

const formatOptions = [
  { value: 'SINGLE_ROUND_ROBIN', title: '单循环', desc: '每两队交手 1 次 · 按积分排名' },
  { value: 'DOUBLE_ROUND_ROBIN', title: '双循环', desc: '每两队交手 2 次 · 按积分排名' },
  { value: 'KNOCKOUT', title: '单败淘汰', desc: '输一场即淘汰 · 树状对阵' },
  { value: 'GROUP_KNOCKOUT', title: '小组赛 + 淘汰赛', desc: '小组单循环 · 每组前 N 名晋级' }
];
const pointsOptions = [
  { value: '210', code: '2-1-0', desc: '胜 2 分 · 平 1 分 · 负 0 分' },
  { value: '310', code: '3-1-0', desc: '胜 3 分 · 平 1 分 · 负 0 分' }
];

const formatType = computed({
  get: () => props.modelValue.formatType || 'SINGLE_ROUND_ROBIN',
  set: (value) => update('formatType', value)
});
const pointsSystem = computed({
  get: () => (Number(props.modelValue.pointsWin) === 3 ? '310' : '210'),
  set: (value) => emit('update:modelValue', {
    ...props.modelValue,
    pointsWin: value === '310' ? 3 : 2,
    pointsDraw: 1,
    pointsLoss: 0
  })
});
const groupCount = computed({
  get: () => positiveNumber(props.modelValue.groupCount, 2),
  set: (value) => update('groupCount', value)
});
const advancePerGroup = computed({
  get: () => positiveNumber(props.modelValue.advancePerGroup, 2),
  set: (value) => update('advancePerGroup', value)
});
const thirdPlace = computed({
  get: () => props.modelValue.thirdPlace !== false,
  set: (value) => update('thirdPlace', value)
});
const bracketSize = computed({
  get: () => Number(props.modelValue.bracketSize) || 8,
  set: (value) => update('bracketSize', value)
});
const showPoints = computed(() => formatType.value !== 'KNOCKOUT');
const isGroup = computed(() => formatType.value === 'GROUP_KNOCKOUT');
const isKnockout = computed(() => formatType.value === 'KNOCKOUT');

function positiveNumber(value, fallback) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : fallback;
}

function update(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value });
}
</script>

<style scoped>
.format-grid,
.points-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  width: 100%;
}
.format-grid :deep(.el-radio),
.points-grid :deep(.el-radio) {
  margin-right: 0;
  height: auto;
  min-height: 54px;
  padding: 8px 12px;
  align-items: flex-start;
  white-space: normal;
}
.format-grid :deep(.el-radio__label),
.points-grid :deep(.el-radio__label) {
  white-space: normal;
  line-height: 1.4;
}
.format-title,
.points-code {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.format-desc,
.points-desc {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
.points-radio {
  min-height: 44px !important;
  align-items: center !important;
}
.format-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
@media (max-width: 640px) {
  .format-grid,
  .points-grid {
    grid-template-columns: 1fr;
  }
}
</style>
