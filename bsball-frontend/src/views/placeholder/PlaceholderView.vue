<template>
  <div class="placeholder-page">
    <h1 class="placeholder-page__title">BS Ball</h1>
    <p class="placeholder-page__status">前端重建中</p>
    <p class="placeholder-page__hint">
      该页面尚未迁移（计划组件：{{ plannedComponent || '—' }}），将在后续批次按契约逐步开放。
    </p>
    <p class="placeholder-page__path">{{ route.fullPath }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const plannedComponent = computed(() => route.meta.plannedComponent || '');
</script>

<style scoped>
.placeholder-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 8px;
}

.placeholder-page__title {
  margin: 0;
  font-size: 28px;
  color: var(--el-text-color-primary, #303133);
}

.placeholder-page__status {
  margin: 0;
  color: var(--el-color-primary, #409eff);
}

.placeholder-page__hint {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary, #909399);
}

.placeholder-page__path {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder, #a8abb2);
}
</style>
