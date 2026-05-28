package com.rootssky.battlepass.managers;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.utils.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final BattlePassPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(BattlePassPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // Only reload config from disk, NEVER save defaults to preserve manual edits
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public String getXpFormula() {
        return config.getString("settings.xp-formula", "{level} * 150");
    }

    public int getMaxLevel() {
        return config.getInt("settings.max-level", 100);
    }

    public double getPremiumXpMultiplier() {
        return config.getDouble("settings.premium-xp-multiplier", 1.5);
    }

    public String getGuiTitleBattlePass() {
        String title = config.getString("gui.title-battlepass", "<gradient:#00cccc:#00ffff>Passe de Batalha</gradient>");
        return title != null ? title : "<gradient:#00cccc:#00ffff>Passe de Batalha</gradient>";
    }

    public String getGuiTitleMissions() {
        String title = config.getString("gui.title-missions", "<gradient:#55ff55:#00aa00>Missoes</gradient>");
        return title != null ? title : "<gradient:#55ff55:#00aa00>Missoes</gradient>";
    }

    public int getGuiItemsPerPage() {
        return config.getInt("gui.items-per-page", 8);
    }

    public long getResgatarTudoCooldownMs() {
        return config.getInt("gui.resgatar-tudo-cooldown", 3) * 1000L;
    }

    public String getMessage(String key) {
        String msg = config.getString("messages." + key, "<red>Mensagem não encontrada: " + key);
        return msg.replace("<prefix>", getPrefix());
    }

    public String getPrefix() {
        return config.getString("messages.prefix", "");
    }

    public int getTotalGuiPages() {
        return (int) Math.ceil((double) getMaxLevel() / getGuiItemsPerPage());
    }

    public Material getGuiMaterial(String key, Material defaultMat) {
        String matName = config.getString("gui." + key);
        if (matName == null || matName.isBlank()) {
            return defaultMat;
        }
        try {
            return Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Utils.log("<red>Material inválido no config: gui." + key + " = " + matName + ". Usando " + defaultMat + ".");
            return defaultMat;
        }
    }

    public Material getMaterialFreeAvailable() {
        return getGuiMaterial("material-free-available", Material.CYAN_STAINED_GLASS_PANE);
    }

    public Material getMaterialFreeClaimed() {
        return getGuiMaterial("material-free-claimed", Material.GRAY_STAINED_GLASS_PANE);
    }

    public Material getMaterialFreeLocked() {
        return getGuiMaterial("material-free-locked", Material.RED_STAINED_GLASS_PANE);
    }

    public Material getMaterialVipAvailable() {
        return getGuiMaterial("material-vip-available", Material.ORANGE_STAINED_GLASS_PANE);
    }

    public Material getMaterialVipClaimed() {
        return getGuiMaterial("material-vip-claimed", Material.GRAY_STAINED_GLASS_PANE);
    }

    public Material getMaterialVipLocked() {
        return getGuiMaterial("material-vip-locked", Material.RED_STAINED_GLASS_PANE);
    }

    public Material getMaterialVipNoAccess() {
        return getGuiMaterial("material-vip-no-access", Material.BLACK_STAINED_GLASS_PANE);
    }

    public Material getMaterialDivider() {
        return getGuiMaterial("material-divider", Material.GOLDEN_CHESTPLATE);
    }

    public Material getMaterialDividerNoAccess() {
        return getGuiMaterial("material-divider-no-access", Material.BARRIER);
    }

    public Material getMaterialNavigationArrow() {
        return getGuiMaterial("material-navigation-arrow", Material.ARROW);
    }

    public Material getMaterialPageIndicator() {
        return getGuiMaterial("material-page-indicator", Material.PAPER);
    }

    public Material getMaterialClose() {
        return getGuiMaterial("material-close", Material.BARRIER);
    }

    public Material getMaterialFill() {
        return getGuiMaterial("material-fill", Material.CYAN_STAINED_GLASS_PANE);
    }

    public Material getMaterialBorder() {
        return getGuiMaterial("material-border", Material.GRAY_STAINED_GLASS_PANE);
    }

    public Material getMaterialLabelFree() {
        return getGuiMaterial("material-label-free", Material.LIME_BANNER);
    }

    public Material getMaterialLabelVip() {
        return getGuiMaterial("material-label-vip", Material.YELLOW_BANNER);
    }

    public Material getMaterialLabelVipNoAccess() {
        return getGuiMaterial("material-label-vip-no-access", Material.GRAY_BANNER);
    }

    public Material getMaterialCabecalhoBg() {
        return getGuiMaterial("material-cabecalho-bg", Material.GRAY_CONCRETE);
    }

    public Material getMaterialCabecalhoPlayer() {
        return getGuiMaterial("material-cabecalho-player", Material.PLAYER_HEAD);
    }

    public Material getMaterialRecompensaFree() {
        return getGuiMaterial("material-recompensa-free", Material.CHEST);
    }

    public Material getMaterialRecompensaVip() {
        return getGuiMaterial("material-recompensa-vip", Material.GOLD_BLOCK);
    }

    public Material getMaterialResgatarTudo() {
        return getGuiMaterial("material-resgatar-tudo", Material.HOPPER);
    }

    public Material getMaterialLivroMissoes() {
        return getGuiMaterial("material-livro-missoes", Material.BOOK);
    }

    public Material getMissionIndicatorDaily() {
        return getGuiMaterial("mission-indicator-daily", Material.IRON_BLOCK);
    }

    public Material getMissionIndicatorWeekly() {
        return getGuiMaterial("mission-indicator-weekly", Material.GOLD_BLOCK);
    }

    public Material getMissionIndicatorMonthly() {
        return getGuiMaterial("mission-indicator-monthly", Material.DIAMOND_BLOCK);
    }

    public Material getMissionBackButton() {
        return getGuiMaterial("mission-back-button", Material.ARROW);
    }

    public Material getMissionCloseButton() {
        return getGuiMaterial("mission-close-button", Material.BARRIER);
    }

    public Material getMissionFill() {
        return getGuiMaterial("mission-fill", Material.CYAN_STAINED_GLASS_PANE);
    }

    public Material getMissionBook() {
        return getGuiMaterial("mission-book", Material.BOOK);
    }

    public boolean showProgressBars() {
        return config.getBoolean("gui.progress.show-progress-bars", true);
    }

    public String getProgressBarCharFilled() {
        return config.getString("gui.progress.progress-bar-char-filled", "▓");
    }

    public String getProgressBarCharEmpty() {
        return config.getString("gui.progress.progress-bar-char-empty", "░");
    }

    public int getProgressBarLength() {
        return config.getInt("gui.progress.progress-bar-length", 10);
    }
}
