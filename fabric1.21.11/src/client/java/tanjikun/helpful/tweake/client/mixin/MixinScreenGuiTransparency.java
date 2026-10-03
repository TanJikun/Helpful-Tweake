package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import tanjikun.helpful.tweake.client.util.GuiTransparencyState;
import tanjikun.helpful.tweake.config.Configs;

/**
 * GUI 界面透明度修改 — 容器界面背景半透明黑色重绘。
 *
 * GUI 内容缩放的作用域开关见 MixinAbstractContainerScreenTransparency
 * （挂 renderContents/method_71085，仅原版容器内容绘制阶段，
 * REI/MaLiLib 等模组 overlay 不受影响），本类只负责背景。
 *
 * 背景重绘：renderTransparentBackground（method_52752）原版以
 * fillGradient(0,0,w,h, 0xC0101010, 0xD0101010) 绘制 75%~82% 的黑色渐变。
 * 主开关开启且当前屏幕是容器界面（AbstractContainerScreen：箱子/工作方块/
 * 交易等）时统一 cancel 原版绘制并由本处接管：
 *   - 75%（默认）= 忠实重绘原版渐变，效果与原版完全一致；
 *   - 其他值 = 以 (percent*255/100 &lt;&lt; 24) | 0x101010 统一纯色填充；
 *   - 0% = 不绘制（背景完全消失）。
 * 绘制期间挂起缩放标志（setSuspended），确保背景只受背景子配置控制，
 * 不会被 GUI 不透明度二次缩放。非容器界面（ESC 菜单、模组界面等）
 * 不接管，保持原版背景。
 *
 * ponytail: @Inject 用 intermediary + remap=false（项目无 refmap）；
 *   renderTransparentBackground = method_52752，声明于 Screen（class_437）
 *   自身，mappings.tiny 已验证；类挂 Screen 是因为目标方法声明于 Screen，
 *   容器判定用 instanceof 完成。
 */
@Mixin(Screen.class)
public abstract class MixinScreenGuiTransparency
{
    @Inject(method = "method_52752", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$backgroundOpacity(GuiGraphics guiGraphics, CallbackInfo ci)
    {
        if (!Configs.Tools.GUI_TRANSPARENCY.getBooleanValue()
                || (Object) this instanceof AbstractContainerScreen == false)
        {
            return;
        }
        // 主开关开启时背景统一由此处接管，挂起缩放标志后自行绘制
        ci.cancel();
        int percent = Configs.Tools.GUI_BACKGROUND_OPACITY.getIntegerValue();
        if (percent <= 0)
        {
            return;
        }
        boolean old = GuiTransparencyState.setSuspended(true);
        if (percent == 75)
        {
            // 默认值：忠实重绘原版 75%~82% 上下渐变
            guiGraphics.fillGradient(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(),
                    0xC0101010, 0xD0101010);
        }
        else
        {
            int alpha = percent * 255 / 100;
            guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(),
                    (alpha << 24) | 0x00101010);
        }
        GuiTransparencyState.setSuspended(old);
    }
}
