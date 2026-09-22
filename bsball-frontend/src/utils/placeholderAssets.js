// 占位资源与图片兜底 —— 行为移植自编译产物 placeholder chunk
export const DEFAULT_TEAM_SVG_PATH = '/bs-ball/default-team.svg';
export const DEFAULT_AVATAR_SVG_PATH = '/bs-ball/default-avatar.svg';

// 灰底圆（头像加载失败兜底）
const GRAY_CIRCLE_SVG =
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><circle cx="32" cy="32" r="32" fill="#e8e8e8"/></svg>';
// 人形占位（队标缺省时的最终兜底）
const PERSON_SVG =
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><circle cx="32" cy="32" r="32" fill="#e0e0e0"/><circle cx="32" cy="24" r="10" fill="#9e9e9e"/><path fill="#9e9e9e" d="M12 54c0-11 8.954-20 20-20s20 9 20 20v2H12v-2z"/></svg>';

const GRAY_CIRCLE_DATA_URI = 'data:image/svg+xml,' + encodeURIComponent(GRAY_CIRCLE_SVG);
const PERSON_DATA_URI = 'data:image/svg+xml,' + encodeURIComponent(PERSON_SVG);

let defaultTeamAbsUrl = null;
function isDefaultTeamImage(img) {
  try {
    if (defaultTeamAbsUrl == null) defaultTeamAbsUrl = new URL(DEFAULT_TEAM_SVG_PATH, window.location.href).href;
    return img.src === defaultTeamAbsUrl;
  } catch {
    return false;
  }
}

// 队标/文字 Logo <img @error>：已是默认图则换灰底圆，否则回落默认队标
export function onTeamLogoError(event) {
  const img = event.target;
  if (!img || img.src.startsWith('data:')) return;
  if (isDefaultTeamImage(img)) {
    img.src = GRAY_CIRCLE_DATA_URI;
    return;
  }
  img.src = DEFAULT_TEAM_SVG_PATH;
}

// 头像 <img @error>：回落人形占位
export function onAvatarError(event) {
  const img = event.target;
  if (!img || img.src.startsWith('data:')) return;
  img.src = PERSON_DATA_URI;
}
