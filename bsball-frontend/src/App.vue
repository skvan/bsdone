<template>
  <el-config-provider :locale="epLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
// 应用根组件 —— 行为移植自编译产物入口 chunk（App setup）
// 职责：Element Plus 语言包联动、按路由切换管理端/门户主题、html 路由标记、
//       跨标签页同步绑定、启动时登录态恢复（/auth/me）
import { computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import zhTw from 'element-plus/es/locale/lang/zh-tw';
import en from 'element-plus/es/locale/lang/en';
import ko from 'element-plus/es/locale/lang/ko';
import ja from 'element-plus/es/locale/lang/ja';
import { useAuthStore } from './stores/auth';
import { useSettingsStore } from './stores/settings';
import { usePortalThemeStore } from './stores/portalTheme';
import { resolveToken, currentTenantCode, ensureUserStoredForTenant } from './api/tokenStorage';
import { accountApi } from './api/account';
import { isAuthSessionError } from './api/request';

const route = useRoute();
const { locale } = useI18n();

// Element Plus 语言包（与编译产物：zh-TW/en/ko/ja 直选，其余 zh-cn）
const epLocale = computed(() => {
  const current = locale.value;
  if (current === 'zh-TW') return zhTw;
  if (current === 'en') return en;
  if (current === 'ko') return ko;
  if (current === 'ja') return ja;
  return zhCn;
});

// 管理端路由用管理端主题；门户路由用门户主题
function applyThemeByRoute() {
  if (route.path.includes('/admin')) {
    const settings = useSettingsStore();
    settings.init();
    settings.applyToDOM();
  } else {
    usePortalThemeStore().applyToDOM();
  }
}

const darkQuery = window.matchMedia('(prefers-color-scheme: dark)');

// 系统明暗变化：auto 模式下即时生效
function onSchemeChange() {
  if (route.path.includes('/admin')) {
    const settings = useSettingsStore();
    if (settings.theme === 'auto') settings.applyToDOM();
  } else {
    const portal = usePortalThemeStore();
    if (portal.theme === 'auto') portal.applyToDOM();
  }
}

watch(() => route.path, applyThemeByRoute, { immediate: true });

// html 上的路由域标记（admin-route / portal-route）
watch(
  () => route.path,
  (path) => {
    const el = document.documentElement;
    const isAdmin = /\/admin(\/|$)/.test(path);
    el.classList.toggle('admin-route', isAdmin);
    el.classList.toggle('portal-route', !isAdmin);
  },
  { immediate: true }
);

onMounted(() => {
  darkQuery.addEventListener('change', onSchemeChange);
  const auth = useAuthStore();
  auth.bindCrossTabSync();
  useSettingsStore().bindCrossTabSync();
  // 已登录租户：拉取最新用户信息并回写存储（失败静默；会话失效则清空登录态）
  if (resolveToken(currentTenantCode())) {
    accountApi
      .me()
      .then((body) => {
        if (body?.data?.user) {
          const code = currentTenantCode();
          ensureUserStoredForTenant(code);
          auth.setUser(body.data.user, code);
        }
      })
      .catch((error) => {
        if (isAuthSessionError(error)) auth.clear();
      });
  }
});

onUnmounted(() => {
  darkQuery.removeEventListener('change', onSchemeChange);
  useAuthStore().unbindCrossTabSync();
  useSettingsStore().unbindCrossTabSync();
});
</script>
