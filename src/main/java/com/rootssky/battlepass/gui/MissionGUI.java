package com.rootssky.battlepass.gui;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.managers.ConfigManager;
import com.rootssky.battlepass.managers.MissionManager;
import com.rootssky.battlepass.models.MissionPeriod;
import com.rootssky.battlepass.models.PlayerProfile;
import com.rootssky.battlepass.utils.ItemBuilder;
import com.rootssky.battlepass.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MissionGUI implements InventoryHolder {

    private static final int TAMANHO = 54;

    // Linha 1: Slots 11-16 -> Missoes Diarias (slot 10 = indicador)
    private static final int[] SLOTS_DAILY = {11, 12, 13, 14, 15, 16};

    // Linha 2: Slots 20-25 -> Missoes Semanais (slot 19 = indicador)
    private static final int[] SLOTS_WEEKLY = {20, 21, 22, 23, 24, 25};

    // Linha 3: Slots 29-34 -> Missoes Mensais (slot 28 = indicador)
    private static final int[] SLOTS_MONTHLY = {29, 30, 31, 32, 33, 34};

    // Rodape
    private static final int SLOT_FECHAR = 49;
    private static final int SLOT_VOLTAR = 45;

    private final BattlePassPlugin plugin;
    private final Player player;
    private final PlayerProfile profile;
    private final Inventory inventory;

    private MissionGUI(BattlePassPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.profile = plugin.playerCache.get(player.getUniqueId());
        String titulo = plugin.getConfigManager().getGuiTitleMissions();
        this.inventory = Bukkit.createInventory(this, TAMANHO, Utils.color(titulo));
    }

    public static void abrir(BattlePassPlugin plugin, Player player) {
        try {
            MissionGUI gui = new MissionGUI(plugin, player);
            gui.construir();
            player.openInventory(gui.getInventory());
        } catch (Exception e) {
            player.sendMessage(Utils.applyPrefix("<red>Erro ao abrir as Missoes."));
            Utils.log("<red>Erro ao abrir MissionGUI: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void construir() {
        preencherFundo();
        if (profile == null) return;

        colocarHeader();
        colocarLinhasMissoes();
        colocarRodape();
    }

    private void preencherFundo() {
        ConfigManager cfg = plugin.getConfigManager();
        for (int slot = 0; slot < TAMANHO; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, new ItemBuilder(cfg.getMaterialFill())
                        .nome(Component.empty())
                        .esconderAtributos()
                        .build());
            }
        }
    }

    private void colocarHeader() {
        MissionManager mm = plugin.getMissionManager();
        int total = mm.getTotalMissions();
        int completas = 0;
        for (MissionPeriod p : MissionPeriod.values()) {
            completas += mm.getCompletedCountByPeriod(player.getUniqueId(), p);
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Utils.color("<gray>Total: <white>" + completas + "<dark_gray>/<white>" + total + " <gray>completas"));
        lore.add(Component.empty());
        lore.add(Utils.color("<gray>Linha 1: <white>Missoes Diarias"));
        lore.add(Utils.color("<gray>Linha 2: <gold>Missoes Semanais"));
        lore.add(Utils.color("<gray>Linha 3: <blue>Missoes Mensais"));
        lore.add(Component.empty());
        lore.add(Utils.color("<green>Clique para resgatar missoes prontas"));

        inventory.setItem(4, new ItemBuilder(Material.BOOK)
                .nome(Utils.color("<bold><gradient:#55ff55:#00aa00>Missoes do Passe</gradient>"))
                .lore(lore)
                .esconderAtributos()
                .brilho()
                .build());
    }

    private void colocarLinhasMissoes() {
        MissionManager mm = plugin.getMissionManager();
        ConfigManager cfg = plugin.getConfigManager();

        colocarIndicadorLinha(cfg.getMissionIndicatorDaily(), 10,
                Utils.color("<green><bold>MISSOES DIARIAS</bold></green>"),
                Utils.color("<yellow>Reseta a cada 24h</yellow>"));

        colocarIndicadorLinha(cfg.getMissionIndicatorWeekly(), 19,
                Utils.color("<gold><bold>MISSOES SEMANAIS</bold></gold>"),
                Utils.color("<yellow>Reseta a cada 7 dias</yellow>"));

        colocarIndicadorLinha(cfg.getMissionIndicatorMonthly(), 28,
                Utils.color("<light_purple><bold>MISSOES MENSAIS</bold></light_purple>"),
                Utils.color("<yellow>Reseta a cada 30 dias</yellow>"));

        preencherLinha(mm, MissionPeriod.DAILY, SLOTS_DAILY, cfg.getMissionIndicatorDaily(),
                Utils.color("<gray><bold>Missões Diárias</bold></gray>"),
                Utils.color("<yellow>Reseta a cada 24h</yellow>"));

        preencherLinha(mm, MissionPeriod.WEEKLY, SLOTS_WEEKLY, cfg.getMissionIndicatorWeekly(),
                Utils.color("<gold><bold>Missões Semanais</bold></gold>"),
                Utils.color("<yellow>Reseta a cada 7 dias</yellow>"));

        preencherLinha(mm, MissionPeriod.MONTHLY, SLOTS_MONTHLY, cfg.getMissionIndicatorMonthly(),
                Utils.color("<blue><bold>Missões Mensais</bold></blue>"),
                Utils.color("<yellow>Reseta a cada 30 dias</yellow>"));
    }

    private void colocarIndicadorLinha(Material material, int slot, Component nome, Component lore) {
        inventory.setItem(slot, new ItemBuilder(material)
                .nome(nome)
                .lore(List.of(Component.empty(), lore))
                .brilho()
                .esconderAtributos()
                .build());
    }

    private void preencherLinha(MissionManager mm, MissionPeriod period, int[] slots,
                                   Material periodIcon, Component labelNome, Component labelLore) {
        java.util.UUID uuid = player.getUniqueId();
        List<MissionManager.MissionData> missoes = mm.getMissionsByPeriod(period);
        ConfigManager cfg = plugin.getConfigManager();

        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            if (i < missoes.size()) {
                inventory.setItem(slot, criarItemMissao(missoes.get(i)));
            } else {
                inventory.setItem(slot, new ItemBuilder(cfg.getMaterialFill())
                        .nome(Component.empty())
                        .esconderAtributos()
                        .build());
            }
        }
    }

    private ItemStack criarItemMissao(MissionManager.MissionData missao) {
        MissionManager mm = plugin.getMissionManager();
        java.util.UUID uuid = player.getUniqueId();
        MissionPeriod period = missao.period();

        boolean completa = mm.isMissionCompleted(uuid, missao.id());
        boolean resgatada = mm.isMissionClaimed(uuid, missao.id());

        Material icon = missao.icon();
        if (icon == null || icon == Material.BARRIER || icon == Material.AIR) {
            icon = Material.PAPER;
        }

        String cor = period.getColorTag();
        String periodoLabel = period.getDisplayName();
        boolean isVip = player.hasPermission("rootssky.passe.vip");
        double vipMult = plugin.getConfig().getDouble("settings.premium-xp-multiplier", 1.5);
        int xpBase = missao.xp();
        int xpVip = (int) Math.round(xpBase * vipMult);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (!missao.description().isEmpty()) {
            lore.add(Utils.color("<gray>" + missao.description()));
            lore.add(Component.empty());
        }

        lore.add(Utils.color("<dark_gray>Periodo: " + cor + periodoLabel));

        String xpLore;
        if (isVip && xpVip > xpBase) {
            xpLore = "<dark_gray>Recompensa: <green>+" + xpBase + " XP <dark_gray>(<green>+" + (xpVip - xpBase) + " VIP</green><dark_gray>)</dark_gray>";
        } else {
            xpLore = "<dark_gray>Recompensa: <green>+" + xpBase + " XP";
        }

        if (resgatada) {
            lore.add(Utils.color(xpLore + " <dark_gray>(resgatada)"));
            lore.add(Component.empty());
            lore.add(Utils.color("<dark_gray>Concluida e resgatada"));

            return new ItemBuilder(icon)
                    .nome(Utils.color("<dark_gray>" + missao.displayName()))
                    .lore(lore)
                    .esconderAtributos()
                    .build();
        }

        if (completa) {
            lore.add(Utils.color(xpLore));
            lore.add(Component.empty());
            lore.add(Utils.color("<bold><green>Clique para Resgatar!</green></bold>"));

            return new ItemBuilder(icon)
                    .nome(Utils.color(cor + missao.displayName()))
                    .lore(lore)
                    .brilho()
                    .esconderAtributos()
                    .build();
        }

        int progresso = mm.getProgress(uuid, missao.id());
        int objetivo = missao.meta();
        double porcentagem = Math.min(1.0, (double) progresso / Math.max(1, objetivo));
        String barra = criarBarraProgresso(porcentagem, progresso, objetivo);

        lore.add(Utils.color("<gray>Progresso: <white>" + progresso + "/" + objetivo + "</white></gray>"));
        lore.add(Utils.color("<gray>" + barra + "</gray>"));
        lore.add(Utils.color("<green>+" + xpBase + " XP Passe" + (isVip && xpVip > xpBase ? " <dark_gray>(<green>+" + (xpVip - xpBase) + " VIP</green><dark_gray>)</dark_gray>" : "") + "</green>"));
        lore.add(Component.empty());
        lore.add(Utils.color("<dark_gray>Em andamento</dark_gray>"));

        return new ItemBuilder(icon)
                .nome(Utils.color("<yellow>" + missao.displayName() + "</yellow>"))
                .lore(lore)
                .esconderAtributos()
                .build();
    }

    private String criarBarraProgresso(double porcentagem, int atual, int maximo) {
        ConfigManager cfg = plugin.getConfigManager();
        int tamanho = cfg.getProgressBarLength();
        String charPreenchido = cfg.getProgressBarCharFilled();
        String charVazio = cfg.getProgressBarCharEmpty();

        int preenchido = (int) (porcentagem * tamanho);
        int vazio = tamanho - preenchido;

        StringBuilder sb = new StringBuilder();
        sb.append("<green>");
        sb.append(String.valueOf(charPreenchido).repeat(preenchido));
        sb.append("<dark_gray>");
        sb.append(String.valueOf(charVazio).repeat(vazio));
        sb.append(" <white>").append(atual).append("<dark_gray>/<white>").append(maximo);
        return sb.toString();
    }

    private void colocarRodape() {
        ConfigManager cfg = plugin.getConfigManager();
        inventory.setItem(SLOT_VOLTAR, new ItemBuilder(cfg.getMissionBackButton())
                .nome(Utils.color("<gray>← Voltar ao Passe</gray>"))
                .esconderAtributos()
                .build());

        inventory.setItem(SLOT_FECHAR, new ItemBuilder(cfg.getMissionCloseButton())
                .nome(Utils.color("<red>Fechar</red>"))
                .esconderAtributos()
                .build());
    }

    public static void aoClicar(BattlePassPlugin plugin, Player player, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (!(holder instanceof MissionGUI gui)) return;

        if (slot == SLOT_VOLTAR) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> BattlePassGUI.abrir(plugin, player), 2L);
            return;
        }

        if (slot == SLOT_FECHAR && item.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        MissionManager mm = plugin.getMissionManager();
        java.util.UUID uuid = player.getUniqueId();

        // Processa clique em qualquer slot de missao
        MissionManager.MissionData missao = procurarMissaoNoSlot(mm, slot);
        if (missao == null) return;

        if (mm.isMissionCompleted(uuid, missao.id()) && !mm.isMissionClaimed(uuid, missao.id())) {
            if (mm.claimMissionReward(player, missao.id())) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> abrir(plugin, player), 2L);
                return;
            }
        } else if (!mm.isMissionCompleted(uuid, missao.id())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        }
    }

    private static MissionManager.MissionData procurarMissaoNoSlot(MissionManager mm, int slot) {
        MissionPeriod period = null;
        int index = -1;

        for (int i = 0; i < SLOTS_DAILY.length; i++) {
            if (SLOTS_DAILY[i] == slot) { period = MissionPeriod.DAILY; index = i; break; }
        }
        if (period == null) {
            for (int i = 0; i < SLOTS_WEEKLY.length; i++) {
                if (SLOTS_WEEKLY[i] == slot) { period = MissionPeriod.WEEKLY; index = i; break; }
            }
        }
        if (period == null) {
            for (int i = 0; i < SLOTS_MONTHLY.length; i++) {
                if (SLOTS_MONTHLY[i] == slot) { period = MissionPeriod.MONTHLY; index = i; break; }
            }
        }

        if (period == null || index < 0) return null;

        List<MissionManager.MissionData> missoes = mm.getMissionsByPeriod(period);
        if (index >= missoes.size()) return null;

        return missoes.get(index);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}