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
    private final RankResolver rankResolver;
    private final RankDisplay rankDisplay;

    public RankNameTagManager(PlayerLevelManager playerLevelManager) {
        this.playerLevelManager = Objects.requireNonNull(playerLevelManager, "playerLevelManager");
        this.rankResolver = new RankResolver();
        this.rankDisplay = new RankDisplay();
    }

    public void update(Player player) {
        Objects.requireNonNull(player, "player");
        RankProfile profile = rankResolver.resolve(player::hasPermission);
        int level = playerLevelManager.getLevel(player.getUniqueId());
        Component prefix = rankDisplay.fullPrefix(level, profile);

        Team team = getOrCreateTeam(player.getUniqueId());
        removeFromOtherLrgTeams(player.getName(), team);
        team.prefix(prefix);
        team.color(rankDisplay.nameColor(profile));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.addEntry(player.getName());

        Component tabName = prefix.append(Component.text(player.getName(), rankDisplay.nameColor(profile)));
        player.playerListName(tabName);
    }

    public void remove(Player player) {
        Objects.requireNonNull(player, "player");
        Scoreboard scoreboard = mainScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null && team.getName().startsWith(TEAM_PREFIX)) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
        player.playerListName(null);
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    private Team getOrCreateTeam(UUID uuid) {
        Scoreboard scoreboard = mainScoreboard();
        String teamName = TEAM_PREFIX + uuid.toString().replace("-", "").substring(0, 12);
        Team team = scoreboard.getTeam(teamName);
        return team != null ? team : scoreboard.registerNewTeam(teamName);
    }

    private void removeFromOtherLrgTeams(String playerName, Team targetTeam) {
        Scoreboard scoreboard = mainScoreboard();
        Team current = scoreboard.getEntryTeam(playerName);
        if (current != null && current != targetTeam && current.getName().startsWith(TEAM_PREFIX)) {
            current.removeEntry(playerName);
            if (current.getEntries().isEmpty()) {
                current.unregister();
            }
        }
    }

    private Scoreboard mainScoreboard() {
        return Objects.requireNonNull(Bukkit.getScoreboardManager(), "scoreboardManager").getMainScoreboard();
    }
}
