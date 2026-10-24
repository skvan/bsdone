#!/usr/bin/env node
/**
 * Issue #130 统计勾稽关系只读审计脚本。
 *
 * 数据来源二选一：
 *   --games-json <path>   已抓取的 [{ game, rows }] JSON（推荐；可复用）
 *   --fetch               从 --api-base 重新抓取（只发 GET，不写业务数据）
 *
 * 示例：
 *   node scripts/audit/audit_stats_invariants.mjs --games-json _audit130/games.json
 *   node scripts/audit/audit_stats_invariants.mjs --fetch \
 *     --api-base https://bsdone.com/bsball-server \
 *     --games-json _audit130/games.json --summary-json _audit130/summary.json
 *
 * 退出码：0=未发现当前缺陷/覆盖缺口；1=发现；2=输入错误。
 *
 * 本脚本只读：不连数据库、不调用 POST/PUT/DELETE，不修改任何业务数据。
 */
import fs from 'node:fs/promises';
import path from 'node:path';

const SEV = { P0: 0, P1: 1, P2: 2, P3: 3 };
const args = parseArgs(process.argv.slice(2));
const API_BASE = (args['api-base'] || 'https://bsdone.com/bsball-server').replace(/\/+$/, '');
const CURRENT_FROM = args['current-from'] || '2026-09-28';
const MAX_SAMPLES = Number(args['max-samples'] || 8);
const GAMES_JSON = args['games-json'] || '';
const SUMMARY_JSON = args['summary-json'] || '';

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i += 1) {
    const a = argv[i];
    if (!a.startsWith('--')) continue;
    const key = a.slice(2);
    if (key === 'fetch') { out.fetch = true; continue; }
    const val = argv[i + 1];
    if (val == null || val.startsWith('--')) { out[key] = true; continue; }
    out[key] = val; i += 1;
  }
  return out;
}

function fail(msg) {
  console.error(`[输入错误] ${msg}`);
  process.exit(2);
}

const n = (v) => (v == null ? 0 : Number(v) || 0);
const sum = (rows, f) => rows.reduce((a, r) => a + n(f(r)), 0);
const isPitcher = (r) => n(r.isPitcher) === 1;
const pos = (r) => String(r.position ?? '').toUpperCase();
const isFieldingPosition = (r) => !['P', 'DH', 'PH'].includes(pos(r));

function ipOuts(ip) {
  const x = n(ip);
  const whole = Math.floor(x + 1e-9);
  let frac = Math.round((x - whole) * 10);
  if (frac === 3) frac = 2;
  return whole * 3 + frac;
}

function ipDecimal(ip) {
  return ipOuts(ip) / 3;
}

/** 每队打者行：同一球员若同时有打击行和投手行，优先取带打击数据的打击行。 */
function battingLines(rows, teamId) {
  const m = new Map();
  for (const r of rows.filter((x) => x.teamId === teamId)) {
    const hasBat = n(r.pa) || n(r.ab) || n(r.h) || n(r.r) || n(r.rbi) || n(r.bb) || n(r.hbp) || n(r.sb) || n(r.cs);
    if (!hasBat) continue;
    const cur = m.get(r.playerId);
    const curIsPitcher = cur ? isPitcher(cur) : false;
    if (!cur || (curIsPitcher && !isPitcher(r))) m.set(r.playerId, r);
  }
  return [...m.values()];
}

function pitchingLines(rows, teamId) {
  return rows.filter((r) => r.teamId === teamId && isPitcher(r));
}

/** 同一球员的守备字段在投手行可能被复制；按球员去重，优先打击行。 */
function fieldingLines(rows, teamId) {
  const m = new Map();
  for (const r of rows.filter((x) => x.teamId === teamId)) {
    if (!isFieldingPosition(r)) continue;
    if (!(n(r.po) || n(r.a) || n(r.e) || n(r.tc) || n(r.dp) || n(r.pb) || n(r.catcherSb) || n(r.catcherCs) || n(r.defInn))) continue;
    const cur = m.get(r.playerId);
    if (!cur || (isPitcher(cur) && !isPitcher(r))) m.set(r.playerId, r);
  }
  return [...m.values()];
}

function makeSink() {
  const map = new Map();
  return {
    add(code, severity, kind, game, msg) {
      // 同一个 code 可能同时出现在当前数据与旧数据；必须按 code|kind 分开统计，
      // 否则第一笔旧数据会把整个 code 标成 legacy，掩盖当前 P0/P1。
      const key = `${code}|${kind}`;
      if (!map.has(key)) map.set(key, { code, severity, kind, total: 0, games: new Set(), samples: [] });
      const c = map.get(key);
      c.total += 1;
      if (game?.id != null) c.games.add(game.id);
      if (c.samples.length < MAX_SAMPLES) {
        c.samples.push({ gameId: game?.id ?? null, gameday: game?.gameday ?? '', msg });
      }
    },
    toObject() {
      const out = {};
      for (const [k, v] of map) {
        out[k] = {
          code: v.code,
          severity: v.severity,
          kind: v.kind,
          total: v.total,
          games: v.games.size,
          samples: v.samples
        };
      }
      return out;
    }
  };
}

async function getJson(url) {
  const res = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(`${url} -> HTTP ${res.status}`);
  const json = await res.json();
  if (json.code !== 200) throw new Error(`${url} -> code ${json.code}: ${json.msg}`);
  return json.data;
}

async function fetchAll(apiBase) {
  const pageSize = 50;
  const first = await getJson(`${apiBase}/game/list?page=1&pageSize=1`);
  const total = first.total;
  const games = [];
  for (let page = 1; games.length < total; page += 1) {
    const data = await getJson(`${apiBase}/game/list?page=${page}&pageSize=${pageSize}`);
    games.push(...(data.list || []));
    if (!data.list?.length) break;
  }
  const result = new Array(games.length);
  let index = 0;
  async function worker() {
    while (index < games.length) {
      const i = index++;
      const g = games[i];
      try {
        const stats = await getJson(`${apiBase}/game/${g.id}/stats`);
        result[i] = { game: g, rows: stats.list || [] };
      } catch (err) {
        result[i] = { game: g, rows: [], error: String(err) };
      }
    }
  }
  await Promise.all([worker(), worker(), worker(), worker()]);
  return result;
}

async function loadGames() {
  if (args.fetch) {
    if (!GAMES_JSON) fail('--fetch 需要同时指定 --games-json 以保存抓取结果');
    const data = await fetchAll(API_BASE);
    await fs.mkdir(path.dirname(GAMES_JSON), { recursive: true });
    await fs.writeFile(GAMES_JSON, JSON.stringify(data, null, 1), 'utf8');
    return data;
  }
  if (!GAMES_JSON) fail('请指定 --games-json，或使用 --fetch');
  try {
    const data = JSON.parse(await fs.readFile(GAMES_JSON, 'utf8'));
    return Array.isArray(data) ? data : data.games;
  } catch (err) {
    fail(`无法读取 ${GAMES_JSON}: ${err.message}`);
  }
}

function audit(games) {
  const sink = makeSink();
  const current = games.filter((x) => x.game && (x.game.gameday || '') >= CURRENT_FROM);
  const currentIds = new Set(current.map((x) => x.game.id));

  for (const entry of games) {
    const { game, rows } = entry;
    if (!game || !Array.isArray(rows) || rows.length === 0) continue;
    const kind = currentIds.has(game.id) ? 'current' : 'legacy';
    const teams = [
      { tid: game.homeTeamId, opp: game.awayTeamId, label: '主队', score: n(game.homeScore), oppScore: n(game.awayScore), h: n(game.homeH), e: n(game.homeE) },
      { tid: game.awayTeamId, opp: game.homeTeamId, label: '客队', score: n(game.awayScore), oppScore: n(game.homeScore), h: n(game.awayH), e: n(game.awayE) }
    ];

    // G01 比分 = 各局得分
    for (const side of ['home', 'away']) {
      const innings = game[`${side}ScoreByInning`] || [];
      if (!Array.isArray(innings) || innings.length === 0) continue;
      const inningSum = innings.reduce((a, v) => a + (v === '/' ? 0 : n(v)), 0);
      if (inningSum !== n(game[`${side}Score`])) {
        sink.add('G01_SCORE_INNING', 'P3', kind, game, `${side} 总分 ${n(game[`${side}Score`])} ≠ 局分合计 ${inningSum}`);
      }
    }

    for (const t of teams) {
      if (t.tid == null) continue;
      const bat = battingLines(rows, t.tid);
      const pit = pitchingLines(rows, t.tid);
      const fld = fieldingLines(rows, t.tid);
      const oppBat = battingLines(rows, t.opp);

      // B11 队伍 R = Σ 个人 R
      const rSum = sum(bat, (r) => r.r);
      if (rSum !== t.score) sink.add('B11_TEAM_R', 'P3', kind, game, `${t.label} 总分 ${t.score} ≠ Σ个人R ${rSum}（差 ${t.score - rSum}）`);

      // B12 队伍 H = Σ 个人 H
      const hSum = sum(bat, (r) => r.h);
      if (hSum !== t.h) sink.add('B12_TEAM_H', 'P3', kind, game, `${t.label} 比分板 H ${t.h} ≠ Σ个人H ${hSum}`);

      // P01 投手 ΣR = 对方总分
      const prSum = sum(pit, (r) => r.pitchR);
      if (pit.length && prSum !== t.oppScore) {
        sink.add('P01_PITCH_R', kind === 'current' ? 'P0' : 'P3', kind, game, `${t.label} 投手ΣR ${prSum} ≠ 对方总分 ${t.oppScore}（差 ${t.oppScore - prSum}）`);
      }
      // 整队没有任何投手行：不能算作 P01，单独标为数据缺失
      if (!pit.length && t.oppScore > 0) {
        sink.add('P00_NO_PITCHER_ROWS', 'P3', kind, game, `${t.label} 对方得 ${t.oppScore} 分，但该队没有任何 is_pitcher=1 的投手行`);
      }

      // F03 队伍 E = Σ 个人 E
      const eSum = sum(fld, (r) => r.e);
      if (eSum !== t.e) sink.add('F03_TEAM_E', kind === 'current' ? 'P2' : 'P3', kind, game, `${t.label} 比分板 E ${t.e} ≠ Σ个人E ${eSum}`);

      // P08-P11 投打对照
      if (pit.length && oppBat.length) {
        const pairs = [
          ['pitchH', 'h', 'P08_PITCH_H', '被安打'],
          ['pitchSo', 'so', 'P09_PITCH_SO', '被三振'],
          ['pitchBbHp', 'bbHp', 'P10_PITCH_BBHP', 'BB+HBP'],
          ['pitchHr', 'hr', 'P11_PITCH_HR', '被墙外HR'],
          ['pitchInsideParkHr', 'insideParkHr', 'P11_PITCH_IPHR', '被场内HR']
        ];
        for (const [pk, bk, code, label] of pairs) {
          const a = sum(pit, (r) => r[pk]);
          const b = sum(oppBat, (r) => r[bk]);
          if (a !== b) sink.add(code, kind === 'current' ? 'P2' : 'P3', kind, game, `${t.label} ${label} 投手侧 ${a} ≠ 对方打者侧 ${b}`);
        }
      }

      // F06/F07 捕手对照
      const catchers = fld.filter((r) => pos(r) === 'C');
      if (catchers.length) {
        const ccs = sum(catchers, (r) => r.catcherCs);
        const cs = sum(oppBat, (r) => r.cs);
        if (ccs !== cs) sink.add('F06_CATCHER_CS', kind === 'current' ? 'P2' : 'P3', kind, game, `${t.label} 捕手CS ${ccs} ≠ 对方CS ${cs}`);
        const csb = sum(catchers, (r) => r.catcherSb);
        const sb = sum(oppBat, (r) => r.sb);
        if (csb !== sb) sink.add('F07_CATCHER_SB', kind === 'current' ? 'P2' : 'P3', kind, game, `${t.label} 捕手SB允许 ${csb} ≠ 对方SB ${sb}`);
      }
    }

    for (const r of rows) {
      // P02/P03 ER 拆分与上界
      if (isPitcher(r)) {
        const r0 = n(r.pitchR), er = n(r.er), uer = n(r.unearnedR), pend = n(r.pendingR);
        if (er + uer + pend !== r0) {
          sink.add('P02_ER_BREAKDOWN', kind === 'current' ? 'P0' : 'P3', kind, game,
            `投手 ${r.playerId}(team ${r.teamId}) R=${r0} ≠ ER ${er}+UER ${uer}+PENDING ${pend}`);
        }
        if (er > r0) sink.add('P03_ER_GT_R', kind === 'current' ? 'P0' : 'P3', kind, game, `投手 ${r.playerId}(team ${r.teamId}) ER ${er} > R ${r0}`);

        // P06 IP 记法
        const frac = Math.round((n(r.ip) - Math.floor(n(r.ip))) * 10);
        if (![0, 1, 2].includes(frac)) sink.add('P06_IP_FORMAT', 'P3', kind, game, `投手 ${r.playerId}(team ${r.teamId}) IP=${r.ip} 小数位非法`);

        // P07 出局数 vs BF（三杀打例外，报告里单独说明）
        const outs = ipOuts(r.ip);
        if (n(r.pitchBf) > 0 && outs > n(r.pitchBf)) {
          sink.add('P07_OUTS_GT_BF', 'P3', kind, game, `投手 ${r.playerId}(team ${r.teamId}) IP ${r.ip}(${outs}出局) > BF ${n(r.pitchBf)}`);
        }

        // P12 PA = BF
        if (n(r.pitchBf) > 0 && n(r.pitchPa) > 0 && n(r.pitchPa) !== n(r.pitchBf)) {
          sink.add('P12_PA_BF', 'P2', kind, game, `投手 ${r.playerId}(team ${r.teamId}) PA ${n(r.pitchPa)} ≠ BF ${n(r.pitchBf)}`);
        }

        // P13 NP 完整性
        if (n(r.pitchBf) > 0 && n(r.np) === 0) {
          const teamPit = pitchingLines(rows, r.teamId);
          const allZero = teamPit.length > 0 && teamPit.every((x) => n(x.np) === 0);
          sink.add(allZero ? 'P13_NP_ZERO_ALL' : 'P13_NP_ZERO_MIXED', 'P3', kind, game, `投手 ${r.playerId}(team ${r.teamId}) BF=${n(r.pitchBf)} 但 NP=0`);
        }
      }

      // B01 PA 公式
      const pa = n(r.pa), ab = n(r.ab);
      if (pa || ab) {
        const bbHp = n(r.bbHp);
        const bb = r.bb != null ? n(r.bb) : null;
        const hbp = r.hbp != null ? n(r.hbp) : null;
        const expected = ab + (bb != null || hbp != null ? n(bb) + n(hbp) : bbHp) + n(r.sf) + n(r.sh);
        if (expected !== pa) sink.add('B01_PA_FORMULA', 'P2', kind, game, `球员 ${r.playerId}(team ${r.teamId}) PA ${pa} ≠ ${expected}（AB+BB/HBP+SF+SH）`);
      }

      // B02 安打分量
      if (n(r.h) < n(r.doubles) + n(r.triples) + n(r.hr) + n(r.insideParkHr)) {
        sink.add('B02_H_COMPONENTS', 'P3', kind, game, `球员 ${r.playerId}(team ${r.teamId}) H=${n(r.h)} < 2B+3B+HR+IPHR`);
      }

      // B09 三振拆分
      if (!isPitcher(r) && n(r.so) !== n(r.soSwing) + n(r.soLooking) && (n(r.soSwing) || n(r.soLooking))) {
        sink.add('B09_SO_SPLIT', 'P3', kind, game, `球员 ${r.playerId}(team ${r.teamId}) SO ${n(r.so)} ≠ SO_swing ${n(r.soSwing)}+SO_looking ${n(r.soLooking)}`);
      }

      // B10 BB+HP 合计
      if (!isPitcher(r) && (r.bb != null || r.hbp != null) && n(r.bbHp) !== n(r.bb) + n(r.hbp)) {
        sink.add('B10_BBHP', 'P3', kind, game, `球员 ${r.playerId}(team ${r.teamId}) BB+HP ${n(r.bbHp)} ≠ BB ${n(r.bb)}+HBP ${n(r.hbp)}`);
      }

      // F01 TC = PO+A+E（非投手行）
      if (isFieldingPosition(r)) {
        const po = n(r.po), a = n(r.a), e = n(r.e), tc = n(r.tc);
        if ((po || a || e || tc) && tc !== po + a + e) {
          sink.add('F01_TC', kind === 'current' ? 'P2' : 'P3', kind, game, `球员 ${r.playerId}(team ${r.teamId},${pos(r)}) TC ${tc} ≠ PO ${po}+A ${a}+E ${e}`);
        }
      }
    }

    // G06 同一球员两行都带打击数据
    const byPlayer = new Map();
    for (const r of rows) {
      if (!(n(r.pa) || n(r.ab))) continue;
      const key = `${r.teamId}_${r.playerId}`;
      byPlayer.set(key, [...(byPlayer.get(key) || []), r]);
    }
    for (const [key, rs] of byPlayer) {
      if (rs.length > 1) sink.add('G06_DUP_BAT_ROWS', kind === 'current' ? 'P2' : 'P3', kind, game, `球员 ${key} 有 ${rs.length} 行都带 PA/AB（投手行可能复制了打击数据）`);
    }
  }

  // 当前数据的覆盖缺口（聚合判断）
  const currentRows = current.flatMap((x) => x.rows || []);
  const currentPitchers = currentRows.filter(isPitcher);
  const currentFielders = currentRows.filter(isFieldingPosition);
  const currentCatchers = currentFielders.filter((r) => pos(r) === 'C');
  const currentBatters = currentRows.filter((r) => !isPitcher(r));

  const coverage = [
    ['C01_PITCH_DECISIONS_ZERO', currentPitchers, (r) => n(r.gs) + n(r.w) + n(r.l) + n(r.sv) + n(r.hld), '当前所有投手 GS/W/L/SV/HLD 均为 0'],
    ['C02_PITCH_GOFO_ZERO', currentPitchers, (r) => n(r.go) + n(r.fo), '当前所有投手 GO/FO 均为 0'],
    ['C03_PITCH_BB_SPLIT_ZERO', currentPitchers, (r) => n(r.pitchBb) + n(r.pitchIbb) + n(r.pitchHbp), '当前所有投手 BB/IBB/HBP 分项均为 0（但 BB+HP 合计有值）', (rows) => sum(rows, (r) => r.pitchBbHp) > 0],
    ['C04_FIELD_DP_ZERO', currentFielders, (r) => n(r.dp), '当前所有守备员 DP 均为 0'],
    ['C05_FIELD_INN_ZERO', currentFielders, (r) => n(r.defInn), '当前所有守备员守备局数 INN 均为 0'],
    ['C06_CATCHER_ZERO', currentCatchers, (r) => n(r.pb) + n(r.catcherSb) + n(r.catcherCs), '当前所有捕手 PB/SB允许/CS 均为 0'],
    ['C07_BAT_IBB_GDP_ZERO', currentBatters, (r) => n(r.ibb) + n(r.gdp), '当前所有打者 IBB/GIDP 均为 0']
  ];
  for (const [code, rows, f, msg, precondition] of coverage) {
    if (rows.length === 0) continue;
    if (precondition && !precondition(rows)) continue;
    const total = sum(rows, f);
    if (total === 0) {
      sink.add(code, 'P1', 'coverage', null, `${msg}（样本 ${rows.length} 行）`);
    }
  }

  return { sink, currentCount: current.length, legacyCount: games.length - current.length, currentRows: currentRows.length };
}

function print(result) {
  const { summary, currentCount, legacyCount, currentRows } = result;
  const entries = Object.values(summary).sort((a, b) => (SEV[a.severity] - SEV[b.severity]) || b.total - a.total);
  console.log('='.repeat(78));
  console.log(`Issue #130 统计勾稽关系只读审计`);
  console.log(`场次：当前 ${currentCount} 场 / 旧数据 ${legacyCount} 场；当前统计行 ${currentRows} 行`);
  console.log(`判定基线：gameday >= ${CURRENT_FROM} 为当前数据`);
  console.log('='.repeat(78));

  if (entries.length === 0) {
    console.log('未发现异常。');
  } else {
    console.log('\n按严重度汇总：');
    for (const e of entries) {
      console.log(`  [${e.severity}][${e.kind}] ${e.code}: ${e.total} 条 / ${e.games} 场`);
    }
    console.log('\n样例：');
    for (const e of entries) {
      console.log(`\n[${e.code}] ${e.severity} / ${e.kind}`);
      for (const s of e.samples) {
        console.log(`  game ${s.gameId ?? '-'} (${s.gameday || '-'}): ${s.msg}`);
      }
    }
  }

  const blocking = entries.filter((e) => e.kind === 'current' || e.kind === 'coverage');
  console.log(`\n合计 ${entries.length} 类异常，${entries.reduce((a, e) => a + e.total, 0)} 条；当前缺陷/覆盖缺口 ${blocking.length} 类。`);
  console.log('本工具只读：未连接数据库、未写入任何业务数据。');
  return blocking.length > 0 ? 1 : 0;
}

async function main() {
  const games = await loadGames();
  if (!Array.isArray(games) || games.length === 0) fail('数据为空或格式不正确');
  const { sink, currentCount, legacyCount, currentRows } = audit(games);
  const summary = sink.toObject();
  if (SUMMARY_JSON) {
    await fs.mkdir(path.dirname(SUMMARY_JSON), { recursive: true });
    await fs.writeFile(SUMMARY_JSON, JSON.stringify(summary, null, 1), 'utf8');
  }
  const code = print({ summary, currentCount, legacyCount, currentRows });
  process.exitCode = code;
}

main().catch((err) => {
  console.error(`[运行错误] ${err.stack || err.message}`);
  process.exitCode = 2;
});
