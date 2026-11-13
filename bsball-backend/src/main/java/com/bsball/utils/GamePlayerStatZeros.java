package com.bsball.utils;

import com.bsball.model.entity.GamePlayerStat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 比赛球员统计行的“全零”判定与剩余字段诊断。
 *
 * <p>只统计比赛数据字段；比赛/球队/球员、打序/登板序/列表序、守位、号码、
 * 左右打/投等身份与展示字段不计入，避免一条没有出赛数据的投手行因排序字段非空而无法清理。
 * `pitchPa` 已开放为可编辑列（与 BF 同步），因此重新纳入全零判定；
 * 实体新增统计字段时，必须同步加入下面的清单。</p>
 */
public final class GamePlayerStatZeros {

    private GamePlayerStatZeros() {
    }

    /** 一个统计字段值，供基座对比/删除诊断使用。 */
    public record FieldValue(String key, String label, double value) {
    }

    /** 一个非零统计字段，供删除诊断展示中文名与值。 */
    public record NonZeroField(String key, String label, double value) {
    }

    private record Field(
            String key,
            String label,
            Function<GamePlayerStat, Number> getter) {
    }

    private static final List<Field> FIELDS = List.of(
            new Field("pa", "PA", GamePlayerStat::getPa),
            new Field("ab", "AB", GamePlayerStat::getAb),
            new Field("r", "R", GamePlayerStat::getR),
            new Field("h", "H", GamePlayerStat::getH),
            new Field("e", "E", GamePlayerStat::getE),
            new Field("bbHp", "BB+HP", GamePlayerStat::getBbHp),
            new Field("sb", "SB", GamePlayerStat::getSb),
            new Field("so", "SO", GamePlayerStat::getSo),
            new Field("soSwing", "挥空", GamePlayerStat::getSoSwing),
            new Field("soLooking", "看三振", GamePlayerStat::getSoLooking),
            new Field("rbi", "RBI", GamePlayerStat::getRbi),
            new Field("doubles", "2B", GamePlayerStat::getDoubles),
            new Field("triples", "3B", GamePlayerStat::getTriples),
            new Field("hr", "HR", GamePlayerStat::getHr),
            new Field("insideParkHr", "场内本垒打", GamePlayerStat::getInsideParkHr),
            new Field("gdp", "GDP", GamePlayerStat::getGdp),
            new Field("sh", "SH", GamePlayerStat::getSh),
            new Field("sf", "SF", GamePlayerStat::getSf),
            new Field("bb", "BB", GamePlayerStat::getBb),
            new Field("ibb", "IBB", GamePlayerStat::getIbb),
            new Field("hbp", "HBP", GamePlayerStat::getHbp),
            new Field("io", "IO", GamePlayerStat::getIo),
            new Field("cs", "CS", GamePlayerStat::getCs),
            new Field("ip", "IP", GamePlayerStat::getIp),
            new Field("outsRecorded", "OUTS", GamePlayerStat::getOutsRecorded),
            new Field("earnedR", "自责(旧)", GamePlayerStat::getEarnedR),
            new Field("unearnedR", "非自责", GamePlayerStat::getUnearnedR),
            new Field("pendingR", "待定", GamePlayerStat::getPendingR),
            new Field("er", "ER", GamePlayerStat::getEr),
            new Field("pitchH", "被安打", GamePlayerStat::getPitchH),
            new Field("pitchDoubles", "被二垒打", GamePlayerStat::getPitchDoubles),
            new Field("pitchTriples", "被三垒打", GamePlayerStat::getPitchTriples),
            new Field("pitchBbHp", "被四坏+触身", GamePlayerStat::getPitchBbHp),
            new Field("pitchSo", "夺三振", GamePlayerStat::getPitchSo),
            new Field("pitchHr", "被本垒打", GamePlayerStat::getPitchHr),
            new Field("pitchInsideParkHr", "被场内本垒打", GamePlayerStat::getPitchInsideParkHr),
            new Field("pitchSh", "被牺牲短打", GamePlayerStat::getPitchSh),
            new Field("pitchSf", "被牺牲飞球", GamePlayerStat::getPitchSf),
            new Field("gs", "GS", GamePlayerStat::getGs),
            new Field("svo", "SVO", GamePlayerStat::getSvo),
            new Field("cg", "CG", GamePlayerStat::getCg),
            new Field("pg", "PG", GamePlayerStat::getPg),
            new Field("w", "W", GamePlayerStat::getW),
            new Field("l", "L", GamePlayerStat::getL),
            new Field("sv", "SV", GamePlayerStat::getSv),
            new Field("hld", "HLD", GamePlayerStat::getHld),
            new Field("pitchBf", "BF", GamePlayerStat::getPitchBf),
            new Field("pitchPa", "PA(投手)", GamePlayerStat::getPitchPa),
            new Field("pitchAb", "AB(投手)", GamePlayerStat::getPitchAb),
            new Field("np", "NP", GamePlayerStat::getNp),
            new Field("pitchBb", "BB(投手)", GamePlayerStat::getPitchBb),
            new Field("pitchIbb", "IBB(投手)", GamePlayerStat::getPitchIbb),
            new Field("pitchHbp", "HBP(投手)", GamePlayerStat::getPitchHbp),
            new Field("pitchIo", "IO(投手)", GamePlayerStat::getPitchIo),
            new Field("pickoffAttempts", "牵制企图", GamePlayerStat::getPickoffAttempts),
            new Field("wp", "WP", GamePlayerStat::getWp),
            new Field("bk", "BK", GamePlayerStat::getBk),
            new Field("pitchR", "R(投手)", GamePlayerStat::getPitchR),
            new Field("go", "GO", GamePlayerStat::getGo),
            new Field("fo", "FO", GamePlayerStat::getFo),
            new Field("tc", "TC", GamePlayerStat::getTc),
            new Field("po", "PO", GamePlayerStat::getPo),
            new Field("a", "A", GamePlayerStat::getA),
            new Field("fieldingGs", "守备GS", GamePlayerStat::getFieldingGs),
            new Field("defInn", "守备局数", GamePlayerStat::getDefInn),
            new Field("dp", "DP", GamePlayerStat::getDp),
            new Field("pb", "PB", GamePlayerStat::getPb),
            new Field("catcherSb", "捕手SB", GamePlayerStat::getCatcherSb),
            new Field("catcherCs", "捕手CS", GamePlayerStat::getCatcherCs)
    );

    /**
     * 该统计行是否没有任何出赛数据（null 与 0 都视为零）。
     *
     * @param stat 比赛球员统计行；null 返回 false
     * @return true 表示所有统计字段均为 null/0
     */
    public static boolean isZero(GamePlayerStat stat) {
        return stat != null && nonZeroFields(stat).isEmpty();
    }

    /** 返回所有非零统计字段（按清单顺序），供删除诊断使用。 */
    public static List<NonZeroField> nonZeroFields(GamePlayerStat stat) {
        List<NonZeroField> fields = new ArrayList<>();
        for (FieldValue value : values(stat)) {
            if (value.value() != 0d) {
                fields.add(new NonZeroField(value.key(), value.label(), value.value()));
            }
        }
        return fields;
    }

    /** 返回全部统计字段的值（null 按 0），供基座与有效值逐字段对比。 */
    public static List<FieldValue> values(GamePlayerStat stat) {
        if (stat == null) {
            return List.of();
        }
        List<FieldValue> fields = new ArrayList<>(FIELDS.size());
        for (Field field : FIELDS) {
            Number raw = field.getter().apply(stat);
            fields.add(new FieldValue(
                    field.key(), field.label(), raw == null ? 0d : raw.doubleValue()));
        }
        return fields;
    }
}
