package tanjikun.helpful.tweake.util;

import com.mojang.serialization.Codec;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import tanjikun.helpful.tweake.HelpfulTweake;
import tanjikun.helpful.tweake.config.CommonConfigs;

/**
 * 切门刷怪塔优化的 AI 冻结标记：Fabric 持久化数据附件，随实体 NBT 存档，
 * 区块重载/重启后仍生效。冻结效果由 MixinMob 的 serverAiStep 取消注入实现。
 *
 * 碰门检测在服务端主线程 tick 末执行（END_SERVER_TICK），而非 finalizeSpawn 现场：
 * finalizeSpawn 可能运行在区块生成线程（自然生成器的 spawn 步骤），生成线程上任何
 * getChunk 系调用（含 getBlockState/getBlockStates——即使目标区块已加载）都会
 * supplyAsync 调度回主线程并 join 阻塞；若主线程恰好在等区块生成 future，双线程
 * 互等 = 区块系统死锁（实测：主->下界传送触发下界生成时服务器与渲染线程全部冻结，
 * 无任何日志与存档写入）。hasChunkAt 门控防不了这种跨线程 join。故 finalizeSpawn
 * 注入只把实体登记进线程安全集合，检测推迟到主线程，冻结最多晚 1 tick，无功能影响。
 *
 * ponytail: 1.21.11 中 setNoAi(true) 会连重力/物理一起停掉（实测生物悬空不坠落），
 *           故不用 NoAi，改用附件标记 + 取消 serverAiStep（与伪和平同机制，
 *           已验证保留重力/击退/受伤，且 noActionTime 不增长、闲置不消失）。
 * 升级路径: 若原版未来提供"仅停 AI 保留物理"的原生开关，可弃用本附件直接用它。
 */
public final class PortalFarmAi
{
    private PortalFarmAi() {}

    public static final AttachmentType<Boolean> FROZEN =
            AttachmentRegistry.createPersistent(HelpfulTweake.id("portal_ai_frozen"), Codec.BOOL);

    /**
     * finalizeSpawn（可能在区块生成线程调用）登记的待检生物。
     * 并发集合：生成线程 add，主线程 tick 末 removeIf 处理（弱一致迭代下
     * 迭代中并发 add 的元素留到下个 tick，不会漏检）。
     */
    private static final Set<Mob> PENDING = ConcurrentHashMap.newKeySet();

    /** 在 onInitialize 调用一次，触发类加载以完成附件注册（须在世界加载/实体反序列化前）。 */
    public static void init() {}

    /** finalizeSpawn 注入点调用：只登记引用，严禁在此（生成线程）查询方块。 */
    public static void schedulePortalCheck(Mob mob)
    {
        PENDING.add(mob);
    }

    /** 服务端每 tick 末调用（END_SERVER_TICK，主线程）：统一检测待检生物是否碰门并冻结。 */
    public static void onEndServerTick(MinecraftServer server)
    {
        if (PENDING.isEmpty())
        {
            return;
        }
        if (!CommonConfigs.portalFarmOptimization)
        {
            PENDING.clear();
            return;
        }
        PENDING.removeIf(PortalFarmAi::checkAndFreeze);
    }

    private static boolean checkAndFreeze(Mob mob)
    {
        if (!mob.isRemoved() && mob.level() instanceof ServerLevel serverLevel
                // 特判：主世界生成的僵尸猪人（基于下界门生成猪人的经验农场依赖其 AI）
                && !(mob instanceof ZombifiedPiglin && serverLevel.dimension().equals(Level.OVERWORLD)))
        {
            AABB bb = mob.getBoundingBox();
            BlockPos minPos = BlockPos.containing(bb.minX, bb.minY, bb.minZ);
            BlockPos maxPos = BlockPos.containing(bb.maxX, bb.minY, bb.maxZ);
            // hasChunkAt（getChunkNow）只查已加载区块，不触发加载；主线程的
            // getBlockStates 即使对未加载区块也只会同步等待（managedBlock 推进
            // 生成队列，无双线程互等），此处门控只是避免无谓的同步加载开销。
            if (serverLevel.hasChunkAt(minPos) && serverLevel.hasChunkAt(maxPos)
                    && serverLevel.hasChunkAt(new BlockPos(minPos.getX(), minPos.getY(), maxPos.getZ()))
                    && serverLevel.hasChunkAt(new BlockPos(maxPos.getX(), minPos.getY(), minPos.getZ()))
                    && serverLevel.getBlockStates(bb)
                            .anyMatch(state -> state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)))
            {
                freeze(mob);
            }
        }
        return true;
    }

    public static void freeze(Mob mob)
    {
        mob.setAttached(FROZEN, Boolean.TRUE);
    }

    public static boolean isFrozen(Mob mob)
    {
        return Boolean.TRUE.equals(mob.getAttached(FROZEN));
    }
}
