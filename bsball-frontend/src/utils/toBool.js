// 真值判定 —— 行为移植自编译产物入口 chunk（In：后端可能返回 true/"true"/1/"1"）
export function toBool(value) {
  return value === true || value === 'true' || value === 1 || value === '1';
}
