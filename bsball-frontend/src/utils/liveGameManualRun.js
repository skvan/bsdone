// 手动进垒/纠偏得分的台账兜底（#449）
// 手动拖拽跑者回本垒、垒况纠偏、盗本垒等得分先进入 runLedger，正常要等下一个打席
// 落定（Qr）才写进 playEvents。如果比赛在下一个打席前保存/结束，台账不会进入 ER 引擎，
// 引擎按 playEvents 绝对重算投手 R 时会漏掉这一分，出现「比分 3、投手 R 2」。
// 这里把台账补成一条独立 playEvent：不改动打席统计，只保证 ER 输入与记分板一致。

// 同一半局要求 play sequence 严格递增；新事件取当前全部事件最大序 +1，
// 避免与后续 Qr（也走本函数）撞序。
export function nextPlaySequence(playEvents = []) {
  let max = 0;
  for (const event of Array.isArray(playEvents) ? playEvents : []) {
    const sequence = Math.floor(Number(event?.sequence));
    if (Number.isFinite(sequence) && sequence > max) {
      max = sequence;
    }
  }
  return max + 1;
}

// 把台账里尚未落定的得分组装成一条独立 playEvent。
// 返回 null 表示没有可入引擎的得分（无跑者 id），调用方不要清空台账以免吞分。
export function pendingRunsPlayEvent(entries, context = {}) {
  const runs = (Array.isArray(entries) ? entries : []).filter((entry) => Number(entry?.runs) > 0);
  if (runs.length === 0) return null;

  const scoringRunners = runs
    .map((entry) => toScoringRunner(entry))
    .filter(Boolean);
  if (scoringRunners.length === 0) return null;

  const inning = Math.max(1, Math.floor(Number(context.inning) || 1));
  const half = context.half === 'bottom' ? 'bottom' : 'top';
  const sequence = nextPlaySequence(context.playEvents);
  const runsScored = runs.reduce((total, entry) => total + Number(entry.runs || 0), 0);
  const now = Number(context.now) || Date.now();

  return {
    id: `manual-run-${inning}-${half}-${sequence}-${now}`,
    inning,
    half,
    sequence,
    batterPlayerId: Number(context.batter?.id) || null,
    batterName: context.batter?.name ?? null,
    pitcherPlayerId: Number(context.pitcher?.id) || null,
    pitcherName: context.pitcher?.name ?? null,
    resultCode: 'MANUAL_RUN',
    resultText: context.resultText ?? '手动进垒得分（保存前补记自责分）',
    rbi: 0,
    actualOutsAdded: 0,
    reconstructedOutsAdded: 0,
    outsBefore: Number(context.outs) || 0,
    outsAfter: Number(context.outs) || 0,
    basesBefore: { 1: null, 2: null, 3: null },
    basesAfter: {
      1: Number(context.runners?.[1]?.id) || null,
      2: Number(context.runners?.[2]?.id) || null,
      3: Number(context.runners?.[3]?.id) || null
    },
    scoringRunnerIds: scoringRunners.map((runner) => runner.runnerId),
    scoringRunners,
    scoreDelta: {
      away: half === 'top' ? runsScored : 0,
      home: half === 'bottom' ? runsScored : 0
    },
    recordedAt: now,
    manual: true
  };
}

// 台账条目映射为 ER 引擎的得分跑者；返回 null 表示没有可用跑者 id。
// 快照恢复时 runner 对象可能未绑定成功，但 runnerId 仍在台账里，两者取其一。
// Qr 与保存前补记共用，避免一处用 runner、一处用 runnerId 造成同类漏分。
export function toScoringRunner(entry, fallbackPitcherId = null) {
  const runnerId = Number(entry?.runner?.id ?? entry?.runnerId);
  if (!Number.isInteger(runnerId) || runnerId <= 0) return null;
  const responsiblePitcherId = Number(
    entry?.responsiblePitcherId ?? entry?.pitcher?.id ?? fallbackPitcherId ?? 0
  );
  return {
    runnerId,
    responsiblePitcherId: Number.isInteger(responsiblePitcherId) && responsiblePitcherId > 0
      ? responsiblePitcherId
      : null,
    origin: entry.origin ?? null,
    overrideStatus: entry.overrideStatus ?? null,
    overrideReason: entry.overrideReason ?? null,
    wouldScoreWithoutMisplay: entry.wouldScoreWithoutMisplay ?? entry.wouldScoreWithoutError ?? null,
    runCause: entry.runCause ?? null,
    wouldScoreWithoutError: entry.wouldScoreWithoutError ?? null,
    rbiCredit: typeof entry.rbiCredit === 'boolean' ? entry.rbiCredit : null,
    rbiReason: entry.rbiReason ?? null
  };
}
