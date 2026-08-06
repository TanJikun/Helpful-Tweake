package tanjikun.helpful.tweake.util;

import java.util.UUID;

/**
 * 控制爬行功能的状态管理（common 侧，客户端与内部服务器共享）。
 *
 * 单人游戏中客户端与内部服务器在同一 JVM 运行，此类的静态字段被两端共享：
 *   - 客户端 tick 负责：设置 localPlayerUuid（本地玩家 UUID）+ 根据 HOLD/TOGGLE 模式
 *     计算 crawlRequested 并写入。
 *   - common Mixin（MixinPlayerCrawl）在 Player.getDesiredPose 中调用 shouldCrawl(uuid)，
 *     仅当 UUID 匹配本地玩家且 crawlRequested=true 时强制返回 SWIMMING pose。
 *
 * UUID 匹配确保只对本地玩家生效：
 *   - 客户端：LocalPlayer 的 UUID 与 localPlayerUuid 相同 → 生效
 *   - 内部服务器：本地玩家的 ServerPlayer UUID 相同 → 生效（关键：让服务器端碰撞箱也变 0.6）
 *   - 其他玩家（RemoteClientPlayer / 局域网内其他 ServerPlayer）：UUID 不匹配 → 不生效
 *
 * TOGGLE 模式的切换状态保存在 toggleCrawling；HOLD 模式不使用此字段。
 */
public final class CrawlState
{
    private static boolean toggleCrawling = false;
    private static boolean crawlRequested = false;
    private static UUID localPlayerUuid = null;

    private CrawlState() {}

    /** 切换 TOGGLE 模式下的爬行状态。 */
    public static void toggle()
    {
        toggleCrawling = !toggleCrawling;
    }

    /** 读取 TOGGLE 模式状态（客户端 tick 计算用）。 */
    public static boolean isToggleCrawling()
    {
        return toggleCrawling;
    }

    /** 设置最终爬行请求状态（客户端 tick 每 tick 调用）。 */
    public static void setCrawlRequested(boolean v)
    {
        crawlRequested = v;
    }

    /** 设置本地玩家 UUID（客户端 tick 调用）。 */
    public static void setLocalPlayerUuid(UUID uuid)
    {
        localPlayerUuid = uuid;
    }

    /** 重置所有状态（玩家断开世界连接等场景调用）。 */
    public static void reset()
    {
        toggleCrawling = false;
        crawlRequested = false;
        localPlayerUuid = null;
    }

    /**
     * 判断指定 Player 是否应该进入爬行姿势。
     * Mixin 调用此方法，纯读字段，不触发按键查询（服务器端安全）。
     */
    public static boolean shouldCrawl(UUID playerUuid)
    {
        return crawlRequested
                && localPlayerUuid != null
                && localPlayerUuid.equals(playerUuid);
    }
}
