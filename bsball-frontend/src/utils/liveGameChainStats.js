// 双杀/三杀链守备统计精细化（DP/TP 专用）——纯函数，可单测
// 设计依据：docs/实时录入-双杀记账与自踩交互设计-2026-10-08.md §二（2026-10-08 用户确认），
//   实施精化：出局点数严格等于 outs（自踩动作必为点；其余从末尾往前补齐）——对 DP 全部场景
//   输出与设计稿除"超语义行"外一致（超语义行按 outs 上限收敛），并修正 TP（k>=3）的点位分配（设计稿验证表未覆盖 TP）。
// 规则：
//   1) 相邻重复合并为"同人自踩动作"（X,X -> 一次 {pos:X, selfstep}）
//   2) 出局点 = 自踩动作 ∪（从末尾往前补齐至 outs 个普通动作）
//   3) 点：非末点 PO+1、A+1（踩垒出局并传出）；末点 PO+1；非点：A+1（纯传球）
//   4) 特例 k==1：该守位 PO x outs（独力：如 [1B,1B] 压缩为 [1B] 的独力双杀）
// 兼容：结算链路传入的链可能已被 _i() 全同压缩（全同 -> 单项），k==1 分支消化该情形

const CHAIN_OUTS = { DP: 2, TP: 3 };

/**
 * DP/TP 显示链规整（2026-10-08 用户裁决：自踩不重复写位号）
 * - 全同链（独力，如 [1B,1B]）保留原样（文本 "3-3"）
 * - 其余：相邻重复合并（[SS,SS,1B] -> [SS,1B]，文本 "6-3"）
 * - 非 DP/TP 返回原链（调用方自行走 _i 等既有显示逻辑）
 */
export function normalizeChainForDisplay(type, steps) {
  if (type !== "DP" && type !== "TP") return steps;
  const seq = (Array.isArray(steps) ? steps : []).map((s) => String(s ?? "").trim()).filter(Boolean);
  if (seq.length >= 2 && seq.every((s) => s === seq[0])) return seq;
  const out = [];
  for (let i = 0; i < seq.length; i += 1) {
    if (i > 0 && seq[i] === seq[i - 1]) continue;
    out.push(seq[i]);
  }
  return out;
}

/**
 * 计算 DP/TP 链的守备统计
 * @param {string} type 击球类型（仅 DP/TP 生效）
 * @param {string[]} steps 守备位链（原始或 _i 压缩后均可）
 * @returns {Array<{pos:string,po:number,a:number}>|null} 非 DP/TP 返回 null（调用方回退原 Dn 逻辑）
 */
export function computeChainFieldingStats(type, steps) {
  const outs = CHAIN_OUTS[type];
  if (!outs) return null;
  const seq = (Array.isArray(steps) ? steps : []).map((s) => String(s ?? "").trim()).filter(Boolean);
  if (seq.length === 0) return [];
  // 相邻重复合并（同人自踩=一次动作）
  const actions = [];
  for (let i = 0; i < seq.length; i += 1) {
    if (i + 1 < seq.length && seq[i] === seq[i + 1]) {
      actions.push({ pos: seq[i], selfstep: true });
      i += 1;
    } else {
      actions.push({ pos: seq[i], selfstep: false });
    }
  }
  const map = new Map();
  const add = (pos, po, a) => {
    const cur = map.get(pos) ?? { pos, po: 0, a: 0 };
    cur.po += po;
    cur.a += a;
    map.set(pos, cur);
  };
  const k = actions.length;
  if (k === 1) {
    add(actions[0].pos, outs, 0);
    return [...map.values()];
  }
  // 出局点：自踩必为点；其余从末尾往前补齐至 outs 个
  const selfCount = actions.filter((x) => x.selfstep).length;
  let need = Math.max(0, outs - selfCount);
  const isPoint = actions.map((x) => x.selfstep);
  for (let i = k - 1; i >= 0 && need > 0; i -= 1) {
    if (!isPoint[i]) {
      isPoint[i] = true;
      need -= 1;
    }
  }
  for (let i = 0; i < k; i += 1) {
    if (isPoint[i]) add(actions[i].pos, 1, i < k - 1 ? 1 : 0);
    else add(actions[i].pos, 0, 1);
  }
  return [...map.values()];
}
