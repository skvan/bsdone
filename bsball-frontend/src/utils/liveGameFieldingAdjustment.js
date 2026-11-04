export const FIELDING_ADJUSTMENT_POSITIONS = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'];

const ERROR_CODES = new Set(['E', 'ADVANCE_FE', 'DK3_REACH_E']);
const BASE_LABELS = ['一', '二', '三'];

/** 赛后补记只针对“失误导致出局没发生”的 Play；捕逸、暴投不算守备失误。 */
export function isFieldingErrorPlay(event) {
  if (!event) return false;
  const code = String(event.resultCode ?? '').toUpperCase();
  const text = String(event.resultText ?? '');
  return ERROR_CODES.has(code) || code.startsWith('ERROR') || text.includes('失误');
}

export function defensiveSide(event) {
  return event?.half === 'top' ? 'home' : 'away';
}

export function describeBaseSituation(bases = {}) {
  const occupied = [1, 2, 3].filter((base) => bases?.[base]);
  if (occupied.length === 0) return '垒上无人';
  if (occupied.length === 3) return '满垒';
  if (occupied.length === 1) return `${BASE_LABELS[occupied[0] - 1]}垒有人`;
  return `${occupied.map((base) => BASE_LABELS[base - 1]).join('、')}垒有人`;
}

/** 合并球员当前守位与注册守位，去重；DH/PH/EP 不是可补记的守备位置。 */
export function playerPositionOptions(player) {
  if (!player) return [];
  const source = [player.position, ...(Array.isArray(player.positions) ? player.positions : [])];
  const options = [];
  for (const raw of source) {
    const position = String(raw ?? '').trim().toUpperCase();
    if (!FIELDING_ADJUSTMENT_POSITIONS.includes(position)) continue;
    if (!options.includes(position)) options.push(position);
  }
  return options;
}

export function buildFieldingAdjustmentCandidates({
  playEvents = [],
  homeLineup = [],
  awayLineup = [],
  homeBench = [],
  awayBench = [],
  homeFieldingPitcherRow = null,
  awayFieldingPitcherRow = null,
  homeRoster = [],
  awayRoster = [],
  adjustments = []
} = {}) {
  const playersBySide = {
    home: uniquePlayers([...homeLineup, ...homeBench, homeFieldingPitcherRow, ...homeRoster]),
    away: uniquePlayers([...awayLineup, ...awayBench, awayFieldingPitcherRow, ...awayRoster])
  };
  const adjustmentsBySource = new Map();
  for (const adjustment of adjustments) {
    const key = String(adjustment?.sourceEventId ?? '');
    if (!key) continue;
    if (!adjustmentsBySource.has(key)) adjustmentsBySource.set(key, []);
    adjustmentsBySource.get(key).push(adjustment);
  }

  return (Array.isArray(playEvents) ? playEvents : [])
    .filter(isFieldingErrorPlay)
    .filter((event) => event?.id != null && String(event.id).trim() !== '')
    .map((event) => {
      const side = defensiveSide(event);
      return {
        event,
        sourceEventId: String(event.id),
        side,
        inningLabel: `${event.inning}${event.half === 'top' ? '上' : '下'} #${event.sequence}`,
        outsBefore: Number(event.outsBefore ?? 0),
        baseLabel: describeBaseSituation(event.basesBefore),
        resultText: event.resultText ?? '',
        players: playersBySide[side],
        adjustments: adjustmentsBySource.get(String(event.id)) ?? []
      };
    });
}

function uniquePlayers(players) {
  const seen = new Set();
  const result = [];
  for (const player of players) {
    if (!player) continue;
    const id = Number(player.id);
    if (!Number.isInteger(id) || id <= 0 || seen.has(id)) continue;
    seen.add(id);
    result.push(player);
  }
  return result;
}
