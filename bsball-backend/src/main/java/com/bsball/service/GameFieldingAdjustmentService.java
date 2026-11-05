package com.bsball.service;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.FieldingAdjustmentCandidateDTO;
import com.bsball.model.dto.FieldingAdjustmentCreateRequest;
import com.bsball.model.dto.FieldingAdjustmentListDTO;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingAdjustment;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.Player;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PlayerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 赛后守备补记：把“失误导致本应完成的出局没发生”时漏记的助杀，独立于 ER 判定单独落账。
 *
 * <p>补记不进入 ER 判定表：ER 每次按半局删除重建，且只重算投手 R/ER；本服务只改球员行 A/TC
 * 与逐位置 A。统计事实仍是 bs_game_player_stat / bs_game_fielding_pos_stat，赛季、门户和
 * 比赛详情等现有读路径不需要感知补记表。</p>
 */
@Service
public class GameFieldingAdjustmentService {
    static final String STATUS_APPROVED = "APPROVED";
    static final String STATUS_REVOKED = "REVOKED";
    static final String STATUS_INVALIDATED = "INVALIDATED";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> VALID_POSITIONS = Set.of(
            "P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF");
    private static final Set<String> ERROR_CODES = Set.of("E", "ADVANCE_FE", "DK3_REACH_E");

    private final GameFieldingAdjustmentRepository adjustmentRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final GameFieldingPosStatRepository gameFieldingPosStatRepository;
    private final PlayerRepository playerRepository;

    public GameFieldingAdjustmentService(
            GameFieldingAdjustmentRepository adjustmentRepository,
            GamePlayerStatRepository gamePlayerStatRepository,
            GameFieldingPosStatRepository gameFieldingPosStatRepository,
            PlayerRepository playerRepository) {
        this.adjustmentRepository = adjustmentRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.gameFieldingPosStatRepository = gameFieldingPosStatRepository;
        this.playerRepository = playerRepository;
    }

    /** 列出全场补记；完整快照中源 Play 丢失的有效补记会先失效并扣回统计。 */
    @Transactional
    public List<FieldingAdjustmentListDTO> list(Game game) {
        Snapshot snapshot = readSnapshot(game);
        invalidateMissingSourcePlays(game, snapshot);
        List<GameFieldingAdjustment> rows = adjustmentRepository
                .findByTenantIdAndGameIdOrderByInningAscTopBottomAscSourceSequenceAscIdAsc(
                        game.getTenantId(), game.getId());
        Map<Long, String> names = loadPlayerNames(rows);
        Set<String> sourceIds = sourceIds(snapshot);
        return rows.stream()
                .map(row -> toDto(row, names, sourceIds, snapshot != null))
                .toList();
    }

    /** 列出赛后补记候选：快照中的失误 Play + 守备方可补记球员。 */
    @Transactional
    public List<FieldingAdjustmentCandidateDTO> listCandidates(Game game) {
        Snapshot snapshot = readSnapshot(game);
        if (snapshot == null || snapshot.plays() == null) {
            return List.of();
        }
        Map<Long, List<FieldingAdjustmentCandidateDTO.PlayerOption>> playersByTeam =
                loadCandidatePlayers(game);
        List<FieldingAdjustmentCandidateDTO> candidates = new ArrayList<>();
        for (JsonNode play : snapshot.plays()) {
            String sourceEventId = text(play, "id");
            if (sourceEventId.isBlank() || !isErrorPlay(play)) {
                continue;
            }
            String half = playHalf(play);
            Long teamId = "top".equals(half) ? game.getHomeTeamId() : game.getAwayTeamId();
            if (teamId == null) {
                continue;
            }
            int inning = integer(play, "inning", 1);
            int sequence = integer(play, "sequence", 0);
            candidates.add(new FieldingAdjustmentCandidateDTO(
                    sourceEventId,
                    inning,
                    half,
                    teamId,
                    sequence,
                    inning + ("top".equals(half) ? "上" : "下") + " #" + sequence,
                    integer(play, "outsBefore", 0),
                    describeBases(play.get("basesBefore")),
                    text(play, "resultText"),
                    playersByTeam.getOrDefault(teamId, List.of())));
        }
        return candidates;
    }

    /** 新增或重新激活补记；源 Play、守备方、守位和重复补记全部由后端校验。 */
    @Transactional
    public FieldingAdjustmentListDTO create(
            Game game, long operatorId, FieldingAdjustmentCreateRequest request) {
        validateCreateRequest(request);
        String sourceEventId = request.getSourceEventId().trim();
        JsonNode play = requireErrorPlay(readSnapshot(game), sourceEventId);
        String half = playHalf(play);
        Long teamId = "top".equals(half) ? game.getHomeTeamId() : game.getAwayTeamId();
        if (teamId == null) {
            throw new BusinessException(400, "比赛缺少守备方球队");
        }
        List<GamePlayerStat> playerStats = playerStats(game, teamId, request.getPlayerId());
        if (playerStats.isEmpty()) {
            throw new BusinessException(400, "补记对象不在本场守备方统计行中");
        }
        String position = resolvePosition(request.getPosition(), playerStats);
        if (position == null) {
            throw new BusinessException(400, "请选择有效的补记守位");
        }

        Optional<GameFieldingAdjustment> existing = adjustmentRepository
                .findFirstByTenantIdAndGameIdAndSourceEventIdAndPlayerIdOrderByIdAsc(
                        game.getTenantId(), game.getId(), sourceEventId, request.getPlayerId());
        if (adjustmentRepository
                .existsByTenantIdAndGameIdAndSourceEventIdAndPlayerIdAndStatus(
                        game.getTenantId(), game.getId(), sourceEventId,
                        request.getPlayerId(), STATUS_APPROVED)) {
            throw new BusinessException(409, "该 Play 已为这名守备员补记助杀");
        }

        GameFieldingAdjustment row = existing.orElseGet(GameFieldingAdjustment::new);
        row.setTenantId(game.getTenantId());
        row.setGameId(game.getId());
        row.setSourceEventId(sourceEventId);
        row.setSourceSequence(integer(play, "sequence", 0));
        row.setInning(integer(play, "inning", 1));
        row.setTopBottom(half);
        row.setTeamId(teamId);
        row.setPlayerId(request.getPlayerId());
        row.setPosition(position);
        row.setAssistCount(1);
        row.setStatus(STATUS_APPROVED);
        row.setSourceText(truncate(text(play, "resultText"), 512));
        row.setNote(blankToNull(request.getNote()));
        row.setRevokedBy(null);
        row.setRevokedAt(null);
        row.setInvalidReason(null);
        if (row.getId() == null) {
            row.setCreatedBy(operatorId);
        }
        GameFieldingAdjustment saved = adjustmentRepository.save(row);

        applyCredits(game, saved, 1);
        return toDto(saved, loadPlayerNames(List.of(saved)), Set.of(saved.getSourceEventId()), true);
    }

    /** 撤销补记：保留审计行，统计即时扣回；重复撤销由前端禁用，后端仍做守卫。 */
    @Transactional
    public FieldingAdjustmentListDTO revoke(Game game, long adjustmentId, long operatorId) {
        GameFieldingAdjustment row = adjustmentRepository
                .findByIdAndTenantIdAndGameId(adjustmentId, game.getTenantId(), game.getId())
                .orElseThrow(() -> new BusinessException(404, "补记记录不存在"));
        if (!STATUS_APPROVED.equals(row.getStatus())) {
            throw new BusinessException(400, "该补记已撤销或已失效");
        }
        row.setStatus(STATUS_REVOKED);
        row.setRevokedBy(operatorId);
        row.setRevokedAt(LocalDateTime.now());
        GameFieldingAdjustment saved = adjustmentRepository.save(row);
        applyCredits(game, saved, -1);
        return toDto(saved, loadPlayerNames(List.of(saved)), Set.of(saved.getSourceEventId()), true);
    }

    /** 保存覆盖球员统计后，仅对本次实际写入的统计行重放补记，避免部分载荷重复 +1。 */
    @Transactional
    public void reapplyPlayerStats(Game game, Set<String> touchedStatKeys) {
        if (touchedStatKeys == null || touchedStatKeys.isEmpty()) {
            return;
        }
        for (GameFieldingAdjustment row : approvedRows(game)) {
            int step = row.getAssistCount() == null ? 1 : row.getAssistCount();
            for (GamePlayerStat stat : creditedPlayerStats(game, row.getTeamId(), row.getPlayerId())) {
                if (!touchedStatKeys.contains(statKey(stat))) {
                    continue;
                }
                adjustPlayerStat(stat, step);
            }
        }
    }

    /** 保存覆盖逐位置守备后，按仍有效的补记重放位置 A。 */
    @Transactional
    public void reapplyPositionStats(Game game) {
        for (GameFieldingAdjustment row : approvedRows(game)) {
            applyPositionCredit(game, row, 1);
        }
    }

    /** 保存前复核仍有效的补记：完整快照中源 Play 丢失时先失效并扣回，避免重放已失效补记。 */
    @Transactional
    public void refreshApprovedValidity(Game game) {
        if (approvedRows(game).isEmpty()) {
            return;
        }
        invalidateMissingSourcePlays(game, readSnapshot(game));
    }

    /** 删除统计行前的守卫：该球员是否仍有已生效的守备补记。 */
    public boolean hasApprovedPlayerAdjustment(long tenantId, long gameId, long playerId) {
        return countApprovedPlayerAdjustments(tenantId, gameId, playerId) > 0;
    }

    /** 该球员仍生效的守备补记条数；删除诊断使用。 */
    public long countApprovedPlayerAdjustments(long tenantId, long gameId, long playerId) {
        if (tenantId <= 0 || gameId <= 0 || playerId <= 0) {
            return 0;
        }
        return adjustmentRepository.countByTenantIdAndGameIdAndPlayerIdAndStatus(
                tenantId, gameId, playerId, STATUS_APPROVED);
    }

    /** 读取时透传：按球员汇总本场已生效补记 A，供 /stats 前端判断统计是否已含补记。 */
    public Map<Long, Integer> approvedAssistTotalsByGame(long gameId) {
        Map<Long, Integer> totals = new HashMap<>();
        if (gameId <= 0) {
            return totals;
        }
        for (GameFieldingAdjustment row : adjustmentRepository.findByGameIdAndStatus(
                gameId, STATUS_APPROVED)) {
            if (row.getPlayerId() == null) {
                continue;
            }
            totals.merge(row.getPlayerId(),
                    row.getAssistCount() == null ? 1 : row.getAssistCount(), Integer::sum);
        }
        return totals;
    }

    private List<GameFieldingAdjustment> approvedRows(Game game) {
        return adjustmentRepository.findByTenantIdAndGameIdAndStatus(
                game.getTenantId(), game.getId(), STATUS_APPROVED);
    }

    private void invalidateMissingSourcePlays(Game game, Snapshot snapshot) {
        if (snapshot == null || !snapshot.complete() || snapshot.plays() == null) {
            return;
        }
        Set<String> ids = sourceIds(snapshot);
        for (GameFieldingAdjustment row : approvedRows(game)) {
            if (ids.contains(row.getSourceEventId())) {
                continue;
            }
            row.setStatus(STATUS_INVALIDATED);
            row.setInvalidReason("源 Play 已不在当前比赛快照中");
            adjustmentRepository.save(row);
            applyCredits(game, row, -1);
        }
    }

    private void applyCredits(Game game, GameFieldingAdjustment row, int delta) {
        applyPlayerCredit(game, row, delta);
        applyPositionCredit(game, row, delta);
    }

    private void applyPlayerCredit(Game game, GameFieldingAdjustment row, int delta) {
        int count = row.getAssistCount() == null ? 1 : row.getAssistCount();
        int step = count * delta;
        List<GamePlayerStat> stats = creditedPlayerStats(game, row.getTeamId(), row.getPlayerId());
        if (stats.isEmpty()) {
            if (step > 0) {
                throw new BusinessException(400, "补记对象不在本场统计行中");
            }
            return;
        }
        for (GamePlayerStat stat : stats) {
            adjustPlayerStat(stat, step);
        }
    }

    private void adjustPlayerStat(GamePlayerStat stat, int step) {
        int oldA = valueOrZero(stat.getA());
        int oldTc = stat.getTc() != null
                ? stat.getTc()
                : valueOrZero(stat.getPo()) + oldA + valueOrZero(stat.getE());
        stat.setA(Math.max(0, oldA + step));
        stat.setTc(Math.max(0, oldTc + step));
        gamePlayerStatRepository.save(stat);
    }

    private static String statKey(GamePlayerStat stat) {
        return stat.getTeamId() + "_" + stat.getPlayerId() + "_"
                + (stat.getIsPitcher() == null ? 0 : stat.getIsPitcher());
    }

    /**
     * 守备统计只落在“可计入球员行”：同场同时有投手行与野手行时，赛季聚合只取野手行，
     * 补记也照此只改野手行，避免页面把并行投手行再显示一次。DH 仅守备投手只有投手行时，
     * 仍改该行。
     */
    private List<GamePlayerStat> creditedPlayerStats(Game game, Long teamId, Long playerId) {
        List<GamePlayerStat> stats = playerStats(game, teamId, playerId);
        List<GamePlayerStat> nonPitcher = stats.stream()
                .filter(stat -> !Integer.valueOf(1).equals(stat.getIsPitcher()))
                .toList();
        return nonPitcher.isEmpty() ? stats : nonPitcher;
    }

    private void applyPositionCredit(Game game, GameFieldingAdjustment row, int delta) {
        int count = row.getAssistCount() == null ? 1 : row.getAssistCount();
        int step = count * delta;
        List<GameFieldingPosStat> rows = gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(game.getId()).stream()
                .filter(item -> Objects.equals(item.getTenantId(), game.getTenantId()))
                .filter(item -> Objects.equals(item.getTeamId(), row.getTeamId()))
                .filter(item -> Objects.equals(item.getPlayerId(), row.getPlayerId()))
                .filter(item -> Objects.equals(item.getPosition(), row.getPosition()))
                .toList();
        if (rows.isEmpty()) {
            if (step <= 0) {
                return;
            }
            GameFieldingPosStat created = new GameFieldingPosStat();
            created.setTenantId(game.getTenantId());
            created.setGameId(game.getId());
            created.setTeamId(row.getTeamId());
            created.setPlayerId(row.getPlayerId());
            created.setPosition(row.getPosition());
            created.setOuts(0);
            created.setPo(0);
            created.setA(step);
            created.setE(0);
            created.setDp(0);
            created.setPb(0);
            created.setCatcherCs(0);
            created.setCatcherSb(0);
            created.setFieldingGs(false);
            created.setSource("MANUAL");
            gameFieldingPosStatRepository.save(created);
            return;
        }
        for (GameFieldingPosStat item : rows) {
            item.setA(Math.max(0, valueOrZero(item.getA()) + step));
            gameFieldingPosStatRepository.save(item);
        }
    }

    private Map<Long, List<FieldingAdjustmentCandidateDTO.PlayerOption>> loadCandidatePlayers(
            Game game) {
        List<GamePlayerStat> stats = gamePlayerStatRepository.findByGameId(game.getId()).stream()
                .filter(stat -> Objects.equals(stat.getTenantId(), game.getTenantId()))
                .filter(stat -> stat.getTeamId() != null && stat.getPlayerId() != null)
                .sorted(Comparator
                        .comparing(GamePlayerStat::getListOrder,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(GamePlayerStat::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();
        Set<Long> playerIds = new HashSet<>();
        for (GamePlayerStat stat : stats) {
            playerIds.add(stat.getPlayerId());
        }
        Map<Long, Player> players = new HashMap<>();
        for (Player player : playerRepository.findAllById(playerIds)) {
            players.put(player.getId(), player);
        }
        Map<Long, Map<Long, FieldingAdjustmentCandidateDTO.PlayerOption>> byTeam =
                new HashMap<>();
        Map<Long, Set<Long>> seenByTeam = new HashMap<>();
        for (GamePlayerStat stat : stats) {
            Set<Long> seen = seenByTeam.computeIfAbsent(stat.getTeamId(), key -> new HashSet<>());
            if (!seen.add(stat.getPlayerId())) {
                continue;
            }
            Player player = players.get(stat.getPlayerId());
            String position = stat.getPosition() == null
                    ? null : stat.getPosition().trim().toUpperCase(Locale.ROOT);
            List<String> positions = new ArrayList<>();
            if (player != null) {
                positions.addAll(player.getPositionsList());
            }
            if (position != null && !position.isBlank() && !positions.contains(position)) {
                positions.add(position);
            }
            String number = stat.getNumber();
            if (number == null && player != null) {
                number = player.getNumber();
            }
            String name = player != null ? player.getName() : "#" + stat.getPlayerId();
            byTeam.computeIfAbsent(stat.getTeamId(), key -> new LinkedHashMap<>())
                    .put(stat.getPlayerId(), new FieldingAdjustmentCandidateDTO.PlayerOption(
                            stat.getPlayerId(),
                            name,
                            number,
                            position,
                            positions));
        }
        Map<Long, List<FieldingAdjustmentCandidateDTO.PlayerOption>> result = new HashMap<>();
        byTeam.forEach((teamId, options) -> result.put(teamId, List.copyOf(options.values())));
        return result;
    }

    private static String describeBases(JsonNode bases) {
        boolean first = bases != null && bases.path("1").asBoolean(false);
        boolean second = bases != null && bases.path("2").asBoolean(false);
        boolean third = bases != null && bases.path("3").asBoolean(false);
        if (!first && !second && !third) {
            return "垒上无人";
        }
        if (first && second && third) {
            return "满垒";
        }
        List<String> occupied = new ArrayList<>();
        if (first) {
            occupied.add("一");
        }
        if (second) {
            occupied.add("二");
        }
        if (third) {
            occupied.add("三");
        }
        return String.join("、", occupied) + "垒有人";
    }

    private List<GamePlayerStat> playerStats(Game game, Long teamId, Long playerId) {
        return gamePlayerStatRepository.findByGameId(game.getId()).stream()
                .filter(stat -> Objects.equals(stat.getTenantId(), game.getTenantId()))
                .filter(stat -> Objects.equals(stat.getTeamId(), teamId))
                .filter(stat -> Objects.equals(stat.getPlayerId(), playerId))
                .toList();
    }

    private static void validateCreateRequest(FieldingAdjustmentCreateRequest request) {
        if (request == null || request.getSourceEventId() == null
                || request.getSourceEventId().isBlank()
                || request.getPlayerId() == null || request.getPlayerId() <= 0) {
            throw new BusinessException(400, "缺少源 Play 或补记球员");
        }
        if (request.getNote() != null && request.getNote().length() > 200) {
            throw new BusinessException(400, "补记备注不能超过 200 字");
        }
    }

    private JsonNode requireErrorPlay(Snapshot snapshot, String sourceEventId) {
        if (snapshot == null || snapshot.plays() == null) {
            throw new BusinessException(400, "缺少比赛快照，无法关联源 Play");
        }
        JsonNode play = findPlay(snapshot, sourceEventId);
        if (play == null) {
            throw new BusinessException(400, "源 Play 不在当前比赛快照中，请刷新后重试");
        }
        if (!isErrorPlay(play)) {
            throw new BusinessException(400, "该 Play 未记录失误，不能补记助杀");
        }
        return play;
    }

    private static String resolvePosition(String requested, List<GamePlayerStat> playerStats) {
        String position = normalizePosition(requested);
        return position != null ? position : normalizePosition(playerStats.get(0).getPosition());
    }

    private FieldingAdjustmentListDTO toDto(
            GameFieldingAdjustment row,
            Map<Long, String> names,
            Set<String> sourceIds,
            boolean snapshotAvailable) {
        boolean sourceValid = snapshotAvailable
                && sourceIds.contains(row.getSourceEventId())
                && !STATUS_INVALIDATED.equals(row.getStatus());
        return new FieldingAdjustmentListDTO(
                row.getId(),
                row.getSourceEventId(),
                row.getInning(),
                row.getTopBottom(),
                row.getSourceSequence(),
                row.getTeamId(),
                row.getPlayerId(),
                names.get(row.getPlayerId()),
                row.getPosition(),
                row.getAssistCount(),
                row.getStatus(),
                row.getSourceText(),
                row.getNote(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getRevokedBy(),
                row.getRevokedAt(),
                row.getInvalidReason(),
                sourceValid);
    }

    private Map<Long, String> loadPlayerNames(List<GameFieldingAdjustment> rows) {
        Set<Long> ids = new HashSet<>();
        for (GameFieldingAdjustment row : rows) {
            if (row.getPlayerId() != null) {
                ids.add(row.getPlayerId());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        for (Player player : playerRepository.findAllById(ids)) {
            names.put(player.getId(), player.getName());
        }
        return names;
    }

    private Snapshot readSnapshot(Game game) {
        String json = game.getLiveSnapshotJson();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode plays = root.path("playEvents");
            return new Snapshot(
                    plays.isArray() ? plays : null,
                    root.path("playEventsComplete").asBoolean(false));
        } catch (Exception error) {
            return null;
        }
    }

    private JsonNode findPlay(Snapshot snapshot, String sourceEventId) {
        for (JsonNode play : snapshot.plays()) {
            if (sourceEventId.equals(text(play, "id"))) {
                return play;
            }
        }
        return null;
    }

    private Set<String> sourceIds(Snapshot snapshot) {
        if (snapshot == null || snapshot.plays() == null) {
            return Set.of();
        }
        Set<String> ids = new HashSet<>();
        for (JsonNode play : snapshot.plays()) {
            String id = text(play, "id");
            if (!id.isBlank()) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** 归一化半局：快照中除明确 bottom 外一律按 top 处理。 */
    private static String playHalf(JsonNode play) {
        return "bottom".equalsIgnoreCase(text(play, "half")) ? "bottom" : "top";
    }

    private boolean isErrorPlay(JsonNode play) {
        String code = text(play, "resultCode").toUpperCase(Locale.ROOT);
        String resultText = text(play, "resultText");
        return ERROR_CODES.contains(code)
                || code.startsWith("ERROR")
                || resultText.contains("失误");
    }

    private static String normalizePosition(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return VALID_POSITIONS.contains(normalized) ? normalized : null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private static int integer(JsonNode node, String field, int fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || !value.isNumber() ? fallback : value.asInt(fallback);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record Snapshot(JsonNode plays, boolean complete) {
    }
}
