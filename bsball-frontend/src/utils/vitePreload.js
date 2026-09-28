// Vite 动态导入预载助手等价实现（编译产物 __vitePreload）
// 原产物：__vitePreload(loader, deps, importer) 内部 Promise.resolve() 链 + deps 预取；
// 本地由打包器/模块系统承担资源加载，等价为直接执行 loader（预取为性能优化，不影响行为）。
export function vitePreload(loader) {
  return loader();
}
