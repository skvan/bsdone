package com.bsball.repository;

import com.bsball.model.entity.GameSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 比赛实时录入快照仓储（2026-11-15 生产 OOM 修复，Issue #686）。
 *
 * <p>只读写 {@code bs_game.live_snapshot_json}，避免比赛列表/详情事务把 TEXT 大字段带入
 * 实体对象图；快照正文平均约 2.4MB，按需单独读取。</p>
 */
public interface GameSnapshotRepository extends JpaRepository<GameSnapshot, Long> {

    /** 按比赛 ID 读取快照正文；不存在时返回空。 */
    @Query(value="select s.liveSnapshotJson from GameSnapshot s where s.id = :id")
    Optional<String> findSnapshotJsonById(@Param(value="id") Long id);

    /**
     * 仅更新快照列与更新人/更新时间，返回受影响行数。
     * 快照为空/空白时统一写 NULL（hasLiveSnapshot 语义仍为“存在有效快照”）；
     * operatorId 为空（无当前用户上下文）时保留原更新人。
     */
    @Modifying(flushAutomatically=true)
    @Query(value="update GameSnapshot s set s.liveSnapshotJson = :json, s.updatedAt = CURRENT_TIMESTAMP, "
            + "s.updatedBy = coalesce(:operatorId, s.updatedBy) where s.id = :id")
    int updateSnapshotJson(@Param(value="id") Long id, @Param(value="json") String json,
            @Param(value="operatorId") Long operatorId);
}
