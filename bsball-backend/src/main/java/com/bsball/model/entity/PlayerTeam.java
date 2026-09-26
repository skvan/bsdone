package com.bsball.model.entity;

import com.bsball.common.json.PositionsJsonUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.annotations.Comment;

/**
 * 球员-球队经历：一位球员可注册多支球队（简历式多段），
 * 背号与守备位置按球队存储，当前球队可多选（{@code current} 字段）。
 */
@Entity
@Table(name = "bs_player_team")
@Comment(value = "球员球队经历")
public class PlayerTeam
extends BaseEntity {
    @Comment(value = "球员ID")
    @Column(nullable = false)
    private Long playerId;
    @Comment(value = "球队ID")
    @Column(nullable = false)
    private Long teamId;
    @Comment(value = "租户ID")
    @Column(nullable = false)
    private Long tenantId;
    @Size(max = 64)
    @Comment(value = "该队背号")
    private String number;
    @Column(columnDefinition = "TEXT")
    @Comment(value = "该队守备位置（JSON 数组）")
    private String positions;
    @Column(name = "is_current", nullable = false)
    @Comment(value = "是否当前球队")
    private Boolean current = Boolean.FALSE;
    @Comment(value = "排序")
    private Integer sort = 0;

    @JsonIgnore
    public String getPositions() {
        return this.positions;
    }

    @JsonIgnore
    public void setPositions(String positions) {
        this.positions = positions;
    }

    @JsonProperty(value = "positions")
    public List<String> getPositionsList() {
        return PositionsJsonUtil.parseList(this.positions);
    }

    @JsonProperty(value = "positions")
    public void setPositionsList(List<String> list) {
        this.positions = PositionsJsonUtil.toStorage(list);
    }

    public Long getPlayerId() {
        return this.playerId;
    }

    public Long getTeamId() {
        return this.teamId;
    }

    public Long getTenantId() {
        return this.tenantId;
    }

    public String getNumber() {
        return this.number;
    }

    public Boolean getCurrent() {
        return this.current;
    }

    public Integer getSort() {
        return this.sort;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
