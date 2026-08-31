package tanjikun.helpful.tweake.client.util;

import net.minecraft.client.Minecraft;

import tanjikun.helpful.tweake.config.Configs;
import tanjikun.helpful.tweake.config.ZoomTransitionMode;

/**
 * 放大镜状态机（非 Mixin，供两个 Mixin 共用）。
 *
 * - 按键按住：从当前画面倍数过渡到默认倍数（每次按下重置，滚轮调整不持久化）
 * - 按键松开：从当前画面倍数过渡回 1 倍
 * - 放大期间滚轮：目标倍数乘除调整，从当前画面倍数平滑过渡到新目标
 * - 过渡动画按真实时间（nanoTime）插值，每帧调用 getRenderZoom() 取当前应渲染的倍数
 */
public class ZoomState
{
    private static final double MIN_MULTIPLIER = 1.0;
    private static final double MAX_MULTIPLIER = 32.0;

    private static boolean keyHeld;
    private static double targetMultiplier = 1.0;
    private static double fromMultiplier = 1.0;
    private static long startNanos = -1L;

    /** 每 tick 由 InitHandler 调用，检测按住/松开并启动过渡 */
    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();
        boolean held = mc.player != null
                && Configs.Tools.ZOOM.getBooleanValue()
                && Configs.Tools.ZOOM.getKeybind().isKeybindHeld();
        if (held == keyHeld)
        {
            return;
        }
        keyHeld = held;
        // 每次按下重置为配置的默认倍数（滚轮调整的结果不保存）；
        // 目标值必须由 startTransition 内部赋值，否则 getRenderZoom()
        // 会把已提前改写的 targetMultiplier 当作过渡起点，from==to 动画失效
        startTransition(held ? Configs.Tools.ZOOM_DEFAULT_MULTIPLIER.getDoubleValue() : 1.0);
    }

    /**
     * 放大期间收到滚轮事件（由 MouseHandler Mixin 转发）。
     * 倍数按 2^(±0.25) 指数步进，滚一格约 ±25%。
     *
     * @return true 表示已消费（按键按住中）
     */
    public static boolean onScroll(double vertical)
    {
        if (!keyHeld)
        {
            return false;
        }
        double next = Math.clamp(targetMultiplier * Math.pow(2.0, 0.25 * vertical),
                MIN_MULTIPLIER, MAX_MULTIPLIER);
        // 目标值由 startTransition 内部赋值（理由同 tick），旧 targetMultiplier 用作计算基准
        startTransition(next);
        return true;
    }

    /** 当前帧应渲染的放大倍数（含过渡插值），由 GameRenderer Mixin 每帧调用 */
    public static double getRenderZoom()
    {
        if (startNanos < 0L)
        {
            return targetMultiplier;
        }
        double durationMs = getDurationMs();
        double t = durationMs <= 0.0 ? 1.0 : (System.nanoTime() - startNanos) / 1e6 / durationMs;
        if (t >= 1.0)
        {
            startNanos = -1L;
            return targetMultiplier;
        }
        ZoomTransitionMode mode = (ZoomTransitionMode) Configs.Tools.ZOOM_TRANSITION.getOptionListValue();
        float eased = mode.apply((float) t);
        return fromMultiplier + (targetMultiplier - fromMultiplier) * eased;
    }

    /** 过渡时长：即时模式 0ms，其余 = 1000ms / 过渡速度 */
    private static double getDurationMs()
    {
        ZoomTransitionMode mode = (ZoomTransitionMode) Configs.Tools.ZOOM_TRANSITION.getOptionListValue();
        if (mode == ZoomTransitionMode.INSTANT)
        {
            return 0.0;
        }
        return 1000.0 / Configs.Tools.ZOOM_TRANSITION_SPEED.getDoubleValue();
    }

    private static void startTransition(double to)
    {
        fromMultiplier = getRenderZoom();
        targetMultiplier = to;
        startNanos = System.nanoTime();
    }
}
