package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.client.util.ZoomState;

/**
 * 放大镜：按住按键时缩小视场角（FOV），画面等效放大。
 *
 * 注入 GameRenderer.getFov (method_3196) 的 RETURN，
 * 将原视场角除以 ZoomState 当前渲染倍数（含过渡动画插值）。
 *
 * method_3196 签名：(Camera, float, boolean) → float
 * （Camera = class_4184/ger）
 *
 * remap=false: method 用 intermediary 名（项目约定，
 * Loom 1.17 + officialMojangMappings 不生成 refmap）
 *
 * 灵感来自：https://modrinth.com/mod/zoomify
 */
@Mixin(GameRenderer.class)
public class MixinGameRendererZoom
{
    @Inject(method = "method_3196", at = @At("RETURN"), remap = false, cancellable = true)
    private void helpfulTweake$applyZoom(Camera camera, float partialTick, boolean useFovSetting,
                                         CallbackInfoReturnable<Float> cir)
    {
        double zoom = ZoomState.getRenderZoom();
        if (zoom > 1.0001)
        {
            cir.setReturnValue((float) (cir.getReturnValueF() / zoom));
        }
    }
}
