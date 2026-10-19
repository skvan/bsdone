# 球队实体(Team)

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
本文档围绕球队实体 Team 的数据模型与业务实现进行系统化说明，覆盖字段定义、分类信息、生命周期字段、关联关系（球员、教练、比赛）、统计计算逻辑与缓存策略，以及 CRUD 操作示例和数据验证规则。目标是帮助开发者快速理解并正确使用球队实体的数据模型与服务能力。

> 变更（Issue #318，2026-10-14）：球队**去联盟归属**——球队是租户级实体，不再隶属任何联盟；通过赛事报名（EventTeam）参与多个办赛方的比赛。`league_id` 列与实体字段保留（防 ddl-auto 改列）但业务读写停止：创建/更新入参一律忽略并落库 null，存量数据已由 Flyway 迁移全量清空（备份表 `bs_team_league_backup_20261014`）。

## 项目结构
- 实体层：Team 继承 BaseEntity，承载球队主数据与审计字段。
- 仓储层：TeamRepository 提供按租户、ID 集合等查询与分页能力。
- 服务层：TeamService 封装创建、更新、解散、列表与选择项等业务流程，包含数据范围控制与「创建即负责」落职。
- API 层：TeamApi 暴露 REST 接口用于前端调用。
- DTO：TeamOptionDto 用于下拉选项等轻量返回。
- 关联与统计：通过 PersonnelHistoryRecorder 记录人员（教练）与球队的变动；通过 StatsService 计算球队战绩与排名。

```mermaid
graph TB
subgraph "API层"
A["TeamApi"]
end
subgraph "服务层"
B["TeamService"]
C["PersonnelHistoryRecorder"]
D["StatsService"]
end
subgraph "持久化层"
E["TeamRepository"]
end
subgraph "实体层"
F["Team"]
G["BaseEntity"]
end
A --> B
B --> E
B --> C
B --> D
E --> F
F --> G
```

## 核心组件
- 球队实体 Team：定义球队标识、基本信息、分类与展示配置等字段。
- 基础实体 BaseEntity：统一的主键、审计与软删除字段。
- 仓储 TeamRepository：提供按租户、ID 集合的查询与分页方法。
- 服务 TeamService：实现 CRUD、数据范围控制、「创建即负责」落职、历史变更记录。
- API TeamApi：对外暴露 /team 相关接口。
- DTO TeamOptionDto：用于下拉选项等场景的轻量数据传输对象。

## 架构总览
下图展示了从请求到存储的完整链路，包括权限与数据范围控制、「创建即负责」落职、历史记录与统计计算的参与点。

```mermaid
sequenceDiagram
participant Client as "客户端"
participant API as "TeamApi"
participant Svc as "TeamService"
participant Repo as "TeamRepository"
participant Hist as "PersonnelHistoryRecorder"
participant Stats as "StatsService"
Client->>API : "POST /team/create"
API->>Svc : "create(body)"
Svc->>Repo : "save(entity)"
Note over Svc,Repo : "设置租户ID、忽略 leagueId（去联盟归属）"
Note over Svc : "创建即负责：非超管/租管创建者落负责人关系"
API-->>Client : "返回新球队ID"
Client->>API : "PUT /team/update/{id}"
API->>Svc : "update(id, body)"
Svc->>Hist : "afterTeamUpdate(before, saved)"
Svc->>Repo : "save(entity)"
API-->>Client : "成功"
Client->>API : "GET /team/list"
API->>Svc : "list(page, pageSize, sort...)"
Svc->>Repo : "findByTenantIdAndDeletedAtIsNull(...)"
API-->>Client : "分页结果"
Client->>API : "GET /team/select-options"
API->>Svc : "listForSelect()"
Svc->>Repo : "findForSelectByTenantId(...)"
API-->>Client : "下拉选项"
Note over Stats,Svc : "统计数据由StatsService基于赛事结果计算<br/>可结合TeamService获取球队维度聚合"
```

## 详细组件分析

### 球队实体 Team 数据模型
- 标识与基本信息
  - 名称 name：必填，最大长度限制，用于展示与检索。
  - 英文名 nameEn：可选，国际化展示。
  - 简称 shortName：可选，用于紧凑展示。
  - 队徽 logo：图片 URL，用于图标展示。
  - 文字标 wordmark：简化版队徽图片 URL。
  - 背景图 bgImage：可选，用于页面装饰。
- 分类与归属
  - 租户 tenantId：多租户隔离字段，由服务层在创建时注入。
  - 联赛 leagueId：**已废弃（Issue #318）**——字段保留仅防 ddl-auto 改列，业务读写停止（创建/更新入参忽略、落库 null）；球队通过赛事报名参与多个办赛方的比赛。
  - 城市 city、主场 stadium：可选的地理与场地信息。
- 联系信息
  - 联系人 contactPerson、电话 contactPhone、邮箱 contactEmail：可选。
- 展示与认证
  - 门户展示 showInPortal：默认开启，控制是否在门户中显示。
  - 平台认证 verified：标记已认证球队。
  - 排序 sort：用于列表排序。
- 生命周期字段（来自 BaseEntity）
  - id：自增主键。
  - createdBy/createdAt：创建人与创建时间。
  - updatedBy/updatedAt：更新人与更新时间。
  - deletedBy/deletedAt：软删除标记。

```mermaid
classDiagram
class BaseEntity {
+Long id
+Long createdBy
+LocalDateTime createdAt
+Long updatedBy
+LocalDateTime updatedAt
+Long deletedBy
+LocalDateTime deletedAt
}
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
Team --|> BaseEntity : "继承"
```

### 数据验证规则
- 非空与长度约束
  - name：非空且最大长度限制。
  - 其他字符串字段：均带有最大长度限制。
- 空白值归一化
  - 服务层在创建/更新前将空白字符串统一置为 null，避免冗余存储。
- 去联盟归属（Issue #318）
  - 创建/更新不再校验联盟：入参 leagueId 一律忽略并落库 null（防旧前端回写脏数据）；「联盟不存在或与当前租户不一致」类校验已成为历史。
- 创建即负责
  - 非超管/租管创建者（单建/批量建队/门户自助建队）自动落为该队 `bs_team_manager` active 行并失效其范围缓存；超管/租管不自动落职（由管理员指派负责人）。
- 负责人唯一性
  - `ux_bs_team_manager_team_user_active`（`WHERE deleted_at IS NULL`）保证同队同人只有一条未软删记录；解除为软停用，解除后可再次指派同一用户。

### 生命周期与状态
- 创建：自动填充 createdAt、updatedAt 与 createdBy（由框架监听器处理）。
- 更新：自动刷新 updatedAt 与 updatedBy。
- 删除：采用软删除，设置 deletedAt 与 deletedBy，查询时过滤已删除记录。
- 可见性：showInPortal 控制门户展示；verified 表示平台认证状态。

### 关联关系
- 与联赛 League：**无归属关系（Issue #318 起）**——原 leagueId 单向归属已废弃；跨办赛方的参与关系经赛事报名（`bs_event_team`）表达。
- 与教练 Coach：通过 PersonnelHistoryRecorder 记录教练加入、离开、转会至某球队的事件，形成“教练-球队”的历史关联。
- 与比赛 Game：通过 StatsService 对每场比赛的胜负平进行聚合，计算球队战绩、积分、胜率、净胜分等指标，间接体现球队与比赛的关联。

```mermaid
flowchart TD
Start(["球队创建/更新"]) --> IgnoreLeague["忽略入参 leagueId（去联盟归属）"]
IgnoreLeague --> SaveTeam["保存球队"]
SaveTeam --> Auto{非超管/租管创建?}
Auto --> |是| Grant["落负责人关系（创建即负责）"]
Auto --> |否| RecordHistory["记录人员历史(如教练变更)"]
Grant --> RecordHistory
RecordHistory --> End(["完成"])
```

### 统计信息与计算逻辑
- 战绩聚合：根据比赛结果累计胜、平、负场次，计算积分、胜率、净胜分等。
- 排名与差距：基于积分、负场、净胜分、队名等进行排序，并计算与前名的差距。
- 最近表现：取最近若干场比赛统计近期胜负平情况，辅助展示趋势。

```mermaid
flowchart TD
A["读取赛事结果"] --> B["按球队分组累计W/D/L"]
B --> C["计算GP= W+D+L"]
C --> D["计算Points = 2*W + D"]
D --> E["计算WinPct = W/GP"]
E --> F["汇总RF/RA并计算RunDiff"]
F --> G["排序并计算GB(与前名的差距)"]
G --> H["输出球队统计行"]
```

### 缓存策略
- 当前代码未显式实现针对球队数据的缓存层。
- 建议策略（概念性）：
  - 读多写少场景：对球队下拉选项、详情等热点数据进行缓存（例如 Redis），设置合理过期时间与失效策略。
  - 统计结果：对球队排名、战绩等重计算结果进行缓存，并在赛事结果变更后触发失效或增量更新。
  - 注意：上述为通用优化建议，具体实现需结合系统现有缓存配置与访问模式评估。

[本节为概念性内容，不直接分析具体文件]

### CRUD 操作示例与数据流
- 创建球队
  - 接口：POST /team/create
  - 流程：参数校验 -> 设置租户ID -> 忽略 leagueId -> 保存 -> 「创建即负责」落职（受限身份）-> 返回新ID
- 更新球队
  - 接口：PUT /team/update/{id}
  - 流程：查找并校验租户 -> 写保护（本队负责人/管理员）-> 空白归一化 -> 忽略 leagueId -> 保存 -> 记录人员历史
- 解散球队
  - 接口：DELETE /team/delete/{id}
  - 流程：查找并校验租户 -> 写保护 -> 未开打比赛守卫 -> 批量球员离队 -> 负责人指派失效 -> 软删
- 列表与选择项
  - 接口：GET /team/list、GET /team/select-options
  - 流程：根据租户与数据范围构建查询 -> 分页/排序 -> 返回结果

```mermaid
sequenceDiagram
participant U as "用户"
participant API as "TeamApi"
participant S as "TeamService"
participant R as "TeamRepository"
U->>API : "POST /team/create"
API->>S : "create(body)"
S->>R : "save(entity)"
R-->>S : "已保存"
S-->>API : "返回新ID"
API-->>U : "成功响应"
```

## 依赖关系分析
- Team 依赖 BaseEntity 获得统一的审计与生命周期管理。
- TeamService 依赖 TeamRepository 进行数据存取，依赖 TeamManagerRepository 落「创建即负责」关系，依赖 PersonnelHistoryRecorder 记录人员历史，依赖 TenantQueryPolicyService/AccountScopeService 与 ResourceGuard 进行租户、数据范围与写保护控制（Issue #318 起不再依赖 LeagueRepository）。
- TeamApi 仅作为入口，委托 TeamService 执行业务。
- StatsService 通过赛事结果计算球队统计，间接与 Team 产生聚合关系。

```mermaid
graph LR
TeamApi --> TeamService
TeamService --> TeamRepository
TeamService --> PersonnelHistoryRecorder
TeamService --> StatsService
TeamRepository --> Team
Team --> BaseEntity
```

## 性能考虑
- 查询优化
  - 使用分页与排序参数减少单次返回数据量。
  - 利用 Repository 提供的按租户、ID 集合查询方法，避免全表扫描。
- 数据归一化
  - 空白字符串归一化为 null，降低无效数据存储与比较开销。
- 统计计算
  - 对球队统计结果可引入缓存以减少重复计算；在赛事结果变更后及时失效或增量更新。
- 并发与事务
  - 删除操作使用事务注解保证一致性；高并发下注意幂等与锁机制。

[本节为通用指导，不直接分析具体文件]

## 故障排查指南
- 无权查看/修改/删除
  - 现象：查询、更新或删除时报 403。
  - 排查：确认当前用户的数据范围与租户匹配；球队写保护要求「本队负责人或管理员」（Issue #318 起联盟身份不再可写他队）。
- 软删除后无法获取
  - 现象：get 或 list 无法返回已删除记录。
  - 排查：确认查询条件过滤了 deletedAt 不为空的记录。

## 结论
Team 实体以清晰的字段划分承载球队标识、分类与展示配置，并通过 BaseEntity 统一管理生命周期。服务层实现了严格的租户隔离、写保护与数据范围控制；Issue #318 起球队为租户级实体（去联盟归属），跨办赛方关系经赛事报名表达，创建者自动负责，结合 PersonnelHistoryRecorder 与 StatsService 完善了人员历史与统计计算。整体设计兼顾可扩展性与可维护性，建议在热点场景引入缓存以提升性能。

## 附录
- 常用接口路径
  - 列表：GET /team/list
  - 详情：GET /team/{id}
  - 创建：POST /team/create
  - 更新：PUT /team/update/{id}
  - 删除：DELETE /team/delete/{id}
  - 下拉选项：GET /team/select-options

---

> 本文整理自 Qoder RepoWiki（基于代码自动生成），仅供参考，以代码为准。
