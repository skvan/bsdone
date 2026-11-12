<template>
  <span
    class="player-claimed-badge"
    role="img"
    aria-label="已认领"
    title="该球员档案已被本人认领"
  >
    <svg viewBox="0 0 16 16" aria-hidden="true" focusable="false">
      <circle cx="8" cy="8" r="8" fill="currentColor" />
      <path
        d="M5.35 8.15 7.05 9.85 10.65 6.25"
        fill="none"
        stroke="#fff"
        stroke-width="1.55"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
  </span>
</template>

<style scoped>
.player-claimed-badge {
  position: absolute;
  right: 0;
  bottom: 0;
  display: grid;
  place-items: center;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  color: #2f6bff;
  background: var(--el-bg-color, #fff);
  box-shadow: 0 0 0 1.5px var(--el-bg-color, #fff), 0 1px 3px rgba(15, 23, 42, 0.3);
  cursor: help;
}

.player-claimed-badge svg {
  display: block;
  width: 12px;
  height: 12px;
}
</style>
