package com.bsball.model.dto;

public class PitcherRunOverrideRequest {
    private Integer runsAllowed;
    private Integer earnedRuns;
    private String note;

    public Integer getRunsAllowed() {
        return runsAllowed;
    }

    public void setRunsAllowed(Integer runsAllowed) {
        this.runsAllowed = runsAllowed;
    }

    public Integer getEarnedRuns() {
        return earnedRuns;
    }

    public void setEarnedRuns(Integer earnedRuns) {
        this.earnedRuns = earnedRuns;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
