/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  lombok.Generated
 */
package com.bsball.common;

import java.util.List;
import java.util.Map;
import lombok.Generated;

/*
 * Exception performing whole class analysis ignored.
 */
public class PageResult<T> {
    private List<T> list;
    private long total;
    private Map<String, Object> meta;

    public static <T> PageResult<T> of(List<T> list, long total) {
        PageResult p = new PageResult();
        p.setList(list);
        p.setTotal(total);
        return p;
    }

    public static <T> PageResult<T> of(List<T> list, long total, Map<String, Object> meta) {
        PageResult p = PageResult.of(list, (long)total);
        p.setMeta(meta);
        return p;
    }

    @Generated
    public PageResult() {
    }

    @Generated
    public List<T> getList() {
        return this.list;
    }

    @Generated
    public long getTotal() {
        return this.total;
    }

    @Generated
    public Map<String, Object> getMeta() {
        return this.meta;
    }

    @Generated
    public void setList(List<T> list) {
        this.list = list;
    }

    @Generated
    public void setTotal(long total) {
        this.total = total;
    }

    @Generated
    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PageResult)) {
            return false;
        }
        PageResult other = (PageResult)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getTotal() != other.getTotal()) {
            return false;
        }
        List this$list = this.getList();
        List other$list = other.getList();
        if (this$list == null ? other$list != null : !((Object)this$list).equals(other$list)) {
            return false;
        }
        Map this$meta = this.getMeta();
        Map other$meta = other.getMeta();
        return !(this$meta == null ? other$meta != null : !((Object)this$meta).equals(other$meta));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PageResult;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $total = this.getTotal();
        result = result * 59 + (int)($total >>> 32 ^ $total);
        List $list = this.getList();
        result = result * 59 + ($list == null ? 43 : ((Object)$list).hashCode());
        Map $meta = this.getMeta();
        result = result * 59 + ($meta == null ? 43 : ((Object)$meta).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "PageResult(list=" + String.valueOf(this.getList()) + ", total=" + this.getTotal() + ", meta=" + String.valueOf(this.getMeta()) + ")";
    }
}

