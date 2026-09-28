<template>
  <div class="welcome">
    <div class="welcome__bg"></div>
    <main class="welcome__inner">
      <div class="welcome__brand">
        <img v-if="logoUrl" :src="logoUrl" class="welcome__logo" alt="logo" />
        <h1 class="welcome__title">{{ siteName }}</h1>
        <p class="welcome__subtitle">{{ siteTitle }}</p>
      </div>

      <div class="welcome__cards">
        <button type="button" class="welcome__card welcome__card--portal" @click="goPortal">
          <span class="welcome__card-icon">⚾</span>
          <span class="welcome__card-name">浏览门户</span>
          <span class="welcome__card-desc">赛事资讯 · 球队球员 · 数据统计</span>
        </button>
        <button type="button" class="welcome__card welcome__card--admin" @click="goAdmin">
          <span class="welcome__card-icon">🗂</span>
          <span class="welcome__card-name">进入管理台</span>
          <span class="welcome__card-desc">赛事管理 · 数据录入 · 系统配置</span>
        </button>
      </div>

      <div class="welcome__links">
        <a class="welcome__link" :href="legacyUrl">切换到旧版</a>
      </div>
    </main>

    <footer v-if="footerText" class="welcome__footer" v-html="footerText"></footer>
  </div>
</template>

<script setup>
// Welcome —— 新版展示/入口页（Issue #105 双轨体验对齐：宣传展示 + 入口选择）
// 品牌信息来自站点配置（appConfig store，/portal/settings）；无门户外壳，独立设计。
import { computed, onMounted } from 'vue';
import { useAppConfigStore } from '../../stores/appConfig';
import { DEFAULT_TENANT_CODE } from '../../utils/tenantRoute';

const config = useAppConfigStore();
onMounted(() => {
  if (!config.config || !config.config.siteName) config.load().catch(() => {});
});

const siteName = computed(() => config.siteName || 'BS Ball');
const siteTitle = computed(() => config.siteTitle || '棒垒球赛事平台');
const logoUrl = computed(() => config.config && config.config.logoUrl ? config.config.logoUrl : '');
const footerText = computed(() => (config.config && config.config.showFooterPortal !== false ? config.footerTextPortal : '') || '');

const portalPath = '/' + DEFAULT_TENANT_CODE + '/';
const legacyUrl = import.meta.env.BASE_URL.replace(/\/$/, '').replace(/-next$/, '') + '/'; // /bs-ball/（旧版原位）

function goPortal() { location.href = portalPath; }
function goAdmin() { location.href = portalPath + 'admin'; }
</script>

<style scoped>
.welcome {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: #fff;
  background: #0f2039;
  overflow: hidden;
}
.welcome__bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1200px 600px at 80% -10%, rgba(201, 162, 39, .28), transparent 60%),
    radial-gradient(900px 500px at 10% 110%, rgba(196, 30, 58, .30), transparent 60%),
    linear-gradient(135deg, #0e1c33 0%, #16304f 55%, #0f2039 100%);
}
.welcome__inner {
  position: relative;
  z-index: 1;
  flex: 1;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  padding: 64px 24px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 44px;
  box-sizing: border-box;
}
.welcome__brand { text-align: center; }
.welcome__logo { height: 72px; margin-bottom: 18px; }
.welcome__title {
  margin: 0;
  font-size: 44px;
  letter-spacing: 2px;
  background: linear-gradient(180deg, #fff 30%, #e8d9a6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.welcome__subtitle { margin: 14px 0 0; font-size: 16px; color: rgba(255, 255, 255, .72); letter-spacing: 1px; }
.welcome__cards { width: 100%; display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 20px; }
.welcome__card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 30px 20px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, .18);
  background: rgba(255, 255, 255, .06);
  color: #fff;
  cursor: pointer;
  transition: transform .15s, background .15s, border-color .15s;
}
.welcome__card:hover { transform: translateY(-3px); background: rgba(255, 255, 255, .11); border-color: rgba(201, 162, 39, .55); }
.welcome__card-icon { font-size: 30px; }
.welcome__card-name { font-size: 20px; font-weight: 600; letter-spacing: 1px; }
.welcome__card-desc { font-size: 12px; color: rgba(255, 255, 255, .6); }
.welcome__links { display: flex; gap: 20px; }
.welcome__link { color: rgba(255, 255, 255, .55); font-size: 13px; text-decoration: none; }
.welcome__link:hover { color: #fff; text-decoration: underline; }
.welcome__footer { position: relative; z-index: 1; text-align: center; padding: 18px 16px; font-size: 13px; color: rgba(255, 255, 255, .65); background: rgba(0, 0, 0, .22); }
</style>
