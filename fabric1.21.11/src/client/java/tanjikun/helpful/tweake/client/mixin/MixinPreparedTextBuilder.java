package tanjikun.helpful.tweake.client.mixin;

import com.mojang.blaze3d.font.GlyphInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import tanjikun.helpful.tweake.client.util.BoldFontManager;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 加粗显示优化：在 Font$PreparedTextBuilder.accept 中拦截 GlyphInfo.getBoldOffset() 调用，
 * 有自定义 TrueType 字体时返回 0.0f，禁用原版 1 像素水平偏移。
 *
 * 替代 MixinGlyphInfo（对接口 default 方法的注入可能存在运行时限制）。
 * 注入到普通类的普通方法更可靠。
 *
 * 从字节码分析，accept(method_72733) 内部调用链：
 *   info.getBoldOffset() → boldOffset → createGlyph(..., boldOffset, ...)
 *
 * method_72733 = Font$PreparedTextBuilder.accept(int, Style, BakedGlyph) → boolean
 *   内部调用 fwp.a:()F（GlyphInfo.getBoldOffset）
 *
 * PreparedTextBuilder 是 Font 的 private 内部类，用 @Mixin(targets=...) 引用。
 * targets 使用 named 类名，Loom 在 remapJar 时映射为 intermediary。
 *
 * remap=false: method 和 target 都用 intermediary 名，不生成 refmap
 */
@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public class MixinPreparedTextBuilder
{
    @Redirect(
        method = "method_72733",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/class_379;method_16799()F"),
        remap = false
    )
    private float helpfulTweake$disableBoldOffset(GlyphInfo info)
    {
        if (Configs.Optimization.BOLD_FONT.getBooleanValue()
                && BoldFontManager.shouldDisableBoldOffset())
        {
            return 0.0f;
        }
        return info.getBoldOffset();
    }
}
