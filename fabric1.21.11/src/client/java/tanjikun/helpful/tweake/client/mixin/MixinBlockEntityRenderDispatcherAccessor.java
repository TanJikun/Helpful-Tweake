package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.MaterialSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @Accessor 访问 BlockEntityRenderDispatcher.materials（private final 字段）。
 * 用于世吞运维助手渲染器获取 MaterialSet，以便用 Material.buffer() 渲染箱子类方块。
 */
@Mixin(BlockEntityRenderDispatcher.class)
public interface MixinBlockEntityRenderDispatcherAccessor
{
    @Accessor("materials")
    MaterialSet getMaterials();
}
