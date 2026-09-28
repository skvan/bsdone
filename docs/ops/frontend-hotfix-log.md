# 前端热修登记表（编译产物轨道 → 重建并入台账）

> 用途：前端源码重建期间（B0–B5），生产前端仍为编译产物（`bsball_project/webapps/`）。凡对编译产物的热修，
> **必须在本表登记**；重建批次开工（SOP 第 0 步）与**生产切换前**必须逐条核对并入——未并入不允许切换。
> 规则来源：`docs/PROJECT_MANAGEMENT.md`「八、前端重建专项」。

## 一、基线核验（2026-09-22）

| 项 | 结果 |
|---|---|
| 核验方式 | 仓库 `webapps/` vs 生产 bsdone.com **逐文件 sha256**（脚本 `recon-verify-mirror2.mjs`，CRLF→LF 归一化） |
| 结果 | 396 文件：完全一致 69 ／ 仅行尾差异 327 ／ **真实内容差异 0** |
| 结论 | **历史所有已生效修复均已包含在仓库产物基线中，无漏账**。重建各批以"当前编译产物"为对照基线，三比对即完成历史热修的完整并入 |
| 备注 | 本地工作区行尾为 CRLF（git autocrlf），生产为 LF；比对与重建均以"归一化内容"为准；热修或发布后请重跑核验并更新本节 |

## 二、历史条目（已回填）

| # | 时间 | 来源 | 内容 | 影响范围 | 并入方式 |
|---|---|---|---|---|---|
| H1 | 历史（仓库建立前后） | 服务器登录守护补丁（README §10.1/§11.3，timer 每日守护） | 门户登录修复（redirect 改写 `/${tenant}/admin/dashboard`、assets 登录修复标记等） | 登录相关 chunk | 已在产物基线中；B2 以对照并入 |
| H2 | 2026-09-14 | commit `1014916`（上游同步） | LiveGame「投手犯規(Balk)」按钮等；登录页/赛事列表与详情/Watch/Lineup/admin bundle 资源更新；`baseball-field.svg` | LiveGame/赛事/登录等 | 已在产物基线中；B5/B4/B2 以对照并入 |
| H3 | 2026-09-14 | commit `1478587`（上游同步） | backend isPitcher 修复配套的前端资源同步 | LiveGame/赛事详情等 | 已在产物基线中；B5/B4 以对照并入 |
| H4 | 2026-09-22 | commit `21a6ef8`（PR #41） | 官网/入口页页脚版本号 v1.0 + 去除 aDz 署名（含 `APP_VERSION` 标记） | `webapps/index.html`、`portal.html`（静态页，重建范围外） | 不适用（非 Vue 范围，无需并入） |
| H5 | 2026-09-22 | commit `93cda93`（PR #40） | 生产部署前端冒烟改 https 校验（部署脚本修复） | `scripts/deploy-prod.sh`（非前端产物） | 已完成（脚本已入库） |
| H6 | 2026-09-23 | commit `4a1e29f`（PR #50） | 修正辰寰 box score 的 IP/R 紀錄與代跑、安全進壘選項 | `LiveGame-*.js` 产物 chunk | 待并入（B5 LiveGame 批次以对照合并；不影响 B1 骨架） |
| H7 | 2026-09-23 | commit `43dee01`（PR #52） | 站点版本号升级至 v1.0.1（页脚 + `APP_VERSION` 标记） | `webapps/index.html`、`portal.html`（静态页，重建范围外） | 不适用（非 Vue 范围，无需并入）；重建 versionCheck 常量显示升级列入 AppConfig 页批次 |

## 三、待并入登记（新热修在此追加）

| # | 日期 | Issue/PR | 改动文件（webapps 内） | 功能点（简述） | 影响页面/模块 | 目标批次 | 并入证据（批次验收引用） | 状态 |
|---|---|---|---|---|---|---|---|---|
| H11 | 2026-09-25 | PR #83（commit `e59917b`，分支 `fix/livegame-runner-out-accumulate`） | `bs-ball/assets/LiveGame-Bjdd5Cir.js` + 同路径 `.js.gz` | 拖动进垒「触杀/封杀」出局不累计：ac() 中 `Bt(Gt(a,y+"出局"))` 引用同块后置声明的 `const y`（TDZ），抛 ReferenceError 导致出局数/halfInningOuts/投手 IP/守备员 PO/事件日志均未执行；修复=将 y/b 纯计算前移至函数头部 const 链（含出局累计修复） | 管理端 LiveGame（`LiveGame` chunk） | B5 LiveGame 批次（与 H6/H8/H9/H10 同轨） | 已并入 v1.1/dev（PR #83）；产物待 B5 对照重建 |
| H10 | 2026-09-24 | Issue #75（本 PR 分支 `fix/livegame-undo-relink`） | `bs-ball/assets/LiveGame-Bjdd5Cir.js` + 同路径 `.js.gz` | 撤销/重做恢复末尾按 id 重挂（relink）在垒跑者与阵容对象（`runners[1..3]`/`mvp`/`svp`/`flow.runner`），修复「撤销后跑者回本垒 R（及 SB/CS）丢统计」；恢复时同时清空待第三出局扣分登记 `Jl()` | 管理端 LiveGame（`LiveGame` chunk） | B5 LiveGame 批次（与 H6/H8/H9 同轨） | 已并入 v1.1/dev（本 PR）；产物待 B5 对照重建 |
| H9 | 2026-09-24 | Issue #71 / PR #72 | `bs-ball/assets/LiveGame-Bjdd5Cir.js` + 同路径 `.js.gz` | 本垒出局原因子菜单 + 强迫进垒确认去掉「违规」（LiveGame 实时录入） | 管理端 LiveGame（`LiveGame` chunk） | B5 LiveGame 批次（与 H6/H8 同轨） | 已并入 v1.1/dev（PR #72）；产物待 B5 对照重建 |
| H8 | 2026-09-24 | Issue #67（本 PR 分支 `fix/livegame-lineup-pitcher-sentinel-id`） | `bs-ball/assets/LiveGameLineup-CRlH9wMS.js` + 同路徑 `.js.gz` | 「確認先發陣容」頁先發投手下拉預設顯示哨兵值 `-10000`：初始化改只認「已填球員且位置為 P」的列（複用既有空槽判斷子 `K`），不再把合成槽位 id 寫進模型；未選時回落 placeholder 與「須指定後才可開始比賽」提示。附帶修好「假 P 列遮蔽真投手」 | 管理端 → 確認先發陣容（`LiveGameLineup` chunk） | B5 LiveGame 批次（與 H6 同軌） | （待並入） |
| H13 | 2026-09-26 | Issue #94 / PR #95（分支 `fix/livewatch-pitcher-dup`） | `bs-ball/assets/LiveGameWatch-BpPdtrjK.js` + 同路径 `.js.gz` | 实时观赛 Pitchers 表「投手兼打者」双行重复、Totals 双倍叠加：观赛页行构建（`buildLiveWatchPlayerStats`）对打序中位置为 P 的球员把打击行的 `isPitcher` 误标为 1（应恒为 0），与投手行双双被 Pitchers 表（仅按 `isPitcher` 过滤）收录；修复=打击行 `isPitcher` 恒 0（`C=B?.isPitcher??0`），与「一人两列」模型对齐 | 管理端 → 实时观赛（`LiveGameWatch` chunk） | B5 LiveGame 批次（与 H6/H8/H9/H10 同轨） | 已并入 v1.1/dev（PR #95，2026-09-26 合并）；产物待 B5 对照重建 |
| H14 | 2026-09-27 | Issue #106（子 #110/#111；分支 `fix/issue-106-livegame-pitcher-ip`） | `bs-ball/assets/LiveGame-Bjdd5Cir.js` + 同路径 `.js.gz` | 实时观赛「投手 IP」两处缺陷：① 快照统计集合漏收「仅守备投手」→ 观赛页显示 0（`kf()` 补齐）；② 出局 IP 归属错误（`dn()` 解析优先级）+ 换局首个出局缺「已累计」（`jr()` 兜底） | 管理端 → 实时录入 / 实时观赛（`LiveGame` chunk） | B5 LiveGame 批次（与 H6/H8/H9/H10/H13 同轨） | 待并入 v1.1/dev（本 PR）；产物待 B5 对照重建 |
| H15 | 2026-09-27 | Issue #117（分支 `feature/version-align-1.1.1`） | `bs-ball/assets/index-ByOnov1B.js`、`AdminLayout-Cx0XHwPP.js`、`PortalLayout-CBb61pt6.js`、`ServerMonitor-ssFjXFDJ.js`（各含同路径 `.js.gz`）；`bs-ball/version.json`(+gz)；`index.html`、`portal.html` | 版本号对齐 1.1.1：补齐**编译产物内共 7 处硬编码版本**（入口：`前端构建版本` / 版本检查常量 / 更新提示常量 / 设备上报 4 处；管理端设置抽屉「前端版本」；门户反馈 `clientVersion`；`ServerMonitor` 拉取 `version.json` 失败兜底），并同步 `version.json`、静态页版本与新工程源码（`AdminSettingsDrawer.js`、`PortalLayout.js`、`useVersionCheck.js`、`portalDevtoolsGuard.js`）；`APP_BUILD_TIME` 一并对齐 | 全部前端（入口页 + 门户 / 管理端 SPA） | 随下次发布（编译产物基线） | 待并入 v1.1/dev（本 PR）；产物待 B5 对照重建 |
| H16 | 2026-09-28 | Issue #131/#132/#133（分支 `fix/issue-131-133-livegame-hotfix`） | `bs-ball/assets/LiveGame-Bjdd5Cir.js` + 同路径 `.js.gz`；`bs-ball/assets/LiveGameWatch-BpPdtrjK.js` + 同路径 `.js.gz` | ① 撤销帧深拷缓存指纹改完整比对（`de()` 的 `a`/`m`），消除撤销时个人统计静默回退（#131，比赛 219 实证）；② 球队 H/E 汇总改 lineup+bench（按 id 去重，与 `kf()` 收集口径一致），修复顶部与 saveLive 的 H/E 漏替补（#132）；③ 安打推进文案修正：H2 时二垒跑者显示「回本垒得分」（读结果数组，如实反映安全/不进垒/出局）、H1 时补齐二垒跑者文案（#133） | 管理端 实时录入 / 实时观赛（`LiveGame` / `LiveGameWatch` chunk） | B5 LiveGame 批次（与 H6/H8/H9/H10/H13/H14 同轨） | 已并入重建源码（本 PR 同步修改 `bsball-frontend/src` 两文件，与产物改动逐字符对应） | 待并入 v1.1/dev（本 PR）；产物随热修部署测试环境 |
| H17 | 2026-09-28 | Issue #135（分支 `fix/issue-135-136-rbi-er-hotfix`；同分支关联 #136 ER 评估单） | `bs-ball/assets/LiveGame-Bjdd5Cir.js` + 同路径 `.js.gz` | 失误上垒不记打者 RBI（#135）：`pu()` 增加 `creditRBI` 开关（默认 true）；E 的两条路径（确认框 `dp` / 直通 `Ql`）与 FC 失误分支（`fc_choice==="0"`）均传 false——修复「防守失误造成的得分被误记打者打点」（比赛 219 场 4 下 Gytis 实证，RBI 应为 0）。ER 同源口径另见 #136（待评估） | 管理端 实时录入（`LiveGame` chunk） | B5 LiveGame 批次（与 H6/H8/H9/H10/H13/H14/H16 同轨） | 已并入重建源码（本 PR 同步修改 `bsball-frontend/src`，与产物改动逐字符对应） | 待并入 v1.1/dev（本 PR）；产物随热修部署测试环境 |

### 登记步骤（每次热修必做）

1. 修改 `bsball_project/webapps/` 的 PR 中，勾选 PR 模板对应项（"已向 frontend-hotfix-log.md 登记"）；
2. 在"三、待并入登记"表追加一行（注明目标批次）；
3. 热修随"下一版本/发布"上线后，如与登记信息有出入（如实际部署了额外文件），在本表补充说明；
4. 对应重建批次开工时（SOP 第 0 步）逐条核对：**并入后**将状态改为"已并入（批次 + 验收记录指针）"。
