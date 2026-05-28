package com.rootssky.battlepass.models;

import java.util.HashSet;
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

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.xp = 0;
        this.premium = false;
        this.completedMissions = new HashSet<>();
        this.claimedRewards = new HashSet<>();
        this.xpFormula = "{level} * 150";
    }

    public void resetProgress() {
        this.level = 1;
        this.xp = 0;
        this.completedMissions.clear();
        this.claimedRewards.clear(); // Clear claimed rewards to allow resgating again
        // Note: Keep config-related data (missions, rewards configuration) intact
        // This method only resets player-specific progress data
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
}