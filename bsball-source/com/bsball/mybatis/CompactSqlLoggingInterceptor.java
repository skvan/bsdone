/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.mybatis.CompactSqlLoggingInterceptor
 *  org.apache.ibatis.cache.CacheKey
 *  org.apache.ibatis.executor.Executor
 *  org.apache.ibatis.mapping.BoundSql
 *  org.apache.ibatis.mapping.MappedStatement
 *  org.apache.ibatis.plugin.Interceptor
 *  org.apache.ibatis.plugin.Intercepts
 *  org.apache.ibatis.plugin.Invocation
 *  org.apache.ibatis.plugin.Plugin
 *  org.apache.ibatis.plugin.Signature
 *  org.apache.ibatis.session.ResultHandler
 *  org.apache.ibatis.session.RowBounds
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.bsball.mybatis;

import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts(value={@Signature(type=Executor.class, method="update", args={MappedStatement.class, Object.class}), @Signature(type=Executor.class, method="query", args={MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}), @Signature(type=Executor.class, method="query", args={MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})})
public class CompactSqlLoggingInterceptor
implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger((String)"com.bsball.mybatis.sql");
    private static final Pattern WHITESPACE = Pattern.compile("[\\r\\n\\s]+");

    public Object intercept(Invocation invocation) throws Throwable {
        if (log.isDebugEnabled()) {
            try {
                BoundSql b;
                Object object;
                Object[] args = invocation.getArgs();
                MappedStatement ms = (MappedStatement)args[0];
                Object parameter = args[1];
                BoundSql boundSql = args.length > 5 && (object = args[5]) instanceof BoundSql ? (b = (BoundSql)object) : ms.getBoundSql(parameter);
                String sql = boundSql.getSql();
                if (sql != null && !sql.isBlank()) {
                    String compact = WHITESPACE.matcher(sql.trim()).replaceAll(" ");
                    log.debug("{}", (Object)compact);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return invocation.proceed();
    }

    public Object plugin(Object target) {
        return Plugin.wrap((Object)target, (Interceptor)this);
    }

    public void setProperties(Properties properties) {
    }
}

