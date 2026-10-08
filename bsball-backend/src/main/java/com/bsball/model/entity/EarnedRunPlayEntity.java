package com.bsball.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bs_earned_run_play")
public class EarnedRunPlayEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tenantId;
    private Long gameId;
    private Integer inning;
    private String half;
    private Integer sequence;
    private String sourceEventId;
    private Integer actualOutsAdded;
    private Integer reconstructedOutsAdded;
    private Boolean rulingPending;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Integer getInning() {
        return inning;
    }

    public void setInning(Integer inning) {
        this.inning = inning;
    }

    public String getHalf() {
        return half;
    }

    public void setHalf(String half) {
        this.half = half;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Integer getActualOutsAdded() {
        return actualOutsAdded;
    }

    public void setActualOutsAdded(Integer actualOutsAdded) {
        this.actualOutsAdded = actualOutsAdded;
    }

    public Integer getReconstructedOutsAdded() {
        return reconstructedOutsAdded;
    }

    public void setReconstructedOutsAdded(Integer reconstructedOutsAdded) {
        this.reconstructedOutsAdded = reconstructedOutsAdded;
    }

    public Boolean getRulingPending() {
        return rulingPending;
    }

    public void setRulingPending(Boolean rulingPending) {
        this.rulingPending = rulingPending;
    }
}
