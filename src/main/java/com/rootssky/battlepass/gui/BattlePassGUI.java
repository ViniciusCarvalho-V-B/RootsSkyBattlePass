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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BattlePassGUI implements InventoryHolder {

    private static final int TAMANHO = 54;
    private static final int ITENS_POR_PAGINA = 8;

    // Layout das linhas (cada linha tem 9 slots)
    // Linha 0 (0-8): Cabeçalho
    // Linha 1 (9-16): Recompensas Free + Label (slot 17)
    // Linha 2 (18-26): Barra de Progresso
    // Linha 3 (27-35): Separador VIP
    // Linha 4 (36-43): Recompensas VIP
    // Linha 5 (45-53): Navegação/Rodapé

    // Slots específicos
    private static final int SLOT_CABECALHO = 4;
    private static final int SLOT_LABEL_FREE = 17;
    private static final int SLOT_RESGATAR_TUDO = 27;
    private static final int SLOT_LIVRO_MISSOES = 31;
    private static final int SLOT_LABEL_VIP = 44;
    private static final int SLOT_ANTERIOR = 45;
    private static final int SLOT_PAGINA_ATUAL = 49;
    private static final int SLOT_PROXIMA = 53;

    // Constantes CMD
    private static final int CMD_FREE = 1000;
    private static final int CMD_VIP = 2000;
    private static final int CMD_BOTAO = 3000;

    private static final Map<UUID, Long> claimAllCooldowns = new ConcurrentHashMap<>();

    private final BattlePassPlugin plugin;
    private final Player player;
    private final PlayerProfile profile;
    private final Inventory inventory;
    private final int pagina;
    private final int totalPaginas;
    private final int maxNivel;
    private final long claimAllCooldownMs;

    private BattlePassGUI(BattlePassPlugin plugin, Player player, int pagina) {
        this.plugin = plugin;
        this.player = player;
        this.profile = plugin.playerCache.get(player.getUniqueId());
        this.maxNivel = plugin.getConfigManager().getMaxLevel();
        this.totalPaginas = (int) Math.ceil(maxNivel / (double) ITENS_POR_PAGINA);
        this.pagina = Math.max(0, Math.min(pagina, totalPaginas - 1));
        this.claimAllCooldownMs = plugin.getConfigManager().getResgatarTudoCooldownMs();
        String titulo = plugin.getConfigManager().getGuiTitleBattlePass();
        this.inventory = Bukkit.createInventory(this, TAMANHO, Utils.color(titulo));
    }

    public static void abrir(BattlePassPlugin plugin, Player player) {
        abrir(plugin, player, 0);
    }

    public static void abrir(BattlePassPlugin plugin, Player player, int pagina) {
        try {
            BattlePassGUI gui = new BattlePassGUI(plugin, player, pagina);
            gui.construir();
            player.openInventory(gui.getInventory());
        } catch (Exception e) {
            player.sendMessage(Utils.applyPrefix("<red>Erro ao abrir o Passe de Batalha."));
            Utils.log("<red>Erro ao abrir BattlePassGUI: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void construir() {
        preencherCabecalho();
        preencherRecompensasFree();
        preencherLabelFree();
        preencherBarraProgresso();
        preencherSeparadorVIP();
        preencherRecompensasVIP();
        preencherLabelVIP();
        preencherNavegacao();
    }

    // ==================== LINHA 0: CABEÇALHO ====================
    private void preencherCabecalho() {
        ConfigManager cfg = plugin.getConfigManager();
        for (int slot = 0; slot <= 8; slot++) {
            if (slot == SLOT_CABECALHO) continue;
            inventory.setItem(slot, new ItemBuilder(cfg.getMaterialCabecalhoBg())
                    .nome(Component.empty())
                    .esconderAtributos()
                    .build());
        }

        if (profile == null) return;

        boolean isVip = player.hasPermission("rootssky.passe.vip");
        Component vipTag = isVip
                ? Utils.color("<bold><gradient:#ffaa00:#ffff55>★ VIP</gradient>")
                : Utils.color("<dark_gray>Gratuito");

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.color("<gray>Jogador: <white>" + player.getName() + "</white></gray>"));
        lore.add(Utils.color("<gray>Nível: <gold>" + profile.getLevel() + "</gold> / <gray>" + maxNivel + "</gray></gray>"));
        lore.add(Utils.color("<gray>XP: <green>" + profile.getXp() + "</green> / <gold>" + profile.calculateNextLevelXP() + "</gold></gray>"));
        lore.add(Utils.color("<gray>Status: </gray>").append(vipTag));

        ItemStack cabecalho = new ItemBuilder(cfg.getMaterialCabecalhoPlayer())
                .nome(Utils.color("<bold><gradient:#55ff55:#00aa00>PASSE DE BATALHA</gradient></bold>"))
                .lore(lore)
                .esconderAtributos()
                .build();

        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) cabecalho.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            cabecalho.setItemMeta(skullMeta);
        }

        inventory.setItem(SLOT_CABECALHO, cabecalho);
    }

    // ==================== LINHA 1: RECOMPENSAS FREE + LABEL ====================
    private void preencherLabelFree() {
        ConfigManager cfg = plugin.getConfigManager();
        inventory.setItem(SLOT_LABEL_FREE, new ItemBuilder(cfg.getMaterialLabelFree())
                .nome(Utils.color("<green><bold>PASSE GRATUITO</bold></green>"))
                .customModelData(CMD_FREE)
                .esconderAtributos()
                .build());
    }

    private void preencherRecompensasFree() {
        if (profile == null) return;

        int inicio = pagina * ITENS_POR_PAGINA + 1;
        int fim = Math.min(inicio + ITENS_POR_PAGINA - 1, maxNivel);
        int slotBase = 9; // Começa no slot 9

        for (int nivel = inicio; nivel <= fim; nivel++) {
            int slot = slotBase + (nivel - inicio);
            if (slot > 16) break;

            ItemStack item = criarItemFree(nivel);
            inventory.setItem(slot, item);
        }
    }

    private ItemStack criarItemFree(int nivel) {
        boolean atingiu = profile.getLevel() >= nivel;
        boolean jaResgatou = !plugin.getRewardManager().canClaim(player, nivel, false);
        ConfigManager cfg = plugin.getConfigManager();

        String tipoPath = "recompensas.gratis";
        String nivelKey = "nivel-" + nivel;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(tipoPath + "." + nivelKey);

        String rewardName = getNomeRecompensa(sec);
        Material rewardMat = cfg.getMaterialRecompensaFree();

        if (!atingiu) {
            List<Component> lore = new ArrayList<>();
            lore.add(Utils.color("<red>🔒 Bloqueado - Alcançe o nível " + nivel + "</red>"));

            return new ItemBuilder(rewardMat)
                    .nome(Utils.color("<dark_gray>Nível " + nivel + "</dark_gray>"))
                    .lore(lore)
                    .esconderAtributos()
                    .build();
        }

        if (jaResgatou) {
            List<Component> lore = new ArrayList<>();
            lore.add(Utils.color("<dark_green>✔ Resgatado</dark_green>"));
            lore.add(Utils.color("<gray>Recompensa: " + rewardName + "</gray>"));

            return new ItemBuilder(rewardMat)
                    .nome(Utils.color("<gray>Nível " + nivel + "</gray>"))
                    .lore(lore)
                    .esconderAtributos()
                    .build();
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.color("<green>✅ Disponível para resgate!</green>"));
        lore.add(Utils.color("<gray>Recompensa: " + rewardName + "</gray>"));
        lore.add(Utils.color("<gray>Clique para resgatar</gray>"));

        return new ItemBuilder(rewardMat)
                .nome(Utils.color("<gold><bold>Nível " + nivel + "</bold></gold>"))
                .lore(lore)
                .addGlow()
                .customModelData(CMD_FREE)
                .esconderAtributos()
                .build();
    }

    // ==================== LINHA 2: BARRA DE PROGRESSO + BOTÕES ====================
    private void preencherBarraProgresso() {
        if (profile == null) return;
        ConfigManager cfg = plugin.getConfigManager();

        double percentual = Math.min(1.0, profile.getXp() / (double) profile.calculateNextLevelXP());
        int filledSlots = (int) (percentual * 9);

        for (int i = 0; i < 9; i++) {
            int slot = 18 + i;
            boolean isFilled = i < filledSlots;

            if (i == 4) {
                ItemStack item = isFilled
                        ? new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                                .nome(Utils.color("<green>Progresso: " + profile.getXp() + "/" + profile.calculateNextLevelXP() + " XP"))
                                .build()
                        : new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                                .nome(Utils.color("<gray>Progresso: " + profile.getXp() + "/" + profile.calculateNextLevelXP() + " XP"))
                                .build();
                inventory.setItem(slot, item);
            } else {
                ItemStack item = isFilled
                        ? new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                                .nome(Component.empty())
                                .esconderAtributos()
                                .build()
                        : new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                                .nome(Component.empty())
                                .esconderAtributos()
                                .build();
                inventory.setItem(slot, item);
            }
        }
    }

    // ==================== LINHA 3: SEPARADOR VIP ====================
    private void preencherSeparadorVIP() {
        ConfigManager cfg = plugin.getConfigManager();
        for (int slot = 27; slot <= 35; slot++) {
            if (slot == SLOT_RESGATAR_TUDO || slot == SLOT_LIVRO_MISSOES) continue;
            inventory.setItem(slot, new ItemBuilder(cfg.getMaterialCabecalhoBg())
                    .nome(Component.empty())
                    .esconderAtributos()
                    .build());
        }

        int disponiveis = contarRecompensasDisponiveis();
        boolean temDisponiveis = disponiveis > 0;

        List<Component> loreResgatar = new ArrayList<>();
        if (temDisponiveis) {
            loreResgatar.add(Utils.color("<green>" + disponiveis + " recompensa(s) disponivel(is)"));
            loreResgatar.add(Utils.color("<gray>Clique para resgatar todos os niveis atingidos</gray>"));
        } else {
            loreResgatar.add(Utils.color("<gray>Nenhuma recompensa disponivel</gray>"));
        }

        ItemBuilder builderResgatar = new ItemBuilder(cfg.getMaterialResgatarTudo())
                .nome(Utils.color("<green><bold>Resgatar Todas Disponiveis</bold></green>"))
                .lore(loreResgatar)
                .customModelData(CMD_BOTAO)
                .esconderAtributos();

        if (temDisponiveis) {
            builderResgatar.brilho();
        }

        inventory.setItem(SLOT_RESGATAR_TUDO, builderResgatar.build());

        MissionManager mm = plugin.getMissionManager();
        int dailyCount = mm.getMissionCountByPeriod(MissionPeriod.DAILY);
        int weeklyCount = mm.getMissionCountByPeriod(MissionPeriod.WEEKLY);
        int monthlyCount = mm.getMissionCountByPeriod(MissionPeriod.MONTHLY);

        if (dailyCount == 0) {
            var daySec = plugin.getConfig().getConfigurationSection("missions.daily");
            if (daySec != null) dailyCount = daySec.getKeys(false).size();
        }
        if (weeklyCount == 0) {
            var weekSec = plugin.getConfig().getConfigurationSection("missions.weekly");
            if (weekSec != null) weeklyCount = weekSec.getKeys(false).size();
        }
        if (monthlyCount == 0) {
            var monSec = plugin.getConfig().getConfigurationSection("missions.monthly");
            if (monSec != null) monthlyCount = monSec.getKeys(false).size();
        }

        List<Component> loreLivro = new ArrayList<>();
        loreLivro.add(Utils.color("<gray>Diárias: <yellow>" + dailyCount + " </yellow><dark_gray>missões"));
        loreLivro.add(Utils.color("<gray>Semanais: <yellow>" + weeklyCount + " </yellow><dark_gray>missões"));
        loreLivro.add(Utils.color("<gray>Mensais: <yellow>" + monthlyCount + " </yellow><dark_gray>missões"));
        loreLivro.add(Component.empty());
        loreLivro.add(Utils.color("<green>Clique para abrir as missões"));

        inventory.setItem(SLOT_LIVRO_MISSOES, new ItemBuilder(cfg.getMaterialLivroMissoes())
                .nome(Utils.color("<yellow><bold>Missões do Passe</bold></yellow>"))
                .lore(loreLivro)
                .addGlow()
                .customModelData(CMD_BOTAO)
                .esconderAtributos()
                .build());
    }

    // ==================== LINHA 4: RECOMPENSAS VIP ====================
    private void preencherRecompensasVIP() {
        if (profile == null) return;

        boolean isVip = player.hasPermission("rootssky.passe.vip");
        int inicio = pagina * ITENS_POR_PAGINA + 1;
        int fim = Math.min(inicio + ITENS_POR_PAGINA - 1, maxNivel);
        int slotBase = 36; // Começa no slot 36

        for (int nivel = inicio; nivel <= fim; nivel++) {
            int slot = slotBase + (nivel - inicio);
            if (slot > 43) break;

            ItemStack item = criarItemVIP(nivel, isVip);
            inventory.setItem(slot, item);
        }
    }

    private ItemStack criarItemVIP(int nivel, boolean isVip) {
        boolean atingiu = profile.getLevel() >= nivel;
        boolean jaResgatou = !plugin.getRewardManager().canClaim(player, nivel, true);
        ConfigManager cfg = plugin.getConfigManager();

        String tipoPath = "recompensas.vip";
        String nivelKey = "nivel-" + nivel;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(tipoPath + "." + nivelKey);

        String rewardName = getNomeRecompensa(sec);
        Material rewardMat = cfg.getMaterialRecompensaVip();

        if (!atingiu || !isVip) {
            List<Component> lore = new ArrayList<>();
            if (!isVip) {
                lore.add(Utils.color("<red>🔒 Exclusivo para VIPs</red>"));
            } else {
                lore.add(Utils.color("<red>🔒 Alcançe o nível " + nivel + "</red>"));
            }

            return new ItemBuilder(rewardMat)
                    .nome(Utils.color("<dark_gray>Nível " + nivel + " (VIP)</dark_gray>"))
                    .lore(lore)
                    .esconderAtributos()
                    .build();
        }

        if (jaResgatou) {
            List<Component> lore = new ArrayList<>();
            lore.add(Utils.color("<dark_green>✔ Resgatado</dark_green>"));
            lore.add(Utils.color("<gray>Recompensa: " + rewardName + "</gray>"));

            return new ItemBuilder(rewardMat)
                    .nome(Utils.color("<gray>Nível " + nivel + " (VIP)</gray>"))
                    .lore(lore)
                    .esconderAtributos()
                    .build();
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.color("<gold>✨ Recompensa VIP disponivel!</gold>"));
        lore.add(Utils.color("<gray>Recompensa: " + rewardName + "</gray>"));
        lore.add(Utils.color("<gold>Clique para resgatar VIP</gold>"));

        return new ItemBuilder(rewardMat)
                .nome(Utils.color("<yellow><bold>Nível " + nivel + " (VIP)</bold></yellow>"))
                .lore(lore)
                .addGoldGlow()
                .customModelData(CMD_VIP)
                .esconderAtributos()
                .build();
    }

    private void preencherLabelVIP() {
        ConfigManager cfg = plugin.getConfigManager();
        inventory.setItem(SLOT_LABEL_VIP, new ItemBuilder(cfg.getMaterialLabelVip())
                .nome(Utils.color("<gold><bold>PASSE VIP</bold></gold>"))
                .customModelData(CMD_VIP)
                .esconderAtributos()
                .build());
    }

    // ==================== LINHA 5: NAVEGAÇÃO ====================
    private void preencherNavegacao() {
        ConfigManager cfg = plugin.getConfigManager();
        Material arrowMat = cfg.getMaterialNavigationArrow();
        Material pageMat = cfg.getMaterialPageIndicator();
        Material glassMat = Material.GRAY_STAINED_GLASS_PANE;

        if (pagina > 0) {
            inventory.setItem(SLOT_ANTERIOR, new ItemBuilder(arrowMat)
                    .nome(Utils.color("<gray>← Página Anterior</gray>"))
                    .customModelData(CMD_BOTAO)
                    .esconderAtributos()
                    .build());
        } else {
            inventory.setItem(SLOT_ANTERIOR, new ItemBuilder(glassMat)
                    .nome(Component.empty())
                    .esconderAtributos()
                    .build());
        }

        int inicioNivel = pagina * ITENS_POR_PAGINA + 1;
        int fimNivel = Math.min(inicioNivel + ITENS_POR_PAGINA - 1, maxNivel);

        inventory.setItem(SLOT_PAGINA_ATUAL, new ItemBuilder(pageMat)
                .nome(Utils.color("<gold>Página " + (pagina + 1) + " de " + totalPaginas + "</gold>"))
                .lore(List.of(Utils.color("<gray>Mostrando niveis " + inicioNivel + " - " + fimNivel + "</gray>")))
                .customModelData(CMD_BOTAO)
                .esconderAtributos()
                .build());

        if (pagina < totalPaginas - 1) {
            inventory.setItem(SLOT_PROXIMA, new ItemBuilder(arrowMat)
                    .nome(Utils.color("<gray>Próxima Página →</gray>"))
                    .customModelData(CMD_BOTAO)
                    .esconderAtributos()
                    .build());
        } else {
            inventory.setItem(SLOT_PROXIMA, new ItemBuilder(glassMat)
                    .nome(Component.empty())
                    .esconderAtributos()
                    .build());
        }

        int[] emptyNavSlots = {46, 47, 48, 50, 51, 52};
        for (int slot : emptyNavSlots) {
            inventory.setItem(slot, new ItemBuilder(glassMat)
                    .nome(Component.empty())
                    .esconderAtributos()
                    .build());
        }
    }

    // ==================== UTILITÁRIOS ====================
    private String getNomeRecompensa(ConfigurationSection sec) {
        if (sec == null) return "Nenhuma";

        double dinheiro = sec.getDouble("dinheiro", 0);
        if (dinheiro > 0) {
            return "<gold>$" + (int) dinheiro + "</gold>";
        }

        List<String> itens = sec.getStringList("itens");
        if (!itens.isEmpty()) {
            String[] partes = itens.get(0).split(":");
            return "<white>" + partes[0] + " x" + (partes.length > 1 ? partes[1] : "1") + "</white>";
        }

        List<String> comandos = sec.getStringList("comandos");
        if (!comandos.isEmpty()) {
            return "<yellow>" + comandos.size() + " comando(s)" + "</yellow>";
        }

        return "Nenhuma";
    }

    private int contarRecompensasDisponiveis() {
        if (profile == null) return 0;

        int count = 0;
        int currentLevel = profile.getLevel();
        boolean isVip = player.hasPermission("rootssky.passe.vip");

        for (int nivel = 1; nivel <= currentLevel; nivel++) {
            if (plugin.getRewardManager().canClaim(player, nivel, false)) {
                count++;
            }
            if (isVip && plugin.getRewardManager().canClaim(player, nivel, true)) {
                count++;
            }
        }

        return count;
    }

    private void resgatarTodas(Player player, int paginaAtual) {
        if (profile == null) return;

        UUID uuid = player.getUniqueId();
        Long ultimo = claimAllCooldowns.get(uuid);
        if (ultimo != null && (System.currentTimeMillis() - ultimo) < claimAllCooldownMs) {
            player.sendMessage(Utils.applyPrefix("<red>Aguarde antes de usar esta função novamente."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }
        claimAllCooldowns.put(uuid, System.currentTimeMillis());

        int resgatadas = 0;
        int currentLevel = profile.getLevel();
        boolean isVip = player.hasPermission("rootssky.passe.vip");

        for (int nivel = 1; nivel <= currentLevel; nivel++) {
            // Bypassa o cooldown interno do RewardManager para cada resgate em lote
            plugin.getRewardManager().limparCooldown(uuid);
            if (plugin.getRewardManager().canClaim(player, nivel, false)) {
                plugin.getRewardManager().claimReward(player, nivel, false);
                resgatadas++;
            }
            plugin.getRewardManager().limparCooldown(uuid);
            if (isVip && plugin.getRewardManager().canClaim(player, nivel, true)) {
                plugin.getRewardManager().claimReward(player, nivel, true);
                resgatadas++;
            }
        }

        if (resgatadas > 0) {
            player.sendMessage(Utils.applyPrefix("<green>✅ " + resgatadas + " recompensa(s) resgatada(s) com sucesso!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> abrir(plugin, player, paginaAtual), 2L);
        } else {
            player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("reward-already")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
        }
    }

    // ==================== HANDLE CLICKS ====================
    public static void aoClicar(BattlePassPlugin plugin, Player player, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (!(holder instanceof BattlePassGUI gui)) return;

        int paginaAtual = gui.pagina;
        int maxNivel = gui.maxNivel;

        try {
            // Navegação
            if (slot == SLOT_PROXIMA && paginaAtual < gui.totalPaginas - 1) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
                abrir(plugin, player, paginaAtual + 1);
                return;
            }

            if (slot == SLOT_ANTERIOR && paginaAtual > 0) {
                abrir(plugin, player, Math.max(0, paginaAtual - 1));
                return;
            }

            // Resgatar Tudo
            if (slot == SLOT_RESGATAR_TUDO) {
                gui.resgatarTodas(player, paginaAtual);
                return;
            }

            // Abrir Missões (clique no LIVRO no slot 31)
            if (slot == SLOT_LIVRO_MISSOES) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                MissionGUI.abrir(plugin, player);
                return;
            }

            // Banner Free (decoração)
            if (slot == SLOT_LABEL_FREE) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                return;
            }

            // Recompensas Free (slots 9-16)
            if (slot >= 9 && slot <= 16) {
                int index = slot - 9;
                int nivel = paginaAtual * ITENS_POR_PAGINA + index + 1;
                if (nivel > maxNivel) return;
                processarCliqueNivel(plugin, player, nivel, false, paginaAtual);
                return;
            }

            // Recompensas VIP (slots 36-43)
            if (slot >= 36 && slot <= 43) {
                int index = slot - 36;
                int nivel = paginaAtual * ITENS_POR_PAGINA + index + 1;
                if (nivel > maxNivel) return;

                boolean isVip = player.hasPermission("rootssky.passe.vip");
                if (!isVip) {
                    player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("vip-required")));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                    return;
                }

                processarCliqueNivel(plugin, player, nivel, true, paginaAtual);
                return;
            }

        } catch (Exception e) {
            Utils.log("<red>Erro ao processar clique no BattlePassGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processarCliqueNivel(BattlePassPlugin plugin, Player player, int nivel, boolean vip, int paginaAtual) {
        PlayerProfile profile = plugin.playerCache.get(player.getUniqueId());
        if (profile == null) return;

        if (!plugin.getRewardManager().canClaim(player, nivel, vip)) {
            player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("reward-already")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }

        if (profile.getLevel() < nivel) {
            player.sendMessage(Utils.applyPrefix(plugin.getConfigManager().getMessage("reward-locked")
                    .replace("%nivel%", String.valueOf(nivel))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }

        plugin.getRewardManager().claimReward(player, nivel, vip);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> abrir(plugin, player, paginaAtual), 2L);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
