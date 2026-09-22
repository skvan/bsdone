import { createRouter, createWebHistory } from 'vue-router';
import Placeholder from '../views/Placeholder.vue';

// B0：仅注册占位路由，保证任意路径可渲染。
// B1 将按提取契约（.qoder/frontend-recon/out/contracts/routes.md，99 条）全量注册
// 门户 / 租户 / 管理三区路由，未迁移页面统一指向占位组件。
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/:pathMatch(.*)*', name: 'Placeholder', component: Placeholder }
  ]
});

export default router;
