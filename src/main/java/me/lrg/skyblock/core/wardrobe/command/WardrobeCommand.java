package me.lrg.skyblock.core.wardrobe.command;

import me.lrg.skyblock.core.playerlevel.unlock.PlayerLevelUnlockManager;
import me.lrg.skyblock.core.wardrobe.gui.WardrobeGui;
import me.lrg.skyblock.core.wardrobe.manager.WardrobeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WardrobeCommand implements CommandExecutor {
    private final PlayerLevelUnlockManager unlockManager;
    private final WardrobeManager wardrobeManager;
    private final WardrobeGui wardrobeGui;

    public WardrobeCommand(PlayerLevelUnlockManager unlockManager,
                           WardrobeManager wardrobeManager,
                           WardrobeGui wardrobeGui) {
        this.unlockManager = unlockManager;
        this.wardrobeManager = wardrobeManager;
        this.wardrobeGui = wardrobeGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤー専用です。");
            return true;
        }
        if (!unlockManager.isUnlocked(player.getUniqueId(), PlayerLevelUnlockManager.WARDROBE)) {
            player.sendMessage(ChatColor.RED + "WardrobeはPlayer Level 5で解放されます。");
            return true;
        }
        if (!wardrobeManager.isLoaded(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Wardrobeデータを読み込み中です。少し待ってから開いてください。");
            return true;
        }
        wardrobeGui.open(player);
        return true;
    }
}
