/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.exception.BusinessException
 */
package com.bsball.exception;

public class BusinessException
extends RuntimeException {
    private final int code;
    /**
     * 本地化文案键（可空）。非空时由 {@code GlobalExceptionHandler} 按请求语言解成正文；
     * 注意不要拿第三个参数当普通文本使——旧代码全是两参形式，三参只会进下面那个带键的构造器。
     */
    private final String messageKey;
    private final Object[] messageArgs;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        this(code, message, null);
    }

    /**
     * 带文案键的一发。{@code message} 继续给中文原文作兜底：漏翻、非 web 上下文
     * （单测直调 service 时根本不过 handler）、以及日志都要有话说。
     */
    public BusinessException(int code, String message, String messageKey, Object... messageArgs) {
        super(message);
        this.code = code;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public int getCode() {
        return this.code;
    }

    public String getMessageKey() {
        return this.messageKey;
    }

    public Object[] getMessageArgs() {
        return this.messageArgs;
    }
}

