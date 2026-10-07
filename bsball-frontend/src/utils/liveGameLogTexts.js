// 日志文本生成器 v1（组/行模型）——规范 v1.0 §2.1
// 约定：纯函数；行主体一律「{位置/角色} {姓名} (#背号)」（无背号省略 (#N) 段）；
// 出局累计仅附着于产生出局的该行行尾：，本棒次X人出局，已累计Y人出局
// 「第N棒 」前缀由 LiveGame 的 addLog 滤镜按「{打者名}:」行首注入（打者行用朴素名+冒号）

export const POS_CN = {
  P: "投手", C: "捕手", "1B": "一垒手", "2B": "二垒手", "3B": "三垒手", SS: "游击",
  LF: "左外野手", CF: "中外野手", RF: "右外野手",
  1: "投手", 2: "捕手", 3: "一垒手", 4: "二垒手", 5: "三垒手", 6: "游击", 7: "左外野手", 8: "中外野手", 9: "右外野手"
};

export const BASE_CN = { 1: "一垒", 2: "二垒", 3: "三垒", 4: "本垒" };

/** 名 (#N)；缺名/缺号安全降级（与 LiveGame.logPlayerName 同口径） */
export function nameWithNumber(p, fallback = "跑者") {
  const n = p?.name ?? fallback;
  return p?.number != null && p?.number !== "" ? `${n} (#${p.number})` : n;
}

/** 一垒跑者 钱乾 (#95) */
export function runnerLabel(base, p, fallback = "跑者") {
  return `${BASE_CN[base] ?? ""}跑者 ${nameWithNumber(p, fallback)}`;
}

/** 5 三垒手 张三 (#51) */
export function fielderLabel(code, p) {
  return `${code} ${POS_CN[code] ?? code} ${nameWithNumber(p, "守备员")}`;
}

/** 本棒次X人出局，已累计Y人出局（X=组内累计新增、Y=半局累计；X<=0 时返回空串） */
export function outsSuffixText(outsCum, total) {
  return outsCum > 0 ? `本棒次${outsCum}人出局，已累计${total}人出局` : "";
}

/** 行尾附着累计（仅出局行调用） */
export function appendOuts(text, outsCum, total) {
  const s = outsSuffixText(outsCum, total);
  return s ? `${text}，${s}` : text;
}

/** C1/C2 安全进垒单行：{原因}：{jv垒}跑者 {名} (#N) {进X — 安全|回本垒得分|回到X|留在X}（rbiNote 可选：#224 打点注记「{打者}：{N}打点」，仅得分分支追加）*/
export function advanceText({ reasonCn, runner, from, to, rbiNote }) {
  const label = runnerLabel(from, runner);
  const verb = to === 4 ? "回本垒得分"
    : to === from ? `留在${BASE_CN[from] ?? ""}`
    : to < from ? `回到${BASE_CN[to] ?? ""}`
    : `进${BASE_CN[to] ?? ""} — 安全`;
  return `${reasonCn}：${label} ${verb}${to === 4 && rbiNote ? `（${rbiNote}）` : ""}`;
}

/** 出局单行：{原因}：{jv垒}跑者 {名} (#N) 于{垒}出局（{链|守备员}[，note]）[，累计]（note 可选：#223 如「接杀回传」）*/
export function outText({ reasonCn, runner, from, to, chain, fielderCode, fielder, note, outsCum = 0, total = 0 }) {
  const base = BASE_CN[to] ?? BASE_CN[from] ?? "";
  const chainTxt = Array.isArray(chain) && chain.length ? `（${chain.join("-")}${note ? `，${note}` : ""}）`
    : fielder ? `（${POS_CN[fielderCode] ?? fielderCode} ${nameWithNumber(fielder, "守备员")}）`
    : note ? `（${note}）`
    : "";
  return appendOuts(`${reasonCn}：${runnerLabel(from, runner)} 于${base}出局${chainTxt}`, outsCum, total);
}

/** C4 盗垒死单行：盗垒死（CS {刺杀位}）：{jv垒}跑者 {名} (#N) 自{垒}盗{垒}[，累计] */
export function caughtStealingText({ fielderPos, runner, from, to, outsCum = 0, total = 0 }) {
  return appendOuts(`盗垒死（CS ${fielderPos}）：${runnerLabel(from, runner)} 自${BASE_CN[from] ?? ""}盗${BASE_CN[to] ?? ""}`, outsCum, total);
}

/** C3 投手牵制·出局链（三行组）：投手行 → 接球守备行 → 跑者出局行（含累计） */
export function pickoffGroup({ pitcher, runner, from, fielderCode, fielder, chain, outsCum = 0, total = 0 }) {
  const lines = [];
  lines.push(`投手 ${nameWithNumber(pitcher, "投手")}: 牵制${BASE_CN[from] ?? ""}`);
  if (fielder) lines.push(`${fielderLabel(fielderCode, fielder)}: 接球触杀（刺杀 PO）`);
  lines.push(appendOuts(`${runnerLabel(from, runner)}: 被牵制出局（投手→${POS_CN[fielderCode] ?? "守备"}）`, outsCum, total));
  return { lines };
}

/** 守备位别名→数字码（链文本统一数字制：'3B'→'5'、SS→'6'…） */
export const NUM_POS = { P: "1", C: "2", "1B": "3", "2B": "4", "3B": "5", SS: "6", LF: "7", CF: "8", RF: "9" };

/** 链码序列转数字制（已是数字则直通） */
export function toNumCodes(codes) {
  return (codes || []).map((c) => {
    const s = String(c ?? "").trim().toUpperCase();
    if (/^[1-9]$/.test(s)) return s;
    return NUM_POS[s] ?? String(c ?? "");
  });
}

/** 守备位→其覆盖垒（用于链中传出目标：“接球传X垒”） */
export const POS_TO_BASE = { P: 1, 1: 1, C: 4, 2: 4, 3: 1, 4: 2, 5: 3, 6: 2, 7: 3, 8: 2, 9: 1, "1B": 1, "2B": 2, "3B": 3, SS: 2, LF: 3, CF: 2, RF: 1 };

/** B4 野手选择多行组：打者击出行 → 守备行×N（逐人）→ 跑者出局行（含累计）→ 打者收尾行 */
export function fcGroup({ batter, ballDesc, chain = [], chainPlayers = [], outRunner, outFrom, outBase, reasonCn = "野手选择 FC", batterTo = 1, outsCum = 0, total = 0 }) {
  const lines = [];
  lines.push(`${batter?.name ?? "打者"}: ${ballDesc}`);
  const n = chain.length;
  for (let i = 0; i < n; i++) {
    const code = chain[i];
    const p = chainPlayers[i];
    const who = `${code} ${POS_CN[code] ?? code}${p ? ` ${nameWithNumber(p, "守备员")}` : ""}`;
    if (n === 1) lines.push(`${who}: 接球封杀${runnerLabel(outFrom, outRunner)}（刺杀 PO）`);
    else if (i < n - 1) lines.push(`${who}: 接球传${BASE_CN[POS_TO_BASE[chain[i + 1]]] ?? "下一垒"}（助杀 A）`);
    else lines.push(`${who}: 踏垒封杀${runnerLabel(outFrom, outRunner)}（刺杀 PO）`);
  }
  const chainTxt = chain.length ? `${chain.join("-")}，` : "";
  lines.push(appendOuts(`${runnerLabel(outFrom, outRunner)}: 于${BASE_CN[outBase] ?? ""}被封杀出局（${chainTxt}${reasonCn}）`, outsCum, total));
  lines.push(`${batter?.name ?? "打者"}: 上${BASE_CN[batterTo] ?? "一垒"}（${reasonCn}）`);
  return { lines };
}

// ===== P2 批次2（模板收敛·纯迁移）：投球/换人/特例行（输出与既有文案逐字一致） =====

/** A 组投球行：{主体}: {文案}[ {计数}]；kind 语义见 LABELS（balk_ball 为「投手犯规（记坏球 X）」特例） */
export function pitchText({ who, kind, count }) {
  if (kind === "balk_ball") return `${who}: 投手犯规（记坏球 ${count}）`;
  if (kind === "foul_k") return `${who}: 击出界外球（两好球后界外三振）${count}`;
  const LABELS = {
    ball: "坏球",
    strike_called: "好球",
    strike_swing: "挥空",
    strikeout_called: "第三好球（判进）",
    strikeout_swing: "第三好球（挥空）",
    strikeout_dropped: "第三好球（漏接三振）",
    foul: "击出界外球",
    hit_by_pitch: "触身球",
    balk: "投手犯规",
    wild_pitch: "暴投 (WP)",
    passed_ball: "捕逸 (PB)",
    walk: "四坏保送"
  };
  const base = LABELS[kind] ?? kind;
  return `${who}: ${base}${count ? ` ${count}` : ""}`;
}

/** A12 教练暂停：教练暂停：{方队名}（缺省 进攻方） */
export function coachTimeoutText(teamName) {
  return `教练暂停：${teamName ?? "进攻方"}`;
}

/** E1 换人：换人: 代跑/代打 {入} 换下 {出}（P2b 纯迁移保留现用词） */
export function subText({ kind, inName, outName }) {
  return `换人: ${kind === "hit" ? "代打" : "代跑"} ${inName} 换下 ${outName}`;
}

/** E2 垒况设置：设置跑者: {名} -> {垒名} */
export function setRunnerText({ name, baseCn }) {
  return `设置跑者: ${name} -> ${baseCn}`;
}

/** C3 牵制·未造成进垒/出局：投手 {名} (#N): 牵制{垒}（未造成进垒/出局） */
export function pickoffNoneText({ pitcher, base }) {
  return `投手 ${nameWithNumber(pitcher, "跑者")}: 牵制${BASE_CN[base] ?? ""}（未造成进垒/出局）`;
}

/** B4 野选推进·未得分：野选推进 · {名} (#N) {判定} — 未得分（保留 #- 降级形态） */
export function fpMissText({ runner, verdict }) {
  return `野选推进 · ${runner?.name} (#${runner?.number ?? "-"}) ${verdict} — 未得分`;
}

/** B6 界外球+失误：界外球+失误：{链串|守备失误} */
export function foulErrorText({ chainText }) {
  return `界外球+失误：${chainText || "守备失误"}`;
}
