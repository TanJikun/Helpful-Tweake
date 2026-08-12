package tanjikun.helpful.tweake.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import tanjikun.helpful.tweake.HelpfulTweake;

public class HelpfulTweakeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (!FabricLoader.getInstance().isModLoaded("malilib")) {
			HelpfulTweake.LOGGER.error("Helpful Tweake requires MaLiLib on the client. Install MaLiLib to enable client features.");
			return;
		}
		// InitHandler imports MaLiLib, JVM loads it lazily here (after isModLoaded check)
		tanjikun.helpful.tweake.InitHandler.registerMalilib();
	}
}
