package com.bsball.model.dto;

import com.bsball.stats.earnedrun.EarnedRunPlay;
import java.util.List;

public class EarnedRunHalfInningRequest {
    private Integer inning;
    private String half;
    private List<EarnedRunPlay> plays;
    /**
     * 明知只重建部分半局（判定数会低于记分栏）时显式确认越过完整性守门；
     * 旧客户端不送该字段时按 false 处理，即默认拒绝用残缺分母覆写投手层。
     */
    private Boolean confirmPartialRebuild;
    /**
     * 该半局是否已打完（第三出局、再见得分、换局或结束比赛）。
     * false 时服务端只算不写：判定、投手覆写、统计均不动，仅回预览四栏供现场显示；
     * 旧客户端不送该字段时按 true 处理，保持与现行行为一致。
     */
    private Boolean halfComplete;
    /**
     * 客户端当前看到的官方总比分（主+客）。同一次保存里记分栏要等 save-live 才落库，
     * 这里用本地比分当守门证人，撤销/改小本半局得分后才不会被旧记分误拒。
     * 旧客户端不送该字段时沿用服务器记分栏。
     */
    private Integer officialRuns;

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

    public Boolean getConfirmPartialRebuild() {
        return confirmPartialRebuild;
    }

    public void setConfirmPartialRebuild(Boolean confirmPartialRebuild) {
        this.confirmPartialRebuild = confirmPartialRebuild;
    }

    public Boolean getHalfComplete() {
        return halfComplete;
    }

    public void setHalfComplete(Boolean halfComplete) {
        this.halfComplete = halfComplete;
    }

    public Integer getOfficialRuns() {
        return officialRuns;
    }

    public void setOfficialRuns(Integer officialRuns) {
        this.officialRuns = officialRuns;
    }
}
