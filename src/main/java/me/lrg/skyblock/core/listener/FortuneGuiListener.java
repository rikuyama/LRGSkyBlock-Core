package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.config.FortuneTargetSettings;
import me.lrg.skyblock.core.gui.FortuneGui;
import me.lrg.skyblock.core.model.FortuneCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class FortuneGuiListener implements Listener {
    private final FortuneGui gui;
    private final FortuneTargetSettings settings;

    public FortuneGuiListener(FortuneGui gui, FortuneTargetSettings settings) {
        this.gui = Objects.requireNonNull(gui, "gui");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(FortuneGui.TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        FortuneCategory category = gui.categoryAt(slot);
        if (category != null) {
            if (event.isRightClick()) {
                Material held = heldBlock(player);
                if (held == null) {
                    player.sendMessage(Component.text("ブロックをメインハンドに持ってください。", NamedTextColor.RED));
                } else {
                    settings.setCategoryOverride(held, category);
                    player.sendMessage(Component.text(held.name() + " を " + category.name() + " に登録しました。", NamedTextColor.GREEN));
                }
            } else {
                boolean enabled = !settings.isCategoryEnabled(category);
                settings.setCategoryEnabled(category, enabled);
                player.sendMessage(Component.text(category.name() + " を " + (enabled ? "有効" : "無効") + "にしました。", NamedTextColor.YELLOW));
            }
            gui.open(player);
            return;
        }

        if (slot == 26) {
            Material held = heldBlock(player);
            if (held == null) {
                player.sendMessage(Component.text("ブロックをメインハンドに持ってください。", NamedTextColor.RED));
            } else {
                settings.removeOverride(held);
                player.sendMessage(Component.text(held.name() + " の例外設定を削除しました。", NamedTextColor.GREEN));
            }
            gui.open(player);
        }
    }

    private Material heldBlock(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR || !item.getType().isBlock()) return null;
        return item.getType();
    }
}
