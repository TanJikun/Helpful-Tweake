package tanjikun.helpful.tweake.client.mixin;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.client.util.CropHelper;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 更方便的收获：拦截玩家右键方块，若目标是成熟作物则执行收获+重新种植。
 * 在单机模式下通过 IntegratedServer 获取服务端世界执行操作（方块重置/掉落/耐久消耗），
 * 多人专用服务器上不生效（需要服务端也安装本模组）。
 *
 * 1.21.11 中 MultiPlayerGameMode 有两个 useItemOn 相关方法（签名相同）：
 *   - method_2896  (useItemOn,        public,  旧 API，不再被主流程调用)
 *   - method_41934 (performUseItemOn, private,  新方法，Minecraft.startUseItem 主流程调用)
 * 必须同时注入两个方法以确保覆盖所有调用路径，否则右键收获不生效。
 *
 * remap=false: Loom 1.17 + officialMojangMappings 不生成 refmap，直接用 intermediary 名
 */
@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameModeHarvest
{
    // 防止同一个右键事件中 method_2896 和 method_41934 双重调用
    private static long lastHarvestTime = 0;
    private static BlockPos lastHarvestPos = null;

    @Inject(method = "method_2896", remap = false,
            at = @At("HEAD"), cancellable = true)
    private void helpfulTweake$harvestOnUseItemOn(LocalPlayer player, InteractionHand hand,
                                                   BlockHitResult result,
                                                   CallbackInfoReturnable<InteractionResult> cir)
    {
        helpfulTweake$tryHarvest(player, hand, result, cir);
    }

    @Inject(method = "method_41934", remap = false,
            at = @At("HEAD"), cancellable = true)
    private void helpfulTweake$harvestOnPerformUseItemOn(LocalPlayer player, InteractionHand hand,
                                                          BlockHitResult result,
                                                          CallbackInfoReturnable<InteractionResult> cir)
    {
        helpfulTweake$tryHarvest(player, hand, result, cir);
    }

    @Unique
    private void helpfulTweake$tryHarvest(LocalPlayer player, InteractionHand hand,
                                           BlockHitResult result,
                                           CallbackInfoReturnable<InteractionResult> cir)
    {
        if (!Configs.Tools.BETTER_HARVEST.getBooleanValue())
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || player == null)
        {
            return;
        }

        BlockPos pos = result.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        if (!CropHelper.isMature(state))
        {
            return;
        }

        if (CropHelper.isBlacklisted(state, Configs.Tools.HARVEST_BLACKLIST.getStrings()))
        {
            return;
        }

        // 防止双重调用：同一方块在 100ms 内只处理一次
        long now = System.currentTimeMillis();
        if (lastHarvestPos == pos && now - lastHarvestTime < 100)
        {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        lastHarvestTime = now;
        lastHarvestPos = pos;

        // 检查锄头要求
        ItemStack mainHand = player.getMainHandItem();
        boolean requireHoe = Configs.Tools.HARVEST_REQUIRE_HOE.getBooleanValue();
        if (requireHoe && !(mainHand.getItem() instanceof HoeItem))
        {
            return;
        }

        // 获取服务端世界（仅单机模式可用）
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null)
        {
            return;
        }

        ServerLevel serverLevel = server.getLevel(player.level().dimension());
        if (serverLevel == null)
        {
            return;
        }

        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
        if (serverPlayer == null)
        {
            return;
        }

        // 瓶子草是双格植物：成熟时 age=3，分 LOWER/UPPER 两格
        // 右键 UPPER 时需定位到 LOWER（作物主体在下半部分，掉落/重置都基于下半部分）
        // 收获时清除上半部分，重置下半部分为 age=0（单格幼苗）
        boolean isPitcher = state.getBlock() instanceof PitcherCropBlock;
        BlockPos harvestPos = pos;
        BlockState harvestState = state;
        if (isPitcher && state.getValue(PitcherCropBlock.HALF) == DoubleBlockHalf.UPPER)
        {
            harvestPos = pos.below();
            harvestState = serverLevel.getBlockState(harvestPos);
        }

        // 生成掉落物（手持物品作为 tool，自动应用时运附魔）
        ItemStack tool = mainHand.isEmpty() ? player.getOffhandItem() : mainHand;
        List<ItemStack> drops = Block.getDrops(harvestState, serverLevel, harvestPos,
                serverLevel.getBlockEntity(harvestPos), null, tool);

        // 从掉落物中优先移除1颗种子用于补种（模拟原版收获-补种逻辑）
        // 这样小麦等作物用掉落的种子补种，不消耗背包种子
        // 火把花/瓶子草(age=4)的掉落物不含种子，需要从背包消耗
        Item seed = CropHelper.getSeedFor(harvestState);
        boolean hasSeed = false;
        if (seed != null)
        {
            hasSeed = helpfulTweake$takeSeedFromDrops(drops, seed);
            if (!hasSeed)
            {
                hasSeed = helpfulTweake$consumeItem(serverPlayer, seed);
            }
        }

        System.out.println("[HelpfulTweake] 收获执行: " + state.getBlock() + " at " + pos
                + ", drops=" + drops.size() + ", isPitcher=" + isPitcher
                + ", seed=" + seed + ", hasSeed=" + hasSeed);

        // 给服务端玩家剩余掉落物
        for (ItemStack drop : drops)
        {
            if (!serverPlayer.getInventory().add(drop))
            {
                serverPlayer.drop(drop, false);
            }
        }

        if (isPitcher)
        {
            // 先重置下半部分为 age=0 + HALF=LOWER（单格幼苗）
            // 再清除上半部分（用 setBlock AIR 替代 destroyBlock 避免 onRemove 副作用）
            if (hasSeed)
            {
                BlockState resetState = harvestState
                        .setValue(PitcherCropBlock.AGE, 0)
                        .setValue(PitcherCropBlock.HALF, DoubleBlockHalf.LOWER);
                serverLevel.setBlock(harvestPos, resetState, Block.UPDATE_ALL);
            }
            else
            {
                serverLevel.setBlock(harvestPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            // 清除上半部分
            serverLevel.setBlock(harvestPos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        else
        {
            if (hasSeed)
            {
                BlockState resetState = CropHelper.getResetState(harvestState);
                serverLevel.setBlock(harvestPos, resetState, Block.UPDATE_ALL);
            }
            else
            {
                serverLevel.destroyBlock(harvestPos, false);
            }
        }

        // 消耗锄头耐久（hurtAndBreak 内部自动处理耐久附魔）
        if (requireHoe && Configs.Tools.HARVEST_HOE_DURABILITY.getBooleanValue())
        {
            ItemStack serverMainHand = serverPlayer.getMainHandItem();
            if (!serverMainHand.isEmpty())
            {
                serverMainHand.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
            }
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Unique
    private boolean helpfulTweake$consumeItem(Player player, Item item)
    {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item) && stack.getCount() > 0)
            {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /**
     * 从掉落物列表中移除1颗种子用于补种。
     * 返回 true 表示找到并移除了种子，false 表示掉落物中不含种子。
     */
    @Unique
    private boolean helpfulTweake$takeSeedFromDrops(List<ItemStack> drops, Item seed)
    {
        java.util.Iterator<ItemStack> it = drops.iterator();
        while (it.hasNext())
        {
            ItemStack drop = it.next();
            if (drop.is(seed))
            {
                drop.shrink(1);
                if (drop.isEmpty())
                {
                    it.remove();
                }
                return true;
            }
        }
        return false;
    }
}
