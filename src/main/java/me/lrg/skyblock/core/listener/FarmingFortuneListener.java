package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.autopickup.AutoPickupManager;
import me.lrg.skyblock.core.manager.FortuneManager;
import me.lrg.skyblock.core.manager.PlacedBlockTracker;
import me.lrg.skyblock.core.util.FortuneToolUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Objects;

public class FarmingFortuneListener implements Listener {

    private final FortuneManager fortuneManager;
    private final PlacedBlockTracker placedBlockTracker;
    private final AutoPickupManager autoPickupManager;

    public FarmingFortuneListener(FortuneManager fortuneManager, PlacedBlockTracker placedBlockTracker, AutoPickupManager autoPickupManager) {
        this.fortuneManager = Objects.requireNonNull(fortuneManager, "fortuneManager");
        this.placedBlockTracker = Objects.requireNonNull(placedBlockTracker, "placedBlockTracker");
        this.autoPickupManager = Objects.requireNonNull(autoPickupManager, "autoPickupManager");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isDropEligible(player)) {
            return;
        }
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (placedBlockTracker.isPlayerPlaced(block)) {
            return;
        }

        if (!fortuneManager.isFarmingTarget(blockType)) {
            return;
        }

        if (isVerticalCrop(blockType)) {
            handleVerticalCrop(event, player, block, blockType);
            return;
        }

        handleNormalCrop(event, player, block, blockType);
    }

    private void handleNormalCrop(BlockBreakEvent event, Player player, Block block, Material blockType) {
        if (!isFullyGrown(block)) {
            return;
        }

        double fortune = fortuneManager.getFarmingFortune(player, blockType);
        ItemStack originalTool = player.getInventory().getItemInMainHand();

        if (fortune <= 0.0 && !FortuneToolUtil.hasVanillaFortune(originalTool)) {
            return;
        }

        ItemStack toolWithoutFortune = FortuneToolUtil.createWithoutVanillaFortune(originalTool);
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

            deliverOrDrop(player, block.getLocation(), java.util.List.of(finalDrop), world);
        }
    }

    private void handleVerticalCrop(BlockBreakEvent event, Player player, Block block, Material blockType) {
        double fortune = fortuneManager.getFarmingFortune(player, blockType);
        ItemStack originalTool = player.getInventory().getItemInMainHand();

        if (fortune <= 0.0 && !FortuneToolUtil.hasVanillaFortune(originalTool)) {
            return;
        }

        int brokenAmount = removeAndCountVerticalCropBlocks(block, blockType);
        if (brokenAmount <= 0) {
            return;
        }

        event.setDropItems(false);
        Material dropMaterial = getVerticalCropDropMaterial(blockType);

        if (dropMaterial == Material.AIR) {
            return;
        }

        int finalAmount = fortune > 0.0
                ? fortuneManager.calculateDropAmount(brokenAmount, fortune)
                : brokenAmount;

        if (finalAmount <= 0) {
            return;
        }

        ItemStack dropItem = new ItemStack(dropMaterial, finalAmount);
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        deliverOrDrop(player, dropLocation, java.util.List.of(dropItem), block.getWorld());
    }

    private int removeAndCountVerticalCropBlocks(Block startBlock, Material material) {
        int count = 1;
        Block currentBlock = startBlock.getRelative(0, 1, 0);

        while (currentBlock.getType() == material) {
            count++;
            Block nextBlock = currentBlock.getRelative(0, 1, 0);
            currentBlock.setType(Material.AIR, false);
            currentBlock = nextBlock;
        }

        return count;
    }

    private boolean isDropEligible(Player player) {
        GameMode gameMode = player.getGameMode();
        return gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR;
    }

    private boolean isFarmingFortuneTarget(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, PUMPKIN,
                 SUGAR_CANE, BAMBOO, BAMBOO_SAPLING,
                 MELON, CACTUS, COCOA, BROWN_MUSHROOM,
                 RED_MUSHROOM, MUSHROOM_STEM, NETHER_WART -> true;
            default -> false;
        };
    }

    private boolean isFullyGrown(Block block) {
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Ageable ageable)) {
            return true;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private boolean isVerticalCrop(Material material) {
        return switch (material) {
            case SUGAR_CANE, BAMBOO, BAMBOO_SAPLING, CACTUS -> true;
            default -> false;
        };
    }

    private Material getVerticalCropDropMaterial(Material blockType) {
        return switch (blockType) {
            case SUGAR_CANE -> Material.SUGAR_CANE;
            case BAMBOO, BAMBOO_SAPLING -> Material.BAMBOO;
            case CACTUS -> Material.CACTUS;
            default -> Material.AIR;
        };
    }

    private boolean shouldApplyFortune(Material blockType, Material dropType) {
        return switch (blockType) {
            case WHEAT -> dropType == Material.WHEAT;
            case CARROTS -> dropType == Material.CARROT;
            case POTATOES -> dropType == Material.POTATO;
            case PUMPKIN -> dropType == Material.PUMPKIN;
            case SUGAR_CANE -> dropType == Material.SUGAR_CANE;
            case BAMBOO, BAMBOO_SAPLING -> dropType == Material.BAMBOO;
            case MELON -> dropType == Material.MELON_SLICE;
            case CACTUS -> dropType == Material.CACTUS;
            case COCOA -> dropType == Material.COCOA_BEANS;
            case BROWN_MUSHROOM -> dropType == Material.BROWN_MUSHROOM;
            case RED_MUSHROOM -> dropType == Material.RED_MUSHROOM;
            case MUSHROOM_STEM -> dropType == Material.MUSHROOM_STEM;
            case NETHER_WART -> dropType == Material.NETHER_WART;
            default -> false;
        };
    }
    private void deliverOrDrop(Player player, Location location, Collection<ItemStack> drops, World world) {
        if (!autoPickupManager.collect(player, drops, location)) {
            drops.forEach(drop -> world.dropItemNaturally(location, drop));
        }
    }

}
