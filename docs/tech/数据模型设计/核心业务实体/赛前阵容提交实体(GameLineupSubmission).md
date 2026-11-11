# 赛前阵容提交实体（GameLineupSubmission）

## 简介
本文件说明赛前阵容提交（电子打序单）实体 `GameLineupSubmission`。它按“一场比赛每队一条”保存球队负责人赛前编辑的先发打线、守位、替补席与先发投手；办赛方实时录入页读取后作为赛前来源，开始录入后仍以 `bs_game.live_snapshot_json` 为权威数据。

## 项目结构
- 实体：`com.bsball.model.entity.GameLineupSubmission`
- 仓储：`com.bsball.repository.GameLineupSubmissionRepository`
- 服务：`com.bsball.service.TeamGameLineupService`
- 接口：`com.bsball.api.TeamGameLineupApi`
- 迁移：`db/migration/postgresql/V20261111150000__game_lineup_submission.sql`

## 表结构（bs_game_lineup_submission）
- 归属：`tenant_id`、`event_id`、`game_id`、`team_id`。
- 数据：`lineup_json`（TEXT，本队阵容 JSON）、`complete`（前端按现有阵容校验计算的完整标记，展示用）。
- 审计：继承 `BaseEntity` 的 `created_*` / `updated_*` / `deleted_*`。
- 唯一：`(game_id, team_id) WHERE deleted_at IS NULL`。
- 辅助索引：`(game_id)`、`(event_id, team_id)`、`(tenant_id)`（均带软删过滤）。

## lineup_json 结构（v:1）
```json
{
  "v": 1,
  "gameId": 900,
  "teamId": 100,
  "gameMode": "BASEBALL",
  "complete": false,
  "lineup": [{ "id": 5, "name": "张三", "number": "7", "position": "CF" }],
  "bench": [{ "id": 6, "name": "李四", "number": "8", "position": "1B" }],
  "fieldingPitcherId": 7,
  "lastDhAddedFromPoolId": null
}
```
- 只承载本队数据，不包含对手阵容与比赛设置。
- `lineup` 为打线顺序；`bench` 为勾选的可上场替补；`fieldingPitcherId` 为仅守备投手；占位行（id<=0）不参与校验。
- 服务端校验球员属于该赛事报名名单；草稿允许不完整。

## 生命周期
1. 球队负责人在“赛事报名 → 赛前阵容”选择本队未开始比赛。
2. 保存 = 按 `(game_id, team_id)` upsert；开赛前可反复修改。
3. 办赛方实时录入页读取双方提交：快照该侧无数据时自动带出；已有编辑时提示手动载入。
4. 比赛开始后球队侧保存返回 400；快照成为录入权威数据。
5. 撤销报名/办赛方移除参赛时，按 `(event_id, team_id)` 软删提交，避免重新邀请后出现旧阵容。

## 约束与口径
- 仅比赛状态为 `scheduled` / `postponed` 时允许保存；`live` / `final` / `cancelled` 拒绝。
- 球队必须已确认参加该赛事，且比赛主客队包含该队。
- 赛事存在参赛记录时（电子秩序册受限），阵容球员必须在该队报名名单内。
- 办赛方读取 `GET /game/{gameId}/lineup-submissions` 只返回仍在比赛双方中的球队提交。

> 本文以代码为准（GameLineupSubmission.java / TeamGameLineupService.java / V20261111150000__game_lineup_submission.sql）。
