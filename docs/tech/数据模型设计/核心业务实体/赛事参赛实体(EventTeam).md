# 赛事参赛实体（EventTeam / EventRosterPlayer）

## 简介
本文件说明电子秩序册（Issue #310）相关的两个实体：赛事参赛登记 `EventTeam` 与秩序册报名球员 `EventRosterPlayer`。能力链路：办赛方邀请球队（invited）→ 球队确认/谢绝（confirmed/declined）→ 球队提交报名名单 → 办赛方锁定（roster_locked）。赛事存在任意参赛记录后，其比赛录入阵容与统计写入仅可使用各队报名球员。文档以代码为依据。

## 项目结构
- 实体层：EventTeam、EventRosterPlayer（均继承 BaseEntity 审计基类）
- 数据访问层：EventTeamRepository、EventRosterPlayerRepository
- 服务层：EventRegistrationService（邀请/确认谢绝/名单差量/锁定/移除；阵容与统计校验入口）
- 接口层：EventRegistrationApi（10 端点）
- 迁移：`db/migration/postgresql/V20261013100000__event_order_book.sql`

## EventTeam（表 bs_event_team，赛事参赛登记）
- 字段要点
  - 归属：tenantId、eventId、teamId（同一赛事同一球队唯一，软删行除外）。
  - 状态：status ∈ {invited, confirmed, declined}；declined 可被重新邀请（重置回 invited 并清空确认/谢绝/提交时间）。
  - 时间：invitedBy/invitedAt、confirmedAt、declinedAt、rosterSubmittedAt（最近提交名单时间）。
  - 锁定：rosterLocked + lockedBy/lockedAt；锁定后球队不可修改名单（403），办赛方移除需先解锁（锁定中移除 403）。
  - 备注：remark（≤500）。
- 业务含义
  - 表达"某球队参加某赛事"的登记与报名进度；办赛方移除 = 软删本行 + 该队全部名单行。
- 索引
  - 唯一：`(event_id, team_id) WHERE deleted_at IS NULL`；辅助：`(event_id, status)`、`team_id`、`tenant_id`（均带软删过滤）。
- 生命周期
  - 邀请创建 → 确认/谢绝 → 名单提交（可反复）→ 锁定/解锁 → 办赛方移除（软删）。

## EventRosterPlayer（表 bs_event_roster_player，秩序册报名球员）
- 字段要点
  - 归属：tenantId、eventId、teamId、playerId（同一赛事同一球队同一球员唯一，软删行除外）。
  - number：报名时该队背号快照（展示用，可空；读取时缺失回落球员档案背号）。
  - sort：名单顺序（按提交时选择顺序重排）。
- 业务含义
  - 名单为快照语义：提交时校验球员为该队当前在役（`bs_player_team.is_current=true`）；提交后球员离队不自动移出（锁定前球队可自行修改）；软删 = 移除报名。
- 索引
  - 唯一：`(event_id, team_id, player_id) WHERE deleted_at IS NULL`；辅助：`(event_id, team_id)`、`player_id`、`tenant_id`。

## 关联与口径
- Event 1—N EventTeam（teamId 间接关联 Team）；EventTeam 1—N EventRosterPlayer。
- 阵容限制启用条件：该赛事存在任意有效 EventTeam 行（含仅邀请）→ 该赛事全部比赛启用；无参赛记录的比赛不受影响。
- 校验入口（服务层）：阵容保存校验两队列+替补 ⊆ 名单；统计写入仅拦截"为本场首次引入名单外球员"。
- 排赛门槛（Issue #318 决策 8/11）：`bs_event.require_roster=true`（新建赛事默认）→ 至少 2 支「已确认且锁定名单」球队才能建比赛且主/客队须属该集合；历史赛事（false）有参赛记录时同口径、无参赛记录时本租户全量可选（`GET /event/{eventId}/game-team-options`）。

## 约束与校验
- 邀请：被邀球队须与赛事同租户；重复邀请幂等（invited/confirmed 跳过）。
- 确认/谢绝：仅 invited 可操作，重复操作幂等；confirmed→谢绝 与 declined→确认 均拒绝（引导经办赛方重邀/线下沟通）。
- 提交名单：须已确认、至少 1 人、球员须为本队 current 在役；锁定后 403。
- 球队侧守卫（Issue #318）：确认/谢绝/提交名单/入队邀请统一由「可管理该球队」裁定（本队负责人或管理员）；原「联盟代管无主球队」分支已移除。

> 本文以代码为准（EventTeam.java / EventRosterPlayer.java / EventRegistrationService.java / V20261013100000__event_order_book.sql / V20261014100000__team_detach_league_and_event_require_roster.sql）。
