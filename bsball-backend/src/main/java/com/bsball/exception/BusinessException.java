package com.bsball.exception;

public class BusinessException
extends RuntimeException {
    private final int code;
    private final String reason;

    public BusinessException(String message) {
        this(500, message, null);
    }

    public BusinessException(int code, String message) {
        this(code, message, null);
    }

    public BusinessException(int code, String message, String reason) {
        super(message);
        this.code = code;
        this.reason = reason;
    }

    public int getCode() {
        return this.code;
    }

    /** 稳定原因码：文案会改，供前端做多语种映射与统计。可为 null。 */
    public String getReason() {
        return this.reason;
    }
}
