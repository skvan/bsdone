/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.Result
 *  lombok.Generated
 */
package com.bsball.common;

import lombok.Generated;

/*
 * Exception performing whole class analysis ignored.
 */
public class Result<T> {
    private int code;
    private String msg;
    private String detail;
    private T data;
    private long timestamp;

    private static <T> void stamp(Result<T> r) {
        r.setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> ok(T data) {
        Result r = new Result();
        r.setCode(200);
        r.setMsg("success");
        r.setData(data);
        Result.stamp((Result)r);
        return r;
    }

    public static <T> Result<T> okEmpty(T data) {
        Result r = new Result();
        r.setCode(1000);
        r.setMsg("no_data");
        r.setData(data);
        Result.stamp((Result)r);
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        Result r = new Result();
        r.setCode(500);
        r.setMsg(msg);
        r.setData(null);
        Result.stamp((Result)r);
        return r;
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result r = new Result();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(null);
        Result.stamp((Result)r);
        return r;
    }

    public static <T> Result<T> fail(int code, String msg, String detail) {
        Result r = new Result();
        r.setCode(code);
        r.setMsg(msg);
        r.setDetail(detail);
        r.setData(null);
        Result.stamp((Result)r);
        return r;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Generated
    public Result() {
    }

    @Generated
    public int getCode() {
        return this.code;
    }

    @Generated
    public String getMsg() {
        return this.msg;
    }

    @Generated
    public String getDetail() {
        return this.detail;
    }

    @Generated
    public T getData() {
        return (T)this.data;
    }

    @Generated
    public long getTimestamp() {
        return this.timestamp;
    }

    @Generated
    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Generated
    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Generated
    public void setData(T data) {
        this.data = data;
    }

    @Generated
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Result)) {
            return false;
        }
        Result other = (Result)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getCode() != other.getCode()) {
            return false;
        }
        if (this.getTimestamp() != other.getTimestamp()) {
            return false;
        }
        String this$msg = this.getMsg();
        String other$msg = other.getMsg();
        if (this$msg == null ? other$msg != null : !this$msg.equals(other$msg)) {
            return false;
        }
        String this$detail = this.getDetail();
        String other$detail = other.getDetail();
        if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) {
            return false;
        }
        Object this$data = this.getData();
        Object other$data = other.getData();
        return !(this$data == null ? other$data != null : !this$data.equals(other$data));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Result;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getCode();
        long $timestamp = this.getTimestamp();
        result = result * 59 + (int)($timestamp >>> 32 ^ $timestamp);
        String $msg = this.getMsg();
        result = result * 59 + ($msg == null ? 43 : $msg.hashCode());
        String $detail = this.getDetail();
        result = result * 59 + ($detail == null ? 43 : $detail.hashCode());
        Object $data = this.getData();
        result = result * 59 + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "Result(code=" + this.getCode() + ", msg=" + this.getMsg() + ", detail=" + this.getDetail() + ", data=" + String.valueOf(this.getData()) + ", timestamp=" + this.getTimestamp() + ")";
    }
}

