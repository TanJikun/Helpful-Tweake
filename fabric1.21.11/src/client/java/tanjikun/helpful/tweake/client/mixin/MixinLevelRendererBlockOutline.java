package tanjikun.helpful.tweake.client.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import tanjikun.helpful.tweake.config.Configs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Mixin(LevelRenderer.class)
public class MixinLevelRendererBlockOutline {
    private static final double REF_DISTANCE = 5.0;
    private static final float MIN_SCALE = 0.15F;
    private static final float WIDTH_BOOST = 1.3F;
    private static final long PERIOD_MS = 6000L;
    private static final double PRECISION = 10000.0; // 保留4位小数，用于量化坐标

    @Unique
    private static final class VecKey {
        private final double x, y, z;

        VecKey(double x, double y, double z) {
            // 量化坐标以消除浮点误差，确保相同物理位置的顶点使用相同的键
            this.x = Math.round(x * PRECISION) / PRECISION;
            this.y = Math.round(y * PRECISION) / PRECISION;
            this.z = Math.round(z * PRECISION) / PRECISION;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof VecKey that))
                return false;
            return Double.compare(that.x, x) == 0 &&
                    Double.compare(that.y, y) == 0 &&
                    Double.compare(that.z, z) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    @Inject(method = "method_62210", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$renderRainbowOutline(MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack, boolean translucentPass, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (!Configs.Tools.RAINBOW_SELECTION.getBooleanValue()) {
            return;
        }

        BlockOutlineRenderState state = levelRenderState.blockOutlineRenderState;
        if (state == null || state.isTranslucent() != translucentPass) {
            return;
        }

        ci.cancel();

        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        BlockPos pos = state.pos();
        VoxelShape shape = state.shape();
        double x = pos.getX() - cameraPos.x;
        double y = pos.getY() - cameraPos.y;
        double z = pos.getZ() - cameraPos.z;

        PoseStack.Pose poseEntry = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());

        double distance = Math.sqrt(x * x + y * y + z * z);
        float widthScale;
        if (distance <= REF_DISTANCE) {
            widthScale = 1.0F;
        } else {
            widthScale = (float) (REF_DISTANCE / distance);
            if (widthScale < MIN_SCALE) {
                widthScale = MIN_SCALE;
            }
        }
        float finalWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth()
                * WIDTH_BOOST * widthScale;

        float timePhase = ((System.currentTimeMillis() % PERIOD_MS) / (float) PERIOD_MS) * 6.0F;

        Map<VecKey, Integer> vertexColorCache = new HashMap<>();

        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            Vector3f normal = new Vector3f(
                    (float) (maxX - minX),
                    (float) (maxY - minY),
                    (float) (maxZ - minZ)).normalize();

            // 使用世界坐标作为键
            double wx1 = pos.getX() + minX;
            double wy1 = pos.getY() + minY;
            double wz1 = pos.getZ() + minZ;
            VecKey key1 = new VecKey(wx1, wy1, wz1);
            int color1 = vertexColorCache.computeIfAbsent(key1,
                    k -> hsvRainbow((float) k.x, (float) k.y, (float) k.z, timePhase));

            double wx2 = pos.getX() + maxX;
            double wy2 = pos.getY() + maxY;
            double wz2 = pos.getZ() + maxZ;
            VecKey key2 = new VecKey(wx2, wy2, wz2);
            int color2 = vertexColorCache.computeIfAbsent(key2,
                    k -> hsvRainbow((float) k.x, (float) k.y, (float) k.z, timePhase));

            consumer.addVertex(poseEntry, (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                    .setColor(color1)
                    .setNormal(poseEntry, normal)
                    .setLineWidth(finalWidth);

            consumer.addVertex(poseEntry, (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                    .setColor(color2)
                    .setNormal(poseEntry, normal)
                    .setLineWidth(finalWidth);
        });

        bufferSource.endBatch(RenderTypes.lines());
    }

    @Unique
    private static int hsvRainbow(float wx, float wy, float wz, float timePhase) {
        float sum = wx + wy + wz;
        float hue = (sum / 6.0F) + (timePhase / 6.0F);
        hue = hue - (float) Math.floor(hue);
        float saturation = 1.0F;
        float value = 1.0F;
        int rgb = hsvToRgb(hue, saturation, value);
        return 0xFF000000 | rgb;
    }

    @Unique
    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1.0F - Math.abs((h * 6.0F) % 2.0F - 1.0F));
        float m = v - c;
        float r, g, b;
        if (h < 1.0F / 6.0F) {
            r = c;
            g = x;
            b = 0;
        } else if (h < 2.0F / 6.0F) {
            r = x;
            g = c;
            b = 0;
        } else if (h < 3.0F / 6.0F) {
            r = 0;
            g = c;
            b = x;
        } else if (h < 4.0F / 6.0F) {
            r = 0;
            g = x;
            b = c;
        } else if (h < 5.0F / 6.0F) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }
        int ri = (int) ((r + m) * 255.0F);
        int gi = (int) ((g + m) * 255.0F);
        int bi = (int) ((b + m) * 255.0F);
        return (ri << 16) | (gi << 8) | bi;
    }
}