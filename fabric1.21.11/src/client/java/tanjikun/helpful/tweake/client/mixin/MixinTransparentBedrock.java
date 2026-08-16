package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 透明基岩：开启后基岩不渲染自身模型，被其挡住的方块面可见。
 *
 * 渲染管线中面剔除的实际流程（Block.shouldRenderFace，反编译验证）：
 *   1. shape = adjacent.getFaceOcclusionShape(dir.getOpposite())
 *   2. if shape == Shapes.block() → return false（不渲染）  ← 基岩在此被剔除！
 *   3. if self.skipRendering(adjacent, dir) → return false
 *   4. if shape == Shapes.empty() → return true（渲染）
 *   5. 复杂遮挡形状比较
 *
 * 因此需要注入的方法（按调用顺序）：
 *   - getRenderShape()（method_26217）→ INVISIBLE：基岩自身不渲染
 *   - getFaceOcclusionShape()（method_26173）→ Shapes.empty()：使步骤2不命中、步骤4命中，直接返回"渲染"
 *   - skipRendering()（method_26187）→ false：步骤3也不命中（双重保险）
 *   - isSolidRender()（method_26216）→ false：其他代码路径（光照等）不再视基岩为实体
 *   - canOcclude()（method_26225）→ false：同上
 *   - getLightBlock()（method_26193）→ 0：基岩不阻挡光线传播，后方方块有正常光照
 *   - propagatesSkylightDown()（method_26167）→ true：天光穿透基岩
 *
 * ponytail: Mixin 方法名使用 intermediary + remap = false（项目无 refmap，详见 project_memory）
 * 灵感来源：https://www.bilibili.com/video/BV1E3jG6gEsZ ，本实现为独立重写，未照搬原代码。
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinTransparentBedrock
{
    @Inject(method = "method_26217", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$transparentBedrock$getRenderShape(CallbackInfoReturnable<RenderShape> cir)
    {
        if (Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue()
                && (Object) this == Blocks.BEDROCK.defaultBlockState())
        {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }

    // getFaceOcclusionShape（method_26173）：shouldRenderFace 的第一道检查，
    // 若返回 full block 则直接剔除面，后续 skipRendering 根本不会被调用。
    // 返回 Shapes.empty() 使其跳过 full-block 检查并直接判定为"应渲染"。
    @Inject(method = "method_26173", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$transparentBedrock$getFaceOcclusionShape(
            Direction direction, CallbackInfoReturnable<VoxelShape> cir)
    {
        if (Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue()
                && (Object) this == Blocks.BEDROCK.defaultBlockState())
        {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "method_26216", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$transparentBedrock$isSolidRender(CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue()
                && (Object) this == Blocks.BEDROCK.defaultBlockState())
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "method_26225", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$transparentBedrock$canOcclude(CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue()
                && (Object) this == Blocks.BEDROCK.defaultBlockState())
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "method_26187", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$transparentBedrock$skipRendering(
            BlockState adjacent, Direction direction, CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue()
                && adjacent == Blocks.BEDROCK.defaultBlockState())
        {
            cir.setReturnValue(false);
        }
    }

    // getLightBlock（method_26193）：基岩默认阻挡全部光照（返回15），
    // 导致其后方方块处于黑暗中。返回0使光线正常穿过。
    @Inject(method = "method_26193", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$transparentBedrock$getLightBlock(CallbackInfoReturnable<Integer> cir)
    {
        if (Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue()
                && (Object) this == Blocks.BEDROCK.defaultBlockState())
        {
            cir.setReturnValue(0);
        }
    }

    // propagatesSkylightDown（method_26167）：允许天光穿透基岩
    @Inject(method = "method_26167", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$transparentBedrock$propagatesSkylightDown(CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue()
                && (Object) this == Blocks.BEDROCK.defaultBlockState())
        {
            cir.setReturnValue(true);
        }
    }

    // getShadeBrightness（method_26210）：AO（环境光遮蔽）计算用此方法判断方块对周围面的遮蔽程度。
    // 基岩默认返回 0.2（强遮蔽），导致周围基岩越多、面越暗。返回 1.0 让 AO 不把基岩当作遮蔽源。
    @Inject(method = "method_26210", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$transparentBedrock$getShadeBrightness(
            BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir)
    {
        if (Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue()
                && (Object) this == Blocks.BEDROCK.defaultBlockState())
        {
            cir.setReturnValue(1.0f);
        }
    }
}
