// vnode props 小工具 —— 行为移植自编译产物 element-plus chunk 内部工具（_l / Cb）
// normalizeVnodeProps：规范 vnode props 的 class/style（非字符串 class → normalizeClass；style 经 normalizeStyle）
import { normalizeClass, normalizeStyle, toRaw } from 'vue';

const isString = (value) => typeof value === 'string';
const isArray = Array.isArray;

export function normalizeVnodeProps(props) {
  if (!props) return null;
  const { class: cls, style } = props;
  if (cls && !isString(cls)) props.class = normalizeClass(cls);
  if (style) props.style = normalizeStyle(style);
  return props;
}

// mergePropValues：合并两个 props 值（数组拼接；对象浅合并；其一缺失取另一个）
export function mergePropValues(left, right) {
  if (!left || !right) return left || right;
  if (isArray(left) && isArray(right)) return left.concat(right);
  return { ...toRaw(left), ...toRaw(right) };
}
