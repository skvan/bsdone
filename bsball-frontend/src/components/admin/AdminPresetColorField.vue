<template>
  <div class="admin-preset-color-field">
    <div class="preset-toolbar">
      <div class="swatches" role="listbox" :aria-label="ariaLabel">
        <button
          v-if="leadingGutter && !allowEmpty"
          key="gutter"
          type="button"
          class="swatch swatch--default swatch--gutter"
          tabindex="-1"
          aria-hidden="true"
        >
          默认
        </button>
        <button
          v-if="allowEmpty"
          key="empty"
          type="button"
          class="swatch swatch--default"
          :class="{ 'is-active': !current }"
          title="跟随主题默认"
          @click="pick('')"
        >
          默认
        </button>
        <button
          v-for="(preset, index) in presets"
          :key="`${preset}-${index}`"
          type="button"
          class="swatch"
          :class="{ 'is-active': isActive(preset) }"
          :title="preset"
          :style="{ background: preset }"
          @click="pick(preset)"
        ></button>
      </div>
      <el-input
        v-if="showManualInput"
        :model-value="modelValue"
        size="small"
        :placeholder="inputPlaceholder"
        :clearable="allowEmpty"
        class="manual-input"
        @update:model-value="onManualInput"
      />
    </div>
  </div>
</template>

<script>
// 预设色选择 —— 行为移植自编译产物 AdminLayout chunk（AdminPresetColorField）
export default {
  __scopeId: 'data-v-1868f2ab'
};
</script>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  modelValue: {},
  presets: {},
  allowEmpty: { type: Boolean, default: false },
  leadingGutter: { type: Boolean, default: false },
  showManualInput: { type: Boolean, default: true },
  inputPlaceholder: { default: '#RRGGBB' },
  ariaLabel: { default: '颜色选择' }
});
const emit = defineEmits(['update:modelValue']);

const normalize = (hex) => (hex ?? '').trim().toLowerCase();
const current = computed(() => normalize(props.modelValue));

function isActive(hex) {
  return current.value === normalize(hex);
}

function pick(hex) {
  emit('update:modelValue', hex);
}

function onManualInput(value) {
  emit('update:modelValue', (value ?? '').trim());
}
</script>
