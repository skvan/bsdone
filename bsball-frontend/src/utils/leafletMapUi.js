// 地图 UI 工具 —— 行为移植自编译产物 leaflet chunk（模块工厂）+ leafletMapUi chunk（底图/全屏控件/图标兜底）
// 依赖 npm leaflet（1.9.x）；产物中的 CJS 模块工厂等价于返回 leaflet 主模块
import L from 'leaflet';
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import iconUrl from 'leaflet/dist/images/marker-icon.png';
import shadowUrl from 'leaflet/dist/images/marker-shadow.png';

// 产物 leaflet chunk 的模块工厂（i()）等价：返回 leaflet 主模块
export function loadLeaflet() {
  return L;
}

// 默认图标兜底（产物：delete _getIconUrl + mergeOptions 三个图片 URL）
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({ iconRetinaUrl, iconUrl, shadowUrl });

const AMAP_ATTRIBUTION = '&copy; <a href="https://lbs.amap.com/" rel="noreferrer">高德地图</a>';

// 矢量底图（产物 w / 导出 i）
export function createVectorTiles() {
  return L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: '1234',
    maxZoom: 18,
    attribution: AMAP_ATTRIBUTION
  });
}

// 卫星底图（产物 k / 导出 r）
export function createSatelliteTiles() {
  return L.tileLayer('https://webst0{s}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}', {
    subdomains: '1234',
    maxZoom: 18,
    attribution: AMAP_ATTRIBUTION
  });
}

// 添加默认底图（产物 D / 导出 n）
export function addDefaultBaseLayer(map) {
  const layer = createVectorTiles();
  layer.addTo(map);
  return layer;
}

const LEAFLET_PREFIX = '<a href="https://leafletjs.com" title="A JavaScript library for interactive maps">Leaflet</a>';
const SVG_EXPAND =
  '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/></svg>';
const SVG_COLLAPSE =
  '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z"/></svg>';

function setAttributionPrefix(map) {
  map.attributionControl?.setPrefix(LEAFLET_PREFIX);
}

function getFullscreenElement() {
  const doc = document;
  return document.fullscreenElement ?? doc.webkitFullscreenElement ?? null;
}

function requestFullscreen(el) {
  const fn = el.requestFullscreen?.bind(el) ?? el.webkitRequestFullscreen?.bind(el) ?? el.msRequestFullscreen?.bind(el);
  return fn ? Promise.resolve(fn()) : Promise.reject(new Error('no fullscreen'));
}

function exitFullscreen() {
  const doc = document;
  const fn = document.exitFullscreen?.bind(document) ?? doc.webkitExitFullscreen?.bind(doc) ?? doc.msExitFullscreen?.bind(doc);
  return fn ? Promise.resolve(fn()) : Promise.reject(new Error('no exit'));
}

// 全屏控件（产物 F，内部使用）
function createFullscreenControl(map, position = 'topright') {
  return new (L.Control.extend({
    options: { position },
    onAdd() {
      const wrap = L.DomUtil.create('div', 'leaflet-control leaflet-bs-fullscreen leaflet-bar');
      const btn = L.DomUtil.create('a', 'leaflet-bs-fullscreen__btn', wrap);
      btn.href = '#';
      btn.setAttribute('role', 'button');
      btn.title = '全屏';
      btn.setAttribute('aria-label', '全屏');
      btn.innerHTML = SVG_EXPAND;
      const container = map.getContainer();
      let cleanup;
      const sync = () => {
        const isFull = getFullscreenElement() === container;
        btn.title = isFull ? '退出全屏' : '全屏';
        btn.setAttribute('aria-label', isFull ? '退出全屏' : '全屏');
        btn.innerHTML = isFull ? SVG_COLLAPSE : SVG_EXPAND;
        requestAnimationFrame(() => map.invalidateSize());
      };
      const onFsChange = () => sync();
      document.addEventListener('fullscreenchange', onFsChange);
      document.addEventListener('webkitfullscreenchange', onFsChange);
      cleanup = () => {
        document.removeEventListener('fullscreenchange', onFsChange);
        document.removeEventListener('webkitfullscreenchange', onFsChange);
      };
      L.DomEvent.disableClickPropagation(wrap);
      L.DomEvent.on(btn, 'click', L.DomEvent.stopPropagation);
      L.DomEvent.on(btn, 'click', L.DomEvent.preventDefault);
      L.DomEvent.on(btn, 'click', () => {
        if (getFullscreenElement() === container) exitFullscreen().catch(() => {});
        else requestFullscreen(container).catch(() => {});
      });
      wrap.__leafletFsCleanup = cleanup;
      sync();
      return wrap;
    },
    onRemove() {
      this.getContainer()?.__leafletFsCleanup?.();
    }
  }))({ position }).addTo(map);
}

// 地图品牌与全屏控件（产物 T / 导出 t）
export function applyMapBranding(map) {
  setAttributionPrefix(map);
  createFullscreenControl(map);
}
