package tanjikun.helpful.tweake.mixin;

import java.util.Map;
import java.util.Set;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.ServerFlagHolder;

/**
 * 未完成进度显示 — 触发全量重新同步。
 *
 * 根因（反编译 PlayerAdvancements.flushDirty / method_12876 验证）：
 *   flushDirty 每 tick 调用，但只在 isFirstPacket=true 或 rootsToUpdate 非空或
 *   progressChanged 非空时才执行。玩家加入世界时 load() 会填充 rootsToUpdate 和
 *   progressChanged，首次 flushDirty 全量同步一次。切换功能后两个集合为空，
 *   flushDirty 直接返回，隐藏进度的可见性不会重新评估。
 *
 *   added 集合是 diff 增量而非全量（反编译 method_48027 visitor 验证）：
 *   updateTreeVisibility 对每个节点调用 visible.add(holder)，仅当返回 true
 *   （首次同步给客户端）才加入 added；isFirstPacket 只作为包的 reset 参数。
 *   因此触发重同步必须复刻 reload 的完整流程（反编译 method_12886 验证）：
 *   visible.clear() + isFirstPacket=true + rootsToUpdate/progressChanged 填充。
 *   漏掉 visible.clear() 时 reset 包只带新可见的隐藏进度（root 们已在 visible
 *   集合中，不进 added），客户端清空进度树后回填的全是缺 parent 的孤儿节点，
 *   无任何 tab → 整页只剩"这里好像什么都没有……"。
 *
 *   双向切换都触发重同步：开启时全量显示，关闭时按原版可见性恢复（reset 包
 *   会让客户端完全重建进度树）。
 *
 * ponytail: @Shadow 用 aliases 指定 intermediary 字段名（项目无 refmap，详见 project_memory）
 */
@Mixin(PlayerAdvancements.class)
public class MixinPlayerAdvancements
{
    @Shadow(aliases = {"field_13396"}, remap = false)
    private boolean isFirstPacket;

    @Shadow(aliases = {"field_46073"}, remap = false)
    private AdvancementTree tree;

    @Shadow(aliases = {"field_41736"}, remap = false)
    private Set<AdvancementNode> rootsToUpdate;

    @Shadow(aliases = {"field_13388"}, remap = false)
    private Set<AdvancementHolder> progressChanged;

    @Shadow(aliases = {"field_13390"}, remap = false)
    private Set<AdvancementHolder> visible;

    @Shadow(aliases = {"field_41735"}, remap = false)
    private Map<AdvancementHolder, AdvancementProgress> progress;

    @Unique
    private boolean helpfulTweake$lastShow = false;

    @Inject(method = "method_12876", at = @At("HEAD"), remap = false)
    private void helpfulTweake$triggerResync(ServerPlayer player, boolean force, CallbackInfo ci)
    {
        boolean current = ServerFlagHolder.showUncompletedAdvancements;
        if (current != helpfulTweake$lastShow)
        {
            helpfulTweake$lastShow = current;
            // 复刻 reload：清空已同步集合，让 flushDirty 的可见性 diff 全量重建 added
            visible.clear();
            isFirstPacket = true;
            for (AdvancementNode root : tree.roots())
            {
                rootsToUpdate.add(root);
            }
            progressChanged.addAll(progress.keySet());
        }
    }
}
