package tanjikun.helpful.tweake.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.CommonConfigs;

/**
 * 更好的船：注入 Entity.maxUpStep（method_49476），当实体是船且功能开启时
 * 返回配置的抬升高度，使 Entity.move() 中的台阶抬升逻辑对船只生效。
 *
 * 必须注入 Entity 而非 AbstractBoat，因为 maxUpStep 声明在 Entity（class_1297），
 * AbstractBoat 未覆盖此方法。@Inject 可命中目标类自己声明的方法，最可靠。
 *
 * 双端 Mixin：此 Mixin 在 main source set，同时作用于客户端和专用服务器。
 * 配置值通过 CommonConfigs 读取，客户端由 MaLiLib 同步，服务端由 JSON 文件提供。
 *
 * ponytail: 注入 Entity 后每次任何实体调用 maxUpStep 都会执行 instanceof 检查
 * 已知上限: maxUpStep 在 Entity.move 中调用，move 每 tick 调用次数有限（玩家/生物/船），
 *           instanceof 开销极小，可忽略
 * 升级路径: 若性能敏感，可改用 Mixin AbstractBoat + @Shadow + 自定义方法绕过，
 *           但需确认 Mixin 能覆盖继承方法（当前 Loom 配置下 soft override 方法名不重映射，不可行）
 */
@Mixin(Entity.class)
public class MixinEntityBoatStep
{
    @Inject(method = "method_49476", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$boatStep(CallbackInfoReturnable<Float> cir)
    {
        if (CommonConfigs.betterBoat && ((Object) this) instanceof AbstractBoat)
        {
            cir.setReturnValue((float) CommonConfigs.boatLiftHeight);
        }
    }
}
