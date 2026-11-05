package com.bsball.model.dto;

public class PitcherTransferRequest {
    private Long sourceStatId;
    private Long targetPlayerId;
    private String note;

    public Long getSourceStatId() {
        return sourceStatId;
    }

    public void setSourceStatId(Long sourceStatId) {
        this.sourceStatId = sourceStatId;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
