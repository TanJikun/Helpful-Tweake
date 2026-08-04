package tanjikun.helpful.tweake.client.util;

import java.util.UUID;

/**
 * 盾牌状态渲染上下文：通过 ThreadLocal 在渲染管线中传递盾牌状态。
 *
 * 流程：
 * 1. MixinLivingEntityRenderer.extractRenderState HEAD → 设置 currentEntityUuid
 * 2. MixinItemInHandRenderer.renderArmWithItem HEAD → 设置 shieldColor（第一人称，查询 LocalPlayer cooldowns）
 * 3. MixinPlayerItemInHandLayer.submitArmWithItem HEAD → 设置 shieldColor（第三人称，用 currentEntityUuid 查找 Player）
 * 4. MixinShieldSpecialRenderer.submit TAIL → 读取 shieldColor，叠加颜色层，清理 shieldColor
 *
 * 注意：其他玩家的盾牌冷却状态不会被服务端同步到客户端，因此对其他玩家始终显示为可用（绿色）。
 * 只有 LocalPlayer（自己）的冷却状态是精确的。
 */
public class ShieldRenderContext
{
    public static final int COLOR_NONE = 0;
    public static final int COLOR_AVAILABLE = 0x4000FF00;  // 25% 绿色
    public static final int COLOR_COOLDOWN = 0x40FF0000;   // 25% 红色

    private static final ThreadLocal<UUID> currentEntityUuid = new ThreadLocal<>();
    private static final ThreadLocal<Integer> shieldColor = ThreadLocal.withInitial(() -> COLOR_NONE);

    public static void setCurrentEntityUuid(UUID uuid) { currentEntityUuid.set(uuid); }
    public static UUID getCurrentEntityUuid() { return currentEntityUuid.get(); }

    public static void setShieldColor(int color) { shieldColor.set(color); }
    public static int getShieldColor() { return shieldColor.get(); }
    public static void clearShieldColor() { shieldColor.remove(); }
}
