package com.bsball.model.dto;

import java.util.List;

/**
 * 个人驾驶舱赛事下拉条目：同一赛事的代表球队与最后一场比赛聚合结果。
 * <p>{@code teamIds}/{@code teamNames} 保留 SQL 聚合的原始字符串，便于与查询列名一致；
 * {@code teamIdList}/{@code teamNameList} 为服务层解析后的结构，供响应直接输出数组。</p>
 */
public class PlayerEventSummaryDTO {
    private Long eventId;
    private String eventName;
    private String status;
    private String startDate;
    private String endDate;
    private String gameMode;
    private String teamIds;
    private String teamNames;
    private List<Long> teamIdList = List.of();
    private List<String> teamNameList = List.of();
    private String lastGameday;
    private int gameCount;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public String getTeamIds() {
        return teamIds;
    }

    public void setTeamIds(String teamIds) {
        this.teamIds = teamIds;
    }

    public String getTeamNames() {
        return teamNames;
    }

    public void setTeamNames(String teamNames) {
        this.teamNames = teamNames;
    }

    public List<Long> getTeamIdList() {
        return teamIdList;
    }

    public void setTeamIdList(List<Long> teamIdList) {
        this.teamIdList = teamIdList == null ? List.of() : teamIdList;
    }

    public List<String> getTeamNameList() {
        return teamNameList;
    }

    public void setTeamNameList(List<String> teamNameList) {
        this.teamNameList = teamNameList == null ? List.of() : teamNameList;
    }

    public String getLastGameday() {
        return lastGameday;
    }

    public void setLastGameday(String lastGameday) {
        this.lastGameday = lastGameday;
    }

    public int getGameCount() {
        return gameCount;
    }

    public void setGameCount(int gameCount) {
        this.gameCount = gameCount;
    }
}
