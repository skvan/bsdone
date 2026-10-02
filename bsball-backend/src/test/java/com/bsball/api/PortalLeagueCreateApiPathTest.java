/*
 * 建盟链路批次①路径契约测试（防回归）：
 *  - 控制器实际暴露路径 = 类级 "/portal" + 方法级 "/league-create" = "/portal/league-create"；
 *  - 与前端 baseURL（/api 前缀后）/ 种子 sys_api / member 白名单谓词三处逐字一致；
 *  - LeagueApi 不得再暴露 league-create（防止类级前缀拼接产生 /league/portal/... 幽灵路径）。
 */
package com.bsball.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bsball.core.InitDataRunner;
import com.bsball.model.entity.SysApi;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@DisplayName("建盟链路：门户建盟端点路径契约（批次①）")
class PortalLeagueCreateApiPathTest {

    private static final String EXPECTED_SEED_PATH = "/portal/league-create";

    @Test
    @DisplayName("控制器真实路径 = /portal + /league-create（与种子/前端逐字一致）")
    void controllerPathMatchesSeed() {
        RequestMapping classMapping = PortalLeagueCreateApi.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping, "PortalLeagueCreateApi 必须声明类级 @RequestMapping");
        assertEquals("/portal", classMapping.value()[0], "类级前缀必须为 /portal");

        Method m = null;
        for (Method candidate : PortalLeagueCreateApi.class.getMethods()) {
            if ("portalCreate".equals(candidate.getName())) {
                m = candidate;
                break;
            }
        }
        assertNotNull(m, "portalCreate 方法必须存在");
        PostMapping mapping = m.getAnnotation(PostMapping.class);
        assertNotNull(mapping, "portalCreate 必须为 @PostMapping");
        String composed = classMapping.value()[0] + mapping.value()[0];
        assertEquals(EXPECTED_SEED_PATH, composed, "组合路径必须与种子注册路径逐字一致");
    }

    @Test
    @DisplayName("LeagueApi 不再暴露 league-create（防类级前缀拼接出幽灵路径）")
    void leagueApiHasNoLeagueCreateEndpoint() {
        for (Method candidate : LeagueApi.class.getMethods()) {
            PostMapping mapping = candidate.getAnnotation(PostMapping.class);
            if (mapping == null) {
                continue;
            }
            for (String value : mapping.value()) {
                assertFalse(value.contains("league-create"), "LeagueApi 不得再暴露 league-create：方法 " + candidate.getName() + " 映射 " + value);
            }
        }
    }

    @Test
    @DisplayName("member 白名单谓词放行 /portal/league-create、拒绝 /league/create")
    void memberPredicateAllowsPortalPathOnly() throws Exception {
        Method predicate = InitDataRunner.class.getDeclaredMethod("isApiAllowedForMember", SysApi.class);
        predicate.setAccessible(true);

        SysApi portalApi = new SysApi();
        portalApi.setPath(EXPECTED_SEED_PATH);
        portalApi.setMethod("POST");
        assertTrue((Boolean) predicate.invoke(null, portalApi), "member 应放行 /portal/league-create");

        SysApi adminCreate = new SysApi();
        adminCreate.setPath("/league/create");
        adminCreate.setMethod("POST");
        assertFalse((Boolean) predicate.invoke(null, adminCreate), "member 不应直通管理端 /league/create");
    }
}
