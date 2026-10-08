<template>
  <div class="welcome">
    <div class="welcome__bg"></div>
    <main class="welcome__inner">
      <div class="welcome__brand">
        <img v-if="logoUrl" :src="logoUrl" class="welcome__logo" alt="logo" />
        <div class="welcome__badge"><span class="welcome__badge-dot"></span>国内领先 · 专业级棒垒球 SaaS 平台</div>
        <h1 class="welcome__title">选择您的<em>入口</em></h1>
        <p class="welcome__subtitle">了解平台功能，或直接进入数据系统</p>
      </div>

      <div class="welcome__cards">
        <button type="button" class="welcome__card welcome__card--intro" @click="goIntro">
          <span class="welcome__card-icon">📋</span>
          <span class="welcome__card-name">系统介绍</span>
          <span class="welcome__card-desc">了解平台功能、定价方案与应用场景</span>
        </button>
        <button type="button" class="welcome__card welcome__card--portal" @click="goPortal">
          <span class="welcome__card-icon">⚾</span>
          <span class="welcome__card-name">进入门户</span>
          <span class="welcome__card-desc">查看赛事数据、球队排名与球员统计</span>
        </button>
      </div>
    </main>

    <footer v-if="footerText" class="welcome__footer" v-html="footerText"></footer>
    <!-- H41：新旧版切换按钮组件化接入（无顶栏页面：右上角悬浮胶囊，与原注入保底一致） -->
    <FrontendSwitchButton variant="floating" />
  </div>
</template>

<script setup>
// Welcome —— 新版展示/入口页（Issue #105 双轨体验对齐；入口选择与旧版入口页对齐：
// 「系统介绍」→ 新版系统介绍页 /intro、「进入门户」→ 门户首页）
// 品牌信息来自站点配置（appConfig store，/portal/settings）；无门户外壳，独立设计。
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAppConfigStore } from '../../stores/appConfig';
import { DEFAULT_TENANT_CODE } from '../../utils/tenantRoute';
import FrontendSwitchButton from '../../components/common/FrontendSwitchButton.vue';

const router = useRouter();
const config = useAppConfigStore();
onMounted(() => {
  if (!config.config || !config.config.siteName) config.load().catch(() => {});
});

// logo 走与门户页头一致的资源解析（resolveAssetUrl：相对路径补 /bs-ball/ 前缀；测试环境实证
// /bs-ball-logo.png 直用 404、/bs-ball/bs-ball-logo.png 200，H25 修复）
const logoUrl = computed(() => (config.config && config.config.logoUrl ? config.resolveAssetUrl(config.config.logoUrl) : ''));
const footerText = computed(() => (config.config && config.config.showFooterPortal !== false ? config.footerTextPortal : '') || '');

// 门户目标按命名路由解析（href 自动携带部署 base：dev=/bs-ball/、线上=/bs-ball-next/）；
// 不可硬编码 '/租户码/'：剥离 base 后即根路由 '/'，会被 redirect /welcome 拦截（原地打转）或落到旧版
const portalHref = router.resolve({ name: 'PortalHome', params: { tenantCode: DEFAULT_TENANT_CODE } }).href;

// 系统介绍页 = 新版系统介绍页（H27 起为本版 SPA 页面；旧版静态官网 /index.html 保留给旧轨）
function goIntro() { location.href = router.resolve({ name: 'PortalIntro' }).href; }
function goPortal() { location.href = portalHref; }
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
.welcome__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 22px;
  padding: 6px 16px;
  border: 1px solid rgba(255, 255, 255, .22);
  border-radius: 999px;
  background: rgba(255, 255, 255, .06);
  font-size: 13px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, .85);
}
.welcome__badge-dot { width: 7px; height: 7px; border-radius: 50%; background: #e3b23c; box-shadow: 0 0 8px rgba(227, 178, 60, .8); }
.welcome__title {
  margin: 0;
  font-size: 44px;
  letter-spacing: 2px;
  background: linear-gradient(180deg, #fff 30%, #e8d9a6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.welcome__title em { font-style: normal; color: #e3b23c; -webkit-text-fill-color: #e3b23c; }
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
.welcome__footer { position: relative; z-index: 1; text-align: center; padding: 18px 16px; font-size: 13px; color: rgba(255, 255, 255, .65); background: rgba(0, 0, 0, .22); }
</style>
