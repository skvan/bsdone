// H33：比赛详情/观赛 playersMap 按队构建——复合键 `teamId:playerId`（严格按队）优先，单键兼容旧路径
// 与 GameDetailContent 的 pm() 查找约定保持同步（键格式 `${teamId}:${playerId}`，同一文件内常数/约定勿改单侧）
export function buildTeamPlayersMap(home, homeTeamId, away, awayTeamId) {
  const map = {};
  const add = (rows, teamId) => {
    for (const row of rows ?? []) {
      if (row?.id == null || teamId == null) continue;
      map[`${teamId}:${row.id}`] = row;
      map[row.id] = row;
    }
  };
  add(home, homeTeamId);
  add(away, awayTeamId);
  return map;
}

// 补集：stats 行中出现但不在两队当前名册的球员——按注册段逐段生成按队副本（current 段优先，无全局回退）
export function addTeamEntryPlayers(map, players) {
  for (const player of players ?? []) {
    if (player?.id == null) continue;
    map[player.id] = player;
    for (const entry of player.teamEntries ?? []) {
      if (entry?.teamId == null) continue;
      const key = `${entry.teamId}:${player.id}`;
      if (map[key] == null || entry.current) map[key] = { ...player, number: entry.number ?? null };
    }
  }
  return map;
}
