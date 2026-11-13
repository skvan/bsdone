// 强迫进垒推进计划（纯函数）。
// 球在界内被击出后，只有被击球员进占垒位挤出的跑者、以及连锁被挤的跑者才自动前进。
// 返回的 runner 保持入参对象引用，调用方直接落位；回本垒的跑者交给本垒确认流程。

const BASES = [1, 2, 3];

function createPlacer(slots, homeRunners) {
  return function place(runner, from, target) {
    const base = target;
    if (base > 3) {
      homeRunners.push({ runner, from });
      return;
    }
    if (slots[base]) {
      const occupant = slots[base];
      slots[base] = null;
      place(occupant.runner, occupant.from, base + 1);
    }
    slots[base] = { runner, from };
  };
}

function collectPlan(slots, homeRunners, batterTo, batterHome) {
  const finalBases = {
    1: slots[1]?.runner ?? null,
    2: slots[2]?.runner ?? null,
    3: slots[3]?.runner ?? null
  };
  const moves = [];
  for (const base of BASES) {
    const slot = slots[base];
    if (!slot || slot.batter || !slot.runner || slot.from === base) continue;
    moves.push({ runner: slot.runner, from: slot.from, to: base });
  }
  return {
    batterTo,
    batterHome,
    moves,
    homeRunners,
    finalBases
  };
}

// 击球员到达 batterBase（1-4）后，被挤出的跑者与强迫链。
// 例：仅二垒有人 + 一垒安打 => 二垒跑者留二垒；一垒有人 + 一垒安打 => 一垒跑者到二垒。
export function resolveBatterForceAdvance({ batterBase, runners, batter } = {}) {
  const base = Math.floor(Number(batterBase));
  if (!(base >= 1 && base <= 4)) return null;
  const slots = { 1: null, 2: null, 3: null };
  const homeRunners = [];
  const place = createPlacer(slots, homeRunners);

  for (let from = 3; from >= 1; from -= 1) {
    const runner = runners?.[from];
    if (!runner) continue;
    const target = from > base ? from : base + 1;
    place(runner, from, target);
  }

  const batterHome = base === 4;
  if (!batterHome) slots[base] = { runner: batter ?? null, from: 0, batter: true };
  return collectPlan(slots, homeRunners, base, batterHome);
}

// 打者在一垒出局（地滚球）；只有一垒有人的连续强迫链才前进。
// 满垒时三垒跑者回本垒，交给本垒确认（安全/出局）。
export function resolveGroundOutForceAdvance({ runners } = {}) {
  const slots = { 1: null, 2: null, 3: null };
  const homeRunners = [];
  const place = createPlacer(slots, homeRunners);
  let top = 0;
  while (top < 3 && runners?.[top + 1]) top += 1;
  for (let base = 3; base > top; base -= 1) {
    if (runners?.[base]) slots[base] = { runner: runners[base], from: base };
  }
  for (let from = Math.min(top, 3); from >= 1; from -= 1) {
    place(runners[from], from, from + 1);
  }
  return collectPlan(slots, homeRunners, 0, false);
}
