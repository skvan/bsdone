# 个人驾驶舱 API

个人驾驶舱（V3 球星年鉴）的固定布局数据接口。第一版只开放「表现图」与「队内排名」的指标/图表选择，不开放模块增删、拖拽排序或缩放。

## 认证与前置条件

- 请求头：`Authorization: Bearer <token>`。
- 当前账号必须已认领球员档案（`bs_player.user_id = 当前用户`），否则返回 404。
- 门户会员默认角色可通过 `/account/*` 自助端点访问；不需要管理台菜单权限。

## GET /api/account/clubhouse

聚合驾驶舱数据。可选参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| `teamId` | Long | 代表球队；不传时取第一条当前球队 |
| `eventId` | Long | 赛事；不传时取该队排序最靠前的进行中/未结束赛事 |

返回 `Result<Map>`，主要字段：

- `player`：本人档案安全视图（姓名、英文名、背号、守位、头像、身高/体重、投打、简介等；不返回联系方式、userId、tenantId）。
- `teams[]`：当前代表球队；含 `events[]`、`primaryEventId`、`primaryEventName`、`standing`（积分榜名次/胜负/胜率/近况）。
- `scope`：本次数据口径 `{ teamId, teamName, eventId, eventName, gameMode }`。
- `stats`：所选范围下的 `batting` / `pitching` / `fielding` 统计 Map（仅本人数据）。
- `config`：当前用户配置（默认见下）。
- `catalog[]`：指标白名单；含 `key`、`label`、`domain`、`viz[]`、`rankable`、`available`。
- `performance`：`{ metricKey, label, viz, domain, points[], summaryValue, items[], available }`。
  - `points`：按比赛日升序的近 12 场数据点；每点含 `label`、`value`、`gameday`、`eventId`。
  - `items`：仅安打构成（环形图）使用。
- `ranks[]`：四个队内排名位；含 `metricKey`、`label`、`value`、`rank`、`total`、`barPct`、`available`。
- `nextGame` / `schedule[]`：跨代表球队/赛事的未开赛或进行中比赛（最多 6 场）。
- `recentGames[]`：最近 3 场个人比赛记录。

数据口径：

1. 表现图/顶部统计：传了 `eventId` 用该赛事统计，否则用全部赛事合计。
2. 队内排名：必须落到「代表球队 + 赛事」；使用该赛事该球队的球员榜单，按指标排序取本人位置。榜单不启用最低打席资格限制，第一版按有统计记录的球员计算。
3. 赛程：取代表球队已确认赛事中 `scheduled` / `live` 且比赛日不早于今天的比赛，按日期/时间升序。
4. 最近战报：取个人比赛日志最近 3 场。
5. 投球指标仅在有投球局数时标记可用；打击指标仅在有打席/打数时可用。

## PUT /api/account/clubhouse/config

保存固定布局下的数据选择。

请求体示例：

```json
{
  "version": 1,
  "layout": "v3-fixed",
  "performance": { "metricKey": "batting.avg", "viz": "line" },
  "ranks": [
    { "metricKey": "batting.avg" },
    { "metricKey": "batting.h" },
    { "metricKey": "batting.obp" },
    { "metricKey": "batting.rbi" }
  ]
}
```

- `layout` 固定为 `v3-fixed`，客户端传其他值会被服务端收敛/拒绝。
- `performance.viz` 必须在指标的兼容列表内；安打构成只允许 `donut`，比率/计数指标允许 `kpi` / `line` / `bar`。
- `ranks` 必须恰好 4 个、不可重复，且只能使用 `rankable=true` 的指标。
- **队内排名不提供全垒打**；默认四格为打击率、安打、上垒率、打点。

## POST /api/account/clubhouse/config/reset

恢复默认配置，返回默认 `config`。

## 错误约定

| 场景 | 返回 |
|---|---|
| 未登录/Token 无效 | 401 |
| 当前账号未关联球员档案 | 404 |
| 指标、图表、排名数量或重复项非法 | 400 |
| 请求的代表球队/赛事不属于本人 | 回落到默认球队/赛事，不返回越权数据 |

## 第一版限制

- 不返回里程碑、球探笔记（当前无可靠数据源），前端不展示，避免造假数据。
- 同一赛事内若球员同时代表多支球队，球队维度统计仍以赛事统计为准；当前业务模型按“不同赛事代表不同球队”处理。
- 指标/目录标签目前为中文；多语言标签在后续统一。
- 不支持模块增删、拖拽排序、缩放和自由位置；这些在后续版本扩展 `modules` 配置。
