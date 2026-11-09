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

const POS_EN = {
  P: "Pitcher", C: "Catcher", "1B": "First Base", "2B": "Second Base", "3B": "Third Base", SS: "Shortstop",
  LF: "Left Field", CF: "Center Field", RF: "Right Field",
  1: "Pitcher", 2: "Catcher", 3: "First Base", 4: "Second Base", 5: "Third Base", 6: "Shortstop", 7: "Left Field", 8: "Center Field", 9: "Right Field"
};
const BASE_EN = { 1: "1B", 2: "2B", 3: "3B", 4: "Home" };
// 英文兜底词：按中文兜底词映射，未收录的回退 Player
const NAME_FALLBACK_EN = { 跑者: "Runner", 守备员: "Fielder", 投手: "Pitcher" };
const REASON_EN = {
  "投手牵制尝试": "Pickoff attempt", "其他": "Other", "未进垒": "Did not advance",
  "盗垒成功 (SB)": "Stole base (SB)", "守备大意 (DI)": "Defensive indifference (DI)",
  "传杀过程中进垒": "Advanced on throw", "传球失误推进": "Advanced on throwing error",
  "守备接球/处理失误推进": "Advanced on fielding error", "守备失误推进": "Advanced on fielding error",
  "盗垒/抢本成功": "Stole base / home", "守备失误得分": "Scored on fielding error",
  "盗本垒成功": "Stole home", "暴投 (WP)": "Wild pitch (WP)", "捕逸 (PB)": "Passed ball (PB)",
  "同一play内失误推进": "Advanced on same-play error", "另一守备失误后再推进": "Advanced after another error",
  "接球/处理失误": "Fielding error", "传球失误": "Throwing error", "传杀过程中得分": "Scored on throw",
  "进垒得分": "Scored on advance", "野选推进得分": "Scored on fielder's choice",
  "双杀推进": "Double-play advance", "三杀推进": "Triple-play advance",
  "双杀推进得分": "Scored on double play", "三杀推进得分": "Scored on triple play",
  "封杀出局 (FO)": "Force out (FO)", "触杀出局 (TO)": "Tag out (TO)", "本垒出局 (Out)": "Out at home",
  "野选推进": "Fielder's choice", "双杀出局": "Double play", "三杀出局": "Triple play",
  "失误 (E) 上垒": "Reached on error (E)", "牵制出局": "Pickoff out", "投手牵制": "Pitcher pickoff",
  "暴投": "Wild pitch", "界外球 + 失误": "Foul ball + error", "失误 + 牺牲打": "Error + sacrifice",
  "高飞牺牲 (SF)": "Sacrifice fly (SF)", "牺牲短打（安全）": "Sacrifice bunt (safe)",
  "牺牲触击（SH）": "Sacrifice bunt (SH)", "因失误安全上垒": "Safe on error",
  "因野手选择安全上垒": "Safe on fielder's choice", "离垒过远出局": "Out for leaving early",
  "同一play内暴投推进": "Advanced on wild pitch in same play", "同一play内捕逸推进": "Advanced on passed ball in same play",
  "守备失误": "Fielding error", "盗垒成功": "Stolen base", "守备大意": "Defensive indifference",
  "盗垒死 (CS)": "Caught stealing (CS)", "妨碍守备出局 (Int.)": "Interference out (Int.)",
  "跑垒违规 (Viol.)": "Runner violation (Viol.)",
  "封杀出局": "Force out", "触杀出局": "Tag out", "进攻方妨碍": "Offensive interference",
  "申诉出局": "Appeal out", "盗垒失败": "Caught stealing", "盗垒（连续进垒）": "Stolen base (continuous)",
  "牵制失误推进": "Advanced on pickoff error", "上一棒内推进": "Advanced on previous play",
  "上一棒战术结果内推进": "Advanced on previous play", "其他失误后推进": "Advanced after another error",
  "野手选择局面下推进": "Advanced on fielder's choice", "同一失误推进": "Advanced on same error",
  "不同失误推进": "Advanced on different error", "安打得分": "Scored on hit", "安打": "Hit",
  "本垒出局": "Out at home", "野手选择 FC": "Fielder's choice"
};
const NOTE_EN = { "接杀回传": "catch and relay" };
const BALL_DESC_EN = [
  [/一垒手/g, "First Baseman"], [/二垒手/g, "Second Baseman"], [/三垒手/g, "Third Baseman"],
  [/左外野手/g, "Left Fielder"], [/中外野手/g, "Center Fielder"], [/右外野手/g, "Right Fielder"],
  [/游击手?/g, "Shortstop"], [/投手/g, "Pitcher"], [/捕手/g, "Catcher"],
  [/强袭滚地球/g, "hard ground ball"], [/内野高飞球/g, "infield pop-up"], [/滚地球/g, "ground ball"],
  [/平飞球/g, "line drive"], [/飞球/g, "fly ball"], [/短打/g, "bunt"],
  [/左上侧/g, " upper-left"], [/右上侧/g, " upper-right"], [/左下侧/g, " lower-left"], [/右下侧/g, " lower-right"],
  [/上侧/g, " upper side"], [/下侧/g, " lower side"], [/左侧/g, " left side"], [/右侧/g, " right side"],
  [/左中外野|左中/g, "left-center"], [/右中外野|右中/g, "right-center"],
  [/左外野|左外/g, "left field"], [/中外野|中外/g, "center field"], [/右外野|右外/g, "right field"],
  [/内野/g, "infield"], [/一垒/g, "first base"], [/二垒/g, "second base"], [/三垒/g, "third base"],
  [/击出/g, "Hit "], [/到/g, " to "], [/（/g, " ("], [/）/g, ")"]
];
const VERDICT_EN = { "判进": "called strike", "安全": "safe", "出局": "out", "未得分": "no run",
  "封杀出局 (FO)": "Force out (FO)", "触杀出局 (TO)": "Tag out (TO)", "本垒出局 (Out)": "Out at home",
  "妨碍守备出局 (Int.)": "Interference out (Int.)", "跑垒违规 (Viol.)": "Runner violation (Viol.)" };

function ballDescEn(text) {
  let out = String(text ?? "");
  for (const [re, en] of BALL_DESC_EN) out = out.replace(re, en);
  return out.replace(/\s+/g, " ").trim();
}

let LOG_LANG = "zh";
export function positionEn(code) {
  return POS_EN[code] ?? code;
}

export function setLogTextLocale(locale) {
  LOG_LANG = String(locale ?? "").startsWith("zh") ? "zh" : "en";
}
const isEn = () => LOG_LANG === "en";
const posLabelEn = (value) => {
  const raw = String(value ?? "").trim();
  if (POS_EN[raw]) return POS_EN[raw];
  const code = Object.keys(POS_CN).find((k) => POS_CN[k] === raw);
  return POS_EN[code] ?? raw;
};
const reasonEn = (reasonCn, fallback = "Advance") => {
  const raw = String(reasonCn ?? "").trim();
  if (!/[\u4e00-\u9fff]/.test(raw)) return raw || fallback;
  if (REASON_EN[raw]) return REASON_EN[raw];
  const base = raw.replace(/\s*[（(].*$/, "").trim();
  return REASON_EN[base] ?? fallback;
};
const noteEn = (note) => NOTE_EN[String(note ?? "").trim()] ?? note;
/** 中文垒名→英文垒名；未收录时原样返回 */
const baseEnFromCn = (baseCn) => {
  const code = Object.keys(BASE_CN).find((key) => BASE_CN[key] === baseCn);
  return BASE_EN[code] ?? baseCn;
};

/** 名 (#N)；缺名/缺号安全降级（与 LiveGame.logPlayerName 同口径） */
export function nameWithNumber(p, fallback = "跑者") {
  const fallbackName = isEn() ? NAME_FALLBACK_EN[fallback] ?? "Player" : fallback;
  const n = p?.name ?? fallbackName;
  return p?.number != null && p?.number !== "" ? `${n} (#${p.number})` : n;
}

/** 一垒跑者 钱乾 (#95) */
export function runnerLabel(base, p, fallback = "跑者") {
  const prefix = isEn() ? `${BASE_EN[base] ?? ""} runner` : `${BASE_CN[base] ?? ""}跑者`;
  return `${prefix} ${nameWithNumber(p, fallback)}`;
}

/** 5 三垒手 张三 (#51) */
export function fielderLabel(code, p) {
  const positions = isEn() ? POS_EN : POS_CN;
  return `${code} ${positions[code] ?? code} ${nameWithNumber(p, "守备员")}`;
}

/** 本棒次X人出局，已累计Y人出局（X=组内累计新增、Y=半局累计；X<=0 时返回空串） */
export function outsSuffixText(outsCum, total) {
  if (isEn()) return outsCum > 0 ? `${outsCum} out${outsCum > 1 ? "s" : ""} this play, ${total} total` : "";
  return outsCum > 0 ? `本棒次${outsCum}人出局，已累计${total}人出局` : "";
}

/** 行尾附着累计（仅出局行调用） */
export function appendOuts(text, outsCum, total) {
  const s = outsSuffixText(outsCum, total);
  if (!s) return text;
  return isEn() ? `${text}; ${s}` : `${text}，${s}`;
}

/** 跑者得分/本垒出局行的原因前缀：按 runCause 映射，未知原因保持安打口径 */
const RUN_CAUSE_LABELS = {
  ERROR: { score: "失误造成得分", play: "失误" },
  FC: { score: "野选推进得分", play: "野选" },
  SACRIFICE: { score: "牺牲打得分", play: "牺牲打" }
};
const RUN_CAUSE_NAMES_EN = { ERROR: "Error", FC: "Fielder's choice", SACRIFICE: "Sacrifice" };

export function runCauseLabel(runCause, kind = "score") {
  if (isEn()) {
    const name = RUN_CAUSE_NAMES_EN[runCause];
    if (name) return kind === "score" ? `Scored on ${name.toLowerCase()}` : name;
    return kind === "score" ? "Scored on hit" : "Hit";
  }
  return RUN_CAUSE_LABELS[runCause]?.[kind] ?? (kind === "play" ? "安打" : "安打得分");
}

/** C1/C2 安全进垒单行：{原因}：{jv垒}跑者 {名} (#N) {进X — 安全|回本垒得分|回到X|留在X}（rbiNote 可选：#224 打点注记「{打者}：{N}打点」，仅得分分支追加）*/
export function advanceText({ reasonCn, runner, from, to, rbiNote }) {
  const label = runnerLabel(from, runner);
  if (isEn()) {
    const verb = to === 4 ? "scores"
      : to === from ? `stays at ${BASE_EN[from] ?? ""}`
      : to < from ? `returns to ${BASE_EN[to] ?? ""}`
      : `advances to ${BASE_EN[to] ?? ""} — safe`;
    const rbiCount = String(rbiNote ?? "").match(/(\d+)\s*打点/)?.[1] ?? "1";
    const note = to === 4 && rbiNote ? ` (${String(rbiNote).split("：")[0]}: ${rbiCount} RBI)` : "";
    return `${reasonEn(reasonCn)}: ${label} ${verb}${note}`;
  }
  const verb = to === 4 ? "回本垒得分"
    : to === from ? `留在${BASE_CN[from] ?? ""}`
    : to < from ? `回到${BASE_CN[to] ?? ""}`
    : `进${BASE_CN[to] ?? ""} — 安全`;
  return `${reasonCn}：${label} ${verb}${to === 4 && rbiNote ? `（${rbiNote}）` : ""}`;
}

/** 出局单行：{原因}：{jv垒}跑者 {名} (#N) 于{垒}出局（{链|守备员}[，note]）[，累计]（note 可选：#223 如「接杀回传」）*/
export function outText({ reasonCn, runner, from, to, chain, fielderCode, fielder, note, outsCum = 0, total = 0 }) {
  if (isEn()) {
    const base = BASE_EN[to] ?? BASE_EN[from] ?? "";
    const chainTxt = Array.isArray(chain) && chain.length
      ? ` (${chain.join("-")}${note ? `, ${noteEn(note)}` : ""})`
      : fielder ? ` (${POS_EN[fielderCode] ?? fielderCode} ${nameWithNumber(fielder, "守备员")})`
      : note ? ` (${noteEn(note)})`
      : "";
    return appendOuts(`${reasonEn(reasonCn, "Out")}: ${runnerLabel(from, runner)} out at ${base}${chainTxt}`, outsCum, total);
  }
  const base = BASE_CN[to] ?? BASE_CN[from] ?? "";
  const chainTxt = Array.isArray(chain) && chain.length ? `（${chain.join("-")}${note ? `，${note}` : ""}）`
    : fielder ? `（${POS_CN[fielderCode] ?? fielderCode} ${nameWithNumber(fielder, "守备员")}）`
    : note ? `（${note}）`
    : "";
  return appendOuts(`${reasonCn}：${runnerLabel(from, runner)} 于${base}出局${chainTxt}`, outsCum, total);
}

/** #223 接杀后回传刺杀离垒跑者：逐名列出原接杀 PO、回传 A、踩垒 PO 与跑者出局 */
export function catchBackDoublePlayGroup({ catchCode, catcher, tagCode, tagger, runner, from, to, outsCum = 1, total = 0 }) {
  const chain = toNumCodes([catchCode, tagCode]);
  const head = isEn()
    ? [
        `${fielderLabel(catchCode, catcher)}: catch and relay (catch: PO; throw: A)`,
        `${fielderLabel(tagCode, tagger)}: catch and tag the base for the out (PO)`
      ]
    : [
        `${fielderLabel(catchCode, catcher)}: 接杀后回传（接杀：刺杀 PO；传球：助杀 A）`,
        `${fielderLabel(tagCode, tagger)}: 接球触垒使跑者出局（刺杀 PO）`
      ];
  return {
    lines: [
      ...head,
      outText({ reasonCn: "离垒过远出局", runner, from, to, chain, note: "接杀回传", outsCum, total })
    ]
  };
}

/** C4 盗垒死单行：盗垒死（CS {刺杀位}）：{jv垒}跑者 {名} (#N) 自{垒}盗{垒}[，累计] */
export function caughtStealingText({ fielderPos, runner, from, to, outsCum = 0, total = 0 }) {
  if (isEn()) return appendOuts(`Caught stealing (CS ${posLabelEn(fielderPos)}): ${runnerLabel(from, runner)} from ${BASE_EN[from] ?? ""} to ${BASE_EN[to] ?? ""}`, outsCum, total);
  return appendOuts(`盗垒死（CS ${fielderPos}）：${runnerLabel(from, runner)} 自${BASE_CN[from] ?? ""}盗${BASE_CN[to] ?? ""}`, outsCum, total);
}

/** C3 投手牵制·出局链（三行组）：投手行 → 接球守备行 → 跑者出局行（含累计） */
export function pickoffGroup({ pitcher, runner, from, fielderCode, fielder, chain, outsCum = 0, total = 0 }) {
  const lines = [];
  if (isEn()) {
    lines.push(`Pitcher ${nameWithNumber(pitcher, "投手")}: pickoff at ${BASE_EN[from] ?? ""}`);
    if (fielder) lines.push(`${fielderLabel(fielderCode, fielder)}: catch and tag (PO)`);
    lines.push(appendOuts(`${runnerLabel(from, runner)}: out on pickoff (P → ${POS_EN[fielderCode] ?? "fielder"})`, outsCum, total));
    return { lines };
  }
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

/** 单出局链规整：相邻重复（自踩动作）合并为一次（5-2-5-5 → 5-2-5） */
export function normalizeSingleOutChain(chain) {
  const seq = (Array.isArray(chain) ? chain : []).map((c) => String(c ?? "").trim()).filter(Boolean);
  const out = [];
  for (const code of seq) {
    if (out[out.length - 1] !== code) out.push(code);
  }
  return out;
}

/**
 * 单出局链出局垒/方式判定（2026-10-23 裁决）
 * - 最后一步有显式自踩动作 → 采用所选动作与垒包；
 * - 无自踩动作 → 沿用原口径：出局垒=起始垒+1、封杀。
 *   不能仅凭末位守备员的覆盖垒反推，否则延长传球链（如 6-4-3 的末尾回传一垒）
 *   会被误判为回垒触杀；回垒触杀必须由自踩动作显式指定。
 */
export function resolveSingleOutChain({ chain, outFrom, stepAction } = {}) {
  const normalized = normalizeSingleOutChain(chain);
  const from = Number(outFrom);
  const step = stepAction && typeof stepAction === "object" ? stepAction : null;
  const explicitType = step?.action ?? (typeof stepAction === "string" ? stepAction : null);
  const outType = explicitType === "tag" || explicitType === "force" ? explicitType : "force";
  const actionBase = step ? Number(step.base) : NaN;
  const outBase = Number.isFinite(actionBase) && actionBase >= 1 && actionBase <= 4
    ? actionBase
    : Number.isFinite(from) ? Math.min(4, from + 1) : null;
  return { chain: normalized, outType, outBase };
}

/** B4 野手选择多行组：打者击出行 → 守备行×N（逐人）→ 跑者出局行（含累计）→ 打者收尾行 */
export function fcGroup({ batter, ballDesc, chain = [], chainPlayers = [], outRunner, outFrom, outBase, reasonCn = "野手选择 FC", batterTo = 1, outsCum = 0, total = 0, outType = "force" }) {
  const lines = [];
  if (isEn()) {
    const reason = reasonEn(reasonCn, "Fielder's choice");
    lines.push(`${batter?.name ?? "Batter"}: ${ballDescEn(ballDesc)}`);
    const n = chain.length;
    const putoutCode = n ? chain[n - 1] : null;
    const assisted = new Set();
    for (let i = 0; i < n; i++) {
      const code = chain[i];
      const p = chainPlayers[i];
      const who = `${code} ${POS_EN[code] ?? code}${p ? ` ${nameWithNumber(p, "守备员")}` : ""}`;
      if (n === 1) lines.push(`${who}: catch and ${outType === "tag" ? "tag" : "force"} ${runnerLabel(outFrom, outRunner)} (PO)`);
      else if (i < n - 1) {
        const credit = code !== putoutCode && !assisted.has(code);
        if (credit) assisted.add(code);
        lines.push(`${who}: catch and throw to ${BASE_EN[POS_TO_BASE[chain[i + 1]]] ?? "next base"}${credit ? " (A)" : ""}`);
      } else lines.push(`${who}: catch and ${outType === "tag" ? "tag" : "step on base for the force"} ${runnerLabel(outFrom, outRunner)} (PO)`);
    }
    const chainTxt = chain.length ? `${chain.join("-")}, ` : "";
    const verb = outType === "tag" ? "tagged out" : "forced out";
    lines.push(appendOuts(`${runnerLabel(outFrom, outRunner)}: ${verb} at ${BASE_EN[outBase] ?? ""} (${chainTxt}${reason})`, outsCum, total));
    lines.push(`${batter?.name ?? "Batter"}: reaches ${BASE_EN[batterTo] ?? "1B"} (${reason})`);
    return { lines };
  }
  lines.push(`${batter?.name ?? "打者"}: ${ballDesc}`);
  const n = chain.length;
  const putoutCode = n ? chain[n - 1] : null;
  const assisted = new Set();
  for (let i = 0; i < n; i++) {
    const code = chain[i];
    const p = chainPlayers[i];
    const who = `${code} ${POS_CN[code] ?? code}${p ? ` ${nameWithNumber(p, "守备员")}` : ""}`;
    if (n === 1) lines.push(`${who}: ${outType === "tag" ? "接球触杀" : "接球封杀"}${runnerLabel(outFrom, outRunner)}（刺杀 PO）`);
    else if (i < n - 1) {
      const credit = code !== putoutCode && !assisted.has(code);
      if (credit) assisted.add(code);
      lines.push(`${who}: 接球传${BASE_CN[POS_TO_BASE[chain[i + 1]]] ?? "下一垒"}${credit ? "（助杀 A）" : ""}`);
    }
    else lines.push(`${who}: ${outType === "tag" ? "接球触杀" : "踏垒封杀"}${runnerLabel(outFrom, outRunner)}（刺杀 PO）`);
  }
  const chainTxt = chain.length ? `${chain.join("-")}，` : "";
  const verb = outType === "tag" ? "被触杀" : "被封杀";
  lines.push(appendOuts(`${runnerLabel(outFrom, outRunner)}: 于${BASE_CN[outBase] ?? ""}${verb}出局（${chainTxt}${reasonCn}）`, outsCum, total));
  lines.push(`${batter?.name ?? "打者"}: 上${BASE_CN[batterTo] ?? "一垒"}（${reasonCn}）`);
  return { lines };
}

// ===== P2 批次2（模板收敛·纯迁移）：投球/换人/特例行（输出与既有文案逐字一致） =====

/** A 组投球行 kind→文案表（中/英） */
const PITCH_LABELS_CN = {
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
const PITCH_LABELS_EN = {
  ball: "Ball",
  strike_called: "Strike (called)",
  strike_swing: "Swing and miss",
  strikeout_called: "Strike three (looking)",
  strikeout_swing: "Strike three (swinging)",
  strikeout_dropped: "Strike three (dropped third strike)",
  foul: "Foul ball",
  hit_by_pitch: "Hit by pitch",
  balk: "Balk",
  wild_pitch: "Wild pitch (WP)",
  passed_ball: "Passed ball (PB)",
  walk: "Walk"
};

/** A 组投球行：{主体}: {文案}[ {计数}]；kind 语义见 PITCH_LABELS_CN（balk_ball 为「投手犯规（记坏球 X）」特例） */
export function pitchText({ who, kind, count }) {
  if (isEn()) {
    if (kind === "balk_ball") return `${who}: balk (ball ${count})`;
    if (kind === "foul_k") return `${who}: foul ball (foul tip strikeout after two strikes) ${count}`;
    return `${who}: ${PITCH_LABELS_EN[kind] ?? kind}${count ? ` ${count}` : ""}`;
  }
  if (kind === "balk_ball") return `${who}: 投手犯规（记坏球 ${count}）`;
  if (kind === "foul_k") return `${who}: 击出界外球（两好球后界外三振）${count}`;
  return `${who}: ${PITCH_LABELS_CN[kind] ?? kind}${count ? ` ${count}` : ""}`;
}

/** A12 教练暂停：教练暂停：{方队名}（缺省 进攻方） */
export function coachTimeoutText(teamName) {
  return isEn() ? `Coach timeout: ${teamName ?? "offense"}` : `教练暂停：${teamName ?? "进攻方"}`;
}

/** E1 换人：换人: 代跑/代打 {入} 换下 {出}（P2b 纯迁移保留现用词） */
export function subText({ kind, inName, outName }) {
  return isEn()
    ? `Substitution: ${kind === "hit" ? "pinch hitter" : "pinch runner"} ${inName} replaces ${outName}`
    : `换人: ${kind === "hit" ? "代打" : "代跑"} ${inName} 换下 ${outName}`;
}

/** E2 垒况设置：设置跑者: {名} -> {垒名} */
export function setRunnerText({ name, baseCn }) {
  if (!isEn()) return `设置跑者: ${name} -> ${baseCn}`;
  return `Set runner: ${name} -> ${baseEnFromCn(baseCn)}`;
}

/** C3 牵制·未造成进垒/出局：投手 {名} (#N): 牵制{垒}（未造成进垒/出局） */
export function pickoffNoneText({ pitcher, base }) {
  return isEn()
    ? `Pitcher ${nameWithNumber(pitcher, "跑者")}: pickoff at ${BASE_EN[base] ?? ""} (no advance/out)`
    : `投手 ${nameWithNumber(pitcher, "跑者")}: 牵制${BASE_CN[base] ?? ""}（未造成进垒/出局）`;
}

/** B4 野选推进·未得分：野选推进 · {名} (#N) {判定} — 未得分（保留 #- 降级形态） */
export function fpMissText({ runner, verdict }) {
  return isEn()
    ? `Fielder's choice advance · ${runner?.name} (#${runner?.number ?? "-"}) ${VERDICT_EN[verdict] ?? verdict} — no run`
    : `野选推进 · ${runner?.name} (#${runner?.number ?? "-"}) ${verdict} — 未得分`;
}

/** B6 界外球+失误：界外球+失误：{链串|守备失误} */
export function foulErrorText({ chainText }) {
  return isEn() ? `Foul ball + error: ${chainText || "fielding error"}` : `界外球+失误：${chainText || "守备失误"}`;
}

// ===== P3（F 系统类归口 + 特例归口；除纠偏前缀统一外逐字保留） =====

/** F 系统行（换局/攻守/纠偏/隐藏注记/guard）；kind 见 switch（adjust_score/adjust_bases 为 F2 统一前缀） */
export function sysText({ kind, detail, runs, inning, isTop, team, nextBatter, who, code }) {
  if (isEn()) {
    switch (kind) {
      case "adjust_score": return `Correction (score): ${detail}`;
      case "adjust_bases": return `Correction (bases): ${detail}`;
      case "third_out_note": return `Third out is a force out: ${runs} run(s) on this play do not count`;
      case "inning_header": return `--- Inning ${inning} ${isTop ? "Top" : "Bottom"} ---`;
      case "switch_sides": return `Sides change. Offense: ${team}, next batter: ${nextBatter}`;
      case "np_backfill": return `${who === "投手" ? "Pitcher" : who}: pitch count was missing; 1 pitch (NP) added for this batted-ball/fielding play`;
      case "dk3_guard": return `Dropped third strike: unknown code ${code}`;
      case "game_start": return "Game started";
      case "game_end": return "Game ended";
      case "force_half": return "Force half-inning: this half-inning ends early (special rule)";
      case "dh_cancel": return `Cancel DH: ${detail}`;
      case "extend_inning": return "Extra inning";
      case "tp_placeholder": return "Ground ball: triple play (placeholder)";
      case "illegal_pitch":
        return detail === "walk_ab_end" ? "Illegal pitch: scored as a ball; the plate appearance ends with a walk."
          : detail === "walk_no_runner" ? "Illegal pitch: scored as a ball (no runners, no advance)"
          : detail === "walk_no_advance" ? "Illegal pitch: scored as a ball; runners do not advance (placeholder)"
          : "Illegal pitch: all runners advance one base";
      default: return "";
    }
  }
  switch (kind) {
    case "adjust_score": return `纠偏（得分）：${detail}`;
    case "adjust_bases": return `纠偏（垒况）：${detail}`;
    case "third_out_note": return `第三出局为封杀：本 play ${runs} 分不计入`;
    case "inning_header": return `--- ${inning}局${isTop ? "上" : "下"} ---`;
    case "switch_sides": return `攻守交换，进攻方：${team}，下一棒：${nextBatter}`;
    case "np_backfill": return `${who}: 先前未记该球球数，已为本次击出/守备补记 1 球（NP）`;
    case "dk3_guard": return `漏接三振：未识别代码 ${code}`;
    case "game_start": return "比赛开始";
    case "game_end": return "比赛已结束";
    case "force_half": return "强制换局：本半局提前结束（特殊换局规则）";
    case "dh_cancel": return `取消DH：${detail}`;
    case "extend_inning": return "进入延长局";
    case "tp_placeholder": return "滚地球：三杀（示意）";
    case "illegal_pitch":
      return detail === "walk_ab_end" ? "不合法投球：已记为坏球；当前打席因四坏结束。"
        : detail === "walk_no_runner" ? "不合法投球：记为坏球（垒上无跑者，未推进）"
        : detail === "walk_no_advance" ? "不合法投球：记为坏球；跑者不推进（示意记录）"
        : "不合法投球：垒上跑者各推进一垒";
    default: return "";
  }
}

/** C2 失误推进（纯迁移逐字：逗号+裸名）：{原因}，{名} 进{垒|"本垒得分"} */
export function errorAdvanceText({ reasonCn, runnerName, to }) {
  if (!isEn()) return `${reasonCn}，${runnerName} 进${to}`;
  const base = to === "本垒得分" ? "Home" : baseEnFromCn(to);
  return `${reasonEn(reasonCn, "Error")}, ${runnerName} advances to ${base}`;
}

/** 盗垒成功单选（纯迁移逐字）：{名}: 盗垒成功 / 盗本垒成功 */
export function stealSingleText({ name, home = false }) {
  return isEn() ? `${name}: ${home ? "stole home" : "stole a base"}` : `${name}: ${home ? "盗本垒成功" : "盗垒成功"}`;
}

/** 跑者留在垒上（调用方已带 #N 标签） */
export function staysOnBaseText(playerLabel) {
  return isEn() ? `Runner ${playerLabel} stays on base` : `跑者 ${playerLabel} 留在垒上`;
}

/** 失误记 E 注记：单/多守备员 */
export function errorNoteText(names, multiple = false) {
  return isEn()
    ? ` (${names}${multiple ? " each credited E" : " credited E"})`
    : `（${names}${multiple ? " 各记 E" : " 记 E"}）`;
}

/** 换人后的守备局数结清注记 */
export function fieldingIpNoteText(playerLabel, ip) {
  return isEn()
    ? ` (${playerLabel}: ${ip} fielding inning${ip === 1 ? "" : "s"} settled for this game)`
    : `（${playerLabel} 本场守备 ${ip} 局结清）`;
}
