package dev.dailywool.keybindprofiles.gui;

import dev.dailywool.keybindprofiles.ProfileStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Asks for one name. Used for saving, renaming and creating folders, which is why it hands the
 * name back through a callback rather than doing anything with it.
 */
public class NamePromptScreen extends Screen {
	private final Screen parent;
	private final String initial;
	private final Consumer<String> onConfirm;

	private EditBox nameBox;
	private Button confirmButton;

	public NamePromptScreen(Screen parent, Component title, String initial, Consumer<String> onConfirm) {
		super(title);
		this.parent = parent;
		this.initial = initial;
		this.onConfirm = onConfirm;
	}

	@Override
	protected void init() {
		this.nameBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20, this.title);
		this.nameBox.setMaxLength(64);
		this.nameBox.setValue(this.initial);
		this.nameBox.setResponder(s -> this.updateConfirm());
		this.addRenderableWidget(this.nameBox);

		this.confirmButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.confirm())
				.bounds(this.width / 2 - 100, this.height / 2 + 12, 98, 20)
				.build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
				.bounds(this.width / 2 + 2, this.height / 2 + 12, 98, 20)
				.build());

		this.updateConfirm();
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.nameBox);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// isConfirmation is enter only. isSelection would also catch space, which you want to be
		// able to type into the box.
		if (event.isConfirmation() && this.confirmButton.active) {
			this.confirm();
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 40, -1);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private void updateConfirm() {
		this.confirmButton.active = ProfileStore.clean(this.nameBox.getValue()) != null;
	}

	private void confirm() {
		String name = ProfileStore.clean(this.nameBox.getValue());

		if (name == null) {
			return;
		}

		// Go back first so the callback is free to put a confirmation prompt on top.
		this.minecraft.gui.setScreen(this.parent);
		this.onConfirm.accept(name);
	}
}
