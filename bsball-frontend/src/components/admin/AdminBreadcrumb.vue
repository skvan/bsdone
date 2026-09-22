<template>
  <div class="admin-breadcrumb-wrap">
    <el-breadcrumb :separator-icon="ArrowRight" class="admin-breadcrumb" :class="{ 'has-children': crumbs.length > 0 }">
      <el-breadcrumb-item :to="{ path: dashboardPath }">首页</el-breadcrumb-item>
      <el-breadcrumb-item v-for="(crumb, index) in crumbs" :key="index">
        <router-link v-if="index < crumbs.length - 1" :to="crumb.path">{{ crumb.titleResolved }}</router-link>
        <span v-else>{{ crumb.titleResolved }}</span>
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script>
// 面包屑 —— 行为移植自编译产物 AdminLayout chunk（AdminBreadcrumb）
export default {
  __scopeId: 'data-v-c5372c02'
};
</script>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowRight } from '@element-plus/icons-vue';
import { normalizeAdminPath } from '../../utils/tenantRoute';

const route = useRoute();

// 工作台路径（带租户）
const dashboardPath = computed(() => {
  const tenant = route.params.tenantCode;
  return tenant ? `/${tenant}/admin/dashboard` : '/admin/dashboard';
});

// 参数占位替换（:id → 实际值）
function resolvePath(path) {
  let resolved = path;
  for (const [key, value] of Object.entries(route.params)) {
    if (value && typeof value === 'string') resolved = resolved.replace(`:${key}`, value);
  }
  return resolved;
}

// 当前页 meta.breadcrumb（[{path,title}]）+ 当前页作为末级
const crumbs = computed(() => {
  const meta = route.meta;
  const title = meta?.title || route.name || '';
  if (!title || normalizeAdminPath(route.path) === '/admin/dashboard') return [];
  const list = (meta?.breadcrumb || []).map((item) => ({
    path: resolvePath(item.path),
    titleResolved: item.title || ''
  }));
  list.push({ path: route.path, titleResolved: title });
  return list;
});
</script>
