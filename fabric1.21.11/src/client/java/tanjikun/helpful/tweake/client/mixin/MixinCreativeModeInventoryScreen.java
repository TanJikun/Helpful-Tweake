package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 创造 Shift 移入快捷栏 — 拦截创造物品栏物品标签页的 shift+左键。
 *
 * 原版行为：物品标签页中 shift+左键把一组物品拿取到光标上（与中键复制一组重复）。
 * 本功能：开启后改为直接把 1 个物品放入快捷栏（优先补入已有同类未满叠，
 * 没有则放入空位），不再经过光标。
 *
 * 服务器同步：CreativeInventoryListener 挂在 inventoryMenu 上，
 * broadcastChanges() 检测到槽位变化后自动发送 ServerboundSetCreativeModeSlotPacket，
 * 与原版数字键换位（SWAP）分支的处理方式一致。
 *
 * 范围限定：isCreativeSlot（slot.container == CONTAINER）只命中物品标签页的
 * 物品格子，物品栏标签页（SlotWrapper）、销毁槽、普通容器均不受影响。
 *
 * ponytail: @Inject/@Shadow 用 intermediary + remap=false（项目无 refmap）；
 *   slotClicked 声明于 AbstractContainerScreen（覆盖方法须用父类 intermediary 名
 *   method_2383），isCreativeSlot 声明于本类（method_2470，经 aliases 匹配）；
 *   跳过 onMouseClickAction（仅输入遥测统计，无用户可见影响）
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeModeInventoryScreen
{
    @Shadow(aliases = {"method_2470"}, remap = false)
    protected abstract boolean isCreativeSlot(Slot slot);

    @Inject(method = "method_2383", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$shiftClickToHotbar(Slot slot, int slotId, int mouseButton,
                                                  ClickType type, CallbackInfo ci)
    {
        if (!Configs.Tools.CREATIVE_SHIFT_TO_HOTBAR.getBooleanValue()
                || type != ClickType.QUICK_MOVE || mouseButton != 0
                || slot == null || !slot.hasItem() || !isCreativeSlot(slot))
        {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
        {
            return;
        }
        // 移动 1 个：优先补入快捷栏已有的同类未满叠，没有则放入第一个空位
        ItemStack proto = slot.getItem();
        Inventory inv = player.getInventory();
        boolean placed = false;
        for (int i = 0; i < 9 && !placed; i++)
        {
            ItemStack cur = inv.getItem(i);
            if (!cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, proto)
                    && cur.getCount() < cur.getMaxStackSize())
            {
                cur.grow(1);
                placed = true;
            }
        }
        for (int i = 0; i < 9 && !placed; i++)
        {
            if (inv.getItem(i).isEmpty())
            {
                inv.setItem(i, proto.copyWithCount(1));
                placed = true;
            }
        }
        player.inventoryMenu.broadcastChanges();
        ci.cancel();
    }
}
