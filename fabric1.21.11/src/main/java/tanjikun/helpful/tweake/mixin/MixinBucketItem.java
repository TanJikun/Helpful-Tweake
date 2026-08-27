package tanjikun.helpful.tweake.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.CommonConfigs;

/**
 * 背包内无限水：当玩家快捷栏+副手+背包内共有两桶及以上的水（水桶或各种鱼桶，
 * 如鳕鱼/鲑鱼/河豚/热带鱼/美西螈/蝌蚪桶）时，生存模式下放水不消耗桶中的水：
 *   - 水桶放水后仍是水桶
 *   - 鱼桶放水（含鱼）后变为水桶（原版 1.21.11 会变成空桶）
 *
 * 注入点为 BucketItem.getEmptySuccessItem(ItemStack, Player)，它是 use() 中
 * 倒水成功后计算"返还物品"的静态方法（原版：非创造返回空桶）。
 * use() 随后调用 ItemUtils.createFilledResult 处理物品替换：
 *   - 手持 1 个：直接替换手上的物品（保持水桶）
 *   - 手持多个（1.21.5+ 桶可堆叠）：原栈消耗 1 个，返还的水桶加入背包，总量不减少
 *
 * 本 Mixin 位于 main 侧，同时作用于客户端（本地预测）与
 * 服务端（集成服务器/独立服务器，读 helpful-tweake-server.json 配置）。
 *
 * ponytail: Mixin 方法名使用 intermediary（method_7732）而非 named（getEmptySuccessItem）
 * 已知上限: 本项目用 officialMojangMappings 但 Loom 1.17 未生成 refmap，运行时游戏类用
 *           intermediary，named 名找不到目标方法。method_7732 在 BucketItem 中唯一无重载。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名。
 */
@Mixin(BucketItem.class)
public class MixinBucketItem
{
    // method_7732 = BucketItem.getEmptySuccessItem(ItemStack, Player) -> ItemStack（静态方法）
    @Inject(method = "method_7732", at = @At("RETURN"), cancellable = true, remap = false)
    private static void helpfulTweake$infiniteWater(ItemStack stack, Player player,
                                                    CallbackInfoReturnable<ItemStack> cir)
    {
        if (!CommonConfigs.infiniteWater)
        {
            return;
        }
        // 创造模式原版已返回原物品（stack），无需处理
        if (player.hasInfiniteMaterials())
        {
            return;
        }
        // 只处理装水的桶：水桶或鱼桶（所有原版 MobBucketItem 装的都是水）
        // 岩浆桶/细雪桶走原版逻辑
        if (!helpfulTweake$isWaterBucket(stack))
        {
            return;
        }
        if (helpfulTweake$countWaterBuckets(player) < 2)
        {
            return;
        }
        cir.setReturnValue(new ItemStack(Items.WATER_BUCKET));
    }

    private static boolean helpfulTweake$isWaterBucket(ItemStack stack)
    {
        return stack.is(Items.WATER_BUCKET) || stack.getItem() instanceof MobBucketItem;
    }

    /**
     * 统计快捷栏+主背包+副手中装水的桶总数（含堆叠数量）。
     * 潜影盒等容器内的物品不被遍历（只看 Inventory 直接持有的 ItemStack）。
     */
    private static int helpfulTweake$countWaterBuckets(Player player)
    {
        int count = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems())
        {
            if (helpfulTweake$isWaterBucket(s))
            {
                count += s.getCount();
            }
        }
        if (helpfulTweake$isWaterBucket(player.getOffhandItem()))
        {
            count += player.getOffhandItem().getCount();
        }
        return count;
    }
}
