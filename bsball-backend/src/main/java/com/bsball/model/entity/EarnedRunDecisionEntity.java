package com.bsball.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "bs_earned_run_decision")
public class EarnedRunDecisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tenantId;
    private Long gameId;
    private Long playId;
    private Long runnerId;
    private Long responsiblePitcherId;
    private String runnerOrigin;
    private String earnedStatus;
    private String unearnedReason;
    private Long overriddenBy;
    private LocalDateTime overriddenAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public Long getPlayId() { return playId; }
    public void setPlayId(Long playId) { this.playId = playId; }
    public Long getRunnerId() { return runnerId; }
    public void setRunnerId(Long runnerId) { this.runnerId = runnerId; }
    public Long getResponsiblePitcherId() { return responsiblePitcherId; }
    public void setResponsiblePitcherId(Long responsiblePitcherId) {
        this.responsiblePitcherId = responsiblePitcherId;
    }
    public String getRunnerOrigin() { return runnerOrigin; }
    public void setRunnerOrigin(String runnerOrigin) { this.runnerOrigin = runnerOrigin; }
    public String getEarnedStatus() { return earnedStatus; }
    public void setEarnedStatus(String earnedStatus) { this.earnedStatus = earnedStatus; }
    public String getUnearnedReason() { return unearnedReason; }
    public void setUnearnedReason(String unearnedReason) { this.unearnedReason = unearnedReason; }
    public Long getOverriddenBy() { return overriddenBy; }
    public void setOverriddenBy(Long overriddenBy) { this.overriddenBy = overriddenBy; }
    public LocalDateTime getOverriddenAt() { return overriddenAt; }
    public void setOverriddenAt(LocalDateTime overriddenAt) { this.overriddenAt = overriddenAt; }
}
