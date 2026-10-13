// 电子秩序册（Issue #310 批 3）：业务错误判定
// request 层非 2xx 抛错时附加 body.code 为 apiCode；网络/系统错误无该项目或为 5xx。
export function isBusinessError(error) {
  return !!error && typeof error.apiCode === 'number' && error.apiCode < 500;
}
