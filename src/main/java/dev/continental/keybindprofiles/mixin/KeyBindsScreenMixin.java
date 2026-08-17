package dev.continental.keybindprofiles.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.continental.keybindprofiles.gui.ProfileScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsScreen.class)
public class KeyBindsScreenMixin {

	/**
	 * Adds the profiles button to the row vanilla builds for "Reset All" and "Done". Grabbing the
	 * local rather than going through the screen's layout field matters: the footer frame centres
	 * whatever you give it, so a fourth child added there would sit on top of that row instead of
	 * beside it.
	 */
	// Vanilla's two buttons are 150 each, which already comes to 308 of the 320 the game guarantees
	// a GUI is wide. A third one at any width would push the row off both edges, so all three get
	// narrowed to fit: 3 * 100 plus two 8px gaps is 316.
	private static final int BUTTON_WIDTH = 100;

	@Inject(method = "addFooter", at = @At("TAIL"))
	private void keybindProfiles$addProfilesButton(CallbackInfo ci, @Local LinearLayout bottomButtons) {
		KeyBindsScreen self = (KeyBindsScreen) (Object) this;

		bottomButtons.visitWidgets(w -> w.setWidth(BUTTON_WIDTH));
		bottomButtons.addChild(Button.builder(
				Component.translatable("keybindprofiles.open"),
				b -> {
					Minecraft mc = Minecraft.getInstance();
					mc.gui.setScreen(new ProfileScreen(self, mc.options));
				}
		).width(BUTTON_WIDTH).build());
	}
}
