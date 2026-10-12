const ERROR_RUN_CAUSES = new Set(['ERROR', 'THROWING_ERROR']);

export function decideRbiForRun({
  runCause = 'BATTER_ACTION',
  wouldScoreWithoutError = null,
  manualRbi = null
} = {}) {
  if (typeof manualRbi === 'boolean') return manualRbi;
  if (ERROR_RUN_CAUSES.has(runCause)) return wouldScoreWithoutError === true;
  return true;
}

export function isDoublePlayThrowingErrorException({
  firstBaseOccupied = false,
  outsBefore = 0,
  outsAdded = 0,
  throwingError = false,
  doublePlayCompleted = false
} = {}) {
  return Boolean(
    firstBaseOccupied &&
    Number(outsBefore) < 2 &&
    Number(outsAdded) >= 1 &&
    throwingError &&
    !doublePlayCompleted
  );
}
