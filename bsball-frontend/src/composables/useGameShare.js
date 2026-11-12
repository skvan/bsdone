// 比赛详情分享：规范化分享链接与分享元数据（标题/描述/分享图）
// 与球员分享保持一致的口径：链接始终指向门户比赛详情页，并带 from=share|poster 来源参数。
const PLATFORM_NAME = 'BSD棒垒球数据平台';

function currentOrigin() {
  return typeof location !== 'undefined' && location.origin ? location.origin : '';
}

function toAbsolute(url, origin) {
  if (!url) return '';
  if (/^(?:https?:|data:|blob:)/i.test(url) || url.startsWith('//')) return url;
  return origin + (url.startsWith('/') ? url : '/' + url);
}

function teamLabel(team, fallback) {
  const label = String(team?.name || team?.shortName || '').trim();
  return label || fallback;
}

export function gameDateTime(game) {
  const gameday = String(game?.gameday || '').trim();
  const gameTime = String(game?.gameTime || '').trim();
  const match = gameTime.match(/(\d{1,2}):(\d{2})/);
  const hhmm = match ? `${match[1].padStart(2, '0')}:${match[2]}` : '';
  return [gameday, hhmm].filter(Boolean).join(' ');
}

/**
 * 门户比赛详情规范分享链接（href 已含部署 base）
 * @param {object} router vue-router 实例
 * @param {number|string} gameId 比赛 ID
 * @param {number|string} eventId 赛事 ID
 * @param {'share'|'poster'} source 分享来源
 * @param {string} tenantCode 租户编码
 */
export function buildGameShareUrl(router, gameId, eventId, source, tenantCode) {
  const href = router.resolve({
    name: 'PortalGameDetail',
    params: {
      tenantCode: String(tenantCode || ''),
      eventId: String(eventId),
      gameId: String(gameId)
    },
    query: { from: source }
  }).href;
  return currentOrigin() + href;
}

/**
 * 组装比赛分享元数据：标题=客队 比分 : 主队（未开赛为 vs）；desc=赛事·场次·日期·平台；imgUrl=球队 logo→平台 logo
 * @param {object} game 比赛对象（GameResponseDTO）
 * @param {object} homeTeam 主队对象
 * @param {object} awayTeam 客队对象
 * @param {object} event 赛事对象
 * @param {object} options 可注入 origin/resolveAsset/platformName 便于测试
 */
export function buildGameShareMeta(game, homeTeam, awayTeam, event, options = {}) {
  if (!game) return null;
  const origin = options.origin != null ? options.origin : currentOrigin();
  const resolveAsset = options.resolveAsset || ((url) => url);
  const platformName = options.platformName || PLATFORM_NAME;
  const homeName = teamLabel(homeTeam, options.fallbackHome || '主队');
  const awayName = teamLabel(awayTeam, options.fallbackAway || '客队');
  const status = String(game.status || '');
  const hasScore =
    (status === 'final' || status === 'live') && game.homeScore != null && game.awayScore != null;
  const title = hasScore
    ? `${awayName} ${game.awayScore} : ${game.homeScore} ${homeName}`
    : `${awayName} vs ${homeName}`;
  const descParts = [];
  if (event?.name) descParts.push(event.name);
  if (game.gameNumber != null) descParts.push(`Game ${game.gameNumber}`);
  const dateTime = gameDateTime(game);
  if (dateTime) descParts.push(dateTime);
  descParts.push(platformName);
  const rawImg = homeTeam?.logo || awayTeam?.logo || '';
  const imgUrl = rawImg ? toAbsolute(resolveAsset(rawImg), origin) : origin + '/static/logo/logo2.png';
  return { title, desc: descParts.join(' · '), imgUrl };
}
