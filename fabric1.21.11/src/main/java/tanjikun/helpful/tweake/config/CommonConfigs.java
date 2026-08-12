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
 * 仅包含需要在服务端生效的功能配置值（不死图腾、经验修补、船抬升）。
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
    public static boolean betterBoat = false;
    public static double boatLiftHeight = 0.0;

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
                betterBoat = obj.has("betterBoat") && obj.get("betterBoat").getAsBoolean();
                boatLiftHeight = obj.has("boatLiftHeight") ? obj.get("boatLiftHeight").getAsDouble() : 0.0;
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
        obj.addProperty("betterBoat", betterBoat);
        obj.addProperty("boatLiftHeight", boatLiftHeight);
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
