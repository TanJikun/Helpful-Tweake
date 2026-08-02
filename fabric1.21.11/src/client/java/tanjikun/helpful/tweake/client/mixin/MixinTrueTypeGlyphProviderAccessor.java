package tanjikun.helpful.tweake.client.mixin;

import com.mojang.blaze3d.font.GlyphProvider;

import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.server.packs.resources.ResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @Invoker 访问 TrueTypeGlyphProviderDefinition 的 private load 方法。
 * 用于加载 cu_ 前缀的 ttf 字体文件为 GlyphProvider。
 *
 * method_51759 = TrueTypeGlyphProviderDefinition.load(ResourceManager) → GlyphProvider
 * remap=false: Loom 1.17 + officialMojangMappings 不生成 refmap，直接用 intermediary 名
 */
@Mixin(TrueTypeGlyphProviderDefinition.class)
public interface MixinTrueTypeGlyphProviderAccessor
{
    @Invoker("method_51759")
    GlyphProvider invokeLoad(ResourceManager resourceManager);
}
