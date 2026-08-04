package tanjikun.helpful.tweake.client.util;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 盾牌状态跟踪器：推测其他玩家的盾牌冷却状态。
 *
 * 双重检测机制：
 * 1. 姿态变化（主）：破盾时 BlocksAttacks.disable() 调用 stopUsingItem()，
 *    玩家 isBlocking() 从 true 变 false。这个姿态变化通过 EntityData 同步到
 *    所有客户端，比声音更可靠。检测到变化后标记 5 秒（100 tick）冷却。
 * 2. 声音监听（辅）：SHIELD_BREAK 声音事件，作为补充信号。
 *
 * 局限：
 * - 姿态变化无法区分"被破盾"和"主动松开右键"，有误判（持续 5 秒后自动恢复）
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

    /** 玩家 UUID → 上一帧的 isBlocking 状态（姿态变化检测用） */
    private static final Map<UUID, Boolean> lastBlockingState = new ConcurrentHashMap<>();

    /**
     * 标记某玩家的盾牌进入冷却。
     */
    public static void markShieldDisabled(UUID playerUuid)
    {
        if (playerUuid == null) return;
        if (Minecraft.getInstance().level == null) return;
        long now = Minecraft.getInstance().level.getGameTime();
        cooldownUntil.put(playerUuid, now + SHIELD_COOLDOWN_TICKS);
    }

    /**
     * 检测姿态变化：玩家从举盾（isBlocking=true）变为不举盾（isBlocking=false）时，
     * 推测盾牌被破。每次渲染其他玩家时调用。
     */
    public static void checkBlockingChange(UUID playerUuid, boolean currentlyBlocking)
    {
        if (playerUuid == null) return;
        Boolean last = lastBlockingState.get(playerUuid);
        if (last != null && last && !currentlyBlocking)
        {
            // 从举盾变不举盾：可能是被破盾
            markShieldDisabled(playerUuid);
        }
        lastBlockingState.put(playerUuid, currentlyBlocking);
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
            lastBlockingState.clear();
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
        // 清理不在当前世界的玩家的姿态记录
        lastBlockingState.keySet().removeIf(uuid ->
        {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return true;
            for (Player p : level.players())
            {
                if (p.getUUID().equals(uuid)) return false;
            }
            return true;
        });
    }
}
