package com.bsball.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 个人驾驶舱自由画布几何：默认装箱、重叠下移与间距校验。
 * <p>画布逻辑宽度固定为 {@value #BOARD_WIDTH}px，模块之间至少保留
 * {@value #GAP}px 间距；所有坐标与尺寸均为整数像素。</p>
 */
final class ClubhouseBoardLayout {
    static final int BOARD_WIDTH = 1216;
    static final int GAP = 16;
    static final int MAX_HEIGHT = 1600;
    static final int MAX_Y = 12000;

    /** 模块在画布上的矩形；顺序与配置数组无关，位置完全由 x/y/width/height 决定。 */
    record ModulePlacement(String key, boolean enabled, int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        ModulePlacement moveTo(int nextX, int nextY) {
            return new ModulePlacement(key, enabled, nextX, nextY, width, height);
        }
    }

    private ClubhouseBoardLayout() {
    }

    /** 按数组顺序从左到右装箱，当前行放不下时换行；行间距固定 {@value #GAP}px。 */
    static List<ModulePlacement> pack(List<ModulePlacement> modules) {
        List<ModulePlacement> out = new ArrayList<>(modules.size());
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (ModulePlacement module : modules) {
            int width = Math.min(BOARD_WIDTH, module.width());
            if (x > 0 && x + width > BOARD_WIDTH) {
                x = 0;
                y += rowHeight + GAP;
                rowHeight = 0;
            }
            out.add(new ModulePlacement(module.key(), module.enabled(), x, y, width, module.height()));
            x += width + GAP;
            rowHeight = Math.max(rowHeight, module.height());
        }
        return out;
    }

    /**
     * 按数组顺序保留几何；启用模块与已接受模块重叠时整体下移到所有已接受模块底部。
     * <p>禁用模块不参与重叠检测，但保留原始几何。</p>
     */
    static List<ModulePlacement> resolveOverlaps(List<ModulePlacement> modules) {
        List<ModulePlacement> out = new ArrayList<>(modules.size());
        List<ModulePlacement> accepted = new ArrayList<>();
        for (ModulePlacement module : modules) {
            if (!module.enabled()) {
                out.add(module);
                continue;
            }
            ModulePlacement candidate = accepted.stream().anyMatch(other -> overlaps(module, other))
                    ? placeBelow(module, accepted)
                    : module;
            out.add(candidate);
            accepted.add(candidate);
        }
        return out;
    }

    /** 放到已占用区域底部并左对齐；空列表放到原点。 */
    static ModulePlacement placeBelow(ModulePlacement module, List<ModulePlacement> placed) {
        int y = placed.isEmpty() ? 0 : maxBottom(placed) + GAP;
        return module.moveTo(0, y);
    }

    /** 两个矩形在水平或垂直方向不足 {@value #GAP}px 即视为重叠。 */
    static boolean overlaps(ModulePlacement a, ModulePlacement b) {
        return a.x() < b.right() + GAP
                && a.right() + GAP > b.x()
                && a.y() < b.bottom() + GAP
                && a.bottom() + GAP > b.y();
    }

    /** 所有模块的最大下边缘；空列表为 0。 */
    static int maxBottom(List<ModulePlacement> modules) {
        return modules.stream().mapToInt(ModulePlacement::bottom).max().orElse(0);
    }
}
