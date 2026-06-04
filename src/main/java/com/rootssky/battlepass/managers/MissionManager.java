package com.rootssky.battlepass.managers;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.models.MissionPeriod;
import com.rootssky.battlepass.models.MissionType;
import com.rootssky.battlepass.models.PlayerProfile;
import com.rootssky.battlepass.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MissionManager {

    private final BattlePassPlugin plugin;
    private final ConcurrentHashMap<UUID, Map<MissionType, AtomicInteger>> playerProgress = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> playerLocks = new ConcurrentHashMap<>();
    private final Map<String, MissionData> missions = new java.util.LinkedHashMap<>();
    private final Map<MissionPeriod, Long> lastReset = new ConcurrentHashMap<>();

    private int schedulerTaskId = -1;

    public record MissionData(
            String id,
            MissionType type,
            MissionPeriod period,
            int meta,
            int xp,
            long resetMillis,
            Material icon,
            String displayName,
            String description
    ) {}

    public MissionManager(BattlePassPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadMissions() {
        missions.clear();

        loadPeriodMissions("missions.daily", MissionPeriod.DAILY);
        loadPeriodMissions("missions.weekly", MissionPeriod.WEEKLY);
        loadPeriodMissions("missions.monthly", MissionPeriod.MONTHLY);

        long now = System.currentTimeMillis();
        for (MissionPeriod period : MissionPeriod.values()) {
            lastReset.putIfAbsent(period, now);
        }

        Map<String, Long> savedTimestamps = plugin.getDatabaseManager().loadResetTimestampsSync();
        for (Map.Entry<String, Long> entry : savedTimestamps.entrySet()) {
            try {
                MissionPeriod period = MissionPeriod.valueOf(entry.getKey());
                lastReset.put(period, entry.getValue());
            } catch (IllegalArgumentException ignored) {
            }
        }

        int daily = getMissionCountByPeriod(MissionPeriod.DAILY);
        int weekly = getMissionCountByPeriod(MissionPeriod.WEEKLY);
        int monthly = getMissionCountByPeriod(MissionPeriod.MONTHLY);
        Utils.log("<green>Carregadas " + missions.size() + " missões: " + daily + " diárias, " + weekly + " semanais, " + monthly + " mensais.");
    }

    private void loadPeriodMissions(String configPath, MissionPeriod period) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(configPath);
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection sec = section.getConfigurationSection(key);
            if (sec == null) continue;

            MissionType type = parseMissionType(sec.getString("type", "QUEBRAR_BLOCOS"), key);
            if (type == null) continue;

            int meta = sec.getInt("target", sec.getInt("meta", 100));
            int xp = sec.getInt("xp_reward", sec.getInt("xp", 50));
            long resetMillis = parseResetMillis(sec, period);
            Material icon = parseIcon(sec.getString("icon", "BARRIER"), key);
            String displayName = sec.getString("name", formatarNome(key));
            String description = sec.getString("description", "");

            missions.put(key, new MissionData(key, type, period, meta, xp, resetMillis, icon, displayName, description));
        }
    }

    private MissionType parseMissionType(String typeName, String key) {
        try {
            return MissionType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            Utils.log("<red>Tipo de missão inválido: " + typeName + " em " + key);
            return null;
        }
    }

    private long parseResetMillis(ConfigurationSection sec, MissionPeriod period) {
        int resetHours = sec.getInt("reset_hours", period.getDefaultHours());
        return (long) resetHours * 3600_000L;
    }

    private Material parseIcon(String iconName, String key) {
        try {
            return Material.valueOf(iconName);
        } catch (IllegalArgumentException e) {
            Utils.log("<red>Icone inválido para missão " + key + ": " + iconName);
            return Material.BARRIER;
        }
    }

    private Material defaultIcon(MissionType type) {
        return switch (type) {
            case MINERAR -> Material.IRON_PICKAXE;
            case QUEBRAR_BLOCOS -> Material.STONE_PICKAXE;
            case COLOCAR_BLOCOS -> Material.BRICKS;
            case MATAR_MOBS -> Material.IRON_SWORD;
            case PESCAR -> Material.FISHING_ROD;
            case COLHER -> Material.GOLDEN_HOE;
            case CRAFTAR -> Material.CRAFTING_TABLE;
            case COMER -> Material.COOKED_BEEF;
        };
    }

    public String formatarNome(String id) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : id.replace("-", "_").toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                capitalizeNext = true;
            } else {
                sb.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return sb.toString();
    }

    public void loadProgressFromProfile(PlayerProfile profile) {
        UUID uuid = profile.getUuid();
        Map<MissionType, AtomicInteger> progress = playerProgress.computeIfAbsent(uuid, k -> new EnumMap<>(MissionType.class));
        for (Map.Entry<MissionType, Integer> entry : profile.getMissionProgress().entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                progress.put(entry.getKey(), new AtomicInteger(entry.getValue()));
            }
        }

        if (profile.getLastDailyReset() > 0) {
            lastReset.put(MissionPeriod.DAILY, profile.getLastDailyReset());
        }
        if (profile.getLastWeeklyReset() > 0) {
            lastReset.put(MissionPeriod.WEEKLY, profile.getLastWeeklyReset());
        }
        if (profile.getLastMonthlyReset() > 0) {
            lastReset.put(MissionPeriod.MONTHLY, profile.getLastMonthlyReset());
        }
    }

    public void saveProgressToProfile(PlayerProfile profile) {
        UUID uuid = profile.getUuid();
        Map<MissionType, AtomicInteger> progress = playerProgress.get(uuid);
        if (progress != null) {
            profile.getMissionProgress().clear();
            for (Map.Entry<MissionType, AtomicInteger> entry : progress.entrySet()) {
                int val = entry.getValue().get();
                if (val > 0) {
                    profile.getMissionProgress().put(entry.getKey(), val);
                }
            }
        }
        profile.setLastDailyReset(lastReset.getOrDefault(MissionPeriod.DAILY, 0L));
        profile.setLastWeeklyReset(lastReset.getOrDefault(MissionPeriod.WEEKLY, 0L));
        profile.setLastMonthlyReset(lastReset.getOrDefault(MissionPeriod.MONTHLY, 0L));
    }

    public void saveAllProgressToProfiles() {
        for (Map.Entry<UUID, PlayerProfile> entry : plugin.playerCache.entrySet()) {
            saveProgressToProfile(entry.getValue());
        }
    }

    public void startResetScheduler() {
        if (schedulerTaskId != -1) return;

        long intervalTicks = 5 * 60 * 20L;

        schedulerTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndResetMissions,
                intervalTicks, intervalTicks).getTaskId();

        Utils.log("<green>Scheduler de reset de missões iniciado (5 min).");
    }

    public void stopResetScheduler() {
        if (schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(schedulerTaskId);
            schedulerTaskId = -1;
        }
    }

    public void checkAndResetMissions() {
        long now = System.currentTimeMillis();

        for (MissionPeriod period : MissionPeriod.values()) {
            Long last = lastReset.get(period);
            if (last == null) {
                lastReset.put(period, now);
                continue;
            }

            long elapsed = now - last;

            boolean shouldReset = switch (period) {
                case DAILY -> elapsed >= MissionPeriod.DAILY.getDefaultMillis();
                case WEEKLY -> elapsed >= MissionPeriod.WEEKLY.getDefaultMillis();
                case MONTHLY -> elapsed >= MissionPeriod.MONTHLY.getDefaultMillis();
            };

            if (shouldReset) {
                resetPeriod(period);
                lastReset.put(period, now);
                plugin.getDatabaseManager().saveResetTimestampSync(period.name(), now);
                Utils.log("<green>Missões " + period.getDisplayName() + "s resetadas automaticamente!");
            }
        }
    }

    public void resetPeriod(MissionPeriod period) {
        List<String> idsToClear = missions.values().stream()
                .filter(m -> m.period() == period)
                .map(MissionData::id)
                .toList();

        for (UUID uuid : playerProgress.keySet()) {
            Map<MissionType, AtomicInteger> progress = playerProgress.get(uuid);
            if (progress != null) {
                for (String id : idsToClear) {
                    MissionData mission = missions.get(id);
                    if (mission != null && progress.containsKey(mission.type())) {
                        progress.get(mission.type()).set(0);
                    }
                }
            }

            PlayerProfile profile = plugin.playerCache.get(uuid);
            if (profile != null) {
                for (String id : idsToClear) {
                    profile.getCompletedMissions().remove(id);
                }
                profile.setModified(true);
            }
        }
    }

    public void addProgress(UUID uuid, MissionType type, int amount) {
        PlayerProfile profile = plugin.playerCache.get(uuid);
        if (profile == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        Object lock = playerLocks.computeIfAbsent(uuid, k -> new Object());

        synchronized (lock) {
            Map<MissionType, AtomicInteger> progress = playerProgress.computeIfAbsent(uuid,
                    k -> new EnumMap<>(MissionType.class));

            AtomicInteger atomicProgress = progress.computeIfAbsent(type, k -> new AtomicInteger(0));
            int progressoAnterior = atomicProgress.get();
            int novoProgresso = atomicProgress.addAndGet(amount);

            MissionData missaoPrincipal = null;
            for (MissionData m : missions.values()) {
                if (m.type() != type) continue;
                if (profile.getCompletedMissions().contains(m.id())) continue;
                if (missaoPrincipal == null || m.meta() < missaoPrincipal.meta()) {
                    missaoPrincipal = m;
                }
            }

            for (MissionData mission : missions.values()) {
                if (mission.type() != type) continue;
                if (profile.getCompletedMissions().contains(mission.id())) continue;

                if (mission.equals(missaoPrincipal)) {
                    notificarMarcos(player, mission, progressoAnterior, novoProgresso);
                }

                if (novoProgresso >= mission.meta()) {
                    if (!profile.getCompletedMissions().add(mission.id())) continue;

                    notificarMissaoCompleta(player, mission);
                }
            }

            profile.setModified(true);
        }
    }

    public boolean claimMissionReward(Player player, String missionId) {
        PlayerProfile profile = plugin.playerCache.get(player.getUniqueId());
        if (profile == null) return false;

        MissionData mission = missions.get(missionId);
        if (mission == null) return false;

        if (!profile.getCompletedMissions().contains(missionId)) return false;

        String claimKey = "mission_" + missionId;
        if (profile.getClaimedRewards().contains(claimKey)) return false;

        boolean isVip = profile.isVip();
        double multiplicador = isVip ? plugin.getConfig().getDouble("settings.premium-xp-multiplier", 1.5) : 1.0;
        int xpBase = mission.xp();
        int xpFinal = (int) Math.round(xpBase * multiplicador);

        profile.getClaimedRewards().add(claimKey);
        profile.setModified(true);
        boolean subiu = profile.addXP(xpFinal);

        String xpDisplay;
        if (isVip && xpFinal > xpBase) {
            int bonus = xpFinal - xpBase;
            xpDisplay = "<white>" + xpBase + "</white><dark_gray>(</dark_gray><green>+" + bonus + "</green><dark_gray>)</dark_gray>";
        } else {
            xpDisplay = "<white>" + xpFinal + "</white>";
        }

        player.sendMessage(Utils.applyPrefix(
                plugin.getConfigManager().getMessage("mission-claimed")
                        .replace("%mission%", mission.displayName())
                        .replace("%xp%", xpFinal + "")
                        .replace("%xp_display%", xpDisplay)
                        .replace("%xp_bonus%", isVip && xpFinal > xpBase ? " <dark_gray>(</dark_gray><green>+" + (xpFinal - xpBase) + " VIP</green><dark_gray>)</dark_gray>" : "")
        ));

        if (subiu) {
            String levelUpMsg = plugin.getConfigManager().getMessage("level-up")
                    .replace("%level%", String.valueOf(profile.getLevel()));
            Utils.sendTitle(player, levelUpMsg, "", 10, 60, 10);
        }

        return true;
    }

    public int getProgress(UUID uuid, MissionType type) {
        Map<MissionType, AtomicInteger> progress = playerProgress.get(uuid);
        if (progress == null) return 0;
        AtomicInteger val = progress.get(type);
        return val != null ? val.get() : 0;
    }

    public int getProgress(UUID uuid, String missionId) {
        MissionData mission = missions.get(missionId);
        if (mission == null) return 0;
        return getProgress(uuid, mission.type());
    }

    public int getRemaining(UUID uuid, MissionType type, String missionId) {
        MissionData mission = missions.get(missionId);
        if (mission == null) return -1;
        int current = getProgress(uuid, type);
        return Math.max(0, mission.meta() - current);
    }

    public boolean isMissionCompleted(UUID uuid, String missionId) {
        PlayerProfile profile = plugin.playerCache.get(uuid);
        if (profile == null) return false;
        return profile.getCompletedMissions().contains(missionId);
    }

    public boolean isMissionClaimed(UUID uuid, String missionId) {
        PlayerProfile profile = plugin.playerCache.get(uuid);
        if (profile == null) return false;
        return profile.getClaimedRewards().contains("mission_" + missionId);
    }

    public String getResetTimeRemaining(MissionPeriod period) {
        Long last = lastReset.get(period);
        if (last == null) return "N/A";

        long elapsed = System.currentTimeMillis() - last;
        long remaining = period.getDefaultMillis() - elapsed;
        if (remaining <= 0) return "Em breve";

        long hours = remaining / 3600_000L;
        long minutes = (remaining % 3600_000L) / 60_000L;

        if (hours > 24) {
            long days = hours / 24;
            long restHours = hours % 24;
            return days + "d " + restHours + "h";
        }
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    public List<MissionData> getMissionsByPeriod(MissionPeriod period) {
        return missions.values().stream()
                .filter(m -> m.period() == period)
                .toList();
    }

    public int getMissionCountByPeriod(MissionPeriod period) {
        return (int) missions.values().stream()
                .filter(m -> m.period() == period)
                .count();
    }

    public int getCompletedCountByPeriod(UUID uuid, MissionPeriod period) {
        PlayerProfile profile = plugin.playerCache.get(uuid);
        if (profile == null) return 0;

        return (int) missions.values().stream()
                .filter(m -> m.period() == period)
                .filter(m -> profile.getCompletedMissions().contains(m.id()))
                .count();
    }

    public Map<MissionPeriod, Long> getLastReset() {
        return lastReset;
    }

    public Map<String, MissionData> getMissions() {
        return missions;
    }

    public int getTotalMissions() {
        return missions.size();
    }

    public List<MissionData> getMissionsPage(int page, int perPage) {
        List<MissionData> all = new ArrayList<>(missions.values());
        int start = page * perPage;
        int end = Math.min(start + perPage, all.size());
        if (start >= all.size()) return List.of();
        return all.subList(start, end);
    }

    public void resetDaily() {
        resetPeriod(MissionPeriod.DAILY);
        lastReset.put(MissionPeriod.DAILY, System.currentTimeMillis());
        Utils.log("<green>Missões diárias resetadas!");
    }

    public void removePlayer(UUID uuid) {
        playerProgress.remove(uuid);
        playerLocks.remove(uuid);
    }

    // ==================== NOTIFICACOES DE MARCO ====================
    private static final double MARCO_25 = 0.25;
    private static final double MARCO_50 = 0.50;
    private static final double MARCO_75 = 0.75;

    private void notificarMarcos(Player player, MissionData mission, int progressoAnterior, int progressoAtual) {
        int meta = mission.meta();
        if (meta <= 0) return;

        int marco25 = (int) Math.ceil(meta * MARCO_25);
        int marco50 = (int) Math.ceil(meta * MARCO_50);
        int marco75 = (int) Math.ceil(meta * MARCO_75);

        String periodo = switch (mission.period()) {
            case DAILY -> "<green>[Diaria]</green> ";
            case WEEKLY -> "<gold>[Semanal]</gold> ";
            case MONTHLY -> "<light_purple>[Mensal]</light_purple> ";
        };

        String msgFormat = periodo + "<gray>" + mission.displayName() + "</gray> <dark_gray>- </dark_gray><white>%progresso%/%meta%</white>";

        if (progressoAnterior < marco25 && progressoAtual >= marco25) {
            String msg = msgFormat.replace("%progresso%", String.valueOf(marco25)).replace("%meta%", String.valueOf(meta));
            Utils.sendActionBar(player, msg);
        }
        if (progressoAnterior < marco50 && progressoAtual >= marco50) {
            String msg = msgFormat.replace("%progresso%", String.valueOf(marco50)).replace("%meta%", String.valueOf(meta));
            Utils.sendActionBar(player, msg);
        }
        if (progressoAnterior < marco75 && progressoAtual >= marco75) {
            String msg = msgFormat.replace("%progresso%", String.valueOf(marco75)).replace("%meta%", String.valueOf(meta));
            Utils.sendActionBar(player, msg);
        }
    }

    private void notificarMissaoCompleta(Player player, MissionData mission) {
        String periodoTag = switch (mission.period()) {
            case DAILY -> "<green>[Diaria]</green>";
            case WEEKLY -> "<gold>[Semanal]</gold>";
            case MONTHLY -> "<light_purple>[Mensal]</light_purple>";
        };

        String titulo = "<green>✅ Missao Concluida!</green>";
        String subtitulo = periodoTag + " <white>" + mission.displayName() + "</white>";

        Utils.sendTitle(player, titulo, subtitulo, 5, 50, 10);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

        player.sendMessage(Utils.color(
                "<green>✦ </green>" + periodoTag + " <green>Missao <bold>" + mission.displayName() + "</bold> concluida!</green>"
        ));
        player.sendMessage(Utils.color(
                "<gray>Abra o menu de missoes para resgatar sua recompensa.</gray>"
        ));
    }
}
