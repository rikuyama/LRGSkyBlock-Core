package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.FortuneManager;
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

    private boolean isFarmingFortuneTarget(Material material) {
        return switch (material) {
            case WHEAT,
                 CARROTS,
                 POTATOES,
                 PUMPKIN,
                 SUGAR_CANE,
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
     * サトウキビ・サボテン・カボチャ・スイカなどはAgeableではないので常にtrue。
     */
    private boolean isFullyGrown(Block block) {
        BlockData blockData = block.getBlockData();

        if (!(blockData instanceof Ageable ageable)) {
            return true;
        }

        return ageable.getAge() >= ageable.getMaximumAge();
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