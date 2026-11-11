<template>
  <div v-loading="loading" class="admin-page team-game-lineup-page">
    <div class="lineup-board-header">
      <span class="header-left">赛前阵容 · {{ context?.teamName || '本队' }}</span>
      <div class="header-actions">
        <el-button @click="goBack">返回比赛列表</el-button>
        <el-button v-if="hasPerm('business:lineup-template:manage')" @click="openTemplates">应用阵容模板</el-button>
        <el-button type="primary" :disabled="saving" :loading="saving" @click="save">保存阵容</el-button>
        <el-tooltip :disabled="!posterDisabledReason" :content="posterDisabledReason" placement="top">
          <span class="poster-btn-wrap">
            <el-button :disabled="!canGeneratePoster" :loading="posterGenerating" @click="generatePoster">
              生成海报
            </el-button>
          </span>
        </el-tooltip>
      </div>
    </div>

    <div v-if="context" class="game-info">
      <span class="game-info__event">{{ context.eventName }}</span>
      <span v-if="context.gameNumber != null">第 {{ context.gameNumber }} 场</span>
      <span>{{ opponentText }}</span>
      <span>{{ formatLineupDateTime(context.gameTime || context.gameday) }}</span>
      <span v-if="context.venue">{{ context.venue }}</span>
      <el-tag v-if="board.lineupComplete.value" size="small" type="success">阵容完整</el-tag>
      <el-tag v-else size="small" type="warning">未完成，可先保存草稿</el-tag>
      <el-tag v-if="isDirty" size="small" type="info">有未保存修改</el-tag>
    </div>

    <el-divider content-position="left">{{ rosterHint }}</el-divider>

    <div v-if="ohtaniRule" class="ohtani-rule-bar">
      <el-button size="small" type="primary" plain :disabled="ohtaniButtonDisabled" @click="onToggleOhtani">
        {{ ohtaniButtonLabel }}
      </el-button>
      <span class="ohtani-rule-bar__hint">{{ ohtaniHint }}</span>
    </div>

    <div v-if="pending" class="lineup-pending-hint">
      待放置：{{ pending.player.name }}｜点卡槽落位；再点卡片或空白处取消
    </div>

    <div class="lineup-board">
      <LineupFieldBoard
        ref="fieldBoard"
        :mode="mode"
        :slots="fieldSlotViews"
        :hover-code="hoverSlotCode"
        @slot-click="onSlotClick"
      />
      <LineupBattingOrder
        ref="battingRef"
        :rows="board.rows.value"
        :error-indices="errorIndices"
        :max-count="board.maxCount.value"
        :drop-hover="isBattingHover"
        :drop-line="battingDropLine"
        @reorder="({ from, to }) => board.reorderBatting(from, to)"
        @pick="(id) => onPick(findPlayer(id))"
      />
      <LineupPoolPanel
        ref="poolRef"
        :pool="board.pool.value"
        :bench="board.bench.value"
        :roster="board.roster.value"
        :pending-player-id="pending ? pending.player.id : null"
        :hover-kind="hoverPanelKind"
        :lineup-ids="board.rows.value.filter((r) => !isPlaceholder(r)).map((r) => r.id)"
        @pick="onPick"
        @batch-change="(ids) => board.setBench(ids)"
      />
    </div>

    <div v-if="lineupErrorMsg" class="lineup-warn">{{ lineupErrorMsg }}</div>

    <LineupPoster
      v-if="context"
      ref="posterRef"
      :context="context"
      :rows="board.rows.value"
      :roster="board.roster.value"
      :bench="board.bench.value"
      :fielding-pitcher-id="board.fieldingPitcherId.value"
    />

    <el-dialog v-model="posterDialogVisible" title="赛前阵容海报" width="520px" @closed="revokePoster">
      <div class="poster-preview-wrap">
        <img v-if="posterUrl" :src="posterUrl" class="poster-preview" alt="赛前阵容海报" />
      </div>
      <template #footer>
        <el-button @click="posterDialogVisible = false">关闭</el-button>
        <el-button :disabled="!posterBlob" @click="copyPoster">复制图片</el-button>
        <el-button type="primary" :disabled="!posterBlob" @click="savePoster">保存图片</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="templateDialog.visible" title="应用阵容模板" width="420px" :close-on-click-modal="false">
      <el-select v-model="templateDialog.selected" placeholder="选择阵容模板" filterable style="width: 100%">
        <el-option v-for="t in templateDialog.list" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <template #footer>
        <el-button @click="templateDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="templateDialog.selected == null" @click="applyTemplate">应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, vLoading } from 'element-plus';
import { fetchPlayersByTeam, fetchRosterPlayers, lineupTemplateApi, teamLineupApi } from '../../api/business';
import { filterOutOfRoster, filterTemplateByPool, pickPoolId } from '../../utils/rosterPool';
import { usePermission } from '../../composables/usePermission';
import { useMediaQuery } from '../../composables/useMediaQuery';
import { useLineupBoard } from '../../composables/useLineupBoard';
import { useLineupDrag } from '../../composables/useLineupDrag';
import { slotsForMode } from '../../utils/lineupFieldPositions';
import { isPlaceholder, findRowIndex, posOf } from '../../utils/lineupBoardRules';
import { visibleLineupError } from '../../utils/lineupValidationMessage';
import {
  buildTeamLineupDraft,
  formatLineupDateTime,
  hasTeamLineupData,
  parseTeamLineupDraft,
  toBoardDraft
} from '../../utils/teamLineupDraft';
import LineupFieldBoard from '../../components/admin/LineupFieldBoard.vue';
import LineupBattingOrder from '../../components/admin/LineupBattingOrder.vue';
import LineupPoolPanel from '../../components/admin/LineupPoolPanel.vue';
import LineupPoster from '../../components/admin/LineupPoster.vue';
import { downloadBlob } from '../../utils/gameDetailCollage';
import '../../styles/lineup-board.css';

void vLoading;

const route = useRoute();
const router = useRouter();
const { hasPerm } = usePermission();
const isFinePointer = useMediaQuery('(pointer: fine)');

const teamId = computed(() => String(route.params.id ?? ''));
const eventId = computed(() => String(route.params.eventId ?? ''));
const gameId = computed(() => Number(route.params.gameId));

const loading = ref(true);
const saving = ref(false);
const context = ref(null);
const mode = ref('BASEBALL');
const extraBatter = ref(false);
const ohtaniRule = ref(false);
const rosterPool = ref({ restricted: false, players: [] });
const templateDialog = ref({ visible: false, list: [], selected: null });
const pending = ref(null);
const dragHover = ref(null);
const dragSource = ref(null);
const savedSignature = ref(null);
const posterRef = ref(null);
const posterGenerating = ref(false);
const posterDialogVisible = ref(false);
const posterUrl = ref('');
const posterBlob = ref(null);

const board = useLineupBoard(mode, computed(() => context.value?.teamId ?? null), {
  extraBatter,
  ohtaniRule
});

const rosterHint = computed(() => {
  if (mode.value === 'SOFTBALL') {
    return extraBatter.value ? '阵容确认（垒球：10-11 人）' : '阵容确认（垒球：10 人）';
  }
  return extraBatter.value ? '阵容确认（棒球：9-12 人，含 EP 额外打击者）' : '阵容确认（棒球：9 人）';
});

const opponentText = computed(() => {
  if (!context.value) return '';
  const side = context.value.side === 'home' ? '主场' : '客场';
  return `${side} · ${context.value.opponentName ? `对阵 ${context.value.opponentName}` : '对手待定'}`;
});

const ohtaniAction = computed(() => {
  if (!ohtaniRule.value) return 'none';
  if (board.hasDhPitcher.value && !board.hasP.value) return 'cancel-dh-pitcher';
  if (board.hasP.value && !board.hasDh.value) return 'pitcher-to-dh';
  if (!board.hasDh.value && board.fieldingPitcherId.value != null) return 'pitcher-to-dh';
  if (board.hasDh.value && !board.hasP.value) return 'dh-to-pitcher';
  return 'none';
});
const ohtaniButtonLabel = computed(() => {
  if (ohtaniAction.value === 'cancel-dh-pitcher') return '取消兼投';
  if (ohtaniAction.value === 'pitcher-to-dh') return '兼DH';
  return '设为兼投';
});
const ohtaniButtonDisabled = computed(() => ohtaniAction.value === 'none');
const ohtaniHint = computed(() => {
  if (!ohtaniRule.value) return '';
  if (board.hasDhPitcher.value && !board.hasP.value) return '大谷规则：DH 可兼先发投手；换投后保留 DH，不可再移防。';
  if (board.hasP.value && board.hasDh.value) return '大谷规则：打线同时有投手位和 DH，请先调整';
  if (board.hasP.value) return '大谷规则：点「兼DH」把当前投手设为 DH 兼投，自动填充 DH 槽';
  if (!board.hasDh.value && board.fieldingPitcherId.value != null) return '大谷规则：点「兼DH」把当前投手设为 DH 兼投';
  if (board.hasDh.value) return '大谷规则：点「设为兼投」把 DH 设为先发投手';
  return '大谷规则：先把球员放到 DH 槽或投手丘，再点对应按钮';
});

function onToggleOhtani() {
  const action = ohtaniAction.value;
  if (action === 'cancel-dh-pitcher' || action === 'dh-to-pitcher') {
    board.toggleDhPitcher();
  } else if (action === 'pitcher-to-dh') {
    board.setPitcherAsDh();
  }
  showNotice(board);
}

const errorIndices = computed(() => board.validation.value.errorRowIndices ?? []);
const lineupErrorMsg = computed(() => visibleLineupError(board.validation.value.msg));

const fieldSlotViews = computed(() => {
  const rows = board.rows.value;
  const filledRows = rows.filter((r) => !isPlaceholder(r));
  const errIdx = board.validation.value.errorRowIndices ?? [];
  const pickedId = pending.value?.player ? String(pending.value.player.id) : null;
  return slotsForMode(mode.value).map((slot) => {
    const code = slot.code;
    const rowIndex = rows.findIndex((r) => !isPlaceholder(r) && posOf(r) === code);
    const row = rowIndex >= 0 ? rows[rowIndex] : null;
    let player = row;
    let badge = '';
    let orderRow = row;
    if (!row && code === 'P' && board.fieldingPitcherId.value != null) {
      const dhPitcherRow = rows.find(
        (r) => !isPlaceholder(r) && posOf(r) === 'DH' && String(r.id) === String(board.fieldingPitcherId.value)
      );
      player = board.roster.value.find((p) => String(p.id) === String(board.fieldingPitcherId.value)) ?? null;
      if (player) badge = dhPitcherRow ? 'DH兼投' : '仅守备';
      if (dhPitcherRow) orderRow = dhPitcherRow;
    }
    const orderIndex = orderRow ? filledRows.findIndex((r) => String(r.id) === String(orderRow.id)) + 1 : 0;
    const error = rowIndex >= 0 && errIdx.includes(rowIndex);
    const selfPending = !!player && String(player.id) === pickedId;
    return { code, player: player ?? null, orderIndex, badge, error, pending: pendingAccepts(code), selfPending };
  });
});

function pendingAccepts(code) {
  const p = pending.value?.player;
  if (!p) return false;
  const inRows = board.rows.value.some((r) => !isPlaceholder(r) && String(r.id) === String(p.id));
  const occ = board.rows.value.find((r) => !isPlaceholder(r) && posOf(r) === code) ?? null;
  if (code === 'DH') return !occ || String(occ.id) === String(p.id);
  if (occ) return true;
  if (inRows) return true;
  return !board.battingFull.value;
}

const fieldBoard = ref(null);
const battingRef = ref(null);
const poolRef = ref(null);
const hoverSlotCode = computed(() => (dragHover.value?.kind === 'slot' ? dragHover.value.code : ''));
const isBattingHover = computed(() => dragHover.value?.kind === 'batting');
const hoverPanelKind = computed(() =>
  dragHover.value?.kind === 'bench' || dragHover.value?.kind === 'pool' ? dragHover.value.kind : ''
);

const battingDropLine = computed(() => {
  if (!isFinePointer.value || !dragSource.value?.isRow) return null;
  const hover = dragHover.value;
  if (hover?.kind !== 'batting' || hover.index == null) return null;
  const playerId = dragSource.value.playerId;
  const from = findRowIndex(board.rows.value, playerId);
  if (from < 0) return null;
  const others = board.rows.value.filter((r) => !isPlaceholder(r) && String(r.id) !== String(playerId));
  const gap = hover.index;
  if (gap > others.length || gap === from) return null;
  return {
    beforeId: others[gap]?.id ?? null,
    afterId: gap === others.length ? others[others.length - 1]?.id ?? null : null
  };
});

const { bind, unbind, wasRecentDrag } = useLineupDrag({
  onDragStart(el) {
    dragSource.value = {
      isRow: el.classList.contains('lineup-batting__row'),
      playerId: el.dataset.playerId ?? null
    };
  },
  onDrop(el, target) {
    const playerId = el.dataset.playerId;
    const player = playerId != null ? findPlayer(playerId) : null;
    if (player) handleDrop(player, target);
    dragSource.value = null;
  },
  onCancel() {
    dragSource.value = null;
  },
  onHover(target) {
    if (pending.value) clearPending();
    dragHover.value = target;
  }
});

function findPlayer(playerId) {
  return (
    board.rows.value.find((r) => String(r.id) === String(playerId)) ??
    board.bench.value.find((p) => String(p.id) === String(playerId)) ??
    board.roster.value.find((p) => String(p.id) === String(playerId)) ??
    null
  );
}

function getLayout(draggedEl = null) {
  const battingEl = battingRef.value?.getListEl?.() ?? null;
  const benchEl = poolRef.value?.getBenchEl?.() ?? null;
  const poolEl = poolRef.value?.getPoolEl?.() ?? null;
  const toRect = (el) => {
    if (!el) return null;
    const r = el.getBoundingClientRect();
    return { left: r.left, top: r.top, width: r.width, height: r.height };
  };
  const battingRowRects = battingEl
    ? [...battingEl.querySelectorAll('.lineup-batting__row')]
        .filter((rowEl) => rowEl !== draggedEl)
        .map((rowEl) => {
          const r = rowEl.getBoundingClientRect();
          return { top: r.top, height: r.height };
        })
    : null;
  return {
    slotRects: fieldBoard.value?.getSlotRects?.() ?? [],
    battingRect: toRect(battingEl),
    battingRowRects,
    benchRect: toRect(benchEl),
    poolRect: toRect(poolEl)
  };
}

function handleDrop(player, target) {
  if (target.kind === 'batting' && isFinePointer.value && dragSource.value?.isRow && target.index != null) {
    const from = findRowIndex(board.rows.value, player.id);
    if (from >= 0) {
      board.reorderBatting(from, target.index);
      showNotice(board);
      return;
    }
  }
  if (target.kind === 'slot') board.placeAtSlot(player, target.code);
  else if (target.kind === 'batting') board.addToBatting(player);
  else if (target.kind === 'bench') board.sendToBench(player.id);
  else if (target.kind === 'pool') {
    const inBench = board.bench.value.some((p) => String(p.id) === String(player.id));
    const inRows = findRowIndex(board.rows.value, player.id) >= 0;
    const fieldingPitcher =
      board.fieldingPitcherId.value != null && String(board.fieldingPitcherId.value) === String(player.id);
    if (inBench) {
      board.setBench(board.bench.value.filter((p) => String(p.id) !== String(player.id)).map((p) => p.id));
    } else if (inRows || fieldingPitcher) {
      board.sendToPool(player.id);
    }
  } else if (target.kind === 'outside') {
    const inLineup =
      findRowIndex(board.rows.value, player.id) >= 0 ||
      (board.fieldingPitcherId.value != null && String(board.fieldingPitcherId.value) === String(player.id));
    if (inLineup) board.sendToBench(player.id);
  }
  showNotice(board);
}

function onPick(player) {
  if (wasRecentDrag()) return;
  if (pending.value && String(pending.value.player.id) === String(player.id)) return clearPending();
  pending.value = { player };
}

function onDocClick() {
  if (pending.value) clearPending();
}

function onSlotClick(code) {
  if (pending.value) {
    board.placeAtSlot(pending.value.player, code);
    showNotice(board);
    clearPending();
    return;
  }
  const occ = fieldSlotViews.value.find((s) => s.code === code)?.player ?? null;
  if (occ) onPick(occ);
}

function clearPending() {
  pending.value = null;
}

function showNotice(b) {
  const msg = b.consumeNotice();
  if (msg) ElMessage.warning(msg);
}

function buildPayload() {
  return buildTeamLineupDraft({
    gameId: Number(gameId.value),
    teamId: context.value?.teamId,
    gameMode: mode.value,
    complete: board.lineupComplete.value,
    lineup: board.rows.value,
    bench: board.bench.value,
    fieldingPitcherId: board.fieldingPitcherId.value,
    lastDhAddedFromPoolId: board.lastDhAddedFromPoolId.value
  });
}

const payloadSignature = computed(() => JSON.stringify(buildPayload()));
const isDirty = computed(() => savedSignature.value != null && payloadSignature.value !== savedSignature.value);

const posterDisabledReason = computed(() => {
  if (posterGenerating.value) return '正在生成海报';
  if (savedSignature.value == null) return '请先保存阵容';
  if (!board.lineupComplete.value) return '阵容完整后才能生成海报';
  if (isDirty.value) return '请先保存最新修改';
  return '';
});
const canGeneratePoster = computed(() => !posterDisabledReason.value);

async function generatePoster() {
  if (!canGeneratePoster.value || !posterRef.value) return;
  posterGenerating.value = true;
  try {
    const { url, blob } = await posterRef.value.generate();
    // 生成期间离开页面：组件已卸载，丢弃结果避免遗留 blob URL
    if (!posterRef.value) {
      URL.revokeObjectURL(url);
      return;
    }
    revokePoster();
    posterUrl.value = url;
    posterBlob.value = blob;
    posterDialogVisible.value = true;
  } catch (err) {
    ElMessage.error(err?.message ?? '海报生成失败');
  } finally {
    posterGenerating.value = false;
  }
}

function revokePoster() {
  if (posterUrl.value) URL.revokeObjectURL(posterUrl.value);
  posterUrl.value = '';
  posterBlob.value = null;
}

function savePoster() {
  if (!posterBlob.value) return;
  downloadBlob(posterBlob.value, `赛前阵容-${context.value?.teamName ?? '球队'}-${context.value?.gameId ?? ''}.png`);
}

async function copyPoster() {
  if (!posterBlob.value) return;
  try {
    if (navigator.clipboard?.write && window.ClipboardItem) {
      await navigator.clipboard.write([new ClipboardItem({ 'image/png': posterBlob.value })]);
      ElMessage.success('海报已复制');
      return;
    }
    ElMessage.warning('当前浏览器不支持复制图片，请使用保存图片');
  } catch {
    ElMessage.warning('复制失败，请使用保存图片');
  }
}

async function save() {
  if (saving.value || !context.value) return;
  saving.value = true;
  try {
    const payload = buildPayload();
    await teamLineupApi.save(context.value.teamId, context.value.gameId, {
      lineupJson: JSON.stringify(payload),
      complete: payload.complete
    });
    savedSignature.value = JSON.stringify(payload);
    ElMessage.success('赛前阵容已保存');
  } catch (err) {
    ElMessage.error(err?.message ?? '保存赛前阵容失败');
  } finally {
    saving.value = false;
  }
}

async function openTemplates() {
  if (!context.value?.teamId) return;
  templateDialog.value.selected = null;
  try {
    const list = (await lineupTemplateApi.list(context.value.teamId)) ?? [];
    templateDialog.value.list = list;
    if (!list.length) {
      ElMessage.info('该球队暂无阵容模板，请先在「阵容模板」菜单中创建');
      return;
    }
    templateDialog.value.visible = true;
  } catch (err) {
    ElMessage.error(err?.message ?? '加载模板列表失败');
  }
}

async function applyTemplate() {
  const id = templateDialog.value.selected;
  if (!context.value?.teamId || id == null) return;
  try {
    const t = await lineupTemplateApi.get(context.value.teamId, id);
    if (!t?.slots?.length) {
      ElMessage.warning('模板数据为空');
      return;
    }
    await ElMessageBox.confirm('将用该模板覆盖当前先发阵容与替补名单，是否继续？', '应用阵容模板');
    const filtered = filterTemplateByPool(t, rosterPool.value);
    if (filtered.removedCount > 0) {
      ElMessage.warning(`模板中有 ${filtered.removedCount} 名球员不在本赛事报名名单中，已留空或移除，请重新选择`);
    }
    board.applyTemplate({
      slots: filtered.slots,
      benchPlayerIds: filtered.benchPlayerIds,
      pitcherId: filtered.pitcherId
    });
    templateDialog.value.visible = false;
  } catch (err) {
    if (err === 'cancel' || err === 'close') return;
    ElMessage.error(err?.message ?? '加载模板失败');
  }
}

function goBack() {
  router.push({ name: 'AdminTeamLineupGames', params: { id: teamId.value, eventId: eventId.value } });
}

async function load() {
  loading.value = true;
  try {
    const data = await teamLineupApi.getContext(teamId.value, gameId.value);
    if (data == null) {
      ElMessage.error('比赛不存在或无权编辑赛前阵容');
      goBack();
      return;
    }
    context.value = data;
    mode.value = data.gameMode || 'BASEBALL';
    extraBatter.value = data.specialRules?.extraBatter === true;
    ohtaniRule.value = data.specialRules?.ohtaniRule === true;

    let pool = await fetchRosterPlayers(eventId.value, data.teamId);
    if (pool.failed) {
      ElMessage.warning('未能加载赛事报名名单，已回退本队全名册（保存时仍将校验）');
      pool = { restricted: false, players: await fetchPlayersByTeam(data.teamId) };
    }
    rosterPool.value = pool;
    board.setRoster(pool.players);
    if (pool.restricted === true && !(pool.players ?? []).length) {
      ElMessage.warning('本队尚未在本赛事提交报名名单，暂无可选球员');
    }

    const draft = parseTeamLineupDraft(data.lineupJson);
    if (draft && hasTeamLineupData(draft)) {
      const boardDraft = toBoardDraft(draft);
      const filtered = filterOutOfRoster(boardDraft.rows, boardDraft.bench, pool);
      const removedNames = (filtered.removed ?? []).map((x) => x.name || x.id).filter((x) => x != null);
      if (removedNames.length) {
        ElMessage.warning(`已从阵容中移除未报名球员：${removedNames.join('、')}`);
      }
      board.loadDraft({
        rows: filtered.keptRows,
        bench: filtered.keptBench,
        fieldingPitcherId: pickPoolId(boardDraft.fieldingPitcherId, pool),
        lastDhAddedFromPoolId: pickPoolId(boardDraft.lastDhAddedFromPoolId, pool)
      });
    }
    savedSignature.value = JSON.stringify(buildPayload());
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  try {
    await load();
  } catch (err) {
    ElMessage.error(err?.message ?? '加载赛前阵容失败');
  }
  await nextTick();
  bind(['.lineup-card', '.lineup-batting__row'], getLayout);
  document.addEventListener('click', onDocClick);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick);
  unbind();
  revokePoster();
});
</script>

<style scoped>
.team-game-lineup-page {
  padding: 12px;
}

.game-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  color: #606266;
  font-size: 13px;
}

.game-info__event {
  color: #303133;
  font-weight: 600;
}

.ohtani-rule-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: 10px 0 4px;
}

.ohtani-rule-bar__hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.45;
}

.poster-btn-wrap {
  display: inline-flex;
}

.poster-preview-wrap {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0b1b2b;
  border-radius: 10px;
  overflow: hidden;
}

.poster-preview {
  display: block;
  width: min(100%, 420px);
  height: auto;
}
</style>
