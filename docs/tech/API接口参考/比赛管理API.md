# 比赛管理API

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖关系分析](#依赖关系分析)
7. [性能与实时性](#性能与实时性)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录：接口清单与示例](#附录接口清单与示例)

## 简介
本文件面向“比赛管理”相关API，覆盖赛事创建、比赛安排、比分更新、比赛结果统计、历史比赛记录查询、精彩时刻标记等能力。文档从系统架构、数据模型、状态流转、CRUD操作、实时同步机制与性能优化等方面给出完整说明，并提供接口清单与调用示例路径，便于前后端对接与运维排障。

## 项目结构
- API层：提供REST接口，负责参数校验、路由分发与统一响应封装
- Service层：业务编排、权限与租户隔离、事务控制、数据一致性保障
- Repository层：数据访问（JPA）
- Model层：实体定义（如比赛、赛事、精彩时刻、历史记录）
- 公共组件：分页、统一返回体、异常处理等

```mermaid
graph TB
Client["客户端"] --> API["API层<br/>GameApi / EventApi / HighlightMomentApi / HistoryRecordApi"]
API --> Svc["Service层<br/>GameService / EventService"]
Svc --> Repo["Repository层<br/>JPA Repositories"]
Repo --> DB["数据库<br/>PostgreSQL/SQLite"]
```

## 核心组件
- 赛事管理：创建、查询、导入比赛结果
- 比赛管理：创建、更新、删除、列表查询、实时快照、比分更新、结果保存
- 精彩时刻：创建、查询、更新、删除
- 历史记录：按多维度查询人员/对象变更历史

## 架构总览
比赛管理采用分层架构：API接收请求并委托Service；Service进行权限校验、租户隔离、业务规则验证与事务控制；Repository完成持久化。关键特性包括：
- 多租户与数据范围控制：通过租户ID与联赛维度限制可访问范围
- 软删除：使用deletedAt字段实现逻辑删除
- 实时快照：比赛支持liveSnapshotJson存储实时比分与局况
- 批量结果导入：支持一次性导入比赛与球员统计数据

```mermaid
sequenceDiagram
participant C as "客户端"
participant G as "GameApi"
participant GS as "GameService"
participant R as "Repository"
participant D as "数据库"
C->>G : POST /game/{id}/save-live
G->>GS : saveLive(id, GameSaveLiveDTO)
GS->>R : 读取/更新比赛与球员统计
R->>D : 写入
D-->>R : 确认
R-->>GS : 成功
GS-->>G : 成功
G-->>C : 200 OK
```

## 详细组件分析

### 赛事管理（Event）
- 功能：赛事的增删改查、按联赛与租户过滤、导入比赛结果
- 权限：基于租户与联赛的数据范围控制
- 状态：默认draft，可按需推进
- 详情附带：GET /event/{id} 响应新增 `specialRules` 字段（联赛特殊规则生效值，瞬态、仅详情接口返回；Issue #275，见文末「联赛特殊规则开关与 EP 制」）

```mermaid
classDiagram
class Event {
+Long tenantId
+String name
+Long leagueId
+String season
+String startDate
+String endDate
+String status
+String gameMode
+String description
+Integer sort
}
```

### 比赛管理（Game）
- 功能：创建、更新、删除、列表查询、获取/保存实时快照、比分更新、结果保存、球员统计CRUD
- 状态：status默认scheduled，支持实时更新；inning/topBottom表示当前局与上下半局
- 实时：liveSnapshotJson用于前端轮询或推送展示实时比分
- 统计：每场比赛的球员统计数据独立维护

```mermaid
classDiagram
class Game {
+Long eventId
+Long tenantId
+Long homeTeamId
+Long awayTeamId
+Integer homeScore
+Integer awayScore
+String homeScoreByInning
+String awayScoreByInning
+Integer totalInnings
+Integer inning
+String topBottom
+String status
+String liveSnapshotJson
+String gameTime
+String gameday
+String venue
+Integer spectatorCount
+String umpireHp
+String gameTag
+String weatherSummary
+Double weatherTempC
+String remark
+Boolean isSpecialResult
+Boolean showRemarkInCard
+Boolean includeStatsInRanking
+String gameMode
}
```

#### 比赛状态流转
```mermaid
stateDiagram-v2
[*] --> 已计划 : "创建比赛"
已计划 --> 进行中 : "开始比赛/更新比分"
进行中 --> 已结束 : "结束比赛/保存结果"
已结束 --> [*]
```

### 精彩时刻（HighlightMoment）
- 功能：创建、查询、更新、删除
- 用途：标记比赛中的高光片段，支持媒体类型与排序权重

```mermaid
classDiagram
class HighlightMoment {
+String subjectType
+Long subjectId
+Long historyEventId
+String displayKey
+Long tenantId
+String title
+String description
+String mediaType
+String mediaSource
+String mediaUrl
+String coverUrl
+String happenedAt
+Integer sortWeight
+Integer pinned
+String status
}
```

### 历史记录（HistoryRecord）
- 功能：按目标类型、关联对象、时间范围等多维度查询变更记录
- 用途：审计与回溯，支持快照与变更内容JSON

```mermaid
classDiagram
class HistoryRecord {
+String recordType
+String type
+String targetType
+Long targetId
+String relatedObjectType
+Long relatedObjectId
+Long tenantId
+String changeDate
+String snapshotJson
+String changePayloadJson
+String remark
}
```

## 依赖关系分析
- API到Service：单向依赖，职责清晰
- Service到Repository：通过JPA进行数据访问
- 权限与租户：Service层集成DataScopeService与TenantQueryPolicyService，确保数据隔离
- 事件与比赛：比赛通过eventId关联赛事，保证上下文一致

```mermaid
graph LR
GameApi --> GameService
EventApi --> EventService
GameService --> GameRepository
GameService --> EventRepository
EventService --> EventRepository
```

## 性能与实时性
- 分页与排序：所有列表接口均支持page、pageSize、sortProp、sortOrder，避免全表扫描
- 条件查询：支持按赛事、年份、球队等多维过滤，减少不必要数据加载
- 实时快照：通过GET/POST /game/{id}/live-snapshot实现轻量级实时比分拉取与更新，适合前端轮询或WebSocket推送
- 批量更新：save-live与save-result支持增量更新比分与统计，降低写放大
- 软删除：使用deletedAt过滤，避免物理删除带来的索引与关联问题
- 建议优化：
  - 对高频查询字段建立复合索引（如tenantId、eventId、gameday）
  - 对liveSnapshotJson做合理分片或缓存策略，避免大对象频繁落库
  - 对统计写入采用批处理或异步队列，削峰填谷

[本节为通用性能建议，不直接引用具体代码行]

## 故障排查指南
- 常见错误码与场景
  - 400 非法参数或缺少必要字段：检查请求体结构与必填项
  - 403 无权限：确认租户与联赛数据范围，检查用户角色与授权
  - 404 资源不存在：核对ID有效性，注意软删除导致不可见
- 定位步骤
  - 查看API日志与Service异常堆栈
  - 检查Repository SQL执行计划与索引命中
  - 核对租户ID、联赛ID、赛事ID是否匹配
- 典型问题
  - 无法修改比赛：可能因非所属租户或已被删除
  - 导入结果失败：校验先发起守备位置唯一性与打序人数要求
  - 实时快照未生效：确认保存接口调用成功且JSON格式正确

## 结论
本套比赛管理API以清晰的层次划分、严格的权限与租户隔离、完善的实时比分与结果管理能力，支撑了从赛事规划到比赛执行与复盘的全流程。通过分页、条件查询与批量更新等手段，兼顾了易用性与性能。建议在部署时结合索引与缓存策略进一步优化高并发场景下的读写性能。

[本节为总结性内容，不直接引用具体代码行]

## 附录：接口清单与示例

### 赛事（Event）
- GET /event/list?page=&pageSize=&sortProp=&sortOrder=
  - 作用：分页查询赛事列表
  - 参考：`EventApi.java:51-55`
- GET /event/{id}
  - 作用：获取单个赛事详情
  - 响应：附带 `specialRules` 字段（`{extraBatter, forceHalf}` 联赛特殊规则生效值；未挂联赛 / 无记录按默认兜底；Issue #275，见文末专题）
  - 参考：`EventApi.java:57-64`
- POST /event/create
  - 作用：创建赛事
  - 参考：`EventApi.java:66-70`
- PUT /event/update/{id}
  - 作用：更新赛事
  - 参考：`EventApi.java:78-82`
- DELETE /event/delete/{id}
  - 作用：删除赛事（软删除）
  - 参考：`EventApi.java:84-88`
- POST /event/{eventId}/import-game-result
  - 作用：导入比赛结果（含比赛与统计）
  - 参考：`EventApi.java:72-76`

### 比赛（Game）
- GET /game/list?page=&pageSize=&sortProp=&sortOrder=&eventId=&eventIds=&years=&teamId=
  - 作用：分页查询比赛列表，支持多维过滤
  - 参考：`GameApi.java:62-69`
- GET /game/{id}
  - 作用：获取比赛详情
  - 响应字段 `engineManaged`：该场是否已有 ER 引擎事件；`true` 时前端把 `ER`/`R` 视为引擎只读字段
  - 参考：`GameApi.java:71-75`
- GET /game/{id}/live-snapshot
  - 作用：获取实时快照（比分、局况等）
  - 参考：`GameApi.java:77-81`
- POST /game/{id}/live-snapshot
  - 作用：保存实时快照
  - Issue #234：`snapshotJson` 内增加 `activeAbContext`、`pendingAtBatResultCode`、`pendingAtBatResultText`，保存打席开始时的垒况、球数、比分、守备位置及待定结果；同样写入撤销/重做帧。旧快照缺少这些字段时按恢复现场初始化上下文，无法补出原打席开始状态。接口仍使用 `{snapshotJson}` 包装，不新增表字段。
  - 参考：`GameApi.java:83-87`
- POST /game/create
  - 作用：创建比赛
  - 参考：`GameApi.java:95-99`
- PUT /game/update/{id}
  - 作用：更新比赛
  - 参考：`GameApi.java:101-105`
- POST /game/{id}/save-live
  - 作用：更新比赛实时比分与部分统计
  - ER 单一写方：若该场已有 `bs_earned_run_play`，请求里的 `er` 被忽略；`pitchR/ER/UER/PENDING` 只由 ER 引擎写。无引擎数据的 legacy 场次仍接受 `er`
  - 纸质表单字段（Issue #347）：打者新增 `io`；投手新增 `pitchDoubles/pitchTriples/pitchSh/pitchSf/pitchIo`，并接收 `pitchBb/pitchIbb/pitchHbp` 分项。`bb`/`pitchBb` 含 IBB；`bbHp=bb+hbp`，`pitchBbHp=pitchBb+pitchHbp`
  - 实时自动写：LiveGame 已自动累加 IBB/BB/HBP 分项、GDP、DP、INN、PB、SB允许、CS、GO/FO、投手 2B/3B/SH/SF/IO 与捕手妨碍 IO；首名投手自动标记 GS。W/L/SV/HLD 仍由管理端补录
  - 参考：`GameApi.java:107-111`
- POST /game/{id}/earned-runs/reconstruct
  - 作用：全量替换并重建指定半局的 ER/UER 判定，返回逐分结果与责任投手汇总
  - 请求：`inning`、`half`（`top`/`bottom`）及按 `sequence` 严格递增的完整 `plays` 列表
  - 判定输入：得分跑者可携带 `wouldScoreWithoutMisplay`（布尔，可空）——记录员对「若没有捕逸等防守失误，该跑者是否仍会得分」的判断；传 `null` 或省略时，捕逸得分保留为 `PENDING` 待人工覆核
  - 注意：该接口不是增量追加；判决待定时应传 `rulingPending=true`，结果会保留为 `PENDING`
  - 累计：按该半局新旧判定差额同步更新责任投手的 `pitchR`、`er`、`unearnedR` 与 `pendingR`，重放不会重复累计；责任投手统计列不存在时自动补建（teamId 取该半局守备方）
  - 响应新增 `pitcherAbsoluteTotals`：每投手应用本次差额后的库内绝对 R/ER/UER/PENDING；前端只覆盖、不累加。旧响应缺少该字段时前端不做本地 ER 变更
  - 权限：沿用比赛写入权限、租户隔离与赛事数据范围校验
- POST /game/{id}/save-result
  - 作用：保存比赛最终结果与统计
  - ER 单一写方：引擎场次忽略请求里的 `er`/`pitchR`；legacy 无引擎数据场次仍允许手工修正
  - 纸质表单字段（Issue #347）：`StatPart` 新增 `io/pitchDoubles/pitchTriples/pitchSh/pitchSf/pitchIo`；管理端不再提交 `pitchBbHp` 合并列，服务端按 `pitchBb+pitchHbp` 保存拆分字段
  - 注：`gameNumber`（场次）与 `venue`（场地）为整字段覆盖——请求体传 null 或省略即视为清空（用于前台不显示场次/场地，issue #28）；其余比分/统计字段仍为增量更新
  - 参考：`GameApi.java:113-117`
- DELETE /game/delete/{id}
  - 作用：删除比赛（软删除）
  - 参考：`GameApi.java:119-123`
- GET /game/{gameId}/stats
  - 作用：查询某场比赛的球员统计
  - 参考：`GameApi.java:89-93`
- POST /game/stats/create
- PUT /game/stats/update/{id}
- DELETE /game/stats/delete/{id}
  - 作用：球员统计CRUD
  - 引擎场次（已有 `bs_earned_run_play`）：create 把 R/ER/UER/PENDING 归零；update 保留四个字段并拒绝修改比赛/球员/投手身份；delete 拒绝删除投手行
  - 参考：`GameApi.java:125-141`

### 精彩时刻（HighlightMoment）
- GET /highlight-moment/list?page=&pageSize=&sortProp=&sortOrder=&subjectType=&subjectId=&mediaType=&status=
  - 作用：分页查询精彩时刻
  - 参考：`HighlightMomentApi.java:46-49`
- POST /highlight-moment/create
  - 作用：创建精彩时刻
  - 参考：`HighlightMomentApi.java:51-55`
- PUT /highlight-moment/update/{id}
  - 作用：更新精彩时刻
  - 参考：`HighlightMomentApi.java:57-61`
- DELETE /highlight-moment/delete/{id}
  - 作用：删除精彩时刻
  - 参考：`HighlightMomentApi.java:63-67`

### 历史记录（HistoryRecord）
- GET /history-record/list?page=&pageSize=&sortProp=&sortOrder=&targetType=&targetId=&relatedObjectType=&relatedObjectId=&type=&recordType=&dateFrom=&dateTo=
  - 作用：分页查询历史记录
  - 参考：`HistoryRecordApi.java:40-44`
- POST /history-record/create
  - 作用：创建历史记录
  - 参考：`HistoryRecordApi.java:46-50`

## 阵容保存与秩序册校验（电子秩序册，Issue #310）

赛事存在任意参赛记录（含仅邀请）后即为「受限赛事」：其比赛录入阵容与统计写入仅可使用各队报名球员。

### 端点

| 方法 | 路径 | 用途 | 说明 |
|---|---|---|---|
| POST | /game/{id}/lineup-snapshot | 阵容专用保存（门①） | body `{snapshotJson}` 同 `/{id}/live-snapshot`；受限赛事解析快照中 `homeLineup/awayLineup/homeBench/awayBench/homeFieldingPitcherId/awayFieldingPitcherId/homeLastDhAddedFromPoolId/awayLastDhAddedFromPoolId` 的球员 id，按主客队分别校验 ⊆ 该队有效报名名单；违规 400（消息含球员名与球队名）。阵容板保存/开始录入走此端点；比赛进行中的高频自动保存仍走 `/{id}/live-snapshot`（不解析校验） |

### 统计写入门（门②）

- `POST /game/{id}/save-live`（stats 部分）、`POST /game/{id}/save-result`（stats 部分）、`POST /game/stats/create`、`PUT /game/stats/update/{id}`：受限赛事下逐条校验 `(teamId, playerId)`。
- **首次引入口径**：仅拦截「为本场首次引入的名单外球员」——若 `(gameId, teamId, playerId)` 已有有效统计行，其更新不受限（保护进行中比赛与历史修正）。
- 违规 400：「球员「X」未在本赛事秩序册名单中，无法记入比赛数据」。
- `save-result` 若同请求迁移 eventId，按**目标赛事**口径校验（`dto.game.eventId` 优先）。
- 无赛事（eventId 为空）或赛事无参赛记录 → 全部跳过（现行为不变）。

### 选人池端点

- `GET /event/{eventId}/teams/{teamId}/roster-players`（见《球队球员API》电子秩序册小节）：受限=报名名单；未受限=本队 current 全量在役。

### 排赛报名制门槛（Issue #318 决策 8/11）

- 赛事结束校验（决策 7）：`PUT /event/update/{id}` 将状态由非 ended 置为 ended 前，校验该赛事未删比赛全部为终态（final 已结束 / cancelled 取消，与球队解散守卫同口径），否则 400「存在未结束的比赛，无法结束赛事」；反向（ended→其它）不拦。
- 报名制门槛（决策 11）：`bs_event.require_roster=true`（新建赛事默认）时，建比赛/改主客队/`save-result` 改主客队/`import-game-result` 导入建比赛统一校验——该赛事「已确认且锁定名单」球队数 < 2 → 400「该赛事需至少两支球队完成报名（确认并锁定名单）后才能安排比赛」；主/客队须属该集合，否则 400「主/客队须为本赛事已完成报名的球队」。
- 历史赛事（require_roster=false）不校验（决策 8 宽松口径，照顾存量比赛）。
- `require_roster` 创建时定型：编辑赛事一律沿用存量值（无 UI 开关，防请求体缺省把存量 false 赛事翻转）。
- 排赛选队候选：`GET /event/{eventId}/game-team-options`（见《球队球员API》）；联盟解散守卫同步改为「名下赛事全部 ended 才可解散」（决策 6）。

## 联赛特殊规则开关与 EP 制（Issue #275 / #274）

联赛级特殊规则开关：`extra_batter`（EP 制 / 额外打击者）与 `force_half`（强制换局），存储于 `bs_league_special_rule`（唯一约束 `(league_id, rule_key)`，规则行只写不删）。取值语义：**行存在 = 以记录为准（支持显式关闭）；无记录 = 默认值**（extraBatter=false、forceHalf=true）。

### 规则读写端点（联赛级配置）

| 方法 | 路径 | 用途 | 说明 |
|---|---|---|---|
| GET | /league/{id}/special-rules | 读取生效值（含默认兜底） | 响应 `Result<Map>`，形状 `{rules:{extraBatter:boolean, forceHalf:boolean}}` |
| PUT | /league/{id}/special-rules | 更新规则（camelCase 键，支持子集 / 全集） | body `{rules:{extraBatter, forceHalf}}`；未知键名 / 非布尔 JSON 值 / 缺失值 / 空映射 400，联赛不存在或已软删 404，跨租户 403；返回更新后生效值 |

- 权限点：`business:league:specialRule`（联赛列表行内「特殊规则」按钮 + 两接口注册）；租管仅可操作本租户联赛，超管可跨租户。
- 存量回填（迁移 `V20261015100000__league_special_rule.sql`）：含超默认阵型（棒球 >9 / 垒球 >10 打击）的联赛自动开启 `extra_batter`（打序统计 ∪ 阵容快照双证据，幂等；未挂联赛赛事跳过）。

### 赛事详情附带（消费侧下发）

- `GET /event/{id}` 响应新增 `specialRules: {extraBatter, forceHalf}`：由 EventService 按赛事 `leagueId` 取生效值附带（未挂联赛 / 无记录 → 默认兜底）；该字段为瞬态（不落表），仅详情接口装配，list / create / update 等响应不含（null 省略）。
- 消费口径（编辑先发阵容页 / 实时录入页 / 结果录入页）：`extraBatter` 关闭 → 棒球阵容恰 9 人、垒球恰 10 人；开启 → 棒球 9-12 人（八守位各 1 + P/DH 至少其一 + EP 至多 3，EP 不守备只打击）、垒球 10-11 人；`forceHalf` 关闭 → 实时录入页隐藏「强制换局」按钮。已保存数据不回溯。

---

> 本文整理自 Qoder RepoWiki（基于代码自动生成），仅供参考，以代码为准。
