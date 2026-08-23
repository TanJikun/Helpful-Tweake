package tanjikun.helpful.tweake.client.util;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 堆叠实体渲染优化：坐标相差小于 0.01 格的同类型实体仅渲染一个。
 *
 * 分组键 = 量化坐标(0.01格精度) + EntityType 注册名 + (ItemEntity 的物品注册名)
 *
 * 例子：
 *   - 同坐标多个猪灵（不管盔甲）→ 同一分组 → 仅渲染一个
 *   - 同坐标 2 堆钻石 + 3 堆金锭 → 两个分组（钻石、金锭）→ 各渲染一个
 *   - 站立分散（差距 ≥ 0.01 格）的苦力怕 → 各自渲染，不合并
 *
 * 帧级缓存：每帧渲染开始时由 WorldRenderEvents.AFTER_ENTITIES 清空，为下一帧准备。
 * shouldRender 的调用顺序决定哪个实体被渲染（第一个通过的实体），
 * 顺序不稳定可能导致闪烁，但满足"仅渲染一个"的需求。
 *
 * ponytail: 用 String 拼接分组键，HashSet 查询 O(1)。
 * 已知上限: 实体极多时每帧首次访问的 shouldSkip 都会 add 到 Set，内存与实体数成正比。
 *           但每帧末尾清空，不会跨帧累积。量化边界附近（如 0.0099 vs 0.0100）可能误判。
 * 升级路径: 可改用 LongOpenHashSet 存储编码后的 long 键（坐标+类型 hash），减少 String 开销；
 *           或空间分桶 + 精确距离比较以严格满足 < 0.01 语义。
 */
public class EntityRenderStackCache
{
    private static final Set<String> renderedKeys = new HashSet<>();

    public static void clear()
    {
        renderedKeys.clear();
    }

    /**
     * 检查当前实体是否应被跳过（同坐标同类型已渲染过）。
     * @return true 表示应跳过（已存在），false 表示应渲染（首次出现，已记录）
     */
    public static boolean shouldSkip(Entity entity)
    {
        String key = buildKey(entity);
        return !renderedKeys.add(key);
    }

    private static String buildKey(Entity entity)
    {
        // 量化到 0.01 格精度：只有坐标相差 < 0.01 才视为同组。
        // 之前用 blockPosition()（1 格整数）粒度太粗，同方块内不完全重合的实体也被合并。
        //
        // ponytail: floor 量化是近似方案。
        // 已知上限: 跨桶边界附近（如 0.0099 vs 0.0100，实际差 0.0001）会被误判为不同组，
        //           导致渲染多个。但实际堆叠场景中实体通常几乎完全重合，此近似足够。
        // 升级路径: 空间分桶 + 精确距离比较，但需 O(n) 遍历，复杂度显著上升。
        long qx = (long) Math.floor(entity.getX() * 100.0);
        long qy = (long) Math.floor(entity.getY() * 100.0);
        long qz = (long) Math.floor(entity.getZ() * 100.0);
        StringBuilder sb = new StringBuilder(56);
        sb.append(qx).append(',').append(qy).append(',').append(qz);

        Identifier typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        sb.append(':').append(typeKey);

        // 掉落物按物品类型区分：钻石和金锭各渲染一个
        if (entity instanceof ItemEntity itemEntity)
        {
            ItemStack stack = itemEntity.getItem();
            Identifier itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
            sb.append(':').append(itemKey);
        }

        return sb.toString();
    }
}
