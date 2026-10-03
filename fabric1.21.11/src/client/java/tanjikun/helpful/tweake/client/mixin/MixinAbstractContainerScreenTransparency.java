package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import tanjikun.helpful.tweake.client.util.GuiTransparencyState;
import tanjikun.helpful.tweake.config.Configs;

/**
 * GUI 界面透明度修改 — 缩放作用域开关（仅容器背景纹理 renderBg 调用期间）。
 *
 * 实测 Fabric 的 beforeRender/afterRender 事件（REI 搜索框等模组 overlay
 * 的挂载点）位于 renderContents（method_71085）内部，若在整个方法头尾
 * 开关缩放标志仍会波及模组组件，因此改为只覆盖原版背景纹理绘制：
 * renderBackground（method_25420）是 renderBg（method_2389，GUI 窗口贴图，
 * 原版子类才实现）的直接调用者，在其对 renderBg 的唯一 invoke 调用点
 * BEFORE/AFTER 开关缩放标志：
 *   - 仅容器背景纹理（含子类画的多层贴图，如附魔台）被缩放；
 *   - REI 等模组 overlay、悬停高亮、tooltip、物品图标、文字均在
 *     renderBg 之外，保持原版不透明。
 * 仅在主开关开启且 GUI 不透明度 &lt; 100 时打开。
 *
 * ponytail: @Inject/@At target 用 intermediary + remap=false（项目无
 *   refmap）。method_25420 声明于 Screen（class_437，obf b），容器类
 *   （class_465，obf gti）以 obf 同名覆盖复用该 intermediary 名
 *   （obf jar javap 实证 gti.b 存在，tiny 中子类条目因映射相同被省略）；
 *   renderBg = method_2389（obf a，签名 (Lgir/class_332;FII)V）。注意：
 *   @At target 的描述符必须用运行时（intermediary）JVM 描述符
 *   (Lnet/minecraft/class_332;FII)V——mappings.tiny 行内 desc 用的
 *   是 obf 命名空间短名（gir），直接照抄会导致 Scanned 0 targets
 *   启动崩溃（实测）。INVOKE 定位依赖运行时字节码，MC 版本更新
 *   若 renderBackground 不再直调 renderBg，defaultRequire 会在启动
 *   时崩溃报错，需同步 target。
 *   简化：悬停高亮/tooltip 不透明（可读性反而更好），物品图标与文字
 *   走独立提交路径本就不受缩放，如需一并透明需另挂路径。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreenTransparency
{
    // renderBackground 内部对 renderBg 的唯一调用点前后开关缩放标志
    @Inject(method = "method_25420",
            at = @At(value = "INVOKE", target = "method_2389(Lnet/minecraft/class_332;FII)V", remap = false),
            remap = false)
    private void helpfulTweake$beginBgTransparency(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                                    float partialTick, CallbackInfo ci)
    {
        if (Configs.Tools.GUI_TRANSPARENCY.getBooleanValue()
                && Configs.Tools.GUI_OPACITY.getIntegerValue() < 100)
        {
            GuiTransparencyState.setInScreenRender(true);
        }
    }

    @Inject(method = "method_25420",
            at = @At(value = "INVOKE", target = "method_2389(Lnet/minecraft/class_332;FII)V", remap = false,
                     shift = At.Shift.AFTER),
            remap = false)
    private void helpfulTweake$endBgTransparency(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                                  float partialTick, CallbackInfo ci)
    {
        GuiTransparencyState.setInScreenRender(false);
    }
}
