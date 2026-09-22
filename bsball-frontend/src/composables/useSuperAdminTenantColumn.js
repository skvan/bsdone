// 超级管理员租户列 —— 行为移植自编译产物 useSuperAdminTenantColumn chunk
// 超管在列表中展示租户名：优先取用户自带 tenants，其次请求租户列表（进程内缓存）
import { computed, onMounted, ref } from 'vue';
import { tenantApi } from '../api/system';
import { useAuthStore } from '../stores/auth';

let tenantMapCache = null;
let tenantMapInflight = null;

function fetchTenantMap() {
  if (tenantMapCache) return Promise.resolve(tenantMapCache);
  if (tenantMapInflight) return tenantMapInflight;
  tenantMapInflight = tenantApi
    .list({ pageSize: 500 })
    .then(({ list }) => {
      const map = new Map();
      for (const tenant of list || []) map.set(tenant.id, tenant.name);
      tenantMapCache = map;
      tenantMapInflight = null;
      return map;
    })
    .catch(() => {
      tenantMapInflight = null;
      tenantMapCache = new Map();
      return tenantMapCache;
    });
  return tenantMapInflight;
}

export function useSuperAdminTenantColumn() {
  const auth = useAuthStore();
  const isSuperAdmin = computed(() => auth.user?.superAdmin === true);
  const tenantMap = ref(null);

  onMounted(async () => {
    if (isSuperAdmin.value) tenantMap.value = await fetchTenantMap();
  });

  function tenantLabel(tenantId) {
    const user = auth.user;
    const id = tenantId != null ? Number(tenantId) : user?.tenantId != null ? Number(user.tenantId) : NaN;
    if (Number.isNaN(id)) return '—';
    const name = user?.tenants?.find((t) => t.id === id)?.name;
    if (name) return name;
    const map = tenantMap.value ?? tenantMapCache;
    if (map) return map.get(id) ?? `#${id}`;
    return `#${id}`;
  }

  return { isSuperAdmin, tenantLabel };
}
