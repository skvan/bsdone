<template>
  <el-divider content-position="left">特殊规则</el-divider>
  <template v-if="available">
    <el-form-item label="EP 制">
      <div class="event-special-rule">
        <el-switch
          :model-value="modelValue.extraBatter"
          @update:model-value="update('extraBatter', $event)"
        />
        <span class="event-special-rule__hint">允许棒球打 10-12 人、垒球打 11 人（默认关闭）</span>
      </div>
    </el-form-item>
    <el-form-item label="强制换局">
      <div class="event-special-rule">
        <el-switch
          :model-value="modelValue.forceHalf"
          @update:model-value="update('forceHalf', $event)"
        />
        <span class="event-special-rule__hint">关闭后本赛事实时录入页隐藏「强制换局」按钮（默认开启）</span>
      </div>
    </el-form-item>
  </template>
  <el-form-item v-else label="特殊规则">
    <span class="event-special-rule__hint">{{ hint }}</span>
  </el-form-item>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Object, required: true },
  available: { type: Boolean, default: false },
  hint: { type: String, default: '' }
});
const emit = defineEmits(['update:modelValue']);

function update(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value });
}
</script>

<style scoped>
.event-special-rule {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.event-special-rule__hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.45;
}
</style>
