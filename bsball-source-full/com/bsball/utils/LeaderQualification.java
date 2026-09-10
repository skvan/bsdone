/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.utils.LeaderQualification
 */
package com.bsball.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Exception performing whole class analysis ignored.
 */
public final class LeaderQualification {
    public static final double BATTING_PA_COEFF = 1.3;
    public static final int PITCHING_IP_PER_SCHEDULED_GAME = 1;

    private LeaderQualification() {
    }

    public static int minPlateAppearances(int teamScheduledGames) {
        if (teamScheduledGames <= 0) {
            return 0;
        }
        return (int)Math.ceil((double)teamScheduledGames * 1.3);
    }

    public static int minInningsPitched(int teamScheduledGames) {
        if (teamScheduledGames <= 0) {
            return 0;
        }
        return teamScheduledGames * 1;
    }

    public static Map<String, Object> battingMeta(int maxTeamScheduledGames) {
        LinkedHashMap<String, Object> q = new LinkedHashMap<String, Object>();
        q.put("paCoeff", 1.3);
        q.put("maxTeamScheduledGames", maxTeamScheduledGames);
        q.put("exampleMinPa", LeaderQualification.minPlateAppearances((int)maxTeamScheduledGames));
        return q;
    }

    public static Map<String, Object> pitchingMeta(int maxTeamScheduledGames) {
        LinkedHashMap<String, Object> q = new LinkedHashMap<String, Object>();
        q.put("ipPerScheduledGame", 1);
        q.put("maxTeamScheduledGames", maxTeamScheduledGames);
        q.put("exampleMinIp", LeaderQualification.minInningsPitched((int)maxTeamScheduledGames));
        return q;
    }
}

