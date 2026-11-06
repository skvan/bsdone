package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bsball.model.entity.SysMenu;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 数据补录权限口径：办赛方、租管、超管均可进入并保存，不新增动作级门槛。 */
class GameBackfillPermissionTest {

    @Test
    @DisplayName("租管默认菜单包含比赛编辑与结果保存，可进入数据补录/复核")
    void tenantAdminDefaultsIncludeBackfillPermissions() throws Exception {
        List<SysMenu> menus = List.of(
                menu(1L, 0L, "/business", null),
                menu(2L, 1L, "/admin/events", null),
                menu(3L, 2L, null, "business:game:edit"),
                menu(4L, 2L, null, "business:game:saveResult"));

        Method method = InitDataRunner.class.getDeclaredMethod(
                "collectTenantAdminDefaultMenuIds", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Long> defaults = (Set<Long>) method.invoke(null, menus);

        assertTrue(defaults.contains(3L), "租管默认集合应包含 business:game:edit");
        assertTrue(defaults.contains(4L), "租管默认集合应包含 business:game:saveResult");
    }

    @Test
    @DisplayName("办赛方预设权限包含比赛编辑与结果保存")
    void leagueOrganizerPresetIncludesBackfillPermissions() throws Exception {
        Field field = InitDataRunner.class.getDeclaredField(
                "PORTAL_LEAGUE_ORGANIZER_PERMISSIONS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) field.get(null);

        assertTrue(permissions.contains("business:game:edit"),
                "办赛方预设应包含 business:game:edit");
        assertTrue(permissions.contains("business:game:saveResult"),
                "办赛方预设应包含 business:game:saveResult");
    }

    private static SysMenu menu(long id, long parentId, String path, String permission) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setPath(path);
        m.setPermission(permission);
        return m;
    }
}
