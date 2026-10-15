// 整册电子秩序册（#310 延伸）：页面数据装配纯函数
// 口径：整册仅收录「已确认」球队；名单以球队行列的 rosterSubmittedAt 判定是否已提交，
// 已提交时逐队取 roster 行（teamId → { rows } | { failed: true }），失败单列占位不阻断整册。
// 运行单测：在 bsball-frontend 目录执行  node --test "tests/*.test.mjs"

/** 仅保留已确认球队并按原列表顺序生成整册分节。 */
export function buildOrderBookSections(teams, rosterMap = {}) {
  const list = Array.isArray(teams) ? teams : [];
  return list
    .filter((team) => team && team.status === 'confirmed')
    .map((team) => {
      const entry = rosterMap?.[team.teamId];
      const submitted = team.rosterSubmittedAt != null;
      return {
        teamId: team.teamId,
        teamName: team.teamName ?? '',
        teamLogo: team.teamLogo ?? null,
        rosterLocked: !!team.rosterLocked,
        playerCount: Number(team.playerCount ?? 0),
        submitted,
        loadFailed: submitted && entry?.failed === true,
        rows: submitted && Array.isArray(entry?.rows) ? entry.rows : []
      };
    });
}

/** 整册汇总：球队数 / 已锁定数 / 报名球员总数（以球队行的名单人数为准）。 */
export function summarizeOrderBook(sections) {
  const list = Array.isArray(sections) ? sections : [];
  return {
    teams: list.length,
    locked: list.filter((s) => s.rosterLocked).length,
    players: list.reduce((sum, s) => sum + (Number(s.playerCount) || 0), 0)
  };
}
