package tanjikun.helpful.tweake.client.mixin;

import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.font.GlyphProvider;

import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @Invoker 访问 FontManager 的 private createFontSet 方法。
 * 用于创建 bold 专用 FontSet（加载 cu_ 前缀字体文件）。
 *
 * method_72785 = FontManager.createFontSet(Identifier, List<GlyphProvider$Conditional>, Set<FontOption>)
 * remap=false: Loom 1.17 + officialMojangMappings 不生成 refmap，直接用 intermediary 名
 */
@Mixin(FontManager.class)
public interface MixinFontManagerAccessor
{
    @Invoker("method_72785")
    FontSet invokeCreateFontSet(Identifier id, List<GlyphProvider.Conditional> providers, Set<FontOption> options);
}
