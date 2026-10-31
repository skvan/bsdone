// BOX SCORE 进攻/守备表展示（#446 关联）
// 进攻表：不再显示守位；替补（无棒次）缩进在对应棒次下
// 守备表：按首个守位归组，投手必在（行构建见 gameFieldingRows.test.mjs）
// 运行：node --test tests/gameDetailBoxScoreGrouping.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { normalizeFieldingPosition } from '../src/utils/gamePlayerStat.js';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
const detail = read('../src/components/admin/GameDetailContent.js');
const css = read('../src/styles/legacy/game-detail-content.css');

test('进攻表不再渲染守位：打者首格不含 position 文本', () => {
  assert.ok(!detail.includes('n(E(a,"position")||"-")'), '打者首格仍显示守位');
  assert.ok(detail.includes('class:"td2"'), '姓名额仍在');
  assert.ok(detail.includes('class:"td3"'), '背号额仍在');
});

test('进攻表替补缩进：只有打击表给无棒次行加 row-sub', () => {
  assert.ok(detail.includes('l.push("row-sub")'), '缺少替补行类');
  assert.match(detail, /\(e==="awayBatters"\|\|e==="homeBatters"\)&&!t\._totals&&t\.battingOrder==null&&l\.push\("row-sub"\)/);
  assert.ok(css.includes('.box-table-batters[data-v-764190f8] tr.row-sub .td2{padding-left:14px}'), '缺少替补缩进样式');
  assert.ok(css.includes('grid-template-columns:48px 1fr 48px'), '打者首格应为三列（去掉守位列）');
  assert.ok(css.includes('.player-info-cell[data-v-764190f8]{grid-template-columns:48px 1fr 48px 52px;'), '投手表首格应保持四列');
});

test('实时录入链路：首个守位写入 payload、快照与无快照重建', () => {
  const live = read('../src/views/admin/LiveGame.js');
  assert.ok(live.includes('firstPosition:normalizeFieldPos(e.firstPosition)||(a.isPitcherRow?"P":"")'), 'save-live payload 未带首个守位');
  assert.ok(live.includes('function ensureFirstFieldPositions()'), '缺少首个守位补齐入口');
  assert.ok(live.includes('ensureFirstFieldPositions();'), '开场/快照/保存未调用补齐');
  assert.ok(live.includes('firstPosition:firstFieldPositionOf(P,b)'), '守备换人入场未记录首个守位');
  // 代打/代跑只是占住被换下者的守位，尚未真正防守：入场时不得写 firstPosition
  assert.ok(!live.includes('firstPosition:firstFieldPositionOf(P,aPos)'), '代跑不得继承首个守位');
  assert.ok(!live.includes('firstPosition:firstFieldPositionOf(w,t.position)'), '代打不得继承首个守位');
  assert.ok(live.includes('firstPosition:normalizeFieldPos(T?.firstPosition)||(T?.battingOrder!=null?normalizeFieldPos(V):"")'), '无快照重建未恢复首个守位');
  assert.ok(!live.includes('firstPosition:normalizeFieldPos(T?.firstPosition)||normalizeFieldPos(V)'), '板凳不应把花名册守位当首个守位');
  const snapshot = live.slice(live.indexOf('function pa(e)'), live.indexOf('function pa(e)') + 3000);
  assert.ok(snapshot.includes('ensureFirstFieldPositions()'), '现场快照前未补齐首个守位');
  assert.ok(snapshot.includes('ensureStartingPositions()'), '现场快照前未补齐先发守备位');
  // 恢复现场时必须先还原 isTop 再补首个守位，否则下半局重进会把主队代打误当守备中
  const topApplied = live.indexOf('l.isTop=P.isTop??!0');
  const ensureRestore = live.indexOf('ensureFirstFieldPositions()', topApplied);
  assert.ok(topApplied > 0 && ensureRestore > topApplied, 'Vl() 的 ensureFirstFieldPositions 必须先还原 gameState.isTop');
  assert.ok(live.includes('startingPosition:e.startingPosition==null?void 0:normalizeFieldPos(e.startingPosition)||""'), 'save-live payload 未带先发守备位（未知应省略以保留旧值）');
  assert.ok(live.includes('function ensureStartingPositions()'), '缺少先发守备位补齐入口');
  assert.ok(live.includes('startingPosition:normalizeFieldPos(T?.startingPosition)||""'), '无快照重建未恢复先发守备位');
});

test('先发守备位补齐：开赛前按阵容写入，开赛后冻结，替补留空', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const at = src.indexOf('function ensureStartingPositions(){');
  assert.ok(at >= 0, '找不到 ensureStartingPositions()');
  let depth = 0, end = -1;
  const open = src.indexOf('{', at);
  for (let i = open; i < src.length; i++) {
    if (src[i] === '{') depth++;
    else if (src[i] === '}' && --depth === 0) { end = i + 1; break; }
  }
  assert.ok(end > 0, 'ensureStartingPositions() 括号不闭合');
  const ref = (v) => ({ value: v });
  const pos = (id, position, extra = {}) => ({ id, position, ...extra });
  const run = (started, home, away, benchHome = [], O = null, De = null, te = null, xe = null) => {
    const ctx = { F: ref(started), q: ref(home), X: ref(away), ye: ref(benchHome), ae: ref([]),
      O: ref(O), te: ref(te), De: ref(De), xe: ref(xe) };
    const factory = new Function('F', 'q', 'X', 'ye', 'ae', 'O', 'te', 'De', 'xe', 'normalizeFieldPos',
      `return (${src.slice(at, end)})`);
    const ensure = factory(ctx.F, ctx.q, ctx.X, ctx.ye, ctx.ae, ctx.O, ctx.te, ctx.De, ctx.xe, normalizeFieldingPosition);
    ensure();
    return [...home, ...away, ...benchHome];
  };

  const home = [pos(1, 'LF'), pos(4, 'DH')];
  const away = [pos(2, 'CF')];
  const bench = [pos(99, 'P')];
  const fpRow = { id: 99 };
  run(false, home, away, bench, 99, null, 99, fpRow);
  assert.equal(home[0].startingPosition, 'LF');
  assert.equal(home[1].startingPosition, '', '先发 DH 不是守备位');
  assert.equal(away[0].startingPosition, 'CF');
  assert.equal(bench[0].startingPosition, 'P', 'DH 制纯先发投手补 P');
  assert.equal(fpRow.startingPosition, 'P', '守备投手行补 P');

  const home2 = [pos(1, 'P', { startingPosition: 'LF' }), pos(5, 'SS'), pos(7, 'RF', { startingPosition: '' })];
  const bench2 = [pos(6, '', { startingPosition: 'CF' })];
  run(true, home2, [], bench2, 100, null, 100, null);
  assert.equal(home2[0].startingPosition, 'LF', '开赛后先发守位冻结，换位不改');
  assert.equal(home2[1].startingPosition, undefined, '旧快照未知先发守位保持未知，不能写成空串擦库');
  assert.equal(home2[2].startingPosition, '', '已知替补保持空串');
  assert.equal(bench2[0].startingPosition, 'CF', '已换下的先发保留先发守位');
});

test('开始录入快照写入先发守备位（含 DH 制纯投手）', () => {
  const board = read('../src/views/admin/LiveGameLineupBoard.vue');
  assert.match(board, /import \{ buildStartingBenchRows, buildStartingLineupRows, buildStartingPitcherRow \}/, '阵容板未引入先发守位映射');
  for (const pair of [
    ['homeLineup', 'buildStartingLineupRows'],
    ['awayLineup', 'buildStartingLineupRows'],
    ['homeBench', 'buildStartingBenchRows'],
    ['awayBench', 'buildStartingBenchRows'],
    ['homeFieldingPitcherRow', 'buildStartingPitcherRow'],
    ['awayFieldingPitcherRow', 'buildStartingPitcherRow']
  ]) {
    assert.ok(board.includes(`${pair[0]}: ${pair[1]}(`), `开始录入快照 ${pair[0]} 未走 ${pair[1]}`);
  }
  const live = read('../src/views/admin/LiveGame.js');
  assert.ok(live.includes('const inheritedSources=[j,A,T,Y,ee].filter(E=>E&&E.startingPosition!=null)'),
    'ft() 未按来源保留先发守备位');
  assert.ok(live.includes('startingPosition:inheritedStartingPosition'),
    'ft() 重建守备投手行时未写入继承的先发守备位');
});

test('实时观赛链路：打者行与投手行都携带首个守位', () => {
  const watch = read('../src/views/admin/LiveGameWatch.js');
  assert.ok(watch.includes('firstPosition:String(s.firstPosition??"")'), '打者行缺少首个守位');
  assert.ok(watch.includes('startingPosition:String(s.startingPosition??"")'), '打者行缺少先发守备位');
  assert.ok(watch.includes('position:"P",firstPosition:String(y.firstPosition??"")'), '投手行缺少首个守位');
  assert.ok(watch.includes('startingPosition:String(y.startingPosition??"")||(B===1?"P":"")'), '投手行缺少先发守备位');
});

test('首个守位补齐：先发/守备中才记，代打代跑等进攻球员留空', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const at = src.indexOf('function ensureFirstFieldPositions(){');
  assert.ok(at >= 0, '找不到 ensureFirstFieldPositions()');
  let depth = 0, end = -1;
  const open = src.indexOf('{', at);
  for (let i = open; i < src.length; i++) {
    if (src[i] === '{') depth++;
    else if (src[i] === '}' && --depth === 0) { end = i + 1; break; }
  }
  assert.ok(end > 0, 'ensureFirstFieldPositions() 括号不闭合');
  const ref = (v) => ({ value: v });
  const pos = (id, position) => ({ id, position });
  const run = (started, isTop, home, away) => {
    const ctx = { F: ref(started), q: ref(home), X: ref(away), ye: ref([]), ae: ref([]), l: { isTop } };
    const factory = new Function('F', 'q', 'X', 'ye', 'ae', 'l', 'normalizeFieldPos',
      `return (${src.slice(at, end)})`);
    const ensure = factory(ctx.F, ctx.q, ctx.X, ctx.ye, ctx.ae, ctx.l, normalizeFieldingPosition);
    ensure();
    return [...home, ...away];
  };

  const home = [pos(1, 'LF')];
  const away = [pos(2, 'CF'), pos(3, 'SS')];
  // 开赛前：所有打线球员按当前守位预置
  run(false, true, home, away);
  assert.equal(home[0].firstPosition, 'LF');
  assert.equal(away[0].firstPosition, 'CF');
  assert.equal(away[1].firstPosition, 'SS');

  // 开赛后 top：主队守备；客队先发保留预置，客队代打（非先发 id）留空
  const home2 = [pos(1, 'LF')];
  const awayStart = [pos(2, 'CF'), pos(3, 'SS')];
  run(false, true, home2, awayStart);
  const away2 = [...awayStart, pos(99, 'SS')];
  run(true, true, home2, away2);
  assert.equal(home2[0].firstPosition, 'LF', '守备方场上球员补齐');
  assert.equal(away2[1].firstPosition, 'SS', '先发保留首个守位');
  assert.equal(away2[2].firstPosition, '', '代打未防守前不得记入');

  // 换到 bottom：客队守备，代打此时才拿到首个守位
  run(true, false, home2, away2);
  assert.equal(away2[2].firstPosition, 'SS', '真正上场防守后才记录');
});

test('后端：first_position / starting_position 迁移与映射齐全', () => {
  const migration = read('../../bsball-backend/src/main/resources/db/migration/postgresql/V20261028160000__game_player_stat_first_position.sql');
  assert.ok(migration.includes('first_position'), '迁移缺少 first_position');
  const startingMigration = read('../../bsball-backend/src/main/resources/db/migration/postgresql/V20261031100000__game_player_stat_starting_position.sql');
  assert.ok(startingMigration.includes('starting_position'), '迁移缺少 starting_position');
  const entity = read('../../bsball-backend/src/main/java/com/bsball/model/entity/GamePlayerStat.java');
  assert.ok(entity.includes('private String firstPosition;'), '实体缺少 firstPosition');
  assert.ok(entity.includes('private String startingPosition;'), '实体缺少 startingPosition');
  const service = read('../../bsball-backend/src/main/java/com/bsball/service/GameService.java');
  assert.ok(service.includes('stat.setFirstPosition(normalizeFirstPosition(part.getFirstPosition()))'), 'saveLive 未映射首个守位');
  assert.ok(service.includes('stat.setFirstPosition(normalizeFirstPosition(part.getPosition()))'), 'saveResult 缺少兜底');
  assert.ok(service.includes('stat.setStartingPosition(normalizeFirstPosition(part.getStartingPosition()))'), 'saveLive 未映射先发守备位');
  assert.ok(service.includes('stat.setStartingPosition('), 'saveResult 缺少先发守位写入');
  assert.ok(service.includes('normalizeFirstPosition(part.getPosition())'), 'saveResult 缺少先发守位兜底');
});

test('Eo payload：首个守位优先取 player.firstPosition，投手行兜底 P', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const at = src.indexOf('function Eo(e,t,a){');
  assert.ok(at >= 0, '找不到 Eo()');
  let depth = 0, end = -1;
  for (let i = at + 'function Eo(e,t,a)'.length - 1; i < src.length; i++) {
    if (src[i] === '{') depth++;
    else if (src[i] === '}' && --depth === 0) { end = i + 1; break; }
  }
  assert.ok(end > 0, 'Eo() 括号不闭合');
  const Eo = new Function('normalizeFieldPos', `return (${src.slice(at, end)})`)(normalizeFieldingPosition);

  const stats = { batting: { ab: 1 }, pitching: { ip: 1 }, fielding: { po: 2 } };
  const twoWay = { id: 95, position: 'P', firstPosition: '3b', startingPosition: '3b', stats };
  assert.equal(Eo(twoWay, 7, { listOrder: 0, isPitcherRow: false }).firstPosition, '3B', '二刀流打者行应带首个守位');
  assert.equal(Eo(twoWay, 7, { listOrder: 20, isPitcherRow: true }).firstPosition, '3B', '二刀流投手行也应带首个守位');
  assert.equal(Eo(twoWay, 7, { listOrder: 0, isPitcherRow: false }).startingPosition, '3B', '二刀流打者行应带先发守位');
  assert.equal(Eo(twoWay, 7, { listOrder: 20, isPitcherRow: true }).startingPosition, '3B', '二刀流投手行应带先发守位');

  const dh = { id: 12, position: 'DH', stats, startingPosition: 'DH' };
  assert.equal(Eo(dh, 7, { listOrder: 4, isPitcherRow: false }).firstPosition, '', 'DH 不是守备位');
  assert.equal(Eo(dh, 7, { listOrder: 4, isPitcherRow: false }).startingPosition, '', 'DH 不是先发守备位');

  // 代打/代跑占着被换下者的守位，但还没有真正上场防守：不得把继承来的 position 当首个/先发守位
  const pinchHitter = { id: 31, position: 'LF', stats };
  assert.equal(Eo(pinchHitter, 7, { listOrder: 8, isPitcherRow: false }).firstPosition, '', '代打未防守前不得继承守位');
  assert.equal(Eo(pinchHitter, 7, { listOrder: 8, isPitcherRow: false }).startingPosition, undefined, '未知先发守位应省略，避免擦除旧值');

  const starterPitcher = { id: 22, position: 'P', startingPosition: 'P', stats };
  assert.equal(Eo(starterPitcher, 7, { listOrder: 9, isPitcherRow: true }).startingPosition, 'P', '先发投手行带 P');
  const reliever = { id: 21, position: '', stats };
  assert.equal(Eo(reliever, 7, { listOrder: 30, isPitcherRow: true }).firstPosition, 'P', '纯投手行兜底 P');
  assert.equal(Eo(reliever, 7, { listOrder: 30, isPitcherRow: true }).startingPosition, undefined, '救援投手未知先发守位应省略');
});
