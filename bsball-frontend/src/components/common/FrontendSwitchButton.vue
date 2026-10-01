<template>
  <button type="button" :class="cls" :title="TITLE" @click="onClick">切回旧版</button>
</template>

<script setup>
// FrontendSwitchButton —— 新旧版前端切换按钮（H41 组件化）
// 背景：原实现为全局 DOM 注入（frontendSwitch.js）+ 悬浮保底 + 顶栏锚点迁移；
// 当管理端顶栏发生重渲染把注入节点清掉后，恢复路径只挂回悬浮按钮（migrated 门槛阻止回迁），
// 导致按钮永久悬浮遮挡管理员下拉。本组件改为在各布局/页面模板内正式渲染，形态稳定。
// variant：admin（管理端顶栏内联弱化样式）｜portal（门户顶栏，继承 portal-header-btn 样式）
//          ｜intro（介绍页导航条胶囊）｜floating（悬浮胶囊，用于欢迎页/登录/404/文档壳等无顶栏页）
import { computed } from 'vue';
import { legacyTarget } from '../../utils/frontendSwitch';

const TITLE = '遇到问题时切回旧版前端（原有版本）';
const props = defineProps({
  variant: { type: String, default: 'floating' }
});

const cls = computed(() => {
  const base = 'frontend-switch-btn';
  if (props.variant === 'portal') return `${base} ${base}--portal portal-header-btn`;
  if (props.variant === 'admin') return `${base} ${base}--admin`;
  if (props.variant === 'intro') return `${base} ${base}--intro`;
  return `${base} ${base}--floating`;
});

function onClick() {
  location.href = legacyTarget();
}
</script>

<style scoped>
.frontend-switch-btn {
  cursor: pointer;
}

/* 管理端顶栏：弱化文字胶囊（与旧注入版逐属性一致） */
.frontend-switch-btn--admin {
  padding: 4px 10px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.08);
  color: inherit;
  font-size: 12px;
  line-height: 18px;
  margin-right: 10px;
  vertical-align: middle;
}

/* 介绍页导航条：胶囊（H28 锚点形态，与旧注入版逐属性一致） */
.frontend-switch-btn--intro {
  padding: 6px 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  font-weight: 500;
  line-height: 20px;
  margin-left: 16px;
  margin-right: 8px;
  flex-shrink: 0;
}

/* 无顶栏页面（欢迎页/登录/404/文档壳）：右上角悬浮胶囊（与旧注入保底形态逐属性一致） */
.frontend-switch-btn--floating {
  position: fixed;
  right: 16px;
  top: 16px;
  z-index: 1900;
  padding: 6px 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(15, 32, 58, 0.82);
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
  opacity: 0.72;
}
</style>
