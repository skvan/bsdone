// 阵容状态与动作（纯状态规则，不 import 任何拖拽库；node 可直接单测）
// 行数组下标 = 棒次；空行（id<0）恒排尾部；行数 ∈ {baseCount, maxCount}
// 语义要点（设计稿 §五）：投手丘单卡槽；投手是否打击由打线列表表达；
//   放 DH → 投手自动移出打线（转仅守备）；移除 DH → 投手自动回打线
import { computed, ref } from 'vue';
// 注意：本文件要能被 node --test 直接加载 → 相对导入必须带 .js 扩展名
import { validateRosterPositions } from '../utils/starterFieldingValidation.js';
import {
  applyKeSpecial, emptyStats, findRowIndex, hasPlayer, isPlaceholder, makePlaceholderRow,
  makeRow, normalizeRows, posOf, slotRow,
} from '../utils/lineupBoardRules.js';

export function useLineupBoard(mode, teamId) {
  const rows = ref([]);
  const bench = ref([]);
  const roster = ref([]);
  const fieldingPitcherId = ref(null);
  const lastDhAddedFromPoolId = ref(null);
  const notice = ref('');

  const baseCount = computed(() => (mode.value === 'SOFTBALL' ? 10 : 9));
  const maxCount = computed(() => (mode.value === 'SOFTBALL' ? 11 : 10));
  const hasDh = computed(() => !!slotRow(rows.value, 'DH'));
  const hasP = computed(() => !!slotRow(rows.value, 'P'));

  const pool = computed(() =>
    roster.value.filter((p) => !hasPlayer(rows.value, p.id) && !hasPlayer(bench.value, p.id))
  );
  const validation = computed(() =>
    // 注意：validateRosterPositions 读的是行对象的 .position 字段 → 必须传行对象，不能传位置字符串数组
    applyKeSpecial(validateRosterPositions(rows.value, mode.value), rows.value, baseCount.value)
  );
  // 「仍需指定仅守备投手」= 处于 DH 制阵型（9 棒且无 P）且**尚未指定**投手
  const fieldingPitcherRequired = computed(
    () => rows.value.length === baseCount.value && validation.value.ok && !hasP.value && fieldingPitcherId.value == null
  );
  // required 已蕴含「投手未指定」→ 「已就绪」即「不再需要指定」
  const fieldingPitcherOk = computed(() => !fieldingPitcherRequired.value);
  const lineupComplete = computed(() => validation.value.ok && fieldingPitcherOk.value);
  const battingFull = computed(() => rows.value.filter((r) => !isPlaceholder(r)).length >= maxCount.value);

  function normalize() {
    rows.value = normalizeRows(rows.value, baseCount.value, maxCount.value, teamId.value);
  }

  function consumeNotice() {
    const n = notice.value;
    notice.value = '';
    return n;
  }

  function slotOfRow(code) {
    return slotRow(rows.value, code);
  }

  /** rows 含 P 行时同步守备投手（沿用旧件 WATCH 语义） */
  function syncPitcher() {
    const p = slotRow(rows.value, 'P');
    if (p) fieldingPitcherId.value = p.id;
  }

  function reset() {
    rows.value = [];
    normalize();
    bench.value = [];
    fieldingPitcherId.value = null;
    lastDhAddedFromPoolId.value = null;
  }

  function setRoster(list) {
    roster.value = Array.isArray(list) ? [...list] : [];
    reset();
  }

  function loadDraft(snapshot) {
    const src = (Array.isArray(snapshot?.rows) ? snapshot.rows : []).slice(0, maxCount.value);
    rows.value = src.map((row) =>
      isPlaceholder(row) ? makePlaceholderRow(teamId.value, 0) : makeRow(row, row.teamId ?? teamId.value, posOf(row))
    );
    normalize();
    bench.value = Array.isArray(snapshot?.bench) ? [...snapshot.bench] : [];
    fieldingPitcherId.value = snapshot?.fieldingPitcherId ?? null;
    lastDhAddedFromPoolId.value = snapshot?.lastDhAddedFromPoolId ?? null;
  }

  /** 清除某球员的一切痕迹（打线行 / 替补 / 守备投手 / lastDh） */
  function detachPlayer(playerId) {
    const idx = findRowIndex(rows.value, playerId);
    if (idx >= 0) {
      const next = [...rows.value];
      next[idx] = makePlaceholderRow(teamId.value, idx);
      rows.value = next;
    }
    bench.value = bench.value.filter((p) => String(p.id) !== String(playerId));
    if (fieldingPitcherId.value != null && String(fieldingPitcherId.value) === String(playerId)) fieldingPitcherId.value = null;
    if (lastDhAddedFromPoolId.value != null && String(lastDhAddedFromPoolId.value) === String(playerId)) lastDhAddedFromPoolId.value = null;
  }

  function takeFirstEmpty(row) {
    const next = [...rows.value];
    const e = next.findIndex((r) => isPlaceholder(r));
    if (e >= 0) next[e] = row; else next.push(row);
    rows.value = next;
  }

  /** 池/替补球员落到卡槽：空槽填入尾部空行；占用槽顶替，原占用者转出到起点区域 */
  function placeFromOutside(player, target) {
    const fromBench = hasPlayer(bench.value, player.id);
    const occ = slotRow(rows.value, target);
    if (occ) {
      const displaced = { ...occ };
      const next = [...rows.value];
      next[findRowIndex(rows.value, occ.id)] = makeRow(player, teamId.value, target);
      rows.value = next;
      if (fromBench) {
        bench.value = [...bench.value, { ...displaced, teamId: teamId.value, position: '', stats: displaced.stats ?? emptyStats() }];
      }
      if (lastDhAddedFromPoolId.value != null && String(displaced.id) === String(lastDhAddedFromPoolId.value)) {
        lastDhAddedFromPoolId.value = null;
      }
    } else {
      takeFirstEmpty(makeRow(player, teamId.value, target));
    }
    bench.value = bench.value.filter((p) => String(p.id) !== String(player.id));
  }

  /** 落到 DH 卡槽：先清掉该球员旧行 → 占位已有 DH 行或取尾部空行 → 决策 6b 投手移出打线 */
  function placeDh(player) {
    const fromBench = hasPlayer(bench.value, player.id);
    const occ = slotRow(rows.value, 'DH');
    const displaced = occ && String(occ.id) !== String(player.id) ? { ...occ } : null;
    let next = rows.value.map((r) =>
      !isPlaceholder(r) && String(r.id) === String(player.id) ? makePlaceholderRow(teamId.value, 0) : r
    );
    const row = makeRow(player, teamId.value, 'DH');
    const occIdx = next.findIndex((r) => !isPlaceholder(r) && posOf(r) === 'DH');
    if (occIdx >= 0) {
      next[occIdx] = row;
    } else {
      const e = next.findIndex((r) => isPlaceholder(r));
      if (e >= 0) next[e] = row; else next = [...next, row];
    }
    rows.value = next;
    if (displaced) {
      if (fromBench) {
        bench.value = [...bench.value, { ...displaced, teamId: teamId.value, position: '', stats: displaced.stats ?? emptyStats() }];
      }
      if (lastDhAddedFromPoolId.value != null && String(displaced.id) === String(lastDhAddedFromPoolId.value)) {
        lastDhAddedFromPoolId.value = null;
      }
    }
    bench.value = bench.value.filter((p) => String(p.id) !== String(player.id));
    lastDhAddedFromPoolId.value = player.id;
    // 决策 6b：放 DH → 投手移出打线（转仅守备）
    const pRow = slotRow(rows.value, 'P');
    if (pRow && String(pRow.id) !== String(player.id)) {
      fieldingPitcherId.value = pRow.id;
      const idx = findRowIndex(rows.value, pRow.id);
      const n2 = [...rows.value];
      n2[idx] = makePlaceholderRow(teamId.value, idx);
      rows.value = n2;
      notice.value = '已按 DH 制把投手移出打线；如需 10 人打击，请把投手拖回打线';
    }
  }

  /** 拖放到球场卡槽（唯一入口） */
  function placeAtSlot(player, code) {
    const target = String(code ?? '').toUpperCase();
    if (!target || player == null) return;
    const i = findRowIndex(rows.value, player.id);
    const inRows = i >= 0;

    if (target === 'DH') {
      placeDh(player);
      normalize();
      syncPitcher();
      return;
    }
    // 决策 15：拖人上投手丘且已有 DH → 仅守备（不进打线）
    if (target === 'P' && !inRows && hasDh.value) {
      bench.value = bench.value.filter((p) => String(p.id) !== String(player.id));
      fieldingPitcherId.value = player.id;
      normalize();
      return;
    }
    if (inRows) {
      const cur = rows.value[i];
      if (posOf(cur) === target) return;
      const occ = slotRow(rows.value, target);
      const next = [...rows.value];
      if (!occ) {
        next[i] = { ...cur, position: target };
      } else {
        // 两行整体对调：球员互换、各自带走的守备位 = 对方原来的守备位（棒次随之互换）
        const j = findRowIndex(rows.value, occ.id);
        const a = next[i];
        const b = next[j];
        next[i] = { ...b, position: posOf(a) };
        next[j] = { ...a, position: posOf(b) };
      }
      rows.value = next;
    } else {
      placeFromOutside(player, target);
    }
    normalize();
    syncPitcher();
  }

  /** 打线列表独立拖人：位置码取该人守备位（仅守备投手 → 'P'），否则 'DH' */
  function addToBatting(player) {
    if (player == null || findRowIndex(rows.value, player.id) >= 0) return;
    const filled = rows.value.filter((r) => !isPlaceholder(r));
    if (filled.length >= maxCount.value) {
      notice.value = `先发打线最多 ${maxCount.value} 人（现口径）；更多打击人数待联赛开关开放（见 Issue #274/#275）`;
      return;
    }
    const isFieldingPitcher =
      fieldingPitcherId.value != null &&
      String(fieldingPitcherId.value) === String(player.id) &&
      !slotRow(rows.value, 'P');
    if (!isFieldingPitcher && slotRow(rows.value, 'DH')) {
      notice.value = `DH 已有人；棒球现口径最多 ${maxCount.value} 人打击（含 1 名 DH）`;
      return;
    }
    takeFirstEmpty(makeRow(player, teamId.value, isFieldingPitcher ? 'P' : 'DH'));
    bench.value = bench.value.filter((p) => String(p.id) !== String(player.id));
    if (isFieldingPitcher) fieldingPitcherId.value = player.id;
    else lastDhAddedFromPoolId.value = player.id;
    normalize();
  }

  /** 移出一律回替补区 */
  function sendBackToBench(playerId) {
    const idx = findRowIndex(rows.value, playerId);
    const fromRoster = roster.value.find((x) => String(x.id) === String(playerId));
    if (idx < 0) {
      if (fieldingPitcherId.value != null && String(fieldingPitcherId.value) === String(playerId)) {
        fieldingPitcherId.value = null;
        if (fromRoster && !hasPlayer(bench.value, fromRoster.id)) bench.value = [...bench.value, fromRoster];
      }
      return;
    }
    const row = rows.value[idx];
    const player = fromRoster ?? { ...row, position: '' };
    const next = [...rows.value];
    next[idx] = makePlaceholderRow(teamId.value, idx);
    rows.value = next;
    if (!hasPlayer(bench.value, player.id)) {
      bench.value = [...bench.value, { ...player, teamId: teamId.value, position: '', stats: player.stats ?? emptyStats() }];
    }
    if (fieldingPitcherId.value != null && String(fieldingPitcherId.value) === String(player.id)) fieldingPitcherId.value = null;
    if (lastDhAddedFromPoolId.value != null && String(lastDhAddedFromPoolId.value) === String(player.id)) lastDhAddedFromPoolId.value = null;
    normalize();
  }

  function reorderBatting(from, to) {
    const filled = rows.value.filter((r) => !isPlaceholder(r));
    const empties = rows.value.filter((r) => isPlaceholder(r));
    if (from < 0 || to < 0 || from >= filled.length || to >= filled.length || from === to) return;
    const [moved] = filled.splice(from, 1);
    filled.splice(to, 0, moved);
    rows.value = [...filled, ...empties];
  }

  /** 移除 DH（决策 16：仅守备投手自动回打线） */
  function removeDh() {
    const dh = slotRow(rows.value, 'DH');
    if (!dh) return;
    detachPlayer(dh.id);
    normalize();
    const pitcher = fieldingPitcherId.value;
    if (!slotRow(rows.value, 'P') && pitcher != null) {
      const p = roster.value.find((x) => String(x.id) === String(pitcher));
      if (p) {
        takeFirstEmpty(makeRow(p, teamId.value, 'P'));
        bench.value = bench.value.filter((x) => String(x.id) !== String(p.id));
        fieldingPitcherId.value = p.id;
        normalize();
      }
    }
  }

  function setFieldingPitcher(playerId) {
    if (playerId == null) {
      fieldingPitcherId.value = null;
      return;
    }
    detachPlayer(playerId);
    fieldingPitcherId.value = playerId;
    normalize();
  }

  /** 批量设置替补（复用旧多选下拉的 id 列表） */
  function setBench(playerIds) {
    const ids = new Set((playerIds ?? []).map(String));
    bench.value = roster.value.filter((p) => ids.has(String(p.id)) && findRowIndex(rows.value, p.id) < 0);
  }

  /** 应用阵容模板（映射沿用旧件 sl()） */
  function applyTemplate({ slots, benchPlayerIds, pitcherId }) {
    const list = (Array.isArray(slots) ? slots : []).slice(0, maxCount.value).map((slot) => {
      const p = roster.value.find((x) => String(x.id) === String(slot?.playerId));
      if (!p) return makePlaceholderRow(teamId.value, 0);
      return makeRow(p, teamId.value, String(slot?.position ?? '').toUpperCase());
    });
    rows.value = list;
    normalize();
    const ids = new Set((benchPlayerIds ?? []).map(String));
    bench.value = roster.value.filter((p) => ids.has(String(p.id)) && findRowIndex(rows.value, p.id) < 0);
    const pitcher = slotRow(rows.value, 'P');
    fieldingPitcherId.value = pitcher
      ? pitcher.id
      : (pitcherId != null && roster.value.some((p) => String(p.id) === String(pitcherId)) ? pitcherId : null);
    lastDhAddedFromPoolId.value = null;
    syncPitcher();
  }

  return {
    rows, bench, roster, pool, fieldingPitcherId, lastDhAddedFromPoolId, notice,
    baseCount, maxCount, hasDh, hasP,
    validation, fieldingPitcherRequired, fieldingPitcherOk, lineupComplete, battingFull,
    slotOfRow, setRoster, loadDraft, reset,
    placeAtSlot, addToBatting, sendBackToBench, reorderBatting,
    removeDh, setFieldingPitcher, setBench, applyTemplate, consumeNotice,
  };
}
