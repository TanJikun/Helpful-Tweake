package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 容器信号输出显示 — 在容器界面标题右侧追加比较器信号强度。
 *
 * 原版 renderLabels 用 0xFF404040 在 (titleLabelX, titleLabelY) 绘制容器名，
 * 本功能在其后追加 " (信号强度)"（容器名+空格+英文括号+数字），颜色一致。
 *
 * 信号计算直接复用原版静态方法 AbstractContainerMenu.getRedstoneSignalFromContainer
 * （即服务端比较器读取容器的同一公式），作用于客户端菜单的容器实例，
 * 其内容随槽位同步包实时更新，因此 GUI 内物品变动时数字实时刷新。
 *
 * 菜单白名单：箱子/陷阱箱/木桶（ChestMenu）、潜影盒（ShulkerBoxMenu）、
 * 漏斗（HopperMenu）、发射器/投掷器（DispenserMenu）、熔炉/高炉/烟熏炉
 * （AbstractFurnaceMenu）、酿造台（BrewingStandMenu）。
 * 白名单外的界面（合成台/铁砧/附魔台/村民等无比较器输出的方块）不显示，
 * 避免出现误导性数字。
 *
 * ponytail: @Inject/@Shadow 用 intermediary + remap=false（项目无 refmap）；
 *   renderLabels = method_2388（AbstractContainerScreen/class_465 唯一，无重载），
 *   titleLabelX = field_25267、titleLabelY = field_25268（经 aliases 匹配）。
 *   简化：讲台等少数比较器逻辑与容器填充度无关的界面未特判（白名单已排除），
 *   mod 自定义容器菜单类不在白名单内，需要时再扩充。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreenSignal
{
    @Shadow(aliases = {"field_25267"}, remap = false)
    protected int titleLabelX;

    @Shadow(aliases = {"field_25268"}, remap = false)
    protected int titleLabelY;

    @Inject(method = "method_2388", at = @At("TAIL"), remap = false)
    private void helpfulTweake$renderContainerSignal(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci)
    {
        if (!Configs.Tools.CONTAINER_SIGNAL_DISPLAY.getBooleanValue())
        {
            return;
        }
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerMenu menu = self.getMenu();
        if (!(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof HopperMenu
                || menu instanceof DispenserMenu || menu instanceof BrewingStandMenu
                || menu instanceof AbstractFurnaceMenu))
        {
            return;
        }
        // 容器槽位在玩家物品栏槽位之前，取第一个非玩家物品栏的容器实例
        Container container = null;
        for (Slot slot : menu.slots)
        {
            if (!(slot.container instanceof Inventory))
            {
                container = slot.container;
                break;
            }
        }
        if (container == null)
        {
            return;
        }
        int signal = AbstractContainerMenu.getRedstoneSignalFromContainer(container);
        guiGraphics.drawString(self.getFont(), " (" + signal + ")",
                this.titleLabelX + self.getFont().width(self.getTitle()),
                this.titleLabelY, 0xFF404040, false);
    }
}
