// 结果录入：投手行“转记”合并规则。
// 目标投手已有值优先；仅把原投手非零、目标为零的投球/守备数据并入目标，并清零原投手行。
// R/ER/UER/PENDING 属 ER 引擎，不在本工具内搬移。
export const PITCHER_TRANSFER_FIELDS = Object.freeze([
  'ip',
  'np',
  'pitAb',
  'pitchH',
  'pitchDoubles',
  'pitchTriples',
  'pitchBbHp',
  'pitchSo',
  'pitchHr',
  'pitchInsideParkHr',
  'pitchSh',
  'pitchSf',
  'pitchPa',
  'pitchBf',
  'pitchBb',
  'pitchIbb',
  'pitchHbp',
  'pitchIo',
  'pickoffAttempts',
  'wp',
  'bk',
  'go',
  'fo',
  'gs',
  'svo',
  'cg',
  'pg',
  'w',
  'l',
  'sv',
  'hld'
]);

// 守备字段单独存放在 D（球员守备统计对象），与 H（投球统计对象）分开合并。
export const PITCHER_FIELDING_TRANSFER_FIELDS = Object.freeze([
  'po',
  'a',
  'e',
  'tc',
  'fieldingGs',
  'defInn',
  'dp',
  'pb',
  'catcherSb',
  'catcherCs'
]);

function mergeFields(source, target, fields) {
  if (!source || !target || source === target) {
    return { moved: 0, cleared: false };
  }
  let moved = 0;
  let cleared = false;
  for (const key of fields) {
    const sourceValue = Number(source[key]) || 0;
    if (sourceValue !== 0) cleared = true;
    const targetValue = Number(target[key]) || 0;
    if (targetValue === 0 && sourceValue !== 0) {
      target[key] = source[key];
      moved += 1;
    }
    source[key] = 0;
  }
  return { moved, cleared };
}

export function mergePitcherStats(source, target) {
  return mergeFields(source, target, PITCHER_TRANSFER_FIELDS);
}

export function mergePitcherFieldingStats(source, target) {
  return mergeFields(source, target, PITCHER_FIELDING_TRANSFER_FIELDS);
}
