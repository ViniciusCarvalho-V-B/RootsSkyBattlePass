package com.rootssky.battlepass.managers;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.models.PlayerProfile;
import com.rootssky.battlepass.utils.Utils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RewardManager {

    private final BattlePassPlugin plugin;
    private Economy economy;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1000L;

    public RewardManager(BattlePassPlugin plugin) {
        this.plugin = plugin;
    }

    public void setupEconomy() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                Utils.log("<green>Vault Economy conectada!");
                return;
            }
        }
        economy = null;
        Utils.log("<red>Vault Economy não encontrada! Recompensas de dinheiro desativadas.");
    }

    private boolean temEconomia() {
        return economy != null && Bukkit.getServicesManager().getRegistration(Economy.class) != null;
    }

    private boolean temCooldown(UUID uuid) {
        Long ultimo = cooldowns.get(uuid);
        if (ultimo == null) return false;
        return (System.currentTimeMillis() - ultimo) < COOLDOWN_MS;
    }

    private void aplicarCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    public boolean canClaim(Player player, int nivel) {
        return canClaim(player, nivel, false);
    }

    public boolean canClaim(Player player, int nivel, boolean vip) {
        PlayerProfile profile = plugin.playerCache.get(player.getUniqueId());
        if (profile == null) return false;
        if (profile.getLevel() < nivel) return false;
        String claimKey = vip ? "vip-" + nivel : String.valueOf(nivel);
        return !profile.getClaimedRewards().contains(claimKey);
    }

    public void claimReward(Player player, int nivel) {
        claimReward(player, nivel, player.hasPermission("rootssky.passe.vip"));
    }

    public void claimReward(Player player, int nivel, boolean vip) {
        UUID uuid = player.getUniqueId();

        if (temCooldown(uuid)) {
            player.sendMessage(Utils.applyPrefix("<red>Aguarde um momento antes de resgatar novamente."));
            return;
        }

        PlayerProfile profile = plugin.playerCache.get(uuid);
        if (profile == null) return;

        synchronized (profile) {
            if (profile.getLevel() < nivel) {
                player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("reward-locked")
                        .replace("%nivel%", String.valueOf(nivel))));
                return;
            }

            String claimKey = vip ? "vip-" + nivel : String.valueOf(nivel);
            if (profile.getClaimedRewards().contains(claimKey)) {
                player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("reward-already")));
                return;
            }

            if (!vip && profile.getClaimedRewards().contains(String.valueOf(nivel))) {
                player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("reward-already")));
                return;
            }

            String tipoPath = vip ? "recompensas.vip" : "recompensas.gratis";
            String nivelKey = "nivel-" + nivel;

            ConfigurationSection recompensaSec = plugin.getConfig().getConfigurationSection(tipoPath + "." + nivelKey);
            if (recompensaSec == null && !vip) {
                recompensaSec = plugin.getConfig().getConfigurationSection("recompensas.gratis." + nivelKey);
            }
            if (recompensaSec == null) {
                player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("no-reward")
                    .replace("%nivel%", String.valueOf(nivel))));
                return;
            }

            profile.getClaimedRewards().add(claimKey);
            aplicarCooldown(uuid);

            try {
                plugin.getDatabaseManager().savePlayer(profile).join();
            } catch (Exception ex) {
                Utils.log("<red>Erro ao salvar progresso de " + player.getName() + " após resgatar: " + ex.getMessage());
            }

            darRecompensas(player, nivel, recompensaSec);

            player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("reward-claimed")
                    .replace("%nivel%", String.valueOf(nivel))));
        }
    }

    private void darRecompensas(Player player, int nivel, ConfigurationSection sec) {
        double dinheiro = sec.getDouble("dinheiro", 0);
        if (dinheiro > 0) {
            if (temEconomia()) {
                economy.depositPlayer(player, dinheiro);
                player.sendMessage(Utils.applyPrefix("<green>+$" + (int) dinheiro + " depositado na sua conta!"));
            } else {
                Utils.log("<yellow>Economia indisponível. Pulando recompensa de $" + (int) dinheiro + " para " + player.getName());
            }
        }

        List<String> itens = sec.getStringList("itens");
        for (String itemStr : itens) {
            String[] partes = itemStr.split(":");
            try {
                Material material = Material.valueOf(partes[0]);
                int qtd = partes.length > 1 ? Integer.parseInt(partes[1]) : 1;
                player.getInventory().addItem(new ItemStack(material, qtd));
            } catch (IllegalArgumentException e) {
                Utils.log("<red>Item inválido no config: " + itemStr);
            }
        }

        List<String> comandos = sec.getStringList("comandos");
        for (String comando : comandos) {
            String cmd = comando
                    .replace("%player%", player.getName())
                    .replace("%nivel%", String.valueOf(nivel));
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        }
    }

    public void limparCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
