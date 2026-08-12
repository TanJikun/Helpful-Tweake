package tanjikun.helpful.tweake.client.util;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.Entity;

/**
 * 实体可见性结果缓存：避免每帧对每个实体做昂贵的光线投射。
 *
 * 缓存策略：实体位置或相机位置变化超过阈值（0.5 格）时才重算。
 *   - 实体不动（如站立怪物、堆叠苦力怕）→ 命中缓存，跳过 clip()
 *   - 玩家走动 → 相机位置变化，但只在跨越阈值时失效
 *
 * 参考自 EntityCulling（tr7zw Protective License，禁止代码复制，仅参考算法思路）：
 *   - 异步多线程：未引入（升级路径，见 ponytail）
 *   - 位置触发重算：已实现
 *   - 批量缓存：已实现
 *
 * ponytail: 用 entity.getId() 作 key，HashMap 查询 O(1)。
 * 已知上限: MAX_SIZE = 4000，超过即清空重建；实体极多时偶发帧抖动。
 * 升级路径: 改 int->long 编码 + LongOpenHashMap 减少装箱；或异步线程计算。
 */
public class EntityVisibilityCache
{
    private static final Map<Integer, CachedVisibility> cache = new HashMap<>();
    private static final double POS_THRESHOLD = 0.5;
    private static final int MAX_SIZE = 4000;
    private static final int ENTRY_TTL_FRAMES = 200;
    private static int frameCounter = 0;

    private record CachedVisibility(boolean visible,
                                    double entityX, double entityY, double entityZ,
                                    double camX, double camY, double camZ,
                                    int frame) {}

    /**
     * 查询缓存。
     * @return Boolean.TRUE/FALSE 缓存命中（可见/不可见）；null 未命中需重算
     */
    public static Boolean get(Entity entity, double camX, double camY, double camZ)
    {
        CachedVisibility c = cache.get(entity.getId());
        if (c == null) return null;
        if (frameCounter - c.frame > ENTRY_TTL_FRAMES) return null;
        if (Math.abs(c.entityX - entity.getX()) > POS_THRESHOLD) return null;
        if (Math.abs(c.entityY - entity.getY()) > POS_THRESHOLD) return null;
        if (Math.abs(c.entityZ - entity.getZ()) > POS_THRESHOLD) return null;
        if (Math.abs(c.camX - camX) > POS_THRESHOLD) return null;
        if (Math.abs(c.camY - camY) > POS_THRESHOLD) return null;
        if (Math.abs(c.camZ - camZ) > POS_THRESHOLD) return null;
        return c.visible;
    }

    public static void put(Entity entity, double camX, double camY, double camZ, boolean visible)
    {
        if (cache.size() > MAX_SIZE)
        {
            cache.clear();
        }
        cache.put(entity.getId(), new CachedVisibility(
                visible, entity.getX(), entity.getY(), entity.getZ(),
                camX, camY, camZ, frameCounter));
    }

    public static void tick()
    {
        frameCounter++;
        if (frameCounter % ENTRY_TTL_FRAMES == 0)
        {
            int now = frameCounter;
            cache.values().removeIf(c -> now - c.frame > ENTRY_TTL_FRAMES);
        }
    }

    public static void clear()
    {
        cache.clear();
    }
}
