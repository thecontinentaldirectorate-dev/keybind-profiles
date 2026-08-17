package dev.dailywool.keybindprofiles;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class KeybindProfiles implements ClientModInitializer {
	public static final String MOD_ID = "keybind-profiles";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		// Created up front so there is somewhere to drop files into before you have saved anything.
		try {
			ProfileStore.createRoot();
		} catch (IOException e) {
			LOG.error("Could not create the profiles folder at {}", ProfileStore.root(), e);
		}
	}
}
