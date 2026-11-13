const UNKNOWN_TEAM_KEY = '__unknown__';

const SIDE_LABELS = {
  away: '客队',
  home: '主队',
  other: '球队'
};

/** 投手 R/ER 表格：客队在前、主队在后；队内按登板序升序，空值最后。 */
export function buildPitcherRunTable(rows, options = {}) {
  const list = (Array.isArray(rows) ? rows : []).filter(Boolean);
  const awayKey = normalizeTeamKey(options.awayTeamId);
  const homeKey = normalizeTeamKey(options.homeTeamId);
  const byTeam = groupRowsByTeam(list);
  const teamKeys = orderedTeamKeys(byTeam, awayKey, homeKey);
  const groups = teamKeys.map((key) => toGroup(
    key,
    byTeam.get(key),
    awayKey,
    homeKey,
    options.teamNameById
  ));
  return { groups, tableRows: flattenGroups(groups) };
}

function groupRowsByTeam(rows) {
  const byTeam = new Map();
  for (const row of rows) {
    const key = normalizeTeamKey(row.teamId) ?? UNKNOWN_TEAM_KEY;
    if (!byTeam.has(key)) byTeam.set(key, []);
    byTeam.get(key).push(row);
  }
  return byTeam;
}

function orderedTeamKeys(byTeam, awayKey, homeKey) {
  const keys = [];
  if (awayKey && byTeam.has(awayKey)) keys.push(awayKey);
  if (homeKey && homeKey !== awayKey && byTeam.has(homeKey)) keys.push(homeKey);
  const rest = [...byTeam.keys()].filter((key) => !keys.includes(key));
  rest.sort(compareTeamKeys);
  return [...keys, ...rest];
}

function compareTeamKeys(left, right) {
  if (left === UNKNOWN_TEAM_KEY) return 1;
  if (right === UNKNOWN_TEAM_KEY) return -1;
  const leftNumber = Number(left);
  const rightNumber = Number(right);
  if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) {
    return leftNumber - rightNumber;
  }
  return String(left).localeCompare(String(right));
}

function toGroup(key, members, awayKey, homeKey, teamNameById) {
  const side = key === awayKey ? 'away' : key === homeKey ? 'home' : 'other';
  const first = members[0];
  return {
    teamId: key === UNKNOWN_TEAM_KEY ? null : first?.teamId ?? null,
    key,
    side,
    teamLabel: SIDE_LABELS[side],
    teamName: key === UNKNOWN_TEAM_KEY
      ? '待定'
      : resolveTeamName(teamNameById, key),
    rows: members.slice().sort(comparePitcherRows)
  };
}

function flattenGroups(groups) {
  const tableRows = [];
  for (const group of groups) {
    group.rows.forEach((row, index) => {
      tableRows.push({
        ...row,
        _teamSide: group.side,
        _teamLabel: group.teamLabel,
        _teamName: group.teamName,
        _teamRowspan: index === 0 ? group.rows.length : 0,
        _teamStart: index === 0,
        _rowKey: `${group.key}-${row.playerId ?? index}-${index}`
      });
    });
  }
  return tableRows;
}

function comparePitcherRows(left, right) {
  const leftOrder = positiveOrder(left.pitcherOrder);
  const rightOrder = positiveOrder(right.pitcherOrder);
  if (leftOrder != null && rightOrder != null && leftOrder !== rightOrder) {
    return leftOrder - rightOrder;
  }
  if (leftOrder != null && rightOrder == null) return -1;
  if (leftOrder == null && rightOrder != null) return 1;
  return (Number(left.playerId) || 0) - (Number(right.playerId) || 0);
}

function positiveOrder(value) {
  const order = Number(value);
  return Number.isFinite(order) && order > 0 ? order : null;
}

function resolveTeamName(teamNameById, teamKey) {
  if (!teamNameById) return '';
  const name = teamNameById instanceof Map
    ? teamNameById.get(teamKey)
    : teamNameById[teamKey];
  return name == null ? '' : String(name);
}

function normalizeTeamKey(value) {
  if (value === null || value === undefined || value === '') return null;
  return String(value);
}
