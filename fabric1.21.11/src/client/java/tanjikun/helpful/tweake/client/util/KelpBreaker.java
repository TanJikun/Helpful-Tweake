package tanjikun.helpful.tweake.client.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 清海带：自动破坏 Litematica 选区内、3D 曼哈顿距离不超过阈值的海带。
 *
 * 行为：
 *   - 每 tick 扫描并破坏所有满足条件的海带（最多 MAX_BREAK_PER_TICK 个）
 *   - slot 选择在循环外只执行一次，避免重复切换
 *   - 自动切换到快捷栏中没有耐久度的物品；全都有耐久则选剩余耐久最多的
 *   - 不转动玩家视角（直接发送网络包，不调用 startDestroyBlock 以避免影响客户端破坏状态机）
 *   - 海带硬度0，瞬时破坏，只需 START_DESTROY_BLOCK
 *
 * 依赖：
 *   - 需要安装 Litematica 模组；未安装时功能不触发（getBooleanValue 仍可为 true 但不执行）
 *   - 通过反射获取 Litematica 选区，未安装时不崩溃
 *
 * ponytail: 每 tick 只破坏一个海带，避免发包过快被反作弊踢出
 * 已知上限: 距离128时曼哈顿球体约8.7M方块，每tick扫描会卡死，用MAX_BLOCKS_PER_TICK限制
 * 升级路径: 可改为先获取选区bounding box再扫描交集，或分帧扫描
 */
public class KelpBreaker implements ClientTickEvents.EndTick
{
    private static final int SELECTION_CACHE_TICKS = 20;
    // ponytail: 每 tick 扫描方块数硬上限，避免大半径卡顿
    // 已知上限: 距离128时曼哈顿球体约8.7M方块，无限制会卡死
    // 升级路径: 可改为先求选区bounding box再扫描交集，或分帧扫描
    private static final int MAX_BLOCKS_PER_TICK = 8192;
    // 每 tick 最多破坏的海带数，避免发包过快被反作弊踢出
    private static final int MAX_BREAK_PER_TICK = 8;

    // 缓存的选区盒子列表：每个 int[6] = {minX, minY, minZ, maxX, maxY, maxZ}，闭区间
    private List<int[]> cachedBoxes = null;
    private int selectionCacheCounter = 0;

    @Override
    public void onEndTick(Minecraft mc)
    {
        if (!Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                || !Configs.Tools.WSM_CLEAR_KELP.getBooleanValue())
        {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded("litematica"))
        {
            return;
        }
        if (mc.player == null || mc.level == null || mc.getConnection() == null)
        {
            return;
        }

        boolean selectionOnly = Configs.Tools.WSM_CLEAR_KELP_SELECTION_ONLY.getBooleanValue();

        refreshSelectionCache();
        if (selectionOnly && (cachedBoxes == null || cachedBoxes.isEmpty()))
        {
            return;
        }

        int maxDistance = Configs.Tools.WSM_CLEAR_KELP_DISTANCE.getIntegerValue();
        BlockPos playerPos = mc.player.blockPosition();
        int px = playerPos.getX();
        int py = playerPos.getY();
        int pz = playerPos.getZ();

        // 循环外选好 slot 并切换，避免每个海带都重算
        Inventory inv = mc.player.getInventory();
        int bestSlot = findBestSlot(inv);
        int currentSlot = inv.getSelectedSlot();
        if (bestSlot != currentSlot)
        {
            inv.setSelectedSlot(bestSlot);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(bestSlot));
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int broken = 0;

        // 遍历 3D 曼哈顿球体，破坏所有满足条件的海带（每 tick 最多 MAX_BREAK_PER_TICK 个）
        int scanned = 0;
        for (int dx = -maxDistance; dx <= maxDistance; dx++)
        {
            int remainingX = maxDistance - Math.abs(dx);
            for (int dy = -remainingX; dy <= remainingX; dy++)
            {
                int remainingY = remainingX - Math.abs(dy);
                for (int dz = -remainingY; dz <= remainingY; dz++)
                {
                    if (scanned++ >= MAX_BLOCKS_PER_TICK)
                    {
                        break;
                    }
                    mutable.set(px + dx, py + dy, pz + dz);
                    BlockState state = mc.level.getBlockState(mutable);

                    // 条件1：是海带、海草或高海草
                    if (!state.is(Blocks.KELP) && !state.is(Blocks.KELP_PLANT)
                            && !state.is(Blocks.SEAGRASS) && !state.is(Blocks.TALL_SEAGRASS))
                    {
                        continue;
                    }

                    // 条件2：开启"仅清理选区内"时，必须在 Litematica 选区内
                    // 条件3：3D 曼哈顿距离 <= maxDistance（已由循环结构保证）
                    if (selectionOnly && !isInSelection(mutable))
                    {
                        continue;
                    }

                    // 满足所有条件，发送破坏包（海带硬度0，瞬时破坏，只需 START_DESTROY_BLOCK）
                    mc.getConnection().send(new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                            mutable.immutable(),
                            Direction.UP
                    ));
                    if (++broken >= MAX_BREAK_PER_TICK)
                    {
                        break;
                    }
                }
            }
        }
    }

    /**
     * 选择最佳快捷栏 slot：
     *   1. 优先当前选中格（如果已经是没耐久的物品，含空手）
     *   2. 其次找第一个没耐久的物品
     *   3. 全都有耐久则选剩余耐久最多的
     */
    private int findBestSlot(Inventory inv)
    {
        int currentSlot = inv.getSelectedSlot();

        // 当前选中格已经是没耐久的物品，直接用
        if (!inv.getItem(currentSlot).isDamageableItem())
        {
            return currentSlot;
        }

        // 找第一个没耐久的物品（含空手：空 stack 的 isDamageableItem() 返回 false）
        for (int i = 0; i < 9; i++)
        {
            if (!inv.getItem(i).isDamageableItem())
            {
                return i;
            }
        }

        // 全都有耐久，找剩余耐久最多的
        int bestSlot = currentSlot;
        int bestDurability = -1;
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = inv.getItem(i);
            if (stack.isDamageableItem())
            {
                int durability = stack.getMaxDamage() - stack.getDamageValue();
                if (durability > bestDurability)
                {
                    bestDurability = durability;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    // ========== Litematica 选区反射检查 ==========

    /**
     * 定期刷新 Litematica 选区缓存。
     * 通过反射调用 DataManager → SelectionManager → AreaSelection → Box 链。
     * 任何异常（未安装、API 变动）都被吞掉，cachedBoxes 置空。
     */
    private void refreshSelectionCache()
    {
        selectionCacheCounter++;
        if (selectionCacheCounter < SELECTION_CACHE_TICKS)
        {
            return;
        }
        selectionCacheCounter = 0;

        try
        {
            Class<?> dmCls = Class.forName("fi.dy.masa.litematica.data.DataManager");
            Object sm = dmCls.getMethod("getSelectionManager").invoke(null);
            if (sm == null)
            {
                cachedBoxes = null;
                return;
            }

            Object area = sm.getClass().getMethod("getCurrentSelection").invoke(sm);
            if (area == null)
            {
                cachedBoxes = null;
                return;
            }

            Object boxes = area.getClass().getMethod("getAllSubRegionBoxes").invoke(area);
            if (!(boxes instanceof List<?> boxList))
            {
                cachedBoxes = null;
                return;
            }

            Class<?> boxCls = Class.forName("fi.dy.masa.litematica.selection.Box");
            Method getPos1 = boxCls.getMethod("getPos1");
            Method getPos2 = boxCls.getMethod("getPos2");

            List<int[]> newBoxes = new ArrayList<>();
            for (Object box : boxList)
            {
                BlockPos p1 = (BlockPos) getPos1.invoke(box);
                BlockPos p2 = (BlockPos) getPos2.invoke(box);
                if (p1 == null || p2 == null)
                {
                    continue;
                }
                newBoxes.add(new int[]{
                        Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()),
                        Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ())
                });
            }
            cachedBoxes = newBoxes;
        }
        catch (Throwable t)
        {
            cachedBoxes = null;
        }
    }

    private boolean isInSelection(BlockPos pos)
    {
        if (cachedBoxes == null || cachedBoxes.isEmpty())
        {
            return false;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        for (int[] box : cachedBoxes)
        {
            if (x >= box[0] && x <= box[3]
                    && y >= box[1] && y <= box[4]
                    && z >= box[2] && z <= box[5])
            {
                return true;
            }
        }
        return false;
    }
}
