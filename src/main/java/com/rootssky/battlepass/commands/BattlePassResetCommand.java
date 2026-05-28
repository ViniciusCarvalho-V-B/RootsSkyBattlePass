package com.rootssky.battlepass.commands;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.models.PlayerProfile;
import com.rootssky.battlepass.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BattlePassResetCommand implements CommandExecutor {
    private final BattlePassPlugin plugin;

    public BattlePassResetCommand(BattlePassPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("rootssky.passe.reset")) {
            sender.sendMessage(Utils.applyPrefix("<red>Você não tem permissão para usar este comando."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Utils.applyPrefix("<red>Uso: /bpreset <jogador>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Utils.applyPrefix("<red>Jogador não encontrado ou não está online."));
            return true;
        }

        PlayerProfile profile = plugin.playerCache.get(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(Utils.applyPrefix("<red>Dados do jogador não encontrados."));
            return true;
        }

        // Close player's inventory to prevent ghost items
        target.closeInventory();

        // Reset player progress using the safe method
        profile.resetProgress();

        // Save to database asynchronously
        plugin.getDatabaseManager().savePlayer(profile).thenRun(() -> {
            sender.sendMessage(Utils.applyPrefix("<green>Progresso do jogador " + target.getName() + " resetado com sucesso!"));
            target.sendMessage(Utils.applyPrefix("<green>Seu progresso no Battle Pass foi resetado!"));
        }).exceptionally(throwable -> {
            sender.sendMessage(Utils.applyPrefix("<red>Erro ao salvar o progresso resetado."));
            return null;
        });

        return true;
    }
}