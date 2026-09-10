/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.DbMigrationRunner
 *  com.bsball.core.DbMigrationRunner$ApiDef
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysApi
 *  com.bsball.model.entity.SysDictData
 *  com.bsball.model.entity.SysDictType
 *  com.bsball.model.entity.SysMenu
 *  com.bsball.model.entity.SysRole
 *  com.bsball.model.entity.SysRoleApi
 *  com.bsball.repository.SysApiRepository
 *  com.bsball.repository.SysDictDataRepository
 *  com.bsball.repository.SysDictTypeRepository
 *  com.bsball.repository.SysMenuRepository
 *  com.bsball.repository.SysRoleApiRepository
 *  com.bsball.repository.SysRoleRepository
 *  com.bsball.utils.ApiGroupUtil
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.core.annotation.Order
 *  org.springframework.jdbc.core.JdbcTemplate
 *  org.springframework.stereotype.Component
 */
package com.bsball.core;

import com.bsball.core.DbMigrationRunner;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysDictData;
import com.bsball.model.entity.SysDictType;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.repository.SysApiRepository;
import com.bsball.repository.SysDictDataRepository;
import com.bsball.repository.SysDictTypeRepository;
import com.bsball.repository.SysMenuRepository;
import com.bsball.repository.SysRoleApiRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.utils.ApiGroupUtil;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
@Order(value=1)
public class DbMigrationRunner
implements CommandLineRunner {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(DbMigrationRunner.class);
    private final DataSource dataSource;
    private final SysApiRepository sysApiRepository;
    private final SysMenuRepository sysMenuRepository;
    private final SysDictTypeRepository sysDictTypeRepository;
    private final SysDictDataRepository sysDictDataRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysRoleApiRepository sysRoleApiRepository;
    private static final List<String> PITCH_COLS = List.of((Object)"pitch_h", (Object)"pitch_bb", (Object)"pitch_hr", (Object)"pitch_hbp", (Object)"pitch_ibb", (Object)"pitch_pa", (Object)"pitch_bf", (Object)"pitch_r");
    private static final List<String> FIELDING_EXTRA_COLS = List.of((Object)"fielding_gs", (Object)"def_inn", (Object)"dp");
    private static final Map<String, String> FIELDING_EXTRA_COMMENTS = Map.of((Object)"fielding_gs", (Object)"\u5b88\u5907\u5148\u53d1\uff08\u8be5\u573a\u8be5\u4f4d\u7f6e\u662f\u5426\u5148\u53d1\uff0c0/1\uff09", (Object)"def_inn", (Object)"\u5b88\u5907\u5c40\u6570", (Object)"dp", (Object)"\u53c2\u4e0e\u53cc\u6740");
    private static final List<String> CATCHER_STAT_COLS = List.of((Object)"pb", (Object)"catcher_sb", (Object)"catcher_cs");
    private static final Map<String, String> CATCHER_STAT_COMMENTS = Map.of((Object)"pb", (Object)"\u6355\u9038", (Object)"catcher_sb", (Object)"\u6355\u624b\u5b88\u5907\uff1a\u76d7\u5792\u6210\u529f\uff08\u5bf9\u624b\uff09", (Object)"catcher_cs", (Object)"\u6355\u624b\u5b88\u5907\uff1a\u76d7\u5792\u88ab\u6740");

    public void run(String ... args) {
        try {
            this.migrateGamePlayerStat();
            this.migrateGameExtraMeta();
            this.migrateGameRecorders();
            this.migrateGameTag();
            this.migrateHistoryAndHighlight();
            this.migrateSysNoticeViewCount();
            this.migrateSysArticleSourceUrlAndSubmitIp();
            this.ensureApiGroups();
            this.ensureGuestHasNoticeArticleCoachApis();
            this.ensureGuestHasPlayerStatsGetApis();
            this.ensureGuestHasStadiumGetApis();
            this.ensureGuestHasPortalBusinessGetApis();
            this.ensureGuestHasPortalDevtoolsPostApi();
            this.ensureDictTypes();
            this.ensureApiGroupContentManagementLabel();
            this.migrateHistoryRecordMenuData();
            this.migrateAdminMenuLayout();
            this.migrateSysMenuRbac();
        }
        catch (Exception e) {
            log.warn("DbMigration \u6267\u884c\u5931\u8d25: {}", (Object)e.getMessage());
        }
    }

    private void migrateSysMenuRbac() {
        try {
            String driver;
            JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);
            try (Connection conn = this.dataSource.getConnection();){
                driver = conn.getMetaData().getDriverName();
            }
            String dl = driver.toLowerCase();
            if (dl.contains("sqlite")) {
                if (!this.columnExistsSqlite(jdbc, "sys_menu", "menu_type")) {
                    jdbc.execute("ALTER TABLE sys_menu ADD COLUMN menu_type INTEGER NOT NULL DEFAULT 2");
                    log.info("sys_menu \u6dfb\u52a0\u5217: menu_type");
                }
                if (!this.columnExistsSqlite(jdbc, "sys_menu", "permission")) {
                    jdbc.execute("ALTER TABLE sys_menu ADD COLUMN permission VARCHAR(100) NULL");
                    log.info("sys_menu \u6dfb\u52a0\u5217: permission");
                }
            } else if (dl.contains("mysql")) {
                if (!this.columnExistsMysql(jdbc, "sys_menu", "menu_type")) {
                    jdbc.execute("ALTER TABLE sys_menu ADD COLUMN menu_type INT NOT NULL DEFAULT 2 COMMENT '1\u76ee\u5f552\u83dc\u53553\u6309\u94ae'");
                    log.info("sys_menu \u6dfb\u52a0\u5217: menu_type");
                }
                if (!this.columnExistsMysql(jdbc, "sys_menu", "permission")) {
                    jdbc.execute("ALTER TABLE sys_menu ADD COLUMN permission VARCHAR(100) NULL COMMENT '\u6743\u9650\u6807\u8bc6'");
                    log.info("sys_menu \u6dfb\u52a0\u5217: permission");
                }
            } else if (dl.contains("postgresql")) {
                if (!this.columnExistsPostgres(jdbc, "sys_menu", "menu_type")) {
                    jdbc.execute("ALTER TABLE sys_menu ADD COLUMN menu_type INTEGER NOT NULL DEFAULT 2");
                    this.tryExecutePostgresDdl(jdbc, "COMMENT ON COLUMN sys_menu.menu_type IS '1\u76ee\u5f552\u83dc\u53553\u6309\u94ae'");
                    log.info("sys_menu \u6dfb\u52a0\u5217: menu_type\uff08PostgreSQL\uff09");
                }
                if (!this.columnExistsPostgres(jdbc, "sys_menu", "permission")) {
                    jdbc.execute("ALTER TABLE sys_menu ADD COLUMN permission VARCHAR(100) NULL");
                    this.tryExecutePostgresDdl(jdbc, "COMMENT ON COLUMN sys_menu.permission IS '\u6743\u9650\u6807\u8bc6'");
                    log.info("sys_menu \u6dfb\u52a0\u5217: permission\uff08PostgreSQL\uff09");
                }
            }
        }
        catch (Exception e) {
            log.debug("migrateSysMenuRbac \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateHistoryRecordMenuData() {
        try {
            List all = this.sysMenuRepository.findAll();
            boolean changed = false;
            for (SysMenu m : all) {
                boolean isHistoryMenu = "\u6cbf\u9769\u8bb0\u5f55".equals(m.getTitle()) || "\u6cbf\u9769\u8bb0\u5f55".equals(m.getName()) || "/admin/personnel-changes".equals(m.getPath()) || "/admin/personnel-change".equals(m.getPath()) || "/admin/history-record".equals(m.getPath()) || "/admin/history-records".equals(m.getPath()) || "AdminPersonnelChanges".equals(m.getRouteName()) || "AdminHistoryRecords".equals(m.getRouteName()) || "views/admin/business/PersonnelChangeList.vue".equals(m.getComponent()) || "views/admin/business/HistoryRecordList.vue".equals(m.getComponent());
                if (!isHistoryMenu) continue;
                if (!"\u6cbf\u9769\u8bb0\u5f55".equals(m.getName())) {
                    m.setName("\u6cbf\u9769\u8bb0\u5f55");
                    changed = true;
                }
                if (!"\u6cbf\u9769\u8bb0\u5f55".equals(m.getTitle())) {
                    m.setTitle("\u6cbf\u9769\u8bb0\u5f55");
                    changed = true;
                }
                if (!"/admin/history-records".equals(m.getPath())) {
                    m.setPath("/admin/history-records");
                    changed = true;
                }
                if (!"AdminHistoryRecords".equals(m.getRouteName())) {
                    m.setRouteName("AdminHistoryRecords");
                    changed = true;
                }
                if ("views/admin/business/HistoryRecordList.vue".equals(m.getComponent())) continue;
                m.setComponent("views/admin/business/HistoryRecordList.vue");
                changed = true;
            }
            if (changed) {
                this.sysMenuRepository.saveAll((Iterable)all);
                log.info("\u5df2\u4fee\u6b63\u83dc\u5355\u6570\u636e\uff1a\u6cbf\u9769\u8bb0\u5f55\u7edf\u4e00\u4e3a /admin/history-records");
            }
        }
        catch (Exception e) {
            log.debug("migrateHistoryRecordMenuData \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateAdminMenuLayout() {
        try {
            List all = this.sysMenuRepository.findAll();
            SysMenu systemRoot = all.stream().filter(m -> "/system".equals(m.getPath()) || "\u7cfb\u7edf\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
            if (systemRoot == null) {
                return;
            }
            boolean changed = false;
            for (SysMenu m2 : all) {
                if (!"/admin/monitor/ip-access-policy".equals(m2.getPath())) continue;
                if (!systemRoot.getId().equals(m2.getParentId())) {
                    m2.setParentId(systemRoot.getId());
                    changed = true;
                }
                if ("IP\u7b56\u7565\u7ba1\u7406".equals(m2.getName()) && "IP\u7b56\u7565\u7ba1\u7406".equals(m2.getTitle())) continue;
                m2.setName("IP\u7b56\u7565\u7ba1\u7406");
                m2.setTitle("IP\u7b56\u7565\u7ba1\u7406");
                changed = true;
            }
            List systemChildren = all.stream().filter(m -> systemRoot.getId().equals(m.getParentId())).sorted(Comparator.comparingInt(SysMenu::getSort).thenComparing(BaseEntity::getId)).collect(Collectors.toCollection(ArrayList::new));
            if (systemChildren.isEmpty()) {
                if (changed) {
                    this.sysMenuRepository.saveAll((Iterable)all);
                    log.info("\u5df2\u8fc1\u79fb\u540e\u53f0\u83dc\u5355\u5e03\u5c40\uff08\u4ec5\u66f4\u65b0 IP\u7b56\u7565\u7ba1\u7406 \u8282\u70b9\uff09");
                }
                return;
            }
            Map<String, SysMenu> byPath = systemChildren.stream().filter(m -> m.getPath() != null).collect(Collectors.toMap(SysMenu::getPath, m -> m, (a, b) -> a));
            ArrayList<SysMenu> reordered = new ArrayList<SysMenu>();
            HashSet<Long> used = new HashSet<Long>();
            String[] topPreferred = new String[]{"/admin/tenants", "/admin/config", "/admin/users"};
            for (String p : topPreferred) {
                SysMenu m3 = byPath.get(p);
                if (m3 == null || !used.add(m3.getId())) continue;
                reordered.add(m3);
            }
            for (SysMenu m4 : systemChildren) {
                if (!used.add(m4.getId())) continue;
                reordered.add(m4);
            }
            SysMenu apiMenu = byPath.get("/admin/apis");
            SysMenu ipPolicyMenu = byPath.get("/admin/monitor/ip-access-policy");
            if (apiMenu != null && ipPolicyMenu != null) {
                reordered.removeIf(m -> Objects.equals(m.getId(), ipPolicyMenu.getId()));
                int apiIdx = -1;
                for (int i = 0; i < reordered.size(); ++i) {
                    if (!Objects.equals(((SysMenu)reordered.get(i)).getId(), apiMenu.getId())) continue;
                    apiIdx = i;
                    break;
                }
                if (apiIdx >= 0 && apiIdx + 1 <= reordered.size()) {
                    reordered.add(apiIdx + 1, ipPolicyMenu);
                } else {
                    reordered.add(ipPolicyMenu);
                }
            }
            SysMenu geoMenu = byPath.get("/admin/ip-location-cache");
            SysMenu ipPolicyForGeo = byPath.get("/admin/monitor/ip-access-policy");
            if (geoMenu != null && ipPolicyForGeo != null) {
                reordered.removeIf(m -> Objects.equals(m.getId(), geoMenu.getId()));
                int ipPolicyIdx = -1;
                for (int i = 0; i < reordered.size(); ++i) {
                    if (!Objects.equals(((SysMenu)reordered.get(i)).getId(), ipPolicyForGeo.getId())) continue;
                    ipPolicyIdx = i;
                    break;
                }
                if (ipPolicyIdx >= 0) {
                    reordered.add(ipPolicyIdx + 1, geoMenu);
                } else {
                    reordered.add(geoMenu);
                }
            }
            for (int i = 0; i < reordered.size(); ++i) {
                SysMenu m5 = (SysMenu)reordered.get(i);
                if (m5.getSort() == i) continue;
                m5.setSort(Integer.valueOf(i));
                changed = true;
            }
            if (changed) {
                this.sysMenuRepository.saveAll((Iterable)all);
                log.info("\u5df2\u8fc1\u79fb\u540e\u53f0\u83dc\u5355\u5e03\u5c40\uff1aIP\u7b56\u7565\u7ba1\u7406\u4f4d\u7f6e/\u540d\u79f0\u3001\u79df\u6237\u7ba1\u7406\u4e0e\u914d\u7f6e\u7ba1\u7406\u987a\u5e8f\u5df2\u66f4\u65b0");
            }
        }
        catch (Exception e) {
            log.debug("migrateAdminMenuLayout \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateSysNoticeViewCount() {
        try {
            String driver;
            JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);
            try (Connection conn = this.dataSource.getConnection();){
                driver = conn.getMetaData().getDriverName();
            }
            String dl = driver.toLowerCase();
            if (dl.contains("sqlite")) {
                if (!this.columnExistsSqlite(jdbc, "sys_article", "view_count")) {
                    jdbc.execute("ALTER TABLE sys_article ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0");
                    log.info("sys_article \u6dfb\u52a0\u5217: view_count");
                }
            } else if (dl.contains("mysql")) {
                if (!this.columnExistsMysql(jdbc, "sys_article", "view_count")) {
                    jdbc.execute("ALTER TABLE sys_article ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0 COMMENT '\u6d4f\u89c8\u6b21\u6570'");
                    log.info("sys_article \u6dfb\u52a0\u5217: view_count");
                }
            } else if (dl.contains("postgresql") || dl.contains("postgres")) {
                this.migrateSysNoticeViewCountPostgres(jdbc);
            }
        }
        catch (Exception e) {
            log.debug("migrateSysNoticeViewCount \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateSysArticleSourceUrlAndSubmitIp() {
        try {
            String driver;
            JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);
            try (Connection conn = this.dataSource.getConnection();){
                driver = conn.getMetaData().getDriverName();
            }
            String dl = driver.toLowerCase();
            if (dl.contains("sqlite")) {
                if (!this.columnExistsSqlite(jdbc, "sys_article", "source_url")) {
                    jdbc.execute("ALTER TABLE sys_article ADD COLUMN source_url VARCHAR(2000)");
                    log.info("sys_article \u6dfb\u52a0\u5217: source_url");
                }
                if (!this.columnExistsSqlite(jdbc, "sys_article", "submit_ip")) {
                    jdbc.execute("ALTER TABLE sys_article ADD COLUMN submit_ip VARCHAR(128)");
                    log.info("sys_article \u6dfb\u52a0\u5217: submit_ip");
                }
                if (!this.columnExistsSqlite(jdbc, "sys_article", "submit_ip_region")) {
                    jdbc.execute("ALTER TABLE sys_article ADD COLUMN submit_ip_region VARCHAR(512)");
                    log.info("sys_article \u6dfb\u52a0\u5217: submit_ip_region");
                }
            } else if (dl.contains("mysql")) {
                if (!this.columnExistsMysql(jdbc, "sys_article", "source_url")) {
                    jdbc.execute("ALTER TABLE sys_article ADD COLUMN source_url VARCHAR(2000) NULL COMMENT '\u539f\u6587\u94fe\u63a5\uff08\u5916\u94fe\uff09'");
                    log.info("sys_article \u6dfb\u52a0\u5217: source_url");
                }
                if (!this.columnExistsMysql(jdbc, "sys_article", "submit_ip")) {
                    jdbc.execute("ALTER TABLE sys_article ADD COLUMN submit_ip VARCHAR(128) NULL COMMENT '\u53d1\u8868/\u4fdd\u5b58\u65f6\u5ba2\u6237\u7aefIP'");
                    log.info("sys_article \u6dfb\u52a0\u5217: submit_ip");
                }
                if (!this.columnExistsMysql(jdbc, "sys_article", "submit_ip_region")) {
                    jdbc.execute("ALTER TABLE sys_article ADD COLUMN submit_ip_region VARCHAR(512) NULL COMMENT '\u53d1\u8868/\u4fdd\u5b58\u65f6IP\u5f52\u5c5e\u5730'");
                    log.info("sys_article \u6dfb\u52a0\u5217: submit_ip_region");
                }
            } else if (dl.contains("postgresql") || dl.contains("postgres")) {
                this.migrateSysArticleSourceUrlAndSubmitIpPostgres(jdbc);
            }
        }
        catch (Exception e) {
            log.debug("migrateSysArticleSourceUrlAndSubmitIp \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateSysArticleSourceUrlAndSubmitIpPostgres(JdbcTemplate jdbc) {
        if (!this.columnExistsPostgres(jdbc, "sys_article", "source_url")) {
            jdbc.execute("ALTER TABLE sys_article ADD COLUMN source_url VARCHAR(2000)");
            log.info("sys_article \u6dfb\u52a0\u5217: source_url\uff08PostgreSQL\uff09");
        }
        if (!this.columnExistsPostgres(jdbc, "sys_article", "submit_ip")) {
            jdbc.execute("ALTER TABLE sys_article ADD COLUMN submit_ip VARCHAR(128)");
            log.info("sys_article \u6dfb\u52a0\u5217: submit_ip\uff08PostgreSQL\uff09");
        }
        if (!this.columnExistsPostgres(jdbc, "sys_article", "submit_ip_region")) {
            jdbc.execute("ALTER TABLE sys_article ADD COLUMN submit_ip_region VARCHAR(512)");
            log.info("sys_article \u6dfb\u52a0\u5217: submit_ip_region\uff08PostgreSQL\uff09");
        }
        try {
            jdbc.execute("COMMENT ON COLUMN sys_article.source_url IS '\u539f\u6587\u94fe\u63a5\uff08\u5916\u94fe\uff09'");
            jdbc.execute("COMMENT ON COLUMN sys_article.submit_ip IS '\u53d1\u8868/\u4fdd\u5b58\u65f6\u5ba2\u6237\u7aef IP\uff08\u670d\u52a1\u7aef\u89e3\u6790\uff09'");
            jdbc.execute("COMMENT ON COLUMN sys_article.submit_ip_region IS '\u53d1\u8868/\u4fdd\u5b58\u65f6 IP \u5f52\u5c5e\u5730\u5c55\u793a\uff08\u670d\u52a1\u7aef\u89e3\u6790\uff09'");
        }
        catch (Exception ex) {
            log.debug("sys_article \u65b0\u5217 COMMENT \u8df3\u8fc7: {}", (Object)ex.getMessage());
        }
    }

    private void migrateGamePlayerStat() {
        try {
            String driver;
            JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);
            try (Connection conn = this.dataSource.getConnection();){
                driver = conn.getMetaData().getDriverName();
            }
            String driverLower = driver.toLowerCase();
            if (driverLower.contains("sqlite")) {
                this.migrateGamePlayerStatSqlite(jdbc);
            } else if (driverLower.contains("mysql")) {
                this.migrateGamePlayerStatMysql(jdbc);
            }
        }
        catch (Exception e) {
            log.debug("migrateGamePlayerStat \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateGameExtraMeta() {
        try {
            String driver;
            JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);
            try (Connection conn = this.dataSource.getConnection();){
                driver = conn.getMetaData().getDriverName();
            }
            String dl = driver.toLowerCase();
            if (dl.contains("sqlite")) {
                this.migrateGameExtraMetaSqlite(jdbc);
            } else if (dl.contains("mysql")) {
                this.migrateGameExtraMetaMysql(jdbc);
            } else if (dl.contains("postgresql") || dl.contains("postgres")) {
                this.migrateGameExtraMetaPostgres(jdbc);
            }
        }
        catch (Exception e) {
            log.debug("migrateGameExtraMeta \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateGameRecorders() {
        try {
            String driver;
            JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);
            try (Connection conn = this.dataSource.getConnection();){
                driver = conn.getMetaData().getDriverName();
            }
            String dl = driver.toLowerCase();
            if (dl.contains("sqlite")) {
                this.addColSqlite(jdbc, "recorders", "TEXT");
            } else if (dl.contains("mysql")) {
                this.addColMysql(jdbc, "recorders", "VARCHAR(500) NULL COMMENT '\u8bb0\u5f55\u5458\uff0c\u591a\u4eba\u9017\u53f7\u6216\u987f\u53f7\u5206\u9694'");
            } else if (dl.contains("postgresql") || dl.contains("postgres")) {
                this.addColPostgres(jdbc, "recorders", "VARCHAR(500)", "\u8bb0\u5f55\u5458\uff0c\u591a\u4eba\u53ef\u7528\u9017\u53f7\u6216\u987f\u53f7\u5206\u9694");
            }
        }
        catch (Exception e) {
            log.debug("migrateGameRecorders \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateGameTag() {
        try {
            String driver;
            JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);
            try (Connection conn = this.dataSource.getConnection();){
                driver = conn.getMetaData().getDriverName();
            }
            String dl = driver.toLowerCase();
            if (dl.contains("sqlite")) {
                this.addColSqlite(jdbc, "game_tag", "TEXT");
            } else if (dl.contains("mysql")) {
                this.addColMysql(jdbc, "game_tag", "VARCHAR(64) NULL COMMENT '\u573a\u6b21\u6807\u7b7e\uff0c\u5982\u51a0\u519b\u8d5b'");
            } else if (dl.contains("postgresql") || dl.contains("postgres")) {
                this.addColPostgres(jdbc, "game_tag", "VARCHAR(64)", "\u573a\u6b21\u6807\u7b7e\uff0c\u5982\u51a0\u519b\u8d5b\uff1b\u6709\u503c\u65f6\u524d\u53f0\u663e\u793a\u5956\u676f\u6807\u8bc6");
            }
        }
        catch (Exception e) {
            log.debug("migrateGameTag \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateHistoryAndHighlight() {
        try {
            String driver;
            JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);
            try (Connection conn = this.dataSource.getConnection();){
                driver = conn.getMetaData().getDriverName();
            }
            String dl = driver.toLowerCase();
            if (dl.contains("sqlite")) {
                if (!this.columnExistsSqlite(jdbc, "bs_personnel_change", "record_type")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN record_type VARCHAR(32) NOT NULL DEFAULT 'event'");
                }
                if (!this.columnExistsSqlite(jdbc, "bs_personnel_change", "snapshot_json")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN snapshot_json TEXT");
                }
                if (!this.columnExistsSqlite(jdbc, "bs_personnel_change", "related_object_type")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN related_object_type VARCHAR(32)");
                }
                if (!this.columnExistsSqlite(jdbc, "bs_personnel_change", "related_object_id")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN related_object_id BIGINT");
                }
                if (!this.columnExistsSqlite(jdbc, "bs_personnel_change", "change_payload_json")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN change_payload_json TEXT");
                }
                if (this.columnExistsSqlite(jdbc, "bs_highlight_moment", "id") && !this.columnExistsSqlite(jdbc, "bs_highlight_moment", "display_key")) {
                    jdbc.execute("ALTER TABLE bs_highlight_moment ADD COLUMN display_key VARCHAR(64) NOT NULL DEFAULT 'player_profile'");
                }
            } else if (dl.contains("mysql")) {
                if (!this.columnExistsMysql(jdbc, "bs_personnel_change", "record_type")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN record_type VARCHAR(32) NOT NULL DEFAULT 'event' COMMENT '\u8bb0\u5f55\u7c7b\u578b\uff1aevent/snapshot'");
                }
                if (!this.columnExistsMysql(jdbc, "bs_personnel_change", "snapshot_json")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN snapshot_json TEXT NULL COMMENT '\u5feb\u7167JSON'");
                }
                if (!this.columnExistsMysql(jdbc, "bs_personnel_change", "related_object_type")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN related_object_type VARCHAR(32) NULL COMMENT '\u5173\u8054\u5bf9\u8c61\u7c7b\u578b'");
                }
                if (!this.columnExistsMysql(jdbc, "bs_personnel_change", "related_object_id")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN related_object_id BIGINT NULL COMMENT '\u5173\u8054\u5bf9\u8c61ID'");
                }
                if (!this.columnExistsMysql(jdbc, "bs_personnel_change", "change_payload_json")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN change_payload_json TEXT NULL COMMENT '\u53d8\u66f4\u5185\u5bb9JSON'");
                }
                if (this.columnExistsMysql(jdbc, "bs_highlight_moment", "id") && !this.columnExistsMysql(jdbc, "bs_highlight_moment", "display_key")) {
                    jdbc.execute("ALTER TABLE bs_highlight_moment ADD COLUMN display_key VARCHAR(64) NOT NULL DEFAULT 'player_profile' COMMENT '\u5c55\u793a\u4f4d\u952e'");
                }
            } else if (dl.contains("postgresql") || dl.contains("postgres")) {
                if (!this.columnExistsPostgres(jdbc, "bs_personnel_change", "record_type")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN record_type VARCHAR(32) NOT NULL DEFAULT 'event'");
                }
                if (!this.columnExistsPostgres(jdbc, "bs_personnel_change", "snapshot_json")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN snapshot_json TEXT");
                }
                if (!this.columnExistsPostgres(jdbc, "bs_personnel_change", "related_object_type")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN related_object_type VARCHAR(32)");
                }
                if (!this.columnExistsPostgres(jdbc, "bs_personnel_change", "related_object_id")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN related_object_id BIGINT");
                }
                if (!this.columnExistsPostgres(jdbc, "bs_personnel_change", "change_payload_json")) {
                    jdbc.execute("ALTER TABLE bs_personnel_change ADD COLUMN change_payload_json TEXT");
                }
                if (this.columnExistsPostgres(jdbc, "bs_highlight_moment", "id") && !this.columnExistsPostgres(jdbc, "bs_highlight_moment", "display_key")) {
                    jdbc.execute("ALTER TABLE bs_highlight_moment ADD COLUMN display_key VARCHAR(64) NOT NULL DEFAULT 'player_profile'");
                }
            }
        }
        catch (Exception e) {
            log.debug("migrateHistoryAndHighlight \u8df3\u8fc7: {}", (Object)e.getMessage());
        }
    }

    private void migrateGameExtraMetaSqlite(JdbcTemplate jdbc) {
        this.addColSqlite(jdbc, "spectator_count", "INTEGER");
        this.addColSqlite(jdbc, "umpire_hp", "TEXT");
        this.addColSqlite(jdbc, "umpire_1b", "TEXT");
        this.addColSqlite(jdbc, "umpire_2b", "TEXT");
        this.addColSqlite(jdbc, "umpire_3b", "TEXT");
        this.addColSqlite(jdbc, "weather_summary", "TEXT");
        this.addColSqlite(jdbc, "weather_temp_c", "REAL");
        this.addColSqlite(jdbc, "weather_wind", "TEXT");
        this.addColSqlite(jdbc, "weather_rain_prob_pct", "INTEGER");
    }

    private void addColSqlite(JdbcTemplate jdbc, String col, String sqlType) {
        if (!this.columnExistsSqlite(jdbc, "bs_game", col)) {
            jdbc.execute("ALTER TABLE bs_game ADD COLUMN " + col + " " + sqlType);
            log.info("bs_game \u6dfb\u52a0\u5217: {}\uff08SQLite\uff09", (Object)col);
        }
    }

    private void migrateGameExtraMetaMysql(JdbcTemplate jdbc) {
        this.addColMysql(jdbc, "spectator_count", "INT NULL COMMENT '\u89c2\u4f17\u4eba\u6570\uff08\u53ef\u7a7a\uff09'");
        this.addColMysql(jdbc, "umpire_hp", "VARCHAR(128) NULL COMMENT '\u4e3b\u5ba1'");
        this.addColMysql(jdbc, "umpire_1b", "VARCHAR(128) NULL COMMENT '\u4e00\u5792\u5ba1'");
        this.addColMysql(jdbc, "umpire_2b", "VARCHAR(128) NULL COMMENT '\u4e8c\u5792\u5ba1'");
        this.addColMysql(jdbc, "umpire_3b", "VARCHAR(128) NULL COMMENT '\u4e09\u5792\u5ba1'");
        this.addColMysql(jdbc, "weather_summary", "VARCHAR(200) NULL COMMENT '\u5929\u6c14\u7b80\u51b5'");
        this.addColMysql(jdbc, "weather_temp_c", "DOUBLE NULL COMMENT '\u6c14\u6e29\u6444\u6c0f\u5ea6'");
        this.addColMysql(jdbc, "weather_wind", "VARCHAR(128) NULL COMMENT '\u98ce\u901f\u98ce\u5411'");
        this.addColMysql(jdbc, "weather_rain_prob_pct", "INT NULL COMMENT '\u9884\u62a5\u964d\u96e8\u6982\u73870-100'");
    }

    private void addColMysql(JdbcTemplate jdbc, String col, String ddlSuffix) {
        if (!this.columnExistsMysql(jdbc, "bs_game", col)) {
            jdbc.execute("ALTER TABLE bs_game ADD COLUMN " + col + " " + ddlSuffix);
            log.info("bs_game \u6dfb\u52a0\u5217: {}\uff08MySQL\uff09", (Object)col);
        }
    }

    private void migrateGameExtraMetaPostgres(JdbcTemplate jdbc) {
        this.addColPostgres(jdbc, "spectator_count", "INTEGER", "\u89c2\u4f17\u4eba\u6570\uff08\u53ef\u7a7a\uff09");
        this.addColPostgres(jdbc, "umpire_hp", "VARCHAR(128)", "\u4e3b\u5ba1");
        this.addColPostgres(jdbc, "umpire_1b", "VARCHAR(128)", "\u4e00\u5792\u5ba1");
        this.addColPostgres(jdbc, "umpire_2b", "VARCHAR(128)", "\u4e8c\u5792\u5ba1");
        this.addColPostgres(jdbc, "umpire_3b", "VARCHAR(128)", "\u4e09\u5792\u5ba1");
        this.addColPostgres(jdbc, "weather_summary", "VARCHAR(200)", "\u5929\u6c14\u7b80\u51b5");
        this.addColPostgres(jdbc, "weather_temp_c", "DOUBLE PRECISION", "\u6c14\u6e29\u6444\u6c0f\u5ea6");
        this.addColPostgres(jdbc, "weather_wind", "VARCHAR(128)", "\u98ce\u901f\u98ce\u5411\u63cf\u8ff0");
        this.addColPostgres(jdbc, "weather_rain_prob_pct", "INTEGER", "\u8d5b\u524d\u9884\u62a5\u964d\u96e8\u6982\u73870-100");
    }

    private void addColPostgres(JdbcTemplate jdbc, String col, String type, String comment) {
        if (!this.columnExistsPostgres(jdbc, "bs_game", col)) {
            jdbc.execute("ALTER TABLE bs_game ADD COLUMN " + col + " " + type);
            log.info("bs_game \u6dfb\u52a0\u5217: {}\uff08PostgreSQL\uff09", (Object)col);
        }
        try {
            String c = comment.replace("'", "''");
            jdbc.execute("COMMENT ON COLUMN bs_game." + col + " IS '" + c + "'");
        }
        catch (Exception ex) {
            log.debug("bs_game.{} COMMENT \u8df3\u8fc7: {}", (Object)col, (Object)ex.getMessage());
        }
    }

    private void migrateGamePlayerStatSqlite(JdbcTemplate jdbc) {
        for (String col : PITCH_COLS) {
            if (this.columnExistsSqlite(jdbc, "bs_game_player_stat", col)) continue;
            jdbc.execute("ALTER TABLE bs_game_player_stat ADD COLUMN " + col + " INTEGER");
            log.info("bs_game_player_stat \u6dfb\u52a0\u5217: {}", (Object)col);
        }
        for (String col : FIELDING_EXTRA_COLS) {
            if (this.columnExistsSqlite(jdbc, "bs_game_player_stat", col)) continue;
            String type = "def_inn".equals(col) ? "REAL" : "INTEGER";
            jdbc.execute("ALTER TABLE bs_game_player_stat ADD COLUMN " + col + " " + type);
            log.info("bs_game_player_stat \u6dfb\u52a0\u5217: {}\uff08SQLite \u4e0d\u652f\u6301\u5217\u6ce8\u91ca\uff09", (Object)col);
        }
        for (String col : CATCHER_STAT_COLS) {
            if (this.columnExistsSqlite(jdbc, "bs_game_player_stat", col)) continue;
            jdbc.execute("ALTER TABLE bs_game_player_stat ADD COLUMN " + col + " INTEGER");
            log.info("bs_game_player_stat \u6dfb\u52a0\u5217: {}", (Object)col);
        }
        if (!this.columnExistsSqlite(jdbc, "bs_game_player_stat", "pitch_inside_park_hr")) {
            jdbc.execute("ALTER TABLE bs_game_player_stat ADD COLUMN pitch_inside_park_hr INTEGER");
            log.info("bs_game_player_stat \u6dfb\u52a0\u5217: pitch_inside_park_hr");
        }
    }

    private void migrateGamePlayerStatMysql(JdbcTemplate jdbc) {
        for (String col : PITCH_COLS) {
            if (this.columnExistsMysql(jdbc, "bs_game_player_stat", col)) continue;
            jdbc.execute("ALTER TABLE bs_game_player_stat ADD COLUMN " + col + " INT NULL");
            log.info("bs_game_player_stat \u6dfb\u52a0\u5217: {}", (Object)col);
        }
        for (String col : FIELDING_EXTRA_COLS) {
            String commentClause;
            String type = "def_inn".equals(col) ? "DOUBLE" : "INT";
            String comment = FIELDING_EXTRA_COMMENTS.getOrDefault(col, "");
            String string = commentClause = comment.isEmpty() ? "" : " COMMENT '" + comment.replace("'", "''") + "'";
            if (!this.columnExistsMysql(jdbc, "bs_game_player_stat", col)) {
                jdbc.execute("ALTER TABLE bs_game_player_stat ADD COLUMN " + col + " " + type + " NULL" + commentClause);
                log.info("bs_game_player_stat \u6dfb\u52a0\u5217: {} \u5907\u6ce8: {}", (Object)col, (Object)comment);
                continue;
            }
            if (comment.isEmpty()) continue;
            jdbc.execute("ALTER TABLE bs_game_player_stat MODIFY COLUMN " + col + " " + type + " NULL" + commentClause);
            log.debug("bs_game_player_stat \u8865\u5907\u6ce8: {}", (Object)col);
        }
        for (String col : CATCHER_STAT_COLS) {
            String commentClause;
            String comment = CATCHER_STAT_COMMENTS.getOrDefault(col, "");
            String string = commentClause = comment.isEmpty() ? "" : " COMMENT '" + comment.replace("'", "''") + "'";
            if (!this.columnExistsMysql(jdbc, "bs_game_player_stat", col)) {
                jdbc.execute("ALTER TABLE bs_game_player_stat ADD COLUMN " + col + " INT NULL" + commentClause);
                log.info("bs_game_player_stat \u6dfb\u52a0\u5217: {} \u5907\u6ce8: {}", (Object)col, (Object)comment);
                continue;
            }
            if (comment.isEmpty()) continue;
            jdbc.execute("ALTER TABLE bs_game_player_stat MODIFY COLUMN " + col + " INT NULL" + commentClause);
            log.debug("bs_game_player_stat \u8865\u5907\u6ce8: {}", (Object)col);
        }
        if (!this.columnExistsMysql(jdbc, "bs_game_player_stat", "pitch_inside_park_hr")) {
            jdbc.execute("ALTER TABLE bs_game_player_stat ADD COLUMN pitch_inside_park_hr INT NULL COMMENT '\u88ab\u573a\u5185\u5168\u5792\u6253\uff08\u4e0d\u542b\u4e8e pitch_hr\uff09'");
            log.info("bs_game_player_stat \u6dfb\u52a0\u5217: pitch_inside_park_hr");
        }
    }

    private boolean columnExistsSqlite(JdbcTemplate jdbc, String table, String column) {
        try {
            List rows = jdbc.queryForList("PRAGMA table_info(" + table + ")");
            return rows.stream().anyMatch(r -> column.equalsIgnoreCase(r.get("name").toString()));
        }
        catch (Exception e) {
            return false;
        }
    }

    private boolean columnExistsMysql(JdbcTemplate jdbc, String table, String column) {
        try {
            Integer cnt = (Integer)jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?", Integer.class, new Object[]{table, column});
            return cnt != null && cnt > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    private void migrateSysNoticeViewCountPostgres(JdbcTemplate jdbc) {
        if (!this.columnExistsPostgres(jdbc, "sys_article", "view_count")) {
            jdbc.execute("ALTER TABLE sys_article ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0");
            log.info("sys_article \u6dfb\u52a0\u5217: view_count\uff08PostgreSQL\uff09");
        } else {
            int filled = jdbc.update("UPDATE sys_article SET view_count = 0 WHERE view_count IS NULL");
            if (filled > 0) {
                log.info("sys_article.view_count\uff1a\u5df2\u5c06 {} \u884c NULL \u66f4\u65b0\u4e3a 0\uff08\u4fbf\u4e8e\u8bbe NOT NULL\uff09", (Object)filled);
            }
            this.tryExecutePostgresDdl(jdbc, "ALTER TABLE sys_article ALTER COLUMN view_count SET DEFAULT 0");
            this.tryExecutePostgresDdl(jdbc, "ALTER TABLE sys_article ALTER COLUMN view_count SET NOT NULL");
        }
        try {
            jdbc.execute("COMMENT ON COLUMN sys_article.view_count IS '\u6d4f\u89c8\u6b21\u6570\uff08\u524d\u53f0\u8be6\u60c5\u8bbf\u95ee\u7d2f\u8ba1\uff09'");
        }
        catch (Exception ex) {
            log.debug("sys_article.view_count COMMENT \u8df3\u8fc7: {}", (Object)ex.getMessage());
        }
    }

    private void tryExecutePostgresDdl(JdbcTemplate jdbc, String sql) {
        try {
            jdbc.execute(sql);
        }
        catch (Exception ex) {
            log.debug("PostgreSQL DDL \u8df3\u8fc7\uff08\u53ef\u80fd\u5df2\u662f\u76ee\u6807\u72b6\u6001\uff09: {} \u2014 {}", (Object)sql, (Object)ex.getMessage());
        }
    }

    private boolean columnExistsPostgres(JdbcTemplate jdbc, String table, String column) {
        try {
            Integer cnt = (Integer)jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns c\nWHERE c.table_schema = current_schema()\n  AND c.table_name = LOWER(?)\n  AND c.column_name = LOWER(?)\n", Integer.class, new Object[]{table, column});
            return cnt != null && cnt > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    private void ensureApiGroups() {
        List apis = this.sysApiRepository.findAll();
        List defs = DbMigrationRunner.apiDefs();
        boolean changed = false;
        for (SysApi api : apis) {
            if (api.getGroupName() != null && !api.getGroupName().isBlank()) continue;
            String group = defs.stream().filter(d -> DbMigrationRunner.sameHttpMethod((String)d.method, (String)api.getMethod()) && Objects.equals(d.path, api.getPath())).map(d -> d.group).findFirst().orElse(ApiGroupUtil.inferGroup((String)api.getPath()));
            api.setGroupName(group);
            this.sysApiRepository.save((Object)api);
            changed = true;
        }
        if (this.sysMenuRepository.count() > 0L) {
            for (ApiDef d2 : defs) {
                boolean exists = apis.stream().anyMatch(a -> DbMigrationRunner.sameHttpMethod((String)d.method, (String)a.getMethod()) && Objects.equals(d.path, a.getPath()));
                if (exists) continue;
                SysApi api = new SysApi();
                api.setPath(d2.path);
                api.setMethod(d2.method);
                api.setDescription(d2.desc);
                api.setGroupName(d2.group);
                this.sysApiRepository.save((Object)api);
                apis = this.sysApiRepository.findAll();
                changed = true;
            }
        }
        if (changed) {
            log.info("API \u5206\u7ec4\u5df2\u8865\u5168");
        }
    }

    private static boolean sameHttpMethod(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private void ensureGuestHasNoticeArticleCoachApis() {
        Optional guestOpt = this.sysRoleRepository.findByTenantIdIsNullAndCode("guest");
        if (guestOpt.isEmpty()) {
            return;
        }
        SysRole guestRole = (SysRole)guestOpt.get();
        List apis = this.sysApiRepository.findAll();
        List noticeAndCoachApiIds = apis.stream().filter(a -> "GET".equals(a.getMethod()) && a.getPath() != null && (a.getPath().contains("/sys/notice") || a.getPath().contains("/sys/article") || a.getPath().contains("/coach"))).map(BaseEntity::getId).distinct().toList();
        if (noticeAndCoachApiIds.isEmpty()) {
            return;
        }
        Set existing = this.sysRoleApiRepository.findByRoleId(guestRole.getId()).stream().map(SysRoleApi::getApiId).collect(Collectors.toCollection(HashSet::new));
        long adminId = 1L;
        int added = 0;
        for (Long apiId : noticeAndCoachApiIds) {
            if (existing.contains(apiId)) continue;
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(guestRole.getId());
            ra.setApiId(apiId);
            ra.setCreatedBy(Long.valueOf(adminId));
            ra.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleApiRepository.save((Object)ra);
            existing.add(apiId);
            ++added;
        }
        if (added > 0) {
            log.info("DbMigration: \u5df2\u4e3a\u6e38\u5ba2\u89d2\u8272\u8865\u7ed1 {} \u4e2a\u6587\u7ae0/\u901a\u77e5/\u6559\u7ec3\u76f8\u5173 GET API", (Object)added);
        }
    }

    private void ensureGuestHasPlayerStatsGetApis() {
        Optional guestOpt = this.sysRoleRepository.findByTenantIdIsNullAndCode("guest");
        if (guestOpt.isEmpty()) {
            return;
        }
        long guestId = ((SysRole)guestOpt.get()).getId();
        List apis = this.sysApiRepository.findAll();
        Set existing = this.sysRoleApiRepository.findByRoleId(Long.valueOf(guestId)).stream().map(SysRoleApi::getApiId).collect(Collectors.toSet());
        long adminId = 1L;
        int added = 0;
        for (SysApi a : apis) {
            String p;
            if (!"GET".equals(a.getMethod()) || (p = a.getPath()) == null) continue;
            boolean playerStats = p.contains("/player") && p.contains("/stats");
            boolean leaders = p.startsWith("/stats/leaders");
            boolean standings = "/stats/standings".equals(p);
            boolean starTopList = "/stats/star/toplist".equals(p);
            if (!playerStats && !leaders && !standings && !starTopList || existing.contains(a.getId())) continue;
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(Long.valueOf(guestId));
            ra.setApiId(a.getId());
            ra.setCreatedBy(Long.valueOf(adminId));
            ra.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleApiRepository.save((Object)ra);
            existing.add(a.getId());
            ++added;
        }
        if (added > 0) {
            log.info("DbMigration: \u5df2\u4e3a\u6e38\u5ba2\u89d2\u8272\u8865\u7ed1 {} \u4e2a\u7403\u5458\u7edf\u8ba1/\u6570\u636e\u6392\u884c\u7c7b GET API", (Object)added);
        }
    }

    private void ensureGuestHasStadiumGetApis() {
        Optional guestOpt = this.sysRoleRepository.findByTenantIdIsNullAndCode("guest");
        if (guestOpt.isEmpty()) {
            return;
        }
        long guestId = ((SysRole)guestOpt.get()).getId();
        List apis = this.sysApiRepository.findAll();
        Set existing = this.sysRoleApiRepository.findByRoleId(Long.valueOf(guestId)).stream().map(SysRoleApi::getApiId).collect(Collectors.toSet());
        long adminId = 1L;
        int added = 0;
        for (SysApi a : apis) {
            String p;
            if (!"GET".equals(a.getMethod()) || (p = a.getPath()) == null || !p.contains("/stadium") || existing.contains(a.getId())) continue;
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(Long.valueOf(guestId));
            ra.setApiId(a.getId());
            ra.setCreatedBy(Long.valueOf(adminId));
            ra.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleApiRepository.save((Object)ra);
            existing.add(a.getId());
            ++added;
        }
        if (added > 0) {
            log.info("DbMigration: \u5df2\u4e3a\u6e38\u5ba2\u89d2\u8272\u8865\u7ed1 {} \u4e2a\u7403\u573a\u76f8\u5173 GET API", (Object)added);
        }
    }

    private void ensureGuestHasPortalBusinessGetApis() {
        Optional guestOpt = this.sysRoleRepository.findByTenantIdIsNullAndCode("guest");
        if (guestOpt.isEmpty()) {
            return;
        }
        long guestId = ((SysRole)guestOpt.get()).getId();
        List apis = this.sysApiRepository.findAll();
        Set existing = this.sysRoleApiRepository.findByRoleId(Long.valueOf(guestId)).stream().map(SysRoleApi::getApiId).collect(Collectors.toSet());
        long adminId = 1L;
        int added = 0;
        for (SysApi a : apis) {
            boolean businessGet;
            String p;
            if (!"GET".equals(a.getMethod()) || (p = a.getPath()) == null || !(businessGet = p.contains("/league") || p.contains("/team") || p.contains("/event") || p.contains("/game") || p.contains("/coach")) || existing.contains(a.getId())) continue;
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(Long.valueOf(guestId));
            ra.setApiId(a.getId());
            ra.setCreatedBy(Long.valueOf(adminId));
            ra.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleApiRepository.save((Object)ra);
            existing.add(a.getId());
            ++added;
        }
        if (added > 0) {
            log.info("DbMigration: \u5df2\u4e3a\u6e38\u5ba2\u89d2\u8272\u8865\u7ed1 {} \u4e2a\u95e8\u6237\u4e1a\u52a1 GET API\uff08\u8054\u76df/\u7403\u961f/\u8d5b\u4e8b/\u6bd4\u8d5b/\u6559\u7ec3\uff09", (Object)added);
        }
    }

    private void ensureGuestHasPortalDevtoolsPostApi() {
        Optional guestOpt = this.sysRoleRepository.findByTenantIdIsNullAndCode("guest");
        if (guestOpt.isEmpty()) {
            return;
        }
        long guestId = ((SysRole)guestOpt.get()).getId();
        List apis = this.sysApiRepository.findAll();
        Set existing = this.sysRoleApiRepository.findByRoleId(Long.valueOf(guestId)).stream().map(ra -> ra.getApiId()).collect(Collectors.toCollection(HashSet::new));
        long adminId = 1L;
        int added = 0;
        for (SysApi a : apis) {
            String p;
            if (!"POST".equals(a.getMethod()) || (p = a.getPath()) == null || !p.contains("/portal/devtools") || existing.contains(a.getId())) continue;
            SysRoleApi ra2 = new SysRoleApi();
            ra2.setRoleId(Long.valueOf(guestId));
            ra2.setApiId(a.getId());
            ra2.setCreatedBy(Long.valueOf(adminId));
            ra2.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleApiRepository.save((Object)ra2);
            existing.add(a.getId());
            ++added;
        }
        if (added > 0) {
            log.info("DbMigration: \u5df2\u4e3a\u6e38\u5ba2\u89d2\u8272\u8865\u7ed1 {} \u4e2a\u95e8\u6237\u5f00\u53d1\u8005\u5de5\u5177\u4e0a\u62a5 API", (Object)added);
        }
    }

    public static List<ApiDef> apiDefs() {
        return List.of((Object[])new ApiDef[]{new ApiDef("/auth/login", "POST", "\u767b\u5f55", "\u8ba4\u8bc1"), new ApiDef("/auth/captcha/options", "GET", "\u67e5\u8be2\u767b\u5f55\u9a8c\u8bc1\u7801\u914d\u7f6e", "\u8ba4\u8bc1"), new ApiDef("/auth/captcha/image", "GET", "\u751f\u6210\u767b\u5f55\u56fe\u5f62\u9a8c\u8bc1\u7801", "\u8ba4\u8bc1"), new ApiDef("/auth/captcha/verify-click", "POST", "\u6821\u9a8c\u70b9\u9009\u9a8c\u8bc1\u7801", "\u8ba4\u8bc1"), new ApiDef("/auth/captcha/verify-drag", "POST", "\u6821\u9a8c\u62d6\u62fd\u62fc\u56fe\u9a8c\u8bc1\u7801", "\u8ba4\u8bc1"), new ApiDef("/auth/change-password", "PATCH", "\u4fee\u6539\u5bc6\u7801", "\u8ba4\u8bc1"), new ApiDef("/sys/user/list", "GET", "\u67e5\u8be2\u7528\u6237\u5217\u8868", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/user/:id", "GET", "\u67e5\u8be2\u7528\u6237\u8be6\u60c5", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/user/create", "POST", "\u521b\u5efa\u7528\u6237", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/user/update/:id", "PUT", "\u66f4\u65b0\u7528\u6237", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/user/delete/:id", "DELETE", "\u5220\u9664\u7528\u6237", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/role/list", "GET", "\u67e5\u8be2\u89d2\u8272\u5217\u8868", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/assign-options", "GET", "\u7528\u6237\u7ba1\u7406\u5206\u914d\u89d2\u8272\u9009\u9879", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/:id", "GET", "\u67e5\u8be2\u89d2\u8272\u8be6\u60c5", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/create", "POST", "\u521b\u5efa\u89d2\u8272", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/update/:id", "PUT", "\u66f4\u65b0\u89d2\u8272", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/delete/:id", "DELETE", "\u5220\u9664\u89d2\u8272", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/menu/list", "GET", "\u67e5\u8be2\u83dc\u5355\u5217\u8868", "\u83dc\u5355\u7ba1\u7406"), new ApiDef("/sys/menu/create", "POST", "\u521b\u5efa\u83dc\u5355", "\u83dc\u5355\u7ba1\u7406"), new ApiDef("/sys/menu/update/:id", "PUT", "\u66f4\u65b0\u83dc\u5355", "\u83dc\u5355\u7ba1\u7406"), new ApiDef("/sys/menu/delete/:id", "DELETE", "\u5220\u9664\u83dc\u5355", "\u83dc\u5355\u7ba1\u7406"), new ApiDef("/sys/api/list", "GET", "\u67e5\u8be2API\u5217\u8868", "API\u7ba1\u7406"), new ApiDef("/sys/api/create", "POST", "\u521b\u5efaAPI", "API\u7ba1\u7406"), new ApiDef("/sys/api/update/:id", "PUT", "\u66f4\u65b0API", "API\u7ba1\u7406"), new ApiDef("/sys/api/delete/:id", "DELETE", "\u5220\u9664API", "API\u7ba1\u7406"), new ApiDef("/sys/dict/type/list", "GET", "\u67e5\u8be2\u5b57\u5178\u7c7b\u578b\u5217\u8868", "\u5b57\u5178\u7c7b\u578b\u7ba1\u7406"), new ApiDef("/sys/dict/data/list", "GET", "\u67e5\u8be2\u5b57\u5178\u6570\u636e\u5217\u8868", "\u5b57\u5178\u6570\u636e\u7ba1\u7406"), new ApiDef("/sys/dict/list", "GET", "\u67e5\u8be2\u5b57\u5178\u5217\u8868", "\u5b57\u5178\u7ba1\u7406"), new ApiDef("/sys/article/list", "GET", "\u67e5\u8be2\u6587\u7ae0\u5217\u8868", "\u6587\u7ae0\u7ba1\u7406"), new ApiDef("/sys/article/platform/list", "GET", "\u67e5\u8be2\u5e73\u53f0\u7ea7\u6587\u7ae0\u5217\u8868\uff08\u95e8\u6237\uff09", "\u6587\u7ae0\u7ba1\u7406"), new ApiDef("/sys/article/:id", "GET", "\u67e5\u8be2\u6587\u7ae0\u8be6\u60c5", "\u6587\u7ae0\u7ba1\u7406"), new ApiDef("/sys/notice/list", "GET", "\u67e5\u8be2\u7cfb\u7edf\u901a\u77e5\u5217\u8868", "\u901a\u77e5\u7ba1\u7406"), new ApiDef("/sys/notice/:id", "GET", "\u67e5\u8be2\u7cfb\u7edf\u901a\u77e5\u8be6\u60c5", "\u901a\u77e5\u7ba1\u7406"), new ApiDef("/sys/resource/list", "GET", "\u67e5\u8be2\u8d44\u6e90\u5217\u8868", "\u8d44\u6e90\u7ba1\u7406"), new ApiDef("/sys/login-log/list", "GET", "\u67e5\u8be2\u767b\u5f55\u8bb0\u5f55\u5217\u8868", "\u767b\u5f55\u5386\u53f2"), new ApiDef("/sys/operation-log/list", "GET", "\u67e5\u8be2\u64cd\u4f5c\u5386\u53f2\u5217\u8868", "\u64cd\u4f5c\u5386\u53f2"), new ApiDef("/sys/config", "GET", "\u67e5\u8be2\u7cfb\u7edf\u914d\u7f6e", "\u914d\u7f6e\u7ba1\u7406"), new ApiDef("/sys/config", "PUT", "\u66f4\u65b0\u7cfb\u7edf\u914d\u7f6e", "\u914d\u7f6e\u7ba1\u7406"), new ApiDef("/sys/monitor/datasource/url", "GET", "\u67e5\u8be2\u6570\u636e\u76d1\u63a7\u5730\u5740", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/server", "GET", "\u67e5\u8be2\u670d\u52a1\u76d1\u63a7", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache", "GET", "\u67e5\u8be2\u7f13\u5b58\u76d1\u63a7", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache/keys", "GET", "\u67e5\u8be2\u7f13\u5b58\u5217\u8868", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache/value", "GET", "\u67e5\u8be2\u7f13\u5b58\u503c", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache/key", "DELETE", "\u5220\u9664\u7f13\u5b58\u952e", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache/clear", "DELETE", "\u6e05\u7a7a\u7f13\u5b58", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/portal/devtools-report/list", "GET", "\u67e5\u8be2\u95e8\u6237\u5f00\u53d1\u8005\u5de5\u5177\u6253\u5f00\u8bb0\u5f55", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/portal/visit-hit/list", "GET", "\u67e5\u8be2\u95e8\u6237\u8bbf\u95ee\u6253\u70b9\u660e\u7ec6", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/portal/ip-location", "GET", "IP \u5f52\u5c5e\u5730\uff08\u9ad8\u5fb7\uff09", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/portal/ip-location/batch", "POST", "\u6279\u91cf IP \u5f52\u5c5e\u5730\uff08\u9ad8\u5fb7\uff09", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/ip-location-cache/page", "GET", "\u5206\u9875\u67e5\u8be2 IP \u5f52\u5c5e\u5730\u7f13\u5b58", "IP\u5730\u7406\u4fe1\u606f"), new ApiDef("/sys/ip-location-cache/lbs-providers", "GET", "\u67e5\u8be2\u53ef\u9009 IP \u5f52\u5c5e\u5730\u6570\u636e\u6e90", "IP\u5730\u7406\u4fe1\u606f"), new ApiDef("/sys/ip-location-cache/refresh", "POST", "\u5f3a\u5236\u91cd\u65b0\u89e3\u6790 IP \u5e76\u5199\u56de\u7f13\u5b58", "IP\u5730\u7406\u4fe1\u606f"), new ApiDef("/league/list", "GET", "\u67e5\u8be2\u8054\u76df\u5217\u8868", "\u8054\u76df\u7ba1\u7406"), new ApiDef("/league/:id", "GET", "\u67e5\u8be2\u8054\u76df\u8be6\u60c5", "\u8054\u76df\u7ba1\u7406"), new ApiDef("/team/list", "GET", "\u67e5\u8be2\u7403\u961f\u5217\u8868", "\u7403\u961f\u7ba1\u7406"), new ApiDef("/team/:id", "GET", "\u67e5\u8be2\u7403\u961f\u8be6\u60c5", "\u7403\u961f\u7ba1\u7406"), new ApiDef("/lineup-template/list", "GET", "\u5206\u9875\u67e5\u8be2\u9635\u5bb9\u6a21\u677f\uff08\u53ef\u6309\u7403\u961f\u7b5b\u9009\uff09", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/list", "GET", "\u67e5\u8be2\u7403\u961f\u9635\u5bb9\u6a21\u677f\u5217\u8868", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/:id", "GET", "\u67e5\u8be2\u7403\u961f\u9635\u5bb9\u6a21\u677f\u8be6\u60c5", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/create", "POST", "\u521b\u5efa\u7403\u961f\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/copy-from-game", "POST", "\u4ece\u6bd4\u8d5b\u590d\u5236\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/update/:id", "PUT", "\u66f4\u65b0\u7403\u961f\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/delete/:id", "DELETE", "\u5220\u9664\u7403\u961f\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/event/list", "GET", "\u67e5\u8be2\u8d5b\u4e8b\u5217\u8868", "\u8d5b\u4e8b\u7ba1\u7406"), new ApiDef("/event/:id", "GET", "\u67e5\u8be2\u8d5b\u4e8b\u8be6\u60c5", "\u8d5b\u4e8b\u7ba1\u7406"), new ApiDef("/event/:eventId/import-game-result", "POST", "\u5bfc\u5165\u6bd4\u8d5b\u7ed3\u679c\uff08\u5355\u4e8b\u52a1\uff09", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/list", "GET", "\u67e5\u8be2\u6bd4\u8d5b\u5217\u8868", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/:id", "GET", "\u67e5\u8be2\u6bd4\u8d5b\u8be6\u60c5", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/:gameId/stats", "GET", "\u67e5\u8be2\u6bd4\u8d5b\u7403\u5458\u6570\u636e", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/:id/save-live", "POST", "\u5b9e\u65f6\u4fdd\u5b58\u6bd4\u8d5b", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/:id/save-result", "POST", "\u4fdd\u5b58\u6bd4\u8d5b\u7ed3\u679c\uff08\u6bd4\u8d5b+\u7edf\u8ba1\u4e8b\u52a1\uff09", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/stats/leaders/batting", "GET", "\u67e5\u8be2\u6253\u51fb\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/pitching", "GET", "\u67e5\u8be2\u6295\u7403\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/fielding", "GET", "\u67e5\u8be2\u9632\u5b88\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/team-batting", "GET", "\u67e5\u8be2\u7403\u961f\u8fdb\u653b\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/team-pitching", "GET", "\u67e5\u8be2\u7403\u961f\u6295\u7403\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/team-fielding", "GET", "\u67e5\u8be2\u7403\u961f\u9632\u5b88\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/standings", "GET", "\u67e5\u8be2\u7403\u961f\u79ef\u5206\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/star/toplist", "GET", "\u67e5\u8be2\u660e\u661f\u7403\u5458\u5355\u9879\u699c\u805a\u5408", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/player/list", "GET", "\u67e5\u8be2\u7403\u5458\u5217\u8868", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/team-options", "GET", "\u6309\u7403\u961f\u67e5\u8be2\u7403\u5458\u9009\u9879", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/check-full-name", "GET", "\u6821\u9a8c\u7403\u5458\u5168\u540d\u662f\u5426\u91cd\u590d", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id", "GET", "\u67e5\u8be2\u7403\u5458\u8be6\u60c5", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats", "GET", "\u67e5\u8be2\u7403\u5458\u6570\u636e\u7edf\u8ba1", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/by-season", "GET", "\u67e5\u8be2\u7403\u5458\u6309\u8d5b\u5b63\u7edf\u8ba1", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/game-log", "GET", "\u67e5\u8be2\u7403\u5458\u6bd4\u8d5b\u65e5\u5fd7", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/drill-down/batting", "GET", "\u7403\u5458\u6570\u636e\u7edf\u8ba1\u94bb\u53d6\uff08\u6253\u51fb\uff09", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/drill-down/pitching", "GET", "\u7403\u5458\u6570\u636e\u7edf\u8ba1\u94bb\u53d6\uff08\u6295\u7403\uff09", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/drill-down/fielding", "GET", "\u7403\u5458\u6570\u636e\u7edf\u8ba1\u94bb\u53d6\uff08\u9632\u5b88\uff09", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/personnel-change/list", "GET", "\u67e5\u8be2\u6cbf\u9769\u8bb0\u5f55\u5217\u8868", "\u6cbf\u9769\u7ba1\u7406"), new ApiDef("/personnel-change/create", "POST", "\u521b\u5efa\u6cbf\u9769\u8bb0\u5f55", "\u6cbf\u9769\u7ba1\u7406"), new ApiDef("/highlight-moment/list", "GET", "\u67e5\u8be2\u9ad8\u5149\u65f6\u523b\u5217\u8868", "\u9ad8\u5149\u65f6\u523b\u7ba1\u7406"), new ApiDef("/highlight-moment/create", "POST", "\u521b\u5efa\u9ad8\u5149\u65f6\u523b", "\u9ad8\u5149\u65f6\u523b\u7ba1\u7406"), new ApiDef("/highlight-moment/update/{id}", "PUT", "\u66f4\u65b0\u9ad8\u5149\u65f6\u523b", "\u9ad8\u5149\u65f6\u523b\u7ba1\u7406"), new ApiDef("/highlight-moment/delete/{id}", "DELETE", "\u5220\u9664\u9ad8\u5149\u65f6\u523b", "\u9ad8\u5149\u65f6\u523b\u7ba1\u7406"), new ApiDef("/region/china/children", "GET", "\u4e2d\u56fd\u884c\u653f\u533a\u5212\u5b50\u7ea7\uff08\u7701\u5e02\u533a\u61d2\u52a0\u8f7d\uff09", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/list", "GET", "\u67e5\u8be2\u7403\u573a\u5217\u8868", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/geojson", "GET", "\u7403\u573a\u5206\u5e03 GeoJSON", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/nearby", "GET", "\u9644\u8fd1\u7403\u573a", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/:id", "GET", "\u67e5\u8be2\u7403\u573a\u8be6\u60c5", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/create", "POST", "\u521b\u5efa\u7403\u573a", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/update/:id", "PUT", "\u66f4\u65b0\u7403\u573a", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/delete/:id", "DELETE", "\u5220\u9664\u7403\u573a", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/sys/portal-visit/summary", "GET", "\u95e8\u6237\u8bbf\u95ee\u7edf\u8ba1\uff08PV/UV\uff09", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/sys/portal-visit/by-province", "GET", "\u95e8\u6237\u8bbf\u95ee\u6309\u7701\u805a\u5408\uff08PV/UV\uff09", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/sys/portal-visit/by-province/cities", "GET", "\u95e8\u6237\u8bbf\u95ee\u6307\u5b9a\u7701\u6309\u5730\u5e02\u805a\u5408\uff08PV/UV\uff09", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/portal/devtools/report", "POST", "\u95e8\u6237\u5f00\u53d1\u8005\u5de5\u5177\u6253\u5f00\u4e0a\u62a5", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/portal/feedback/captcha-image", "GET", "\u95e8\u6237\u610f\u89c1\u53cd\u9988\u9a8c\u8bc1\u7801", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/portal/feedback/submit", "POST", "\u63d0\u4ea4\u95e8\u6237\u610f\u89c1\u53cd\u9988", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/portal/feedback/my-list", "GET", "\u67e5\u8be2\u6211\u7684\u95e8\u6237\u610f\u89c1\u53cd\u9988", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/sys/portal/feedback/list", "GET", "\u5206\u9875\u67e5\u8be2\u95e8\u6237\u610f\u89c1\u53cd\u9988", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/portal/feedback/reply", "POST", "\u56de\u590d\u95e8\u6237\u610f\u89c1\u53cd\u9988", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406")});
    }

    private void ensureDictTypes() {
        this.ensureDictTypeWithData("venue", "\u6bd4\u8d5b\u573a\u5730", "\u7403\u573a/\u573a\u5730", List.of((Object)new String[]{"\u4e3b\u573a", "home"}, (Object)new String[]{"\u5ba2\u573a", "away"}, (Object)new String[]{"\u4e2d\u7acb", "neutral"}));
        this.ensureDictTypeWithData("player_status", "\u7403\u5458\u72b6\u6001", "\u5728\u5f79/\u9000\u5f79\u7b49", List.of((Object)new String[]{"\u5728\u5f79", "active"}, (Object)new String[]{"\u9000\u5f79", "retired"}, (Object)new String[]{"\u4f24\u75c5", "injured"}, (Object)new String[]{"\u5176\u4ed6", "other"}));
        this.ensureDictTypeWithData("notice_type", "\u5185\u5bb9\u7c7b\u578b", "\u5185\u5bb9\u7ba1\u7406\uff1a\u95e8\u6237\u8d44\u8baf/\u516c\u544a\u5206\u7c7b\uff08\u4e0e sys_article.type \u4e00\u81f4\uff09", List.of((Object)new String[]{"\u8d44\u8baf", "news"}, (Object)new String[]{"\u516c\u544a", "announcement"}, (Object)new String[]{"\u901a\u77e5", "notice"}, (Object)new String[]{"\u516c\u793a", "publicity"}));
        this.ensureDictTypeWithData("content_format", "\u5185\u5bb9\u683c\u5f0f", "\u5185\u5bb9\u7ba1\u7406\uff1a\u6b63\u6587\u5b58\u50a8\u683c\u5f0f", List.of((Object)new String[]{"HTML", "html"}, (Object)new String[]{"Markdown", "markdown"}));
    }

    private void ensureDictTypeWithData(String typeCode, String name, String remark, List<String[]> labelValuePairs) {
        if (!this.sysDictTypeRepository.findAllByType(typeCode).isEmpty()) {
            return;
        }
        SysDictType t = new SysDictType();
        t.setName(name);
        t.setType(typeCode);
        t.setStatus(Integer.valueOf(1));
        t.setRemark(remark);
        t = (SysDictType)this.sysDictTypeRepository.save((Object)t);
        for (String[] e : labelValuePairs) {
            SysDictData dd = new SysDictData();
            dd.setDictTypeId(t.getId());
            dd.setLabel(e[0]);
            dd.setValue(e[1]);
            dd.setSort(Integer.valueOf(0));
            dd.setStatus(Integer.valueOf(1));
            this.sysDictDataRepository.save((Object)dd);
        }
        log.info("\u5b57\u5178\u7c7b\u578b\u5df2\u8865\u5168: {}", (Object)name);
    }

    private void ensureApiGroupContentManagementLabel() {
        List types = this.sysDictTypeRepository.findAllByType("api_group");
        if (types.isEmpty()) {
            return;
        }
        long typeId = ((SysDictType)types.get(0)).getId();
        List rows = this.sysDictDataRepository.findByDictTypeIdOrderBySortAscIdAsc(Long.valueOf(typeId));
        boolean exists = rows.stream().anyMatch(d -> "\u5185\u5bb9\u7ba1\u7406".equals(d.getLabel()) || "\u5185\u5bb9\u7ba1\u7406".equals(d.getValue()));
        if (exists) {
            return;
        }
        int maxSort = rows.stream().map(SysDictData::getSort).filter(s -> s != null).max(Comparator.naturalOrder()).orElse(0);
        SysDictData dd = new SysDictData();
        dd.setDictTypeId(Long.valueOf(typeId));
        dd.setLabel("\u5185\u5bb9\u7ba1\u7406");
        dd.setValue("\u5185\u5bb9\u7ba1\u7406");
        dd.setSort(Integer.valueOf(maxSort + 1));
        dd.setStatus(Integer.valueOf(1));
        this.sysDictDataRepository.save((Object)dd);
        log.info("API \u5206\u7ec4\u5b57\u5178\u5df2\u8865\u5168: \u5185\u5bb9\u7ba1\u7406");
    }

    @Generated
    public DbMigrationRunner(DataSource dataSource, SysApiRepository sysApiRepository, SysMenuRepository sysMenuRepository, SysDictTypeRepository sysDictTypeRepository, SysDictDataRepository sysDictDataRepository, SysRoleRepository sysRoleRepository, SysRoleApiRepository sysRoleApiRepository) {
        this.dataSource = dataSource;
        this.sysApiRepository = sysApiRepository;
        this.sysMenuRepository = sysMenuRepository;
        this.sysDictTypeRepository = sysDictTypeRepository;
        this.sysDictDataRepository = sysDictDataRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.sysRoleApiRepository = sysRoleApiRepository;
    }
}

