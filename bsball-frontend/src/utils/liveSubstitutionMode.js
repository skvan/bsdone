// 实时录入·换人事务纯函数（issue #444 后续）：before/after 按打序槽位做净变化 diff。
// 只比较两次点击之间的端点，不保留中间犹豫/纠错过程；页面状态变更仍由 LiveGame 的 cc() 完成。

export function normalizePosition(value) {
  return String(value ?? '').trim().toUpperCase();
}

function playerId(player) {
  const id = Number(player?.id);
  return Number.isFinite(id) && id > 0 ? id : 0;
}

function playerName(player, id) {
  const name = String(player?.name ?? '').trim();
  return name || (id > 0 ? `#${id}` : '');
}

function findPlayer(team, id) {
  const n = Number(id);
  if (!Number.isFinite(n) || n <= 0) return null;
  return (team?.lineup ?? []).find((p) => playerId(p) === n)
    ?? (team?.bench ?? []).find((p) => playerId(p) === n)
    ?? (playerId(team?.fpRow) === n ? team.fpRow : null);
}

function starterIdsFromBefore(before) {
  const ids = Array.isArray(before?.starterIds) ? before.starterIds.map((v) => Number(v) || null) : [];
  if (ids.length === 12 && ids.some((v) => v != null)) return ids;
  return Array.from({ length: 12 }, (_, i) => {
    const id = playerId(before?.lineup?.[i]);
    return id > 0 ? id : null;
  });
}

function historyFromBefore(before) {
  const source = Array.isArray(before?.slotHistory) ? before.slotHistory : [];
  return Array.from({ length: 12 }, (_, i) => {
    const row = source[i];
    return Array.isArray(row) ? row.map(Number).filter((id) => Number.isFinite(id) && id > 0) : [];
  });
}

export function diffTeamState(before, after, { gameMode = 'BASEBALL' } = {}) {
  const teamId = Number(after?.teamId ?? before?.teamId) || 0;
  const beforeLineup = Array.isArray(before?.lineup) ? before.lineup : [];
  const afterLineup = Array.isArray(after?.lineup) ? after.lineup : [];
  const slots = Math.max(beforeLineup.length, afterLineup.length);
  const substitutions = [];
  const positionChanges = [];

  for (let i = 0; i < slots; i++) {
    const out = beforeLineup[i];
    const inP = afterLineup[i];
    const outId = playerId(out);
    const inId = playerId(inP);
    if (!outId && !inId) continue;
    const slot = i + 1;
    if (outId !== inId) {
      substitutions.push({
        type: 'SUBSTITUTION',
        slot,
        outPlayerId: outId || null,
        inPlayerId: inId || null,
        outName: playerName(out, outId),
        inName: playerName(inP, inId),
        outPosition: normalizePosition(out?.position),
        inPosition: normalizePosition(inP?.position),
      });
      continue;
    }
    const outPos = normalizePosition(out?.position);
    const inPos = normalizePosition(inP?.position);
    if (outPos !== inPos) {
      positionChanges.push({
        type: 'POSITION_CHANGE',
        slot,
        playerId: outId,
        name: playerName(inP, inId),
        outPosition: outPos,
        inPosition: inPos,
      });
    }
  }

  const beforePitcherId = Number(before?.fpId) || 0;
  const afterPitcherId = Number(after?.fpId) || 0;
  let pitchingChange = null;
  if (beforePitcherId !== afterPitcherId && (beforePitcherId > 0 || afterPitcherId > 0)) {
    const outPitcher = findPlayer(before, beforePitcherId);
    const inPitcher = findPlayer(after, afterPitcherId);
    pitchingChange = {
      outPlayerId: beforePitcherId || null,
      inPlayerId: afterPitcherId || null,
      outName: playerName(outPitcher, beforePitcherId),
      inName: playerName(inPitcher, afterPitcherId),
    };
  }

  const beforeHasDh = beforeLineup.some((p) => normalizePosition(p?.position) === 'DH');
  const afterHasDh = afterLineup.some((p) => normalizePosition(p?.position) === 'DH');
  // 取消 DH 的两种净变化形态：
  // 1) DH 槽位被投手（或互换后的投手）占用；
  // 2) DH 移防后打线不再有 DH，且原投手进入打序（换上者 = before 的投手）。
  // 垒球 11 人 P↔野手 / DH↔野手互换后打线仍有 DH，不得误标；同人只换守位也不算。
  const dhSub = substitutions.find((s) => s.outPosition === 'DH' && s.inPosition !== 'DH') ?? null;
  const pitcherEnteredLineup = beforePitcherId > 0
    ? substitutions.find((s) => s.inPlayerId === beforePitcherId) ?? null
    : null;
  const dhCancelSource = beforeHasDh && !afterHasDh ? (dhSub ?? pitcherEnteredLineup) : null;
  const dhCancel = dhCancelSource
    ? {
      slot: dhCancelSource.slot,
      outPlayerId: dhCancelSource.outPlayerId,
      inPlayerId: dhCancelSource.inPlayerId,
      outName: dhCancelSource.outName,
      inName: dhCancelSource.inName,
    }
    : null;

  return {
    teamId,
    gameMode,
    substitutions,
    positionChanges,
    pitchingChange,
    dhCancel,
    hasChanges: substitutions.length > 0 || positionChanges.length > 0 || pitchingChange != null,
  };
}

/** 打席数：优先取 pa，缺失时按 AB+BB+HBP+SF+SH+IO 计算。 */
export function battingAppearances(player) {
  const b = player?.stats?.batting;
  if (!b) return 0;
  const pa = Number(b.pa);
  if (Number.isFinite(pa) && pa > 0) return pa;
  return [b.ab, b.bb, b.hbp, b.sf, b.sh, b.io]
    .reduce((sum, value) => sum + (Number(value) || 0), 0);
}

/** 实际投过球的投手数，用于判断对方是否已经换投。 */
export function countActivePitchers(players) {
  const activeIds = new Set();
  for (const player of Array.isArray(players) ? players : []) {
    const id = playerId(player);
    if (!id) continue;
    const p = player?.stats?.pitching ?? {};
    const active = [p.np, p.pitchBf, p.pitchPa, p.ip, p.pitchSo, p.pitchH, p.wp, p.bk, p.er]
      .some((value) => (Number(value) || 0) > 0);
    if (active) activeIds.add(id);
  }
  return activeIds.size;
}

/** 当前 DH 槽位与先发 DH；starterIds 缺失时回落到当前 DH。 */
export function startingDhInfo(team) {
  const lineup = Array.isArray(team?.lineup) ? team.lineup : [];
  const dhIndex = lineup.findIndex((player) => normalizePosition(player?.position) === 'DH');
  if (dhIndex < 0) return null;
  const starterIds = Array.isArray(team?.starterIds) ? team.starterIds : [];
  const starterId = Number(starterIds[dhIndex]) || 0;
  const current = lineup[dhIndex] ?? null;
  const candidates = [
    ...lineup,
    ...(Array.isArray(team?.bench) ? team.bench : []),
    ...(Array.isArray(team?.roster) ? team.roster : []),
    team?.fpRow,
  ].filter(Boolean);
  const starter = starterId
    ? candidates.find((player) => playerId(player) === starterId) ?? null
    : current;
  return { dhIndex, current, starterId, starter };
}

/**
 * DH 取消规则（MLB OBR 5.11(a)(2)/(8)/(14)）：
 * 先发 DH 至少完成一次打击，除非对方已经换投；P↔野手互换、DH 进守备等也会终止 DH。
 * 实时数据无法覆盖所有换投细节，因此返回 warning 供操作员确认，而不是静默放行。
 */
export function canCancelDh({ team, opponentPlayers, sport = 'BASEBALL' } = {}) {
  const info = startingDhInfo(team);
  if (!info) return { ok: false, warning: '当前打线没有 DH。' };
  if (!info.starter) {
    return { ok: false, warning: '无法确认先发 DH，请核对阵容后再取消 DH。' };
  }
  // 先发 DH 至少打击一次的规定（MLB OBR 5.11）只适用于棒球；垒球不弹此提醒。
  if (String(sport).toUpperCase() === 'SOFTBALL') return { ok: true, warning: '' };
  const batted = battingAppearances(info.starter) > 0;
  const opponentChanged = countActivePitchers(opponentPlayers) >= 2;
  if (batted || opponentChanged) return { ok: true, warning: '' };
  return {
    ok: false,
    warning: `规则要求先发 DH 至少完成一次打击（对方换投除外）。先发 DH ${playerName(info.starter, info.starterId)} 尚未打击，当前取消 DH 可能不合规，请确认对方已换投或已满足条件。`,
  };
}

export function summarizeTeamDiff(diff, teamName = '') {
  const lines = [];
  const prefix = teamName ? `【${teamName}】` : '';
  for (const s of diff?.substitutions ?? []) {
    const out = s.outPlayerId ? `${s.outName}(${s.outPosition || '-'})` : '空位';
    const inP = s.inPlayerId ? `${s.inName}(${s.inPosition || '-'})` : '空位';
    const dh = diff?.dhCancel && diff.dhCancel.slot === s.slot && diff.dhCancel.outPlayerId === s.outPlayerId
      ? '（取消DH）'
      : '';
    lines.push(`${prefix}第${s.slot}棒 ${out} → ${inP}${dh}`);
  }
  for (const p of diff?.positionChanges ?? []) {
    lines.push(`${prefix}守位调整：${p.name} ${p.outPosition || '-'} → ${p.inPosition || '-'}`);
  }
  if (diff?.pitchingChange) {
    const covered = (diff.substitutions ?? []).some(
      (s) => s.outPlayerId === diff.pitchingChange.outPlayerId && s.inPlayerId === diff.pitchingChange.inPlayerId,
    );
    if (!covered) lines.push(`${prefix}投手更换：${diff.pitchingChange.outName} → ${diff.pitchingChange.inName}`);
  }
  return lines;
}

export function buildSubstitutionEvents(diffsBySide, { transactionId, inning, topBottom } = {}) {
  const events = [];
  for (const diff of diffsBySide ?? []) {
    if (!diff?.teamId) continue;
    const teamId = Number(diff.teamId);
    for (const s of diff.substitutions ?? []) {
      const dh = diff.dhCancel && diff.dhCancel.slot === s.slot && diff.dhCancel.outPlayerId === s.outPlayerId;
      events.push({
        transactionId,
        teamId,
        inning: inning ?? null,
        topBottom: topBottom ?? null,
        changeType: dh ? 'DH_CANCEL' : 'SUBSTITUTION',
        slot: s.slot,
        outPlayerId: s.outPlayerId,
        inPlayerId: s.inPlayerId,
        outPosition: s.outPosition || null,
        inPosition: s.inPosition || null,
      });
    }
    for (const p of diff.positionChanges ?? []) {
      events.push({
        transactionId,
        teamId,
        inning: inning ?? null,
        topBottom: topBottom ?? null,
        changeType: 'POSITION_CHANGE',
        slot: p.slot,
        outPlayerId: p.playerId,
        inPlayerId: p.playerId,
        outPosition: p.outPosition || null,
        inPosition: p.inPosition || null,
      });
    }
    if (diff.pitchingChange) {
      const covered = (diff.substitutions ?? []).some(
        (s) => s.outPlayerId === diff.pitchingChange.outPlayerId && s.inPlayerId === diff.pitchingChange.inPlayerId,
      );
      if (!covered) {
        events.push({
          transactionId,
          teamId,
          inning: inning ?? null,
          topBottom: topBottom ?? null,
          changeType: 'PITCHING_CHANGE',
          slot: null,
          outPlayerId: diff.pitchingChange.outPlayerId,
          inPlayerId: diff.pitchingChange.inPlayerId,
          outPosition: 'P',
          inPosition: 'P',
        });
      }
    }
  }
  return events;
}

export function applyNetTeamState(before, after, diff, {
  sport = 'BASEBALL',
  reentryAllowed = String(sport).toUpperCase() === 'SOFTBALL',
} = {}) {
  const starterIds = starterIdsFromBefore(before);
  const slotHistory = historyFromBefore(before);
  const unavailable = Array.from(new Set((before?.unavailable ?? []).map(Number).filter((id) => Number.isFinite(id) && id > 0)));
  // 关闭态读时回填：历史快照可能未把垒球换下者写入 unavailable；替补席上的前打序球员一律视为不可再上。
  if (!reentryAllowed) {
    const formerIds = new Set([...starterIds, ...slotHistory.flat()]
      .map(Number).filter((id) => Number.isFinite(id) && id > 0));
    for (const p of before?.bench ?? []) {
      const id = Number(p?.id);
      if (id > 0 && formerIds.has(id) && !unavailable.includes(id)) unavailable.push(id);
    }
  }

  for (const s of diff?.substitutions ?? []) {
    const slotIndex = Number(s.slot) - 1;
    if (slotIndex < 0 || slotIndex >= 12) continue;
    const outId = Number(s.outPlayerId) || 0;
    const inId = Number(s.inPlayerId) || 0;
    if (outId > 0 && outId !== inId) {
      const starterId = Number(starterIds[slotIndex]) || 0;
      if (starterId !== outId && !slotHistory[slotIndex].includes(outId)) {
        slotHistory[slotIndex] = [...slotHistory[slotIndex], outId];
      }
      // 大谷规则：DH兼投被代打但继续投球（或离开打序后仍为守备投手）时仍在比赛中，不能进不可再上
      const outStillActive = (after?.lineup ?? []).some((p) => playerId(p) === outId)
        || Number(after?.fpId) === outId
        || playerId(after?.fpRow) === outId;
      if (!outStillActive && !reentryAllowed && !unavailable.includes(outId)) {
        unavailable.push(outId);
      }
    }
    if (inId > 0) {
      const idx = unavailable.indexOf(inId);
      if (idx >= 0) unavailable.splice(idx, 1);
    }
  }

  const pitchingChange = diff?.pitchingChange;
  const outPitcherId = Number(pitchingChange?.outPlayerId) || 0;
  const inPitcherId = Number(pitchingChange?.inPlayerId) || 0;
  const outPitcherStillActive = outPitcherId > 0 && (
    (after?.lineup ?? []).some((p) => playerId(p) === outPitcherId)
    || Number(after?.fpId) === outPitcherId
    || playerId(after?.fpRow) === outPitcherId
  );
  if (outPitcherId > 0 && outPitcherId !== inPitcherId && !outPitcherStillActive
      && !reentryAllowed && !unavailable.includes(outPitcherId)) {
    unavailable.push(outPitcherId);
  }
  if (inPitcherId > 0) {
    const idx = unavailable.indexOf(inPitcherId);
    if (idx >= 0) unavailable.splice(idx, 1);
  }

  const outPositions = new Map();
  for (const s of diff?.substitutions ?? []) {
    if (s.outPlayerId && s.outPosition) outPositions.set(Number(s.outPlayerId), s.outPosition);
  }
  if (outPitcherId > 0 && !outPitcherStillActive && !outPositions.has(outPitcherId)) {
    outPositions.set(outPitcherId, 'P');
  }
  return {
    starterIds,
    slotHistory,
    unavailable,
    outPositions,
  };
}

/**
 * 有效不可再上名单（Issue #479）：
 * - 显式 unavailable；
 * - 关闭再上场时，回填替补席上的前打序球员（先发槽或换人史出现过的球员），
 *   兼容历史快照里垒球未写 unavailable 的场次。
 */
export function effectiveUnavailableIds({
  unavailable, bench, starterIds, slotHistory, reentryAllowed = false,
} = {}) {
  const ids = new Set((Array.isArray(unavailable) ? unavailable : [])
    .map(Number).filter((id) => Number.isFinite(id) && id > 0));
  if (!reentryAllowed) {
    const formerIds = new Set([
      ...(Array.isArray(starterIds) ? starterIds : []),
      ...(Array.isArray(slotHistory) ? slotHistory.flat() : []),
    ].map(Number).filter((id) => Number.isFinite(id) && id > 0));
    for (const p of Array.isArray(bench) ? bench : []) {
      const id = Number(p?.id);
      if (id > 0 && formerIds.has(id)) ids.add(id);
    }
  }
  return [...ids];
}

/**
 * 同一换人事务内的再上场拦截（Issue #479）：关闭再上场时，任何被换下者不得在同事务内以
 * 其他槽位/投手身份重新上场（净 diff 会同时出现该人的 out 与 in，绕过 unavailable 判断）。
 */
export function findReentryInTransaction(diffsBySide, { reentryAllowed = false } = {}) {
  if (reentryAllowed) return null;
  for (const diff of Array.isArray(diffsBySide) ? diffsBySide : []) {
    const substitutions = Array.isArray(diff?.substitutions) ? diff.substitutions : [];
    const outs = new Set(substitutions.map((s) => Number(s?.outPlayerId))
      .filter((id) => Number.isFinite(id) && id > 0));
    const hit = substitutions.find((s) => outs.has(Number(s?.inPlayerId)));
    if (hit) {
      return { teamId: Number(diff?.teamId) || 0, inPlayerId: Number(hit.inPlayerId), slot: hit.slot };
    }
    const pitcherIn = Number(diff?.pitchingChange?.inPlayerId);
    if (Number.isFinite(pitcherIn) && pitcherIn > 0 && outs.has(pitcherIn)) {
      return { teamId: Number(diff?.teamId) || 0, inPlayerId: pitcherIn, slot: null };
    }
  }
  return null;
}

/**
 * 球员原打序槽位（0-based）：先发槽优先，其次换人史；纯板凳球员返回 null。
 * 供垒球再上场「回原棒次」校验使用（Issue #479）。
 */
export function originalLineupSlot(team, playerId) {
  const id = Number(playerId) || 0;
  if (!id) return null;
  const starters = Array.isArray(team?.starterIds) ? team.starterIds : [];
  const fromStarter = starters.findIndex((value) => Number(value) === id);
  if (fromStarter >= 0) return fromStarter;
  const history = Array.isArray(team?.slotHistory) ? team.slotHistory : [];
  for (let i = 0; i < history.length; i++) {
    const row = Array.isArray(history[i]) ? history[i] : [];
    if (row.some((value) => Number(value) === id)) return i;
  }
  return null;
}

/**
 * 垒球再上场同棒次校验（Issue #479）：纯板凳球员放行；有原槽位时必须回到该槽位。
 * targetSlot 为 -1/空表示无法定位目标打序（如仅守备投手行），对再上场球员同样阻止。
 */
export function validateReentrySlot({ team, playerId, targetSlot, text } = {}) {
  const tr = typeof text === 'function' ? text : null;
  const original = originalLineupSlot(team, playerId);
  if (original == null) return { ok: true, error: '' };
  const target = targetSlot == null || targetSlot === '' ? -1 : Number(targetSlot);
  if (target === original) return { ok: true, error: '' };
  if (target < 0 || !Number.isInteger(target)) {
    return {
      ok: false,
      error: tr
        ? tr('liveGame.sub.reentrySlotUnknown')
        : '无法确认该球员的原打序槽位，请从其原打序位置换人',
    };
  }
  const slot = original + 1;
  return {
    ok: false,
    error: tr
      ? tr('liveGame.sub.reentrySlot', { slot })
      : `垒球再上场必须回到原打序（第${slot}棒）`,
  };
}
