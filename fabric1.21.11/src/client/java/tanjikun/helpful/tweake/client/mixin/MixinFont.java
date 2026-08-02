package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.client.util.BoldFontManager;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 加粗显示优化：当文字样式为 bold 且 cu_ 字体可用时，
 * 从 bold FontSet 获取 glyph 代替原版 glyph。
 *
 * method_72731 = Font.getGlyph(int, Style) → BakedGlyph
 * remap=false: Loom 1.17 + officialMojangMappings 不生成 refmap，直接用 intermediary 名
 */
@Mixin(Font.class)
public class MixinFont
{
    @Inject(method = "method_72731", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$getBoldGlyph(int codepoint, Style style,
                                             CallbackInfoReturnable<BakedGlyph> cir)
    {
        if (!Configs.Optimization.BOLD_FONT.getBooleanValue()) return;
        if (!style.isBold()) return;

        BakedGlyph boldGlyph = BoldFontManager.getBoldGlyph(codepoint);
        if (boldGlyph != null)
        {
            cir.setReturnValue(boldGlyph);
        }
    }
}
