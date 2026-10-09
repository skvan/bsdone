// 单飞去抖调度器（coalescing throttle 语义）：窗口内多次 schedule 合并为一次执行；
// 注意：窗口不随新事件后移（首批事件后固定 delayMs 触发），风暴持续时收敛为每窗口至多一次。
// 背景：门户存储桥收到同源标签高频写入（如新旧双版本互写 app-config 缓存）时合并为一次拉取，
//       配合"事件拉取不写回缓存"断链（2026-10-09 生产 429 实证修复）。
export function createSingleFlightDebounce(fn, delayMs = 300) {
  let timer = null;
  return {
    schedule() {
      if (timer != null) return;
      timer = setTimeout(() => {
        timer = null;
        fn();
      }, delayMs);
    },
    cancel() {
      if (timer != null) {
        clearTimeout(timer);
        timer = null;
      }
    }
  };
}
