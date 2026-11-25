// 投手 AB 状态判定（#702）：区分“库值等于自动公式”与“人工覆写”
// 口径：AB = max(0, BF − BB − HBP − SH − SF)；BB 含 IBB，不额外单减
import { calcAtBatsFromBf, legacyAtBatsFromBf } from './gamePlayerStat.js';

function clampAb(value){
  return Math.max(0, Math.floor(Number(value)||0))
}

/**
 * 判定一行投手数据的 AB 来源（仅用于载入）。
 * - 库值等于新公式或旧公式（无 SH/SF）→ 自动值，返回当前新公式；
 * - 其余非空值 → 视为人工覆写，保留；
 * - 会话内已标记人工（abManual=true）→ 直接保留当前值。
 * 局限：库表无持久人工标记，无法完全区分“历史自动值”和“恰好等于旧公式的人工值”，
 * 后者载入时会被当作历史自动值按新公式修正；保存侧只认显式 abManual，不做反推。
 */
export function inferPitchAbState(row){
  const current=calcAtBatsFromBf(row);
  if(row?.abManual===true)return{pitAb:clampAb(row.pitAb),abManual:true}
  const saved=row?.pitchAb;
  if(saved==null||Number.isNaN(Number(saved)))return{pitAb:current,abManual:false}
  const value=clampAb(saved);
  // 0 值兜底（#702 回填同步）：仅当库值本身为 0 且新公式>0 时视为旧默认值，按新公式自动展示；
  // 0 且公式=0 保持 0；负数不属于旧默认 0，仍按人工值 clamp 后保留
  if(Number(saved)===0&&current>0)return{pitAb:current,abManual:false}
  const manual=value!==current&&value!==legacyAtBatsFromBf(row);
  return{pitAb:manual?value:current,abManual:manual}
}

/** BF/BB/IBB/HBP/SH/SF 变化后重算自动行的 AB；人工行保持不动 */
export function syncAutoPitchAb(row){
  if(!row||row.abManual===true)return row;
  row.pitAb=calcAtBatsFromBf(row);
  return row
}

/**
 * 保存载荷：只认会话内的显式人工标记（#702 跟修）。
 * 不再用 row.pitchAb 反推，避免改过 BF/SH/SF 后旧库值被误判为人工而保存陈旧 AB。
 * 人工→clamp 人工值；自动→按当前分项实时计算的新公式（不写默认 0）。
 */
export function toSavePitchAb(row){
  return row?.abManual===true?clampAb(row.pitAb):calcAtBatsFromBf(row)
}
