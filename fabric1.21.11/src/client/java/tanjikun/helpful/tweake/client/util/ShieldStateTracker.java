package tanjikun.helpful.tweake.client.util;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 盾牌状态跟踪器：通过监听客户端接收的声音事件，推测其他玩家的盾牌冷却状态。
 *
 * 原理：服务端不会将其他玩家的盾牌冷却同步到客户端，但盾牌被斧头击中禁用时会播放
 * SHIELD_BREAK 声音。客户端监听此声音事件，通过实体 ID 或坐标定位玩家，
 * 本地维护 5 秒（100 tick）冷却计时器。
 *
 * 局限：
 * - 仅在收到 SHIELD_BREAK 声音事件时触发，依赖原版发送该声音
 * - 计时器基于客户端 tick，与服务端实际冷却可能有偏差
 * - 玩家退出渲染范围后状态可能残留（自动清理）
 *
 * 灵感来自：https://modrinth.com/mod/shield-statuses
 */
public class ShieldStateTracker
{
    /** 盾牌冷却时长（tick）= 5 秒 */
    private static final int SHIELD_COOLDOWN_TICKS = 100;

    /** 玩家 UUID → 冷却到期时刻（game time tick） */
    private static final Map<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();

    /**
     * 标记某玩家的盾牌进入冷却（收到 SHIELD_BREAK 声音）。
     */
    public static void markShieldDisabled(UUID playerUuid)
    {
        if (playerUuid == null) return;
        long now = Minecraft.getInstance().level.getGameTime();
        cooldownUntil.put(playerUuid, now + SHIELD_COOLDOWN_TICKS);
    }

    /**
     * 查询某玩家的盾牌是否处于冷却中。
     * LocalPlayer 使用真实的 ItemCooldowns（不经过此 Tracker）。
     */
    public static boolean isShieldOnCooldown(UUID playerUuid)
    {
        if (playerUuid == null) return false;
        Long until = cooldownUntil.get(playerUuid);
        if (until == null) return false;
        long now = Minecraft.getInstance().level.getGameTime();
        if (now >= until)
        {
            cooldownUntil.remove(playerUuid);
            return false;
        }
        return true;
    }

    /**
     * 通过实体 ID 查找玩家并标记盾牌冷却（来自 ClientboundSoundEntityPacket）。
     */
    public static void markShieldDisabledByEntityId(int entityId)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (level.getEntity(entityId) instanceof Player player)
        {
            markShieldDisabled(player.getUUID());
        }
    }

    /**
     * 通过坐标查找最近玩家并标记盾牌冷却（来自 ClientboundSoundPacket）。
     * 半径 4.0 内搜索（声音位置可能偏离玩家中心，留余量防漏判）。
     */
    public static void markShieldDisabledByPosition(double x, double y, double z)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Player nearest = null;
        double nearestDistSq = 16.0; // 半径 4.0 的平方
        for (Player p : level.players())
        {
            double dx = p.getX() - x;
            double dy = p.getY() - y;
            double dz = p.getZ() - z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < nearestDistSq)
            {
                nearestDistSq = distSq;
                nearest = p;
            }
        }
        if (nearest != null)
        {
            markShieldDisabled(nearest.getUUID());
        }
    }

    /**
     * 清理已过期的条目，避免内存泄漏。
     * 建议每秒调用一次。
     */
    public static void cleanup()
    {
        if (Minecraft.getInstance().level == null)
        {
            cooldownUntil.clear();
            return;
        }
        long now = Minecraft.getInstance().level.getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = cooldownUntil.entrySet().iterator();
        while (it.hasNext())
        {
            if (now >= it.next().getValue())
            {
                it.remove();
            }
        }
    }
}
