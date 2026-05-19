package com.rootssky.battlepass.models;

public enum MissionPeriod {

    DAILY(24),
    WEEKLY(168),
    MONTHLY(720);

    private final int defaultHours;

    MissionPeriod(int defaultHours) {
        this.defaultHours = defaultHours;
    }

    public int getDefaultHours() {
        return defaultHours;
    }

    public long getDefaultMillis() {
        return (long) defaultHours * 3600_000L;
    }

    public String getDisplayName() {
        return switch (this) {
            case DAILY -> "Diária";
            case WEEKLY -> "Semanal";
            case MONTHLY -> "Mensal";
        };
    }

    public String getColorTag() {
        return switch (this) {
            case DAILY -> "<green>";
            case WEEKLY -> "<gold>";
            case MONTHLY -> "<light_purple>";
        };
    }
}
