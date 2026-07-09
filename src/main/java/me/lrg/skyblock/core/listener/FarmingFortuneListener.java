package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.FortuneManager;
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

/**
 * Farming Fortuneを作物ドロップへ反映するListener。
 *
 * このクラスの役割:
 * - 作物ブロック破壊を受け取る
 * - FortuneManagerでドロップ量を計算する
 * - Fortune対象アイテムだけ増やして落とす
 *
 * 注意:
 * - SQLは書かない
 * - StatsDataを直接触らない
 * - Fortune計算はFortuneManagerに任せる
 */
public class FarmingFortuneListener implements Listener {

    private final FortuneManager fortuneManager;

    public FarmingFortuneListener(FortuneManager fortuneManager) {
        this.fortuneManager = Objects.requireNonNull(fortuneManager, "fortuneManager");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (!isFarmingFortuneTarget(blockType)) {
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

        if (fortune <= 0.0) {
            return;
        }

        Collection<ItemStack> originalDrops = block.getDrops(player.getInventory().getItemInMainHand(), player);

        if (originalDrops.isEmpty()) {
            return;
        }

        event.setDropItems(false);

        World world = block.getWorld();

        for (ItemStack originalDrop : originalDrops) {
            ItemStack finalDrop = originalDrop.clone();

            if (shouldApplyFortune(blockType, finalDrop.getType())) {
                int finalAmount = fortuneManager.calculateDropAmount(finalDrop.getAmount(), fortune);
                finalDrop.setAmount(finalAmount);
            }

            world.dropItemNaturally(block.getLocation(), finalDrop);
        }
    }

    /**
     * サトウキビ・竹・サボテン専用。
     *
     * 重要:
     * - event.setDropItems(false) だけだと上のブロックが通常ドロップする可能性がある
     * - そのためイベントをキャンセルして、対象ブロックを全部手動でAIRにする
     * - 壊したブロック数にFortuneを乗せて、まとめてドロップする
     */
    private void handleVerticalCrop(BlockBreakEvent event, Player player, Block block, Material blockType) {
        double fortune = fortuneManager.getFarmingFortune(player, blockType);

        if (fortune <= 0.0) {
            return;
        }

        int brokenAmount = countVerticalCropBlocks(block, blockType);

        if (brokenAmount <= 0) {
            return;
        }

        event.setDropItems(false);

        Material dropMaterial = getVerticalCropDropMaterial(blockType);

        if (dropMaterial == Material.AIR) {
            return;
        }

        int finalAmount = fortuneManager.calculateDropAmount(brokenAmount, fortune);

        if (finalAmount <= 0) {
            return;
        }

        ItemStack dropItem = new ItemStack(dropMaterial, finalAmount);
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

        block.getWorld().dropItemNaturally(dropLocation, dropItem);
    }

    /**
     * 壊した位置から上方向に、同じ縦作物が何個あるか数える。
     *
     * 例:
     * サトウキビ3段の一番下を壊す
     * -> 3個として数える
     *
     * 真ん中を壊す
     * -> 真ん中 + 上だけ数える
     */
    private int countVerticalCropBlocks(Block startBlock, Material material) {
        int count = 0;
        Block currentBlock = startBlock;

        while (currentBlock.getType() == material) {
            count++;
            currentBlock = currentBlock.getRelative(0, 1, 0);
        }

        return count;
    }

    /**
     * 壊した位置から上方向の縦作物を全部AIRにする。
     */

    private boolean isFarmingFortuneTarget(Material material) {
        return switch (material) {
            case WHEAT,
                 CARROTS,
                 POTATOES,
                 PUMPKIN,
                 SUGAR_CANE,
                 BAMBOO,
                 BAMBOO_SAPLING,
                 MELON,
                 CACTUS,
                 COCOA,
                 BROWN_MUSHROOM,
                 RED_MUSHROOM,
                 MUSHROOM_STEM,
                 NETHER_WART -> true;
            default -> false;
        };
    }

    /**
     * 成長段階がある作物は最大成長だけFortune対象にする。
     * サトウキビ・竹・サボテン・カボチャ・スイカなどはAgeableではないので常にtrue。
     */
    private boolean isFullyGrown(Block block) {
        BlockData blockData = block.getBlockData();

        if (!(blockData instanceof Ageable ageable)) {
            return true;
        }

        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private boolean isVerticalCrop(Material material) {
        return switch (material) {
            case SUGAR_CANE,
                 BAMBOO,
                 BAMBOO_SAPLING,
                 CACTUS -> true;
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

    /**
     * 作物本体だけFortuneを乗せる。
     * 種などにはFortuneを乗せない。
     */
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
}