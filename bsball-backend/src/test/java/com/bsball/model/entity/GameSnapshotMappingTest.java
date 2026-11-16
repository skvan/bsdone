package com.bsball.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hibernate.annotations.Formula;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.PersistentClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 比赛实体映射边界（2026-11-15 生产 OOM 回归，Issue #686）：
 * 列表查询返回完整 Game 实体。若实体继续映射 live_snapshot_json（生产平均 2.4MB/场），
 * 100 条分页就会把约 240MB 快照文本读入堆内存，在约 894MB 堆上限下触发 OutOfMemoryError。
 * 快照列改由同表轻量实体 GameSnapshot 单独读写，Game 不得再声明该字段。
 */
class GameSnapshotMappingTest {

    @Test
    @DisplayName("Game 实体不得映射 liveSnapshotJson 大字段（防列表查询把快照读入堆）")
    void gameEntity_doesNotDeclareSnapshotField() {
        assertThrows(NoSuchFieldException.class,
                () -> Game.class.getDeclaredField("liveSnapshotJson"));
    }

    @Test
    @DisplayName("快照标记使用仅判 NULL 的 SQL 计算字段（避免 length() 触发 detoast）")
    void gameSnapshotFlag_usesNullCheckOnly() throws Exception {
        Formula formula = Game.class.getDeclaredField("hasLiveSnapshot").getAnnotation(Formula.class);
        assertNotNull(formula);
        assertEquals("(live_snapshot_json is not null)", formula.value());
    }

    @Test
    @DisplayName("Game 与 GameSnapshot 同表映射元数据可构建（防物理列出现多个逻辑列名）")
    void metadata_buildsForGameAndSnapshot() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            sources.addAnnotatedClass(Game.class);
            sources.addAnnotatedClass(GameSnapshot.class);
            var metadata = sources.buildMetadata();
            assertNotNull(metadata.getEntityBinding(Game.class.getName()));
            PersistentClass snapshotBinding = metadata.getEntityBinding(GameSnapshot.class.getName());
            assertNotNull(snapshotBinding);
            assertEquals("live_snapshot_json", firstColumnName(snapshotBinding, "liveSnapshotJson"));
            assertEquals("updated_at", firstColumnName(snapshotBinding, "updatedAt"));
            assertEquals("updated_by", firstColumnName(snapshotBinding, "updatedBy"));
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static String firstColumnName(PersistentClass binding, String property) {
        return binding.getProperty(property).getColumns().get(0).getName();
    }
}
