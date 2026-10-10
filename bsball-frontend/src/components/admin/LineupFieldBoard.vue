<template>
  <div class="lineup-field" :style="{ aspectRatio: `${FIELD_VIEWBOX.width} / ${FIELD_VIEWBOX.height}` }">
    <svg class="lineup-field__bg" :viewBox="`0 0 ${FIELD_VIEWBOX.width} ${FIELD_VIEWBOX.height}`" aria-hidden="true">
      <!-- 草地与外野弧 -->
      <rect x="0" y="0" :width="FIELD_VIEWBOX.width" :height="FIELD_VIEWBOX.height" class="field-grass" />
      <path :d="OUTFIELD_ARC" class="field-grass-infield" />
      <path :d="INFIELD_DIAMOND" class="field-dirt" />
      <!-- 垒线 -->
      <path :d="BASE_LINES" class="field-base-line" />
      <!-- 垒包 -->
      <rect v-for="b in BASES" :key="b.k" :x="b.x - 4" :y="b.y - 4" width="8" height="8" class="field-base" />
      <!-- 投手丘 -->
      <circle :cx="slotDef('P').x" :cy="slotDef('P').y" r="10" class="field-mound" />
    </svg>

    <div
      v-for="slot in renderSlots"
      :key="slot.code"
      class="lineup-slot"
      :class="{
        'lineup-slot--empty': !slot.player,
        'lineup-slot--filled': !!slot.player,
        'lineup-slot--dh': slot.code === 'DH',
        'is-error': slot.error,
        'is-pending': slot.pending,
      }"
      :data-slot-code="slot.code"
      :ref="(el) => registerSlotEl(slot.code, el)"
      :style="slotStyle(slot.code)"
      @click="emit('slot-click', slot.code)"
    >
      <span class="lineup-slot__label">{{ slot.label }}</span>
      <LineupPlayerCard
        v-if="slot.player"
        :player="slot.player"
        :position="slot.code"
        :order-index="slot.orderIndex"
        :badge="slot.badge"
        variant="field"
        :state="slot.error ? 'error' : 'idle'"
        @open="emit('card-open', slot.code)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { FIELD_VIEWBOX, FIELD_SLOTS, DH_SLOT, slotDef } from '../../utils/lineupFieldPositions';
import LineupPlayerCard from './LineupPlayerCard.vue';

// 内野俯视图球场：SVG 背景铺满，卡槽用绝对定位（卡片是 DOM，便于拖拽与命中）
const props = defineProps({
  slots: { type: Array, default: () => [] }, // [{code, player, orderIndex, badge, error, pending}]
  mode: { type: String, default: '' },
});
const emit = defineEmits(['slot-click', 'card-open']);

// 卡槽 = 9 个守备位 + DH；命中父组件传入的同 code 数据后合并
const renderSlots = computed(() =>
  [...FIELD_SLOTS, DH_SLOT].map((s) => ({
    code: s.code,
    label: `${s.code} ${s.label}`,
    ...(props.slots.find((x) => x.code === s.code) ?? {}),
  }))
);

// 卡槽按视口百分比定位，随球场缩放
const slotStyle = (code) => {
  const def = slotDef(code);
  return {
    left: `${(def.x / FIELD_VIEWBOX.width) * 100}%`,
    top: `${(def.y / FIELD_VIEWBOX.height) * 100}%`,
  };
};

// 收集卡槽原生元素，供页面计算落点 rect（模板 ref 回调在卸载时 el 为 null）
const slotEls = {};
const registerSlotEl = (code, el) => {
  if (el) slotEls[code] = el;
  else delete slotEls[code];
};

defineExpose({
  slotEls,
  getSlotRects: () =>
    Object.entries(slotEls).map(([code, el]) => {
      const r = el.getBoundingClientRect();
      return { code, left: r.left, top: r.top, width: r.width, height: r.height };
    }),
});

// SVG 几何常量（坐标系与 LiveGame.js 的 field-svg 同族：本垒在下、外野在上）
const OUTFIELD_ARC = 'M 0 470 A 400 400 0 0 1 400 470 Z';
const INFIELD_DIAMOND = 'M 200 410 L 300 310 L 200 210 L 100 310 Z';
const BASE_LINES = 'M 200 410 L 300 310 M 200 410 L 100 310';
const BASES = [
  { k: 1, x: 300, y: 310 },
  { k: 2, x: 200, y: 210 },
  { k: 3, x: 100, y: 310 },
];
</script>
