// 浏览器识别 —— 行为移植自编译产物 browserInfo chunk
export function parseBrowserInfo(userAgent) {
  if (!userAgent?.trim()) return { name: '未知', version: '—' };
  let m = /Edg(?:e|A|iOS)?\/([\d.]+)/i.exec(userAgent);
  if (m) return { name: 'Microsoft Edge', version: m[1] ?? '—' };
  m = /(?:OPR|Opera)\/([\d.]+)/i.exec(userAgent);
  if (m) return { name: 'Opera', version: m[1] ?? '—' };
  if (/Brave/i.test(userAgent) && /Chrome/i.test(userAgent)) {
    return { name: 'Brave', version: /Chrome\/([\d.]+)/i.exec(userAgent)?.[1] ?? '—' };
  }
  m = /Chrome\/([\d.]+)/i.exec(userAgent);
  if (m && !/Edg/i.test(userAgent)) return { name: 'Google Chrome', version: m[1] ?? '—' };
  m = /Firefox\/([\d.]+)/i.exec(userAgent);
  if (m) return { name: 'Firefox', version: m[1] ?? '—' };
  if (/Safari/i.test(userAgent) && !/Chrome/i.test(userAgent)) {
    return { name: 'Safari', version: /Version\/([\d.]+)/i.exec(userAgent)?.[1] ?? '—' };
  }
  if (/MSIE ([\d.]+)/i.test(userAgent)) return { name: 'Internet Explorer', version: /MSIE ([\d.]+)/i.exec(userAgent)?.[1] ?? '—' };
  if (/Trident/i.test(userAgent)) return { name: 'Internet Explorer', version: /rv:([\d.]+)/i.exec(userAgent)?.[1] ?? '—' };
  return { name: '未知浏览器', version: '—' };
}
