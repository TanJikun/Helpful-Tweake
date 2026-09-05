package tanjikun.helpful.tweake.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.gameevent.GameEvent;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.util.FakePeaceManager;

/**
 * 伪和平优化子配置"坚守者听声音不钻地"关闭时：
 * 被冻结 AI 的坚守者不再接收任何振动（完全失聪），
 * 与原版 NoAi 坚守者"听不到声音"的行为一致。
 *
 * 原版 Warden$VibrationUser.canReceiveVibration 首个检查就是 Warden.isNoAi()，
 * 即 NoAi 坚守者本就失聪；本 Mixin 在不设置 NoAi 标志（无状态冻结）的前提下
 * 提供等价的失聪效果。子配置开启时本 Mixin 不拦截，被冻结的坚守者仍能听到
 * 声音并刷新钻地冷却（该机制在 Warden.tick() 中执行，不属于 AI，不受冻结影响），
 * 而"听到声音寻路/愤怒"等其他机制随 brain 冻结一并失效。
 *
 * ponytail: Mixin 类与方法名均使用 intermediary（class_7260$class_8507 / method_32970）
 * 已知上限: 无 refmap 环境下 targets 字符串无法从 named 重映射，必须直接写生产环境
 *           intermediary 类名。目标类为私有内部类，无法用类引用（.class）形式声明。
 *           method_32970 = VibrationSystem$User.canReceiveVibration（首次声明于接口
 *           class_8514$class_5719，mappings.tiny 已验证），field_44600 = 外部 Warden
 *           引用（synthetic outer ref，named 与 intermediary jar 中同名，javap 已验证）。
 *           开发环境（named 类名）下该目标类不会加载，Mixin 静默不生效，不报错。
 * 升级路径: 升级 Loom 生成 refmap 后可改回 targets = "Warden$VibrationUser" + named 方法名。
 */
@Mixin(targets = "net.minecraft.class_7260$class_8507", remap = false)
public class MixinWardenVibrationUser
{
    // field_44600 = Warden$VibrationUser 持有的外部 Warden 引用（synthetic outer ref）
    @Shadow(aliases = "field_44600", remap = false) @Final private Warden owner;

    // method_32970 = VibrationSystem$User.canReceiveVibration(ServerLevel, BlockPos, Holder<GameEvent>, GameEvent.Context) -> boolean
    @Inject(method = "method_32970", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$deafenFrozenWarden(ServerLevel level, BlockPos pos,
                                                  Holder<GameEvent> gameEvent,
                                                  GameEvent.Context context,
                                                  CallbackInfoReturnable<Boolean> cir)
    {
        if (FakePeaceManager.shouldDeafenWarden(this.owner))
        {
            cir.setReturnValue(false);
        }
    }
}
