package tanjikun.helpful.tweake.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * 放大镜的过渡函数选项，共 14 种标准缓动函数。
 * apply(t) 将线性进度 t∈[0,1] 映射为缓动进度，恒满足
 * f(0)=0（即时模式除外，直接返回 1）、f(1)=1、单调不减。
 *
 * 灵感来自：https://modrinth.com/mod/zoomify
 */
public enum ZoomTransitionMode implements IConfigOptionListEntry
{
    INSTANT         ("instant"),
    LINEAR          ("linear"),
    EASE_IN_SINE    ("ease_in_sine"),
    EASE_OUT_SINE   ("ease_out_sine"),
    EASE_IN_OUT_SINE("ease_in_out_sine"),
    EASE_IN_QUAD    ("ease_in_quad"),
    EASE_OUT_QUAD   ("ease_out_quad"),
    EASE_IN_OUT_QUAD("ease_in_out_quad"),
    EASE_IN_CUBIC   ("ease_in_cubic"),
    EASE_OUT_CUBIC  ("ease_out_cubic"),
    EASE_IN_OUT_CUBIC("ease_in_out_cubic"),
    EASE_IN_EXPO    ("ease_in_expo"),
    EASE_OUT_EXPO   ("ease_out_expo"),
    EASE_IN_OUT_EXPO("ease_in_out_expo");

    private static final ZoomTransitionMode[] VALUES = values();
    private static final String TRANSLATION_KEY_PREFIX = "helpful_tweake.config.zoom.transition.";

    private final String serializedName;

    ZoomTransitionMode(String serializedName)
    {
        this.serializedName = serializedName;
    }

    /**
     * 将线性过渡进度 t（0~1）映射为缓动进度（0~1）。
     */
    public float apply(float t)
    {
        return switch (this)
        {
            case INSTANT -> 1.0F;
            case LINEAR -> t;
            case EASE_IN_SINE -> (float) (1 - Math.cos(t * Math.PI / 2));
            case EASE_OUT_SINE -> (float) Math.sin(t * Math.PI / 2);
            case EASE_IN_OUT_SINE -> (float) (-(Math.cos(Math.PI * t) - 1) / 2);
            case EASE_IN_QUAD -> t * t;
            case EASE_OUT_QUAD -> 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT_QUAD -> t < 0.5F ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
            case EASE_IN_CUBIC -> t * t * t;
            case EASE_OUT_CUBIC -> 1 - (float) Math.pow(1 - t, 3);
            case EASE_IN_OUT_CUBIC -> t < 0.5F ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
            case EASE_IN_EXPO -> t == 0 ? 0 : (float) Math.pow(2, 10 * t - 10);
            case EASE_OUT_EXPO -> t >= 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
            case EASE_IN_OUT_EXPO -> t == 0 ? 0 : t >= 1 ? 1 : t < 0.5F
                    ? (float) (Math.pow(2, 20 * t - 10) / 2)
                    : (float) ((2 - Math.pow(2, -20 * t + 10)) / 2);
        };
    }

    @Override
    public String getStringValue()
    {
        return serializedName;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(TRANSLATION_KEY_PREFIX + serializedName);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        int idx = ordinal();
        int next = forward ? (idx + 1) % VALUES.length
                           : (idx - 1 + VALUES.length) % VALUES.length;
        return VALUES[next];
    }

    @Override
    public IConfigOptionListEntry fromString(String value)
    {
        for (ZoomTransitionMode mode : VALUES)
        {
            if (mode.serializedName.equals(value))
            {
                return mode;
            }
        }
        return EASE_OUT_EXPO;
    }

    /**
     * 自检：验证所有模式的 apply 满足端点值、单调不减、值域 [0,1]。
     * 公式写错（如端点不为 1 或中间越界）时 assert 会失败。
     * 运行方式：java -ea -cp build/classes/java/client tanjikun.helpful.tweake.config.ZoomTransitionMode
     */
    public static void main(String[] args)
    {
        for (ZoomTransitionMode mode : VALUES)
        {
            float start = mode.apply(0.0F);
            if (mode != INSTANT)
            {
                assert start == 0.0F : mode + " apply(0) = " + start + "，应为 0";
            }
            float last = start;
            for (int i = 1; i <= 100; i++)
            {
                float v = mode.apply(i / 100.0F);
                assert v >= last - 1e-6F : mode + " 在 t=" + i / 100.0F + " 处非单调";
                assert v >= -1e-4F && v <= 1.0F + 1e-4F : mode + " 在 t=" + i / 100.0F + " 处越界: " + v;
                last = v;
            }
            assert last >= 1.0F - 1e-3F : mode + " apply(1) = " + last + "，应为 1";
        }
        System.out.println("ZoomTransitionMode self-check passed");
    }
}
