package tanjikun.helpful.tweake.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.CommonConfigs;

/**
 * 全局经验修补：当功能开启时，玩家拾取经验球后，装备栏/主副手/快捷栏/背包内
 * 所有带经验修补附魔且已损坏的物品都会参与修复，不需要拿在手上。
 *
 * 潜影盒等可存储物品的容器内部的物品天然不被遍历（只看 Inventory 直接持有的 ItemStack），
 * 满足"忽略容器内物品"的需求。
 *
 * 原理：原版 repairPlayerItems 只遍历 6 个 EquipmentSlot（含主副手）查找候选，
 * 本 Mixin 在功能开启时完全替代原版逻辑，扩展候选池到全背包 42 格
 * （6 装备槽 + 36 主背包/快捷栏），并自行实现"随机选一个、按比例消耗 xp"的循环。
 * 此为基于经验修补原理的独立重写，未照搬原版代码。
 *
 * 双端 Mixin：此 Mixin 在 main source set，同时作用于客户端和专用服务器。
 * 配置值通过 CommonConfigs 读取，客户端由 MaLiLib 同步，服务端由 JSON 文件提供。
 *
 * ponytail: Mixin 方法名使用 intermediary（method_35051）而非 named（repairPlayerItems）
 * 已知上限: 本项目用 officialMojangMappings 但 Loom 1.17 未生成 refmap，导致 Mixin 注解
 *           的 method 字符串无法从 named 重映射到 intermediary。运行时游戏类用 intermediary。
 *           method_35051 是 ExperienceOrb 中的静态方法，签名 (ServerPlayer, int) -> int，
 *           无歧义。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名 "repairPlayerItems"。
 */
@Mixin(ExperienceOrb.class)
public class MixinExperienceOrb
{
    // method_35051 = ExperienceOrb.repairPlayerItems(ServerPlayer, int) -> int
    // 静态方法，handler 也必须是 static，且没有 this 参数
    @Inject(method = "method_35051", at = @At("HEAD"), cancellable = true, remap = false)
    private static void helpfulTweake$repairAllItems(ServerPlayer player, int xp,
                                                     CallbackInfoReturnable<Integer> cir)
    {
        if (!CommonConfigs.globalMending)
        {
            // 功能关闭，走原版逻辑
            return;
        }
        cir.setReturnValue(helpfulTweake$doRepairAll(player, xp));
    }

    /**
     * 自定义修复逻辑：遍历全背包候选，循环消耗 xp 修复随机物品。
     * 返回剩余未用于修复的 xp（会作为普通经验给予玩家）。
     */
    private static int helpfulTweake$doRepairAll(ServerPlayer player, int xp)
    {
        if (xp <= 0)
        {
            return xp;
        }

        // 收集候选物品：装备槽 6 格（含主副手）+ 主背包+快捷栏 36 格
        // Inventory.getNonEquipmentItems() 返回快捷栏+主背包（不含装备），正好补齐
        // 潜影盒等容器内的物品不在 Inventory 直接持有中，天然不被遍历
        List<ItemStack> candidates = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            ItemStack stack = player.getItemBySlot(slot);
            if (helpfulTweake$isRepairable(stack))
            {
                candidates.add(stack);
            }
        }

        Inventory inv = player.getInventory();
        NonNullList<ItemStack> nonEquipment = inv.getNonEquipmentItems();
        for (ItemStack stack : nonEquipment)
        {
            if (helpfulTweake$isRepairable(stack))
            {
                candidates.add(stack);
            }
        }

        if (candidates.isEmpty())
        {
            return xp;
        }

        // 1.21.11 中 ServerPlayer 无 serverLevel() 方法，用 (ServerLevel) player.level() 显式转换
        // 运行时玩家在服务端，level() 实际返回 ServerLevel，cast 安全
        ServerLevel level = (ServerLevel) player.level();
        int remaining = xp;

        // 循环：每次随机选一个候选修复，直到 xp 耗尽或候选全部修满
        while (remaining > 0 && !candidates.isEmpty())
        {
            int idx = level.getRandom().nextInt(candidates.size());
            ItemStack target = candidates.get(idx);

            // 候选可能在循环过程中被修满
            if (!target.isDamaged())
            {
                candidates.remove(idx);
                continue;
            }

            // 当前剩余 xp 能修复多少耐久
            int durabilityRepairable = EnchantmentHelper.modifyDurabilityToRepairFromXp(level, target, remaining);
            if (durabilityRepairable <= 0)
            {
                candidates.remove(idx);
                continue;
            }

            int currentDamage = target.getDamageValue();
            int actualRepair = Math.min(durabilityRepairable, currentDamage);
            target.setDamageValue(currentDamage - actualRepair);

            // 按比例计算消耗的 xp：
            // modifyDurabilityToRepairFromXp(level, stack, xp) 返回"xp 能修复的耐久"，
            // 反推：修复 actualRepair 耐久消耗的 xp = actualRepair * xp / durabilityRepairable（向上取整）
            int xpUsed = (int) Math.ceil((double) actualRepair * remaining / durabilityRepairable);
            if (xpUsed <= 0)
            {
                xpUsed = 1;
            }
            remaining -= xpUsed;
            if (remaining < 0)
            {
                remaining = 0;
            }

            // 已修满则从候选移除
            if (!target.isDamaged())
            {
                candidates.remove(idx);
            }
        }

        return remaining;
    }

    /**
     * 判断物品是否可被经验修补修复：必须带 REPAIR_WITH_XP 效果且已损坏。
     * 1.21.11 中经验修补附魔通过 data-driven 的 EnchantmentEffectComponents.REPAIR_WITH_XP
     * 组件表达，EnchantmentHelper.has 会遍历物品附魔组件检查是否包含该效果。
     */
    private static boolean helpfulTweake$isRepairable(ItemStack stack)
    {
        return stack != null && !stack.isEmpty()
                && stack.isDamaged()
                && EnchantmentHelper.has(stack, EnchantmentEffectComponents.REPAIR_WITH_XP);
    }
}
