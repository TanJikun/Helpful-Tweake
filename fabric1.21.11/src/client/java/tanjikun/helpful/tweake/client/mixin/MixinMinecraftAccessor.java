package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @Accessor 访问 Minecraft.fontManager（private 字段）。
 * 用于在 BoldFontManager 中获取 FontManager 实例，进而调用 createFontSet。
 *
 * Loom 1.17 + officialMojangMappings 会生成 refmap，named 名 fontManager
 * 在 dev（named）与生产（obfuscated）环境均能正确映射。
 */
@Mixin(Minecraft.class)
public interface MixinMinecraftAccessor
{
    @Accessor("fontManager")
    FontManager getFontManager();
}
