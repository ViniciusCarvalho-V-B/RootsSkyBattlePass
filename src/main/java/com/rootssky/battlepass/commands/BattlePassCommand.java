package com.rootssky.battlepass.commands;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.gui.BattlePassGUI;
import com.rootssky.battlepass.models.PlayerProfile;
import com.rootssky.battlepass.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BattlePassCommand implements CommandExecutor, TabCompleter {

    private final BattlePassPlugin plugin;

    public BattlePassCommand(BattlePassPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("passeadmin")) {
            return executarAdmin(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Utils.color("<red>Apenas jogadores podem usar este comando."));
            return true;
        }

        if (!player.hasPermission("rootssky.passe.use") && !player.hasPermission("battlepass.use")) {
            player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("perfil")) {
            enviarPerfil(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("missoes")) {
            com.rootssky.battlepass.gui.MissionGUI.abrir(plugin, player);
            return true;
        }

        BattlePassGUI.abrir(plugin, player);
        return true;
    }

    private void enviarPerfil(Player player) {
        PlayerProfile profile = plugin.playerCache.get(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(Utils.applyPrefix("<red>Dados não carregados. Reconecte-se."));
            return;
        }

        String msg = plugin.getConfigManager().getMessage("profile")
                .replace("%player%", player.getName())
                .replace("%level%", String.valueOf(profile.getLevel()))
                .replace("%xp%", String.valueOf(profile.getXp()))
                .replace("%required%", String.valueOf(profile.calculateNextLevelXP()));

        player.sendMessage(Utils.applyPrefix(msg));
    }

    private boolean executarAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rootssky.passe.admin") && !sender.hasPermission("battlepass.admin")) {
            sender.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            enviarAjudaAdmin(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> executarReload(sender);
            case "reset" -> executarReset(sender, args);
            case "givevip" -> executarGiveVip(sender, args);
            case "setlevel" -> executarSetLevel(sender, args);
            case "addxp" -> executarAddXp(sender, args);
            default -> enviarAjudaAdmin(sender);
        }

        return true;
    }

    private void executarReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        plugin.getMissionManager().loadMissions();
        plugin.getRewardManager().setupEconomy();

        String formula = plugin.getConfigManager().getXpFormula();
        for (PlayerProfile profile : plugin.playerCache.values()) {
            profile.setXpFormula(formula);
        }

        sender.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("reloaded")));
        Utils.log("<green>Configurações recarregadas por " + sender.getName());
    }

    private void executarReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Utils.applyPrefix("<red>Uso: /passeadmin reset <jogador>"));
            return;
        }

        Player alvo = Bukkit.getPlayer(args[1]);
        if (alvo == null) {
            sender.sendMessage(Utils.applyPrefix("<red>Jogador não encontrado."));
            return;
        }

        PlayerProfile profile = plugin.playerCache.get(alvo.getUniqueId());
        if (profile == null) {
            sender.sendMessage(Utils.applyPrefix("<red>Dados do jogador não estão carregados."));
            return;
        }

        // 1. Zerar dados do perfil em memória
        profile.setLevel(1);
        profile.setXp(0);
        profile.setPremium(false);
        profile.getClaimedRewards().clear();
        profile.getCompletedMissions().clear();
        plugin.getMissionManager().removePlayer(alvo.getUniqueId());
        plugin.getRewardManager().limparCooldown(alvo.getUniqueId());

        // 2. Salvar no banco de dados (bloqueante)
        try {
            plugin.getDatabaseManager().savePlayer(profile).join();
        } catch (Exception e) {
            sender.sendMessage(Utils.applyPrefix("<red>Erro ao salvar no banco de dados."));
            return;
        }

        // 3. Remover do cache (força reload limpo)
        plugin.playerCache.remove(alvo.getUniqueId());

        // 4. Recarregar perfil "zerado" do banco
        PlayerProfile freshProfile;
        try {
            freshProfile = plugin.getDatabaseManager().loadPlayer(alvo.getUniqueId()).join();
        } catch (Exception e) {
            sender.sendMessage(Utils.applyPrefix("<red>Erro ao recarregar perfil do banco."));
            return;
        }

        // 5. Colocar perfil limpo no cache
        plugin.playerCache.put(alvo.getUniqueId(), freshProfile);

        sender.sendMessage(Utils.applyPrefix("<green>Passe de " + alvo.getName() + " completamente resetado!"));
        alvo.sendMessage(Utils.color("<green>Seu passe foi completamente resetado!"));
        Utils.log("<yellow>Passe de " + alvo.getName() + " resetado por " + sender.getName());

        // 6. Reabrir a GUI com o perfil zerado
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (alvo.isOnline()) {
                BattlePassGUI.abrir(plugin, alvo, 0);
            }
        }, 3L);
    }

    private void executarGiveVip(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Utils.applyPrefix("<red>Uso: /passeadmin givevip <jogador>"));
            return;
        }

        Player alvo = Bukkit.getPlayer(args[1]);
        if (alvo == null) {
            sender.sendMessage(Utils.applyPrefix("<red>Jogador não encontrado."));
            return;
        }

        if (alvo.hasPermission("rootssky.passe.vip")) {
            sender.sendMessage(Utils.applyPrefix("<yellow>" + alvo.getName() + " já possui VIP."));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + alvo.getName() + " permission set rootssky.passe.vip true");
        });

        sender.sendMessage(Utils.applyPrefix("<green>VIP concedido a " + alvo.getName() + "!"));
        alvo.sendMessage(Utils.applyPrefix("<gold>✓ Você recebeu o status <bold>VIP</bold><gold> no Passe de Batalha!"));
    }

    private void executarSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Utils.applyPrefix("<red>Uso: /passeadmin setlevel <jogador> <nível>"));
            return;
        }

        Player alvo = Bukkit.getPlayer(args[1]);
        if (alvo == null) {
            sender.sendMessage(Utils.applyPrefix("<red>Jogador não encontrado."));
            return;
        }

        int nivel;
        try {
            nivel = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.applyPrefix("<red>Nível inválido."));
            return;
        }

        PlayerProfile profile = plugin.playerCache.get(alvo.getUniqueId());
        if (profile == null) {
            sender.sendMessage(Utils.applyPrefix("<red>Dados do jogador não estão carregados."));
            return;
        }

        profile.setLevel(nivel);
        profile.setXp(0);

        plugin.getDatabaseManager().savePlayer(profile).thenRun(() -> {
            sender.sendMessage(Utils.applyPrefix("<green>Nível de " + alvo.getName() + " definido para " + nivel + "!"));
        });
    }

    private void executarAddXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Utils.applyPrefix("<red>Uso: /passeadmin addxp <jogador> <quantidade>"));
            return;
        }

        Player alvo = Bukkit.getPlayer(args[1]);
        if (alvo == null) {
            sender.sendMessage(Utils.applyPrefix("<red>Jogador não encontrado."));
            return;
        }

        int quantidade;
        try {
            quantidade = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.applyPrefix("<red>Quantidade inválida."));
            return;
        }

        PlayerProfile profile = plugin.playerCache.get(alvo.getUniqueId());
        if (profile == null) {
            sender.sendMessage(Utils.applyPrefix("<red>Dados do jogador não estão carregados."));
            return;
        }

        boolean subiu = profile.addXP(quantidade);

        String xpMsg = plugin.getConfigManager().getMessage("xp-gained").replace("%xp%", String.valueOf(quantidade));
        Utils.sendActionBar(alvo, xpMsg);

        if (subiu) {
            String levelUpMsg = plugin.getConfigManager().getMessage("level-up").replace("%level%", String.valueOf(profile.getLevel()));
            Utils.sendTitle(alvo, levelUpMsg, "", 10, 60, 10);
        }

        plugin.getDatabaseManager().savePlayer(profile).thenRun(() -> {
            sender.sendMessage(Utils.applyPrefix("<green>+" + quantidade + " XP adicionado a " + alvo.getName() + "!"));
        });
    }

    private void enviarAjudaAdmin(CommandSender sender) {
        sender.sendMessage(Utils.applyPrefix("<gold>⚙ Comandos Administrativos:"));
        sender.sendMessage(Utils.applyPrefix("<gray>/passeadmin reload <dark_gray>- Recarregar configurações"));
        sender.sendMessage(Utils.applyPrefix("<gray>/passeadmin reset <jogador> <dark_gray>- Resetar progresso"));
        sender.sendMessage(Utils.applyPrefix("<gray>/passeadmin givevip <jogador> <dark_gray>- Dar VIP (LuckPerms)"));
        sender.sendMessage(Utils.applyPrefix("<gray>/passeadmin setlevel <jogador> <nível> <dark_gray>- Definir nível"));
        sender.sendMessage(Utils.applyPrefix("<gray>/passeadmin addxp <jogador> <xp> <dark_gray>- Adicionar XP"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("passeadmin")) {
            if (!sender.hasPermission("rootssky.passe.admin") && !sender.hasPermission("battlepass.admin")) {
                return completions;
            }

            if (args.length == 1) {
                completions.add("reload");
                completions.add("reset");
                completions.add("givevip");
                completions.add("setlevel");
                completions.add("addxp");
            } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else {
            if (args.length == 1) {
                completions.add("perfil");
                completions.add("missoes");
            }
        }

        return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
    }
}
