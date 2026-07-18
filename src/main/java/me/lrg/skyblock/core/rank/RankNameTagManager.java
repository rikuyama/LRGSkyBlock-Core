package me.lrg.skyblock.core.rank;

import me.lrg.skyblock.core.playerlevel.manager.PlayerLevelManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Objects;
import java.util.UUID;

public final class RankNameTagManager {
    private static final String TEAM_PREFIX = "lrg_";

    private final PlayerLevelManager playerLevelManager;
    private final RankResolver rankResolver = new RankResolver();
    private final RankDisplay rankDisplay = new RankDisplay();
    private int animationPhase;
    private int refreshCounter;

    public RankNameTagManager(PlayerLevelManager playerLevelManager) {
        this.playerLevelManager = Objects.requireNonNull(playerLevelManager, "playerLevelManager");
    }

    public void update(Player player) {
        Objects.requireNonNull(player, "player");
        RankProfile profile = rankResolver.resolve(player::hasPermission);
        int level = playerLevelManager.getLevel(player.getUniqueId());
        Component prefix = rankDisplay.fullPrefix(level, profile, animationPhase);

        Team team = getOrCreateTeam(player.getUniqueId());
        // エントリーを先に維持してから表示だけ更新することで、虹色更新中の一瞬の消失を防ぐ。
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.prefix(prefix);
        team.color(rankDisplay.nameColor(profile));
        removeFromOtherLrgTeams(player.getName(), team);

        player.playerListName(prefix.append(Component.text(player.getName(), rankDisplay.nameColor(profile))));
    }

    public void remove(Player player) {
        Objects.requireNonNull(player, "player");
        Team team = mainScoreboard().getEntryTeam(player.getName());
        if (team != null && team.getName().startsWith(TEAM_PREFIX)) team.removeEntry(player.getName());
        player.playerListName(null);
    }

    public void tick() {
        animationPhase = Math.floorMod(animationPhase + 1, 7);
        refreshCounter++;
        boolean fullRefresh = refreshCounter >= 20;
        if (fullRefresh) refreshCounter = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = playerLevelManager.getLevel(player.getUniqueId());
            if (fullRefresh || rankDisplay.isRainbowLevel(level)) update(player);
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) update(player);
    }

    private Team getOrCreateTeam(UUID uuid) {
        Scoreboard scoreboard = mainScoreboard();
        String teamName = TEAM_PREFIX + uuid.toString().replace("-", "").substring(0, 12);
        Team team = scoreboard.getTeam(teamName);
        return team != null ? team : scoreboard.registerNewTeam(teamName);
    }

    private void removeFromOtherLrgTeams(String playerName, Team targetTeam) {
        Team current = mainScoreboard().getEntryTeam(playerName);
        if (current != null && current != targetTeam && current.getName().startsWith(TEAM_PREFIX)) {
            current.removeEntry(playerName);
        }
    }

    private Scoreboard mainScoreboard() {
        return Objects.requireNonNull(Bukkit.getScoreboardManager(), "scoreboardManager").getMainScoreboard();
    }
}
