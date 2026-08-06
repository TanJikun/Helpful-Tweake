package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @Accessor 访问 Camera.position（private 字段）。
 * 1.21.11 中 Camera 类移除了 getPosition() 方法，position 字段为 private，
 * 需通过 accessor 暴露。用于世吞运维助手渲染器获取相机世界坐标。
 */
@Mixin(Camera.class)
public interface MixinCameraAccessor
{
    @Accessor("position")
    Vec3 getPosition();
}
