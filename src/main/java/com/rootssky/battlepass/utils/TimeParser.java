package com.rootssky.battlepass.utils;

public final class TimeParser {

    private TimeParser() {
    }

    public static long parseToMillis(String timeStr) {
        if (timeStr.equalsIgnoreCase("permanent") || timeStr.equalsIgnoreCase("perm")) {
            return -1;
        }

        long amount = Long.parseLong(timeStr.replaceAll("[^0-9]", ""));
        String unit = timeStr.replaceAll("[0-9]", "").toLowerCase();

        return switch (unit) {
            case "mo" -> amount * 30 * 24 * 60 * 60 * 1000L;
            case "d" -> amount * 24 * 60 * 60 * 1000L;
            case "h" -> amount * 60 * 60 * 1000L;
            case "m" -> amount * 60 * 1000L;
            case "s" -> amount * 1000L;
            default -> throw new IllegalArgumentException("Unidade inválida: " + unit);
        };
    }
}
