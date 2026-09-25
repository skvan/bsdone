// 超级管理员租户选择态 —— 移植自编译产物 request chunk（adminConfigTenant store）
import { defineStore } from 'pinia';

export const useAdminConfigTenantStore = defineStore('adminConfigTenant', {
  state: () => ({
    overrideTenantId: null
  }),
  actions: {
    setOverride(tenantId) {
      this.overrideTenantId = tenantId;
    }
  }
});
