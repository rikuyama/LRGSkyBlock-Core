package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.FortuneManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Objects;

public class ForagingFortuneListener implements Listener {

    private final FortuneManager fortuneManager;

    public ForagingFortuneListener(FortuneManager fortuneManager) {
        this.fortuneManager = Objects.requireNonNull(fortuneManager, "fortuneManager");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (!isForagingFortuneTarget(blockType)) {
            return;
        }

        double fortune = fortuneManager.getForagingFortune(player, blockType);

        ItemStack originalTool = player.getInventory().getItemInMainHand();

        if (fortune <= 0.0 && !hasFortune(originalTool)) {
            return;
        }

        ItemStack toolWithoutFortune = createToolWithoutFortune(originalTool);
        Collection<ItemStack> originalDrops = block.getDrops(toolWithoutFortune, player);

        if (originalDrops.isEmpty()) {
            return;
        }

        event.setDropItems(false);

        World world = block.getWorld();

        for (ItemStack originalDrop : originalDrops) {
            ItemStack finalDrop = originalDrop.clone();

            if (fortune > 0.0 && shouldApplyFortune(blockType, finalDrop.getType())) {
                int finalAmount = fortuneManager.calculateDropAmount(finalDrop.getAmount(), fortune);
                finalDrop.setAmount(finalAmount);
            }

            world.dropItemNaturally(block.getLocation(), finalDrop);
        }
    }

    private ItemStack createToolWithoutFortune(ItemStack originalTool) {
        if (originalTool == null || originalTool.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }

        ItemStack tool = originalTool.clone();
        tool.removeEnchantment(Enchantment.FORTUNE);

        return tool;
    }

    private boolean hasFortune(ItemStack tool) {
        return tool != null
                && tool.getType() != Material.AIR
                && tool.getEnchantmentLevel(Enchantment.FORTUNE) > 0;
    }

    private boolean isForagingFortuneTarget(Material material) {
        return switch (material) {
            case OAK_LOG,
                 BIRCH_LOG,
                 SPRUCE_LOG,
                 JUNGLE_LOG,
                 ACACIA_LOG,
                 DARK_OAK_LOG,
                 MANGROVE_LOG,
                 CHERRY_LOG,
                 CRIMSON_STEM,
                 WARPED_STEM,

                 STRIPPED_OAK_LOG,
                 STRIPPED_BIRCH_LOG,
                 STRIPPED_SPRUCE_LOG,
                 STRIPPED_JUNGLE_LOG,
                 STRIPPED_ACACIA_LOG,
                 STRIPPED_DARK_OAK_LOG,
                 STRIPPED_MANGROVE_LOG,
                 STRIPPED_CHERRY_LOG,
                 STRIPPED_CRIMSON_STEM,
                 STRIPPED_WARPED_STEM -> true;
            default -> false;
        };
    }

    private boolean shouldApplyFortune(Material blockType, Material dropType) {
        return switch (blockType) {
            case OAK_LOG,
                 STRIPPED_OAK_LOG -> dropType == Material.OAK_LOG || dropType == Material.STRIPPED_OAK_LOG;

            case BIRCH_LOG,
                 STRIPPED_BIRCH_LOG -> dropType == Material.BIRCH_LOG || dropType == Material.STRIPPED_BIRCH_LOG;

            case SPRUCE_LOG,
                 STRIPPED_SPRUCE_LOG -> dropType == Material.SPRUCE_LOG || dropType == Material.STRIPPED_SPRUCE_LOG;

            case JUNGLE_LOG,
                 STRIPPED_JUNGLE_LOG -> dropType == Material.JUNGLE_LOG || dropType == Material.STRIPPED_JUNGLE_LOG;

            case ACACIA_LOG,
                 STRIPPED_ACACIA_LOG -> dropType == Material.ACACIA_LOG || dropType == Material.STRIPPED_ACACIA_LOG;

            case DARK_OAK_LOG,
                 STRIPPED_DARK_OAK_LOG -> dropType == Material.DARK_OAK_LOG || dropType == Material.STRIPPED_DARK_OAK_LOG;

            case MANGROVE_LOG,
                 STRIPPED_MANGROVE_LOG -> dropType == Material.MANGROVE_LOG || dropType == Material.STRIPPED_MANGROVE_LOG;

            case CHERRY_LOG,
                 STRIPPED_CHERRY_LOG -> dropType == Material.CHERRY_LOG || dropType == Material.STRIPPED_CHERRY_LOG;

            case CRIMSON_STEM,
                 STRIPPED_CRIMSON_STEM -> dropType == Material.CRIMSON_STEM || dropType == Material.STRIPPED_CRIMSON_STEM;

            case WARPED_STEM,
                 STRIPPED_WARPED_STEM -> dropType == Material.WARPED_STEM || dropType == Material.STRIPPED_WARPED_STEM;

            default -> false;
        };
    }
}