// 路由骨架（B1）—— 按 B0 提取契约（out/contracts/routes.md，99 条）全量注册
// 约定：
//  - 未迁移页面统一指向 PlaceholderView；meta.plannedComponent 记录计划组件（后续批次按此替换）
//  - meta.requiresAuth 标记管理端路由（守卫见 ./guards.js，B1-b 已落地）
//  - 门户端 /:tenantCode 为用户侧；/docs、/404、/service-unavailable 为平台级页面
import { createRouter, createWebHistory } from 'vue-router';
import PlaceholderView from '../views/placeholder/PlaceholderView.vue';
import TenantShell from '../layouts/TenantShell.vue';
import AdminLayout from '../components/admin/AdminLayout.js';
import AccountLogin from '../views/auth/AccountLogin.js';
import Register from '../views/auth/Register.js';
import ForgotPassword from '../views/auth/ForgotPassword.js';
import PlayerClaimInvite from '../views/auth/PlayerClaimInvite.js';
import Login from '../views/auth/Login.js';
import Users from '../views/admin/Users.js';
import Roles from '../views/admin/Roles.js';
import Menus from '../views/admin/Menus.js';
import Apis from '../views/admin/Apis.js';
import Dict from '../views/admin/Dict.js';
import Announcements from '../views/admin/Announcements.js';
import Content from '../views/admin/Content.js';
import ContentEdit from '../views/admin/ContentEdit.js';
import Resources from '../views/admin/Resources.js';
import MediaIcons from '../views/admin/MediaIcons.js';
import MediaGallery from '../views/admin/MediaGallery.js';
import LoginLogs from '../views/admin/LoginLogs.js';
import OperationLogs from '../views/admin/OperationLogs.js';
import PortalDevtoolsReportList from '../views/admin/PortalDevtoolsReportList.js';
import PortalVisitHitList from '../views/admin/PortalVisitHitList.js';
import PortalFeedbackList from '../views/admin/PortalFeedbackList.js';
import IpLocationCache from '../views/admin/IpLocationCache.js';
import Tenants from '../views/admin/Tenants.js';
import IpAccessPolicy from '../views/admin/IpAccessPolicy.js';
import Leagues from '../views/admin/Leagues.js';
import Stadiums from '../views/admin/Stadiums.js';
import StadiumDistribution from '../views/admin/StadiumDistribution.js';
import Teams from '../views/admin/Teams.js';
import Coaches from '../views/admin/Coaches.js';
import PlayerList from '../views/admin/PlayerList.js';
import PlayerClaimReview from '../views/admin/PlayerClaimReview.js';
import { installRouterGuards } from './guards';
import { ROUTE_TITLES, ROUTE_TITLE_KEYS } from './legacy-meta';

// 占位路由工厂
function ph(plannedComponent, extraMeta = {}) {
  return { component: PlaceholderView, meta: { plannedComponent, ...extraMeta } };
}

const adminChild = (path, name, plannedComponent, extra = {}) => ({
  path,
  name,
  ...ph(plannedComponent, { requiresAuth: true, ...extra })
});

const routes = [
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
        { path: 'account/login', name: 'PortalAccountLogin', component: AccountLogin },
        { path: 'account/register', name: 'PortalRegister', component: Register },
        { path: 'account/forgot-password', name: 'PortalForgotPassword', component: ForgotPassword },
        { path: 'claim/:token', name: 'PortalPlayerClaimInvite', component: PlayerClaimInvite },
        { path: 'admin/login', name: 'AdminLogin', component: Login, meta: { requiresAuth: false } },
        {
          path: 'admin',
          name: 'AdminRoot',
          component: AdminLayout,
          meta: { requiresAuth: true },
          children: [
            { path: '', redirect: { name: 'AdminDashboard' } },
            adminChild('dashboard', 'AdminDashboard', 'Dashboard'),
            { path: 'users', name: 'AdminUsers', component: Users },
            { path: 'roles', name: 'AdminRoles', component: Roles },
            { path: 'menus', name: 'AdminMenus', component: Menus },
            { path: 'apis', name: 'AdminApis', component: Apis },
            { path: 'dict', name: 'AdminDict', component: Dict },
            { path: 'announcements', name: 'AdminAnnouncements', component: Announcements },
            { path: 'articles', name: 'AdminArticles', component: Content },
            { path: 'articles/new', name: 'AdminArticlesNew', component: ContentEdit },
            { path: 'articles/:id/edit', name: 'AdminArticlesEdit', component: ContentEdit },
            { path: 'content', name: 'AdminContent', component: Content },
            { path: 'content/new', name: 'AdminContentNew', component: ContentEdit },
            { path: 'content/:id/edit', name: 'AdminContentEdit', component: ContentEdit },
            adminChild('config', 'AdminConfig', 'AppConfig'),
            { path: 'resources', name: 'AdminResources', component: Resources },
            { path: 'media-icons', name: 'AdminMediaIcons', component: MediaIcons },
            { path: 'media-gallery', name: 'AdminMediaGallery', component: MediaGallery },
            { path: 'login-logs', name: 'AdminLoginLogs', component: LoginLogs },
            { path: 'operation-logs', name: 'AdminOperationLogs', component: OperationLogs },
            { path: 'tenants', name: 'AdminTenants', component: Tenants },
            adminChild('monitor/data', 'AdminMonitorData', 'DataMonitor'),
            adminChild('monitor/server', 'AdminMonitorServer', 'ServerMonitor'),
            adminChild('monitor/cache', 'AdminMonitorCache', 'CacheMonitor'),
            adminChild('monitor/cache-list', 'AdminMonitorCacheList', 'CacheList'),
            { path: 'monitor/portal-devtools-report', name: 'AdminMonitorPortalDevtoolsReport', component: PortalDevtoolsReportList },
            { path: 'monitor/portal-visit-hit', name: 'AdminMonitorPortalVisitHit', component: PortalVisitHitList },
            { path: 'monitor/portal-feedback', name: 'AdminMonitorPortalFeedback', component: PortalFeedbackList },
            { path: 'monitor/ip-access-policy', name: 'AdminMonitorIpAccessPolicy', component: IpAccessPolicy },
            { path: 'ip-location-cache', name: 'AdminIpLocationCache', component: IpLocationCache },
            { path: 'leagues', name: 'AdminLeagues', component: Leagues },
            { path: 'stadiums/distribution', name: 'AdminStadiumDistribution', component: StadiumDistribution },
            { path: 'stadiums', name: 'AdminStadiums', component: Stadiums },
            { path: 'teams', name: 'AdminTeams', component: Teams },
            adminChild('lineup-templates', 'AdminLineupTemplates', 'LineupTemplates'),
            { path: 'coaches', name: 'AdminCoaches', component: Coaches },
            { path: 'players', name: 'AdminPlayerList', component: PlayerList },
            adminChild('players/:id', 'AdminPlayerDetail', 'PlayerDetail'),
            { path: 'player-claims', name: 'AdminPlayerClaimReview', component: PlayerClaimReview },
            { path: 'teams/:id/players', name: 'AdminTeamPlayers', component: PlayerList },
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
];

// 回填原版 meta 标题（页面标题/标签页标题/面包屑末级）
// 必须在 createRouter 之前执行：matcher 对路由记录做浅拷贝，meta 若原本为 undefined，
// 在 createRouter 之后补 meta 不会反映到运行时 route.meta（B1-c 冒烟曾因 CSS 文本假阳性未暴露）
function applyLegacyMeta(records) {
  for (const record of records || []) {
    if (record.name) {
      record.meta = record.meta || {};
      if (record.meta.title == null && ROUTE_TITLES[record.name]) record.meta.title = ROUTE_TITLES[record.name];
      if (record.meta.titleKey == null && ROUTE_TITLE_KEYS[record.name]) record.meta.titleKey = ROUTE_TITLE_KEYS[record.name];
    }
    if (record.children) applyLegacyMeta(record.children);
  }
}
applyLegacyMeta(routes);

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

installRouterGuards(router);

export default router;
