package dev.dailywool.keybindprofiles;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A saved set of bindings, stored as binding name ("key.jump") to key name ("key.keyboard.space").
 * Those are the same two strings the vanilla options file uses, so profiles stay readable and
 * survive version changes as well as options.txt does.
 */
public record Profile(Map<String, String> binds) {

	public static Profile capture(Options options) {
		Map<String, String> binds = new LinkedHashMap<>();
		for (KeyMapping k : options.keyMappings) {
			binds.put(k.getName(), k.saveString());
		}

		return new Profile(binds);
	}

	/**
	 * Applies every binding this profile knows about, and returns how many it had to skip.
	 */
	public int applyTo(Options options) {
		int skipped = 0;

		for (KeyMapping k : options.keyMappings) {
			String saved = this.binds.get(k.getName());

			// Anything the profile has never heard of keeps whatever it is bound to now. A profile
			// saved before you installed a mod should not quietly unbind that mod's keys.
			if (saved == null) {
				continue;
			}

			try {
				k.setKey(InputConstants.getKey(saved));
			} catch (IllegalArgumentException e) {
				// getKey parses the number out of names like "key.mouse.4" and throws on anything
				// it cannot place, which is what you get from a profile written by a version that
				// spelled the key differently. Count it and carry on so one dead entry does not
				// cost you the other forty.
				skipped++;
			}
		}

		KeyMapping.resetMapping();
		options.save();
		return skipped;
	}
}
