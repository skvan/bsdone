package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

/**
 * 比赛实时录入快照（2026-11-15 生产 OOM 修复，Issue #686）。
 *
 * <p>生产 {@code bs_game.live_snapshot_json} 平均约 2.4MB、最大 12MB。此前该列与 {@link Game}
 * 同实体映射，比赛列表每取 100 场就会把约 240MB 文本读入堆内存，在约 894MB 堆上限下触发
 * {@code OutOfMemoryError}。本类只映射比赛 ID 与快照列，由 {@code GameSnapshotRepository}
 * 按需读写，比赛列表查询不再触碰大字段。</p>
 */
@Entity
@Table(name="bs_game")
@Comment(value="\u6bd4\u8d5b\u5b9e\u65f6\u5f55\u5165\u5feb\u7167\uff08\u5927\u5b57\u6bb5\u5355\u72ec\u6620\u5c04\uff09")
public class GameSnapshot {
    @Id
    @Comment(value="\u6bd4\u8d5bID")
    private Long id;

    @Column(name="live_snapshot_json", columnDefinition="TEXT")
    @Comment(value="\u5b9e\u65f6\u5f55\u5165\u5feb\u7167 JSON")
    private String liveSnapshotJson;

    @Column(name="updated_at")
    @Comment(value="\u66f4\u65b0\u65f6\u95f4")
    private LocalDateTime updatedAt;

    @Column(name="updated_by")
    @Comment(value="\u66f4\u65b0\u4eba")
    private Long updatedBy;

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getLiveSnapshotJson() {
        return this.liveSnapshotJson;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setLiveSnapshotJson(String liveSnapshotJson) {
        this.liveSnapshotJson = liveSnapshotJson;
    }
}
