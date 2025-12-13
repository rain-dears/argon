package dev.lvstrng.vxe;

import net.fabricmc.api.ModInitializer;

import java.io.IOException;

public final class Main implements ModInitializer {
	@Override
	public void onInitialize() {
		try {
			new Vxe();
		} catch (InterruptedException | IOException ignored) {}
	}
}
