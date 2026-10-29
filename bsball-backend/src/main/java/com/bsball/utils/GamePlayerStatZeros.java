package com.bsball.utils;

import com.bsball.model.entity.GamePlayerStat;

/**
 * 比赛球员统计行的“全零”判定。
 *
 * <p>只统计比赛数据字段；比赛/球队/球员、打序/登板序/列表序、守位、号码、
 * 左右打/投等身份与展示字段不计入，避免一条没有出赛数据的投手行因排序字段非空而无法清理。
 * `pitchPa` 是结果页不暴露、前端无法清零的辅助计数；投手面对打者以 `pitchBf` 为准，
 * 故仅 `pitchPa` 非零（即使 `pitchBf` 为 0）也视为零数据，否则这类投手行会永远删不掉。
 * 实体新增统计字段时，必须同步加入下面的清单。</p>
 */
public final class GamePlayerStatZeros {

    private GamePlayerStatZeros() {
    }

    /**
     * 该统计行是否没有任何出赛数据（null 与 0 都视为零）。
     *
     * @param stat 比赛球员统计行；null 返回 false
     * @return true 表示所有统计字段均为 null/0
     */
    public static boolean isZero(GamePlayerStat stat) {
        if (stat == null) {
            return false;
        }
        return allZero(
                stat.getPa(),
                stat.getAb(),
                stat.getR(),
                stat.getH(),
                stat.getE(),
                stat.getBbHp(),
                stat.getSb(),
                stat.getSo(),
                stat.getSoSwing(),
                stat.getSoLooking(),
                stat.getRbi(),
                stat.getDoubles(),
                stat.getTriples(),
                stat.getHr(),
                stat.getInsideParkHr(),
                stat.getGdp(),
                stat.getSh(),
                stat.getSf(),
                stat.getBb(),
                stat.getIbb(),
                stat.getHbp(),
                stat.getIo(),
                stat.getCs(),
                stat.getIp(),
                stat.getEarnedR(),
                stat.getUnearnedR(),
                stat.getPendingR(),
                stat.getEr(),
                stat.getPitchH(),
                stat.getPitchDoubles(),
                stat.getPitchTriples(),
                stat.getPitchBbHp(),
                stat.getPitchSo(),
                stat.getPitchHr(),
                stat.getPitchInsideParkHr(),
                stat.getPitchSh(),
                stat.getPitchSf(),
                stat.getGs(),
                stat.getSvo(),
                stat.getCg(),
                stat.getPg(),
                stat.getW(),
                stat.getL(),
                stat.getSv(),
                stat.getHld(),
                stat.getPitchBf(),
                stat.getPitchAb(),
                stat.getNp(),
                stat.getPitchBb(),
                stat.getPitchIbb(),
                stat.getPitchHbp(),
                stat.getPitchIo(),
                stat.getWp(),
                stat.getBk(),
                stat.getPitchR(),
                stat.getGo(),
                stat.getFo(),
                stat.getTc(),
                stat.getPo(),
                stat.getA(),
                stat.getFieldingGs(),
                stat.getDefInn(),
                stat.getDp(),
                stat.getPb(),
                stat.getCatcherSb(),
                stat.getCatcherCs());
    }

    private static boolean allZero(Number... values) {
        for (Number value : values) {
            if (value != null && value.doubleValue() != 0d) {
                return false;
            }
        }
        return true;
    }
}
