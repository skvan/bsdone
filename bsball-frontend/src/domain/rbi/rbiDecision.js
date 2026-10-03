const RBI_RESULTS = new Set(['H1', 'H2', 'H3', 'HR', 'IPHR', 'SF', 'SH', 'FC', 'BB', 'IBB', 'HBP']);
const NO_RBI_CAUSES = new Set(['WILD_PITCH', 'PASSED_BALL', 'BALK']);
const DOUBLE_PLAY_CONTEXTS = new Set([
  'FORCE_DOUBLE_PLAY',
  'REVERSE_FORCE_DOUBLE_PLAY',
  'FAILED_DOUBLE_PLAY_OPPORTUNITY',
]);

export function decideRbi(input = {}) {
  const resultCode = String(input.resultCode ?? '').toUpperCase();
  const scoreCause = String(input.scoreCause ?? 'BATTED_BALL').toUpperCase();
  const doublePlayContext = String(input.doublePlayContext ?? 'NONE').toUpperCase();
  const errorPhase = String(input.errorPhase ?? 'NONE').toUpperCase();
  const wouldScoreWithOrdinaryDefense = input.wouldScoreWithOrdinaryDefense === true;

  if (NO_RBI_CAUSES.has(scoreCause)) {
    return decision(false, 'NON_BATTER_ADVANCE', input);
  }
  if (DOUBLE_PLAY_CONTEXTS.has(doublePlayContext) || resultCode === 'DP' || resultCode === 'TP') {
    return decision(false, 'DOUBLE_PLAY', input);
  }
  if (resultCode === 'E') {
    return wouldScoreWithOrdinaryDefense
      ? decision(true, 'ORDINARY_DEFENSE_RUN', input)
      : decision(false, 'ERROR_CREATED_RUN', input);
  }
  if (errorPhase !== 'NONE') {
    return errorPhase === 'AFTER_INITIAL_PLAY'
      && wouldScoreWithOrdinaryDefense
      && RBI_RESULTS.has(resultCode)
      ? decision(true, 'RUN_PRECEDED_ERROR', input)
      : decision(false, 'ERROR_CREATED_RUN', input);
  }
  return RBI_RESULTS.has(resultCode)
    ? decision(true, 'BATTER_CAUSED_RUN', input)
    : decision(false, 'NO_RBI_RESULT', input);
}

export function rbiContextForBattedBall({ playType, bipCode, basesBefore, outsBefore } = {}) {
  const resultCode = String(playType ?? '').toUpperCase();
  const normalizedCode = String(bipCode ?? '').toLowerCase();
  const sacrificeError = resultCode === 'E' && normalizedCode.includes(':sac:e');
  const failedDoublePlayOpportunity = resultCode === 'E'
    && normalizedCode.includes(':dp:e')
    && Boolean(basesBefore?.[1])
    && Number(outsBefore ?? 0) < 2;
  const doublePlayContext = resultCode === 'DP' || resultCode === 'TP'
    ? (normalizedCode.includes('reverse') ? 'REVERSE_FORCE_DOUBLE_PLAY' : 'FORCE_DOUBLE_PLAY')
    : failedDoublePlayOpportunity ? 'FAILED_DOUBLE_PLAY_OPPORTUNITY' : 'NONE';

  return {
    resultCode,
    scoreCause: 'BATTED_BALL',
    errorPhase: resultCode === 'E' ? 'INITIAL_PLAY' : 'NONE',
    wouldScoreWithOrdinaryDefense: sacrificeError,
    doublePlayContext,
    bipCode: normalizedCode || null,
  };
}

export function rbiContextForAdvance({ resultCode, advanceReasonId } = {}) {
  const reason = String(advanceReasonId ?? '');
  const samePlayError = ['safe_same_err', 'safe_same_err_rbi', 'safe_te', 'safe_fe'].includes(reason);
  const initialResult = String(resultCode ?? '').toUpperCase();

  return {
    resultCode: initialResult,
    scoreCause: 'BATTED_BALL',
    errorPhase: samePlayError ? 'AFTER_INITIAL_PLAY' : 'SEPARATE_PLAY',
    wouldScoreWithOrdinaryDefense: reason === 'safe_same_err_rbi' && RBI_RESULTS.has(initialResult),
    doublePlayContext: initialResult === 'DP' || initialResult === 'TP'
      ? 'FORCE_DOUBLE_PLAY'
      : 'NONE',
    advanceReasonId: reason || null,
  };
}

export function sacrificeStatForError({ bipCode, scoredRuns, runnerAdvanced } = {}) {
  const normalizedCode = String(bipCode ?? '').toLowerCase();
  if (!normalizedCode.includes(':sac:e')) return null;
  if (normalizedCode.startsWith('bip:b:')) {
    return Number(scoredRuns ?? 0) > 0 || runnerAdvanced === true ? 'SH' : null;
  }
  return Number(scoredRuns ?? 0) > 0 ? 'SF' : null;
}

function decision(creditRbi, reason, input) {
  return {
    creditRbi,
    reason,
    resultCode: String(input.resultCode ?? '').toUpperCase(),
    scoreCause: String(input.scoreCause ?? 'BATTED_BALL').toUpperCase(),
    errorPhase: String(input.errorPhase ?? 'NONE').toUpperCase(),
    wouldScoreWithOrdinaryDefense: input.wouldScoreWithOrdinaryDefense === true,
    doublePlayContext: String(input.doublePlayContext ?? 'NONE').toUpperCase(),
    bipCode: input.bipCode ?? null,
    advanceReasonId: input.advanceReasonId ?? null,
  };
}
