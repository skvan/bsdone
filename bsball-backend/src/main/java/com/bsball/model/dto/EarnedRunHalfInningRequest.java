package com.bsball.model.dto;

import com.bsball.stats.earnedrun.EarnedRunPlay;
import java.util.List;

public class EarnedRunHalfInningRequest {
    private Integer inning;
    private String half;
    private List<EarnedRunPlay> plays;

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

    public List<EarnedRunPlay> getPlays() {
        return plays;
    }

    public void setPlays(List<EarnedRunPlay> plays) {
        this.plays = plays;
    }
}
