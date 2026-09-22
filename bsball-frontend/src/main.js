import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';

// 说明：全局注册 Element Plus，与旧版编译产物行为对齐（旧入口 chunk 全局注册）。
// 路由与契约层随批次建设：B1 将按契约（.qoder/frontend-recon/out/contracts/routes.md，99 条）全量注册。
createApp(App).use(ElementPlus).use(router).mount('#app');
