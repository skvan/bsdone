<template>
  <div
    class="lineup-card"
    :class="[`lineup-card--${variant}`, `is-${state}`, { 'lineup-card--empty': !player }]"
    :data-player-id="player?.id"
    data-drag-handle
  >
    <template v-if="player">
      <span v-if="orderIndex" class="lineup-card__order">{{ orderIndex }}</span>
      <span class="lineup-card__number">#{{ player.number ?? '-' }}</span>
      <span class="lineup-card__name">{{ player.name }}</span>
      <span v-if="position" class="lineup-card__pos">{{ position }}</span>
      <span v-if="badge" class="lineup-card__badge">{{ badge }}</span>
    </template>
    <template v-else>
      <span class="lineup-card__placeholder">{{ placeholderText }}</span>
    </template>
  </div>
</template>

<script setup>
// 球员卡：池 / 替补 / 球场卡槽 / 打线 共用。
// 根元素带 data-drag-handle（interactjs 的默认拖拽区）；不要放 button 之类可聚焦元素，避免与长按拖拽抢手势。
const props = defineProps({
  player: { type: Object, default: null },
  position: { type: String, default: '' },
  orderIndex: { type: Number, default: 0 },
  badge: { type: String, default: '' },
  variant: { type: String, default: 'pool' },
  state: { type: String, default: 'idle' },
  placeholderText: { type: String, default: '' },
});
const emit = defineEmits(['pick', 'open']);
</script>
