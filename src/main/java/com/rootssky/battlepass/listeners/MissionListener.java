package com.rootssky.battlepass.listeners;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.models.MissionType;
import com.rootssky.battlepass.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.Set;

public class MissionListener implements Listener {

    private final BattlePassPlugin plugin;
    private final boolean ss2Habilitado;

    private static final Set<Material> MINERIOS = Set.of(
            Material.STONE,
            Material.COBBLESTONE,
            Material.ANDESITE,
            Material.GRANITE,
            Material.DIORITE,
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_GOLD_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS
    );

    public MissionListener(BattlePassPlugin plugin) {
        this.plugin = plugin;
        this.ss2Habilitado = Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2");
        if (!ss2Habilitado) {
            Utils.log("<yellow>SuperiorSkyblock2 nao detectado! Missoes sem verificacao de ilha.");
        }
    }

    /**
     * Verifica se o jogador possui uma ilha ativa no SuperiorSkyblock2.
     * Retorna true se SS2 nao esta habilitado (permite progresso offline).
     */
    private boolean verificarIlha(Player player) {
        if (!ss2Habilitado) return true;

        try {
            var ss2Player = com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI.getPlayer(player);
            if (ss2Player == null) return false;
            var ilha = ss2Player.getIsland();
            if (ilha == null) return false;
            return ilha.isMember(ss2Player);
        } catch (NoClassDefFoundError | Exception e) {
            // Fallback para testes locais: permite progresso mesmo sem SS2
            plugin.getLogger().warning("SS2 API falhou para " + player.getName() + ", usando fallback. Erro: " + e.getMessage());
            return true;
        }
    }

    /**
     * Processa o progresso de uma missão para o jogador, aplicando verificacao de ilha.
     */
    private void processarMissao(Player player, MissionType tipo, int quantidade) {
        try {
            if (!verificarIlha(player)) return;

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getMissionManager().addProgress(player.getUniqueId(), tipo, quantidade);
                } catch (Exception e) {
                    Utils.log("<red>Erro ao adicionar progresso da missão para " + player.getName() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Utils.log("<red>Erro ao processar missão para " + player.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrarBloco(BlockBreakEvent event) {
        try {
            Player player = event.getPlayer();
            Material material = event.getBlock().getType();

            MissionType tipo;
            if (MINERIOS.contains(material)) {
                tipo = MissionType.MINERAR;
            } else if (CROPS.contains(material)) {
                tipo = MissionType.COLHER;
            } else {
                tipo = MissionType.QUEBRAR_BLOCOS;
            }

            processarMissao(player, tipo, 1);
        } catch (Exception e) {
            Utils.log("<red>Erro no BlockBreakEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocarBloco(BlockPlaceEvent event) {
        try {
            processarMissao(event.getPlayer(), MissionType.COLOCAR_BLOCOS, 1);
        } catch (Exception e) {
            Utils.log("<red>Erro no BlockPlaceEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoMatarMob(EntityDeathEvent event) {
        try {
            LivingEntity entity = event.getEntity();
            if (!(entity instanceof Monster)) return;

            Player player = entity.getKiller();
            if (player == null) return;

            processarMissao(player, MissionType.MATAR_MOBS, 1);
        } catch (Exception e) {
            Utils.log("<red>Erro no EntityDeathEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPescar(PlayerFishEvent event) {
        try {
            if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

            processarMissao(event.getPlayer(), MissionType.PESCAR, 1);
        } catch (Exception e) {
            Utils.log("<red>Erro no PlayerFishEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCraftar(CraftItemEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player player)) return;

            int quantidade = event.getRecipe().getResult().getAmount();
            if (event.isShiftClick()) {
                quantidade = Math.max(1, quantidade);
            }

            processarMissao(player, MissionType.CRAFTAR, quantidade);
        } catch (Exception e) {
            Utils.log("<red>Erro no CraftItemEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoComer(PlayerItemConsumeEvent event) {
        try {
            processarMissao(event.getPlayer(), MissionType.COMER, 1);
        } catch (Exception e) {
            Utils.log("<red>Erro no PlayerItemConsumeEvent: " + e.getMessage());
        }
    }
}
