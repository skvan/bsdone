// 媒体图标渲染组件 —— 行为移植自编译产物 menuMediaIcons chunk
// 渲染方式：innerHTML 注入数据库下发的 SVG（与编译产物一致，scopeId 保持 data-v-4c79f335）
import { defineComponent, markRaw, createVNode, openBlock, createElementBlock } from 'vue';
import { exportSfc } from '../../utils/exportSfc';
import '../../styles/legacy/menu-media-icons.css';

const MediaMenuIcon = defineComponent({
  __name: 'MediaMenuIcon',
  props: {
    svg: {}
  },
  setup(props) {
    return () => (openBlock(), createElementBlock('span', { class: 'media-menu-icon', innerHTML: props.svg }, null, 8, ['innerHTML']));
  }
});

export const MediaMenuIconComponent = exportSfc(MediaMenuIcon, [['__scopeId', 'data-v-4c79f335']]);

// 生成一次性包装组件（markRaw 缓存，与编译产物 MenuMediaIconWrap 等价）
export function createMenuMediaIconWrap(svg) {
  return markRaw(
    defineComponent({
      name: 'MenuMediaIconWrap',
      setup() {
        return () => createVNode(MediaMenuIconComponent, { svg });
      }
    })
  );
}
