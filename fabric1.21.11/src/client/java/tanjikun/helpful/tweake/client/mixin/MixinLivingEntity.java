package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 更好的自动跳跃：当功能开启且实体为本地玩家时，将台阶高度属性返回值提升到 1.25，
 * 使玩家可直接"走上"≤1.25 格高的障碍而无需触发跳跃，从而不损失水平速度。
 *
 * 原理：Minecraft 中 step height 决定实体能不跳跃直接跨越的高度上限。
 * 原版 auto-jump 会触发真实跳跃并重置水平速度；本功能只抬高 step height，
 * 走上去的过程由引擎内置的碰撞抬升逻辑完成，水平速度得以保留。
 * 灵感来源于 Accessible Step，此为独立重写实现。
 *
 * ponytail: Mixin 方法名使用 intermediary（method_45325）而非 named（getAttributeValue）
 * 已知上限: 本项目用 officialMojangMappings 但 Loom 1.17 未生成 refmap，导致 Mixin 注解
 *           的 method 字符串无法从 named 重映射到 intermediary。运行时游戏类用 intermediary。
 *           method_45325 在 LivingEntity 中唯一（对应 getAttributeValue(Attribute):double），
 *           无歧义，故不带描述符即可精确匹配。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名 "getAttributeValue"。
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntity
{
    private static final double BETTER_AUTO_JUMP_HEIGHT = 1.25;

    // remap = false: 方法名已是 intermediary，不需 Loom 重映射
    // 参数类型用 Holder<Attribute>（1.21.11 中 getAttributeValue 的签名是 (Holder)D）
    @Inject(method = "method_45325", remap = false,
            at = @At("RETURN"), cancellable = true)
    private void helpfulTweake$modifyStepHeight(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir)
    {
        // 仅对 STEP_HEIGHT 属性、本地玩家、功能开启时生效
        // Attributes.STEP_HEIGHT 本身就是 Holder<Attribute> 类型
        if (attribute == Attributes.STEP_HEIGHT
                && Configs.Tools.BETTER_AUTO_JUMP.getBooleanValue())
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && (Object) this == mc.player)
            {
                cir.setReturnValue(BETTER_AUTO_JUMP_HEIGHT);
            }
        }
    }
}
