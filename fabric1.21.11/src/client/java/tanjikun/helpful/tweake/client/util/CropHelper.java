package tanjikun.helpful.tweake.client.util;

import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 作物收获辅助工具：成熟度检测、种子映射、黑名单检查、状态重置。
 * 支持原版作物（小麦/胡萝卜/马铃薯/甜菜根/火把花/地狱疣/可可豆/瓶子草），
 * 以及继承 CropBlock 的模组作物。
 *
 * 特殊：火把花成熟后（age=2）方块变成 Blocks.TORCHFLOWER（FlowerBlock），
 * 不再是 CropBlock。需要单独判断 Blocks.TORCHFLOWER。
 */
public class CropHelper
{
    // 原版作物方块 → 种子/种植物品
    // 注意：Blocks.TORCHFLOWER（成熟火把花花朵）也映射到火把花种子
    private static final Map<Block, Item> CROP_TO_SEED = Map.of(
            Blocks.WHEAT, Items.WHEAT_SEEDS,
            Blocks.CARROTS, Items.CARROT,
            Blocks.POTATOES, Items.POTATO,
            Blocks.BEETROOTS, Items.BEETROOT_SEEDS,
            Blocks.TORCHFLOWER_CROP, Items.TORCHFLOWER_SEEDS,
            Blocks.TORCHFLOWER, Items.TORCHFLOWER_SEEDS,
            Blocks.NETHER_WART, Items.NETHER_WART,
            Blocks.COCOA, Items.COCOA_BEANS,
            Blocks.PITCHER_CROP, Items.PITCHER_POD
    );

    /**
     * 判断方块状态是否为成熟作物。
     * CropBlock 子类用 isMaxAge()（虚方法分派，子类重写 getMaxAge/getAgeProperty 后正确返回）。
     * 火把花成熟后变成 Blocks.TORCHFLOWER（非 CropBlock），需单独判断。
     */
    public static boolean isMature(BlockState state)
    {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop)
        {
            return crop.isMaxAge(state);
        }
        // 火把花成熟后（age=2）方块变成 Blocks.TORCHFLOWER（FlowerBlock），不再是 CropBlock
        if (block == Blocks.TORCHFLOWER)
        {
            return true;
        }
        if (block instanceof NetherWartBlock)
        {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (block instanceof CocoaBlock)
        {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        if (block instanceof PitcherCropBlock)
        {
            return state.getValue(PitcherCropBlock.AGE) >= 4;
        }
        return false;
    }

    /**
     * 获取作物对应的种子物品，未知作物返回 null。
     */
    public static Item getSeedFor(BlockState state)
    {
        return CROP_TO_SEED.get(state.getBlock());
    }

    /**
     * 获取重置为 age=0 的作物状态（保留其他属性如 HALF）。
     *
     * CropBlock 子类不能用 CropBlock.AGE 静态字段：子类（如 BeetrootBlock）
     * 定义了自己的 AGE（0-3），与基类 AGE（0-7）是不同的 IntegerProperty 实例。
     * 改为遍历 BlockState 的属性，找名为 "age" 的 IntegerProperty（属性名字符串运行时稳定）。
     *
     * 火把花成熟后是 Blocks.TORCHFLOWER，重置为 Blocks.TORCHFLOWER_CROP 的默认状态（age=0）。
     */
    public static BlockState getResetState(BlockState state)
    {
        Block block = state.getBlock();
        if (block instanceof CropBlock)
        {
            IntegerProperty ageProp = findAgeProperty(state);
            if (ageProp != null)
            {
                return state.setValue(ageProp, 0);
            }
            return state;
        }
        // 火把花成熟后是 Blocks.TORCHFLOWER，重置为作物方块的 age=0 状态
        if (block == Blocks.TORCHFLOWER)
        {
            return Blocks.TORCHFLOWER_CROP.defaultBlockState();
        }
        if (block instanceof NetherWartBlock)
        {
            return state.setValue(NetherWartBlock.AGE, 0);
        }
        if (block instanceof CocoaBlock)
        {
            return state.setValue(CocoaBlock.AGE, 0);
        }
        if (block instanceof PitcherCropBlock)
        {
            return state.setValue(PitcherCropBlock.AGE, 0);
        }
        return state;
    }

    /**
     * 遍历 BlockState 的属性，找名为 "age" 的 IntegerProperty。
     * 属性名字符串 "age" 在运行时不变（不是方法名/字段名，不受混淆影响）。
     */
    private static IntegerProperty findAgeProperty(BlockState state)
    {
        for (Property<?> prop : state.getProperties())
        {
            if (prop instanceof IntegerProperty && "age".equals(prop.getName()))
            {
                return (IntegerProperty) prop;
            }
        }
        return null;
    }

    /**
     * 检查方块是否在黑名单中（按游戏内 ID 匹配，如 "minecraft:wheat"）。
     */
    public static boolean isBlacklisted(BlockState state, List<String> blacklist)
    {
        if (blacklist.isEmpty())
        {
            return false;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String idStr = id.toString();
        for (String entry : blacklist)
        {
            if (idStr.equals(entry.trim()))
            {
                return true;
            }
        }
        return false;
    }
}
