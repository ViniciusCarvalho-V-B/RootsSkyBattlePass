package com.rootssky.battlepass.listeners;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.managers.DatabaseManager;
import com.rootssky.battlepass.models.PlayerProfile;
import com.rootssky.battlepass.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final BattlePassPlugin plugin;

    public PlayerListener(BattlePassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DatabaseManager db = plugin.getDatabaseManager();

        db.loadPlayer(player.getUniqueId()).thenAccept(profile -> {
            plugin.playerCache.put(player.getUniqueId(), profile);

            plugin.getServer().getRegionScheduler().execute(plugin, player.getLocation(), () -> {
                if (player.isOnline()) {
                    String msg = plugin.getConfigManager().getMessage("welcome")
                            .replace("%player%", player.getName())
                            .replace("%level%", String.valueOf(profile.getLevel()));
                    player.sendMessage(Utils.applyPrefix(msg));
                }
            });
        }).exceptionally(ex -> {
            Utils.log("<red>Falha ao carregar dados de " + player.getName() + ": " + ex.getMessage());
            return null;
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.playerCache.remove(player.getUniqueId());

        if (profile != null) {
            plugin.getDatabaseManager().savePlayer(profile).exceptionally(ex -> {
                Utils.log("<red>Falha ao salvar dados de " + player.getName() + ": " + ex.getMessage());
                return null;
            });
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (!plugin.playerCache.containsKey(player.getUniqueId())) {
            plugin.getDatabaseManager().loadPlayer(player.getUniqueId()).thenAccept(profile -> {
                plugin.playerCache.put(player.getUniqueId(), profile);
            }).exceptionally(ex -> {
                Utils.log("<red>Falha ao carregar dados (world change) de " + player.getName() + ": " + ex.getMessage());
                return null;
            });
        }
    }
}
