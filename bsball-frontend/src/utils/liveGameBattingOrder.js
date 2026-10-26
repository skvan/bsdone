// 实时录入·打序重建（issue #445 / #444）——纯函数，可单测，不依赖 Vue 与网络
// 背景：录入现场有两条恢复路径。快照路径（Vl()）会连同 batterIndex / lineupStarterIds 一起恢复；
//   而「无快照、仅按云端已保存统计重建」的路径（Do()）只重排名单，既没有把「当前打者指针」找回来，
//   也会在名单解析不到某人时把他丢掉。两者叠加的后果：
//     1) 重新进入现场后 batterIndex 停在 0，之后的每一个打席都记到错误的人头上 → 某人 PA/AB 凭空多一次（#445）
//     2) 丢位会让其后所有人的「数组下标」整体前移，而棒次在系统里就是靠下标承载的（No() 借 br() 从名单数组
//        惰性推导槽位再回送 battingOrder）→ 一次丢位会被固化成服务端错误的 batting_order（#444 的上下颠倒）
// 本模块把这两件事收敛成可验证的纯逻辑：解析不到的球员保留槽位（不丢位），当前打者指针按该队已完成的打席数回溯。

/** 打序槽位数：软式 11 人、其余 12 人（与 Do() 原实现同口径） */
export function lineupSlotCount(sport) {
  return String(sport ?? '').toUpperCase() === 'SOFTBALL' ? 11 : 12;
}

const num = (v) => {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
};

/**
 * 单行的打席数（PA）。
 * 存档 payload 里 pa 是推导值（LiveGame.js Eo()：ab+bb+hbp+sf+sh+io），
 * 所以服务端行的 pa 为空时，必须用同一套口径回填，不能当成 0。
 */
export function plateAppearances(row) {
  if (!row) return 0;
  if (row.pa !== null && row.pa !== undefined && Number.isFinite(Number(row.pa))) return Number(row.pa);
  return num(row.ab) + num(row.bb) + num(row.hbp) + num(row.sf) + num(row.sh) + num(row.io);
}

/** 该队已完成打席总数（只算打者列；投手列的 ab 是「被打者」口径，不参与打序回溯） */
export function teamPlateAppearances(statRows, teamId) {
  if (!Array.isArray(statRows)) return 0;
  const tid = Number(teamId);
  return statRows.reduce((sum, row) => {
    if (!row || Number(row.teamId) !== tid) return sum;
    if (num(row.isPitcher) === 1) return sum;
    return sum + plateAppearances(row);
  }, 0);
}

/**
 * 由服务端统计行 + 该队 roster 重建打序槽位（取代 Do() 里 w() 的排序段）。
 * 与旧实现的语义差别只有一处：**解析不到 roster 的 playerId 不再被丢弃**，而是保留槽位，
 * 由调用方用统计行本身造出占位球员。丢位会造成其后所有人下标前移（#445 的错记根源之一）。
 * 保持不变的语义：服务端没有任何 battingOrder>0 的行时返回空数组——那代表该场还没确认先发打序，
 * 调用方据此把用户引导到先发阵容页，不能凭空拿 roster 顺序顶替真实棒次。
 */
export function resolveBattingSlots({ statRows, roster, teamId, sport } = {}) {
  const tid = Number(teamId);
  const ordered = new Map();
  (Array.isArray(statRows) ? statRows : []).forEach((row) => {
    if (!row || Number(row.teamId) !== tid) return;
    const order = num(row.battingOrder);
    if (order > 0) ordered.set(order, row);
  });
  const slots = [...ordered.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([order, row]) => ({ playerId: Number(row.playerId), battingOrder: order }));
  if (slots.length === 0) return [];

  const size = lineupSlotCount(sport);
  const taken = new Set(slots.map((s) => s.playerId));
  (Array.isArray(roster) ? roster : []).forEach((player) => {
    if (slots.length >= size) return;
    const id = Number(player?.id);
    if (!Number.isFinite(id) || id <= 0 || taken.has(id)) return;
    taken.add(id);
    slots.push({ playerId: id, battingOrder: null });
  });
  return slots;
}

/**
 * 名单数组 → lineupStarterIds（长度 12、空位 null），与 LiveGame.js 的 gr() 同口径。
 * 单独实现而非直接调 br()：br() 写死 fe.value.every(...)，而 fe 的初值是 null（x(null)），
 * 走「无快照重建」这条路径时它会先抛 TypeError，正好把要复原的时机挡住。
 */
export function starterIdsFromLineup(lineup) {
  const list = Array.isArray(lineup) ? lineup : [];
  return Array.from({ length: 12 }, (_, i) => {
    const id = Number(list[i]?.id);
    return Number.isFinite(id) && id > 0 ? id : null;
  });
}

/**
 * 回溯「当前打者」在打序数组里的下标。
 * 依据：录入时每完成一个打席就推进一格（fo()：batterIndex+1 mod 名单长度），全程不按局重置，
 * 所以「该队已完成打席数 mod 名单长度」就是当前站位。进行中的半个打席尚未计入 PA，指针正停在他身上。
 */
export function deriveBatterIndex(slotCount, paTotal) {
  const n = Number(slotCount);
  if (!Number.isFinite(n) || n <= 0) return 0;
  return Math.max(0, Math.floor(num(paTotal))) % n;
}
