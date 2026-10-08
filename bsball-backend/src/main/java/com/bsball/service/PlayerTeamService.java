package com.bsball.service;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.PlayerTeamEntryDto;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 球员-球队经历服务：负责多队注册的校验、差异落库（简历式多段）、
 * 球员镜像字段（teamId/number/positions = 主注册）回写与接口输出填充。
 */
@Service
public class PlayerTeamService {
    private final PlayerTeamRepository playerTeamRepository;
    private final TeamRepository teamRepository;

    public PlayerTeamService(
            PlayerTeamRepository playerTeamRepository,
            TeamRepository teamRepository) {
        this.playerTeamRepository = playerTeamRepository;
        this.teamRepository = teamRepository;
    }

    /** 同步计划：待新增/更新/软删的经历 + 主注册 + 变更前后“当前球队”集合 */
    public record PlayerTeamSyncPlan(
            List<PlayerTeam> toCreate,
            List<PlayerTeam> toUpdate,
            List<PlayerTeam> toDelete,
            PlayerTeam primary,
            Set<Long> beforeCurrentTeamIds,
            Set<Long> afterCurrentTeamIds,
            List<PlayerTeam> desiredEntries) {
    }

    /** 计算保存计划（不落库）：entriesProvided=true 时全量替换，false 时按旧字段兼容语义。 */
    public PlayerTeamSyncPlan plan(Player player, List<PlayerTeamEntryDto> incoming, boolean entriesProvided) {
        List<PlayerTeam> existing = player.getId() == null
                ? List.of()
                : this.playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(player.getId());
        Set<Long> before = PlayerTeamService.currentTeamIdsOf(existing);
        List<PlayerTeam> toCreate = new ArrayList<>();
        List<PlayerTeam> toUpdate = new ArrayList<>();
        List<PlayerTeam> toDelete = new ArrayList<>();
        List<PlayerTeam> desired;
        if (entriesProvided) {
            desired = this.buildFromIncoming(player, incoming == null ? List.of() : incoming, existing, toCreate,
                    toUpdate, toDelete);
        } else {
            desired = this.buildFromLegacy(player, existing, toCreate, toUpdate);
        }
        PlayerTeam primary = PlayerTeamService.pickPrimary(desired);
        return new PlayerTeamSyncPlan(toCreate, toUpdate, toDelete, primary, before,
                PlayerTeamService.currentTeamIdsOf(desired), desired);
    }

    /** 将主注册回写为球员镜像字段（teamId/number/positions）。 */
    public void applyMirror(Player player, PlayerTeamSyncPlan plan) {
        PlayerTeam primary = plan.primary();
        if (primary == null) {
            player.setTeamId(null);
            player.setNumber(null);
            player.setPositions(null);
            return;
        }
        player.setTeamId(primary.getTeamId());
        player.setNumber(primary.getNumber());
        player.setPositions(primary.getPositions());
    }

    /** 落库保存计划（需球员已获得 ID）。 */
    public void persistPlan(Long playerId, PlayerTeamSyncPlan plan) {
        Long uid = CurrentUserHolder.get();
        LocalDateTime now = LocalDateTime.now();
        for (PlayerTeam row : plan.toCreate()) {
            row.setPlayerId(playerId);
            this.playerTeamRepository.save(row);
        }
        for (PlayerTeam row : plan.toUpdate()) {
            this.playerTeamRepository.save(row);
        }
        for (PlayerTeam row : plan.toDelete()) {
            row.setDeletedAt(now);
            row.setDeletedBy(uid);
            this.playerTeamRepository.save(row);
        }
    }

    /** 导入等旧路径：按球员镜像字段确保一条“当前球队”经历（不记录演进事件）。 */
    public void syncLegacyEntry(Player player) {
        if (player == null || player.getId() == null) {
            return;
        }
        PlayerTeamSyncPlan syncPlan = this.plan(player, null, false);
        this.persistPlan(player.getId(), syncPlan);
    }

    /** 填充单个球员的 teamEntries 输出字段。 */
    public void attachEntries(Player player) {
        if (player == null) {
            return;
        }
        this.attachEntries(List.of(player));
    }

    /** 批量填充多个球员的 teamEntries 输出字段。 */
    public void attachEntries(List<Player> players) {
        if (players == null || players.isEmpty()) {
            return;
        }
        List<Long> ids = players.stream().map(Player::getId).filter(Objects::nonNull).distinct().toList();
        Map<Long, List<PlayerTeam>> byPlayer = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            for (PlayerTeam e : this.playerTeamRepository.findByPlayerIdInAndDeletedAtIsNullOrderBySortAscIdAsc(ids)) {
                if (e.getPlayerId() == null) {
                    continue;
                }
                byPlayer.computeIfAbsent(e.getPlayerId(), k -> new ArrayList<>()).add(e);
            }
        }
        Map<Long, String> teamNames = new HashMap<>();
        if (!byPlayer.isEmpty()) {
            LinkedHashSet<Long> teamIds = new LinkedHashSet<>();
            for (List<PlayerTeam> rows : byPlayer.values()) {
                for (PlayerTeam e : rows) {
                    teamIds.add(e.getTeamId());
                }
            }
            if (!teamIds.isEmpty()) {
                for (Team team : this.teamRepository.findAllById(teamIds)) {
                    teamNames.put(team.getId(), team.getName());
                }
            }
        }
        for (Player p : players) {
            if (p == null) {
                continue;
            }
            List<PlayerTeam> rows = p.getId() == null ? List.of() : byPlayer.getOrDefault(p.getId(), List.of());
            List<PlayerTeam> sorted = sortedBySortThenId(rows);
            ArrayList<PlayerTeamEntryDto> out = new ArrayList<>(sorted.size());
            for (PlayerTeam e : sorted) {
                out.add(new PlayerTeamEntryDto(e.getId(), e.getTeamId(), teamNames.get(e.getTeamId()), e.getNumber(),
                        e.getPositionsList(), Boolean.TRUE.equals(e.getCurrent()), e.getSort()));
            }
            p.setTeamEntries(out);
        }
    }

    /** 球员当前是否注册在该球队（用于认领等校验）。 */
    public boolean isCurrentlyInTeam(Long playerId, Long teamId) {
        if (playerId == null || teamId == null) {
            return false;
        }
        return this.playerTeamRepository.countCurrentEntry(playerId, teamId) > 0L;
    }

    /** 球员的“当前球队”ID 集合（按经历排序）。 */
    public Set<Long> currentTeamIds(Long playerId) {
        if (playerId == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(this.playerTeamRepository.findCurrentTeamIdsByPlayerId(playerId));
    }

    /** 球员是否在任一给定球队中处于“当前球队”。 */
    public long countCurrentEntriesInTeams(Long playerId, Collection<Long> teamIds) {
        if (playerId == null || teamIds == null || teamIds.isEmpty()) {
            return 0L;
        }
        return this.playerTeamRepository.countCurrentEntryInTeams(playerId, teamIds);
    }

    /** 批量取“球员 -> 当前球队ID集合”映射（不含无当前经历的球员）。 */
    public Map<Long, Set<Long>> currentTeamIdsByPlayerIds(Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = playerIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<Long, Set<Long>> out = new LinkedHashMap<>();
        for (PlayerTeam e : this.playerTeamRepository.findByPlayerIdInAndDeletedAtIsNullAndCurrentTrue(ids)) {
            if (e.getPlayerId() == null) {
                continue;
            }
            out.computeIfAbsent(e.getPlayerId(), k -> new LinkedHashSet<>()).add(e.getTeamId());
        }
        return out;
    }

    private List<PlayerTeam> buildFromIncoming(Player player, List<PlayerTeamEntryDto> incoming,
            List<PlayerTeam> existing, List<PlayerTeam> toCreate, List<PlayerTeam> toUpdate,
            List<PlayerTeam> toDelete) {
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        for (PlayerTeamEntryDto dto : incoming) {
            Long teamId = dto.teamId();
            if (teamId == null || teamId <= 0L) {
                throw new BusinessException(400, "球队经历缺少有效球队 ID（teamId）");
            }
            if (!seen.add(teamId)) {
                throw new BusinessException(400, "球队经历存在重复球队：ID " + teamId);
            }
        }
        Map<Long, PlayerTeam> existingByTeam = new LinkedHashMap<>();
        for (PlayerTeam e : existing) {
            existingByTeam.put(e.getTeamId(), e);
        }
        List<PlayerTeam> desired = new ArrayList<>(incoming.size());
        for (PlayerTeamEntryDto dto : incoming) {
            Long teamId = dto.teamId();
            this.validateTeam(player, teamId);
            Boolean newCurrent = dto.current() != null && dto.current();
            String newNumber = PlayerTeamService.normalizeNumber(dto.number());
            List<String> newPositions = dto.positions() == null ? List.of() : dto.positions();
            PlayerTeam row = existingByTeam.get(teamId);
            if (row == null) {
                boolean revived = false;
                row = this.findRevivable(player, teamId);
                if (row != null) {
                    revived = true;
                    row.setDeletedAt(null);
                    row.setDeletedBy(null);
                } else {
                    row = new PlayerTeam();
                }
                this.applyFields(row, player, teamId, newNumber, newPositions, newCurrent, dto.sort());
                if (revived) {
                    toUpdate.add(row);
                } else {
                    toCreate.add(row);
                }
            } else {
                boolean changed = !Objects.equals(Boolean.TRUE.equals(row.getCurrent()), newCurrent)
                        || !Objects.equals(row.getNumber(), newNumber)
                        || !Objects.equals(row.getPositionsList(), newPositions)
                        || dto.sort() != null && !Objects.equals(row.getSort(), dto.sort());
                if (changed) {
                    this.applyFields(row, player, teamId, newNumber, newPositions, newCurrent, dto.sort());
                    toUpdate.add(row);
                }
            }
            desired.add(row);
        }
        for (PlayerTeam e : existing) {
            if (!seen.contains(e.getTeamId())) {
                toDelete.add(e);
            }
        }
        return desired;
    }

    private List<PlayerTeam> buildFromLegacy(Player player, List<PlayerTeam> existing, List<PlayerTeam> toCreate,
            List<PlayerTeam> toUpdate) {
        List<PlayerTeam> desired = new ArrayList<>(existing);
        Long teamId = player.getTeamId();
        if (teamId != null && teamId > 0L) {
            this.validateTeam(player, teamId);
            PlayerTeam row = null;
            for (PlayerTeam e : existing) {
                if (Objects.equals(e.getTeamId(), teamId)) {
                    row = e;
                    break;
                }
            }
            String number = PlayerTeamService.normalizeNumber(player.getNumber());
            String positions = player.getPositions();
            boolean created = false;
            boolean revived = false;
            if (row == null) {
                row = this.findRevivable(player, teamId);
                if (row != null) {
                    revived = true;
                    row.setDeletedAt(null);
                    row.setDeletedBy(null);
                } else {
                    row = new PlayerTeam();
                    created = true;
                }
            }
            boolean changed = created || revived
                    || !Boolean.TRUE.equals(row.getCurrent())
                    || !Objects.equals(row.getNumber(), number)
                    || !Objects.equals(row.getPositions(), positions);
            if (changed) {
                row.setPlayerId(player.getId());
                row.setTeamId(teamId);
                row.setTenantId(player.getTenantId());
                row.setNumber(number);
                row.setPositions(positions);
                row.setCurrent(true);
                if (created) {
                    desired.add(row);
                    toCreate.add(row);
                } else {
                    toUpdate.add(row);
                }
            }
        } else {
            for (PlayerTeam e : desired) {
                if (Boolean.TRUE.equals(e.getCurrent())) {
                    e.setCurrent(false);
                    toUpdate.add(e);
                }
            }
        }
        return desired;
    }

    private void applyFields(PlayerTeam row, Player player, Long teamId, String number, List<String> positions,
            Boolean current, Integer sort) {
        row.setPlayerId(player.getId());
        row.setTeamId(teamId);
        row.setTenantId(player.getTenantId());
        row.setNumber(number);
        row.setPositionsList(positions);
        row.setCurrent(current);
        if (sort != null) {
            row.setSort(sort);
        }
    }

    private Team validateTeam(Player player, Long teamId) {
        Team team = this.teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            throw new BusinessException(400, "球队不存在，请先创建球队");
        }
        if (!Objects.equals(team.getTenantId(), player.getTenantId())) {
            throw new BusinessException(400, "球队与当前租户不一致");
        }
        return team;
    }

    private PlayerTeam findRevivable(Player player, Long teamId) {
        if (player.getId() == null) {
            return null;
        }
        return this.playerTeamRepository
                .findFirstByPlayerIdAndTeamIdAndDeletedAtIsNotNullOrderByIdDesc(player.getId(), teamId)
                .orElse(null);
    }

    private static String normalizeNumber(String number) {
        if (number == null || number.isBlank()) {
            return null;
        }
        return number.trim();
    }

    private static PlayerTeam pickPrimary(List<PlayerTeam> entries) {
        for (PlayerTeam e : sortedBySortThenId(entries)) {
            if (Boolean.TRUE.equals(e.getCurrent())) {
                return e;
            }
        }
        return null;
    }

    private static List<PlayerTeam> sortedBySortThenId(List<PlayerTeam> entries) {
        List<PlayerTeam> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator
                .comparingInt((PlayerTeam e) -> e.getSort() == null ? 0 : e.getSort())
                .thenComparing(PlayerTeam::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return sorted;
    }

    private static Set<Long> currentTeamIdsOf(List<PlayerTeam> entries) {
        LinkedHashSet<Long> out = new LinkedHashSet<>();
        for (PlayerTeam e : entries) {
            if (Boolean.TRUE.equals(e.getCurrent())) {
                out.add(e.getTeamId());
            }
        }
        return out;
    }
}
