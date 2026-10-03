package tanjikun.helpful.tweake.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.util.PortalFarmAi;

/**
 * 切门刷怪塔优化的跨维度冷却注入。
 *
 * 根因：冻结生物保留传送门检测（非 AI 逻辑），而 Mob 的传送等待时间为 0——
 * 生物传送到下界后落在门内不会走开（AI 已冻结），300 tick 冷却一结束就被
 * 下界门传回主世界，又碰门再传回，形成两维度间的无限 ping-pong。每次往返
 * 都强制加载目标区块并把两个维度的门区块标脏，实测导致：下界门内被循环
 * 生物占满后玩家/假人站门内不触发传送、保存 the_nether 时区块持续变化而
 * 卡死无响应。
 *
 * 修复：冻结生物跨维度传送落地后，setPortalCooldown() 会取本方法返回值作
 * 为冷却时长——对带 portal_ai_frozen 附件的 Mob 返回超大冷却，冷却期间
 * setAsInsidePortal 直接跳过，生物不再回传，停留在目标维度（切门塔的预期
 * 行为：主世界门送走、下界侧堆积）。主世界侧新生物不受影响：生成冻结时
 * 未设冷却，首次碰门传送正常。
 *
 * ponytail: 用超大冷却近似"永久"，不引入新状态位。
 * 已知上限: 冷却每 tick 递减（processPortalCooldown 在 >0 时自减），
 *           Integer.MAX_VALUE/4 ≈ 5.4 亿 tick（数年游戏时长）后归零，
 *           理论上会恢复可传送；存档中已有冻结生物最多再跳一次门后自愈。
 * 升级路径: 若需严格永久，可改为 Mixin setAsInsidePortal 头部直接取消。
 *
 * ponytail: Mixin 方法名使用 intermediary（method_5806）而非 named。
 * 已知上限: getDimensionChangingDelay 声明于 Entity（class_1297），无任何
 *           子类 override（mappings.tiny 全局唯一），玩家与冻结生物共用此
 *           方法体，故必须用 instanceof Mob 门控，只改冻结生物的返回值。
 * 升级路径: 同 MixinMob——Loom 生成 refmap 后可改回 named 名。
 *
 * 双端 Mixin：见 MixinMob 类注释。
 */
@Mixin(Entity.class)
public class MixinEntity
{
    // method_5806 = Entity.getDimensionChangingDelay()，public int，
    // setPortalCooldown() 无参版在跨维度传送落地后调用它取冷却时长
    @Inject(method = "method_5806", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$frozenNoReturnPortal(CallbackInfoReturnable<Integer> cir)
    {
        Entity self = (Entity) (Object) this;
        if (self instanceof Mob mob && PortalFarmAi.isFrozen(mob))
        {
            cir.setReturnValue(Integer.MAX_VALUE / 4);
        }
    }
}
