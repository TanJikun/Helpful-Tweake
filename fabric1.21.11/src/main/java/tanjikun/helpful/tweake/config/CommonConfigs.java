package tanjikun.helpful.tweake.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

import tanjikun.helpful.tweake.HelpfulTweake;

/**
 * 双端配置：服务端从 JSON 读取，客户端从 MaLiLib 配置同步。
 *
 * 仅包含需要在服务端生效的功能配置值（不死图腾、经验修补）。
 * 客户端在每 tick 从 MaLiLib Configs 同步到此类静态字段。
 * 服务端无 MaLiLib，直接从 JSON 文件读写。
 */
public class CommonConfigs
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "helpful-tweake-server.json";

    // 服务端逻辑功能开关
    public static boolean betterTotem = false;
    public static boolean globalMending = false;
    public static boolean infiniteWater = false;
    // 伪和平优化（含子配置：坚守者听声音不钻地）
    public static boolean fakePeaceful = false;
    public static boolean fakePeacefulWardenHearing = true;
    // 切门刷怪塔优化：生成时碰撞箱碰到下界/末地传送门方块的生物永久禁用AI（主世界僵尸猪人豁免）
    public static boolean portalFarmOptimization = false;

    /**
     * 从 JSON 文件加载配置（服务端调用）。
     * 客户端也调用，但值会被 MaLiLib 同步覆盖。
     */
    public static void loadFromFile()
    {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        if (!Files.exists(configPath))
        {
            saveToFile();
            return;
        }
        try
        {
            String json = Files.readString(configPath);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj != null)
            {
                betterTotem = obj.has("betterTotem") && obj.get("betterTotem").getAsBoolean();
                globalMending = obj.has("globalMending") && obj.get("globalMending").getAsBoolean();
                infiniteWater = obj.has("infiniteWater") && obj.get("infiniteWater").getAsBoolean();
                fakePeaceful = obj.has("fakePeaceful") && obj.get("fakePeaceful").getAsBoolean();
                fakePeacefulWardenHearing = !obj.has("fakePeacefulWardenHearing") || obj.get("fakePeacefulWardenHearing").getAsBoolean();
                portalFarmOptimization = obj.has("portalFarmOptimization") && obj.get("portalFarmOptimization").getAsBoolean();
            }
        }
        catch (Exception e)
        {
            HelpfulTweake.LOGGER.error("Failed to load common config, using defaults", e);
        }
    }

    /**
     * 保存配置到 JSON 文件（服务端调用）。
     */
    public static void saveToFile()
    {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        JsonObject obj = new JsonObject();
        obj.addProperty("betterTotem", betterTotem);
        obj.addProperty("globalMending", globalMending);
        obj.addProperty("infiniteWater", infiniteWater);
        obj.addProperty("fakePeaceful", fakePeaceful);
        obj.addProperty("fakePeacefulWardenHearing", fakePeacefulWardenHearing);
        obj.addProperty("portalFarmOptimization", portalFarmOptimization);
        try
        {
            Files.writeString(configPath, GSON.toJson(obj));
        }
        catch (Exception e)
        {
            HelpfulTweake.LOGGER.error("Failed to save common config", e);
        }
    }
}
