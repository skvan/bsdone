/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.mapper.PlayerStatsMapper
 *  com.bsball.mapper.StatsLeadersMapper
 *  com.bsball.model.dto.BattingLeaderDTO
 *  com.bsball.model.dto.EventStatRow
 *  com.bsball.model.dto.FieldingLeaderDTO
 *  com.bsball.model.dto.PitchingLeaderDTO
 *  com.bsball.model.dto.PlayerGameLogEntryDTO
 *  com.bsball.model.dto.PlayerStatsByEventDTO
 *  com.bsball.model.dto.PlayerStatsDTO$Batting
 *  com.bsball.model.dto.PlayerStatsDTO$Fielding
 *  com.bsball.model.dto.PlayerStatsDTO$Pitching
 *  com.bsball.model.dto.StandingGameRowDTO
 *  com.bsball.model.dto.TeamOptionDto
 *  com.bsball.service.StatsService
 *  com.bsball.service.StatsService$StandLine
 *  com.bsball.service.StatsService$StarTopListMetricSpec
 *  com.bsball.service.TeamService
 *  com.bsball.stats.PlayerStatDrillBattingMetric
 *  com.bsball.stats.PlayerStatDrillFieldingMetric
 *  com.bsball.stats.PlayerStatDrillPitchingMetric
 *  com.bsball.utils.LeaderQualification
 *  com.bsball.utils.StatsFormatUtil
 *  com.bsball.utils.StatsLeadersOrderBy
 *  lombok.Generated
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.mapper.PlayerStatsMapper;
import com.bsball.mapper.StatsLeadersMapper;
import com.bsball.model.dto.BattingLeaderDTO;
import com.bsball.model.dto.EventStatRow;
import com.bsball.model.dto.FieldingLeaderDTO;
import com.bsball.model.dto.PitchingLeaderDTO;
import com.bsball.model.dto.PlayerGameLogEntryDTO;
import com.bsball.model.dto.PlayerStatsByEventDTO;
import com.bsball.model.dto.PlayerStatsDTO;
import com.bsball.model.dto.StandingGameRowDTO;
import com.bsball.model.dto.TeamOptionDto;
import com.bsball.service.StatsService;
import com.bsball.service.TeamService;
import com.bsball.stats.PlayerStatDrillBattingMetric;
import com.bsball.stats.PlayerStatDrillFieldingMetric;
import com.bsball.stats.PlayerStatDrillPitchingMetric;
import com.bsball.utils.LeaderQualification;
import com.bsball.utils.StatsFormatUtil;
import com.bsball.utils.StatsLeadersOrderBy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class StatsService {
    private final PlayerStatsMapper playerStatsMapper;
    private final StatsLeadersMapper statsLeadersMapper;
    private final TenantProperties tenantProperties;
    private final TeamService teamService;
    private static final List<StarTopListMetricSpec> STAR_TOP_LIST_SPECS = List.of((Object)new StarTopListMetricSpec("era", "pitching", "era", "asc", "era", "\u9632\u5fa1\u7387", "\u6295\u7403", "ERA"), (Object)new StarTopListMetricSpec("avg", "batting", "avg", "desc", "avg", "\u6253\u51fb\u7387", "\u6253\u51fb", "AVG"), (Object)new StarTopListMetricSpec("w", "pitching", "w", "desc", "w", "\u80dc\u6295", "\u6295\u7403", "W"), (Object)new StarTopListMetricSpec("h", "batting", "h", "desc", "h", "\u5b89\u6253\u6570", "\u6253\u51fb", "H"), (Object)new StarTopListMetricSpec("sv", "pitching", "sv", "desc", "sv", "\u6551\u63f4\u6210\u529f", "\u6295\u7403", "SV"), (Object)new StarTopListMetricSpec("hr", "batting", "hr", "desc", "hr", "\u5168\u5792\u6253", "\u6253\u51fb", "HR"), (Object)new StarTopListMetricSpec("hld", "pitching", "hld", "desc", "hld", "\u4e2d\u7ee7\u6210\u529f", "\u6295\u7403", "HLD"), (Object)new StarTopListMetricSpec("rbi", "batting", "rbi", "desc", "rbi", "\u6253\u70b9", "\u6253\u51fb", "RBI"), (Object)new StarTopListMetricSpec("so", "pitching", "pitchSo", "desc", "pitchSo", "\u593a\u4e09\u632f", "\u6295\u7403", "SO"), (Object)new StarTopListMetricSpec("sb", "batting", "sb", "desc", "sb", "\u76d7\u5792\u6210\u529f", "\u6253\u51fb", "SB"));

    private long resolveTenantId() {
        Long t = CurrentUserHolder.getTenantId();
        return t != null ? t.longValue() : this.tenantProperties.getDefaultId();
    }

    public Map<String, Object> getPlayerStats(Long playerId, String gameMode) {
        try {
            long tid = this.resolveTenantId();
            String gm = StatsService.normalizeGameMode((String)gameMode);
            PlayerStatsDTO.Batting bat = this.playerStatsMapper.selectBattingStats(playerId, Long.valueOf(tid), gm);
            PlayerStatsDTO.Pitching pit = this.playerStatsMapper.selectPitchingStats(playerId, Long.valueOf(tid), gm);
            PlayerStatsDTO.Fielding fld = this.playerStatsMapper.selectFieldingStats(playerId, Long.valueOf(tid), gm);
            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put("batting", this.formatBatting(bat));
            result.put("pitching", this.formatPitching(pit));
            result.put("fielding", this.formatFielding(fld));
            return result;
        }
        catch (Exception e) {
            LoggerFactory.getLogger(StatsService.class).error("getPlayerStats failed for playerId={}", (Object)playerId, (Object)e);
            return this.emptyPlayerStats();
        }
    }

    public Map<String, Object> emptyPlayerStats() {
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("batting", new HashMap());
        result.put("pitching", new HashMap());
        result.put("fielding", new HashMap());
        return result;
    }

    public List<PlayerStatsByEventDTO> getPlayerStatsBySeason(Long playerId, String gameMode) {
        if (playerId == null) {
            return List.of();
        }
        try {
            long tid = this.resolveTenantId();
            String gm = StatsService.normalizeGameMode((String)gameMode);
            List batRows = this.playerStatsMapper.selectBattingStatsByEvent(playerId, Long.valueOf(tid), gm);
            List pitRows = this.playerStatsMapper.selectPitchingStatsByEvent(playerId, Long.valueOf(tid), gm);
            List fldRows = this.playerStatsMapper.selectFieldingStatsByEvent(playerId, Long.valueOf(tid), gm);
            LinkedHashSet<Long> eventIds = new LinkedHashSet<Long>();
            for (EventStatRow r2 : batRows) {
                eventIds.add(r2.getEventId());
            }
            for (EventStatRow r2 : pitRows) {
                eventIds.add(r2.getEventId());
            }
            for (EventStatRow r2 : fldRows) {
                eventIds.add(r2.getEventId());
            }
            Map<Long, EventStatRow> batByEvent = batRows.stream().collect(Collectors.toMap(EventStatRow::getEventId, r -> r, (a, b) -> a));
            Map<Long, EventStatRow> pitByEvent = pitRows.stream().collect(Collectors.toMap(EventStatRow::getEventId, r -> r, (a, b) -> a));
            Map<Long, EventStatRow> fldByEvent = fldRows.stream().collect(Collectors.toMap(EventStatRow::getEventId, r -> r, (a, b) -> a));
            HashMap<Long, String> teamNamesByEvent = new HashMap<Long, String>();
            if (!eventIds.isEmpty()) {
                List tnRows = this.playerStatsMapper.selectTeamNamesByPlayerEvents(playerId, Long.valueOf(tid), new ArrayList(eventIds), gm);
                for (Map row : tnRows) {
                    Long eid = StatsService.getLong((Map)row, (String)"eventId");
                    if (eid == null) continue;
                    teamNamesByEvent.put(eid, StatsService.strVal((Map)row, (String)"teamNames"));
                }
            }
            ArrayList<PlayerStatsByEventDTO> list = new ArrayList<PlayerStatsByEventDTO>();
            for (Long eid : eventIds) {
                String eventName;
                EventStatRow br = batByEvent.get(eid);
                EventStatRow pr = pitByEvent.get(eid);
                EventStatRow fr = fldByEvent.get(eid);
                String string = br != null ? br.getEventName() : (eventName = pr != null ? pr.getEventName() : fr.getEventName());
                String season = br != null ? br.getSeason() : (pr != null ? pr.getSeason() : fr.getSeason());
                PlayerStatsByEventDTO dto = new PlayerStatsByEventDTO();
                dto.setEventId(eid);
                dto.setEventName(eventName);
                dto.setSeason(season != null ? season : "");
                dto.setTeamNames((String)teamNamesByEvent.get(eid));
                dto.setBatting(br != null && (br.getAb() > 0 || br.getPa() > 0) ? this.formatBatting(this.eventRowToBatting(br)) : new HashMap());
                dto.setPitching(pr != null && pr.getIp() > 0.0 ? this.formatPitching(this.eventRowToPitching(pr)) : new HashMap());
                dto.setFielding(fr != null && (fr.getGp() > 0 || fr.getInn() > 0.0 || fr.getTc() > 0 || fr.getPb() > 0 || fr.getCatcherCs() > 0) ? this.formatFielding(this.eventRowToFielding(fr)) : new HashMap());
                list.add(dto);
            }
            list.sort(Comparator.comparing(PlayerStatsByEventDTO::getSeason).reversed().thenComparing(PlayerStatsByEventDTO::getEventId, Comparator.reverseOrder()));
            return list;
        }
        catch (Exception e) {
            LoggerFactory.getLogger(StatsService.class).error("getPlayerStatsBySeason failed for playerId={}", (Object)playerId, (Object)e);
            return List.of();
        }
    }

    public List<PlayerGameLogEntryDTO> getPlayerGameLog(Long playerId, int limit, String gameMode) {
        if (playerId == null || limit <= 0) {
            return List.of();
        }
        try {
            long tid = this.resolveTenantId();
            String gm = StatsService.normalizeGameMode((String)gameMode);
            List rows = this.playerStatsMapper.selectGameLog(playerId, Long.valueOf(tid), Math.min(limit, 100), gm);
            ArrayList<PlayerGameLogEntryDTO> list = new ArrayList<PlayerGameLogEntryDTO>();
            for (Map row : rows) {
                PlayerGameLogEntryDTO dto = new PlayerGameLogEntryDTO();
                dto.setGameId(StatsService.getLong((Map)row, (String)"gameId"));
                dto.setEventId(StatsService.getLong((Map)row, (String)"eventId"));
                dto.setGameday(StatsService.strVal((Map)row, (String)"gameday"));
                dto.setEventName(StatsService.strVal((Map)row, (String)"eventName"));
                dto.setHomeTeamId(StatsService.getLong((Map)row, (String)"homeTeamId"));
                dto.setAwayTeamId(StatsService.getLong((Map)row, (String)"awayTeamId"));
                dto.setHomeScore(Integer.valueOf(StatsService.getInt((Map)row, (String)"homeScore")));
                dto.setAwayScore(Integer.valueOf(StatsService.getInt((Map)row, (String)"awayScore")));
                dto.setTeamId(StatsService.getLong((Map)row, (String)"teamId"));
                dto.setTeamName(StatsService.strVal((Map)row, (String)"teamName"));
                Object isHome = StatsService.mapGet((Map)row, (String)"isHome");
                dto.setIsHome(Boolean.valueOf(isHome instanceof Boolean ? (Boolean)isHome : isHome instanceof Number && ((Number)isHome).intValue() != 0));
                dto.setBatting(this.formatBatting(this.mapToBatting(row)));
                dto.setPitching(this.formatPitching(this.mapToPitching(row)));
                dto.setFielding(this.formatFielding(this.mapToFielding(row)));
                list.add(dto);
            }
            return list;
        }
        catch (Exception e) {
            LoggerFactory.getLogger(StatsService.class).error("getPlayerGameLog failed for playerId={}", (Object)playerId, (Object)e);
            return List.of();
        }
    }

    private static Object mapGet(Map<String, Object> m, String key) {
        if (m == null || key == null) {
            return null;
        }
        if (m.containsKey(key)) {
            return m.get(key);
        }
        Object v = m.get(key.toLowerCase(Locale.ROOT));
        if (v != null) {
            return v;
        }
        String normalizedTarget = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Object> e : m.entrySet()) {
            String nk;
            String k = e.getKey();
            if (k == null || !normalizedTarget.equals(nk = k.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT))) continue;
            return e.getValue();
        }
        return null;
    }

    private static String strVal(Map<String, Object> m, String key) {
        Object v = StatsService.mapGet(m, (String)key);
        return v == null ? null : v.toString();
    }

    private static Long getLong(Map<String, Object> m, String key) {
        Object v = StatsService.mapGet(m, (String)key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number)v).longValue();
        }
        try {
            return Long.parseLong(v.toString());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static int getInt(Map<String, Object> m, String key) {
        Object v = StatsService.mapGet(m, (String)key);
        if (v == null) {
            return 0;
        }
        if (v instanceof Number) {
            return ((Number)v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double getDouble(Map<String, Object> m, String key) {
        Object v = StatsService.mapGet(m, (String)key);
        if (v == null) {
            return 0.0;
        }
        if (v instanceof Number) {
            return ((Number)v).doubleValue();
        }
        try {
            return Double.parseDouble(v.toString());
        }
        catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private PlayerStatsDTO.Batting eventRowToBatting(EventStatRow r) {
        PlayerStatsDTO.Batting b = new PlayerStatsDTO.Batting();
        b.setGp(r.getGp());
        b.setPa(r.getPa());
        b.setAb(r.getAb());
        b.setR(r.getR());
        b.setH(r.getH());
        b.setRbi(r.getRbi());
        b.setHr(r.getHr());
        b.setInsideParkHr(r.getInsideParkHr());
        b.setSb(r.getSb());
        b.setBbHp(r.getBbHp());
        b.setSo(r.getSo());
        b.setDoubles(r.getDoubles());
        b.setTriples(r.getTriples());
        b.setE(r.getE());
        b.setGdp(r.getGdp());
        b.setSh(r.getSh());
        b.setSf(r.getSf());
        b.setBb(r.getBb());
        b.setIbb(r.getIbb());
        b.setHbp(r.getHbp());
        b.setCs(r.getCs());
        return b;
    }

    private PlayerStatsDTO.Pitching eventRowToPitching(EventStatRow r) {
        PlayerStatsDTO.Pitching p = new PlayerStatsDTO.Pitching();
        p.setGp(r.getGp());
        p.setIp(r.getIp());
        p.setEr(r.getEr());
        p.setPitchH(r.getPitchH());
        p.setPitchBbHp(r.getPitchBbHp());
        p.setPitchSo(r.getPitchSo());
        p.setPitchHr(r.getPitchHr());
        p.setPitchInsideParkHr(r.getPitchInsideParkHr());
        p.setGs(r.getGs());
        p.setSvo(r.getSvo());
        p.setCg(r.getCg());
        p.setPg(r.getPg());
        p.setW(r.getW());
        p.setL(r.getL());
        p.setSv(r.getSv());
        p.setHld(r.getHld());
        p.setPitchPa(r.getPitchPa());
        p.setPitchBf(r.getPitchBf());
        p.setPitchAb(r.getPitchAb());
        p.setNp(r.getNp());
        p.setPitchBb(r.getPitchBb());
        p.setPitchIbb(r.getPitchIbb());
        p.setPitchHbp(r.getPitchHbp());
        p.setWp(r.getWp());
        p.setBk(r.getBk());
        p.setPitchR(r.getPitchR());
        p.setGo(r.getGo());
        p.setFo(r.getFo());
        return p;
    }

    private PlayerStatsDTO.Fielding eventRowToFielding(EventStatRow r) {
        PlayerStatsDTO.Fielding f = new PlayerStatsDTO.Fielding();
        f.setGp(r.getGp());
        f.setInn(StatsFormatUtil.normalizeBaseballIp((double)r.getInn()));
        f.setTc(r.getTc());
        f.setPo(r.getPo());
        f.setA(r.getA());
        f.setE(r.getE());
        f.setPb(r.getPb());
        f.setCatcherCs(r.getCatcherCs());
        return f;
    }

    private PlayerStatsDTO.Batting mapToBatting(Map<String, Object> row) {
        PlayerStatsDTO.Batting b = new PlayerStatsDTO.Batting();
        b.setGp(1);
        b.setPa(StatsService.getInt(row, (String)"pa"));
        b.setAb(StatsService.getInt(row, (String)"ab"));
        b.setR(StatsService.getInt(row, (String)"r"));
        b.setH(StatsService.getInt(row, (String)"h"));
        b.setRbi(StatsService.getInt(row, (String)"rbi"));
        b.setHr(StatsService.getInt(row, (String)"hr"));
        b.setInsideParkHr(StatsService.getInt(row, (String)"insideParkHr"));
        b.setSb(StatsService.getInt(row, (String)"sb"));
        b.setBbHp(StatsService.getInt(row, (String)"bbHp"));
        b.setSo(StatsService.getInt(row, (String)"so"));
        b.setDoubles(StatsService.getInt(row, (String)"doubles"));
        b.setTriples(StatsService.getInt(row, (String)"triples"));
        b.setE(StatsService.getInt(row, (String)"e"));
        b.setGdp(StatsService.getInt(row, (String)"gdp"));
        b.setSh(StatsService.getInt(row, (String)"sh"));
        b.setSf(StatsService.getInt(row, (String)"sf"));
        b.setBb(StatsService.getInt(row, (String)"bb"));
        b.setIbb(StatsService.getInt(row, (String)"ibb"));
        b.setHbp(StatsService.getInt(row, (String)"hbp"));
        b.setCs(StatsService.getInt(row, (String)"cs"));
        return b;
    }

    private PlayerStatsDTO.Pitching mapToPitching(Map<String, Object> row) {
        PlayerStatsDTO.Pitching p = new PlayerStatsDTO.Pitching();
        p.setGp(StatsService.getInt(row, (String)"isPitcher") != 0 ? 1 : 0);
        p.setIp(StatsService.getDouble(row, (String)"ip"));
        p.setEr(StatsService.getInt(row, (String)"er"));
        p.setPitchH(StatsService.getInt(row, (String)"pitchH"));
        p.setPitchBbHp(StatsService.getInt(row, (String)"pitchBbHp"));
        p.setPitchSo(StatsService.getInt(row, (String)"pitchSo"));
        p.setPitchHr(StatsService.getInt(row, (String)"pitchHr"));
        p.setPitchInsideParkHr(StatsService.getInt(row, (String)"pitchInsideParkHr"));
        p.setGs(StatsService.getInt(row, (String)"gs"));
        p.setSvo(StatsService.getInt(row, (String)"svo"));
        p.setCg(StatsService.getInt(row, (String)"cg"));
        p.setPg(StatsService.getInt(row, (String)"pg"));
        p.setW(StatsService.getInt(row, (String)"w"));
        p.setL(StatsService.getInt(row, (String)"l"));
        p.setSv(StatsService.getInt(row, (String)"sv"));
        p.setHld(StatsService.getInt(row, (String)"hld"));
        p.setPitchPa(StatsService.getInt(row, (String)"pitchPa"));
        p.setPitchBf(StatsService.getInt(row, (String)"pitchBf"));
        p.setPitchAb(StatsService.getInt(row, (String)"pitchAb"));
        p.setNp(StatsService.getInt(row, (String)"np"));
        p.setPitchBb(StatsService.getInt(row, (String)"pitchBb"));
        p.setPitchIbb(StatsService.getInt(row, (String)"pitchIbb"));
        p.setPitchHbp(StatsService.getInt(row, (String)"pitchHbp"));
        p.setWp(StatsService.getInt(row, (String)"wp"));
        p.setBk(StatsService.getInt(row, (String)"bk"));
        p.setPitchR(StatsService.getInt(row, (String)"pitchR"));
        p.setGo(StatsService.getInt(row, (String)"go"));
        p.setFo(StatsService.getInt(row, (String)"fo"));
        return p;
    }

    private PlayerStatsDTO.Fielding mapToFielding(Map<String, Object> row) {
        PlayerStatsDTO.Fielding f = new PlayerStatsDTO.Fielding();
        int tc = StatsService.getInt(row, (String)"tc");
        int po = StatsService.getInt(row, (String)"po");
        int a = StatsService.getInt(row, (String)"a");
        int e = StatsService.getInt(row, (String)"e");
        double defInn = StatsFormatUtil.normalizeBaseballIp((double)StatsService.getDouble(row, (String)"defInn"));
        f.setGp(tc > 0 || po > 0 || a > 0 || e > 0 || defInn > 0.0 ? 1 : 0);
        f.setInn(defInn);
        f.setTc(tc > 0 ? tc : po + a + e);
        f.setPo(po);
        f.setA(a);
        f.setE(e);
        f.setPb(StatsService.getInt(row, (String)"pb"));
        f.setCatcherCs(StatsService.getInt(row, (String)"catcherCs"));
        return f;
    }

    private static int obpWalksNumerator(PlayerStatsDTO.Batting b) {
        int hbp;
        int bb = b.getBb();
        int split = bb + (hbp = b.getHbp());
        if (split > 0) {
            return split;
        }
        return b.getBbHp();
    }

    private Map<String, Object> formatBatting(PlayerStatsDTO.Batting b) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        if (b == null) {
            return m;
        }
        int ab = b.getAb();
        int pa = b.getPa();
        int h = b.getH();
        int doubles = b.getDoubles();
        int triples = b.getTriples();
        int hr = b.getHr();
        int insideParkHr = b.getInsideParkHr();
        int sb = b.getSb();
        int cs = b.getCs();
        int singles = Math.max(0, h - doubles - triples - hr - insideParkHr);
        int tb = singles + doubles * 2 + triples * 3 + hr * 4 + insideParkHr * 4;
        m.put("gp", b.getGp());
        m.put("pa", pa);
        m.put("ab", ab);
        m.put("r", b.getR());
        m.put("h", h);
        m.put("rbi", b.getRbi());
        m.put("hr", hr);
        m.put("insideParkHr", b.getInsideParkHr());
        m.put("sb", sb);
        m.put("bbHp", b.getBbHp());
        m.put("so", b.getSo());
        m.put("doubles", doubles);
        m.put("triples", triples);
        m.put("e", b.getE());
        m.put("gdp", b.getGdp());
        m.put("sh", b.getSh());
        m.put("sf", b.getSf());
        m.put("bb", b.getBb());
        m.put("ibb", b.getIbb());
        m.put("hbp", b.getHbp());
        m.put("cs", cs);
        m.put("singles", singles);
        m.put("tb", tb);
        int walksForObp = StatsService.obpWalksNumerator((PlayerStatsDTO.Batting)b);
        m.put("avg", ab > 0 ? StatsFormatUtil.fmtAvg((double)((double)h / (double)ab)) : "0");
        m.put("obp", pa > 0 ? StatsFormatUtil.fmtAvg((double)((double)(h + walksForObp) / (double)pa)) : "0");
        m.put("slg", ab > 0 ? StatsFormatUtil.fmtAvg((double)((double)tb / (double)ab)) : "0");
        m.put("ops", ab > 0 && pa > 0 ? StatsFormatUtil.fmtDecimal((double)((double)(h + walksForObp) / (double)pa + (double)tb / (double)ab), (int)3) : "0");
        m.put("sbPct", sb + cs > 0 ? StatsFormatUtil.fmtPct((Double)(100.0 * (double)sb / (double)(sb + cs))) : "0");
        return m;
    }

    private Map<String, Object> formatPitching(PlayerStatsDTO.Pitching p) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        if (p == null) {
            return m;
        }
        double ip = p.getIp();
        m.put("gp", p.getGp());
        m.put("ip", ip);
        m.put("er", p.getEr());
        m.put("pitchH", p.getPitchH());
        m.put("pitchBbHp", p.getPitchBbHp());
        m.put("pitchSo", p.getPitchSo());
        m.put("pitchHr", p.getPitchHr());
        m.put("pitchInsideParkHr", p.getPitchInsideParkHr());
        m.put("gs", p.getGs());
        m.put("svo", p.getSvo());
        m.put("cg", p.getCg());
        m.put("pg", p.getPg());
        m.put("w", p.getW());
        m.put("l", p.getL());
        m.put("sv", p.getSv());
        m.put("hld", p.getHld());
        m.put("pitchPa", p.getPitchPa());
        m.put("pitchBf", p.getPitchBf());
        m.put("np", p.getNp());
        m.put("pitchBb", p.getPitchBb());
        m.put("pitchIbb", p.getPitchIbb());
        m.put("pitchHbp", p.getPitchHbp());
        m.put("wp", p.getWp());
        m.put("bk", p.getBk());
        m.put("pitchR", p.getPitchR());
        m.put("go", p.getGo());
        m.put("fo", p.getFo());
        double ipDec = StatsFormatUtil.baseballIpToDecimalInnings((double)ip);
        m.put("whip", ipDec > 0.0 ? StatsFormatUtil.fmtDecimal((double)((double)(p.getPitchH() + p.getPitchBbHp()) / ipDec), (int)2) : "0");
        m.put("era", ipDec > 0.0 ? StatsFormatUtil.fmtDecimal((double)(9.0 * (double)p.getEr() / ipDec), (int)2) : "0");
        m.put("goFo", p.getFo() > 0 ? StatsFormatUtil.fmtDecimal((double)((double)p.getGo() / (double)p.getFo()), (int)2) : "-");
        m.put("pitchAb", p.getPitchAb());
        return m;
    }

    private Map<String, Object> formatFielding(PlayerStatsDTO.Fielding f) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        if (f == null) {
            return m;
        }
        int tc = f.getTc();
        m.put("gp", f.getGp());
        m.put("inn", StatsFormatUtil.normalizeBaseballIp((double)f.getInn()));
        m.put("tc", tc);
        m.put("po", f.getPo());
        m.put("a", f.getA());
        m.put("e", f.getE());
        m.put("pb", f.getPb());
        m.put("catcherCs", f.getCatcherCs());
        m.put("tcPct", tc > 0 ? StatsFormatUtil.fmtPct((Double)(100.0 * (double)(f.getPo() + f.getA()) / (double)tc)) : "0");
        return m;
    }

    private static String normalizeLeaderKeyword(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeHomeAway(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().toLowerCase(Locale.ROOT);
        if ("home".equals(t) || "away".equals(t)) {
            return t;
        }
        return null;
    }

    private static String normalizeHand(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().toUpperCase(Locale.ROOT);
        if ("L".equals(t) || "R".equals(t) || "S".equals(t)) {
            return t;
        }
        return null;
    }

    private static String normalizeGameMode(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().toUpperCase(Locale.ROOT);
        if ("BASEBALL".equals(t) || "SOFTBALL".equals(t)) {
            return t;
        }
        return null;
    }

    private static int normalizePage(int page) {
        return Math.max(1, page);
    }

    private static int normalizePageSize(int pageSize) {
        return Math.min(500, Math.max(1, pageSize));
    }

    private static String normalizeTeamSortOrder(String sortOrder, String defaultDir) {
        if (sortOrder != null && sortOrder.toLowerCase(Locale.ROOT).startsWith("asc")) {
            return "ASC";
        }
        if (sortOrder != null && sortOrder.toLowerCase(Locale.ROOT).startsWith("desc")) {
            return "DESC";
        }
        return defaultDir;
    }

    private static String teamBattingOrderBy(String sortProp, String sortOrder) {
        String p = sortProp == null ? "ops" : sortProp.trim();
        String d = StatsService.normalizeTeamSortOrder((String)sortOrder, (String)"DESC");
        Set allow = Set.of((Object[])new String[]{"teamId", "teamName", "gp", "pa", "ab", "r", "h", "rbi", "doubles", "triples", "hr", "insideParkHr", "sb", "cs", "so", "bb", "hbp", "tb", "avg", "obp", "slg", "ops"});
        if (!allow.contains(p)) {
            p = "ops";
            d = "DESC";
        }
        String primary = switch (p) {
            case "ops" -> "CASE WHEN (q.obp + q.slg) IS NULL THEN 1 ELSE 0 END, (q.obp + q.slg) " + d;
            case "teamName" -> "q.teamname " + d;
            case "teamId" -> "q.teamid " + d;
            default -> "q." + p + " " + d;
        };
        return primary + ", q.teamid ASC";
    }

    private static String teamPitchingOrderBy(String sortProp, String sortOrder) {
        String p = sortProp == null ? "era" : sortProp.trim();
        String d = StatsService.normalizeTeamSortOrder((String)sortOrder, (String)"ASC");
        Set allow = Set.of((Object[])new String[]{"teamId", "teamName", "gp", "gs", "w", "l", "sv", "hld", "ip", "whip", "era", "pitchBf", "np", "pitchH", "pitchHr", "pitchInsideParkHr", "pitchBb", "pitchHbp", "pitchSo", "wp", "bk", "pitchR", "er"});
        if (!allow.contains(p)) {
            p = "era";
            d = "ASC";
        }
        String primary = switch (p) {
            case "teamName" -> "q.teamname " + d;
            case "teamId" -> "q.teamid " + d;
            default -> "q." + p + " " + d;
        };
        return primary + ", q.teamid ASC";
    }

    private static String teamFieldingOrderBy(String sortProp, String sortOrder) {
        String p = sortProp == null ? "tcPct" : sortProp.trim();
        String d = StatsService.normalizeTeamSortOrder((String)sortOrder, (String)"DESC");
        Set allow = Set.of((Object[])new String[]{"teamId", "teamName", "gp", "gs", "inn", "tc", "po", "a", "e", "dp", "pb", "catcherCs", "tcPct"});
        if (!allow.contains(p)) {
            p = "tcPct";
            d = "DESC";
        }
        String primary = switch (p) {
            case "teamName" -> "q.teamname " + d;
            case "teamId" -> "q.teamid " + d;
            default -> "q." + p + " " + d;
        };
        return primary + ", q.teamid ASC";
    }

    public PageResult<Map<String, Object>> getBattingLeaders(Long eventId, List<Long> eventIds, List<String> years, List<Long> teamIds, String playerKeyword, String position, String homeAway, String batterHand, String pitcherHand, String gameMode, int page, int pageSize, String sortProp, String sortOrder) {
        return this.getBattingLeaders(eventId, eventIds, years, teamIds, playerKeyword, position, homeAway, batterHand, pitcherHand, gameMode, page, pageSize, sortProp, sortOrder, true);
    }

    public PageResult<Map<String, Object>> getBattingLeaders(Long eventId, List<Long> eventIds, List<String> years, List<Long> teamIds, String playerKeyword, String position, String homeAway, String batterHand, String pitcherHand, String gameMode, int page, int pageSize, String sortProp, String sortOrder, boolean applyQualification) {
        long tid = this.resolveTenantId();
        int p = StatsService.normalizePage((int)page);
        int ps = StatsService.normalizePageSize((int)pageSize);
        long offset = (long)(p - 1) * (long)ps;
        String kw = StatsService.normalizeLeaderKeyword((String)playerKeyword);
        String pos = StatsService.normalizeLeaderKeyword((String)position);
        String ha = StatsService.normalizeHomeAway((String)homeAway);
        String bh = StatsService.normalizeHand((String)batterHand);
        String ph = StatsService.normalizeHand((String)pitcherHand);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        String orderBy = StatsLeadersOrderBy.batting((String)sortProp, (String)sortOrder);
        long total = this.statsLeadersMapper.countBattingLeaders(Long.valueOf(tid), eventId, eventIds, years, teamIds, kw, pos, ha, bh, ph, gm, Boolean.valueOf(applyQualification));
        List rows = this.statsLeadersMapper.selectBattingLeadersPage(Long.valueOf(tid), eventId, eventIds, years, teamIds, kw, pos, ha, bh, ph, gm, Boolean.valueOf(applyQualification), orderBy, ps, offset);
        int maxScheduledGames = this.statsLeadersMapper.selectMaxTeamScheduledGames(Long.valueOf(tid), eventId, eventIds, years, gm);
        LinkedHashMap<String, Map> meta = new LinkedHashMap<String, Map>();
        meta.put("qualification", LeaderQualification.battingMeta((int)maxScheduledGames));
        return PageResult.of(rows.stream().map(arg_0 -> this.toBattingLeaderMap(arg_0)).collect(Collectors.toList()), (long)total, meta);
    }

    public PageResult<Map<String, Object>> getPitchingLeaders(Long eventId, List<Long> eventIds, List<String> years, List<Long> teamIds, String playerKeyword, String position, String homeAway, String batterHand, String pitcherHand, String gameMode, int page, int pageSize, String sortProp, String sortOrder) {
        return this.getPitchingLeaders(eventId, eventIds, years, teamIds, playerKeyword, position, homeAway, batterHand, pitcherHand, gameMode, page, pageSize, sortProp, sortOrder, true);
    }

    public PageResult<Map<String, Object>> getPitchingLeaders(Long eventId, List<Long> eventIds, List<String> years, List<Long> teamIds, String playerKeyword, String position, String homeAway, String batterHand, String pitcherHand, String gameMode, int page, int pageSize, String sortProp, String sortOrder, boolean applyQualification) {
        long tid = this.resolveTenantId();
        int p = StatsService.normalizePage((int)page);
        int ps = StatsService.normalizePageSize((int)pageSize);
        long offset = (long)(p - 1) * (long)ps;
        String kw = StatsService.normalizeLeaderKeyword((String)playerKeyword);
        String pos = StatsService.normalizeLeaderKeyword((String)position);
        String ha = StatsService.normalizeHomeAway((String)homeAway);
        String bh = StatsService.normalizeHand((String)batterHand);
        String ph = StatsService.normalizeHand((String)pitcherHand);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        String orderBy = StatsLeadersOrderBy.pitching((String)sortProp, (String)sortOrder);
        long total = this.statsLeadersMapper.countPitchingLeaders(Long.valueOf(tid), eventId, eventIds, years, teamIds, kw, pos, ha, bh, ph, gm, Boolean.valueOf(applyQualification));
        List rows = this.statsLeadersMapper.selectPitchingLeadersPage(Long.valueOf(tid), eventId, eventIds, years, teamIds, kw, pos, ha, bh, ph, gm, Boolean.valueOf(applyQualification), orderBy, ps, offset);
        int maxScheduledGames = this.statsLeadersMapper.selectMaxTeamScheduledGames(Long.valueOf(tid), eventId, eventIds, years, gm);
        LinkedHashMap<String, Map> meta = new LinkedHashMap<String, Map>();
        meta.put("qualification", LeaderQualification.pitchingMeta((int)maxScheduledGames));
        return PageResult.of(rows.stream().map(arg_0 -> this.toPitchingLeaderMap(arg_0)).collect(Collectors.toList()), (long)total, meta);
    }

    public PageResult<Map<String, Object>> getFieldingLeaders(Long eventId, List<Long> eventIds, List<String> years, List<Long> teamIds, String playerKeyword, String position, String homeAway, String batterHand, String pitcherHand, String gameMode, int page, int pageSize, String sortProp, String sortOrder) {
        long tid = this.resolveTenantId();
        int p = StatsService.normalizePage((int)page);
        int ps = StatsService.normalizePageSize((int)pageSize);
        long offset = (long)(p - 1) * (long)ps;
        String kw = StatsService.normalizeLeaderKeyword((String)playerKeyword);
        String pos = StatsService.normalizeLeaderKeyword((String)position);
        String ha = StatsService.normalizeHomeAway((String)homeAway);
        String bh = StatsService.normalizeHand((String)batterHand);
        String ph = StatsService.normalizeHand((String)pitcherHand);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        String orderBy = StatsLeadersOrderBy.fielding((String)sortProp, (String)sortOrder);
        long total = this.statsLeadersMapper.countFieldingLeaders(Long.valueOf(tid), eventId, eventIds, years, teamIds, kw, pos, ha, bh, ph, gm);
        List rows = this.statsLeadersMapper.selectFieldingLeadersPage(Long.valueOf(tid), eventId, eventIds, years, teamIds, kw, pos, ha, bh, ph, gm, orderBy, ps, offset);
        return PageResult.of(rows.stream().map(arg_0 -> this.toFieldingLeaderMap(arg_0)).collect(Collectors.toList()), (long)total);
    }

    public PageResult<Map<String, Object>> getTeamBattingLeaders(Long eventId, List<Long> eventIds, List<String> years, List<Long> teamIds, String homeAway, String gameMode, int page, int pageSize, String sortProp, String sortOrder) {
        long tid = this.resolveTenantId();
        int p = StatsService.normalizePage((int)page);
        int ps = StatsService.normalizePageSize((int)pageSize);
        long offset = (long)(p - 1) * (long)ps;
        String ha = StatsService.normalizeHomeAway((String)homeAway);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        String orderBy = StatsService.teamBattingOrderBy((String)sortProp, (String)sortOrder);
        long total = this.statsLeadersMapper.countTeamBattingLeaders(Long.valueOf(tid), eventId, eventIds, years, teamIds, ha, gm);
        List rows = this.statsLeadersMapper.selectTeamBattingLeadersPage(Long.valueOf(tid), eventId, eventIds, years, teamIds, ha, gm, orderBy, ps, offset);
        List out = rows.stream().map(arg_0 -> this.toTeamBattingLeaderMap(arg_0)).collect(Collectors.toList());
        return PageResult.of(out, (long)total);
    }

    public PageResult<Map<String, Object>> getTeamPitchingLeaders(Long eventId, List<Long> eventIds, List<String> years, List<Long> teamIds, String homeAway, String gameMode, int page, int pageSize, String sortProp, String sortOrder) {
        long tid = this.resolveTenantId();
        int p = StatsService.normalizePage((int)page);
        int ps = StatsService.normalizePageSize((int)pageSize);
        long offset = (long)(p - 1) * (long)ps;
        String ha = StatsService.normalizeHomeAway((String)homeAway);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        String orderBy = StatsService.teamPitchingOrderBy((String)sortProp, (String)sortOrder);
        long total = this.statsLeadersMapper.countTeamPitchingLeaders(Long.valueOf(tid), eventId, eventIds, years, teamIds, ha, gm);
        List rows = this.statsLeadersMapper.selectTeamPitchingLeadersPage(Long.valueOf(tid), eventId, eventIds, years, teamIds, ha, gm, orderBy, ps, offset);
        List out = rows.stream().map(arg_0 -> this.toTeamPitchingLeaderMap(arg_0)).collect(Collectors.toList());
        return PageResult.of(out, (long)total);
    }

    public PageResult<Map<String, Object>> getTeamFieldingLeaders(Long eventId, List<Long> eventIds, List<String> years, List<Long> teamIds, String homeAway, String gameMode, int page, int pageSize, String sortProp, String sortOrder) {
        long tid = this.resolveTenantId();
        int p = StatsService.normalizePage((int)page);
        int ps = StatsService.normalizePageSize((int)pageSize);
        long offset = (long)(p - 1) * (long)ps;
        String ha = StatsService.normalizeHomeAway((String)homeAway);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        String orderBy = StatsService.teamFieldingOrderBy((String)sortProp, (String)sortOrder);
        long total = this.statsLeadersMapper.countTeamFieldingLeaders(Long.valueOf(tid), eventId, eventIds, years, teamIds, ha, gm);
        List rows = this.statsLeadersMapper.selectTeamFieldingLeadersPage(Long.valueOf(tid), eventId, eventIds, years, teamIds, ha, gm, orderBy, ps, offset);
        List out = rows.stream().map(arg_0 -> this.toTeamFieldingLeaderMap(arg_0)).collect(Collectors.toList());
        return PageResult.of(out, (long)total);
    }

    public PageResult<Map<String, Object>> getStandings(Long eventId, List<Long> eventIds, List<String> years, String gameMode, int page, int pageSize) {
        int ps;
        long tid = this.resolveTenantId();
        String gm = StatsService.normalizeGameMode((String)gameMode);
        List teamList = this.teamService.listForSelect();
        if (teamList.isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        Set validTeamIds = teamList.stream().map(TeamOptionDto::id).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        List games = this.statsLeadersMapper.selectStandingGames(Long.valueOf(tid), eventId, eventIds, years, gm);
        List all = this.buildStandingsRows(games, teamList, validTeamIds);
        long total = all.size();
        if (total == 0L) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        int p = StatsService.normalizePage((int)page);
        int from = (p - 1) * (ps = StatsService.normalizePageSize((int)pageSize));
        if ((long)from >= total) {
            return PageResult.of((List)List.of(), (long)total);
        }
        int to = (int)Math.min((long)from + (long)ps, total);
        return PageResult.of(new ArrayList(all.subList(from, to)), (long)total);
    }

    private static int normalizeTopListLimit(Integer limit) {
        int n = limit == null ? 10 : limit;
        return Math.max(1, Math.min(20, n));
    }

    public Map<String, Object> getStarTopList(Long eventId, List<Long> eventIds, List<String> years, String gameMode, Integer limit, List<String> includeMetrics) {
        int topN = StatsService.normalizeTopListLimit((Integer)limit);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        Set includeSet = includeMetrics == null ? Set.of() : (Set)includeMetrics.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toCollection(LinkedHashSet::new));
        ArrayList metrics = new ArrayList();
        for (StarTopListMetricSpec spec : STAR_TOP_LIST_SPECS) {
            if (!includeSet.isEmpty() && !includeSet.contains(spec.key())) continue;
            PageResult pr = "batting".equals(spec.domain()) ? this.getBattingLeaders(eventId, eventIds, years, null, null, null, null, null, null, gm, 1, topN, spec.sortProp(), spec.sortOrder(), "avg".equals(spec.key())) : this.getPitchingLeaders(eventId, eventIds, years, null, null, null, null, null, null, gm, 1, topN, spec.sortProp(), spec.sortOrder(), "era".equals(spec.key()));
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("key", spec.key());
            item.put("title", spec.title());
            item.put("group", spec.group());
            item.put("valueLabel", spec.valueLabel());
            item.put("valueProp", spec.valueProp());
            item.put("sortProp", spec.sortProp());
            item.put("sortOrder", spec.sortOrder());
            item.put("rows", pr.getList() == null ? List.of() : pr.getList());
            metrics.add(item);
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("limit", topN);
        out.put("metrics", metrics);
        return out;
    }

    private static String blankSeasonToNull(String season) {
        if (season == null || season.isBlank()) {
            return null;
        }
        return season.trim();
    }

    private static int drillPageSize(Integer pageSize) {
        int ps = pageSize == null ? 20 : pageSize;
        return Math.min(100, Math.max(1, ps));
    }

    public PageResult<Map<String, Object>> drillDownBatting(Long playerId, String metric, Integer page, Integer pageSize, Long eventId, String season, String gameMode) {
        if (playerId == null) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        PlayerStatDrillBattingMetric m = PlayerStatDrillBattingMetric.fromApi((String)metric);
        if (m == null) {
            throw new BusinessException(400, "\u4e0d\u652f\u6301\u7684\u6253\u51fb\u6307\u6807: " + metric);
        }
        long tid = this.resolveTenantId();
        int p = StatsService.normalizePage((int)(page == null ? 1 : page));
        int ps = StatsService.drillPageSize((Integer)pageSize);
        long offset = (long)(p - 1) * (long)ps;
        String seasonNorm = StatsService.blankSeasonToNull((String)season);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        long total = this.playerStatsMapper.countBattingDrillDown(playerId, tid, eventId, seasonNorm, gm);
        List rows = total == 0L ? List.of() : this.playerStatsMapper.selectBattingDrillDownPage(playerId, tid, eventId, seasonNorm, m.getValueSql(), ps, offset, gm);
        return PageResult.of((List)rows, (long)total);
    }

    public PageResult<Map<String, Object>> drillDownPitching(Long playerId, String metric, Integer page, Integer pageSize, Long eventId, String season, String gameMode) {
        if (playerId == null) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        PlayerStatDrillPitchingMetric m = PlayerStatDrillPitchingMetric.fromApi((String)metric);
        if (m == null) {
            throw new BusinessException(400, "\u4e0d\u652f\u6301\u7684\u6295\u7403\u6307\u6807: " + metric);
        }
        long tid = this.resolveTenantId();
        int p = StatsService.normalizePage((int)(page == null ? 1 : page));
        int ps = StatsService.drillPageSize((Integer)pageSize);
        long offset = (long)(p - 1) * (long)ps;
        String seasonNorm = StatsService.blankSeasonToNull((String)season);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        long total = this.playerStatsMapper.countPitchingDrillDown(playerId, tid, eventId, seasonNorm, gm);
        List rows = total == 0L ? List.of() : this.playerStatsMapper.selectPitchingDrillDownPage(playerId, tid, eventId, seasonNorm, m.getValueSql(), ps, offset, gm);
        return PageResult.of((List)rows, (long)total);
    }

    public PageResult<Map<String, Object>> drillDownFielding(Long playerId, String metric, Integer page, Integer pageSize, Long eventId, String season, String gameMode) {
        if (playerId == null) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        PlayerStatDrillFieldingMetric m = PlayerStatDrillFieldingMetric.fromApi((String)metric);
        if (m == null) {
            throw new BusinessException(400, "\u4e0d\u652f\u6301\u7684\u5b88\u5907\u6307\u6807: " + metric);
        }
        long tid = this.resolveTenantId();
        int p = StatsService.normalizePage((int)(page == null ? 1 : page));
        int ps = StatsService.drillPageSize((Integer)pageSize);
        long offset = (long)(p - 1) * (long)ps;
        String seasonNorm = StatsService.blankSeasonToNull((String)season);
        String gm = StatsService.normalizeGameMode((String)gameMode);
        long total = this.playerStatsMapper.countFieldingDrillDown(playerId, tid, eventId, seasonNorm, gm);
        List rows = total == 0L ? List.of() : this.playerStatsMapper.selectFieldingDrillDownPage(playerId, tid, eventId, seasonNorm, m.getValueSql(), ps, offset, gm);
        return PageResult.of((List)rows, (long)total);
    }

    private Map<String, Object> toBattingLeaderMap(BattingLeaderDTO d) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("playerId", d.getPlayerId());
        m.put("playerName", d.getPlayerName());
        m.put("teamId", d.getTeamId());
        m.put("teamName", d.getTeamName());
        m.put("position", d.getPosition());
        m.put("number", d.getNumber());
        m.put("gp", d.getGp() != null ? d.getGp() : 0);
        m.put("pa", d.getPa() != null ? d.getPa() : 0);
        m.put("ab", d.getAb() != null ? d.getAb() : 0);
        m.put("r", d.getR() != null ? d.getR() : 0);
        m.put("rbi", d.getRbi() != null ? d.getRbi() : 0);
        m.put("h", d.getH() != null ? d.getH() : 0);
        m.put("singles", d.getSingles() != null ? d.getSingles() : 0);
        m.put("doubles", d.getDoubles() != null ? d.getDoubles() : 0);
        m.put("triples", d.getTriples() != null ? d.getTriples() : 0);
        m.put("hr", d.getHr() != null ? d.getHr() : 0);
        m.put("insideParkHr", d.getInsideParkHr() != null ? d.getInsideParkHr() : 0);
        m.put("tb", d.getTb() != null ? d.getTb() : 0);
        m.put("so", d.getSo() != null ? d.getSo() : 0);
        m.put("sb", d.getSb() != null ? d.getSb() : 0);
        m.put("gdp", d.getGdp() != null ? d.getGdp() : 0);
        m.put("sh", d.getSh() != null ? d.getSh() : 0);
        m.put("sf", d.getSf() != null ? d.getSf() : 0);
        m.put("bb", d.getBb() != null ? d.getBb() : 0);
        m.put("ibb", d.getIbb() != null ? d.getIbb() : 0);
        m.put("hbp", d.getHbp() != null ? d.getHbp() : 0);
        m.put("cs", d.getCs() != null ? d.getCs() : 0);
        m.put("avg", d.getAvg() != null ? StatsFormatUtil.fmtAvg((double)d.getAvg()) : "0");
        m.put("obp", d.getObp() != null ? StatsFormatUtil.fmtAvg((double)d.getObp()) : "0");
        m.put("slg", d.getSlg() != null ? StatsFormatUtil.fmtAvg((double)d.getSlg()) : "0");
        m.put("sbPct", d.getSbPct() != null ? StatsFormatUtil.fmtPct((Double)d.getSbPct()) : "0");
        m.put("ops", d.getAvg() != null && d.getObp() != null && d.getSlg() != null ? StatsFormatUtil.fmtDecimal((double)(d.getObp() + d.getSlg()), (int)3) : "0");
        m.put("ssi", 0);
        return m;
    }

    private Map<String, Object> toPitchingLeaderMap(PitchingLeaderDTO d) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("playerId", d.getPlayerId());
        m.put("playerName", d.getPlayerName());
        m.put("teamId", d.getTeamId());
        m.put("teamName", d.getTeamName());
        m.put("position", d.getPosition());
        m.put("number", d.getNumber());
        m.put("gp", d.getGp());
        m.put("gs", d.getGs());
        m.put("svo", d.getSvo());
        m.put("cg", d.getCg());
        m.put("pg", d.getPg());
        m.put("w", d.getW());
        m.put("l", d.getL());
        m.put("sv", d.getSv());
        m.put("hld", d.getHld());
        m.put("ip", d.getIp());
        m.put("whip", d.getWhip() != null ? StatsFormatUtil.fmtDecimal((double)d.getWhip(), (int)2) : "-");
        m.put("era", d.getEra() != null ? StatsFormatUtil.fmtDecimal((double)d.getEra(), (int)2) : "-");
        m.put("pitchPa", d.getPitchPa());
        m.put("pitchBf", d.getPitchBf());
        m.put("np", d.getNp());
        m.put("pitchH", d.getPitchH());
        m.put("pitchHr", d.getPitchHr());
        m.put("pitchInsideParkHr", d.getPitchInsideParkHr() != null ? d.getPitchInsideParkHr() : 0);
        m.put("pitchBb", d.getPitchBb());
        m.put("pitchIbb", d.getPitchIbb());
        m.put("pitchHbp", d.getPitchHbp());
        m.put("pitchSo", d.getPitchSo());
        m.put("wp", d.getWp());
        m.put("bk", d.getBk());
        m.put("pitchR", d.getPitchR());
        m.put("er", d.getEr());
        m.put("go", d.getGo());
        m.put("fo", d.getFo());
        m.put("goFo", d.getGoFo() != null ? StatsFormatUtil.fmtDecimal((double)d.getGoFo(), (int)2) : "-");
        return m;
    }

    private Map<String, Object> toFieldingLeaderMap(FieldingLeaderDTO d) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("playerId", d.getPlayerId());
        m.put("playerName", d.getPlayerName());
        m.put("teamId", d.getTeamId());
        m.put("teamName", d.getTeamName());
        m.put("position", d.getPosition());
        m.put("number", d.getNumber());
        m.put("gp", d.getGp());
        m.put("gs", d.getGs() != null ? d.getGs() : 0);
        m.put("inn", StatsFormatUtil.normalizeBaseballIp((double)(d.getInn() != null ? d.getInn() : 0.0)));
        m.put("tc", d.getTc());
        m.put("po", d.getPo());
        m.put("a", d.getA());
        m.put("e", d.getE());
        m.put("dp", d.getDp() != null ? d.getDp() : 0);
        m.put("tcPct", d.getTcPct() != null ? StatsFormatUtil.fmtDecimal((double)d.getTcPct(), (int)1) + "%" : "-");
        return m;
    }

    private Map<String, Object> toTeamBattingLeaderMap(Map<String, Object> d) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        long teamId = Optional.ofNullable(StatsService.getLong(d, (String)"teamId")).orElse(0L);
        String teamName = Optional.ofNullable(StatsService.strVal(d, (String)"teamName")).orElse("\u7403\u961f#" + teamId);
        Double avg = StatsService.getDouble(d, (String)"avg");
        Double obp = StatsService.getDouble(d, (String)"obp");
        Double slg = StatsService.getDouble(d, (String)"slg");
        m.put("teamId", teamId);
        m.put("teamName", teamName);
        m.put("gp", StatsService.getInt(d, (String)"gp"));
        m.put("pa", StatsService.getInt(d, (String)"pa"));
        m.put("ab", StatsService.getInt(d, (String)"ab"));
        m.put("r", StatsService.getInt(d, (String)"r"));
        m.put("h", StatsService.getInt(d, (String)"h"));
        m.put("rbi", StatsService.getInt(d, (String)"rbi"));
        m.put("doubles", StatsService.getInt(d, (String)"doubles"));
        m.put("triples", StatsService.getInt(d, (String)"triples"));
        m.put("hr", StatsService.getInt(d, (String)"hr"));
        m.put("insideParkHr", StatsService.getInt(d, (String)"insideParkHr"));
        m.put("sb", StatsService.getInt(d, (String)"sb"));
        m.put("cs", StatsService.getInt(d, (String)"cs"));
        m.put("so", StatsService.getInt(d, (String)"so"));
        m.put("bb", StatsService.getInt(d, (String)"bb"));
        m.put("hbp", StatsService.getInt(d, (String)"hbp"));
        m.put("tb", StatsService.getInt(d, (String)"tb"));
        m.put("avg", avg > 0.0 ? StatsFormatUtil.fmtAvg((double)avg) : "0");
        m.put("obp", obp > 0.0 ? StatsFormatUtil.fmtAvg((double)obp) : "0");
        m.put("slg", slg > 0.0 ? StatsFormatUtil.fmtAvg((double)slg) : "0");
        m.put("ops", obp > 0.0 || slg > 0.0 ? StatsFormatUtil.fmtDecimal((double)(obp + slg), (int)3) : "0");
        return m;
    }

    private Map<String, Object> toTeamPitchingLeaderMap(Map<String, Object> d) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        long teamId = Optional.ofNullable(StatsService.getLong(d, (String)"teamId")).orElse(0L);
        String teamName = Optional.ofNullable(StatsService.strVal(d, (String)"teamName")).orElse("\u7403\u961f#" + teamId);
        m.put("teamId", teamId);
        m.put("teamName", teamName);
        m.put("gp", StatsService.getInt(d, (String)"gp"));
        m.put("gs", StatsService.getInt(d, (String)"gs"));
        m.put("w", StatsService.getInt(d, (String)"w"));
        m.put("l", StatsService.getInt(d, (String)"l"));
        m.put("sv", StatsService.getInt(d, (String)"sv"));
        m.put("hld", StatsService.getInt(d, (String)"hld"));
        m.put("ip", StatsService.getDouble(d, (String)"ip"));
        double whip = StatsService.getDouble(d, (String)"whip");
        m.put("whip", whip > 0.0 ? StatsFormatUtil.fmtDecimal((double)whip, (int)2) : "-");
        double era = StatsService.getDouble(d, (String)"era");
        m.put("era", era > 0.0 ? StatsFormatUtil.fmtDecimal((double)era, (int)2) : "-");
        m.put("pitchBf", StatsService.getInt(d, (String)"pitchBf"));
        m.put("np", StatsService.getInt(d, (String)"np"));
        m.put("pitchH", StatsService.getInt(d, (String)"pitchH"));
        m.put("pitchHr", StatsService.getInt(d, (String)"pitchHr"));
        m.put("pitchInsideParkHr", StatsService.getInt(d, (String)"pitchInsideParkHr"));
        m.put("pitchBb", StatsService.getInt(d, (String)"pitchBb"));
        m.put("pitchHbp", StatsService.getInt(d, (String)"pitchHbp"));
        m.put("pitchSo", StatsService.getInt(d, (String)"pitchSo"));
        m.put("wp", StatsService.getInt(d, (String)"wp"));
        m.put("bk", StatsService.getInt(d, (String)"bk"));
        m.put("pitchR", StatsService.getInt(d, (String)"pitchR"));
        m.put("er", StatsService.getInt(d, (String)"er"));
        return m;
    }

    private Map<String, Object> toTeamFieldingLeaderMap(Map<String, Object> d) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        long teamId = Optional.ofNullable(StatsService.getLong(d, (String)"teamId")).orElse(0L);
        String teamName = Optional.ofNullable(StatsService.strVal(d, (String)"teamName")).orElse("\u7403\u961f#" + teamId);
        m.put("teamId", teamId);
        m.put("teamName", teamName);
        m.put("gp", StatsService.getInt(d, (String)"gp"));
        m.put("gs", StatsService.getInt(d, (String)"gs"));
        m.put("inn", StatsFormatUtil.normalizeBaseballIp((double)StatsService.getDouble(d, (String)"inn")));
        m.put("tc", StatsService.getInt(d, (String)"tc"));
        m.put("po", StatsService.getInt(d, (String)"po"));
        m.put("a", StatsService.getInt(d, (String)"a"));
        m.put("e", StatsService.getInt(d, (String)"e"));
        m.put("dp", StatsService.getInt(d, (String)"dp"));
        m.put("pb", StatsService.getInt(d, (String)"pb"));
        m.put("catcherCs", StatsService.getInt(d, (String)"catcherCs"));
        m.put("tcPct", StatsFormatUtil.fmtDecimal((double)StatsService.getDouble(d, (String)"tcPct"), (int)1) + "%");
        return m;
    }

    private static String standingsGameSortKey(StandingGameRowDTO g) {
        String day = g.getGameday() == null ? "" : g.getGameday().trim();
        long id = g.getId() == null ? 0L : g.getId();
        return day + "\t" + String.format(Locale.ROOT, "%012d", id);
    }

    private static void pushStandLine(Map<Long, List<StandLine>> teamGames, long teamId, StandLine line) {
        teamGames.computeIfAbsent(teamId, k -> new ArrayList()).add(line);
    }

    private List<Map<String, Object>> buildStandingsRows(List<StandingGameRowDTO> games, List<TeamOptionDto> teamList, Set<Long> validTeamIds) {
        HashMap<Long, int[]> wl = new HashMap<Long, int[]>();
        HashMap teamGames = new HashMap();
        Map teamNameById = teamList.stream().filter(t -> t.id() != null && validTeamIds.contains(t.id())).collect(Collectors.toMap(TeamOptionDto::id, TeamOptionDto::name, (a, b) -> a, LinkedHashMap::new));
        for (StandingGameRowDTO standingGameRowDTO : games) {
            int asv;
            Long l = standingGameRowDTO.getHomeTeamId();
            Long l2 = standingGameRowDTO.getAwayTeamId();
            if (l == null || l2 == null || !validTeamIds.contains(l) || !validTeamIds.contains(l2)) continue;
            Integer hs = standingGameRowDTO.getHomeScore();
            Integer as = standingGameRowDTO.getAwayScore();
            if (hs == null || as == null) continue;
            String sk = StatsService.standingsGameSortKey((StandingGameRowDTO)standingGameRowDTO);
            int hsv = hs;
            if (hsv > (asv = as.intValue())) {
                int[] nArray = wl.computeIfAbsent(l, k -> new int[3]);
                nArray[0] = nArray[0] + 1;
                int[] nArray2 = wl.computeIfAbsent(l2, k -> new int[3]);
                nArray2[2] = nArray2[2] + 1;
                StatsService.pushStandLine(teamGames, (long)l, (StandLine)new StandLine(sk, 'W', hsv, asv));
                StatsService.pushStandLine(teamGames, (long)l2, (StandLine)new StandLine(sk, 'L', asv, hsv));
                continue;
            }
            if (hsv < asv) {
                int[] nArray = wl.computeIfAbsent(l2, k -> new int[3]);
                nArray[0] = nArray[0] + 1;
                int[] nArray3 = wl.computeIfAbsent(l, k -> new int[3]);
                nArray3[2] = nArray3[2] + 1;
                StatsService.pushStandLine(teamGames, (long)l2, (StandLine)new StandLine(sk, 'W', asv, hsv));
                StatsService.pushStandLine(teamGames, (long)l, (StandLine)new StandLine(sk, 'L', hsv, asv));
                continue;
            }
            int[] nArray = wl.computeIfAbsent(l, k -> new int[3]);
            nArray[1] = nArray[1] + 1;
            int[] nArray4 = wl.computeIfAbsent(l2, k -> new int[3]);
            nArray4[1] = nArray4[1] + 1;
            StatsService.pushStandLine(teamGames, (long)l, (StandLine)new StandLine(sk, 'D', hsv, asv));
            StatsService.pushStandLine(teamGames, (long)l2, (StandLine)new StandLine(sk, 'D', asv, hsv));
        }
        ArrayList<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map.Entry entry : wl.entrySet()) {
            Long l = (Long)entry.getKey();
            int[] r = (int[])entry.getValue();
            int w = r[0];
            int d = r[1];
            int l3 = r[2];
            int gp = w + d + l3;
            int points = w * 2 + d;
            double winPct = gp > 0 ? (double)w / (double)gp : 0.0;
            ArrayList<StandLine> lines = new ArrayList<StandLine>(teamGames.getOrDefault(l, List.of()));
            lines.sort(Comparator.comparing(StandLine::sortKey));
            int rf = 0;
            int ra = 0;
            for (StandLine ln : lines) {
                rf += ln.rf();
                ra += ln.ra();
            }
            int runDiff = rf - ra;
            int lastN = Math.min(10, lines.size());
            int recentW = 0;
            int recentD = 0;
            int recentL = 0;
            for (int i = Math.max(0, lines.size() - lastN); i < lines.size(); ++i) {
                char o = ((StandLine)lines.get(i)).outcome();
                if (o == 'W') {
                    ++recentW;
                    continue;
                }
                if (o == 'L') {
                    ++recentL;
                    continue;
                }
                ++recentD;
            }
            String streakType = "none";
            int streakCount = 0;
            if (!lines.isEmpty()) {
                char tail = ((StandLine)lines.get(lines.size() - 1)).outcome();
                streakType = String.valueOf(tail);
                for (int i = lines.size() - 1; i >= 0 && ((StandLine)lines.get(i)).outcome() == tail; --i) {
                    ++streakCount;
                }
            }
            LinkedHashMap<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("teamId", l);
            row.put("teamName", teamNameById.getOrDefault(l, "\u7403\u961f#" + l));
            row.put("win", w);
            row.put("draw", d);
            row.put("loss", l3);
            row.put("points", points);
            row.put("winPct", winPct);
            row.put("gb", 0.0);
            row.put("runDiff", runDiff);
            row.put("streakType", streakType);
            row.put("streakCount", streakCount);
            row.put("recentW", recentW);
            row.put("recentD", recentD);
            row.put("recentL", recentL);
            row.put("recentN", lastN);
            rows.add(row);
        }
        rows.sort((a, b) -> {
            String nb;
            int rdb;
            int lb;
            int pb = ((Number)b.get("points")).intValue();
            int pa = ((Number)a.get("points")).intValue();
            if (pa != pb) {
                return Integer.compare(pb, pa);
            }
            int wb = ((Number)b.get("win")).intValue();
            int wa = ((Number)a.get("win")).intValue();
            if (wa != wb) {
                return Integer.compare(wb, wa);
            }
            int la = ((Number)a.get("loss")).intValue();
            if (la != (lb = ((Number)b.get("loss")).intValue())) {
                return Integer.compare(la, lb);
            }
            int rda = ((Number)a.get("runDiff")).intValue();
            if (rda != (rdb = ((Number)b.get("runDiff")).intValue())) {
                return Integer.compare(rdb, rda);
            }
            String na = String.valueOf(a.get("teamName"));
            int nameCmp = na.compareTo(nb = String.valueOf(b.get("teamName")));
            if (nameCmp != 0) {
                return nameCmp;
            }
            return Long.compare(((Number)a.get("teamId")).longValue(), ((Number)b.get("teamId")).longValue());
        });
        if (!rows.isEmpty()) {
            int n = ((Number)((Map)rows.get(0)).get("points")).intValue();
            for (Map map : rows) {
                int pts = ((Number)map.get("points")).intValue();
                map.put("gb", (double)(n - pts) / 2.0);
            }
        }
        return rows;
    }

    @Generated
    public StatsService(PlayerStatsMapper playerStatsMapper, StatsLeadersMapper statsLeadersMapper, TenantProperties tenantProperties, TeamService teamService) {
        this.playerStatsMapper = playerStatsMapper;
        this.statsLeadersMapper = statsLeadersMapper;
        this.tenantProperties = tenantProperties;
        this.teamService = teamService;
    }
}

