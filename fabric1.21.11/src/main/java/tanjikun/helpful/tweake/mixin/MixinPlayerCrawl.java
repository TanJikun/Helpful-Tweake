package tanjikun.helpful.tweake.mixin;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.util.CrawlState;

/**
 * 控制爬行：当功能激活且按键触发时，强制 Player.getDesiredPose 返回 SWIMMING，
 * 使 updatePlayerPose 调用 setPose(SWIMMING)，碰撞箱高度变为 0.6（可钻入 1 格高空间）。
 *
 * 此 Mixin 在 common source set，同时应用于客户端和内部服务器（单人游戏）：
 *   - 客户端 LocalPlayer：碰撞箱变 0.6，渲染正确
 *   - 内部服务器 ServerPlayer：碰撞箱变 0.6，碰撞检测通过（关键：解决"鬼畜/退出来"问题）
 *
 * 之前的纯客户端 Mixin 方案导致客户端碰撞箱 0.6 但服务器端仍 1.8，服务器碰撞检测
 * 认为玩家撞墙并把玩家推回，表现为"进去一点又退出来，前进鬼畜"。
 *
 * 1.21.11 中没有 Pose.CRAWLING，爬行和游泳共用 Pose.SWIMMING。
 * 旧版 LivingEntity.calculatePose 已重构为 Player.getDesiredPose (method_66325)
 * 和 Player.updatePlayerPose (method_7318)。
 *
 * 注入点：Player.getDesiredPose (method_66325) RETURN，覆盖返回值强制 SWIMMING。
 * 通过 CrawlState.shouldCrawl(uuid) 判断是否本地玩家请求爬行，UUID 匹配避免影响其他玩家。
 *
 * 灵感来源：https://modrinth.com/mod/crawl
 *
 * ponytail: Mixin 方法名使用 intermediary（method_66325）而非 named（getDesiredPose）
 * 已知上限: Loom 1.17 + officialMojangMappings 未生成 refmap，注解 method 字符串无法
 *           从 named 重映射到 intermediary。method_66325 签名 ()Lchx;（返回 Pose），
 *           private 方法，无重载歧义。局域网开放时其他玩家的 ServerPlayer 通过 UUID
 *           匹配排除；专用服务器场景此 mod 不安装（environment:client），不适用。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名 "getDesiredPose"。
 */
@Mixin(Player.class)
public class MixinPlayerCrawl
{
    // method_66325 = Player.getDesiredPose() -> Pose
    @Inject(method = "method_66325", at = @At("RETURN"), cancellable = true, remap = false)
    private void helpfulTweake$forceCrawlPose(CallbackInfoReturnable<Pose> cir)
    {
        Player self = (Player) (Object) this;
        if (!CrawlState.shouldCrawl(self.getUUID()))
        {
            return;
        }

        // 排除不应爬行的场景：鞘翅飞行、骑乘、睡眠、旋风攻击
        // 这些场景原版 getDesiredPose 返回特定 Pose，不应覆盖
        if (self.isFallFlying() || self.isPassenger()
                || self.isSleeping() || self.isAutoSpinAttack())
        {
            return;
        }

        cir.setReturnValue(Pose.SWIMMING);
    }
}
