# 赛事特殊规则实体（EventSpecialRule）

## 简介

赛事级特殊规则开关（Issue #275，2026-10-24 口径调整）：`extra_batter`（EP 制）与 `force_half`（强制换局）。
语义：**行存在 = 以记录为准；无记录 = 默认值（extraBatter=false、forceHalf=true）**。

是否允许配置由两层决定：

1. 联赛收费能力 `bs_league_special_rule.special_rules_enabled`（租户管理员/超管在联赛编辑弹窗开启）；
2. 赛事已有规则（能力关闭后已配置赛事保留，仍可继续使用/编辑）。

## 结构

- 实体：`EventSpecialRule`（继承 BaseEntity）
- 数据访问：`EventSpecialRuleRepository`（findByEventId / findByEventIdAndRuleKey / existsByEventId）
- 服务：`EventSpecialRuleService`（默认值兜底、按行 upsert、hasRules）
- 装配：`EventService.get` 返回 `specialRules:{specialRulesEnabled,hasEventRules,extraBatter,forceHalf}`；创建/更新在同一事务写入
- 迁移：`V20261025100000__event_special_rule.sql`

## 表 bs_event_special_rule

| 字段 | 说明 |
|---|---|
| tenant_id | 租户ID |
| event_id | 赛事ID（逻辑关联 bs_event.id） |
| rule_key | `extra_batter` / `force_half` |
| enabled | 是否启用 |
| 审计字段 | created_by/created_at/updated_by/updated_at/deleted_by/deleted_at |

唯一约束：`(event_id, rule_key)`（uk_bs_event_special_rule）。

## 取值与门禁

- 默认：extraBatter=false、forceHalf=true。
- 赛事详情同时返回：
  - `specialRulesEnabled`：联赛收费能力是否开启；
  - `hasEventRules`：赛事是否已有显式规则行；
  - `extraBatter` / `forceHalf`：实际生效值。
- 创建赛事携带规则：要求赛事挂联赛且该联赛收费能力开启，否则 400。
- 更新赛事携带规则：赛事已有规则（hasEventRules）时允许（能力关闭也保留）；否则要求联赛收费能力开启，否则 400。
- 写权限沿用赛事编辑接口与 `business:event:edit`；联赛能力的读写由 `business:league:specialRule` 控制。

## 存量迁移

- 旧 `bs_league_special_rule` 的 `extra_batter` / `force_half` 行复制到该联赛下每个赛事（幂等、只复制显式行）；
- 对存在旧规则行的联赛写入 `special_rules_enabled=true`（自动开启收费能力，保持现状）；
- 旧联赛表保留；旧联赛实际规则接口标记 deprecated。

> 本文以代码为准（EventSpecialRule.java / EventSpecialRuleService.java / EventService.java / V20261025100000__event_special_rule.sql）。
