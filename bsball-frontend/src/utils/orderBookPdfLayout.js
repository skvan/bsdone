// 门户报名名单 PDF：固定 A4 分页与离屏 HTML（纯函数，便于单测）
const PAGE_WIDTH = 794;
const PAGE_HEIGHT = 1123;

export const ORDER_BOOK_PDF_GEOMETRY = Object.freeze({
  pageWidth: PAGE_WIDTH,
  pageHeight: PAGE_HEIGHT,
  padding: 28,
  footerHeight: 30,
  bodyPaddingTop: 8,
  firstHeaderHeight: 100,
  continuationHeaderHeight: 56,
  columns: 5,
  columnGap: 10,
  cardHeight: 176,
  rowGap: 12,
  rowHeight: 188,
  teamHeaderHeight: 40,
  placeholderHeight: 56
});

const G = ORDER_BOOK_PDF_GEOMETRY;

const HTML_ESCAPE = Object.freeze({
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;'
});

const escapeHtml = (value) =>
  String(value ?? '').replace(/[&<>"']/g, (char) => HTML_ESCAPE[char]);

const truncate = (value, max) => {
  const text = String(value ?? '').trim();
  return text.length > max ? `${text.slice(0, max)}…` : text;
};

const truncateToWidth = (value, maxWidth, wideWidth = 16, narrowWidth = 9) => {
  const text = String(value ?? '').trim();
  const charWidth = (char) => {
    if (char.codePointAt(0) > 0x7f) return wideWidth;
    return /[A-Z0-9]/.test(char) ? Math.max(narrowWidth + 1, Math.round(wideWidth * 0.85)) : narrowWidth;
  };
  const total = [...text].reduce((sum, char) => sum + charWidth(char), 0);
  if (total <= maxWidth) return text;
  let used = 0;
  let out = '';
  for (const char of text) {
    const width = charWidth(char);
    if (used + width + wideWidth > maxWidth) return `${out}…`;
    out += char;
    used += width;
  }
  return `${out}…`;
};

const chunkRows = (rows, size) => {
  const list = Array.isArray(rows) ? rows : [];
  const chunks = [];
  for (let i = 0; i < list.length; i += size) chunks.push(list.slice(i, i + size));
  return chunks;
};

function bodyHeightFor(page) {
  const header = page.header === 'first' ? G.firstHeaderHeight : G.continuationHeaderHeight;
  return G.pageHeight - G.padding * 2 - G.footerHeight - G.bodyPaddingTop - header;
}

function placeholderReason(section) {
  if (section?.loadFailed) return 'loadFailed';
  if (!section?.submitted) return 'notSubmitted';
  return 'noActive';
}

/**
 * 以“队头 + 卡行”为单位装箱：整队优先整放，超长队按卡行续页并重复队头；
 * 卡行永不拆开，队头后至少跟一行卡片或占位。
 */
export function paginateOrderBookCards(sections) {
  const list = Array.isArray(sections) ? sections.filter(Boolean) : [];
  if (!list.length) return [];

  const pages = [];
  let used = 0;

  const addPage = () => {
    pages.push({ header: pages.length === 0 ? 'first' : 'continuation', teams: [] });
    used = 0;
  };

  addPage();
  for (const section of list) {
    const rowChunks = chunkRows(section.rows, G.columns);
    let rowIndex = 0;
    let continued = false;
    let addedPlaceholder = false;

    while (rowIndex < rowChunks.length || (rowChunks.length === 0 && !addedPlaceholder)) {
      const page = pages[pages.length - 1];
      const capacity = bodyHeightFor(page);
      const minimumCost = rowChunks.length ? G.rowHeight : G.placeholderHeight;

      if (used + G.teamHeaderHeight + minimumCost > capacity) {
        addPage();
        continue;
      }

      const entry = { section, continued, rows: [], placeholder: null };
      page.teams.push(entry);
      used += G.teamHeaderHeight;

      if (rowChunks.length === 0) {
        entry.placeholder = placeholderReason(section);
        used += G.placeholderHeight;
        addedPlaceholder = true;
        continue;
      }

      while (rowIndex < rowChunks.length && used + G.rowHeight <= capacity) {
        entry.rows.push(rowChunks[rowIndex]);
        used += G.rowHeight;
        rowIndex += 1;
      }

      if (rowIndex < rowChunks.length) {
        addPage();
        continued = true;
      }
    }
  }
  return pages;
}

export function fitImageContainSquare(width, height, maxSize) {
  const w = Number(width);
  const h = Number(height);
  if (!Number.isFinite(w) || !Number.isFinite(h) || w <= 0 || h <= 0) {
    return { side: 0, drawWidth: 0, drawHeight: 0, offsetX: 0, offsetY: 0 };
  }
  const side = Math.max(1, Math.min(maxSize, Math.max(w, h)));
  const scale = Math.min(1, maxSize / Math.max(w, h));
  const drawWidth = Math.max(1, Math.round(w * scale));
  const drawHeight = Math.max(1, Math.round(h * scale));
  return {
    side,
    drawWidth,
    drawHeight,
    offsetX: Math.round((side - drawWidth) / 2),
    offsetY: Math.round((side - drawHeight) / 2)
  };
}

export function scaleImageToFit(width, height, maxSize) {
  const w = Number(width);
  const h = Number(height);
  if (!Number.isFinite(w) || !Number.isFinite(h) || w <= 0 || h <= 0) return { width: 0, height: 0 };
  const scale = Math.min(1, maxSize / Math.max(w, h));
  return {
    width: Math.max(1, Math.round(w * scale)),
    height: Math.max(1, Math.round(h * scale))
  };
}

function imageValue(images, url) {
  if (!url) return '';
  if (images instanceof Map) return images.get(url) || '';
  return images?.[url] || '';
}

function renderImageOrInitial(images, url, label, className) {
  const dataUrl = imageValue(images, url);
  if (dataUrl) {
    return `<span class="${className}"><img src="${escapeHtml(dataUrl)}" alt=""></span>`;
  }
  const text = String(label || '?').trim().slice(0, 1) || '?';
  return `<span class="${className}"><i>${escapeHtml(text)}</i></span>`;
}

function renderCard(player, images) {
  const positions = Array.isArray(player.positions) ? player.positions.join(' / ') : '';
  return `<div class="obp-card">
    ${renderImageOrInitial(images, player.avatar, player.name, 'obp-avatar')}
    <div class="obp-pos">${escapeHtml(truncateToWidth(positions, 120, 12, 7))}</div>
    <div class="obp-name">${escapeHtml(truncateToWidth(player.name, 120))}</div>
    <div class="obp-number">${escapeHtml(truncate(player.number || '—', 8))}</div>
  </div>`;
}

function renderTeam(entry, images, labels) {
  const section = entry.section || {};
  const name = section.teamName || `ID:${section.teamId}`;
  const continued = entry.continued ? labels.continued || '' : '';
  const locked = section.rosterLocked
    ? `<span class="obp-locked">${escapeHtml(labels.locked || '')}</span>`
    : '';
  const count = section.loadFailed
    ? ''
    : `<span class="obp-team-count">${escapeHtml(labels.playerCount || '')} · ${Number(section.playerCount) || 0}</span>`;
  const body = entry.placeholder
    ? `<div class="obp-placeholder">${escapeHtml(labels[entry.placeholder] || '')}</div>`
    : entry.rows.map((row) => `<div class="obp-grid">${row.map((p) => renderCard(p, images)).join('')}</div>`).join('');

  return `<section class="obp-team">
    <div class="obp-team-head">
      ${renderImageOrInitial(images, section.teamLogo, name, 'obp-team-logo')}
      <span class="obp-team-name">${escapeHtml(truncateToWidth(name, 420))}${escapeHtml(continued)}</span>
      ${locked}${count}
    </div>
    ${body}
  </section>`;
}

const PAGE_STYLE = `
.obp-page{width:${PAGE_WIDTH}px;height:${PAGE_HEIGHT}px;box-sizing:border-box;padding:${G.padding}px;background:#fff;color:#0f172a;display:flex;flex-direction:column;overflow:hidden;font-family:"PingFang SC","Microsoft YaHei","Noto Sans SC",sans-serif}
.obp-head{box-sizing:border-box;flex:none;display:flex;flex-direction:column;justify-content:center;border-bottom:3px solid #1d4ed8}
.obp-head--first{height:${G.firstHeaderHeight}px;padding-bottom:12px}
.obp-head--continuation{height:${G.continuationHeaderHeight}px;padding-bottom:8px}
.obp-title{font-size:26px;line-height:1.15;font-weight:800;color:#0f172a;max-width:100%;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.obp-event{font-size:17px;line-height:1.3;font-weight:700;color:#1e3a8a;margin-top:4px;max-width:100%;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.obp-meta{display:flex;justify-content:space-between;gap:18px;margin-top:6px;font-size:12px;line-height:1.3;color:#64748b}
.obp-meta span{min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.obp-meta span:last-child{flex:none}
.obp-cont{font-size:18px;font-weight:700;color:#1e3a8a;max-width:100%;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.obp-body{box-sizing:border-box;flex:1;min-height:0;overflow:hidden;padding-top:${G.bodyPaddingTop}px}
.obp-team-head{height:36px;margin-bottom:4px;display:flex;align-items:center;gap:8px;min-width:0}
.obp-team-logo{width:26px;height:26px;border-radius:7px;background:#eff6ff;color:#1d4ed8;display:inline-flex;align-items:center;justify-content:center;position:relative;overflow:hidden;flex:none;font-size:13px;font-weight:800}
.obp-team-logo img{width:100%;height:100%;object-fit:contain;position:relative;z-index:1;background:#fff}
.obp-team-name{flex:1;min-width:0;font-size:16px;line-height:1;font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.obp-locked{flex:none;border:1px solid #f59e0b;border-radius:5px;padding:2px 6px;color:#b45309;background:#fffbeb;font-size:11px;line-height:1.2}
.obp-team-count{flex:none;color:#64748b;font-size:11px}
.obp-grid{display:grid;grid-template-columns:repeat(${G.columns},138px);column-gap:${G.columnGap}px;height:${G.cardHeight}px;margin-bottom:${G.rowGap}px}
.obp-card{box-sizing:border-box;width:138px;height:${G.cardHeight}px;border:1px solid #e2e8f0;border-radius:10px;background:#f8fafc;padding:8px;display:flex;flex-direction:column;align-items:center;overflow:hidden}
.obp-avatar{width:88px;height:88px;border-radius:8px;background:#e2e8f0;color:#64748b;display:flex;align-items:center;justify-content:center;position:relative;overflow:hidden;flex:none;font-size:30px;font-weight:800}
.obp-avatar img{width:100%;height:100%;object-fit:cover;position:relative;z-index:1}
.obp-avatar i,.obp-team-logo i{font-style:normal}
.obp-pos{width:100%;height:20px;margin-top:6px;font-size:12px;line-height:20px;color:#475569;text-align:center;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.obp-name{width:100%;height:24px;font-size:16px;line-height:24px;font-weight:800;color:#0f172a;text-align:center;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.obp-number{width:100%;height:20px;font-size:15px;line-height:20px;color:#1d4ed8;text-align:center;font-weight:700}
.obp-placeholder{height:${G.placeholderHeight}px;box-sizing:border-box;border:1px dashed #cbd5e1;border-radius:8px;background:#f8fafc;color:#64748b;display:flex;align-items:center;padding:0 14px;font-size:13px}
.obp-foot{height:${G.footerHeight}px;flex:none;box-sizing:border-box;border-top:1px solid #e2e8f0;padding-top:8px;display:flex;justify-content:space-between;align-items:flex-start;color:#94a3b8;font-size:11px}
`;

export function buildOrderBookPdfPagesHtml(pages, context = {}) {
  const list = Array.isArray(pages) ? pages : [];
  const labels = context.labels || {};
  const images = context.images || {};
  const eventLabel = truncate(
    `${context.eventName || ''}${context.eventSeason ? ` · ${context.eventSeason}` : ''}`,
    42
  );
  const continuationLabel = truncate(`${context.title || ''} · ${context.eventName || ''}`, 40);
  const renderPage = (page, index) => {
    const first = page.header === 'first';
    const head = first
      ? `<div class="obp-head obp-head--first">
          <div class="obp-title">${escapeHtml(context.title || '')}</div>
          <div class="obp-event">${escapeHtml(eventLabel)}</div>
          <div class="obp-meta"><span>${escapeHtml(truncate(context.summaryText || '', 44))}</span><span>${escapeHtml(context.generatedAtLine || '')}</span></div>
        </div>`
      : `<div class="obp-head obp-head--continuation"><div class="obp-cont">${escapeHtml(continuationLabel)}</div></div>`;
    return `<div class="obp-page" data-page="${index + 1}">
      ${head}
      <div class="obp-body">${(page.teams || []).map((entry) => renderTeam(entry, images, labels)).join('')}</div>
      <div class="obp-foot"><span>${escapeHtml(context.generatedAtLine || '')}</span><span>${index + 1} / ${list.length}</span></div>
    </div>`;
  };
  return `<style>${PAGE_STYLE}</style>${list.map(renderPage).join('')}`;
}

export function sanitizePdfFileName(name, fallback = 'order-book') {
  const cleaned = String(name ?? '').replace(/[/\\?%*:|"<>]/g, '_').trim().slice(0, 80);
  return cleaned || fallback;
}
