// 租户路由组合式 —— 行为移植自编译产物 useTenantRouter chunk
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { buildAdminPath, buildPortalPath } from '../utils/tenantRoute';

export function useTenantRouter() {
  const route = useRoute();
  const tenantCode = computed(() => String(route.params.tenantCode || ''));

  function portalPath(subPath) {
    const tenant = tenantCode.value;
    if (tenant) return buildPortalPath(tenant, subPath);
    return subPath.startsWith('/') ? subPath : '/' + subPath;
  }

  function adminPath(subPath) {
    const tenant = tenantCode.value;
    if (tenant) return buildAdminPath(tenant, subPath);
    return subPath.startsWith('/') ? subPath : '/' + subPath;
  }

  return { tenantCode, portalPath, adminPath };
}
