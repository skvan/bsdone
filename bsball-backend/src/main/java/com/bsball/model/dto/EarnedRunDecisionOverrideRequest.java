package com.bsball.model.dto;

public class EarnedRunDecisionOverrideRequest {
    private String earnedStatus;
    private String unearnedReason;

    public String getEarnedStatus() { return earnedStatus; }
    public void setEarnedStatus(String earnedStatus) { this.earnedStatus = earnedStatus; }
    public String getUnearnedReason() { return unearnedReason; }
    public void setUnearnedReason(String unearnedReason) { this.unearnedReason = unearnedReason; }
}
