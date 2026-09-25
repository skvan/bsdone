// 文章/通知封面图解析 —— 行为移植自编译产物 noticeContentImage chunk
// 优先 cover；否则从正文提取首图（markdown 优先匹配，其次 html img，再宽匹配 markdown）
function pickContentImage(content, contentType) {
  if (!content?.trim()) return undefined;
  if ((contentType || 'html').toLowerCase() === 'markdown') {
    const md = content.match(/!\[[^\]]*]\(\s*([^)\s]+)\s*(?:\s["'][^"']*["'])?\s*\)/);
    if (md?.[1]) return md[1].trim();
  }
  const img = content.match(/<img[^>]+src\s*=\s*["']([^"']+)["']/i);
  if (img?.[1]) return img[1].trim();
  const loose = content.match(/!\[[^\]]*]\(\s*([^)\s]+)/);
  if (loose?.[1]) return loose[1].trim();
  return undefined;
}

export function resolveNoticeCoverUrl(item) {
  const cover = item.cover?.trim();
  return cover || pickContentImage(item.content ?? undefined, item.contentType);
}
