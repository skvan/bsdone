/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.SQLiteNoCommentDialect
 *  org.hibernate.community.dialect.SQLiteDialect
 */
package com.bsball.config;

import org.hibernate.community.dialect.SQLiteDialect;

public class SQLiteNoCommentDialect
extends SQLiteDialect {
    public boolean supportsCommentOn() {
        return false;
    }
}

