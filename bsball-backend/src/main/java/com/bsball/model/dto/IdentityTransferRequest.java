package com.bsball.model.dto;

import java.util.List;

/** 整名球员身份转记请求。 */
public class IdentityTransferRequest {
    private Long sourcePlayerId;
    private Long targetPlayerId;
    private List<String> scopes;
    private String note;

    public Long getSourcePlayerId() {
        return sourcePlayerId;
    }

    public void setSourcePlayerId(Long sourcePlayerId) {
        this.sourcePlayerId = sourcePlayerId;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
