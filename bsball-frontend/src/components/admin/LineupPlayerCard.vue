<template>
  <div
    class="lineup-card"
    :class="[`lineup-card--${variant}`, `is-${state}`, { 'lineup-card--empty': !player }]"
    :data-player-id="player?.id"
    data-drag-handle
    @click.stop="onClick"
  >
    <template v-if="player">
      <!-- 球场卡槽：头像居中、背号在左上角、名字在头像正下方（用户反馈） -->
      <template v-if="variant === 'field'">
        <span class="lineup-card__numcorner">#{{ player.number ?? '-' }}</span>
        <span v-if="orderIndex" class="lineup-card__order">{{ orderIndex }}</span>
        <el-avatar class="lineup-card__avatar" :size="22" shape="square" :src="avatarUrl">{{ nameInitial }}</el-avatar>
        <span class="lineup-card__name">{{ player.name }}</span>
        <span v-if="badge" class="lineup-card__badge">{{ badge }}</span>
      </template>
      <template v-else>
        <el-avatar class="lineup-card__avatar" :size="22" shape="square" :src="avatarUrl">{{ nameInitial }}</el-avatar>
        <span class="lineup-card__number">#{{ player.number ?? '-' }}</span>
        <span class="lineup-card__name">{{ player.name }}</span>
        <span v-if="badge" class="lineup-card__badge">{{ badge }}</span>
      </template>
    </template>
    <template v-else>
      <span class="lineup-card__placeholder">{{ placeholderText }}</span>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { resolveAssetUrl } from '../../api/request';

// 球员卡：池 / 替补 / 球场卡槽 共用。
// 根元素带 data-drag-handle（interactjs 的默认拖拽区）；不要放 button 之类可聚焦元素，避免与长按拖拽抢手势。
const props = defineProps({
  player: { type: Object, default: null },
  orderIndex: { type: Number, default: 0 },
  badge: { type: String, default: '' },
  variant: { type: String, default: 'pool' },
  state: { type: String, default: 'idle' },
  placeholderText: { type: String, default: '' },
});
const emit = defineEmits(['pick', 'open']);

// 头像：沿用全站约定 resolveAssetUrl（内部已对空值兜底）；无头像时 el-avatar 退回插槽内容（姓名首字）
const avatarUrl = computed(() => resolveAssetUrl(props.player?.avatar));
const nameInitial = computed(() => (props.player?.name || '?').slice(0, 1));

// 单击消歧（设计稿 §四.3/§四.4）：池/替补卡片 → 进入待放置态（点选点放）；
// 已落位卡片（球场）→ 打开操作面板（面板内含「移到其他位置」，覆盖触屏移位场景）
function onClick() {
  if (props.variant === 'pool' || props.variant === 'bench') emit('pick', props.player);
  else emit('open', props.player);
}
</script>
