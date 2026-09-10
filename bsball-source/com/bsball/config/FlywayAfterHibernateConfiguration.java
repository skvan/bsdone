/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.FlywayAfterHibernateConfiguration
 *  lombok.Generated
 *  org.flywaydb.core.Flyway
 *  org.flywaydb.core.api.configuration.FluentConfiguration
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.ObjectProvider
 *  org.springframework.boot.ApplicationRunner
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.boot.context.properties.EnableConfigurationProperties
 *  org.springframework.boot.context.properties.PropertyMapper
 *  org.springframework.boot.flyway.autoconfigure.FlywayProperties
 *  org.springframework.boot.jdbc.DataSourceBuilder
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.context.annotation.Profile
 *  org.springframework.core.annotation.Order
 *  org.springframework.core.io.ResourceLoader
 *  org.springframework.jdbc.datasource.SimpleDriverDataSource
 *  org.springframework.util.CollectionUtils
 *  org.springframework.util.StringUtils
 */
package com.bsball.config;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.Generated;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/*
 * Exception performing whole class analysis ignored.
 */
@Configuration
@Profile(value={"dev", "prod"})
@EnableConfigurationProperties(value={FlywayProperties.class})
@ConditionalOnProperty(prefix="app.flyway", name={"run-after-hibernate"}, havingValue="true")
public class FlywayAfterHibernateConfiguration {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(FlywayAfterHibernateConfiguration.class);

    @Bean
    @Order(value=-2147483648)
    ApplicationRunner flywayMigrateAfterHibernate(FlywayProperties properties, ResourceLoader resourceLoader, ObjectProvider<DataSource> dataSource) {
        return args -> {
            DataSource migrationDs = FlywayAfterHibernateConfiguration.resolveMigrationDataSource((FlywayProperties)properties, (ObjectProvider)dataSource);
            FluentConfiguration configuration = new FluentConfiguration(resourceLoader.getClassLoader());
            configuration.dataSource(migrationDs);
            FlywayAfterHibernateConfiguration.applyFlywayProperties((FluentConfiguration)configuration, (FlywayProperties)properties);
            Flyway flyway = configuration.load();
            log.info("Flyway\uff1a\u5728 Hibernate \u5efa\u8868\u4e4b\u540e\u6267\u884c\u8fc1\u79fb\uff08run-after-hibernate\uff09");
            flyway.migrate();
        };
    }

    private static DataSource resolveMigrationDataSource(FlywayProperties properties, ObjectProvider<DataSource> dataSource) {
        if (StringUtils.hasText((String)properties.getUrl())) {
            DataSourceBuilder builder = DataSourceBuilder.create().type(SimpleDriverDataSource.class);
            builder.url(properties.getUrl());
            builder.username(properties.getUser());
            builder.password(properties.getPassword());
            if (StringUtils.hasText((String)properties.getDriverClassName())) {
                builder.driverClassName(properties.getDriverClassName());
            }
            return builder.build();
        }
        return (DataSource)dataSource.getObject();
    }

    private static void applyFlywayProperties(FluentConfiguration configuration, FlywayProperties properties) {
        PropertyMapper map = PropertyMapper.get();
        List locs = properties.getLocations();
        if (locs == null || locs.isEmpty()) {
            configuration.locations(new String[]{"classpath:db/migration/postgresql"});
        } else {
            configuration.locations(locs.toArray(new String[0]));
        }
        map.from((Object)properties.isFailOnMissingLocations()).to(arg_0 -> ((FluentConfiguration)configuration).failOnMissingLocations(arg_0));
        map.from((Object)properties.getEncoding()).to(arg_0 -> ((FluentConfiguration)configuration).encoding(arg_0));
        map.from((Object)properties.getConnectRetries()).to(arg_0 -> ((FluentConfiguration)configuration).connectRetries(arg_0));
        map.from((Object)properties.getConnectRetriesInterval()).as(Duration::getSeconds).as(Long::intValue).to(arg_0 -> ((FluentConfiguration)configuration).connectRetriesInterval(arg_0));
        map.from((Object)properties.getLockRetryCount()).to(arg_0 -> ((FluentConfiguration)configuration).lockRetryCount(arg_0));
        map.from((Object)properties.getDefaultSchema()).to(arg_0 -> ((FluentConfiguration)configuration).defaultSchema(arg_0));
        map.from((Object)properties.getSchemas()).as(StringUtils::toStringArray).to(arg_0 -> ((FluentConfiguration)configuration).schemas(arg_0));
        map.from((Object)properties.isCreateSchemas()).to(arg_0 -> ((FluentConfiguration)configuration).createSchemas(arg_0));
        map.from((Object)properties.getTable()).to(arg_0 -> ((FluentConfiguration)configuration).table(arg_0));
        map.from((Object)properties.getTablespace()).to(arg_0 -> ((FluentConfiguration)configuration).tablespace(arg_0));
        map.from((Object)properties.getBaselineDescription()).to(arg_0 -> ((FluentConfiguration)configuration).baselineDescription(arg_0));
        map.from((Object)properties.getBaselineVersion()).to(arg_0 -> ((FluentConfiguration)configuration).baselineVersion(arg_0));
        map.from((Object)properties.getInstalledBy()).to(arg_0 -> ((FluentConfiguration)configuration).installedBy(arg_0));
        map.from((Object)properties.getPlaceholders()).to(arg_0 -> ((FluentConfiguration)configuration).placeholders(arg_0));
        map.from((Object)properties.getPlaceholderPrefix()).to(arg_0 -> ((FluentConfiguration)configuration).placeholderPrefix(arg_0));
        map.from((Object)properties.getPlaceholderSuffix()).to(arg_0 -> ((FluentConfiguration)configuration).placeholderSuffix(arg_0));
        map.from((Object)properties.getPlaceholderSeparator()).to(arg_0 -> ((FluentConfiguration)configuration).placeholderSeparator(arg_0));
        map.from((Object)properties.isPlaceholderReplacement()).to(arg_0 -> ((FluentConfiguration)configuration).placeholderReplacement(arg_0));
        map.from((Object)properties.getSqlMigrationPrefix()).to(arg_0 -> ((FluentConfiguration)configuration).sqlMigrationPrefix(arg_0));
        map.from((Object)properties.getSqlMigrationSuffixes()).as(StringUtils::toStringArray).to(arg_0 -> ((FluentConfiguration)configuration).sqlMigrationSuffixes(arg_0));
        map.from((Object)properties.getSqlMigrationSeparator()).to(arg_0 -> ((FluentConfiguration)configuration).sqlMigrationSeparator(arg_0));
        map.from((Object)properties.getRepeatableSqlMigrationPrefix()).to(arg_0 -> ((FluentConfiguration)configuration).repeatableSqlMigrationPrefix(arg_0));
        map.from((Object)properties.getTarget()).to(arg_0 -> ((FluentConfiguration)configuration).target(arg_0));
        map.from((Object)properties.isBaselineOnMigrate()).to(arg_0 -> ((FluentConfiguration)configuration).baselineOnMigrate(arg_0));
        map.from((Object)properties.isCleanDisabled()).to(arg_0 -> ((FluentConfiguration)configuration).cleanDisabled(arg_0));
        map.from((Object)properties.isGroup()).to(arg_0 -> ((FluentConfiguration)configuration).group(arg_0));
        map.from((Object)properties.isMixed()).to(arg_0 -> ((FluentConfiguration)configuration).mixed(arg_0));
        map.from((Object)properties.isOutOfOrder()).to(arg_0 -> ((FluentConfiguration)configuration).outOfOrder(arg_0));
        map.from((Object)properties.isSkipDefaultCallbacks()).to(arg_0 -> ((FluentConfiguration)configuration).skipDefaultCallbacks(arg_0));
        map.from((Object)properties.isSkipDefaultResolvers()).to(arg_0 -> ((FluentConfiguration)configuration).skipDefaultResolvers(arg_0));
        map.from((Object)properties.isValidateMigrationNaming()).to(arg_0 -> ((FluentConfiguration)configuration).validateMigrationNaming(arg_0));
        map.from((Object)properties.isValidateOnMigrate()).to(arg_0 -> ((FluentConfiguration)configuration).validateOnMigrate(arg_0));
        map.from((Object)properties.getInitSqls()).whenNot(CollectionUtils::isEmpty).as(initSqls -> StringUtils.collectionToDelimitedString((Collection)initSqls, (String)"\n")).to(arg_0 -> ((FluentConfiguration)configuration).initSql(arg_0));
        map.from((Object)properties.getScriptPlaceholderPrefix()).to(arg_0 -> ((FluentConfiguration)configuration).scriptPlaceholderPrefix(arg_0));
        map.from((Object)properties.getScriptPlaceholderSuffix()).to(arg_0 -> ((FluentConfiguration)configuration).scriptPlaceholderSuffix(arg_0));
        map.from(() -> ((FlywayProperties)properties).getLoggers()).to(arg_0 -> ((FluentConfiguration)configuration).loggers(arg_0));
        map.from((Object)properties.getBatch()).to(arg_0 -> ((FluentConfiguration)configuration).batch(arg_0));
        map.from((Object)properties.getDryRunOutput()).to(arg_0 -> ((FluentConfiguration)configuration).dryRunOutput(arg_0));
        map.from((Object)properties.getErrorOverrides()).to(arg_0 -> ((FluentConfiguration)configuration).errorOverrides(arg_0));
        map.from((Object)properties.getStream()).to(arg_0 -> ((FluentConfiguration)configuration).stream(arg_0));
        map.from((Object)properties.getJdbcProperties()).whenNot(Map::isEmpty).to(arg_0 -> ((FluentConfiguration)configuration).jdbcProperties(arg_0));
        map.from((Object)properties.getKerberosConfigFile()).to(arg_0 -> ((FluentConfiguration)configuration).kerberosConfigFile(arg_0));
        map.from((Object)properties.getOutputQueryResults()).to(arg_0 -> ((FluentConfiguration)configuration).outputQueryResults(arg_0));
        map.from((Object)properties.getSkipExecutingMigrations()).to(arg_0 -> ((FluentConfiguration)configuration).skipExecutingMigrations(arg_0));
        map.from((Object)properties.getIgnoreMigrationPatterns()).whenNot(CollectionUtils::isEmpty).to(patterns -> configuration.ignoreMigrationPatterns(patterns.toArray(new String[0])));
        map.from((Object)properties.getDetectEncoding()).to(arg_0 -> ((FluentConfiguration)configuration).detectEncoding(arg_0));
        try {
            map.from((Object)properties.isExecuteInTransaction()).to(arg_0 -> ((FluentConfiguration)configuration).executeInTransaction(arg_0));
        }
        catch (NoSuchMethodError noSuchMethodError) {
            // empty catch block
        }
    }
}

