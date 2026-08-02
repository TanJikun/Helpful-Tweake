package tanjikun.helpful.tweake.client.mixin;

import com.mojang.blaze3d.font.GlyphInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.client.util.BoldFontManager;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 加粗显示优化：禁用原版 bold 的1像素水平偏移。
 *
 * 原版 bold 渲染 = 同一 glyph 偏移1像素再画一次（getBoldOffset 返回 1.0f）。
 * 使用高清字体包时这会导致加粗文字错位。返回 0.0f 可完全禁用偏移。
 *
 * 仅当有自定义 TrueType 字体时禁用（无自定义字体时沿用原版）。
 *
 * method_16799 = GlyphInfo.getBoldOffset() → float（default 方法，返回 1.0f）
 * remap=false: Loom 1.17 + officialMojangMappings 不生成 refmap，直接用 intermediary 名
 */
@Mixin(GlyphInfo.class)
public interface MixinGlyphInfo
{
    @Inject(method = "method_16799", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$disableBoldOffset(CallbackInfoReturnable<Float> cir)
    {
        if (Configs.Optimization.BOLD_FONT.getBooleanValue()
                && BoldFontManager.shouldDisableBoldOffset())
        {
            cir.setReturnValue(0.0f);
        }
    }
}
