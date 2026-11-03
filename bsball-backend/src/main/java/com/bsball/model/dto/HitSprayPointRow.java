package com.bsball.model.dto;

import java.math.BigDecimal;

/**
 * 驾驶舱打击落点查询行：只保留统计与绘图需要的字段。
 * <p>刻意不查 created_at/recorded_at 等时间列——测试库该列为 TIMESTAMPTZ，
 * MyBatis 的 LocalDateTime 转换会失败，导致落点接口静默返回空。</p>
 */
public class HitSprayPointRow {
    private Long gameId;
    private String resultCode;
    private String bipCode;
    private BigDecimal sprayX;
    private BigDecimal sprayY;
    private String sprayZone;
    private String sprayDepth;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getBipCode() {
        return bipCode;
    }

    public void setBipCode(String bipCode) {
        this.bipCode = bipCode;
    }

    public BigDecimal getSprayX() {
        return sprayX;
    }

    public void setSprayX(BigDecimal sprayX) {
        this.sprayX = sprayX;
    }

    public BigDecimal getSprayY() {
        return sprayY;
    }

    public void setSprayY(BigDecimal sprayY) {
        this.sprayY = sprayY;
    }

    public String getSprayZone() {
        return sprayZone;
    }

    public void setSprayZone(String sprayZone) {
        this.sprayZone = sprayZone;
    }

    public String getSprayDepth() {
        return sprayDepth;
    }

    public void setSprayDepth(String sprayDepth) {
        this.sprayDepth = sprayDepth;
    }
}
