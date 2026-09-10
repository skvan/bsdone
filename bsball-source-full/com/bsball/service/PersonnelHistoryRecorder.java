/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.Coach
 *  com.bsball.model.entity.HistoryRecord
 *  com.bsball.model.entity.League
 *  com.bsball.model.entity.Player
 *  com.bsball.model.entity.Team
 *  com.bsball.repository.HistoryRecordRepository
 *  com.bsball.service.PersonnelHistoryRecorder
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.model.entity.Coach;
import com.bsball.model.entity.HistoryRecord;
import com.bsball.model.entity.League;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.Team;
import com.bsball.repository.HistoryRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class PersonnelHistoryRecorder {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(PersonnelHistoryRecorder.class);
    private static final String ACTIVE = "active";
    private static final String AUTO_REMARK = "\u7cfb\u7edf\u81ea\u52a8\u8bb0\u5f55";
    private final HistoryRecordRepository historyRecordRepository;
    private final ObjectMapper objectMapper;

    private static Long normTeam(Long id) {
        return id != null && id > 0L ? id : null;
    }

    private static String today() {
        return LocalDate.now().toString();
    }

    private static boolean isActiveStatus(String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return "active".equalsIgnoreCase(status.trim());
    }

    private void persist(HistoryRecord r) {
        try {
            this.historyRecordRepository.save((Object)r);
            if (log.isDebugEnabled()) {
                log.debug("\u6cbf\u9769\u81ea\u52a8\u8bb0\u5f55\u5df2\u5199\u5165: type={} targetType={} targetId={}", new Object[]{r.getType(), r.getTargetType(), r.getTargetId()});
            }
        }
        catch (Exception e) {
            log.warn("\u6cbf\u9769\u81ea\u52a8\u8bb0\u5f55\u5199\u5165\u5931\u8d25: type={} targetType={} targetId={} \u2014 {}", new Object[]{r.getType(), r.getTargetType(), r.getTargetId(), e.toString(), e});
        }
    }

    private String jsonPayload(Map<String, Object> map) {
        try {
            return this.objectMapper.writeValueAsString(map);
        }
        catch (Exception e) {
            return null;
        }
    }

    private HistoryRecord baseEvent(String targetType, long targetId, long tenantId, String type) {
        HistoryRecord r = new HistoryRecord();
        r.setRecordType("event");
        r.setTargetType(targetType);
        r.setTargetId(Long.valueOf(targetId));
        r.setTenantId(Long.valueOf(tenantId));
        r.setType(type);
        r.setChangeDate(PersonnelHistoryRecorder.today());
        r.setRemark("\u7cfb\u7edf\u81ea\u52a8\u8bb0\u5f55");
        return r;
    }

    public void afterPlayerCreate(Player saved) {
        if (saved == null || saved.getId() == null) {
            return;
        }
        Long tid = saved.getTenantId();
        if (tid == null) {
            return;
        }
        Long team = PersonnelHistoryRecorder.normTeam((Long)saved.getTeamId());
        if (team != null) {
            HistoryRecord r = this.baseEvent("player", saved.getId().longValue(), tid.longValue(), "join");
            r.setRelatedObjectType("team");
            r.setRelatedObjectId(team);
            this.persist(r);
        }
    }

    public void afterPlayerUpdate(Player before, Player after) {
        LinkedHashMap<String, Long> payload;
        HistoryRecord r;
        if (before == null || after == null || after.getId() == null || after.getTenantId() == null) {
            return;
        }
        long tid = after.getTenantId();
        long pid = after.getId();
        Long oldTeam = PersonnelHistoryRecorder.normTeam((Long)before.getTeamId());
        Long newTeam = PersonnelHistoryRecorder.normTeam((Long)after.getTeamId());
        boolean wasActive = PersonnelHistoryRecorder.isActiveStatus((String)before.getStatus());
        boolean nowActive = PersonnelHistoryRecorder.isActiveStatus((String)after.getStatus());
        if (wasActive && !nowActive) {
            r = this.baseEvent("player", pid, tid, "retire");
            if (newTeam != null) {
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(newTeam);
            } else if (oldTeam != null) {
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(oldTeam);
            }
            this.persist(r);
        }
        if (!Objects.equals(oldTeam, newTeam)) {
            if (oldTeam == null && newTeam != null) {
                r = this.baseEvent("player", pid, tid, "join");
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(newTeam);
                this.persist(r);
            } else if (oldTeam != null && newTeam == null) {
                r = this.baseEvent("player", pid, tid, "leave");
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(oldTeam);
                this.persist(r);
            } else if (oldTeam != null) {
                r = this.baseEvent("player", pid, tid, "transfer");
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(newTeam);
                payload = new LinkedHashMap<String, Long>();
                payload.put("fromTeamId", oldTeam);
                payload.put("toTeamId", newTeam);
                r.setChangePayloadJson(this.jsonPayload(payload));
                this.persist(r);
            }
        }
        if (PersonnelHistoryRecorder.playerProfileFieldsChanged((Player)before, (Player)after)) {
            r = this.baseEvent("player", pid, tid, "profile_update");
            payload = PersonnelHistoryRecorder.playerProfilePayload((Player)before, (Player)after);
            r.setChangePayloadJson(this.jsonPayload((Map)payload));
            this.persist(r);
        }
    }

    private static boolean playerProfileFieldsChanged(Player a, Player b) {
        return !Objects.equals(PersonnelHistoryRecorder.str((String)a.getName()), PersonnelHistoryRecorder.str((String)b.getName())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getShortName()), PersonnelHistoryRecorder.str((String)b.getShortName())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getNameEn()), PersonnelHistoryRecorder.str((String)b.getNameEn())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getNickname()), PersonnelHistoryRecorder.str((String)b.getNickname())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getNumber()), PersonnelHistoryRecorder.str((String)b.getNumber()));
    }

    private static String str(String s) {
        return s == null ? null : s.trim();
    }

    private static Map<String, Object> playerProfilePayload(Player before, Player after) {
        LinkedHashMap beforeMap = new LinkedHashMap();
        LinkedHashMap afterMap = new LinkedHashMap();
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"name", (String)before.getName(), (String)after.getName());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"shortName", (String)before.getShortName(), (String)after.getShortName());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"nameEn", (String)before.getNameEn(), (String)after.getNameEn());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"nickname", (String)before.getNickname(), (String)after.getNickname());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"number", (String)before.getNumber(), (String)after.getNumber());
        return PersonnelHistoryRecorder.wrapProfilePayload(beforeMap, afterMap, (String[])new String[]{"name", "shortName", "nameEn", "nickname", "number"});
    }

    private static void putFullProfileField(Map<String, Object> beforeMap, Map<String, Object> afterMap, String key, String a, String b) {
        beforeMap.put(key, PersonnelHistoryRecorder.str((String)a));
        afterMap.put(key, PersonnelHistoryRecorder.str((String)b));
    }

    private static List<String> changedFieldKeys(Map<String, Object> beforeMap, Map<String, Object> afterMap, String ... keysInOrder) {
        ArrayList<String> fields = new ArrayList<String>();
        for (String key : keysInOrder) {
            if (Objects.equals(beforeMap.get(key), afterMap.get(key))) continue;
            fields.add(key);
        }
        return fields;
    }

    private static Map<String, Object> wrapProfilePayload(Map<String, Object> beforeMap, Map<String, Object> afterMap, String ... keyOrder) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("changedFields", PersonnelHistoryRecorder.changedFieldKeys(beforeMap, afterMap, (String[])keyOrder));
        out.put("before", beforeMap);
        out.put("after", afterMap);
        return out;
    }

    public void afterCoachCreate(Coach saved) {
        if (saved == null || saved.getId() == null || saved.getTenantId() == null) {
            return;
        }
        Long team = PersonnelHistoryRecorder.normTeam((Long)saved.getTeamId());
        if (team != null) {
            HistoryRecord r = this.baseEvent("coach", saved.getId().longValue(), saved.getTenantId().longValue(), "join");
            r.setRelatedObjectType("team");
            r.setRelatedObjectId(team);
            this.persist(r);
        }
    }

    public void afterCoachUpdate(Coach before, Coach after) {
        Map<String, Long> payload;
        HistoryRecord r;
        if (before == null || after == null || after.getId() == null || after.getTenantId() == null) {
            return;
        }
        long tid = after.getTenantId();
        long cid = after.getId();
        Long oldTeam = PersonnelHistoryRecorder.normTeam((Long)before.getTeamId());
        Long newTeam = PersonnelHistoryRecorder.normTeam((Long)after.getTeamId());
        if (!Objects.equals(PersonnelHistoryRecorder.str((String)before.getPosition()), PersonnelHistoryRecorder.str((String)after.getPosition()))) {
            r = this.baseEvent("coach", cid, tid, "appointment");
            if (newTeam != null) {
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(newTeam);
            } else if (oldTeam != null) {
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(oldTeam);
            }
            payload = new LinkedHashMap<String, String>();
            payload.put("positionBefore", (Long)((Object)PersonnelHistoryRecorder.str((String)before.getPosition())));
            payload.put("positionAfter", (Long)((Object)PersonnelHistoryRecorder.str((String)after.getPosition())));
            r.setChangePayloadJson(this.jsonPayload(payload));
            this.persist(r);
        }
        if (!Objects.equals(oldTeam, newTeam)) {
            if (oldTeam == null && newTeam != null) {
                r = this.baseEvent("coach", cid, tid, "join");
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(newTeam);
                this.persist(r);
            } else if (oldTeam != null && newTeam == null) {
                r = this.baseEvent("coach", cid, tid, "leave");
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(oldTeam);
                this.persist(r);
            } else if (oldTeam != null) {
                r = this.baseEvent("coach", cid, tid, "transfer");
                r.setRelatedObjectType("team");
                r.setRelatedObjectId(newTeam);
                payload = new LinkedHashMap();
                payload.put("fromTeamId", oldTeam);
                payload.put("toTeamId", newTeam);
                r.setChangePayloadJson(this.jsonPayload(payload));
                this.persist(r);
            }
        }
        if (PersonnelHistoryRecorder.coachProfileFieldsChanged((Coach)before, (Coach)after)) {
            r = this.baseEvent("coach", cid, tid, "profile_update");
            payload = PersonnelHistoryRecorder.coachProfilePayload((Coach)before, (Coach)after);
            r.setChangePayloadJson(this.jsonPayload(payload));
            this.persist(r);
        }
    }

    private static boolean coachProfileFieldsChanged(Coach a, Coach b) {
        return !Objects.equals(PersonnelHistoryRecorder.str((String)a.getName()), PersonnelHistoryRecorder.str((String)b.getName())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getPhoto()), PersonnelHistoryRecorder.str((String)b.getPhoto())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getContactPhone()), PersonnelHistoryRecorder.str((String)b.getContactPhone())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getContactEmail()), PersonnelHistoryRecorder.str((String)b.getContactEmail()));
    }

    private static Map<String, Object> coachProfilePayload(Coach before, Coach after) {
        LinkedHashMap beforeMap = new LinkedHashMap();
        LinkedHashMap afterMap = new LinkedHashMap();
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"name", (String)before.getName(), (String)after.getName());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"photo", (String)before.getPhoto(), (String)after.getPhoto());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"contactPhone", (String)before.getContactPhone(), (String)after.getContactPhone());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"contactEmail", (String)before.getContactEmail(), (String)after.getContactEmail());
        return PersonnelHistoryRecorder.wrapProfilePayload(beforeMap, afterMap, (String[])new String[]{"name", "photo", "contactPhone", "contactEmail"});
    }

    public void afterTeamUpdate(Team before, Team after) {
        if (before == null || after == null || after.getId() == null || after.getTenantId() == null) {
            return;
        }
        if (!PersonnelHistoryRecorder.teamProfileFieldsChanged((Team)before, (Team)after)) {
            return;
        }
        HistoryRecord r = this.baseEvent("team", after.getId().longValue(), after.getTenantId().longValue(), "profile_update");
        Map payload = PersonnelHistoryRecorder.teamProfilePayload((Team)before, (Team)after);
        r.setChangePayloadJson(this.jsonPayload(payload));
        this.persist(r);
    }

    private static boolean teamProfileFieldsChanged(Team a, Team b) {
        return !Objects.equals(PersonnelHistoryRecorder.str((String)a.getName()), PersonnelHistoryRecorder.str((String)b.getName())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getNameEn()), PersonnelHistoryRecorder.str((String)b.getNameEn())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getShortName()), PersonnelHistoryRecorder.str((String)b.getShortName())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getLogo()), PersonnelHistoryRecorder.str((String)b.getLogo()));
    }

    private static Map<String, Object> teamProfilePayload(Team before, Team after) {
        LinkedHashMap beforeMap = new LinkedHashMap();
        LinkedHashMap afterMap = new LinkedHashMap();
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"name", (String)before.getName(), (String)after.getName());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"nameEn", (String)before.getNameEn(), (String)after.getNameEn());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"shortName", (String)before.getShortName(), (String)after.getShortName());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"logo", (String)before.getLogo(), (String)after.getLogo());
        return PersonnelHistoryRecorder.wrapProfilePayload(beforeMap, afterMap, (String[])new String[]{"name", "nameEn", "shortName", "logo"});
    }

    public void afterLeagueUpdate(League before, League after) {
        if (before == null || after == null || after.getId() == null || after.getTenantId() == null) {
            return;
        }
        if (!PersonnelHistoryRecorder.leagueProfileFieldsChanged((League)before, (League)after)) {
            return;
        }
        HistoryRecord r = this.baseEvent("league", after.getId().longValue(), after.getTenantId().longValue(), "profile_update");
        Map payload = PersonnelHistoryRecorder.leagueProfilePayload((League)before, (League)after);
        r.setChangePayloadJson(this.jsonPayload(payload));
        this.persist(r);
    }

    private static boolean leagueProfileFieldsChanged(League a, League b) {
        return !Objects.equals(PersonnelHistoryRecorder.str((String)a.getName()), PersonnelHistoryRecorder.str((String)b.getName())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getNameEn()), PersonnelHistoryRecorder.str((String)b.getNameEn())) || !Objects.equals(PersonnelHistoryRecorder.str((String)a.getLogo()), PersonnelHistoryRecorder.str((String)b.getLogo()));
    }

    private static Map<String, Object> leagueProfilePayload(League before, League after) {
        LinkedHashMap beforeMap = new LinkedHashMap();
        LinkedHashMap afterMap = new LinkedHashMap();
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"name", (String)before.getName(), (String)after.getName());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"nameEn", (String)before.getNameEn(), (String)after.getNameEn());
        PersonnelHistoryRecorder.putFullProfileField(beforeMap, afterMap, (String)"logo", (String)before.getLogo(), (String)after.getLogo());
        return PersonnelHistoryRecorder.wrapProfilePayload(beforeMap, afterMap, (String[])new String[]{"name", "nameEn", "logo"});
    }

    public static League snapshotLeague(League src) {
        if (src == null) {
            return null;
        }
        League c = new League();
        c.setId(src.getId());
        c.setTenantId(src.getTenantId());
        c.setName(src.getName());
        c.setNameEn(src.getNameEn());
        c.setLogo(src.getLogo());
        return c;
    }

    public static Team snapshotTeam(Team src) {
        if (src == null) {
            return null;
        }
        Team c = new Team();
        c.setId(src.getId());
        c.setTenantId(src.getTenantId());
        c.setLeagueId(src.getLeagueId());
        c.setName(src.getName());
        c.setNameEn(src.getNameEn());
        c.setShortName(src.getShortName());
        c.setLogo(src.getLogo());
        return c;
    }

    public static Player snapshotPlayer(Player src) {
        if (src == null) {
            return null;
        }
        Player c = new Player();
        c.setId(src.getId());
        c.setTenantId(src.getTenantId());
        c.setTeamId(src.getTeamId());
        c.setStatus(src.getStatus());
        c.setName(src.getName());
        c.setShortName(src.getShortName());
        c.setNameEn(src.getNameEn());
        c.setNickname(src.getNickname());
        c.setNumber(src.getNumber());
        return c;
    }

    public static Coach snapshotCoach(Coach src) {
        if (src == null) {
            return null;
        }
        Coach c = new Coach();
        c.setId(src.getId());
        c.setTenantId(src.getTenantId());
        c.setTeamId(src.getTeamId());
        c.setPosition(src.getPosition());
        c.setName(src.getName());
        c.setPhoto(src.getPhoto());
        c.setContactPhone(src.getContactPhone());
        c.setContactEmail(src.getContactEmail());
        return c;
    }

    @Generated
    public PersonnelHistoryRecorder(HistoryRecordRepository historyRecordRepository, ObjectMapper objectMapper) {
        this.historyRecordRepository = historyRecordRepository;
        this.objectMapper = objectMapper;
    }
}

