package tanjikun.helpful.tweake.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.level.gameevent.GameEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.CommonConfigs;

/**
 * 更好的不死图腾：当功能开启时，玩家死亡触发图腾时，从装备栏、主副手、快捷栏、背包中
 * 查找第一个带 DEATH_PROTECTION 组件的物品（即不死图腾），不需要拿在手上。
 *
 * 潜影盒等可存储物品的容器内部的物品天然不被遍历（只看 Inventory 直接持有的 ItemStack），
 * 满足"忽略容器内物品"的需求。
 *
 * 原版 checkTotemDeathProtection 仅遍历主副手（InteractionHand.values()），本 Mixin 在功能
 * 开启时完全替代原版逻辑，将候选池扩展到全背包 42 格（6 装备槽 + 36 主背包/快捷栏）。
 *
 * 与原版一致的行为：
 *   - BYPASSES_INVULNERABILITY 标签的伤害（如 /kill 的 OUT_OF_WORLD）不可被图腾抵抗，直接返回 false
 *   - 找到图腾后：复制物品栈 → 消耗 1 个 → 触发统计/进度（仅 ServerPlayer）→ setHealth(1) →
 *     applyEffects（恢复、抗火、伤害吸收等）→ broadcastEntityEvent(35) 触发客户端图腾动画
 *
 * ponytail: Mixin 方法名使用 intermediary（method_6095）而非 named（checkTotemDeathProtection）
 * 已知上限: 本项目用 officialMojangMappings 但 Loom 1.17 未生成 refmap，导致 Mixin 注解
 *           的 method 字符串无法从 named 重映射到 intermediary。运行时游戏类用 intermediary。
 *           method_6095 是 LivingEntity 中的 private 方法，签名 (DamageSource) -> boolean，
 *           无歧义。原版方法是 private，但 Mixin 仍可注入（remap=false 避免 refmap 依赖）。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名 "checkTotemDeathProtection"。
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityDeath
{
    // method_6095 = LivingEntity.checkTotemDeathProtection(DamageSource) -> boolean
    // 原版方法可见性为 private，handler 用默认实例方法签名即可（不带 static）
    @Inject(method = "method_6095", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$useTotemFromAnywhere(DamageSource source,
                                                     CallbackInfoReturnable<Boolean> cir)
    {
        if (!CommonConfigs.betterTotem)
        {
            // 功能关闭，走原版逻辑
            return;
        }

        // 与原版第一行一致：BYPASSES_INVULNERABILITY 伤害（/kill 等）不可被图腾抵抗
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
        {
            cir.setReturnValue(false);
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;

        // 收集候选：装备槽 6 格（含主副手）+ 主背包/快捷栏 36 格
        // Inventory.getNonEquipmentItems() 返回快捷栏+主背包（不含装备），正好补齐
        // 潜影盒等容器内的物品不在 Inventory 直接持有中，天然不被遍历
        List<ItemStack> candidates = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            ItemStack stack = self.getItemBySlot(slot);
            if (helpfulTweake$hasDeathProtection(stack))
            {
                candidates.add(stack);
            }
        }

        // 仅 Player 有 Inventory；非 Player 的 LivingEntity（如僵尸）不会有图腾，跳过
        if (self instanceof Player player)
        {
            Inventory inv = player.getInventory();
            NonNullList<ItemStack> nonEquipment = inv.getNonEquipmentItems();
            for (ItemStack stack : nonEquipment)
            {
                if (helpfulTweake$hasDeathProtection(stack))
                {
                    candidates.add(stack);
                }
            }
        }

        if (candidates.isEmpty())
        {
            cir.setReturnValue(false);
            return;
        }

        // 用第一个候选触发图腾（原版用 break 取第一个，这里取列表首项，行为等价）
        ItemStack totem = candidates.get(0);
        DeathProtection deathProtection = totem.get(DataComponents.DEATH_PROTECTION);
        ItemStack totemCopy = totem.copy();
        totem.shrink(1);

        // 触发统计/进度（仅 ServerPlayer，与原版一致）
        if (self instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.awardStat(Stats.ITEM_USED.get(totemCopy.getItem()));
            CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, totemCopy);
            Holder.Reference<GameEvent> eventRef = GameEvent.ITEM_INTERACT_FINISH;
            totemCopy.causeUseVibration(self, eventRef);
        }

        // 设置生命为 1（保留半颗心）
        self.setHealth(1.0F);

        // 应用图腾的 deathEffects（清除效果 + 再生 + 抗火 + 伤害吸收等）
        deathProtection.applyEffects(totemCopy, self);

        // 广播图腾动画事件（客户端接收 byte 35 后播放图腾动画 + 粒子 + 音效）
        self.level().broadcastEntityEvent(self, (byte) 35);

        cir.setReturnValue(true);
    }

    /**
     * 判断物品是否带 DEATH_PROTECTION 组件（即原版不死图腾判定方式）。
     * 1.21+ 中不死图腾通过 DataComponents.DEATH_PROTECTION 数据组件表达，
     * 支持模组自定义的"死亡保护"物品。
     * 用 get(...) != null 判定，与原版字节码一致（DataComponentHolder 接口无 has 方法）。
     */
    private static boolean helpfulTweake$hasDeathProtection(ItemStack stack)
    {
        return stack != null && !stack.isEmpty()
                && stack.get(DataComponents.DEATH_PROTECTION) != null;
    }
}
