package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;

import tanjikun.helpful.tweake.client.util.GuiTransparencyState;
import tanjikun.helpful.tweake.config.Configs;

/**
 * GUI 界面透明度修改 — 在 GuiGraphics 的两个绘制汇聚点按配置缩放颜色 alpha。
 *
 * 1.21.11 的 GUI 渲染（RenderPipeline 体系）中，所有纹理 blit 最终进入
 * submitBlit（method_70847，最后一个 int color 参数），所有纯色/渐变填充
 * 最终进入 submitColoredRectangle（method_70848，int color + Integer colorTo），
 * 颜色以 ARGB 形式传入并烘进顶点，两处是带颜色的唯一汇聚点。
 *
 * 在两处 HEAD 拦截：缩放标志激活时 cancel 原调用，把 color 的 alpha
 * 按百分比缩小后经 @Shadow 重放（suspended 标志防止重放再次进入处理器）。
 * 文字（glyph 渲染态）不经过这两处，保持不透明以保证可读性。
 *
 * ponytail: @Shadow/@Inject 用 intermediary + remap=false（项目无 refmap），
 *   均为 GuiGraphics（class_332）内唯一方法，无重载歧义，mappings.tiny 已验证。
 *   简化：物品图标可能走独立的 item-atlas 提交路径而不经过 submitBlit，
 *   保持不透明；如需一并透明，需另在物品渲染路径挂钩，本功能不做。
 */
@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphicsTransparency
{
    @Shadow(aliases = {"method_70847"}, remap = false)
    private void submitBlit(RenderPipeline pipeline, GpuTextureView texture, GpuSampler sampler,
                            int x1, int y1, int x2, int y2, float u, float v, float u2, float v2, int color)
    {
    }

    @Shadow(aliases = {"method_70848"}, remap = false)
    private void submitColoredRectangle(RenderPipeline pipeline, TextureSetup textures,
                                        int x1, int y1, int x2, int y2, int color, Integer colorTo)
    {
    }

    @Inject(method = "method_70847", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$blitOpacity(RenderPipeline pipeline, GpuTextureView texture, GpuSampler sampler,
                                            int x1, int y1, int x2, int y2, float u, float v, float u2, float v2,
                                            int color, CallbackInfo ci)
    {
        if (!GuiTransparencyState.isGuiOpacityActive())
        {
            return;
        }
        ci.cancel();
        int percent = Configs.Tools.GUI_OPACITY.getIntegerValue();
        boolean old = GuiTransparencyState.setSuspended(true);
        this.submitBlit(pipeline, texture, sampler, x1, y1, x2, y2, u, v, u2, v2,
                GuiTransparencyState.scaleAlpha(color, percent));
        GuiTransparencyState.setSuspended(old);
    }

    @Inject(method = "method_70848", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$fillOpacity(RenderPipeline pipeline, TextureSetup textures,
                                           int x1, int y1, int x2, int y2, int color, Integer colorTo,
                                           CallbackInfo ci)
    {
        if (!GuiTransparencyState.isGuiOpacityActive())
        {
            return;
        }
        ci.cancel();
        int percent = Configs.Tools.GUI_OPACITY.getIntegerValue();
        Integer scaledTo = colorTo == null ? null : GuiTransparencyState.scaleAlpha(colorTo, percent);
        boolean old = GuiTransparencyState.setSuspended(true);
        this.submitColoredRectangle(pipeline, textures, x1, y1, x2, y2,
                GuiTransparencyState.scaleAlpha(color, percent), scaledTo);
        GuiTransparencyState.setSuspended(old);
    }
}
