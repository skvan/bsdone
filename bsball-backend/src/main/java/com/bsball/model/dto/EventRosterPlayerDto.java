package com.bsball.model.dto;

import java.util.List;

/**
 * 秩序册报名球员视图 DTO：
 * {@code number} 优先取报名时快照，缺失时回落球员档案背号；{@code avatar} 取球员档案头像（卡片展示用，可空）；
 * {@code currentInTeam} 标识球员当前是否仍在本队（提示用；整册页仅展示在役）。
 */
public record EventRosterPlayerDto(
        Long playerId,
        String name,
        String avatar,
        String number,
        List<String> positions,
        String status,
        Boolean currentInTeam) {
}
