// 新旧版前端切换：旧版目标映射（Issue #105；H41 起为纯工具函数）
// 架构（用户确认）：旧版原位 /bs-ball/（零改动、base 天然匹配）；重建版部署于 /bs-ball-next/。
// H41 变更：按钮渲染从「全局 DOM 注入 + 悬浮保底 + 顶栏锚点迁移」改为组件化
//   （components/common/FrontendSwitchButton.vue）：由管理端布局（3 套顶栏变体）/门户布局/介绍页/
//   欢迎页/管理端登录/404 占位/文档壳模板内直接渲染；原注入机制整体下线
//   （顶栏重渲染不再导致按钮降级为悬浮胶囊遮挡管理员）。本文件仅保留 legacyTarget()。
// 页面级版本映射（H27）：新版→旧版按当前路径前缀互换（两版路由同构）；
// 特例：入口页 → 旧版首页；介绍页 → 旧版介绍页；根路径兜底旧版首页。
// dev（base=/bs-ball/）下映射等价自指（本地无旧版），属预期限制。
export function legacyTarget() {
  const base = ((import.meta.env && import.meta.env.BASE_URL) || '/bs-ball/').replace(/\/+$/, '');
  let p = (typeof location !== 'undefined' && location.pathname) || '/';
  if (base && base !== '/' && p.startsWith(base)) p = p.slice(base.length) || '/';
  p = p.replace(/\/+$/, '') || '/';
  if (p === '/welcome') return '/bs-ball/';
  if (p === '/intro') return '/index.html';
  return '/bs-ball' + (p === '/' ? '/' : p);
}
