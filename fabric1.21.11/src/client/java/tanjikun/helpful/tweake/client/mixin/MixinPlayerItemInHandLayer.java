package tanjikun.helpful.tweake.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.client.util.ShieldRenderContext;
import tanjikun.helpful.tweake.client.util.ShieldStateTracker;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 第三人称盾牌状态上下文设置。
 *
 * 注入 PlayerItemInHandLayer.submitArmWithItem (method_62594) 的 HEAD，
 * 检查手持物品是否盾牌，用 currentEntityUuid 查找 Player，查询冷却状态，设置 shieldColor。
 *
 * method_62594 签名：(AvatarRenderState, ItemStackRenderState, ItemStack, HumanoidArm, PoseStack, SubmitNodeCollector, int) → void
 *
 * 状态来源：
 * - LocalPlayer（F5视角）：使用真实 ItemCooldowns（服务端同步，精确）
 * - 其他玩家：服务端不同步冷却，使用 ShieldStateTracker（监听 SHIELD_BREAK 声音推测）
 *
 * remap=false: method 用 intermediary 名（项目约定）
 *
 * 灵感来自：https://modrinth.com/mod/shield-statuses
 */
@Mixin(PlayerItemInHandLayer.class)
public class MixinPlayerItemInHandLayer
{
    @Inject(method = "method_62594", at = @At("HEAD"), remap = false)
    private void helpfulTweake$setShieldColorThirdPerson(AvatarRenderState state,
                                                           ItemStackRenderState itemState,
                                                           ItemStack stack, HumanoidArm arm,
                                                           PoseStack poseStack, SubmitNodeCollector collector,
                                                           int packedLight, CallbackInfo ci)
    {
        if (!Configs.Tools.SHIELD_STATUS.getBooleanValue()) return;
        if (!(stack.getItem() instanceof ShieldItem)) return;

        // 1.21.11: AvatarRenderState 继承 EntityRenderState，有 public int id 字段（实体 ID）。
        // extractRenderState 和 submitArmWithItem 不连续调用，ThreadLocal 不可靠。
        // 直接从 render state 的 id 通过 level.getEntity(id) 获取 Player。
        // 普通字段访问会被 Loom remapJar 自动重映射为 intermediary 名。
        if (Minecraft.getInstance().level == null)
        {
            ShieldRenderContext.setShieldColor(ShieldRenderContext.COLOR_AVAILABLE);
            return;
        }

        // state.id 继承自 EntityRenderState（public int id）
        net.minecraft.world.entity.Entity entity = Minecraft.getInstance().level.getEntity(state.id);
        Player player = (entity instanceof Player) ? (Player) entity : null;

        boolean onCooldown;
        if (player == null)
        {
            onCooldown = false;
        }
        else if (player.isLocalPlayer())
        {
            // LocalPlayer（F5视角）：使用真实冷却状态
            onCooldown = player.getCooldowns().isOnCooldown(stack);
        }
        else
        {
            // 其他玩家：服务端不同步 ItemCooldowns 到其他客户端。
            // 双重检测：
            // 1. 姿态变化（主）：破盾时 stopUsingItem() → isBlocking 从 true 变 false
            // 2. 声音监听（辅）：SHIELD_BREAK 声音事件
            java.util.UUID uuid = player.getUUID();
            ShieldStateTracker.checkBlockingChange(uuid, player.isBlocking());
            onCooldown = ShieldStateTracker.isShieldOnCooldown(uuid);
        }

        ShieldRenderContext.setShieldColor(onCooldown
                ? ShieldRenderContext.COLOR_COOLDOWN
                : ShieldRenderContext.COLOR_AVAILABLE);
    }
}
