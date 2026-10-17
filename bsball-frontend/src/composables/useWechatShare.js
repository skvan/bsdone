import { watch } from 'vue';
import { initWechatShare, updateShareData } from '../utils/wechatJssdk.js';

const PLATFORM_NAME = 'BSD棒垒球数据平台';
const SHARE_ROUTE_NAME = 'PortalPlayerDetail';

function currentOrigin() {
  return typeof location !== 'undefined' && location.origin ? location.origin : '';
}

/** 门户球员页规范分享链接（任何上下文生成均指向门户；href 已含部署 base） */
export function buildPlayerShareUrl(router, playerId, source, tenantCode) {
  const href = router.resolve({
    name: SHARE_ROUTE_NAME,
    params: { tenantCode: String(tenantCode || ''), id: String(playerId) },
    query: { from: source }
  }).href;
  return currentOrigin() + href;
}

function toAbsolute(url, origin) {
  if (!url) return '';
  // 已是绝对/内联/协议相对 URL：原样返回，不拼 origin
  if (/^(?:https?:|data:|blob:)/i.test(url) || url.startsWith('//')) return url;
  return origin + (url.startsWith('/') ? url : '/' + url);
}

/** 数据摘要：击球员优先（打击率/本垒打/打点），否则投手（防御率/夺三振/胜投），最多 3 项 */
function buildStatSummary(stats) {
  const batting = stats && stats.batting;
  const pitching = stats && stats.pitching;
  const items = [];
  if (batting && batting.ab > 0) {
    if (batting.avg) items.push(`打击率 ${batting.avg}`);
    if (batting.hr > 0) items.push(`${batting.hr}HR`);
    if (batting.rbi > 0) items.push(`${batting.rbi}打点`);
  } else if (pitching && (pitching.ip > 0 || pitching.gp > 0)) {
    if (pitching.era) items.push(`防御率 ${pitching.era}`);
    if (pitching.pitchSo > 0) items.push(`${pitching.pitchSo}K`);
    if (pitching.w > 0) items.push(`${pitching.w}胜`);
  }
  return items.slice(0, 3).join(' · ');
}

/** 组装分享元数据（标题=球员名+数据摘要；desc=球队·联赛·平台；imgUrl=人物图→平台 logo） */
export function buildPlayerShareMeta(player, team, league, stats, options = {}) {
  if (!player || !player.name) return null;
  const origin = options.origin != null ? options.origin : currentOrigin();
  const resolveAsset = options.resolveAsset || ((u) => u);
  const platformName = options.platformName || PLATFORM_NAME;
  const summary = buildStatSummary(stats);
  const title = summary ? `${player.name} | ${summary}` : player.name;
  const desc = [team && team.name, league && league.name, platformName].filter((x) => x).join(' · ');
  const rawImg = typeof player.avatar === 'string' ? player.avatar.trim() : '';
  const imgUrl = rawImg ? toAbsolute(resolveAsset(rawImg), origin) : origin + '/static/logo/logo2.png';
  return { title, desc, imgUrl };
}

/** 分享元数据完整键：title/desc/imgUrl 任一变化都要重配分享数据 */
function metaKeyOf(meta) {
  return `${meta.title}\u0000${meta.desc || ''}\u0000${meta.imgUrl || ''}`;
}

/** 微信内自动配置分享：链接变化→完整初始化；同链接元数据变化→同页更新；初始化期间元数据变化则在配置成功后补发 */
export function useWechatShare(metaRef, deps = {}) {
  let lastLink = '';
  let lastMetaKey = '';
  let latestMeta = null;
  let initToken = 0;
  return watch(metaRef, (meta) => {
    if (!meta || !meta.link || !meta.title) return;
    latestMeta = meta;
    if (meta.link !== lastLink) {
      const link = meta.link;
      const initializedKey = metaKeyOf(meta);
      const token = ++initToken;
      lastLink = link;
      lastMetaKey = initializedKey;
      initWechatShare(meta, deps).then(() => {
        // 签名/config 期间元数据已更新：配置成功后补发一次最新分享数据
        if (token !== initToken || !latestMeta || latestMeta.link !== link) return;
        if (metaKeyOf(latestMeta) === initializedKey) return;
        lastMetaKey = metaKeyOf(latestMeta);
        updateShareData(latestMeta, deps);
      });
      return;
    }
    if (metaKeyOf(meta) !== lastMetaKey) {
      lastMetaKey = metaKeyOf(meta);
      updateShareData(meta, deps);
    }
  }, { immediate: true });
}
