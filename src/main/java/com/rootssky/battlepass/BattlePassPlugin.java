package com.rootssky.battlepass;

import com.rootssky.battlepass.commands.BattlePassCommand;
import com.rootssky.battlepass.gui.BattlePassGUI;
import com.rootssky.battlepass.gui.MissionGUI;
import com.rootssky.battlepass.listeners.MissionListener;
import com.rootssky.battlepass.listeners.PlayerListener;
import com.rootssky.battlepass.managers.ConfigManager;
import com.rootssky.battlepass.managers.DatabaseManager;
import com.rootssky.battlepass.managers.MissionManager;
import com.rootssky.battlepass.managers.RewardManager;
import com.rootssky.battlepass.models.MissionPeriod;
import com.rootssky.battlepass.models.PlayerProfile;
import com.rootssky.battlepass.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BattlePassPlugin extends JavaPlugin {

    private static BattlePassPlugin instance;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private MissionManager missionManager;
    private RewardManager rewardManager;
    private BukkitTask autoSaveTask;

    public ConcurrentHashMap<UUID, PlayerProfile> playerCache = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.reload();

        databaseManager = new DatabaseManager(this, getDataFolder());
        databaseManager.init();

        missionManager = new MissionManager(this);
        missionManager.loadMissions();
        missionManager.startResetScheduler();

        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            int saved = 0;
            for (PlayerProfile profile : playerCache.values()) {
                if (profile.isModified()) {
                    try {
                        missionManager.saveProgressToProfile(profile);
                        databaseManager.savePlayerSync(profile);
                        saved++;
                    } catch (Exception e) {
                        getLogger().warning("Auto-save falhou para " + profile.getUuid() + ": " + e.getMessage());
                    }
                }
            }
            if (saved > 0) {
                getLogger().info("Auto-save: " + saved + " perfis salvos.");
            }
        }, 6000L, 6000L); // 5 minutos (100 ticks = 5s, 6000 ticks = 5min)
        getLogger().info("Auto-save agendado a cada 5 minutos.");

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            long now = System.currentTimeMillis();
            for (PlayerProfile profile : playerCache.values()) {
                if (profile.isVip() && profile.getVipExpiresAt() != -1 && profile.getVipExpiresAt() < now) {
                    profile.setVip(false);
                    profile.setVipExpiresAt(0);
                    try {
                        databaseManager.savePlayerSync(profile);
                    } catch (Exception e) {
                        getLogger().warning("Falha ao salvar expiração VIP para " + profile.getUuid() + ": " + e.getMessage());
                    }
                    UUID uuid = profile.getUuid();
                    Bukkit.getScheduler().runTask(this, () -> {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage(Utils.applyPrefix("<red>❌ <bold>VIP EXPIROU</bold> <red>Seu Battle Pass VIP expirou!</red>"));
                        }
                    });
                }
            }
        }, 12000L, 12000L);
        getLogger().info("Verificação de expiração VIP agendada a cada 10 minutos.");

        rewardManager = new RewardManager(this);
        rewardManager.setupEconomy();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MissionListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);

        BattlePassCommand commandHandler = new BattlePassCommand(this);
        getCommand("battlepass").setExecutor(commandHandler);
        getCommand("battlepass").setTabCompleter(commandHandler);
        getCommand("passeadmin").setExecutor(commandHandler);
        getCommand("passeadmin").setTabCompleter(commandHandler);

        // Register the new reset command
        com.rootssky.battlepass.commands.BattlePassResetCommand resetCommand = new com.rootssky.battlepass.commands.BattlePassResetCommand(this);
        if (getCommand("bpreset") != null) {
            getCommand("bpreset").setExecutor(resetCommand);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.rootssky.battlepass.placeholders.RootsSkyPlaceholderExpansion(this).register();
            Utils.log("<green>PlaceholderAPI integrado!");
        }

        Utils.log("<green>BattlePass ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Salvando dados antes do desligamento...");

        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        if (missionManager != null && databaseManager != null) {
            missionManager.saveAllProgressToProfiles();

            for (Map.Entry<MissionPeriod, Long> entry : missionManager.getLastReset().entrySet()) {
                databaseManager.saveResetTimestampSync(entry.getKey().name(), entry.getValue());
            }
        }

        List<PlayerProfile> perfis = new ArrayList<>(playerCache.values());
        playerCache.clear();

        if (databaseManager != null && !perfis.isEmpty()) {
            databaseManager.flushAll(perfis).join();
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (rewardManager != null) rewardManager.limparCooldown(online.getUniqueId());
            if (missionManager != null) missionManager.removePlayer(online.getUniqueId());
        }

        if (missionManager != null) missionManager.stopResetScheduler();

        if (databaseManager != null) {
            databaseManager.close();
        }

        Utils.log("<red>BattlePass desativado.");
    }

    public static BattlePassPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MissionManager getMissionManager() {
        return missionManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    private class GUIListener implements Listener {

        @EventHandler
        public void aoClicarInventario(InventoryClickEvent event) {
            InventoryHolder holder = event.getInventory().getHolder();

            if (holder instanceof BattlePassGUI) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    BattlePassGUI.aoClicar(BattlePassPlugin.this, player, event.getRawSlot(), event.getCurrentItem());
                }
            } else if (holder instanceof MissionGUI) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    MissionGUI.aoClicar(BattlePassPlugin.this, player, event.getRawSlot(), event.getCurrentItem());
                }
            }
        }
    }
}
