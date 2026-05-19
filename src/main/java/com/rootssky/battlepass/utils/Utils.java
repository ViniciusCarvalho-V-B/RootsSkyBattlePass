package com.rootssky.battlepass.utils;

import com.rootssky.battlepass.BattlePassPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class Utils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Utils() {
    }

    public static Component color(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    public static Component applyPrefix(String message) {
        String prefix = BattlePassPlugin.getInstance().getConfigManager().getPrefix();
        return color(prefix + message);
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(color(message));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );

        Title titleObj = Title.title(
                color(title),
                color(subtitle),
                times
        );

        player.showTitle(titleObj);
    }

    public static void log(String message) {
        Bukkit.getLogger().info("[RootsSkyBattlePass] " + message);
    }
}
