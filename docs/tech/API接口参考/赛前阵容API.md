# 赛前阵容 API

## 简介
赛前阵容提交（电子打序单）接口：球队负责人查看本队未开始比赛、读取单场编辑上下文、保存本队阵容；办赛方读取一场比赛双方已提交的阵容。所有响应使用统一 `Result<T>` 包装，写守卫与状态/名单校验在服务层。

## 球队侧

### GET /team/{teamId}/lineup-games?eventId={eventId}
- 守卫：`assertCanManageTeam(teamId)`；球队须已确认参加该赛事。
- 返回该赛事下本队 `scheduled/postponed` 比赛及阵容保存状态。
- 响应 `TeamLineupGameListDto`：
```json
{
  "eventId": 700,
  "eventName": "春季联赛",
  "season": "2026",
  "gameMode": "BASEBALL",
  "games": [{
    "gameId": 900,
    "gameNumber": 3,
    "gameday": "2026-11-15",
    "gameTime": "2026-11-15 14:00:00",
    "venue": "天母棒球场",
    "status": "scheduled",
    "side": "home",
    "opponentTeamId": 200,
    "opponentName": "猛虎队",
    "hasSubmission": true,
    "lineupUpdatedAt": "2026-11-11T18:20:00",
    "complete": true
  }]
}
```

### GET /team/{teamId}/lineup-games/{gameId}
- 守卫：`assertCanManageTeam(teamId)`；比赛须为本队参加且未开始。
- 只返回本队提交，不返回对手阵容。
- 响应 `TeamLineupContextDto`：比赛/赛事信息、`specialRules`（extraBatter/ohtaniRule 等）、本队 `teamId/teamName/teamLogo`、`side`、对手、`rosterRestricted/rosterCount`、`lineupJson`、`lineupUpdatedAt`、`complete`。

### PUT /team/{teamId}/lineup-games/{gameId}
- 守卫：`assertCanManageTeam(teamId)`；比赛须为本队参加且未开始；球队须已确认报名。
- 请求体：
```json
{
  "lineupJson": "{\"v\":1,\"gameId\":900,\"teamId\":100,\"lineup\":[...],\"bench\":[...]}",
  "complete": false
}
```
- 校验：JSON v:1、gameId/teamId 匹配、打线人数上限（棒球 12 / 垒球 11）、替补最多 60、同一球员不重复、阵容球员属于报名名单。
- 行为：按 `(gameId, teamId)` upsert，允许草稿不完整；`live/final/cancelled` 返回 400。
- 响应：`{ "saved": true }`

## 办赛方侧

### GET /game/{gameId}/lineup-submissions
- 守卫：`assertCanManageGame(gameId)`。
- 返回该场仍在主客队中的球队提交，按提交记录顺序；无提交返回空数组。
- 响应 `GameLineupSubmissionDto[]`：
```json
[{
  "teamId": 100,
  "teamName": "雷霆队",
  "teamLogo": "https://...",
  "side": "home",
  "lineupJson": "{\"v\":1,...}",
  "updatedAt": "2026-11-11T18:20:00",
  "complete": true
}]
```

## 错误码
- 400：比赛已开始/已结束、阵容格式错误、人数超限、重复球员、名单外球员、赛事/比赛未关联。
- 403：无权管理球队/比赛、球队未确认报名、比赛不是本队参加。
- 404：球队/赛事/比赛不存在或跨租户不可见、球队未参加赛事。

## 前端入口
- 球队：球队管理 → 赛事报名 → 行内「赛前阵容」→ 比赛列表 → 编辑阵容。
- 办赛方：赛程列表 → 该场比赛行内「继续录入」→ 阵容页自动/手动载入球队提交。
