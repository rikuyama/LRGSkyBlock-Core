package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.FortuneManager;
import me.lrg.skyblock.core.util.FortuneToolUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Objects;

/**
 * LRGMine管理外の通常採掘にMining Fortuneを適用するListener。
 */
public class MiningFortuneListener implements Listener {

    private final FortuneManager fortuneManager;

    public MiningFortuneListener(FortuneManager fortuneManager) {
        this.fortuneManager = Objects.requireNonNull(fortuneManager, "fortuneManager");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (!isMiningFortuneTarget(blockType)) {
            return;
        }

        ItemStack originalTool = player.getInventory().getItemInMainHand();
        ItemStack toolWithoutFortune = FortuneToolUtil.createWithoutVanillaFortune(originalTool);
        Collection<ItemStack> originalDrops = block.getDrops(toolWithoutFortune, player);

        if (originalDrops.isEmpty()) {
            return;
        }

        double totalFortune = fortuneManager.getMiningFortune(player, blockType)
                + FortuneToolUtil.getMiningFortuneFromEnchant(originalTool);

        if (totalFortune <= 0.0) {
            return;
        }

        event.setDropItems(false);
        World world = block.getWorld();

        for (ItemStack originalDrop : originalDrops) {
            ItemStack finalDrop = originalDrop.clone();

            if (shouldApplyMiningFortune(blockType, finalDrop.getType())) {
                int finalAmount = fortuneManager.calculateDropAmount(finalDrop.getAmount(), totalFortune);
                finalDrop.setAmount(finalAmount);
            }

            world.dropItemNaturally(block.getLocation(), finalDrop);
        }
    }

    private boolean isMiningFortuneTarget(Material material) {
        return isOre(material)
                || isBlockFortuneTarget(material)
                || isGemstone(material)
                || isDwarvenMetal(material);
    }

    private boolean shouldApplyMiningFortune(Material blockType, Material dropType) {
        if (isOre(blockType)) {
            return isOreDrop(dropType);
        }
        if (isBlockFortuneTarget(blockType)) {
            return isBlockDrop(dropType);
        }
        if (isGemstone(blockType)) {
            return isGemstoneDrop(dropType);
        }
        if (isDwarvenMetal(blockType)) {
            return isDwarvenMetalDrop(dropType);
        }
        return false;
    }

    private boolean isOre(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                 IRON_ORE, DEEPSLATE_IRON_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 NETHER_GOLD_ORE, NETHER_QUARTZ_ORE -> true;
            default -> false;
        };
    }

    private boolean isOreDrop(Material material) {
        return switch (material) {
            case COAL, RAW_IRON, RAW_COPPER, RAW_GOLD,
                 REDSTONE, EMERALD, LAPIS_LAZULI,
                 DIAMOND, GOLD_NUGGET, QUARTZ -> true;
            default -> false;
        };
    }

    private boolean isBlockFortuneTarget(Material material) {
        return switch (material) {
            case STONE, COBBLESTONE, DEEPSLATE, COBBLED_DEEPSLATE,
                 ANDESITE, DIORITE, GRANITE, TUFF, CALCITE,
                 BASALT, BLACKSTONE, NETHERRACK, END_STONE -> true;
            default -> false;
        };
    }

    private boolean isBlockDrop(Material material) {
        return isBlockFortuneTarget(material);
    }

    private boolean isGemstone(Material material) {
        return switch (material) {
            case AMETHYST_BLOCK, BUDDING_AMETHYST, AMETHYST_CLUSTER,
                 SMALL_AMETHYST_BUD, MEDIUM_AMETHYST_BUD,
                 LARGE_AMETHYST_BUD -> true;
            default -> false;
        };
    }

    private boolean isGemstoneDrop(Material material) {
        return material == Material.AMETHYST_SHARD || material == Material.AMETHYST_BLOCK;
    }

    private boolean isDwarvenMetal(Material material) {
        return switch (material) {
            case IRON_ORE, DEEPSLATE_IRON_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 RAW_IRON_BLOCK, RAW_GOLD_BLOCK, RAW_COPPER_BLOCK -> true;
            default -> false;
        };
    }

    private boolean isDwarvenMetalDrop(Material material) {
        return switch (material) {
            case RAW_IRON, RAW_GOLD, RAW_COPPER,
                 RAW_IRON_BLOCK, RAW_GOLD_BLOCK, RAW_COPPER_BLOCK -> true;
            default -> false;
        };
    }
}
