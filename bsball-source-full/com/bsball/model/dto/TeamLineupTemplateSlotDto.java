/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.TeamLineupTemplateSlotDto
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class TeamLineupTemplateSlotDto {
    private Long playerId;
    private Integer battingOrder;
    private String position;
    private String number;
    private String batHand;
    private Integer fieldingGs;

    @Generated
    public TeamLineupTemplateSlotDto() {
    }

    @Generated
    public Long getPlayerId() {
        return this.playerId;
    }

    @Generated
    public Integer getBattingOrder() {
        return this.battingOrder;
    }

    @Generated
    public String getPosition() {
        return this.position;
    }

    @Generated
    public String getNumber() {
        return this.number;
    }

    @Generated
    public String getBatHand() {
        return this.batHand;
    }

    @Generated
    public Integer getFieldingGs() {
        return this.fieldingGs;
    }

    @Generated
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    @Generated
    public void setBattingOrder(Integer battingOrder) {
        this.battingOrder = battingOrder;
    }

    @Generated
    public void setPosition(String position) {
        this.position = position;
    }

    @Generated
    public void setNumber(String number) {
        this.number = number;
    }

    @Generated
    public void setBatHand(String batHand) {
        this.batHand = batHand;
    }

    @Generated
    public void setFieldingGs(Integer fieldingGs) {
        this.fieldingGs = fieldingGs;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TeamLineupTemplateSlotDto)) {
            return false;
        }
        TeamLineupTemplateSlotDto other = (TeamLineupTemplateSlotDto)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$playerId = this.getPlayerId();
        Long other$playerId = other.getPlayerId();
        if (this$playerId == null ? other$playerId != null : !((Object)this$playerId).equals(other$playerId)) {
            return false;
        }
        Integer this$battingOrder = this.getBattingOrder();
        Integer other$battingOrder = other.getBattingOrder();
        if (this$battingOrder == null ? other$battingOrder != null : !((Object)this$battingOrder).equals(other$battingOrder)) {
            return false;
        }
        Integer this$fieldingGs = this.getFieldingGs();
        Integer other$fieldingGs = other.getFieldingGs();
        if (this$fieldingGs == null ? other$fieldingGs != null : !((Object)this$fieldingGs).equals(other$fieldingGs)) {
            return false;
        }
        String this$position = this.getPosition();
        String other$position = other.getPosition();
        if (this$position == null ? other$position != null : !this$position.equals(other$position)) {
            return false;
        }
        String this$number = this.getNumber();
        String other$number = other.getNumber();
        if (this$number == null ? other$number != null : !this$number.equals(other$number)) {
            return false;
        }
        String this$batHand = this.getBatHand();
        String other$batHand = other.getBatHand();
        return !(this$batHand == null ? other$batHand != null : !this$batHand.equals(other$batHand));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TeamLineupTemplateSlotDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $playerId = this.getPlayerId();
        result = result * 59 + ($playerId == null ? 43 : ((Object)$playerId).hashCode());
        Integer $battingOrder = this.getBattingOrder();
        result = result * 59 + ($battingOrder == null ? 43 : ((Object)$battingOrder).hashCode());
        Integer $fieldingGs = this.getFieldingGs();
        result = result * 59 + ($fieldingGs == null ? 43 : ((Object)$fieldingGs).hashCode());
        String $position = this.getPosition();
        result = result * 59 + ($position == null ? 43 : $position.hashCode());
        String $number = this.getNumber();
        result = result * 59 + ($number == null ? 43 : $number.hashCode());
        String $batHand = this.getBatHand();
        result = result * 59 + ($batHand == null ? 43 : $batHand.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TeamLineupTemplateSlotDto(playerId=" + this.getPlayerId() + ", battingOrder=" + this.getBattingOrder() + ", position=" + this.getPosition() + ", number=" + this.getNumber() + ", batHand=" + this.getBatHand() + ", fieldingGs=" + this.getFieldingGs() + ")";
    }
}

