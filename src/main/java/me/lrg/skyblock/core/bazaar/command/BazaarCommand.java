package me.lrg.skyblock.core.bazaar.command;

import me.lrg.skyblock.core.bazaar.config.BazaarMessages;
import me.lrg.skyblock.core.bazaar.gui.BazaarGui;
import me.lrg.skyblock.core.playerlevel.unlock.PlayerLevelUnlockManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BazaarCommand implements CommandExecutor {
    private final BazaarGui gui;
    private final PlayerLevelUnlockManager unlockManager;
    private final BazaarMessages messages;

    public BazaarCommand(BazaarGui gui, PlayerLevelUnlockManager unlockManager, BazaarMessages messages) {
        this.gui = gui;
        this.unlockManager = unlockManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.text("bazaar.messages.player-only"));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!player.hasPermission("lrgskyblock.command.bazaar.admin")) {
                player.sendMessage(messages.text("bazaar.messages.no-permission"));
                return true;
            }
            gui.openAdmin(player);
            return true;
        }
        if (!unlockManager.isUnlocked(player.getUniqueId(), PlayerLevelUnlockManager.BAZAAR)) {
            player.sendMessage(messages.text("bazaar.messages.locked"));
            return true;
        }
        gui.openPlayer(player);
        return true;
    }
}
