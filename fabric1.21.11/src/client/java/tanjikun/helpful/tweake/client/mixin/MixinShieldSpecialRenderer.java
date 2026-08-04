package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.renderer.special.ShieldSpecialRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.client.util.ShieldRenderContext;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 盾牌状态显示：通过修改顶点颜色实现染色（绿色=可用，红色=冷却中）。
 *
 * 原版 ShieldSpecialRenderer.submit (method_65707) 内部通过 invokeinterface 调用
 * SubmitNodeCollector (class_11659) 上的 submitModelPart (method_73494) 渲染盾牌
 * plate 和 handle，第9个参数（index=8）是 color，原版传 -1（0xFFFFFFFF 白色，乘法=保持原色）。
 *
 * @ModifyArg 将 color 替换为带色调的颜色：
 * - 可用：0xFF30FF30（不透明高饱和绿）
 * - 冷却：0xFFFF3030（不透明高饱和红）
 *
 * 顶点颜色与纹理是乘法关系，白色保持原色，染色等于给盾牌加颜色滤镜。
 * 性能零开销，盾牌尺寸不变，完美支持材质包。
 *
 * TAIL 注入清理 ThreadLocal，防止颜色残留到下一次渲染。
 *
 * remap=false: method/target 用 intermediary 名（项目约定，Loom 1.17 + officialMojangMappings
 * 不生成 refmap）。target 的 owner 用 class_11659（SubmitNodeCollector 接口）匹配字节码
 * invokeinterface 的实际 owner，而不是 class_11785（OrderedSubmitNodeCollector，方法声明类）。
 *
 * 灵感来自：https://modrinth.com/mod/shield-statuses
 */
@Mixin(ShieldSpecialRenderer.class)
public class MixinShieldSpecialRenderer
{
    @ModifyArg(
        method = "method_65707",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_11659;method_73494(Lnet/minecraft/class_630;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/class_1921;IILnet/minecraft/class_1058;ZZILnet/minecraft/class_11683$class_11792;I)V",
            remap = false
        ),
        index = 8,
        remap = false
    )
    private int helpfulTweake$dyeShieldColor(int originalColor)
    {
        if (!Configs.Tools.SHIELD_STATUS.getBooleanValue()) return originalColor;

        int color = ShieldRenderContext.getShieldColor();
        if (color == ShieldRenderContext.COLOR_NONE) return originalColor;
        return color;
    }

    @Inject(
        method = "method_65707",
        at = @At("TAIL"),
        remap = false
    )
    private void helpfulTweake$clearShieldColor(CallbackInfo ci)
    {
        ShieldRenderContext.clearShieldColor();
    }
}
