# 赛事积分榜与对阵 API

本文记录 Issue #665「赛事赛果、积分榜与对阵图」的对外接口与计算口径，以代码为准。

## 一、公开只读接口

### GET /event/{eventId}/tournament

返回指定赛事的积分榜、赛果比分、淘汰赛对阵与提示信息。门户赛事详情页使用。

**路径参数**

| 参数 | 类型 | 说明 |
|---|---|---|
| eventId | Long | 赛事 ID |

**响应 `Result<TournamentViewDTO>`**

```json
{
  "eventId": 1,
  "eventName": "2026 花城联盟春季赛",
  "season": "2026春",
  "formatType": "GROUP_KNOCKOUT",
  "pointsRule": { "win": 2, "draw": 1, "loss": 0 },
  "standings": [
    {
      "stage": "GROUP",
      "groupCode": "A",
      "groupName": "A 组",
      "advanceCount": 2,
      "rows": [
        {
          "rank": 1,
          "teamId": 1,
          "teamName": "广州暴力羊",
          "teamLogo": "/files/team/1.png",
          "played": 3,
          "wins": 3,
          "losses": 0,
          "draws": 0,
          "points": 6,
          "winPct": 1.0,
          "gamesBehind": 0.0,
          "runsFor": 20,
          "runsAgainst": 6,
          "runDiff": 14,
          "qualified": true
        }
      ],
      "matches": [
        { "gameId": 11, "homeTeamId": 1, "homeScore": 5, "awayTeamId": 2, "awayScore": 2 }
      ]
    }
  ],
  "bracket": {
    "rounds": [
      {
        "code": "SF",
        "name": "半决赛",
        "matches": [
          {
            "slot": 1,
            "gameId": 31,
            "gameNumber": 31,
            "homeTeamId": 1,
            "homeTeamName": "广州暴力羊",
            "homeScore": 5,
            "awayTeamId": 6,
            "awayTeamName": "佛山闪电",
            "awayScore": 2,
            "status": "final",
            "winnerTeamId": 1
          }
        ]
      }
    ],
    "championTeamId": 1,
    "thirdPlace": null
  },
  "warnings": []
}
```

**字段说明**

- `formatType`：`SINGLE_ROUND_ROBIN` / `DOUBLE_ROUND_ROBIN` / `KNOCKOUT` / `GROUP_KNOCKOUT`。
- `standings`：单/双循环只有一张 `stage=ROUND_ROBIN` 表；小组赛每个组一张 `stage=GROUP` 表；纯淘汰赛为空。
- `rows`：按当前排名排序；`winPct = W / (W + L)`（平局不计入分母）；`gamesBehind` 以榜首为基准；小组赛 `qualified` 标记前 `advanceCount` 名。
- `matches`：该积分表对应的已完赛比分，前端用来生成“行队在左、列队在右”的对称赛果矩阵；单循环每个对阵 1 条，双循环每对 2 条。
- `bracket`：纯淘汰赛/小组+淘汰赛返回；`rounds` 不含三四名，三四名单独在 `thirdPlace`。
- `warnings`：例如“有 N 场已结束比赛缺少比分，未计入积分榜”。

**错误**

- `404`：赛事不存在、已删除或当前租户不可见。
- `403`：数据范围不允许查看该赛事。

## 二、积分计算口径

1. 仅统计 `status=final` 且主客队比分都非空的比赛；`scheduled/live/postponed/cancelled` 不计入。
2. 积分：赛事级 `pointsWin / pointsDraw / pointsLoss`，默认 2/1/0；创建积分型赛事时可选 2-1-0 或 3-1-0。
3. 排名：积分降序 → 相互战绩（同分队之间的小积分表）→ 相互净胜分 → 总净胜分 → 总得分 → 总失分少 → 球队 ID 稳定排序。
4. 双循环：两回合所有已完赛比赛都计入，不做特殊处理。
5. 小组赛：小组赛阶段（`stage_code=GROUP` 或未填阶段）按组统计；淘汰赛阶段不计入小组积分。
6. `matches` 与 `rows` 使用同一批可计入比赛，保证赛果矩阵与积分一致。

## 三、淘汰赛口径

- 阶段代码：`R16`（1/8 决赛）、`QF`（1/4 决赛）、`SF`（半决赛）、`FINAL`（决赛）、`THIRD_PLACE`（三四名）。
- 轮次顺序按上述固定顺序输出；同轮按 `bracket_slot`、`game_number`、`id` 排序。
- 胜者：比赛已结束、比分非空且不相等时，比分高者；平局或缺比分不判定胜者。
- 冠军：决赛胜者；三四名：三四名决赛胜者。
- 前端树状图按轮次列 + 连接线展示，移动端可横向滚动。

## 四、后台配置字段

### 赛事（`bs_event`）

| 字段 | 说明 |
|---|---|
| format_type | 赛制 |
| points_win / points_draw / points_loss | 积分制 |
| group_count | 小组数（GROUP_KNOCKOUT） |
| advance_per_group | 每组晋级数（GROUP_KNOCKOUT） |
| third_place | 是否设置三四名 |
| bracket_size | 单败淘汰签表规模 |

创建/更新赛事的 `Event` 请求体携带上述字段；`EventService` 负责默认值与合法性校验。编辑时缺省字段沿用存量值。

### 比赛（`bs_game`）

| 字段 | 说明 |
|---|---|
| stage_code | 阶段；单/双循环会被清空 |
| group_code | 小组组别（小组赛） |
| round_no | 双循环回合 / 小组赛轮次 |
| bracket_slot | 淘汰赛同轮内对阵位 |

`GameService` 按赛事赛制校验阶段与组别；单/双循环不使用阶段字段。

### 参赛球队分组

```http
PUT /event/{eventId}/teams/{teamId}/group
Content-Type: application/json

{ "groupCode": "A" }
```

- `groupCode` 为空表示清除分组。
- 需具备赛事管理权限；组别最长 16 字符。

## 五、数据库迁移

`V20261113120000__event_tournament_format.sql`：

- `bs_event` 新增赛制、积分制、小组、三四名、签表规模字段；
- `bs_game` 新增阶段、组别、轮次、对阵位；
- `bs_event_team` 新增组别；
- 新增 `(event_id, stage_code, group_code)` 与 `(event_id, group_code)` 索引。

存量赛事默认 `SINGLE_ROUND_ROBIN` + 2-1-0，无需补录即可生成积分榜。
