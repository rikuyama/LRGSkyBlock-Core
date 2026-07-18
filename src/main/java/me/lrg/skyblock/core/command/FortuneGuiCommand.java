package me.lrg.skyblock.core.command;

import me.lrg.skyblock.core.gui.FortuneGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FortuneGuiCommand implements CommandExecutor {
    private final FortuneGui gui;

    public FortuneGuiCommand(FortuneGui gui) {
        this.gui = Objects.requireNonNull(gui, "gui");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはゲーム内で実行してください。");
            return true;
        }
        if (!player.hasPermission("lrgskyblock.command.fortunegui")) {
            player.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return true;
        }
        gui.open(player);
        return true;
    }
}
