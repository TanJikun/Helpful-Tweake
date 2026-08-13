package tanjikun.helpful.tweake;

/**
 * Common 侧标志位持有者，供 common Mixin 读取客户端配置状态。
 * 客户端在配置变更时通过 InitHandler 更新这些标志位。
 */
public class ServerFlagHolder
{
    public static boolean disableLiquidInteraction = false;
}
