package com.rootssky.battlepass.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;
    private int level;
    private int xp;
    private boolean premium;
    private final Set<String> completedMissions;
    private final Set<String> claimedRewards;
    private String xpFormula;
    private boolean modified;
    private final Map<MissionType, Integer> missionProgress;
    private long lastDailyReset;
    private long lastWeeklyReset;
    private long lastMonthlyReset;
    private boolean vip;
    private long vipExpiresAt;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.xp = 0;
        this.premium = false;
        this.vip = false;
        this.vipExpiresAt = 0;
        this.completedMissions = new HashSet<>();
        this.claimedRewards = new HashSet<>();
        this.xpFormula = "{level} * 150";
        this.modified = false;
        this.missionProgress = new HashMap<>();
        this.lastDailyReset = 0;
        this.lastWeeklyReset = 0;
        this.lastMonthlyReset = 0;
    }

    public void resetProgress() {
        this.level = 1;
        this.xp = 0;
        this.completedMissions.clear();
        this.claimedRewards.clear();
        this.missionProgress.clear();
        this.lastDailyReset = 0;
        this.lastWeeklyReset = 0;
        this.lastMonthlyReset = 0;
        this.vip = false;
        this.vipExpiresAt = 0;
        this.modified = true;
    }

    public boolean addXP(int amount) {
        this.xp += amount;
        boolean leveledUp = false;

        while (this.xp >= calculateNextLevelXP()) {
            this.xp -= calculateNextLevelXP();
            this.level++;
            leveledUp = true;
        }

        return leveledUp;
    }

    public int calculateNextLevelXP() {
        String formula = this.xpFormula.replace("{level}", String.valueOf(level));
        try {
            String[] parts = formula.split("\\\\*");
            if (parts.length == 2) {
                double a = Double.parseDouble(parts[0].trim());
                double b = Double.parseDouble(parts[1].trim());
                return (int) (a * b);
            }
            return (int) Double.parseDouble(formula.trim());
        } catch (Exception e) {
            return level * 150;
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public Set<String> getCompletedMissions() {
        return completedMissions;
    }

    public Set<String> getClaimedRewards() {
        return claimedRewards;
    }

    public void setXpFormula(String formula) {
        this.xpFormula = formula;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public Map<MissionType, Integer> getMissionProgress() {
        return missionProgress;
    }

    public long getLastDailyReset() {
        return lastDailyReset;
    }

    public void setLastDailyReset(long lastDailyReset) {
        this.lastDailyReset = lastDailyReset;
    }

    public long getLastWeeklyReset() {
        return lastWeeklyReset;
    }

    public void setLastWeeklyReset(long lastWeeklyReset) {
        this.lastWeeklyReset = lastWeeklyReset;
    }

    public long getLastMonthlyReset() {
        return lastMonthlyReset;
    }

    public void setLastMonthlyReset(long lastMonthlyReset) {
        this.lastMonthlyReset = lastMonthlyReset;
    }

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
        this.modified = true;
    }

    public long getVipExpiresAt() {
        return vipExpiresAt;
    }

    public void setVipExpiresAt(long vipExpiresAt) {
        this.vipExpiresAt = vipExpiresAt;
        this.modified = true;
    }
}