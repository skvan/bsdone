// 整册电子秩序册（#310 延伸）：页面数据装配纯函数
// 口径：整册仅收录「已确认」球队，且仅展示**在役**球员（currentInTeam=false 的离队球员不展示）；
// 名单以球队行列的 rosterSubmittedAt 判定是否已提交，已提交时逐队取 roster 行
// （teamId → { rows } | { failed: true }），失败单列占位不阻断整册。
// 运行单测：在 bsball-frontend 目录执行  node --test "tests/*.test.mjs"

// 在役口径：显式 false 才视为离队（缺省/旧数据视为在役，避免误过滤）
const isActive = (row) => row?.currentInTeam !== false;

/** 仅保留已确认球队并按原列表顺序生成整册分节（名单行已过滤为在役）。 */
export function buildOrderBookSections(teams, rosterMap = {}) {
  const list = Array.isArray(teams) ? teams : [];
  return list
    .filter((team) => team && team.status === 'confirmed')
    .map((team) => {
      const entry = rosterMap?.[team.teamId];
      const submitted = team.rosterSubmittedAt != null;
      const rows = submitted && Array.isArray(entry?.rows) ? entry.rows.filter(isActive) : [];
      return {
        teamId: team.teamId,
        teamName: team.teamName ?? '',
        teamLogo: team.teamLogo ?? null,
        rosterLocked: !!team.rosterLocked,
        playerCount: rows.length,
        submitted,
        loadFailed: submitted && entry?.failed === true,
        rows
      };
    });
}

/** 整册汇总：球队数 / 已锁定数 / 在役报名球员总数（与卡片、列表展示口径一致）。 */
export function summarizeOrderBook(sections) {
  const list = Array.isArray(sections) ? sections : [];
  return {
    teams: list.length,
    locked: list.filter((s) => s.rosterLocked).length,
    players: list.reduce((sum, s) => sum + (Number(s.playerCount) || 0), 0)
  };
}

// 去重追加（保持出现顺序）：多队报名时背号/位置/队名各只保留一份
const pushUnique = (list, value) => {
  if (value && !list.includes(value)) list.push(value);
};

/**
 * 列表视图（不按球队聚合）：跨队按球员合并（同一球员多队报名为一行，球队列完整列出全部队名、
 * 背号去重后以 " / " 拼接），按姓名排序；入参为已过滤在役的分节。
 */
export function buildFlatOrderBookPlayers(sections) {
  const byPlayer = new Map();
  for (const s of Array.isArray(sections) ? sections : []) {
    for (const row of Array.isArray(s?.rows) ? s.rows : []) {
      const id = row?.playerId;
      if (id == null) continue;
      let agg = byPlayer.get(id);
      if (!agg) {
        agg = { playerId: id, name: row.name ?? '', avatar: row.avatar ?? null, numbers: [], positions: [], teams: [] };
        byPlayer.set(id, agg);
      }
      pushUnique(agg.numbers, String(row.number ?? '').trim());
      for (const pos of Array.isArray(row.positions) ? row.positions : []) {
        pushUnique(agg.positions, pos);
      }
      pushUnique(agg.teams, s.teamName || (s.teamId != null ? `ID:${s.teamId}` : ''));
    }
  }
  return [...byPlayer.values()]
    .map((p) => ({
      ...p,
      numberText: p.numbers.join(' / '),
      positionText: p.positions.join(' / '),
      teamText: p.teams.join('、')
    }))
    .sort((a, b) => String(a.name ?? '').localeCompare(String(b.name ?? ''), 'zh-Hans-CN'));
}
