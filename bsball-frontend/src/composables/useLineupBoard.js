// 阵容状态与动作（纯状态规则，不 import 任何拖拽库；node 可直接单测）
// 行数组下标 = 棒次；空行（id<0）恒排尾部；行数 ∈ [baseCount, absMax]
// 上限口径（设计稿 §4.3）：maxCount = 开关开启 ? absMax : baseCount；新增/落位门与 battingFull 用
//   maxCount（关闭态 fail-closed）；normalize/loadDraft/applyTemplate 用 absMax（读取保真，绝不裁剪已存数据）
// 语义要点（设计稿 §五）：投手丘单卡槽；投手是否打击由打线列表表达；
//   放 DH → 投手自动移出打线（转仅守备）；移除 DH → 投手自动回打线
import { computed, ref } from 'vue';
// 注意：本文件要能被 node --test 直接加载 → 相对导入必须带 .js 扩展名
import { validateRosterPositions } from '../utils/starterFieldingValidation.js';
import {
  applyKeSpecial, emptyStats, findRowIndex, hasPlayer, isPlaceholder, makePlaceholderRow,
  makeRow, normalizeRows, posOf, slotRow,
} from '../utils/lineupBoardRules.js';
import { sortByJersey } from '../utils/sortByJersey.js';

export function useLineupBoard(mode, teamId, opts = {}) {
  const rows = ref([]);
  const bench = ref([]);
  const roster = ref([]);
  const fieldingPitcherId = ref(null);
  const lastDhAddedFromPoolId = ref(null);
  const notice = ref('');
  const translate = typeof opts.text === 'function' ? opts.text : null;
  function setNotice(key, fallback, params) {
    notice.value = translate ? translate(key, params) : fallback;
  }

  const baseCount = computed(() => (mode.value === 'SOFTBALL' ? 10 : 9));
  // 开关缺省/未传 = 关闭（fail-closed）；opts.extraBatter 为 Ref/computed<boolean>
  const extraBatterOn = computed(() => !!opts.extraBatter?.value);
  // 大谷规则（DH 兼先发投手，Issue #525）；缺省/未传 = 关闭
  const ohtaniRuleOn = computed(() => !!opts.ohtaniRule?.value);
  // 绝对上限：仅用于读取保真；新增受开关上限 maxCount 约束
  const absMax = computed(() => (mode.value === 'SOFTBALL' ? 11 : 12));
  const maxCount = computed(() => (extraBatterOn.value ? absMax.value : baseCount.value));
  const hasDh = computed(() => !!slotRow(rows.value, 'DH'));
  const hasP = computed(() => !!slotRow(rows.value, 'P'));
  const hasDhPitcher = computed(() => {
    const dh = slotRow(rows.value, 'DH');
    return !!dh && isFieldingPitcherId(dh.id);
  });

  const pool = computed(() =>
    sortByJersey(roster.value.filter((p) => !hasPlayer(rows.value, p.id) && !hasPlayer(bench.value, p.id)))
  );
  const validation = computed(() =>
    // 注意：validateRosterPositions 读的是行对象的 .position 字段 → 必须传行对象，不能传位置字符串数组
    applyKeSpecial(validateRosterPositions(rows.value, mode.value, { extraBatter: extraBatterOn.value }), rows.value, baseCount.value)
  );
  // 「仍需指定仅守备投手」= 校验通过且**打线无 P**且尚未指定投手（覆盖 8+DH+EP 等 >baseCount 的无 P 阵）
  const fieldingPitcherRequired = computed(
    () => validation.value.ok && !hasP.value && fieldingPitcherId.value == null
  );
  // required 已蕴含「投手未指定」→ 「已就绪」即「不再需要指定」
  const fieldingPitcherOk = computed(() => !fieldingPitcherRequired.value);
  const lineupComplete = computed(() => validation.value.ok && fieldingPitcherOk.value);
  const battingFull = computed(() => rows.value.filter((r) => !isPlaceholder(r)).length >= maxCount.value);

  function normalize() {
    rows.value = normalizeRows(rows.value, baseCount.value, absMax.value, teamId.value);
  }

  function consumeNotice() {
    const n = notice.value;
    notice.value = '';
    return n;
  }

  function sortBench() {
    bench.value = sortByJersey(bench.value);
  }

  function slotOfRow(code) {
    return slotRow(rows.value, code);
  }

  /** 该球员是否已登记为守备投手（大谷规则下可与 DH 并存） */
  function isFieldingPitcherId(playerId) {
    return playerId != null && fieldingPitcherId.value != null && String(fieldingPitcherId.value) === String(playerId);
  }

  /** rows 含 P 行时同步守备投手；若「仅守备投手」已被放到其他守备位则清除该身份（避免同一球员双槽渲染） */
  function syncPitcher() {
    const p = slotRow(rows.value, 'P');
    if (p) fieldingPitcherId.value = p.id;
    else if (fieldingPitcherId.value != null) {
      const idx = findRowIndex(rows.value, fieldingPitcherId.value);
      // 大谷规则：DH 可以同时是先发投手（保留 DH 打席），其余打线位置不可兼投
      if (idx >= 0 && !(ohtaniRuleOn.value && posOf(rows.value[idx]) === 'DH')) fieldingPitcherId.value = null;
    }
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
    // 读取保真：快照按 absMax 恢复，绝不裁剪已存数据（超编由 validation 显式报错直至调整）
    const src = (Array.isArray(snapshot?.rows) ? snapshot.rows : []).slice(0, absMax.value);
    rows.value = src.map((row) =>
      isPlaceholder(row) ? makePlaceholderRow(teamId.value, 0) : makeRow(row, row.teamId ?? teamId.value, posOf(row))
    );
    normalize();
    bench.value = Array.isArray(snapshot?.bench) ? [...snapshot.bench] : [];
    sortBench();
    fieldingPitcherId.value = snapshot?.fieldingPitcherId ?? null;
    lastDhAddedFromPoolId.value = snapshot?.lastDhAddedFromPoolId ?? null;
    syncPitcher();
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
    sortBench();
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
    if (displaced && isFieldingPitcherId(displaced.id)) fieldingPitcherId.value = null;
    if (displaced) {
      if (fromBench) {
        bench.value = [...bench.value, { ...displaced, teamId: teamId.value, position: '', stats: displaced.stats ?? emptyStats() }];
      }
      if (lastDhAddedFromPoolId.value != null && String(displaced.id) === String(lastDhAddedFromPoolId.value)) {
        lastDhAddedFromPoolId.value = null;
      }
    }
    bench.value = bench.value.filter((p) => String(p.id) !== String(player.id));
    sortBench();
    lastDhAddedFromPoolId.value = player.id;
    // 决策 6b：放 DH → 投手移出打线（转仅守备）
    const pRow = slotRow(rows.value, 'P');
    if (pRow && String(pRow.id) !== String(player.id)) {
      fieldingPitcherId.value = pRow.id;
      const idx = findRowIndex(rows.value, pRow.id);
      const n2 = [...rows.value];
      n2[idx] = makePlaceholderRow(teamId.value, idx);
      rows.value = n2;
      setNotice('liveGame.lineup.notice.dhPitcherOut', '已按 DH 制把投手移出打线；如需 10 人打击，请把投手拖回打线');
    }
  }

  /** 拖放到球场卡槽（唯一入口） */
  function placeAtSlot(player, code) {
    const target = String(code ?? '').toUpperCase();
    if (!target || player == null) return;
    const i = findRowIndex(rows.value, player.id);
    const inRows = i >= 0;

    // 大谷规则：仅守备投手拖到 DH 不自动兼投，请用按钮
    if (target === 'DH' && ohtaniRuleOn.value && isFieldingPitcherId(player.id)) {
      const dh = slotRow(rows.value, 'DH');
      if (!dh || String(dh.id) !== String(player.id)) {
        setNotice('liveGame.lineup.notice.ohtaniUseButtonDh', '大谷规则下请用「设为兼投」按钮设置 DH 兼投');
        return;
      }
    }

    if (target === 'DH') {
      const dh = slotRow(rows.value, 'DH');
      const dhOccupiedByOther = dh != null && String(dh.id) !== String(player.id);
      // 已在打线 + DH 槽已被他人占用 → 落入下方通用「占用槽对调」分支（决策 #5 修订：棒次不动）；
      // 其余情形（空 DH 槽 / 池 / 替补）走 placeDh（含决策 6b「放 DH → 投手移出打线」联动）
      if (!inRows || !dhOccupiedByOther) {
        placeDh(player);
        normalize();
        syncPitcher();
        return;
      }
    }
    // 大谷规则：DH 拖到 P 不执行兼投，避免误变成打击投手并失去 DH
    if (target === 'P' && inRows && ohtaniRuleOn.value && posOf(rows.value[i]) === 'DH' && !hasP.value) {
      setNotice('liveGame.lineup.notice.ohtaniUseButtonP', '大谷规则下请用「设为兼投」按钮；直接拖到投手位会变成打击投手并失去 DH');
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
      // 大谷规则：兼投球员离开 DH → 自动取消兼投（先发投手身份清除）
      if (ohtaniRuleOn.value && posOf(cur) === 'DH' && isFieldingPitcherId(player.id)) {
        fieldingPitcherId.value = null;
        occ
          ? setNotice('liveGame.lineup.notice.twoWayCancelled', '已取消兼投')
          : setNotice('liveGame.lineup.notice.twoWayCancelledNeedDh', '已取消兼投；DH 槽需补人');
      }
      const next = [...rows.value];
      if (!occ) {
        next[i] = { ...cur, position: target };
      } else {
        // 只换守备位：两行各自的棒次（行下标）保持不动（负责人口径：场上换位不连打序一起换）
        const j = findRowIndex(rows.value, occ.id);
        next[i] = { ...cur, position: target };
        next[j] = { ...occ, position: posOf(cur) };
      }
      rows.value = next;
    } else {
      placeFromOutside(player, target);
    }
    normalize();
    syncPitcher();
  }

  /** 打线列表独立拖人：位置码 = 仅守备投手 → 'P'；无 DH → 'DH'；已有 DH → 棒球 'EP'（EP 制落位），
   *  垒球无 EP → 维持「DH 已有人」拒绝（垒球现口径最多 N 人打击，含 1 名 DH） */
  function addToBatting(player) {
    if (player == null || findRowIndex(rows.value, player.id) >= 0) return;
    const filled = rows.value.filter((r) => !isPlaceholder(r));
    if (filled.length >= maxCount.value) {
      extraBatterOn.value
        ? setNotice('liveGame.lineup.notice.maxBattingExtra', `先发打线最多 ${maxCount.value} 人`, { max: maxCount.value })
        : setNotice('liveGame.lineup.notice.epNotOpen', `该联赛未开放 EP 制（额外打击者），请联系管理员（上限 ${maxCount.value} 人）`, { max: maxCount.value });
      return;
    }
    const isFieldingPitcher =
      fieldingPitcherId.value != null &&
      String(fieldingPitcherId.value) === String(player.id) &&
      !slotRow(rows.value, 'P');
    // 垒球无 EP 制：已有 DH 时非投手不再新增（避免落出非法守备位 EP）
    if (!isFieldingPitcher && slotRow(rows.value, 'DH') && mode.value === 'SOFTBALL') {
      setNotice('liveGame.lineup.notice.dhOccupiedSoftball', `DH 已有人；垒球现口径最多 ${maxCount.value} 人打击（含 1 名 DH）`, { max: maxCount.value });
      return;
    }
    const position = isFieldingPitcher ? 'P' : (slotRow(rows.value, 'DH') ? 'EP' : 'DH');
    takeFirstEmpty(makeRow(player, teamId.value, position));
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
        sortBench();
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
    sortBench();
    if (fieldingPitcherId.value != null && String(fieldingPitcherId.value) === String(player.id)) fieldingPitcherId.value = null;
    if (lastDhAddedFromPoolId.value != null && String(lastDhAddedFromPoolId.value) === String(player.id)) lastDhAddedFromPoolId.value = null;
    normalize();
  }

  /** 放回替补区（统一入口）：DH 走 removeDh（含决策 16「仅守备投手自动回打线」联动）；
   *  打线内 / 仅守备投手 → sendBackToBench；纯池球员 → 直接入替补名单 */
  function sendToBench(playerId) {
    const dh = slotRow(rows.value, 'DH');
    if (dh && String(dh.id) === String(playerId)) return removeDh();
    const inRows = findRowIndex(rows.value, playerId) >= 0;
    const isFieldingPitcher =
      fieldingPitcherId.value != null && String(fieldingPitcherId.value) === String(playerId);
    if (inRows || isFieldingPitcher) return sendBackToBench(playerId);
    if (!hasPlayer(bench.value, playerId)) {
      setBench([...bench.value.map((p) => p.id), playerId]);
    }
  }

  /** 直接移出阵容回到可选池（不进入替补席）；DH 被移出时沿用决策 16：仅守备投手自动回打线 */
  function sendToPool(playerId) {
    const wasDh = String(slotRow(rows.value, 'DH')?.id ?? '') === String(playerId);
    detachPlayer(playerId);
    normalize();
    if (!wasDh) return;
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

  function reorderBatting(from, to) {
    const filled = rows.value.filter((r) => !isPlaceholder(r));
    const empties = rows.value.filter((r) => isPlaceholder(r));
    if (from < 0 || to < 0 || from >= filled.length || to >= filled.length || from === to) return;
    const [moved] = filled.splice(from, 1);
    filled.splice(to, 0, moved);
    rows.value = [...filled, ...empties];
  }

  /** 移除 DH（决策 16：仅守备投手自动回打线；决策 10：DH 球员回替补区） */
  function removeDh() {
    const dh = slotRow(rows.value, 'DH');
    if (!dh) return;
    const pitcherBefore = fieldingPitcherId.value;
    const dhWasPitcher = isFieldingPitcherId(dh.id);
    const dhPlayer = roster.value.find((x) => String(x.id) === String(dh.id)) ?? { ...dh, position: '' };
    detachPlayer(dh.id);
    if (!hasPlayer(bench.value, dhPlayer.id)) {
      bench.value = [...bench.value, { ...dhPlayer, teamId: teamId.value, position: '', stats: dhPlayer.stats ?? emptyStats() }];
    }
    sortBench();
    normalize();
    // 大谷规则：移除 DH 后，DH兼投球员回到 P 打席（等同决策 16 的投手回打线）
    const pitcher = dhWasPitcher ? pitcherBefore : fieldingPitcherId.value;
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
    const idx = findRowIndex(rows.value, playerId);
    // 大谷规则：DH 兼投只登记投手身份，不移出打线
    if (idx >= 0 && ohtaniRuleOn.value && posOf(rows.value[idx]) === 'DH' && !hasP.value) {
      fieldingPitcherId.value = playerId;
      return;
    }
    detachPlayer(playerId);
    fieldingPitcherId.value = playerId;
    normalize();
  }

  /** 大谷规则：点击「设为兼投」/「取消兼投」 */
  function toggleDhPitcher() {
    if (!ohtaniRuleOn.value) {
      setNotice('liveGame.lineup.notice.ohtaniOff', '该赛事未开启大谷规则');
      return false;
    }
    const dh = slotRow(rows.value, 'DH');
    if (!dh) {
      setNotice('liveGame.lineup.notice.putDhFirst', '请先在 DH 槽放入球员');
      return false;
    }
    if (hasP.value) {
      setNotice('liveGame.lineup.notice.lineupHasPitcher', '打线已有投手位，无法设置 DH 兼投');
      return false;
    }
    if (isFieldingPitcherId(dh.id)) {
      fieldingPitcherId.value = null;
      setNotice('liveGame.lineup.notice.twoWayCancelledKeepDh', '已取消兼投；DH 保留');
      return true;
    }
    fieldingPitcherId.value = dh.id;
    setNotice('liveGame.lineup.notice.twoWayRegistered', '已按大谷规则登记：该球员同时为 DH 与先发投手');
    return true;
  }

  /** 批量设置替补（复用旧多选下拉的 id 列表） */
  function setBench(playerIds) {
    const ids = new Set((playerIds ?? []).map(String));
    bench.value = roster.value.filter((p) => ids.has(String(p.id)) && findRowIndex(rows.value, p.id) < 0);
    sortBench();
  }

  /** 应用阵容模板（映射沿用旧件 sl()；读取保真按 absMax，不裁剪模板行数） */
  function applyTemplate({ slots, benchPlayerIds, pitcherId }) {
    const list = (Array.isArray(slots) ? slots : []).slice(0, absMax.value).map((slot) => {
      const p = roster.value.find((x) => String(x.id) === String(slot?.playerId));
      if (!p) return makePlaceholderRow(teamId.value, 0);
      return makeRow(p, teamId.value, String(slot?.position ?? '').toUpperCase());
    });
    rows.value = list;
    normalize();
    const ids = new Set((benchPlayerIds ?? []).map(String));
    bench.value = roster.value.filter((p) => ids.has(String(p.id)) && findRowIndex(rows.value, p.id) < 0);
    sortBench();
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
    placeAtSlot, addToBatting, sendBackToBench, sendToBench, sendToPool, reorderBatting,
    removeDh, setFieldingPitcher, hasDhPitcher, toggleDhPitcher, setBench, applyTemplate, consumeNotice,
  };
}
