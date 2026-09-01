package tanjikun.helpful.tweake.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 禁用B36渲染：开启后不渲染移动中的活塞技术方块（B36）。
 *
 * 1.21.11 中 B36（PistonMovingBlockEntity/class_2669）由 PistonHeadRenderer
 * （class_835）渲染，moving_piston 方块本身的模型为空，可见部分全部经
 * submit(PistonHeadRenderState, ...)（method_3576）绘制。
 * 在 HEAD 取消该方法即可完全跳过 B36 渲染：
 * 活塞伸缩动画期间被移动的方块不可见，伸缩结束后方块恢复正常显示。
 *
 * ponytail: Mixin 方法名使用 intermediary + remap = false（项目无 refmap，详见 project_memory）
 */
@Mixin(PistonHeadRenderer.class)
public class MixinPistonHeadRenderer
{
    @Inject(method = "method_3576", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$disableB36Render(PistonHeadRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci)
    {
        if (Configs.Optimization.DISABLE_B36_RENDER.getBooleanValue())
        {
            ci.cancel();
        }
    }
}
