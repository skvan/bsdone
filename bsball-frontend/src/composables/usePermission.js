// 权限判定组合式 —— 行为移植自编译产物 usePermission chunk
import { computed } from 'vue';
import { useAuthStore } from '../stores/auth';

export function usePermission() {
  const auth = useAuthStore();
  const perms = computed(() => auth.perms);

  function hasPerm(code) {
    return auth.hasPerm(code);
  }

  function hasMenuId(menuId) {
    if (auth.user?.superAdmin) return true;
    return auth.menuIds.has(menuId);
  }

  return { perms, hasPerm, hasMenuId };
}
