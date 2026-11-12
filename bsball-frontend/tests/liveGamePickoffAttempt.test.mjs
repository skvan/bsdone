// 牵制企图（POA）计入投手统计 + 牵制出局补投手 A
// 运行：node --test tests/liveGamePickoffAttempt.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');

function slice(src, startMarker, endMarker) {
  const start = src.indexOf(startMarker);
  assert.ok(start >= 0, `缺少 ${startMarker}`);
  const end = src.indexOf(endMarker, start);
  assert.ok(end > start, `缺少 ${endMarker}`);
  return src.slice(start, end);
}

test('实时统计对象初始化与快照合并包含 pickoffAttempts', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const pt = slice(src, 'function Pt(){', 'function ue(e){');
  assert.match(pt, /pickoffAttempts:0/, '投手统计默认值缺少 pickoffAttempts');
});

test('save-live 载荷与 /stats 载入都映射 pickoffAttempts', () => {
  const src = read('../src/views/admin/LiveGame.js');
  assert.match(src, /pickoffAttempts:r\.pickoffAttempts\?\?0/, 'Eo() 保存载荷未映射 pickoffAttempts');
  assert.match(src, /pickoffAttempts:T\?\.pickoffAttempts\?\?0/, 'Do() 载入映射未映射 pickoffAttempts');
});

test('同垒牵制未果、出局、暴传三种结果各记一次投手牵制企图', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const credit = slice(src, 'function creditPickoffAttempt(){', 'function Sp(e){');
  assert.match(credit, /pitching\.pickoffAttempts=\(pitching\.pickoffAttempts\?\?0\)\+1/, '缺少投手牵制企图累加');
  const sp = slice(src, 'function Sp(e){', 'function kp(e){');
  assert.ok(sp.includes('creditPickoffAttempt()'), '同垒牵制选择分支未调用计数');
  const pickoffOut = slice(src, 'if(a?.type==="PICKOFF_OUT"){', 'if(a?.type==="CATCH_BACK_DP"){');
  assert.ok(pickoffOut.includes('creditPickoffAttempt()'), 'PICKOFF_OUT 完成时未记牵制企图');
  const advanceFe = slice(src, 'function lc(e){', 'function ic(){');
  assert.match(advanceFe, /advanceFeReasonId==="safe_pk_err"[\s\S]{0,120}creditPickoffAttempt\(\)/, '牵制失误推进完成时未记牵制企图');
});

test('牵制出局时接球守备员不是投手则补投手 A', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const pickoffOut = slice(src, 'if(a?.type==="PICKOFF_OUT"){', 'if(a?.type==="CATCH_BACK_DP"){');
  assert.match(pickoffOut, /Number\(P0\?\.id\)!==Number\(Se\.value\?\.id\)/, '缺少投手与接球守备员的同人判定');
  assert.match(pickoffOut, /stats\.fielding\.a=\(.*fielding\.a\?\?0\)\+1/, '缺少投手 A 累加');
  assert.match(pickoffOut, /accrueFieldingPlay\([^)]*"P",\{a:1\}\)/, '缺少投手 A 的逐位置记账');
});

test('观赛页、结果页、比赛详情、球员详情均带上 pickoffAttempts', () => {
  const watch = read('../src/views/admin/LiveGameWatch.js');
  assert.ok(watch.includes('pickoffAttempts:k.pickoffAttempts'), '观赛页投手行未映射 pickoffAttempts');
  const games = read('../src/views/admin/Games.js');
  assert.ok(games.includes('pickoffAttempts'), '结果页未包含 pickoffAttempts');
  assert.match(games, /columnFields668[\s\S]{0,160}pickoffAttempts/, '结果页旧列设置迁移未补 pickoffAttempts');
  const fieldSettings = read('../src/utils/gameDetailFieldSettingsStorage.js');
  assert.ok(fieldSettings.includes("{ key: 'pickoffAttempts', label: 'POA'"), '比赛详情默认投手字段缺少 POA');
  const playerDetail = read('../src/components/admin/PlayerDetailContent.js');
  assert.ok(playerDetail.includes('prop:"pickoffAttempts"'), '球员详情投手表缺少 POA 列');
});

test('后端实体、迁移、保存映射与统计聚合包含 pickoff_attempts', () => {
  const entity = read('../../bsball-backend/src/main/java/com/bsball/model/entity/GamePlayerStat.java');
  assert.match(entity, /private Integer pickoffAttempts;/, '实体缺少 pickoffAttempts');
  const liveDto = read('../../bsball-backend/src/main/java/com/bsball/model/dto/GameSaveLiveDTO.java');
  assert.match(liveDto, /private Integer pickoffAttempts;/, 'save-live DTO 缺少 pickoffAttempts');
  const resultDto = read('../../bsball-backend/src/main/java/com/bsball/model/dto/SaveGameResultDTO.java');
  assert.match(resultDto, /private Integer pickoffAttempts;/, 'save-result DTO 缺少 pickoffAttempts');
  const gameService = read('../../bsball-backend/src/main/java/com/bsball/service/GameService.java');
  assert.match(gameService, /setPickoffAttempts\(part\.getPickoffAttempts\(\)\)/, 'GameService 未落库 pickoffAttempts');
  const mapper = read('../../bsball-backend/src/main/resources/mapper/PlayerStatsMapper.xml');
  assert.match(mapper, /SUM\(s\.pickoff_attempts\)/, '统计聚合缺少 pickoff_attempts');
  const statsService = read('../../bsball-backend/src/main/java/com/bsball/service/StatsService.java');
  assert.match(statsService, /setPickoffAttempts\(/, 'StatsService 未映射 pickoffAttempts');
  const migrationDir = new URL('../../bsball-backend/src/main/resources/db/migration/postgresql/', import.meta.url);
  const migration = fs.readdirSync(migrationDir).find((name) => name.includes('pickoff_attempts'));
  assert.ok(migration, '缺少 pickoff_attempts Flyway 迁移');
  assert.match(fs.readFileSync(new URL(migration, migrationDir), 'utf8'), /pickoff_attempts/);
});
