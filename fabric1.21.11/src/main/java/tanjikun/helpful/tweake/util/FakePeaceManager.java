package tanjikun.helpful.tweake.util;

import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;

import tanjikun.helpful.tweake.config.CommonConfigs;

/**
 * 伪和平优化：同一方块内敌对生物数量超过阈值（70）时，移除该方块内所有敌对生物的 AI。
 *
 * 实现方式：无状态拦截。服务端每秒扫描一次各维度敌对生物的方块坐标分布，
 * 把超过阈值的方块记入 FROZEN_BLOCKS；MixinMob 拦截 Mob.serverAiStep 时查询
 * 该集合决定是否取消。不设置 NoAi 标志、不修改实体数据、不写盘，
 * 因此功能关闭或数量回落到阈值及以下后 AI 自动恢复。
 *
 * 生物实体本身保留，服务端对敌对生物总数的判定（刷怪上限计算）不受影响，
 * 伪和平效果（占满敌对刷怪上限）依然成立。
 *
 * 坚守者例外（子配置"坚守者听声音不钻地"）：
 * - 振动监听在 Warden.tick() 中执行而非 serverAiStep，被冻结的坚守者天然仍能听到
 *   声音并刷新钻地冷却（永不钻地），但 brain 不再 tick，"听到声音寻路/愤怒"等
 *   其他机制同步失效。
 * - 子配置关闭时，MixinWardenVibrationUser 拦截 canReceiveVibration 使其完全失聪
 *   （等价于原版 NoAi 坚守者的听不到声音行为）。
 *
 * ponytail: 扫描间隔 20 tick（1 秒），冻结状态最多滞后 1 秒生效/恢复；
 *           计数含所有已加载区块实体，敌对生物数万级时每秒一次全量遍历仍是可接受的
 *           开销。若未来需要更精细，可改为按区块增量维护。
 * 升级路径: 阈值/间隔如需可配置，加 ConfigInteger 即可。
 */
public class FakePeaceManager
{
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int HOSTILE_COUNT_THRESHOLD = 70;

    /** 每维度"已冻结"方块集合（BlockPos.asLong()）。仅服务端线程读写。 */
    private static final Map<ResourceKey<Level>, LongOpenHashSet> FROZEN_BLOCKS = new HashMap<>();

    /**
     * 服务端每 tick 调用（END_SERVER_TICK），每 SCAN_INTERVAL_TICKS 扫描一次。
     */
    public static void onEndServerTick(MinecraftServer server)
    {
        if (!CommonConfigs.fakePeaceful)
        {
            if (!FROZEN_BLOCKS.isEmpty())
            {
                FROZEN_BLOCKS.clear();
            }
            return;
        }
        if (server.getTickCount() % SCAN_INTERVAL_TICKS != 0)
        {
            return;
        }

        FROZEN_BLOCKS.clear();
        for (ServerLevel level : server.getAllLevels())
        {
            Long2IntOpenHashMap counts = new Long2IntOpenHashMap();
            for (Entity entity : level.getAllEntities())
            {
                if (entity instanceof Enemy && entity.isAlive())
                {
                    counts.addTo(entity.blockPosition().asLong(), 1);
                }
            }

            LongOpenHashSet frozen = null;
            for (var entry : counts.long2IntEntrySet())
            {
                if (entry.getIntValue() > HOSTILE_COUNT_THRESHOLD)
                {
                    if (frozen == null)
                    {
                        frozen = new LongOpenHashSet();
                    }
                    frozen.add(entry.getLongKey());
                }
            }
            if (frozen != null)
            {
                FROZEN_BLOCKS.put(level.dimension(), frozen);
            }
        }
    }

    /**
     * 该敌对生物的 AI 是否应被冻结（Mob.serverAiStep 拦截点调用）。
     */
    public static boolean shouldFreezeAi(Mob mob)
    {
        if (!CommonConfigs.fakePeaceful || !(mob instanceof Enemy))
        {
            return false;
        }
        LongOpenHashSet frozen = FROZEN_BLOCKS.get(mob.level().dimension());
        return frozen != null && frozen.contains(mob.blockPosition().asLong());
    }

    /**
     * 被冻结的坚守者是否应失聪（Warden$VibrationUser.canReceiveVibration 拦截点调用）。
     * 仅在子配置"坚守者听声音不钻地"关闭时失聪。
     */
    public static boolean shouldDeafenWarden(Warden warden)
    {
        return !CommonConfigs.fakePeacefulWardenHearing && shouldFreezeAi(warden);
    }
}
