package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.client.util.ZoomState;

/**
 * 放大镜：放大期间（按住按键）滚轮用于调整放大倍数，
 * 取消原版滚轮事件（游戏内滚轮原用于切换快捷栏）。
 *
 * 注入 MouseHandler.onScroll (method_1598) 的 HEAD，
 * 签名：(long window, double horizontal, double vertical) → void。
 *
 * remap=false: method 用 intermediary 名（项目约定）
 *
 * 灵感来自：https://modrinth.com/mod/zoomify
 */
@Mixin(MouseHandler.class)
public class MixinMouseHandlerZoom
{
    @Inject(method = "method_1598", at = @At("HEAD"), remap = false, cancellable = true)
    private void helpfulTweake$zoomScroll(long window, double horizontal, double vertical,
                                          CallbackInfo ci)
    {
        // 界面打开时不拦截，滚轮交给当前界面（如物品栏滚动）
        if (Minecraft.getInstance().screen != null)
        {
            return;
        }
        if (ZoomState.onScroll(vertical))
        {
            ci.cancel();
        }
    }
}
