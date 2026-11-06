package com.bsball.model.dto;

/** ER 判定改派/作废请求。改派时 targetPitcherId 必填；作废时忽略。 */
public class EarnedRunDecisionAdjustRequest {
    private Long targetPitcherId;
    private String reason;

    public Long getTargetPitcherId() {
        return targetPitcherId;
    }

    public void setTargetPitcherId(Long targetPitcherId) {
        this.targetPitcherId = targetPitcherId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
