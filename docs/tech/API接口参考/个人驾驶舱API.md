# 个人驾驶舱 API

个人驾驶舱（V3 球星年鉴）的模块化数据接口。支持九个业务模块的显示/隐藏、排序与桌面端宽高调整，以及「表现图」与「队内排名」的指标/图表选择。宽高在服务端白名单范围内校验；手机端忽略宽高，按单列展示。

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
- `config`：当前用户配置（version 3；`modules[]` 含 `width`/`height`，默认见 PUT 示例）。
- `moduleCatalog[]`：模块白名单；含 `key`、`label`、`span`（默认列宽）、`minSpan`（最小列宽）、`maxSpan`（固定 12），共 9 个模块。
- `milestoneCatalog[]`：生涯里程碑指标白名单；含 `key`、`label`、`shortLabel`、`domain`、`unit`、`defaultSelected`、`thresholds[]`。
- `catalog[]`：指标白名单；含 `key`、`label`、`domain`、`viz[]`、`rankable`、`available`。
- `performance`：`{ metricKey, label, viz, domain, points[], summaryValue, items[], available }`。
  - `points`：按比赛日升序的近 12 场数据点；每点含 `label`、`value`、`gameday`、`eventId`。
  - `items`：仅安打构成（环形图）使用。
- `ranks[]`：四个队内排名位；含 `metricKey`、`label`、`value`、`rank`、`total`、`barPct`、`available`。
- `evaluation`：总体评价雷达数据。
  - `available`：当前范围是否至少有一张可用雷达图。
  - `comparison`：比较口径，固定为 `同队同赛事`。
  - `minSample`：最少样本数，当前为 3。
  - `cards[]`：打击、守备固定各一张；有投球局数时追加投球一张。
    - `domain`：`batting` / `fielding` / `pitching`。
    - `title` / `subtitle`：卡片标题与指标缩写。
    - `available`、`reason`：无数据或样本不足时 `available=false`，并给出原因。
    - `score` / `grade`：五个轴百分位的平均值，以及 S/A/B/C 等展示等级。
    - `sampleSize`：参与比较的有效球员数。
    - `axes[]`：`key`、`label`、`abbr`、`value`（原始值）、`percentile`（0–100 同队同赛事百分位）。
  - 打击轴：`AVG`、`OBP`、`SLG`、`RBI`、`SB`；有效打席门槛 3。
  - 守备轴：`FPCT`、`PO/G`、`A/G`、`DP/G`、`RF/9`；有效守备局数门槛 3 局。
  - 投球轴：`ERA`、`WHIP`、`K/9`、`BB/9`、`IP`；有效投球局数门槛 1 局。ERA / WHIP / BB/9 越低越好，服务端已反向换算百分位。`IP` 展示保留棒球记法（如 `5.2` 表示 5⅔ 局），百分位按出局数换算。
  - 百分位只在所选代表球队与赛事内计算；样本少于 3 人时不生成雷达值，避免小样本误读。
  - 守备指标受守位影响，前端须与“同队同赛事”口径及守备局数一起展示。
- 生涯里程碑数据不在驾驶舱首屏返回；前端仅在里程碑模块启用时调用 `GET /api/account/clubhouse/milestones` 按需加载，字段见下文独立接口。
- `nextGame` / `schedule[]`：跨代表球队/赛事的未开赛或进行中比赛（最多 6 场）。
- `recentGames[]`：最近 3 场个人比赛记录。

数据口径：

1. 表现图/顶部统计：传了 `eventId` 用该赛事统计，否则用全部赛事合计。
2. 队内排名：必须落到「代表球队 + 赛事」；使用该赛事该球队的球员榜单，按指标排序取本人位置。榜单不启用最低打席资格限制，第一版按有统计记录的球员计算。
3. 赛程：取代表球队已确认赛事中 `scheduled` / `live` 且比赛日不早于今天的比赛，按日期/时间升序。
4. 最近战报：取个人比赛日志最近 3 场。
5. 投球指标仅在有投球局数时标记可用；打击指标仅在有打席/打数时可用。
6. 总体评价：以所选代表球队 + 赛事的有出场数据球员为样本，计算五个职业棒球口径指标的百分位；投球反向指标先取反再计算。打击样本至少 3 打席、守备至少 3 局、投球至少 1 局；样本球员少于 3 人时不出雷达值。
7. 生涯里程碑：前端仅在 `milestones` 模块启用时按需请求独立接口；全租户生涯，跨全部赛事与代表球队；只汇总计入排行的比赛日志，且不按棒球/垒球模式过滤。累计与时间线使用同一份日志；时间线从系统记录的最早比赛开始，不能补齐系统外历史。

## GET /api/account/clubhouse/milestones

按需加载全租户生涯里程碑。仅当前账号已认领球员档案、且前端启用里程碑模块时调用。

返回 `Result<Map>`：

- `available`：当前是否有可展示的累计指标。
- `scopeLabel`：固定为 `全租户生涯 · 跨赛事与球队`。
- `note`：统计口径说明。
- `reachedCount`：已选指标已达成里程碑总数。
- `items[]`：`metricKey`、`label`、`shortLabel`、`domain`、`unit`、`value`、`nextTarget`、`remaining`、`progressPct`、`reachedCount`。
- `timeline[]`：最近 5 项，含 `metricKey`、`title`、`gameday`、`eventName`、`opponentName`、`teamName`、`gameId`。
- `pitching.ip` 的 `value`/`remaining` 使用棒球记法；进度按出局数计算。
- 该接口不接收 `teamId`/`eventId`，不受驾驶舱范围栏影响；查询全部比赛模式。

## PUT /api/account/clubhouse/config

保存模块顺序/开关/宽高、表现图与队内排名选择。

请求体示例：

```json
{
  "version": 3,
  "layout": "v3-modular",
  "modules": [
    { "key": "ranks", "enabled": true, "width": 12, "height": 0 },
    { "key": "evaluation", "enabled": true, "width": 12, "height": 0 },
    { "key": "milestones", "enabled": true, "width": 12, "height": 0 },
    { "key": "performance", "enabled": true, "width": 8, "height": 0 },
    { "key": "teams", "enabled": true, "width": 4, "height": 0 },
    { "key": "schedule", "enabled": true, "width": 8, "height": 0 },
    { "key": "about", "enabled": true, "width": 4, "height": 0 },
    { "key": "recentGames", "enabled": true, "width": 8, "height": 0 },
    { "key": "quickLinks", "enabled": true, "width": 4, "height": 0 }
  ],
  "milestones": {
    "metricKeys": ["batting.h", "batting.rbi", "batting.sb", "batting.gp", "pitching.ip", "pitching.so"]
  },
  "performance": { "metricKey": "batting.avg", "viz": "line" },
  "ranks": [
    { "metricKey": "batting.avg" },
    { "metricKey": "batting.h" },
    { "metricKey": "batting.obp" },
    { "metricKey": "batting.rbi" }
  ]
}
```

- `modules` 数组顺序即展示顺序；只接受白名单 key，且不能重复，否则返回 400。
- `enabled` 必须是布尔值；缺省视为 `true`。允许九个模块全部关闭。
- `width`：整数列数，范围为该模块 `minSpan`–12；缺省取该模块默认列宽。低于最小列宽、超过 12、非整数或字符串均返回 400。
- `height`：整数，0–80，单位为 10px；`0` 表示高度随内容自适应，非 0 表示最小高度，内容更高时模块仍会自然增高。负值、超过 80、小数或字符串返回 400。
- 最小列宽：队内坐标/数据表现/接下来的比赛/最近战报 6 列；总体评价/生涯里程碑 8 列；代表球队/关于我/快捷入口 4 列。
- 缺失的模块会按默认顺序追加到末尾，保证始终保存完整九项；追加模块使用默认宽高。
- `milestones.metricKeys`：1–6 项、不可重复、只能使用里程碑白名单指标；缺失或空数组回退默认 6 项。
- `modules` 缺省时按默认模块保存，兼容旧客户端。
- 服务端统一输出 `version=3` / `layout=v3-modular`；客户端传入的 `layout`/`version` 不作为可信输入。
- 宽高仅影响桌面 12 列网格；`≤980px` 时前端忽略保存的宽高，按单列展示，但配置本身保留。
- `performance.viz` 必须在指标的兼容列表内；安打构成只允许 `donut`，比率/计数指标允许 `kpi` / `line` / `bar`。
- `ranks` 必须恰好 4 个、不可重复，且只能使用 `rankable=true` 的指标。
- **队内排名不提供全垒打**；默认四格为打击率、安打、上垒率、打点。

### 旧配置兼容

`version: 1` / `layout: v3-fixed` 以及 `version: 2` 的已存配置在读取时保留 `performance`、`ranks`、模块顺序与开关，自动补全模块默认宽高并返回 v3；GET 不写库，下次保存时落库。

## POST /api/account/clubhouse/config/reset

恢复默认配置，返回默认 `config`。

## 错误约定

| 场景 | 返回 |
|---|---|
| 未登录/Token 无效 | 401 |
| 当前账号未关联球员档案 | 404 |
| 指标、图表、排名数量或重复项非法 | 400 |
| 未知或重复的模块 key、非布尔 enabled | 400 |
| 模块宽度低于最小列宽/超过 12/非整数，或高度不在 0–80/非整数 | 400 |
| 里程碑指标超过 6 项、非数组、未知或重复 | 400 |
| 请求的代表球队/赛事不属于本人 | 回落到默认球队/赛事，不返回越权数据 |

## 当前限制

- 不返回球探笔记（当前无可靠数据源），前端不展示，避免造假数据。
- 生涯里程碑时间线只能从系统记录的比赛开始，不能补齐系统外历史；全量日志只在独立里程碑接口按需读取，不分页；后续数据量大时可改为 SQL 窗口累计。
- 同一赛事内若球员同时代表多支球队，球队维度统计仍以赛事统计为准；当前业务模型按“不同赛事代表不同球队”处理。
- 指标/目录标签目前为中文；多语言标签在后续统一。
- 模块宽高只在桌面端生效；手机端固定单列，不提供长按选中或尺寸手柄。
- 本轮只开放尺寸调整，不支持画布内拖动换位或自由定位；排序仍通过设置抽屉完成。桌面网格按配置顺序稀疏排布，允许留白。
- 高度以内容高度为硬下限，服务端只校验 0–80 的范围；具体内容下限由前端按实际渲染高度约束。
- 除生涯里程碑外，隐藏模块只影响展示，驾驶舱仍返回其余数据；里程碑模块隐藏时前端不发起独立请求，也不读取全量生涯日志。
