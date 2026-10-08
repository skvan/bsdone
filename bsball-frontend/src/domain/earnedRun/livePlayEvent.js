const RUNNER_ORIGINS = new Set(['NORMAL', 'ERROR', 'TIE_BREAK', 'WILD_PITCH', 'PASSED_BALL', 'INTERFERENCE']);
const PENDING_RULINGS = new Set(['PENDING', 'UNKNOWN']);

export function createLivePlayEvent(input = {}) {
  const pitcherId = positiveId(input.pitcherId);
  const scoringRunners = normalizeScoringRunners(input.scoringRunners, pitcherId, input.resultCode);

  return {
    playId: String(input.playId ?? input.id ?? `play-${Date.now()}`),
    sequence: Math.max(1, Number(input.sequence ?? 1)),
    inning: Math.max(1, Number(input.inning ?? 1)),
    half: input.half === 'bottom' ? 'bottom' : 'top',
    actualOutsAdded: clampOuts(input.actualOutsAdded),
    reconstructedOutsAdded: clampOuts(input.reconstructedOutsAdded ?? input.actualOutsAdded),
    rulingPending: Boolean(input.rulingPending),
    resultCode: input.resultCode ?? 'OTHER',
    resultText: input.resultText ?? '',
    batterPlayerId: positiveId(input.batterPlayerId),
    pitcherPlayerId: pitcherId,
    basesBefore: normalizeBases(input.basesBefore),
    basesAfter: normalizeBases(input.basesAfter),
    scoringRunners,
    scoreDelta: {
      away: Math.max(0, Number(input.scoreDelta?.away ?? 0)),
      home: Math.max(0, Number(input.scoreDelta?.home ?? 0))
    },
    recordedAt: input.recordedAt ?? Date.now()
  };
}

// 桥接层：把实时录入写入的 playEvents（含 outsBefore/outsAfter 与得分跑者）
// 归一化为 #149 第一阶段契约所需的 EarnedRunPlay 载荷；第二阶段将由
// runner movement 重放接管（见 issue #149 / #203）。
export function toEarnedRunPlay(event = {}) {
  const pitcherId = positiveId(event.pitcherPlayerId);
  const actualOutsAdded = clampOuts(
    event.actualOutsAdded ?? (Number(event.outsAfter ?? 0) - Number(event.outsBefore ?? 0))
  );
  return {
    playId: resolvePlayId(event),
    sequence: Math.max(1, Math.floor(Number(event.sequence ?? 1)) || 1),
    actualOutsAdded,
    reconstructedOutsAdded: clampOuts(event.reconstructedOutsAdded ?? actualOutsAdded),
    rulingPending: Boolean(event.rulingPending),
    scoringRunners: normalizeScoringRunners(event.scoringRunners, pitcherId, event.resultCode)
  };
}

function normalizeScoringRunners(runners, pitcherId, resultCode) {
  return (Array.isArray(runners) ? runners : []).map((runner) => ({
    runnerId: positiveId(runner.runnerId),
    responsiblePitcherId: positiveId(runner.responsiblePitcherId ?? pitcherId),
    origin: inferRunnerOrigin(runner.origin, resultCode),
    overrideStatus: PENDING_RULINGS.has(runner.overrideStatus)
      ? 'PENDING'
      : runner.overrideStatus ?? null,
    overrideReason: runner.overrideReason ?? null
  })).filter((runner) => runner.runnerId && runner.responsiblePitcherId);
}

// 由「局次+攻守+sequence」生成稳定正数 playId：同一半局重放幂等，且满足后端
// playId>0 与 (game_id, source_event_id) 唯一约束；不用时间戳，避免同批次撞号。
function resolvePlayId(event) {
  const explicit = Number(event.playId);
  if (Number.isInteger(explicit) && explicit > 0) return explicit;
  const inning = Math.max(1, Math.floor(Number(event.inning ?? 1)) || 1);
  const sequence = Math.max(1, Math.floor(Number(event.sequence ?? 1)) || 1);
  const half = event.half === 'bottom' ? 1 : 0;
  return inning * 1000000 + half * 500000 + (sequence % 500000);
}

export function inferRunnerOrigin(explicitOrigin, resultCode) {
  if (RUNNER_ORIGINS.has(explicitOrigin)) return explicitOrigin;
  const code = String(resultCode ?? '').toUpperCase();
  if (code === 'TIE_BREAK' || code === 'TBR') return 'TIE_BREAK';
  if (code === 'ADVANCE_FE' || code === 'ERROR_ADVANCE') return 'ERROR';
  if (/^E(?:[0-9]+)?$/.test(code) || code.startsWith('ERROR')) return 'ERROR';
  if (code === 'WP' || code === 'WILD_PITCH' || code === 'DK3_REACH_WP') return 'WILD_PITCH';
  if (code === 'PB' || code === 'PASSED_BALL' || code === 'CATCHER_ERROR'
    || code === 'DK3_REACH_PB') return 'PASSED_BALL';
  if (code === 'INTERFERENCE' || code === 'INT') return 'INTERFERENCE';
  return 'NORMAL';
}

function normalizeBases(bases = {}) {
  return { 1: positiveId(bases[1]), 2: positiveId(bases[2]), 3: positiveId(bases[3]) };
}

function positiveId(value) {
  const id = Number(value);
  return Number.isInteger(id) && id > 0 ? id : null;
}

function clampOuts(value) {
  const outs = Number(value);
  return Number.isFinite(outs) ? Math.max(0, Math.min(3, outs)) : 0;
}
