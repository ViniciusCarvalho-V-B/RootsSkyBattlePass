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
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        config.addDefault("settings.xp-formula", "{level} * 150");
        config.addDefault("settings.max-level", 100);
        config.addDefault("settings.premium-xp-multiplier", 1.5);

        config.addDefault("gui.title-battlepass", "<gradient:#00cccc:#00ffff>Passe de Batalha</gradient> <dark_gray>— <gradient:#55ff55:#00aa00>RootsSky</gradient>");
        config.addDefault("gui.title-missions", "<gradient:#55ff55:#00aa00>Missoes</gradient> <dark_gray>— <gradient:#00cccc:#00ffff>Passe de Batalha</gradient>");
        config.addDefault("gui.items-per-page", 8);
        config.addDefault("gui.resgatar-tudo-cooldown", 3);
        config.addDefault("gui.material-free-available", "CYAN_STAINED_GLASS_PANE");
        config.addDefault("gui.material-free-claimed", "GRAY_STAINED_GLASS_PANE");
        config.addDefault("gui.material-free-locked", "RED_STAINED_GLASS_PANE");
        config.addDefault("gui.material-vip-available", "ORANGE_STAINED_GLASS_PANE");
        config.addDefault("gui.material-vip-claimed", "GRAY_STAINED_GLASS_PANE");
        config.addDefault("gui.material-vip-locked", "RED_STAINED_GLASS_PANE");
        config.addDefault("gui.material-vip-no-access", "BLACK_STAINED_GLASS_PANE");
        config.addDefault("gui.material-divider", "GOLDEN_CHESTPLATE");
        config.addDefault("gui.material-divider-no-access", "BARRIER");
        config.addDefault("gui.material-navigation-arrow", "ARROW");
        config.addDefault("gui.material-page-indicator", "PAPER");
        config.addDefault("gui.material-close", "BARRIER");
        config.addDefault("gui.material-fill", "CYAN_STAINED_GLASS_PANE");
        config.addDefault("gui.material-border", "GRAY_STAINED_GLASS_PANE");
        config.addDefault("gui.material-label-free", "LIME_BANNER");
        config.addDefault("gui.material-label-vip", "YELLOW_BANNER");
        config.addDefault("gui.material-label-vip-no-access", "GRAY_BANNER");
        config.addDefault("gui.material-cabecalho-bg", "GRAY_CONCRETE");
        config.addDefault("gui.material-cabecalho-player", "PLAYER_HEAD");
        config.addDefault("gui.material-recompensa-free", "CHEST");
        config.addDefault("gui.material-recompensa-vip", "GOLD_BLOCK");
        config.addDefault("gui.material-resgatar-tudo", "HOPPER");
        config.addDefault("gui.material-livro-missoes", "BOOK");
        config.addDefault("gui.mission-indicator-daily", "IRON_BLOCK");
        config.addDefault("gui.mission-indicator-weekly", "GOLD_BLOCK");
        config.addDefault("gui.mission-indicator-monthly", "DIAMOND_BLOCK");
        config.addDefault("gui.mission-back-button", "ARROW");
        config.addDefault("gui.mission-close-button", "BARRIER");
        config.addDefault("gui.mission-fill", "GRAY_CONCRETE");
        config.addDefault("gui.mission-book", "BOOK");
        config.addDefault("gui.progress.show-progress-bars", true);
        config.addDefault("gui.progress.progress-bar-char-filled", "▓");
        config.addDefault("gui.progress.progress-bar-char-empty", "░");
        config.addDefault("gui.progress.progress-bar-length", 10);

        config.addDefault("messages.prefix", "<gradient:#55ff55:#00aa00><bold>[RootsSky]</bold></gradient> ");
        config.addDefault("messages.welcome", "<gray>Olá <white>%player%</white>! Seu nível do Passe: <gold>%level%</gold>");
        config.addDefault("messages.level-up", "<gradient:#ffaa00:#ffff55>⚡ <bold>LEVEL UP!</bold> Agora você é Nível <gold>%level%</gold>!</gradient>");
        config.addDefault("messages.xp-gained", "<gray>+%xp% XP</gray>");
        config.addDefault("messages.no-permission", "<red>✗ Sem permissão.</red>");
        config.addDefault("messages.reloaded", "<green>✓ Configurações recarregadas!</green>");
        config.addDefault("messages.profile", "<yellow>%player%</yellow> <gray>- Nível: <gold>%level%</gold> | XP: <gold>%xp%</gold>/<gold>%required%</gold>");
        config.addDefault("messages.premium-only", "<gradient:#ffaa00:#ffff55>⭐ Isso é exclusivo para jogadores <bold>VIP</bold>!</gradient>");
        config.addDefault("messages.reward-claimed", "<green>✅ Recompensa do nível <bold>%nivel%</bold> resgatada!</green>");
        config.addDefault("messages.reward-already", "<gray>⚠️ Você já resgatou esta recompensa.</gray>");
        config.addDefault("messages.reward-locked", "<red>🔒 Nível <bold>%nivel%</bold> necessário para esta recompensa.</red>");
        config.addDefault("messages.vip-required", "<gradient:#ffaa00:#ffff55>⭐ Esta recompensa é exclusiva para jogadores <bold>VIP</bold>!</gradient>");
        config.addDefault("messages.no-reward", "<gray>❌ Nenhuma recompensa configurada para este nível.</gray>");
        config.addDefault("messages.mission-claimed", "<green>Recompensa da missao <bold>%mission%</bold> resgatada! <dark_gray>(+%xp% XP)%xp_bonus%</dark_gray></green>");
        config.addDefault("messages.mission-reset", "<gray>Missões %period% foram resetadas!</gray>");

        config.addDefault("missions.daily.mine_50.type", "MINERAR");
        config.addDefault("missions.daily.mine_50.target", 50);
        config.addDefault("missions.daily.mine_50.xp_reward", 30);
        config.addDefault("missions.daily.mine_50.reset_hours", 24);
        config.addDefault("missions.daily.mine_50.icon", "IRON_PICKAXE");
        config.addDefault("missions.daily.mine_50.name", "Minerador Iniciante");
        config.addDefault("missions.daily.mine_50.description", "Minere 50 blocos de minerio");
        config.addDefault("missions.daily.kill_20.type", "MATAR_MOBS");
        config.addDefault("missions.daily.kill_20.target", 20);
        config.addDefault("missions.daily.kill_20.xp_reward", 40);
        config.addDefault("missions.daily.kill_20.reset_hours", 24);
        config.addDefault("missions.daily.kill_20.icon", "IRON_SWORD");
        config.addDefault("missions.daily.kill_20.name", "Cacador de Mobs");
        config.addDefault("missions.daily.kill_20.description", "Elimine 20 mobs hostis");
        config.addDefault("missions.daily.fish_15.type", "PESCAR");
        config.addDefault("missions.daily.fish_15.target", 15);
        config.addDefault("missions.daily.fish_15.xp_reward", 35);
        config.addDefault("missions.daily.fish_15.reset_hours", 24);
        config.addDefault("missions.daily.fish_15.icon", "FISHING_ROD");
        config.addDefault("missions.daily.fish_15.name", "Pescador");
        config.addDefault("missions.daily.fish_15.description", "Pesque 15 peixes");
        config.addDefault("missions.daily.craft_10.type", "CRAFTAR");
        config.addDefault("missions.daily.craft_10.target", 10);
        config.addDefault("missions.daily.craft_10.xp_reward", 25);
        config.addDefault("missions.daily.craft_10.reset_hours", 24);
        config.addDefault("missions.daily.craft_10.icon", "CRAFTING_TABLE");
        config.addDefault("missions.daily.craft_10.name", "Artesao");
        config.addDefault("missions.daily.craft_10.description", "Crafte 10 itens");
        config.addDefault("missions.daily.harvest_64.type", "COLHER");
        config.addDefault("missions.daily.harvest_64.target", 64);
        config.addDefault("missions.daily.harvest_64.xp_reward", 30);
        config.addDefault("missions.daily.harvest_64.reset_hours", 24);
        config.addDefault("missions.daily.harvest_64.icon", "GOLDEN_HOE");
        config.addDefault("missions.daily.harvest_64.name", "Fazendeiro");
        config.addDefault("missions.daily.harvest_64.description", "Colha 64 plantacoes");
        config.addDefault("missions.daily.eat_5.type", "COMER");
        config.addDefault("missions.daily.eat_5.target", 5);
        config.addDefault("missions.daily.eat_5.xp_reward", 15);
        config.addDefault("missions.daily.eat_5.reset_hours", 24);
        config.addDefault("missions.daily.eat_5.icon", "COOKED_BEEF");
        config.addDefault("missions.daily.eat_5.name", "Guloso");
        config.addDefault("missions.daily.eat_5.description", "Coma 5 itens");
        config.addDefault("missions.daily.build_200.type", "COLOCAR_BLOCOS");
        config.addDefault("missions.daily.build_200.target", 200);
        config.addDefault("missions.daily.build_200.xp_reward", 50);
        config.addDefault("missions.daily.build_200.reset_hours", 24);
        config.addDefault("missions.daily.build_200.icon", "BRICKS");
        config.addDefault("missions.daily.build_200.name", "Construtor");
        config.addDefault("missions.daily.build_200.description", "Coloque 200 blocos");

        config.addDefault("missions.weekly.kill_100.type", "MATAR_MOBS");
        config.addDefault("missions.weekly.kill_100.target", 100);
        config.addDefault("missions.weekly.kill_100.xp_reward", 150);
        config.addDefault("missions.weekly.kill_100.reset_hours", 168);
        config.addDefault("missions.weekly.kill_100.icon", "DIAMOND_SWORD");
        config.addDefault("missions.weekly.kill_100.name", "Guerreiro Semanal");
        config.addDefault("missions.weekly.kill_100.description", "Elimine 100 mobs hostis");
        config.addDefault("missions.weekly.mine_200.type", "MINERAR");
        config.addDefault("missions.weekly.mine_200.target", 200);
        config.addDefault("missions.weekly.mine_200.xp_reward", 200);
        config.addDefault("missions.weekly.mine_200.reset_hours", 168);
        config.addDefault("missions.weekly.mine_200.icon", "DIAMOND_PICKAXE");
        config.addDefault("missions.weekly.mine_200.name", "Mineiro Profissional");
        config.addDefault("missions.weekly.mine_200.description", "Mine 200 blocos de minerio");
        config.addDefault("missions.weekly.fish_50.type", "PESCAR");
        config.addDefault("missions.weekly.fish_50.target", 50);
        config.addDefault("missions.weekly.fish_50.xp_reward", 120);
        config.addDefault("missions.weekly.fish_50.reset_hours", 168);
        config.addDefault("missions.weekly.fish_50.icon", "FISHING_ROD");
        config.addDefault("missions.weekly.fish_50.name", "Mestre Pescador");
        config.addDefault("missions.weekly.fish_50.description", "Pesque 50 peixes");
        config.addDefault("missions.weekly.craft_50.type", "CRAFTAR");
        config.addDefault("missions.weekly.craft_50.target", 50);
        config.addDefault("missions.weekly.craft_50.xp_reward", 180);
        config.addDefault("missions.weekly.craft_50.reset_hours", 168);
        config.addDefault("missions.weekly.craft_50.icon", "CRAFTING_TABLE");
        config.addDefault("missions.weekly.craft_50.name", "Fabricante Semanal");
        config.addDefault("missions.weekly.craft_50.description", "Crafte 50 itens");

        config.addDefault("missions.monthly.kill_500.type", "MATAR_MOBS");
        config.addDefault("missions.monthly.kill_500.target", 500);
        config.addDefault("missions.monthly.kill_500.xp_reward", 500);
        config.addDefault("missions.monthly.kill_500.reset_hours", 720);
        config.addDefault("missions.monthly.kill_500.icon", "NETHERITE_SWORD");
        config.addDefault("missions.monthly.kill_500.name", "Lenda de Guerra");
        config.addDefault("missions.monthly.kill_500.description", "Elimine 500 mobs hostis");
        config.addDefault("missions.monthly.mine_1000.type", "MINERAR");
        config.addDefault("missions.monthly.mine_1000.target", 1000);
        config.addDefault("missions.monthly.mine_1000.xp_reward", 600);
        config.addDefault("missions.monthly.mine_1000.reset_hours", 720);
        config.addDefault("missions.monthly.mine_1000.icon", "NETHERITE_PICKAXE");
        config.addDefault("missions.monthly.mine_1000.name", "Escavador Lendario");
        config.addDefault("missions.monthly.mine_1000.description", "Mine 1000 blocos de minerio");

        config.options().copyDefaults(true);
        plugin.saveConfig();
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
