# 个人驾驶舱 API

个人驾驶舱（V3 球星年鉴）的模块化数据接口。支持九个业务模块的显示/隐藏、排序与桌面端宽高调整，以及「表现图」与「队内排名」的指标/图表选择。宽高在服务端白名单范围内校验；手机端忽略宽高，按单列展示。

数据范围默认是**全部球队 + 生涯**（当前运动类型下跨全部代表球队、全部赛事），可通过 `gameMode` / `teamId` / `eventId` 切换到具体运动类型、球队或赛事。球队与赛事相互独立，不自动互锁。

## 认证与前置条件

- 请求头：`Authorization: Bearer <token>`。
- 当前账号必须已认领球员档案（`bs_player.user_id = 当前用户`），否则返回 404。
- 门户会员默认角色可通过 `/account/*` 自助端点访问；不需要管理台菜单权限。

## GET /api/account/clubhouse

聚合驾驶舱数据。可选参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| `teamId` | Long | 可选；代表球队。不传、非法、不属于本人，或在生效运动类型下没有已确认赛事时按「全部球队」处理。 |
| `eventId` | Long | 可选；赛事。不传、非法或不属于当前球队（全部球队时为任一代表球队）的已确认赛事时按「生涯」处理。 |
| `gameMode` | String | 可选；`BASEBALL` / `SOFTBALL`，忽略大小写与首尾空白，非法值忽略。优先级：请求参数 → 所选赛事的运动类型 → `BASEBALL`。 |

- 球队与赛事相互独立：只传 `eventId` 时 `teamId` 仍为全部球队；选择球队不会自动选中该队第一个赛事。
- 同一球员在同一赛事代表多支球队时，`events[]` 只出现一次，`teamIds` / `teamNames` 列出全部代表球队。

返回 `Result<Map>`，主要字段：

- `player`：本人档案安全视图（姓名、英文名、背号、守位、头像、身高/体重、投打、简介等；不返回联系方式、userId、tenantId）。
- `teams[]`：当前全部代表球队（不按运动类型过滤，前端按 `events[].gameMode` 过滤）；含 `events[]`、`primaryEventId`、`primaryEventName`、`standing`（积分榜名次/胜负/胜率/近况）。
- `events[]`：赛事下拉数据源，只返回当前 `gameMode` 下该球员有比赛记录、且属于当前范围球队已确认赛事的赛事。
  - `id`、`name`、`status`、`startDate`、`endDate`、`gameMode`。
  - `teamIds[]` / `teamNames[]`：该球员在该赛事代表的球队，两个数组按球队 ID 升序一一对应；多队赛事合并为一条。
  - `lastGameday`：该球员在该赛事的最后一场比赛日期（`yyyy-MM-dd`，仅计入排行的比赛）；已选球队时取该队在该赛事的最后一场；无比赛为 `null`。
  - `gameCount`：该球员在该赛事的比赛场次。
  - 排序：`lastGameday` 倒序，`null` 排最后；同日期按赛事名升序。
- `scope`：本次数据口径 `{ teamId, teamName, eventId, eventName, gameMode, teamAll, career }`。
  - `teamId=null` 表示全部球队，`teamName` 固定为 `全部球队`；`eventId=null` 表示生涯，`eventName` 固定为 `生涯`。
  - `teamAll = (teamId == null)`；`career = (eventId == null)`。
- `stats`：所选范围下的 `batting` / `pitching` / `fielding` 统计 Map（仅本人数据）。
  - 全部球队 + 生涯：当前运动类型下跨全部代表球队、全部赛事的合计。
  - 全部球队 + 赛事：该赛事内该球员代表所有球队的合计。
  - 球队 + 生涯：该球队跨赛事的合计。
  - 球队 + 赛事：该球队在该赛事的合计，不使用赛事总合计代替。
  - 球队维度只统计球员在该队出场的比赛；四种范围均只计入排行的比赛（`include_stats_in_ranking`）。
  - 零数据契约：球队维度查询返回完整字段（计数为 `0`、比率类为 `"0"`、投手 `goFo` 为 `"-"`）；全部球队 + 赛事无个人统计时返回三个空 Map，某领域无数据时该领域为空 Map。前端按 `gp` / `ab` / `pa` / `ip` 判断空态，不把 `0` / `0.000` 当成真实表现展示。
- `config`：当前用户配置（version 3；`modules[]` 含 `width`/`height`，默认见 PUT 示例）。
- `moduleCatalog[]`：模块白名单；含 `key`、`label`、`span`（默认列宽）、`minSpan`（最小列宽）、`maxSpan`（固定 12），共 9 个模块。
- `milestoneCatalog[]`：生涯里程碑指标白名单；含 `key`、`label`、`shortLabel`、`domain`、`unit`、`defaultSelected`、`thresholds[]`。
- `catalog[]`：指标白名单；含 `key`、`label`、`domain`、`viz[]`、`rankable`、`available`。
- `performance`：`{ metricKey, label, viz, domain, points[], summaryValue, items[], available }`。
  - `points`：按当前球队/赛事范围先取最近 30 场计入排行的比赛，过滤掉该指标无出场的场次后，保留最近 12 场并按比赛日升序；每点含 `label`、`value`、`gameday`、`eventId`。
  - `summaryValue`：取自当前范围的 `stats`；`available=false` 时 `points` 为空。
  - `items`：仅安打构成（环形图）使用。
- `ranks[]`：四个队内排名位；含 `metricKey`、`label`、`domain`、`value`、`rank`、`total`、`barPct`、`available`。
  - `teamId=null`（全部球队）时返回空数组，前端不渲染「队内坐标」模块。
  - 球队 + 生涯：该球队跨赛事排行榜（`eventId=null`、按球队过滤、不启用最低打席资格）。
  - 球队 + 赛事：该球队在该赛事的排行榜。
  - `barPct = round(100 × (total − rank + 1) / total)`；本人不在榜单时不产出 `rank`。
- `evaluation`：总体评价数据，固定为**全租户生涯**口径：对比池为当前租户、同一运动类型、有有效生涯数据的球员，每个分项取排行榜前 500 名（上限 500 人），不跨租户、不混棒球/垒球。雷达与总体评分**不随顶部球队/赛事筛选变化**；本对象始终返回三张六轴卡与 `overall` 对象。
  - `available`：是否存在可用的总体评分（`overall.score != null`）。
  - `comparison`：比较口径，固定为 `全租户生涯`。
  - `minSample`：各分项展示门槛 `{ batting: 10, pitching: 3, fielding: 6 }`，单位分别为 PA / IP / 守备局。
  - `cards[]`：固定三张，顺序为 `batting`、`fielding`、`pitching`；未达展示门槛的卡 `available=false`，仍返回完整卡片骨架：`axes: []`、`score: null`、`grade: null`、`sampleSize: 0`、`ownSample: 0`、`reliability: 0.0`，并以 `reason` 说明原因。
    - `domain`：`batting` / `fielding` / `pitching`。
    - `title` / `subtitle`：卡片标题与指标缩写。
    - `available`、`reason`：`available=false` 时给出原因（如「暂无打击数据」「打击样本不足（至少 10 PA）」）。
    - `score` / `grade`：分项评分（0–100 整数，已按可靠性向 50 收缩）与等级 `S` / `A+` / `A` / `B+` / `B` / `C+` / `C` / `D`。
    - `sampleSize`：**对比池人数**（参与百分位的球员数），不是本人样本量。
    - `ownSample` / `sampleUnit`：**本人样本量**与单位；打击为整数 PA，投球与守备为十进制局数（保留 1 位小数），单位分别为 `PA` / `IP` / `INN`。
    - `reliability` / `confidenceLabel`：0–1 可靠性系数（保留 2 位小数）与文案 `样本充足` / `参考值` / `样本不足`。
    - `axes[]`：`key`、`label`、`abbr`、`value`（格式化后的原始值）、`percentile`（0–100 全租户生涯百分位）。并列取平均名次；单样本给 50；空池给 0。
    - `note`：口径提示。打击固定为 `参考 wOBA`，投球固定为 `参考 FIP`；守备仅在回退全体守备池时为 `未按守位分组`（`P` / `DH` 组当前必然回退）。
    - `meta`：打击 `{ woba }`（3 位小数）；投球 `{ fip, fipConstant }`（`fip` 与租户常数 `C` 均为 2 位小数）；守备无 `meta`。
  - 打击轴（6 轴，均越高越好）：`AVG`、`OBP`、`SLG`、`OPS`、`RBI`、`SB`；展示门槛 10 PA，稳定门槛 50 PA。
  - 守备轴（6 轴，均越高越好）：`FPCT`、`TC/G`、`PO/G`、`A/G`、`DP/G`、`RF/9`；展示门槛 6 守备局，稳定门槛 30 守备局。优先在同守位组（C / 1B / INF / OF / DH / P）内比较，组内有效球员不足 5 人时回退全体守备池并标注 `未按守位分组`；守备排行榜查询排除 `P` / `DH` / `PH`，因此 `P` / `DH` 组的池恒为空、当前必然回退。`IF`（内野）与 2B / 3B / SS 同归 `INF` 组。
  - 投球轴（6 轴）：`ERA`、`WHIP`、`K/9`、`BB/9`、`HR/9`、`IP`；`ERA` / `WHIP` / `BB/9` / `HR/9` 越低越好，服务端已反向换算百分位；`IP` 越高越好。展示门槛 3 IP，稳定门槛 15 IP。`IP` 展示保留棒球记法（如 `5.2` 表示 5⅔ 局），百分位与阈值按十进制局数换算。
  - wOBA / FIP **只作为分项质量项**，不进入雷达轴：打击评分权重 wOBA 0.50 / OBP 0.20 / SLG 0.15 / RBI 0.10 / SB 0.05；投球评分权重 FIP 0.50 / K/9 0.20 / BB/9 0.15 / WHIP 0.15；守备六轴权重 FPCT 0.30 / TC/G 0.15 / PO/G 0.15 / A/G 0.15 / DP/G 0.10 / RF/9 0.15。
  - 算法细节（百分位、wOBA/FIP 公式、角色权重、收缩与等级）见 [个人驾驶舱评价算法](../个人驾驶舱评价算法.md)。
  - `overall`：总体评分对象。
    - `score` / `grade`：按角色权重合并各分项评分后的 0–100 整数与等级；无任何可用分项时为 `null`。
    - `reliability` / `confidenceLabel`：按分项权重加权的总体可靠性（保留 2 位小数）与文案。
    - `role`：角色 `POSITION_PLAYER`（野手）/ `PITCHER`（投手）/ `TWO_WAY`（二刀流）。投球达到展示门槛才计为投手/二刀流；二刀流还需打击达展示门槛且本人 PA ≥ 50、IP ≥ 15。
    - `weights`：实际参与评分的分项权重（已归一化，保留 3 位小数）。野手 0.55 / 0.45（打击/守备）；投手 0.70 / 0.15 / 0.15 或 0.80 / 0.20（无打击时）；二刀流 0.40 / 0.40 / 0.20（打击/投球/守备）。
    - `sampleSize`：对象 `{ batting, pitching, fielding }`，为各分项**本人样本量**（PA / IP / 守备局），用于前端展示「本人 PA · IP · 局」；投球、守备为未四舍五入的原始十进制局数，可能与卡片 `ownSample`（保留 1 位小数）不同。某分项不可用（未达展示门槛或无数据）时该字段固定为 `0`，不代表真实样本量。
- 生涯里程碑数据不在驾驶舱首屏返回；前端仅在里程碑模块启用时调用 `GET /api/account/clubhouse/milestones` 按需加载，字段见下文独立接口。
- `nextGame` / `schedule[]`：当前球队/赛事范围内的未开赛或进行中比赛，按日期/时间升序，最多 6 场。
  - 选了球队只查该队；全部球队时查全部代表球队。
  - 选了赛事只保留该赛事；全部球队 + 赛事时包含该球员在该赛事代表的所有球队，逐场返回实际代表球队。
  - 赛事的运动类型必须与当前 `gameMode` 一致。
- `recentGames[]`：当前球队/赛事范围内最近 3 场个人比赛记录（先过滤再取 3 场），逐场返回实际代表球队；不受「仅计入排行的比赛」限制。

数据口径：

1. 范围解析：`teamId` 非法或不属于本人时回落全部球队；`eventId` 非法或不在当前球队（全部球队时为任一代表球队）的已确认赛事内时回落生涯。只选赛事不改变球队；选择球队时仅当当前赛事不在新范围才回落生涯。球队与运动类型一致：生效运动类型下该球队没有已确认赛事时，球队与赛事一起回落全部球队/生涯（与前端球队选项口径一致）。
2. 运动类型：请求参数优先，其次所选赛事，最后默认棒球；非法值忽略，赛事未设置运动类型时按棒球处理。统计、赛事下拉、表现、战报、赛程均只取该运动类型的数据。
3. 顶部统计：按四种范围取数——全部球队 + 生涯为跨队跨赛事合计；全部球队 + 赛事为该赛事跨队合计；球队 + 生涯为该队跨赛事合计；球队 + 赛事为该队在该赛事的合计（不得用赛事总合计代替）。球队维度按 `bs_game_player_stat.team_id` 过滤，只统计球员在该队出场的比赛。
4. 队内排名：全部球队不计算；球队 + 生涯使用该队跨赛事榜单；球队 + 赛事使用该队在该赛事的榜单。榜单不启用最低打席资格限制，按有统计记录的球员计算。
5. 表现图：先按范围过滤再取最近 30 场计入排行的比赛，剔除该指标无出场的场次后保留最近 12 场。
6. 最近战报：先按球队/赛事过滤，再取最近 3 场。
7. 赛程：取当前范围球队在已确认赛事中的 `scheduled` / `live` 比赛，比赛日不早于今天，且赛事类型与 `gameMode` 一致；按日期/时间升序取前 6 场。
8. 投球指标仅在有投球局数时标记可用；打击指标仅在有打席/打数时可用。
9. 总体评价：固定为全租户生涯口径，取当前租户、同一运动类型的排行榜前 500 名作为对比池（打击按 PA、投球按 IP、守备按守备局降序），不随球队/赛事筛选变化。打击展示门槛 10 PA、守备 6 局、投球 3 IP；稳定门槛分别为 50 PA、30 局、15 IP，未达稳定门槛时评分向 50 收缩并标注「参考值」。各轴百分位按池内名次计算，ERA/WHIP/BB/9/HR/9 等反向指标先取反；守备优先同守位组，组内不足 5 人回退全体（`P` / `DH` 必然回退）。最终按角色权重合成总体评分（0–100 + 等级）。
10. 生涯里程碑：前端仅在 `milestones` 模块启用时按需请求独立接口；全租户生涯，跨全部赛事与代表球队；只汇总计入排行的比赛日志，且不按棒球/垒球模式过滤。累计与时间线使用同一份日志；时间线从系统记录的最早比赛开始，不能补齐系统外历史。

## GET /api/account/clubhouse/milestones

按需加载全租户生涯里程碑。仅当前账号已认领球员档案、且前端启用里程碑模块时调用。

返回 `Result<Map>`：

- `available`：当前是否有可展示的累计指标。
- `scopeLabel`：固定为 `全租户生涯 · 跨赛事与球队`。
- `note`：统计口径说明。
- `reachedCount`：已选指标已达成里程碑总数。
- `items[]`：`metricKey`、`label`、`shortLabel`、`domain`、`unit`、`value`、`nextTarget`、`remaining`、`progressPct`、`reachedCount`。
- `timeline[]`：最近最多 20 项，按比赛日倒序（最新在前），含 `metricKey`、`title`、`gameday`、`eventName`、`opponentName`、`teamName`、`gameId`。
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
| 请求的代表球队/赛事不属于本人，或赛事不属于当前球队 | 回落到全部球队/生涯，不返回越权数据 |

## 当前限制

- 不返回球探笔记（当前无可靠数据源），前端不展示，避免造假数据。
- 生涯里程碑时间线只能从系统记录的比赛开始，不能补齐系统外历史；全量日志只在独立里程碑接口按需读取，不分页；后续数据量大时可改为 SQL 窗口累计。
- 球队维度统计按 `bs_game_player_stat.team_id` 过滤；同一赛事代表多支球队时，「球队 + 赛事」只算该队，「全部球队 + 赛事」为该球员在该赛事的跨队合计，两者不相等。
- `events[]` 只包含当前运动类型下该球员有比赛记录的已确认赛事；仅确认报名、尚无出场的赛事不会出现在赛事下拉中。
- 总体评价/雷达固定为「全租户生涯」，不随球队/赛事筛选变化；每个分项对比池上限 500 人，超出部分不参与百分位。评分依赖系统内已记录的比赛，系统外历史无法补齐。
- 评价算法无球场/联盟校正，wOBA 使用固定业余权重，FIP 常数为租户级近似值；两者均标注「参考」，且不进入雷达轴。`P` / `DH` 守备组当前必然回退到全体守备百分位并标注「未按守位分组」（守备排行榜排除 `P` / `DH` / `PH`）。评分实时计算、无缓存与历史快照，详见 [个人驾驶舱评价算法](../个人驾驶舱评价算法.md)。
- 指标/目录标签目前为中文；多语言标签在后续统一。
- 模块宽高只在桌面端生效；手机端固定单列，不提供编辑入口、选中框或尺寸手柄。
- 桌面布局编辑通过「编辑布局」按钮进入；进入后单击模块选中，再拖动尺寸手柄或工具栏「移动」手柄调整大小/顺序。不支持自由定位、重叠或跨栏吸附。排序也可在设置抽屉用上移/下移完成。桌面网格按配置顺序稀疏排布，允许留白。
- 高度以内容高度为硬下限，服务端只校验 0–80 的范围；具体内容下限由前端按实际渲染高度约束。
- 除生涯里程碑外，隐藏模块只影响展示，驾驶舱仍返回其余数据；里程碑模块隐藏时前端不发起独立请求，也不读取全量生涯日志。
- 队内坐标模块在「全部球队」时不渲染，但配置中的开关、顺序与宽高会保留；数据设置抽屉仍显示该项并提示“仅在选择球队后显示”。
- `nextGame` / `schedule[]` 在每种范围内上限为 6 场；所选范围内第 7 场及以后的比赛不会返回。范围固定后不再混入其他赛事，前端按范围显示空态。
