# 球队球员API

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖关系分析](#依赖关系分析)
7. [性能考虑](#性能考虑)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录](#附录)

## 简介
本文件面向球队与球员管理相关API的开发者与使用者，系统化说明以下能力：
- 球队信息CRUD（创建、查询、更新、删除）
- 球员档案管理（增删改查、导入、统计、明细钻取）
- 教练管理（增删改查、选择列表）
- 阵容模板（创建、复制自比赛、更新、删除、详情）
- 球队负责人管理（指派、移除）
- 多租户数据隔离机制与权限控制要点
- 实体关系图与数据结构定义

## 项目结构
围绕“球队-球员-教练-阵容模板”的业务域，系统采用分层架构：
- API层：暴露REST接口，负责参数校验与结果封装
- Service层：业务编排、权限与租户策略、事务边界
- Repository层：数据访问（JPA/MyBatis）
- Entity层：领域模型映射数据库表
- 核心支撑：租户解析、数据范围、审计与历史变更记录

```mermaid
graph TB
Client["客户端"] --> API["API 控制器"]
API --> Svc["服务层"]
Svc --> Repo["仓储层"]
Repo --> DB["数据库"]
subgraph "核心支撑"
TRF["租户解析过滤器"]
DSC["数据范围服务"]
AUD["人员历史/审计"]
end
API -.-> TRF
Svc -.-> DSC
Svc -.-> AUD
```

## 核心组件
- 球队管理API：提供球队选项、分页列表、详情、创建、更新、删除等接口
- 球员管理API：提供选项、分页列表、详情、统计数据、赛季统计、比赛日志、击球/投球/防守钻取、批量删除、批量导入等接口；球员档案支持“球员-球队经历”多队注册（teamEntries）
- 教练管理API：提供选项、分页列表、详情、创建、更新、删除等接口
- 阵容模板API：按球队维度提供模板列表、详情、创建、从比赛复制、更新、删除
- 球队负责人API：按球队维度列出、指派、移除负责人（需管理员权限）

## 架构总览
请求进入后，先由租户解析过滤器提取并设置当前租户上下文；随后路由到对应API控制器，控制器调用服务层完成业务逻辑。服务层在读写前进行租户与数据范围校验，必要时结合仓库执行持久化操作。

```mermaid
sequenceDiagram
participant C as "客户端"
participant F as "租户解析过滤器"
participant A as "API 控制器"
participant S as "服务层"
participant R as "仓储层"
C->>F : HTTP 请求
F-->>C : 设置租户上下文并转发
C->>A : 调用接口
A->>S : 执行业务方法
S->>R : 查询/保存数据
R-->>S : 返回结果
S-->>A : 业务结果
A-->>C : 统一响应
```

## 详细组件分析

### 球队管理（Team）
- 接口概览
  - GET /team/select-options：获取下拉选项
  - GET /team/list：分页列表（支持排序）
  - GET /team/{id}：获取详情
  - POST /team/create：创建球队
  - PUT /team/update/{id}：更新球队
  - DELETE /team/delete/{id}：软删除
- 关键行为
  - 列表与选项均受租户与数据范围限制（管理上下文窄读=自有球队集合；联盟不再派生球队可见域，Issue #318）
  - 创建/更新不再读写联盟：leagueId 入参一律忽略（落库 null）；创建即负责——非超管/租管创建者自动落为该队负责人（含批量建队与门户自助建队）
  - 删除为软删除，记录删除时间与操作人；解散前须无未开打比赛并走完整级联（球员离队/负责人失效）
- 数据模型要点
  - 包含租户ID、名称、标识、城市、主场、描述、联系方式、展示开关、认证状态、排序等字段（`league_id` 列保留但已废弃恒为 null，实体字段写入停止，Issue #318）

```mermaid
classDiagram
class Team {
+Long tenantId
+Long leagueId
+String name
+String nameEn
+String shortName
+String logo
+String wordmark
+String bgImage
+String city
+String stadium
+String description
+String contactPhone
+String contactEmail
+String contactPerson
+Integer showInPortal
+Integer verified
+Integer sort
}
```

### 球员管理（Player）
- 接口概览
  - GET /player/select-options：下拉选项
  - GET /player/list：分页列表（支持按球队、ID集合、关键字、号码、位置、投打惯用手、状态、加入日期区间筛选）；球队筛选=存在该队“当前”经历，号码/位置筛选按经历段匹配；返回项含 teamEntries
  - GET /player/check-full-name：全名重复性检查
  - GET /player/team-options?teamId=：某队“当前注册”球员可选项（号码/位置取自该队注册段，含投打手、状态，供阵容编辑选择）
  - GET /player/{id}：详情（含 teamEntries 球队经历）
  - GET /player/{id}/stats：综合统计（可按比赛模式过滤）
  - GET /player/{id}/stats/by-season：按赛季统计
  - GET /player/{id}/stats/game-log：比赛日志（默认最近30条）
  - GET /player/{id}/stats/drill-down/batting|pitching|fielding：击球/投球/防守钻取（支持分页、赛事、赛季、比赛模式）
  - POST /player/create：创建（请求体可携带 teamEntries）
  - PUT /player/update/{id}：更新（请求体可携带 teamEntries）
  - DELETE /player/delete/{id}：删除
  - POST /player/delete-batch：批量删除
  - POST /player/import：批量导入（支持重复策略）
- 关键行为
  - 列表与选项均受租户与数据范围限制；支持自由球员（无“当前”经历）与指定球队（存在该队“当前”经历）两种视图
  - 数据范围判定：受限用户按“当前经历是否落在授权球队”判定可见性（列表与详情一致；ID集合查询沿用旧可见性以兼容历史比赛详情补全）
  - 受限角色不提供「自由球员」视图：`teamId=0` 查询一律空页——无归属球员（含已离队且未加入新队者）不属于任何授权球队；找自由球员请走「邀请球员」候选搜索
  - 统计与钻取通过服务聚合计算并按模式过滤；统计按 playerId 聚合，不受多队注册影响
  - 导入与旧页面更新会按旧字段确保/更新一条“当前球队”经历
- 数据模型要点
  - 关联用户ID、租户ID；包含姓名、昵称、号码镜像、位置镜像、头像、背景配置、出生信息、身高体重、投打手、选秀/首秀、学历、当前加入记录ID、状态、联系方式、简介、排序等
  - 多队注册：teamId/number/positions 为“主注册”镜像字段；完整球队经历见 bs_player_team（下表）

```mermaid
classDiagram
class Player {
+Long userId
+Long teamId
+Long tenantId
+String name
+String shortName
+String nameEn
+String nickname
+String number
+String positions
+String avatar
+String bgImage
+Map~String,Object~ bgFocusConfig
+String[] bgImages
+String birthDate
+String birthPlace
+String height
+String weight
+String throwHand
+String batHand
+String draft
+String debut
+String education
+Long currentJoinRecordId
+String status
+String contactPhone
+String contactEmail
+String intro
+Integer sort
}
```

#### 球员-球队经历（PlayerTeam / bs_player_team）
一位球员可注册多支球队（简历式多段），每段包含：球队、该队背号、该队守备位置、是否“当前球队”（可多选）。

```mermaid
classDiagram
class PlayerTeam {
+Long playerId
+Long teamId
+Long tenantId
+String number
+String positions
+Boolean current
+Integer sort
}
```

- 接口约定（随球员档案嵌套，无独立路由）：
  - 详情/列表输出：GET /player/{id}、GET /player/list 返回项均含 `teamEntries: [{id, teamId, teamName, number, positions[], current, sort}]`
  - 创建/更新：POST /player/create、PUT /player/update/{id} 请求体可携带 `teamEntries`
    - 提供 teamEntries：全量替换语义——缺失段被移除（软删）；`current=false` 表示保留经历但不再“当前”
    - 未提供 teamEntries（旧页面兼容）：teamId>0 时确保该队一段“当前”经历并按请求体号码/守备位置更新该段（其他段不动）；teamId 为空时取消全部“当前”标记（自由球员）
  - 镜像回写：保存后 teamId/number/positions 取“主注册”（“当前”段按 sort,id 排序取第一段）；无“当前”段时三者置空
  - 校验：段内球队重复 → 400；球队不存在或跨租户 → 400
- 演进记录联动：“当前”集合变化时写 join/leave/transfer 事件，并回写 currentJoinRecordId
- 阵容选择：GET /player/team-options?teamId= 返回该队“当前注册”球员，号码/守备位置取该队注册段（旧版阵容/模板页无需前端改动即受益）
- 过渡期说明：旧编译版页面（球员列表/详情等）按镜像字段展示，仅显示主队/主背号；新工程（前端重建 B4 批次）将按 teamEntries 完整展示与编辑

### 教练管理（Coach）
- 接口概览
  - GET /coach/select-options：下拉选项
  - GET /coach/list：分页列表（支持关键字、球队筛选）
  - GET /coach/{id}：详情
  - POST /coach/create：创建
  - PUT /coach/update/{id}：更新
  - DELETE /coach/delete/{id}：删除
- 关键行为
  - 列表与选项受租户与数据范围限制；支持自由教练与绑定球队教练
  - 创建/更新时自动应用所属租户并校验球队归属
  - 删除为软删除
- 数据模型要点
  - 关联球队ID、租户ID；包含姓名、职位、照片、联系方式、当前加入记录ID、排序等

```mermaid
classDiagram
class Coach {
+Long teamId
+Long tenantId
+String name
+String position
+String photo
+String contactPhone
+String contactEmail
+Long currentJoinRecordId
+Integer sort
}
```

### 阵容模板（TeamLineupTemplate）
- 接口概览（按球队维度）
  - GET /team/{teamId}/lineup-template/list：模板列表
  - GET /team/{teamId}/lineup-template/{id}：模板详情（含首发槽位、替补球员ID、先发投手ID）
  - POST /team/{teamId}/lineup-template/create：创建模板
  - POST /team/{teamId}/lineup-template/copy-from-game：从比赛复制生成模板
  - PUT /team/{teamId}/lineup-template/update/{id}：更新模板
  - DELETE /team/{teamId}/lineup-template/delete/{id}：删除模板
- 关键行为
  - 所有操作均需验证球队存在与访问权限
  - 创建/更新对槽位与替补进行合法性校验，确保必填位置完整且无冲突
  - 从比赛复制时校验比赛归属与队伍侧（主/客）
- 数据模型要点
  - 关联租户ID、球队ID；包含模板名称、描述、首发槽位JSON等

```mermaid
classDiagram
class TeamLineupTemplate {
+Long tenantId
+Long teamId
+String name
+String description
+String slotsJson
}
```

### 球队负责人（TeamManager）
- 接口概览（按球队维度）
  - GET /team/{teamId}/managers：列出活跃负责人
  - POST /team/{teamId}/managers：指派负责人（需管理员权限）
  - DELETE /team/{teamId}/managers/{userId}：移除负责人
- 关键行为
  - 仅超级管理员或租户管理员可指派/移除
  - 指派时需校验用户非空、球队存在、未重复指派
  - 移除时为软停用，记录删除时间
- 数据模型要点
  - 关联租户ID、球队ID、用户ID；状态active/inactive

```mermaid
classDiagram
class TeamManager {
+Long tenantId
+Long teamId
+Long userId
+String status
}
```

### 实体关系图（ER）
```mermaid
erDiagram
TEAM {
bigint id PK
bigint tenant_id
bigint league_id
string name
string short_name
string logo
string wordmark
string bg_image
string city
string stadium
text description
string contact_phone
string contact_email
string contact_person
integer show_in_portal
integer verified
integer sort
}
PLAYER {
bigint id PK
bigint user_id
bigint team_id FK
bigint tenant_id
string name
string short_name
string name_en
string nickname
string number
text positions
string avatar
string bg_image
jsonb bg_focus_config
jsonb bg_images
string birth_date
string birth_place
string height
string weight
string throw_hand
string bat_hand
string draft
string debut
string education
bigint current_join_record_id
string status
string contact_phone
string contact_email
string intro
integer sort
}
COACH {
bigint id PK
bigint team_id FK
bigint tenant_id
string name
string position
string photo
string contact_phone
string contact_email
bigint current_join_record_id
integer sort
}
TEAM_LINEUP_TEMPLATE {
bigint id PK
bigint tenant_id
bigint team_id FK
string name
string description
text slots_json
}
TEAM_MANAGER {
bigint id PK
bigint tenant_id
bigint team_id FK
bigint user_id
string status
}
PLAYER_TEAM {
bigint id PK
bigint player_id FK
bigint team_id FK
bigint tenant_id
string number
text positions
boolean is_current
int sort
}
TEAM ||--o{ PLAYER : "拥有"
TEAM ||--o{ COACH : "拥有"
TEAM ||--o{ TEAM_LINEUP_TEMPLATE : "拥有"
TEAM ||--o{ TEAM_MANAGER : "负责人"
PLAYER ||--o{ PLAYER_TEAM : "注册"
TEAM ||--o{ PLAYER_TEAM : "多队经历"
```

## 依赖关系分析
- API 到 Service 的单向依赖，Service 组合多个仓储与支撑服务
- 数据访问通过 JPA Specification 构建动态查询条件，结合租户与数据范围实现细粒度隔离
- 阵容模板依赖比赛与比赛球员统计以支持“从比赛复制”
- 负责人管理直接操作仓储并校验管理员权限

```mermaid
graph LR
TAPI["TeamApi"] --> TSvc["TeamService"]
PAPI["PlayerApi"] --> PSvc["PlayerService"]
CAPI["CoachApi"] --> CSvc["CoachService"]
LTA["TeamLineupTemplateApi"] --> LTSvc["TeamLineupTemplateService"]
MAPI["TeamManagerApi"] --> Repo["Repository"]
TSvc --> Repo
PSvc --> Repo
CSvc --> Repo
LTSvc --> Repo
```

## 性能考虑
- 列表与选项查询使用分页与排序，避免全量加载
- 复杂筛选通过Specification在数据库层完成，减少内存计算
- 阵容模板复制涉及跨表读取，建议按需传入必要参数并限制返回字段
- 批量导入应控制批次大小，避免长事务与锁竞争
- 统计与钻取接口可能涉及多表聚合，建议在高频场景引入缓存或预计算

[本节为通用指导，不直接分析具体文件]

## 故障排查指南
- 常见错误码与提示
  - 400：参数非法（如缺少必填项、值不合法）
  - 401：未登录或令牌无效
  - 403：无权限（越权访问、非管理员操作负责人等）
  - 404：资源不存在（球队、模板、比赛等）
- 定位思路
  - 确认请求是否携带正确的Authorization头（负责人相关接口）
  - 检查租户上下文是否正确解析（过滤器设置）
  - 核对数据范围是否允许访问目标对象（受限用户按“当前经历球队”判定球员可见性）
  - 查看服务层抛出的业务异常与日志堆栈
  - 球队经历相关 400：重复球队/球队不存在/跨租户不匹配（核对 teamEntries 内容与球队归属）

## 结论
本套API围绕球队、球员、教练与阵容模板构建了完整的生命周期管理能力，并通过统一的租户解析与数据范围机制保障多租户环境下的数据安全与隔离。建议在实际使用中：
- 严格遵循接口参数规范与权限要求
- 合理使用分页与筛选条件提升性能
- 对敏感操作（负责人指派/移除）做好审计与告警
- 对高并发统计与钻取场景评估缓存与预计算方案

[本节为总结性内容，不直接分析具体文件]

## 附录

### 多租户数据隔离机制说明
- 请求进入时由租户解析过滤器提取并设置当前租户ID
- 服务层在查询/写入前强制注入tenantId并校验数据范围
- 列表与选项查询根据有效数据范围进一步缩小结果集
- 创建/更新时校验关联资源的租户一致性，防止跨租户污染

```mermaid
flowchart TD
Start(["请求进入"]) --> Resolve["解析租户ID"]
Resolve --> Scope["计算有效数据范围"]
Scope --> Query{"是否允许访问?"}
Query -- 否 --> Deny["拒绝访问(403)"]
Query -- 是 --> Exec["执行业务逻辑"]
Exec --> End(["返回结果"])
```

## 电子秩序册（赛事报名，Issue #310）

面向办赛方与球队管理员的赛事报名（秩序册）能力：办赛方在赛事上邀请球队 → 球队确认参加 → 提交本队在役球员报名名单 → 办赛方锁定名单。赛事存在任意参赛记录后，其比赛录入阵容与统计写入仅可使用报名球员（阵容限制细则见《比赛管理API》）。

### 端点清单

| 方法 | 路径 | 用途 | 守卫 |
|---|---|---|---|
| POST | /event/{eventId}/teams/invite | 邀请球队参赛（body: `{teamIds:[], remark?}`；幂等：已邀/已确认跳过，已谢绝重邀重置为 invited） | 办赛方（赛事所属联盟） |
| GET | /event/{eventId}/team-options | 赛事可邀请球队：本租户全部未删球队（含他联盟/无联盟，不受管理域过滤；Issue #315 修复） | 办赛方（赛事所属联盟）/租管/超管 |
| GET | /event/{eventId}/game-team-options | 排赛候选球队（Issue #318）：报名制赛事或已有参赛记录→仅「已确认且锁定名单」球队；历史无参赛记录赛事→本租户全部未删球队 | 办赛方（赛事所属联盟）/租管/超管 |
| GET | /event/{eventId}/teams | 赛事参赛列表（球队名/徽、状态、名单人数、锁定态、最近提交时间） | 登录+同租户 |
| POST | /event/{eventId}/teams/{teamId}/remove | 移除参赛（软删登记+全部名单行；锁定后 403，须先解锁） | 办赛方 |
| POST | /event/{eventId}/teams/{teamId}/lock | 锁定报名名单（留痕 lockedBy/lockedAt） | 办赛方 |
| POST | /event/{eventId}/teams/{teamId}/unlock | 解锁报名名单 | 办赛方 |
| GET | /event/{eventId}/teams/{teamId}/roster | 查看报名名单明细（姓名/头像/背号快照/位置/是否仍在队） | 登录+同租户 |
| GET | /event/{eventId}/teams/{teamId}/roster-players | 阵容选人池（受限=该队报名名单球员；未受限=本队 current 全量在役） | 登录+同租户 |
| GET | /team/{teamId}/player-invite-candidates | 球员候选搜索：本租户未删且**当前不在本队**的球员（排除已在本队者）；keyword 匹配姓名/昵称/简称；附认领状态、现属球队；分页，pageSize 上限 100，按 id 升序 | 可管理该球队 |
| POST | /team/{teamId}/player-claim-invite | 创建入队邀请（body: `{playerId?, expireHours?, maxUses?, remark?}`；未认领→定向绑定该球员；否则→通用 token） | 可管理该球队 |
| GET | /team/{teamId}/player-claim-invites | 入队邀请列表（含 token 供复制链接、绑定球员名、状态/使用次数/有效期） | 可管理该球队 |
| POST | /team/{teamId}/player-claim-invites/{id}/revoke | 撤销入队邀请（幂等；跨队/不存在 404） | 可管理该球队 |
| GET | /team/{teamId}/event-registrations | 球队侧「我的赛事报名」列表（赛事名/赛季/模式/状态/能否撤销） | 登录+同租户 |
| POST | /event/{eventId}/teams/{teamId}/confirm | 确认参加（仅 invited 可操作；重复确认幂等） | 可管理该球队 |
| POST | /event/{eventId}/teams/{teamId}/decline | 谢绝参赛（仅 invited 可操作；重复谢绝幂等） | 可管理该球队 |
| POST | /event/{eventId}/teams/{teamId}/withdraw | 撤销报名（球队侧自助退赛）：仅已确认且本队尚无已开赛（进行中/已结束）比赛；软删登记+全部名单行，名单锁定不拦（撤销＝整队退出） | 可管理该球队 |
| PUT | /event/{eventId}/teams/{teamId}/roster | 提交/修改名单（body: `{playerIds:[]}` 按展示顺序；锁定 403；须已确认；至少 1 人；球员须本队 current 在役） | 可管理该球队 |

### 关键规则
- 状态机：`invited → confirmed / declined`；declined 可被办赛方重新邀请（重置回 invited，清空确认/谢绝/提交时间）。
- 撤销报名（球队侧自助退赛）：仅已确认的登记可撤销；本队在该赛事**尚无已开赛比赛**（进行中/已结束）才允许，已有已开赛比赛 → 400；名单锁定**不拦**（撤销＝整队退出，名单随登记作废）。撤销后办赛方侧参赛列表/整册页/邀请候选同步移除；已排赛程保留由办赛方后续处理（相关比赛暂不可录入，待办赛方删除或改队）。软删留痕（deletedBy/deletedAt），可被重新邀请。
- 名单为快照语义：提交时校验球员为该队当前在役（`bs_player_team.is_current=true`）；提交后球员离队不自动移出，锁定前球队可自行修改；软删 = 移除报名。
- 球队侧权限（Issue #318）：确认/谢绝/提交名单/入队邀请等统一由「可管理该球队」裁定（球队负责人或管理员）；原「联盟代管无主球队」分支已随去联盟归属移除——无主球队由租管/超管指派负责人，或由创建者「创建即负责」。
- 排赛门槛（Issue #318 决策 8/11）：新建赛事默认报名制（`bs_event.require_roster=true`）——至少 2 支「已确认且锁定名单」球队才能建比赛，且主/客队须属该集合；历史赛事（false）有参赛记录时同口径、无参赛记录时本租户全量可选。排赛选队走 `game-team-options` 端点。
- 邀请入队（批 4）：候选=本租户全量未删球员（非超管对跨租户球队一律 404，超管全局豁免）；未认领→定向绑定邀请、已认领→通用链接；**撤销后旧链接失效**（领取/认领接口返回 400「邀请链接已失效」）；创建邀请/候选守卫=可管理该球队（Issue #318 起无联盟代管分支）。

### 阵容限制口径（双门校验，Issue #310）
- 启用条件：赛事存在任意有效参赛记录（含仅邀请）即对该赛事全部比赛启用；无参赛记录的赛事不受影响。
- 门①（阵容保存）：`POST /game/{id}/lineup-snapshot` 校验两队列+替补+守备投手 ⊆ 对应球队名单（详见《比赛管理API》）。
- 门②（统计写入）：`save-live`/`save-result`/`/game/stats/*` 逐条校验，仅拦截"为本场首次引入名单外球员"。
- 前端选人池经 `roster-players` 端点接入：受限时只返回报名球员；未报名/未提交名单的球队池为空（UI 提示区分）。

### 典型错误码
- 400：球队不存在或不属于当前租户 / 请选择要邀请的球队 / 备注不能超过 500 字 / 请先确认参加后再提交名单 / 以下球员当前不在本队：… / 已谢绝参赛，请联系办赛方重新邀请 / 已确认参赛，不能直接谢绝 / 请至少选择 1 名球员 / 仅已确认参赛的球队可撤销报名 / 球队已有比赛开赛，不能撤销报名
- 403：无权管理该球队（无权限）/ 名单已锁定，无法修改（提交名单）/ 名单已锁定，请先解锁再移除（移除参赛时）
- 404：赛事不存在（含赛事已归还/软删）/ 该球队未参加本赛事

---

> 本文整理自 Qoder RepoWiki（基于代码自动生成），仅供参考，以代码为准。
