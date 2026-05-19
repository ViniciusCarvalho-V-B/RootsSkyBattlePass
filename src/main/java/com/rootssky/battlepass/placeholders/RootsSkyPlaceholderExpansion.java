package com.rootssky.battlepass.placeholders;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.models.PlayerProfile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class RootsSkyPlaceholderExpansion extends PlaceholderExpansion {

    private final BattlePassPlugin plugin;

    public RootsSkyPlaceholderExpansion(BattlePassPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rootssky_passe";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) return "";

        PlayerProfile profile = plugin.playerCache.get(offlinePlayer.getUniqueId());
        if (profile == null) return "0";

        return switch (params.toLowerCase()) {
            case "nivel", "level" -> String.valueOf(profile.getLevel());
            case "xp" -> String.valueOf(profile.getXp());
            case "xp_requerido", "xp_necessario", "required" -> String.valueOf(profile.calculateNextLevelXP());
            case "vip" -> offlinePlayer.isOnline() && offlinePlayer.getPlayer().hasPermission("rootssky.passe.vip") ? "VIP" : "Gratuito";
            case "missoes_completas" -> String.valueOf(profile.getCompletedMissions().size());
            case "recompensas_resgatadas" -> String.valueOf(profile.getClaimedRewards().size());
            default -> null;
        };
    }
}
