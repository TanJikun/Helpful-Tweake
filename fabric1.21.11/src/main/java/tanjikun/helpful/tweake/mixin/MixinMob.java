package tanjikun.helpful.tweake.mixin;

import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.util.FakePeaceManager;

/**
 * 伪和平优化：敌对生物所在方块内敌对数量超过阈值（70）时，
 * 取消 serverAiStep（感知/目标选择器/寻路/brain/移动控制全部跳过），
 * 实现无 AI。生物实体保留，仍计入服务端敌对生物总数判定。
 *
 * 取消 serverAiStep 同时意味着 noActionTime 不再增长，被冻结的生物
 * 不会触发闲置消失，堆积状态更稳定。
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
 */
@Mixin(Mob.class)
public class MixinMob
{
    // method_6023 = LivingEntity.serverAiStep()，Mob 中为 protected final void
    @Inject(method = "method_6023", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$fakePeacefulFreezeAi(CallbackInfo ci)
    {
        if (FakePeaceManager.shouldFreezeAi((Mob) (Object) this))
        {
            ci.cancel();
        }
    }
}
