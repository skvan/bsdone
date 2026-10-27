package com.bsball.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Mapper：PostgreSQL ER 列名映射")
class MapperColumnMappingTest {

    private static final List<String> MAPPERS = List.of(
            "PlayerStatsMapper.xml",
            "StatsLeadersMapper.xml"
    );

    @Test
    @DisplayName("R/UER/PENDING 在 PostgreSQL 使用旧列，SQL 主体不直接引用新列")
    void postgresUsesLegacyColumns() throws IOException {
        for (String name : MAPPERS) {
            String xml = read(name);
            assertFragment(xml, name, "pitchRCol", "s.pitchr", "s.pitch_r");
            assertFragment(xml, name, "unearnedRCol", "s.unearnedr", "s.unearned_r");
            assertFragment(xml, name, "pendingRCol", "s.pendingr", "s.pending_r");

            String body = xml
                    .replaceAll("(?s)<sql\\s+id=\"pitchRCol\">.*?</sql>", "")
                    .replaceAll("(?s)<sql\\s+id=\"unearnedRCol\">.*?</sql>", "")
                    .replaceAll("(?s)<sql\\s+id=\"pendingRCol\">.*?</sql>", "");
            assertFalse(body.contains("s.pitch_r"), name + " SQL 主体仍直接引用 s.pitch_r");
            assertFalse(body.contains("s.unearned_r"), name + " SQL 主体仍直接引用 s.unearned_r");
            assertFalse(body.contains("s.pending_r"), name + " SQL 主体仍直接引用 s.pending_r");
        }
    }

    private static void assertFragment(String xml, String name, String id, String legacy, String modern) {
        var matcher = Pattern.compile("(?s)<sql\\s+id=\"" + Pattern.quote(id) + "\">(.*?)</sql>").matcher(xml);
        assertTrue(matcher.find(), name + " 缺少 " + id);
        String fragment = matcher.group(1);
        assertTrue(fragment.contains("_databaseId == 'PostgreSQL'"), name + " " + id + " 缺少 PostgreSQL 分支");
        assertTrue(fragment.contains(legacy), name + " " + id + " PostgreSQL 分支应使用 " + legacy);
        assertTrue(fragment.contains(modern), name + " " + id + " 默认分支应使用 " + modern);
    }

    private static String read(String name) throws IOException {
        try (InputStream in = MapperColumnMappingTest.class.getResourceAsStream("/mapper/" + name)) {
            assertNotNull(in, "缺少资源 /mapper/" + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
