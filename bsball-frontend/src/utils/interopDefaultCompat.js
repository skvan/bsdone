// rolldown CJS 互操作助手 —— 行为近似移植自编译产物 rolldown-runtime chunk（导出 r）
// 语义：将模块（可能为 CJS 或无 default 的 ESM 命名空间）转为带 default 的命名空间；
// forceDefault 为真时即使 __esModule 也强制包裹 default（对应产物调用形态 xx(mod, 1)）
export function interopDefaultCompat(mod, forceDefault) {
  if (!forceDefault && mod != null && mod.__esModule) return mod;
  const ns = mod != null ? Object.assign({}, mod) : {};
  Object.defineProperty(ns, 'default', { value: mod, enumerable: true });
  return ns;
}

// rolldown CJS 模块工厂等价（导出 i）：执行工厂函数并返回其 exports
export function requireCjsModule(factory) {
  const cache = factory.__cachedExports;
  if (cache) return cache;
  const mod = { exports: {} };
  factory(mod.exports, mod);
  factory.__cachedExports = mod.exports;
  return mod.exports;
}
