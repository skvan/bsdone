const RUNNER_ORIGINS = new Set(['NORMAL', 'ERROR', 'TIE_BREAK']);
const PENDING_RULINGS = new Set(['PENDING', 'UNKNOWN']);

export function createLivePlayEvent(input = {}) {
  const pitcherId = positiveId(input.pitcherId);
  const scoringRunners = (input.scoringRunners ?? []).map((runner) => ({
    runnerId: positiveId(runner.runnerId),
    responsiblePitcherId: positiveId(runner.responsiblePitcherId ?? pitcherId),
    origin: RUNNER_ORIGINS.has(runner.origin) ? runner.origin : 'NORMAL',
    overrideStatus: PENDING_RULINGS.has(runner.overrideStatus)
      ? 'PENDING'
      : runner.overrideStatus ?? null,
    overrideReason: runner.overrideReason ?? null
  })).filter((runner) => runner.runnerId && runner.responsiblePitcherId);

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

export function toEarnedRunPlay(event) {
  return {
    playId: numericId(event.playId),
    sequence: event.sequence,
    actualOutsAdded: event.actualOutsAdded,
    reconstructedOutsAdded: event.reconstructedOutsAdded,
    rulingPending: event.rulingPending,
    scoringRunners: event.scoringRunners
  };
}

function normalizeBases(bases = {}) {
  return { 1: positiveId(bases[1]), 2: positiveId(bases[2]), 3: positiveId(bases[3]) };
}

function positiveId(value) {
  const id = Number(value);
  return Number.isInteger(id) && id > 0 ? id : null;
}

function numericId(value) {
  const id = Number(value);
  return Number.isInteger(id) && id > 0 ? id : Date.now();
}

function clampOuts(value) {
  return Math.max(0, Math.min(3, Number(value ?? 0)));
}
