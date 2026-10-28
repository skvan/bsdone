# 球员批量导入：外部辅助工具与接口契约

> 本文描述**仓库外**的一个 Python 批量导入工具，以及它与后端接口的对接契约。
> 目的：当球员资料以「千行计」时，避免在管理台逐笔点击；同时把三个会让人「以为工具坏了」的坑写清楚。
>
> **工具本体不在本仓库**（本仓库目前没有任何 `.py` 文件）。是否收进来由维护者决定，见文末。

## 适用场景

管理台「球员管理 → 新增」是一笔一笔填的。当一支球队要一次建 50～100 人、或整个租户要灌入数百名球员时，逐笔录入不实际。此工具从 Excel 读取花名册，校验后调用后端批量接口写入。

## 工具构成

| 文件 | 行 | 作用 |
|---|---|---|
| `player_import_tool.py` | 521 | 桌面 GUI 版（Tkinter），无头像支持 |
| `player_import_web.py` | 355 | 本地 Web 版（`http.server` + 浏览器界面），**支持头像** |
| `webui.html` | 369 | Web 版界面 |
| `generate_template.py` | 244 | 产生带说明页与下拉校验的 Excel 模板 |
| `啟動工具.command` | 23 | macOS 双击启动（首次自动 `pip install --user -r requirements.txt`） |
| `build_exe.bat` + `player_import.spec` | 71 / 48 | Windows 用 PyInstaller 打包成 `dist/player_import.exe` |

依赖只有两个：`openpyxl>=3.1.0`、`requests>=2.28.0`。预设服务地址 `https://bsdone.com/bsball-server`。

## 接口契约

三个接口，均已对照 `v2.3/dev` 源码验证存在且形状吻合：

| 步骤 | 接口 | 后端定义 |
|---|---|---|
| 登录 | `POST /auth/login`，body `{username, password}` → `data.token` | `AuthApi.java:74` |
| 取球队 ID 清单 | `GET /team/list` | `TeamApi.java:54` |
| 批量导入 | `POST /player/import`，body `{"items":[…Player…], "duplicateStrategy":"overwrite"|"skip"}` → `{created, updated, skipped}` | `PlayerApi.java:163`、`PlayerService.java:1086` |

- 请求体类型是 `record PlayerImportRequest(List<Player> items, String duplicateStrategy)`，也就是 **items 直接反序列化成 `Player` 实体**，字段名必须用实体属性名。
- `duplicateStrategy` 判定为 `"overwrite".equalsIgnoreCase(...)`，其余一律视为跳过（`PlayerService.java:1108`）。
- 回传只有 `created / updated / skipped` 三个计数。**`name` 为空白的行会直接算进 `skipped`**（不报错、不写库）。
- 实际落库写两张表：`bs_player`（`Player` 是 `@Entity @Table(name="bs_player")`）与球员-球队关联表 `bs_player_team`——每笔新建或覆盖后都会 `syncLegacyEntry()` 重算关联并 create/update 关联行（不删除既有经历）。
- 后续三个请求都带 `Authorization: Bearer <token>`；导入落在**该账号自己的租户上下文**（`batchImport` 取 `tenantQueryPolicyService.requiredTenantId()`），跨租户的 `teamId` 会被一致性校验拒绝。

字段对应（Excel 中文标题 → `Player` 属性，21 项）：

```
姓名→name  背号→number  球队ID→teamId  简称→shortName  英文名→nameEn  昵称→nickname
守备位置→positions  出生日期→birthDate  出生地→birthPlace  身高→height  体重→weight
投球惯用→throwHand  打击惯用→batHand  选秀→draft  首秀→debut  学历→education
状态→status  联络电话→contactPhone  联络Email→contactEmail  简介→intro  排序→sort
```

必填：`name`（空白行会计入 `skipped`）。`number`、`teamId` 没有 DTO 级必填校验；但受限身份建档时 `teamId` 不能为空且必须是其可管理的球队，否则会被 `assertCanCreateUnclaimedPlayer` 以 403 拒绝。注意 `birthDate / height / weight` 在实体上是 **String**（不是日期/数字），`sort` 是 `Integer`、`teamId` 是 `Long`，`intro` 上限 2000 字。

## 坑一：登录验证码默认开启（会让工具直接登不进去）

`/auth/login` 会先调用 `authCaptchaService.validateLoginCaptchaIfNeeded(tid, body)`（`AuthApi.java:90`）。租户验证码**默认为开启**：

```yaml
# bsball-backend/src/main/resources/application.yml:185-187
      enabled: ${APP_AUTH_CAPTCHA_ENABLED:true}
      # 验证码类型：input（字符输入）/ click（点选）/ drag（拖拽）/ random（每次在 click、drag 中随机）
      type: ${APP_AUTH_CAPTCHA_TYPE:random}
```

而工具只送 `{username, password}`、不带 `captchaId`，于是被 `UnauthorizedException("请输入验证码")` 挡下（`AuthCaptchaService.java:295-305`）。**GUI 与 Web 版都还没有实现验证码**，这不是用法问题。

先确认目标站点当前状态：

```bash
curl -s '<server>/auth/captcha/options'   # 看 enabled 是 true 还是 false
```

三种走法，择一：

1. 在导入用的环境把验证码关掉：环境变量 `APP_AUTH_CAPTCHA_ENABLED=false`，或该租户在「配置管理」把 `authCaptchaEnabled` 存为 false（库内配置会覆盖 yml 默认）。**导入完记得恢复。**
2. 用带验证码的正式流程：`/auth/captcha/image` 出题、`/auth/captcha/verify-click` 或 `/auth/captcha/verify-drag` 人机验证通过后，把响应里的 `verifyToken` 作为 `captchaVerifyToken`，连同 `captchaId` 一起提交登录——需要人工互动，目前得改工具才支持。
3. 只在测试租户/测试环境使用，不动正式租户配置。

## 坑二：去重键已经不是「姓名 + 背号」

`batchImport` 的去重口径已在批次 3b（spec §6.9）改为**对齐自助建档**：

> 同租户 `name` + `birthDate`（仅两者皆非空时才参与去重）

因此：

- **`birthDate` 留空 → 该笔不参与去重 → 每次都当新球员建立**，重跑一次就多一批重复人。批量导入前务必把出生日期填满。
- 同名但生日不同 → 视为两人（旧口径会视为同一人）。
- 工具产生的 Excel 模板说明页与界面提示仍写着「姓名+背号作为重复判断依据」，**这段说明已过时**，以本节为准。

`duplicateStrategy` 的 skip / overwrite 是作用在这个新键上的。

## 坑三：`overwrite` 是整列覆写，不是局部更新

`PlayerRepository extends JpaRepository<Player, Long>`，且 `Player` **没有 `@DynamicUpdate`**。覆盖分支做的是：

```java
p.setId(existing.getId());
p.setCreatedAt(existing.getCreatedAt());
playerRepository.save(p);      // JPA merge → 全部栏位一起写回
```

`p` 是由 Excel 行直接反序列化出来的 `Player`，**Excel 里没填的属性就是 null**，于是 merge 会把这些栏位一并写成 null；实体自带默认值的栏位则会被重置成默认——`status`（默认 `"active"`）、`sort`（默认 `0`）与 `platformOwned`（默认 `false`）。因此 `userId` 等未填栏位会被清空，平台归属标记也会被重置。

所以：

- 用 `overwrite` 之前，Excel 必须是该球员的**完整数据**，不能只填「要改的那几栏」，否则会把身高、学历、联络方式等清空、把状态重置成 `active`。
- 想只改一两个栏位，走管理台单笔编辑，不要用批量导入的 overwrite。
- 保险做法：先对同一批数据用 `skip` 跑一次确认去重命中符合预期，再决定是否真的要 overwrite。

## 权限面

`/player/import` 包含在 `team_manager` 的角色谓词内（`InitDataRunner.java:1330-1353`），球队管理员可用。另外每次建立未认领球员会过 `resourceGuard.assertCanCreateUnclaimedPlayer(teamId)`：不受限身份可建无队球员；受限身份必须提供其可管理的球队，`teamId` 为空或越权时返回 403。

## 建议的验收顺序

1. 先 `curl '<server>/auth/captcha/options'` 确认验证码状态，必要时按坑一处理。
2. 用 `generate_template.py` 产生模板，**填满 `出生日期`**。
3. 先只放 **3 笔**导入，核对回传的 `created/updated/skipped` 与管理台球员列表（含守备位置、投打惯用、状态是否正常显示）。
4. 再导一次同样 3 笔、策略选 `skip`，应得到 `created=0`、`skipped=3`——这一步是在验去重键是否如预期生效。
5. 确认无误后才上量。要局部修正字段时**不要**用 `overwrite`（见坑三，它是整列覆写），改用管理台单笔编辑。

## 与本仓库统计链路无关

本工具只写 `bs_player` 与 `bs_player_team`，不涉及 `bs_game_player_stat`，与比赛录入/统计（例如 #387 那条投手统计链路）互不影响。

## 如果要收进仓库

仓库现在 0 个 `.py`，`scripts/` 下只有 `.mjs` / `.sh` / `.ps1`。收进来需要先决定：放 `tools/player-import/` 还是 `scripts/`、是否引入 Python 依赖与 CI 检查、Excel 模板是否改由前端导入页取代。本文件只提供契约与坑位，**不含工具本体**；若希望改为正式内建功能，建议另开 issue 讨论「前端 Excel 导入」方案，那样就不需要外部工具与关验证码这条 workaround。
