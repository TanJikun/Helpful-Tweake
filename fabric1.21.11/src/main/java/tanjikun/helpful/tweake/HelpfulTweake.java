package tanjikun.helpful.tweake;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tanjikun.helpful.tweake.config.CommonConfigs;
import tanjikun.helpful.tweake.util.FakePeaceManager;
import tanjikun.helpful.tweake.util.PortalFarmAi;

public class HelpfulTweake implements ModInitializer {
	public static final String MOD_ID = "helpful-tweake";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, or errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 双端：加载服务端配置（客户端值会被 MaLiLib 同步覆盖）
		CommonConfigs.loadFromFile();
		// 伪和平优化：每 tick 检查是否需要扫描敌对生物方块分布
		ServerTickEvents.END_SERVER_TICK.register(FakePeaceManager::onEndServerTick);
		// 切门刷怪塔优化：主线程 tick 末检测 finalizeSpawn 登记的生物是否碰门
		// （finalizeSpawn 可能在区块生成线程调用，现场查方块会导致跨线程死锁）
		ServerTickEvents.END_SERVER_TICK.register(PortalFarmAi::onEndServerTick);
		// 切门刷怪塔优化：触发冻结附件注册（须在世界加载前完成，否则实体反序列化附件失败）
		PortalFarmAi.init();
		LOGGER.info("Helpful Tweake initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
