package com.nba.nbanonbettingapp.dto;

import com.nba.nbanonbettingapp.dto.BdlStatDTO;

/**
 * Supported stat types for prop analysis.
 * Maps the user-facing string (e.g. "ast") to the correct BdlStatDTO field.
 *
 * Usage: StatType.from("ast").extract(stat)
 */
public enum StatType {

    PTS("pts") {
        @Override public Integer extract(BdlStatDTO s) { return s.pts(); }
    },
    REB("reb") {
        @Override public Integer extract(BdlStatDTO s) { return s.reb(); }
    },
    AST("ast") {
        @Override public Integer extract(BdlStatDTO s) { return s.ast(); }
    },
    STL("stl") {
        @Override public Integer extract(BdlStatDTO s) { return s.stl(); }
    },
    BLK("blk") {
        @Override public Integer extract(BdlStatDTO s) { return s.blk(); }
    },
    TURNOVER("turnover") {
        @Override public Integer extract(BdlStatDTO s) { return s.turnover(); }
    },
    FG3M("fg3m") {
        @Override public Integer extract(BdlStatDTO s) { return s.fg3m(); }
    };

    private final String key;

    StatType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /** Extracts the relevant stat value from a BdlStatDTO. Returns null if not recorded. */
    public abstract Integer extract(BdlStatDTO s);

    /**
     * Resolves a string key to a StatType.
     * Case-insensitive. Throws if the stat type is not supported.
     */
    public static StatType from(String key) {
        for (StatType type : values()) {
            if (type.key.equalsIgnoreCase(key)) return type;
        }
        throw new IllegalArgumentException(
                "Unsupported statType: '" + key + "'. " +
                        "Supported values: pts, reb, ast, stl, blk, turnover, fg3m");
    }
}