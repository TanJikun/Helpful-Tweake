package tanjikun.helpful.tweake.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.client.util.ShieldRenderContext;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 盾牌状态显示：在盾牌渲染完成后叠加半透明颜色层。
 * 绿色=可用，红色=冷却中。
 *
 * 注入 ShieldSpecialRenderer.submit (method_65707) 的 TAIL，
 * 此时 PoseStack 仍在盾牌本地坐标系，可直接复用 model 渲染颜色层。
 *
 * 1.21.11 中 RenderType 无静态工厂方法，通过 Model.renderType(ResourceLocation) 获取。
 * Model.renderType (method_23500) 继承自 Model (gzp)。
 *
 * @Shadow model 字段（intermediary: field_55441, named: model, obf: b）：
 *   Loom 1.17 + officialMojangMappings 不生成 refmap，@Shadow 默认用 Java 字段名（model），
 *   生产环境字段名为 intermediary（field_55441），找不到会崩溃。
 *   修复：用 aliases 指定 intermediary 名 field_55441 + remap = false。
 *   Mixin 先找主名字 model（生产环境不存在），再找 aliases 中的 field_55441（命中）。
 *
 * remap=false: method 用 intermediary 名（项目约定，Loom 1.17 不生成 refmap）
 *
 * 灵感来自：https://modrinth.com/mod/shield-statuses
 */
@Mixin(ShieldSpecialRenderer.class)
public class MixinShieldSpecialRenderer
{
    @Shadow(aliases = {"field_55441"}, remap = false)
    @Final
    private ShieldModel model;

    // 无 banner 的盾牌基础纹理
    private static final Identifier SHIELD_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/shield_base_nopattern.png");

    @Inject(method = "method_65707", at = @At("TAIL"), remap = false)
    private void helpfulTweake$renderShieldStatus(DataComponentMap map, ItemDisplayContext ctx,
                                                    PoseStack poseStack, SubmitNodeCollector collector,
                                                    int packedLight, int packedOverlay,
                                                    boolean enchanted, int seed, CallbackInfo ci)
    {
        int color = ShieldRenderContext.getShieldColor();
        ShieldRenderContext.clearShieldColor();

        if (color == ShieldRenderContext.COLOR_NONE) return;
        if (!Configs.Tools.SHIELD_STATUS.getBooleanValue()) return;

        // 1.21.11: 通过 Model.renderType(ResourceLocation) 获取 RenderType
        RenderType renderType = model.renderType(SHIELD_TEXTURE);
        VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource()
                .getBuffer(renderType);

        // 原版 submit 在 TAIL 前已 popPose，需自己复刻 PoseStack 状态：
        // pushPose + scale(1, -1, -1)（翻转 Y/Z 轴），渲染后 popPose
        // 字节码验证（javap）：偏移75 pushPose，79-84 scale(1,-1,-1)，251 popPose
        //
        // plate 几何 z=-2 到 -1（厚1），原版 scale(1,-1,-1) 后 z=[1,2]。
        // 单纯 scale(1.02,-1.02,-1.02) 让颜色层 z=[1.02,2.04]，
        // 仅正面凸出 0.04，背面 z=1.02 被原版 z=1 遮挡（深度测试）。
        // 修复：渲染两次，translate Z ±0.05（在 scale 前，即几何先平移再缩放）：
        // - zOffset=+0.05：颜色层 z=[0.97,1.99]，背面凸出（z<1）
        // - zOffset=-0.05：颜色层 z=[1.07,2.09]，正面凸出（z>2）
        renderShieldOverlay(poseStack, buffer, packedLight, packedOverlay, color, 0.05f);
        renderShieldOverlay(poseStack, buffer, packedLight, packedOverlay, color, -0.05f);
    }

    private void renderShieldOverlay(PoseStack poseStack, VertexConsumer buffer,
                                      int packedLight, int packedOverlay, int color, float zOffset)
    {
        poseStack.pushPose();
        poseStack.scale(1.02f, -1.02f, -1.02f);
        poseStack.translate(0.0f, 0.0f, zOffset);

        model.plate().render(poseStack, buffer, packedLight, packedOverlay, color);
        model.handle().render(poseStack, buffer, packedLight, packedOverlay, color);

        poseStack.popPose();
    }
}
