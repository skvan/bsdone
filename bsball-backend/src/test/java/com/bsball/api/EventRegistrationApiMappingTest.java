package com.bsball.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * 电子秩序册（Issue #310）控制器映射守卫：
 * 防「服务/种子就位但控制器漏映射」类缺口（批 3 评审实证教训），对全部 12 个端点做映射存在性断言。
 */
@DisplayName("电子秩序册 控制器映射守卫（Issue #310）")
class EventRegistrationApiMappingTest {

    private static boolean hasMapping(Method method, String httpMethod, String path) {
        switch (httpMethod) {
            case "GET" -> {
                GetMapping m = method.getAnnotation(GetMapping.class);
                return m != null && Arrays.asList(m.value()).contains(path);
            }
            case "POST" -> {
                PostMapping m = method.getAnnotation(PostMapping.class);
                return m != null && Arrays.asList(m.value()).contains(path);
            }
            case "PUT" -> {
                PutMapping m = method.getAnnotation(PutMapping.class);
                return m != null && Arrays.asList(m.value()).contains(path);
            }
            default -> {
                return false;
            }
        }
    }

    @Test
    @DisplayName("12 个端点全部具备控制器映射（路径与权限种子一致）")
    void allEndpointsMapped() throws Exception {
        record Expectation(String method, String javaName, Class<?>[] params, String http, String path) {
        }
        List<Expectation> expectations = List.of(
                new Expectation("invite", "invite", new Class<?>[]{Long.class, com.bsball.model.dto.EventTeamInviteRequest.class}, "POST", "/event/{eventId}/teams/invite"),
                new Expectation("listEventTeams", "listEventTeams", new Class<?>[]{Long.class}, "GET", "/event/{eventId}/teams"),
                new Expectation("invitableTeams", "invitableTeams", new Class<?>[]{Long.class}, "GET", "/event/{eventId}/team-options"),
                new Expectation("remove", "remove", new Class<?>[]{Long.class, Long.class}, "POST", "/event/{eventId}/teams/{teamId}/remove"),
                new Expectation("lock", "lock", new Class<?>[]{Long.class, Long.class}, "POST", "/event/{eventId}/teams/{teamId}/lock"),
                new Expectation("unlock", "unlock", new Class<?>[]{Long.class, Long.class}, "POST", "/event/{eventId}/teams/{teamId}/unlock"),
                new Expectation("roster", "roster", new Class<?>[]{Long.class, Long.class}, "GET", "/event/{eventId}/teams/{teamId}/roster"),
                new Expectation("rosterPlayers", "rosterPlayers", new Class<?>[]{Long.class, Long.class}, "GET", "/event/{eventId}/teams/{teamId}/roster-players"),
                new Expectation("myRegistrations", "myRegistrations", new Class<?>[]{Long.class}, "GET", "/team/{teamId}/event-registrations"),
                new Expectation("confirm", "confirm", new Class<?>[]{Long.class, Long.class}, "POST", "/event/{eventId}/teams/{teamId}/confirm"),
                new Expectation("decline", "decline", new Class<?>[]{Long.class, Long.class}, "POST", "/event/{eventId}/teams/{teamId}/decline"),
                new Expectation("submitRoster", "submitRoster", new Class<?>[]{Long.class, Long.class, com.bsball.model.dto.EventRosterSubmitRequest.class}, "PUT", "/event/{eventId}/teams/{teamId}/roster"));

        assertEquals(12, expectations.size());
        for (Expectation e : expectations) {
            Method m = EventRegistrationApi.class.getMethod(e.javaName(), e.params());
            assertNotNull(m, e.path() + " 方法应存在");
            assertTrue(hasMapping(m, e.http(), e.path()),
                    e.http() + " " + e.path() + " 应具备对应注解映射（防服务/种子就位而控制器漏映射）");
        }
    }
}
