// 路由骨架（B1）—— 按 B0 提取契约（out/contracts/routes.md，99 条）全量注册
// 约定：
//  - 未迁移页面统一指向 PlaceholderView；meta.plannedComponent 记录计划组件（后续批次按此替换）
//  - meta.requiresAuth 标记管理端路由（守卫见 ./guards.js，B1-b 已落地）
//  - 门户端 /:tenantCode 为用户侧；/docs、/404、/service-unavailable 为平台级页面
import { createRouter, createWebHistory } from 'vue-router';
import PlaceholderView from '../views/placeholder/PlaceholderView.vue';
import TenantShell from '../layouts/TenantShell.vue';
import { installRouterGuards } from './guards';

// 占位路由工厂
function ph(plannedComponent, extraMeta = {}) {
  return { component: PlaceholderView, meta: { plannedComponent, ...extraMeta } };
}

const adminChild = (path, name, plannedComponent, extra = {}) => ({
  path,
  name,
  ...ph(plannedComponent, { requiresAuth: true, ...extra })
});

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'PortalHome', ...ph('Home') },
    {
      path: '/docs',
      name: 'PlatformDocsIndex',
      ...ph('PlatformDocsShell'),
      children: [{ path: ':id', name: 'PlatformArticleDetail', ...ph('NoticeDetail') }]
    },
    { path: '/404', name: 'NotFound', ...ph('NotFound') },
    { path: '/service-unavailable', name: 'ServiceUnavailable', ...ph('ServiceUnavailable') },
    // 旧版 /admin/* 直达路径（编译产物中的正则路由）——原行为待 B2 运行态核验，暂回落 404
    { path: '/admin/:pathMatch(.*)*', name: 'LegacyAdminFallback', redirect: '/404' },
    {
      path: '/:tenantCode',
      name: 'TenantShell',
      component: TenantShell,
      children: [
        { path: '', name: 'TenantIndex', redirect: { name: 'PortalHome' } },
        { path: 'article', name: 'PortalArticleList', ...ph('News') },
        { path: 'article/:id', name: 'PortalArticleDetail', ...ph('NoticeDetail') },
        { path: 'events', name: 'PortalEvents', ...ph('Events') },
        { path: 'events/:eventId/games', name: 'PortalEventGamesRedirect', redirect: { name: 'PortalEvents' } },
        { path: 'events/:eventId/games/:gameId', name: 'PortalGameDetail', ...ph('GameDetail') },
        { path: 'teams', name: 'PortalTeams', ...ph('Teams') },
        { path: 'star-players', name: 'PortalStarPlayers', ...ph('StarPlayers') },
        { path: 'teams/:id', name: 'PortalTeamDetail', ...ph('TeamDetail') },
        { path: 'stadiums/:id', name: 'PortalStadiumDetail', ...ph('StadiumDetail') },
        { path: 'players/:id', name: 'PortalPlayerDetail', ...ph('PlayerDetail') },
        { path: 'stats', name: 'PortalStats', ...ph('Stats') },
        { path: 'docs/:id', name: 'PortalAnnouncementDoc', ...ph('AnnouncementDoc') },
        { path: 'account/profile', name: 'PortalAccountProfile', ...ph('AccountProfile') },
        { path: 'account/login', name: 'PortalAccountLogin', ...ph('AccountLogin') },
        { path: 'account/register', name: 'PortalRegister', ...ph('Register') },
        { path: 'account/forgot-password', name: 'PortalForgotPassword', ...ph('ForgotPassword') },
        { path: 'claim/:token', name: 'PortalPlayerClaimInvite', ...ph('PlayerClaimInvite') },
        { path: 'admin/login', name: 'AdminLogin', ...ph('Login', { requiresAuth: false }) },
        {
          path: 'admin',
          name: 'AdminRoot',
          ...ph('AdminLayout', { requiresAuth: true }),
          children: [
            { path: '', redirect: { name: 'AdminDashboard' } },
            adminChild('dashboard', 'AdminDashboard', 'Dashboard'),
            adminChild('users', 'AdminUsers', 'Users'),
            adminChild('roles', 'AdminRoles', 'Roles'),
            adminChild('menus', 'AdminMenus', 'Menus'),
            adminChild('apis', 'AdminApis', 'Apis'),
            adminChild('dict', 'AdminDict', 'Dict'),
            adminChild('announcements', 'AdminAnnouncements', 'Announcements'),
            adminChild('articles', 'AdminArticles', 'Content'),
            adminChild('articles/new', 'AdminArticlesNew', 'ContentEdit'),
            adminChild('articles/:id/edit', 'AdminArticlesEdit', 'ContentEdit'),
            adminChild('content', 'AdminContent', 'Content'),
            adminChild('content/new', 'AdminContentNew', 'ContentEdit'),
            adminChild('content/:id/edit', 'AdminContentEdit', 'ContentEdit'),
            adminChild('config', 'AdminConfig', 'AppConfig'),
            adminChild('resources', 'AdminResources', 'Resources'),
            adminChild('media-icons', 'AdminMediaIcons', 'MediaIcons'),
            adminChild('media-gallery', 'AdminMediaGallery', 'MediaGallery'),
            adminChild('login-logs', 'AdminLoginLogs', 'LoginLogs'),
            adminChild('operation-logs', 'AdminOperationLogs', 'OperationLogs'),
            adminChild('tenants', 'AdminTenants', 'Tenants'),
            adminChild('monitor/data', 'AdminMonitorData', 'DataMonitor'),
            adminChild('monitor/server', 'AdminMonitorServer', 'ServerMonitor'),
            adminChild('monitor/cache', 'AdminMonitorCache', 'CacheMonitor'),
            adminChild('monitor/cache-list', 'AdminMonitorCacheList', 'CacheList'),
            adminChild('monitor/portal-devtools-report', 'AdminMonitorPortalDevtoolsReport', 'PortalDevtoolsReportList'),
            adminChild('monitor/portal-visit-hit', 'AdminMonitorPortalVisitHit', 'PortalVisitHitList'),
            adminChild('monitor/portal-feedback', 'AdminMonitorPortalFeedback', 'PortalFeedbackList'),
            adminChild('monitor/ip-access-policy', 'AdminMonitorIpAccessPolicy', 'IpAccessPolicy'),
            adminChild('ip-location-cache', 'AdminIpLocationCache', 'IpLocationCache'),
            adminChild('leagues', 'AdminLeagues', 'Leagues'),
            adminChild('stadiums/distribution', 'AdminStadiumDistribution', 'StadiumDistribution'),
            adminChild('stadiums', 'AdminStadiums', 'Stadiums'),
            adminChild('teams', 'AdminTeams', 'Teams'),
            adminChild('lineup-templates', 'AdminLineupTemplates', 'LineupTemplates'),
            adminChild('coaches', 'AdminCoaches', 'Coaches'),
            adminChild('players', 'AdminPlayerList', 'PlayerList'),
            adminChild('players/:id', 'AdminPlayerDetail', 'PlayerDetail'),
            adminChild('player-claims', 'AdminPlayerClaimReview', 'PlayerClaimReview'),
            adminChild('teams/:id/players', 'AdminTeamPlayers', 'PlayerList'),
            adminChild('events', 'AdminEvents', 'Events'),
            adminChild('events/:eventId/bracket', 'AdminEventBracket', 'EventBracket'),
            adminChild('events/:eventId/games', 'AdminGames', 'Games'),
            adminChild('events/:eventId/games/new', 'AdminGameNew', 'Games'),
            adminChild('events/:eventId/games/:gameId/edit', 'AdminGameEdit', 'Games'),
            adminChild('events/:eventId/games/:gameId/live', 'AdminGameLiveResume', 'LiveGame'),
            adminChild('events/:eventId/games/:gameId/lineup', 'AdminGameLiveLineupGame', 'LiveGameLineup'),
            adminChild('events/:eventId/games/:gameId/watch', 'AdminGameLiveWatch', 'LiveGameWatch'),
            adminChild('events/:eventId/games/:gameId', 'AdminGameDetail', 'GameDetail'),
            adminChild('events/:eventId/games/live', 'AdminGameLiveLineup', 'LiveGameLineup'),
            adminChild('history-records', 'AdminHistoryRecords', 'HistoryRecordList'),
            adminChild('highlight-moments', 'AdminHighlightMoments', 'HighlightMomentList')
          ]
        }
      ]
    },
    { path: '/:pathMatch(.*)*', name: 'NotFoundCatchAll', redirect: '/404' }
  ]
});

installRouterGuards(router);

export default router;
