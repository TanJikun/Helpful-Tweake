package tanjikun.helpful.tweake.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.CommonConfigs;
import tanjikun.helpful.tweake.util.FakePeaceManager;
import tanjikun.helpful.tweake.util.PortalFarmAi;

/**
 * serverAiStep 取消注入，两处共用：
 * - 伪和平优化：敌对生物所在方块内敌对数量超过阈值（70）时冻结
 *   （感知/目标选择器/寻路/brain/移动控制全部跳过），实现无 AI。
 *   生物实体保留，仍计入服务端敌对生物总数判定。
 * - 切门刷怪塔优化：生成时碰撞箱碰到下界/末地传送门方块的生物
 *   带持久冻结附件（见 PortalFarmAi），永久冻结。
 *
 * 取消 serverAiStep 同时意味着 noActionTime 不再增长，被冻结的生物
 * 不会触发闲置消失，堆积状态更稳定。重力/物理不在 serverAiStep 中，
 * 不受取消影响（这正是不用 setNoAi 的原因——1.21.11 中 NoAI 连重力一起停）。
 *
 * 双端 Mixin：此 Mixin 在 main source set，同时作用于客户端（集成服务器）和专用服务器。
 * 配置值通过 CommonConfigs 读取，客户端由 MaLiLib 同步，服务端由 JSON 文件提供。
 *
 * ponytail: Mixin 方法名使用 intermediary（method_6023）而非 named（serverAiStep）
 * 已知上限: 本项目用 officialMojangMappings 但 Loom 1.17 未生成 refmap，导致 Mixin 注解
 *           的 method 字符串无法从 named 重映射到 intermediary。运行时游戏类用 intermediary。
 *           serverAiStep 首次声明于 LivingEntity（class_1309），其 intermediary 名为
 *           method_6023（已在 mappings.tiny 验证归属），Mob（class_1308）中的 override
 *           编译后同名 method_6023（intermediary jar 中 javap 已验证存在）。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名 "serverAiStep"。
 *
 * 本类另承载切门刷怪塔优化（helpfulTweake$portalFarmDisableAi，见方法注释）。
 */
@Mixin(Mob.class)
public class MixinMob
{
    // method_6023 = LivingEntity.serverAiStep()，Mob 中为 protected final void
    // 伪和平：敌对密度超阈值时冻结；切门优化：带 portal_ai_frozen 附件的生物永久冻结
    @Inject(method = "method_6023", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$freezeAi(CallbackInfo ci)
    {
        Mob self = (Mob) (Object) this;
        if (FakePeaceManager.shouldFreezeAi(self) || PortalFarmAi.isFrozen(self))
        {
            ci.cancel();
        }
    }

    /**
     * 切门刷怪塔优化：生物生成流程收尾（finalizeSpawn，实体尚未加入世界、AI 从未 tick）
     * 时登记待检生物，主线程 tick 末检测碰撞箱是否与下界/末地传送门方块相交，相交则
     * 打上持久冻结标记（PortalFarmAi.FROZEN，Fabric 附件随 NBT 存档，区块重载/重启后
     * 仍生效），由 serverAiStep 注入取消 AI。不用 setNoAi：1.21.11 中 NoAI 连重力/物理
     * 一起停（实测生物悬空不坠落）；取消 serverAiStep 则保留重力/击退/受伤/传送门传送
     * 等非 AI 逻辑。
     *
     * method_5943 = Mob.finalizeSpawn(ServerLevelAccessor, DifficultyInstance,
     * EntitySpawnReason, SpawnGroupData)，已在 mappings.tiny 验证为全局唯一的
     * finalizeSpawn，签名与 Mob 的 javap 一致。自然生成（NaturalSpawner）、刷怪笼
     * （BaseSpawner）、下界门猪人与刷怪蛋（EntityType.spawn 全部重载共用内部方法）
     * 都经过此方法；/summon、繁殖等直接生成不走此路径，不受影响。
     *
     * ponytail: 猪人特判（主世界豁免）与碰撞箱检测都在 PortalFarmAi.checkAndFreeze，
     *           按字面语义只豁免"主世界"（dimension == OVERWORLD）的僵尸猪人。
     * 已知上限: 1.21.11 原版猪人门生成判定已改为环境属性 NETHER_PORTAL_SPAWNS_PIGLINS
     *           （数据包/模组可将其扩展到其他维度），若模组维度开启该属性且有猪人门
     *           农场，此处不会豁免该维度的猪人。
     * 升级路径: 如需更精确，改为查询 EnvironmentAttributeSystem 的该环境属性。
     */
    @Inject(method = "method_5943", at = @At("RETURN"), remap = false)
    private void helpfulTweake$portalFarmDisableAi(CallbackInfoReturnable<SpawnGroupData> cir)
    {
        if (!CommonConfigs.portalFarmOptimization)
        {
            return;
        }
        // 只登记引用，严禁在此查方块：finalizeSpawn 可能运行在区块生成线程
        // （自然生成器 spawn 步骤），生成线程上任何 getChunk 系调用（含
        // getBlockState，即使目标区块已加载）都会 supplyAsync 调度回主线程
        // 并 join 阻塞；若主线程恰好在等区块生成 future，双线程互等 =
        // 区块系统死锁（实测主->下界传送即卡死，hasChunkAt 门控防不了
        // 跨线程 join）。检测推迟到主线程 tick 末，冻结最多晚 1 tick。
        PortalFarmAi.schedulePortalCheck((Mob) (Object) this);
    }
}
