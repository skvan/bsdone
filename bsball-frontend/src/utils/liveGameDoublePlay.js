// 双杀/三杀出局归属（#530）
// 传球链只记录球从谁到谁；出局由「拖拽持球守备员到垒包」的 stepAction 逐个记录：
// 每次拖拽产生一个被迫进垒的封杀出局点，链上的顺序即出局顺序。
// 出局跑者取该垒的被迫进垒者；该垒没有被迫跑者时未决，不再退到触杀。
// 打者只有在一垒出局点被记录时出局，否则安全上一垒。
// 纯函数，不依赖 LiveGame 状态；返回的跑者是入参对象引用，便于调用方直接落位。

const OUT_COUNT = { DP: 2, TP: 3 };

// 双杀/三杀入口门槛：一垒必须有人（一垒跑者在一垒被迫进二垒），
// 仅二垒/三垒有人且非被迫进垒时不算双杀，按单出局分别记录。
export function canStartDoublePlay(runners) {
  return Boolean(runners?.[1]);
}

export function canStartTriplePlay(runners) {
  return Boolean(runners?.[1] && runners?.[2]);
}

// 双杀出局只记被迫进垒的封杀：一垒=打者、二垒=一垒跑者、三垒=二垒跑者、本垒=三垒跑者。
const FORCED_RUNNER = { 1: 'batter', 2: '1', 3: '2', 4: '3' };
export const BASE_NAME = { 1: '一垒', 2: '二垒', 3: '三垒', 4: '本垒' };

function normalizePos(value) {
  return String(value ?? '').trim().toUpperCase();
}

function normalizeBase(value) {
  const base = Math.floor(Number(value));
  return base >= 1 && base <= 4 ? base : null;
}

function normalizeActions(steps, stepActions) {
  const seq = (Array.isArray(steps) ? steps : []).map(normalizePos).filter(Boolean);
  const actions = [];
  const source = Array.isArray(stepActions) ? stepActions : [];
  const readAction = (index) => {
    const raw = source[index];
    if (raw == null) return { action: null, base: null };
    if (typeof raw === 'string') return { action: raw, base: null };
    return {
      action: raw.action === 'force' || raw.action === 'tag' ? raw.action : null,
      base: normalizeBase(raw.base)
    };
  };
  for (let i = 0; i < seq.length; i += 1) {
    if (i + 1 < seq.length && seq[i] === seq[i + 1]) {
      const actionIndex = i + 1;
      const explicit = readAction(actionIndex);
      if (!explicit.action && !explicit.base) {
        const fallback = readAction(i);
        explicit.action = fallback.action;
        explicit.base = fallback.base;
      }
      actions.push({ pos: seq[i], actionIndex, ...explicit });
      i += 1;
    } else {
      actions.push({ pos: seq[i], actionIndex: i, ...readAction(i) });
    }
  }
  return actions;
}

function runnerFromBase(runnerKey) {
  return runnerKey === 'batter' ? 0 : Number(runnerKey);
}

// 在确定出局后，把剩余跑者按被迫进垒推进；领先跑者优先占垒，后方跑者逐垒后退，
// 退到原垒仍无位置则记为冲突，由调用方提示手选。
function placeRemainingRunners(runners, used, batterSafe, batter, runner3Decision) {
  const bases = { 1: null, 2: null, 3: null };
  let conflict = false;
  const place = (runnerKey, preferred, lowest) => {
    for (let base = preferred; base >= lowest; base -= 1) {
      if (!bases[base]) {
        bases[base] = runners[runnerKey];
        return true;
      }
    }
    conflict = true;
    return false;
  };
  if (batterSafe && batter) bases[1] = batter;
  if (runners[3] && !used.has('3')) {
    if (runner3Decision !== 'safe' && runner3Decision !== 'out') place('3', 3, 3);
  }
  if (runners[2] && !used.has('2')) place('2', 3, 2);
  if (runners[1] && !used.has('1')) place('1', 2, 1);
  return { bases, conflict };
}

export function resolveDoublePlay({
  type = 'DP',
  steps,
  stepActions,
  runners,
  batter,
  overrides,
  runner3Decision = null
} = {}) {
  const outCount = OUT_COUNT[type] ?? 2;
  const actions = normalizeActions(steps, stepActions).map((action, arrayIndex) => ({ ...action, arrayIndex }));
  const pre = {
    batter: batter ?? null,
    1: runners?.[1] ?? null,
    2: runners?.[2] ?? null,
    3: runners?.[3] ?? null
  };
  // 出局点 = 带垒包和封杀/触杀的拖拽动作；链顺序即出局顺序
  const explicitOuts = actions.filter((action) => action.base != null && (action.action === 'force' || action.action === 'tag'));
  const selected = explicitOuts.slice(0, outCount);
  const extraOuts = explicitOuts.slice(outCount);
  const selectedIndex = new Map(selected.map((action, index) => [action.arrayIndex, index]));
  const used = new Set();
  const outs = [];
  const unresolved = [];

  for (let index = 0; index < selected.length; index += 1) {
    const action = selected[index];
    const override = overrides?.[index] ?? null;
    const base = normalizeBase(override?.base) ?? action.base;
    let runnerKey = override?.runnerKey ?? null;
    if (runnerKey != null && (used.has(runnerKey) || !pre[runnerKey])) runnerKey = null;
    if (runnerKey == null) {
      const forcedKey = FORCED_RUNNER[base];
      if (forcedKey && !used.has(forcedKey) && pre[forcedKey]) runnerKey = forcedKey;
    }
    if (!runnerKey) {
      unresolved.push({ index, fielder: action.pos, base, reason: 'runner' });
      outs.push({ index, fielder: action.pos, base, runner: null, runnerKey: null, fromBase: null, action: null });
      continue;
    }
    const chosenAction = 'force';
    used.add(runnerKey);
    outs.push({
      index,
      fielder: action.pos,
      base,
      runner: pre[runnerKey],
      runnerKey,
      fromBase: runnerFromBase(runnerKey),
      action: chosenAction
    });
  }

  const complete = selected.length === outCount && extraOuts.length === 0;
  const missingBatter = !pre.batter;
  const lastSelected = selected[selected.length - 1] ?? null;
  const batterOut = outs.some((out) => out.runnerKey === 'batter');
  const batterSafe = !batterOut && !missingBatter;
  const runner3Pending = Boolean(complete && pre[3] && !used.has('3'));
  const needsRunner3Decision = Boolean(runner3Pending && pre[1] && pre[2] && batterOut && runner3Decision == null);
  const placement = placeRemainingRunners(pre, used, batterSafe, pre.batter, runner3Decision);
  const runner3Run = Boolean(runner3Pending && runner3Decision === 'safe');
  const valid = complete && !missingBatter && unresolved.length === 0 && !placement.conflict;

  const actionResults = actions.map((action) => {
    const outIndex = selectedIndex.get(action.arrayIndex);
    if (outIndex == null) return { ...action, base: null, runner: null, out: false };
    const out = outs[outIndex];
    return {
      ...action,
      base: out?.base ?? action.base,
      action: out?.action ?? action.action,
      runner: out?.runner ?? null,
      out: true
    };
  });

  return {
    valid,
    reason: missingBatter
      ? 'missing_batter'
      : selected.length < outCount
        ? 'outs_incomplete'
        : extraOuts.length
          ? 'too_many_outs'
          : unresolved.length
            ? 'unresolved'
            : placement.conflict
              ? 'advance_conflict'
              : null,
    outCount,
    needsOuts: Math.max(0, outCount - selected.length),
    lastOutStepIndex: lastSelected?.actionIndex ?? null,
    actions: actionResults,
    outs,
    unresolved,
    batterOut,
    batterSafe,
    gdp: type === 'DP' && batterOut,
    finalBases: placement.bases,
    needsRunner3Decision,
    runner3Run
  };
}
