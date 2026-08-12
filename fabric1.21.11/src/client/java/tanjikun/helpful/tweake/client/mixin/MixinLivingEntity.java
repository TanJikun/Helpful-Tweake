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
 * 更好的自动跳跃：对 LivingEntity 的台阶高度属性注入。
 *
 * 当功能开启且实体为本地玩家时，将台阶高度属性返回值提升到 1.25，
 * 使玩家可直接"走上"≤1.25 格高的障碍而无需触发跳跃，从而不损失水平速度。
 * 灵感来源于 Accessible Step，此为独立重写实现。
 *
 * 子配置"潜行不上坡"开启且玩家潜行时，强制台阶高度为 0，
 * 使玩家潜行时连地毯（0.0625 高）也无法登上。仅在更好的自动跳跃开启时生效。
 *
 * ponytail: Mixin 方法名使用 intermediary（method_45325）而非 named。
 * 已知上限: Loom 1.17 + officialMojangMappings 不生成 refmap，注解字符串无法从
 *           named 重映射到 intermediary，运行时游戏类用 intermediary。
 *           method_45325 = getAttributeValue(Holder):double，在 LivingEntity 中无歧义。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名。
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntity
{
    private static final double BETTER_AUTO_JUMP_HEIGHT = 1.25;
    private static final double SNEAK_NO_SLOPE_HEIGHT = 0.0;

    // method_45325 = LivingEntity.getAttributeValue(Holder<Attribute>) (intermediary 名)
    // remap = false: 方法名已是 intermediary，不需 Loom 重映射
    @Inject(method = "method_45325", remap = false,
            at = @At("RETURN"), cancellable = true)
    private void helpfulTweake$modifyStepHeight(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir)
    {
        if (attribute == Attributes.STEP_HEIGHT
                && Configs.Tools.BETTER_AUTO_JUMP.getBooleanValue())
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && (Object) this == mc.player)
            {
                // 潜行不上坡：潜行时强制 step height = 0，连地毯（0.0625 高）也登不上
                if (Configs.Tools.SNEAK_NO_SLOPE.getBooleanValue()
                        && mc.player.isShiftKeyDown())
                {
                    cir.setReturnValue(SNEAK_NO_SLOPE_HEIGHT);
                    return;
                }
                cir.setReturnValue(BETTER_AUTO_JUMP_HEIGHT);
            }
        }
    }
}
