package dev.continental.keybindprofiles.gui;

import dev.continental.keybindprofiles.KeybindProfiles;
import dev.continental.keybindprofiles.Profile;
import dev.continental.keybindprofiles.ProfileStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One screen per folder. Opening a folder pushes another of these with the parent screen behind
 * it, so Done and Escape walk back up the way they do everywhere else in the game.
 */
public class ProfileScreen extends Screen {
	private static final Component CONFIRM_TITLE = Component.translatable("keybindprofiles.confirm.title");
	private static final Component FAILED = Component.translatable("keybindprofiles.status.failed");

	private static final int FOLDER_COLOUR = 0xFFFCDD05;
	private static final int TEXT_COLOUR = -1;

	// Two rows of buttons plus the padding vanilla leaves under a single row.
	private static final int FOOTER_HEIGHT = 61;
	private static final int HEADER_HEIGHT = 42;

	private final Screen parent;
	private final Options options;
	private final Path dir;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);

	private EntryList list;
	private Button loadButton;
	private Button renameButton;
	private Button deleteButton;
	private Component status;

	public ProfileScreen(Screen parent, Options options, Path dir) {
		super(titleFor(dir));
		this.parent = parent;
		this.options = options;
		this.dir = dir;
	}

	private static Component titleFor(Path dir) {
		return dir.equals(ProfileStore.root())
				? Component.translatable("keybindprofiles.title")
				: Component.literal(ProfileStore.displayName(dir));
	}

	@Override
	protected void init() {
		LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(this.title, this.font));
		header.addChild(new StringWidget(this.breadcrumb(), this.font));

		this.list = this.layout.addToContents(new EntryList(this.minecraft));

		LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical().spacing(8));
		footer.defaultCellSetting().alignHorizontallyCenter();

		LinearLayout top = footer.addChild(LinearLayout.horizontal().spacing(8));
		this.loadButton = top.addChild(Button.builder(Component.translatable("keybindprofiles.load"),
				b -> this.loadSelected()).width(96).build());
		top.addChild(Button.builder(Component.translatable("keybindprofiles.save"),
				b -> this.promptSave()).width(96).build());
		top.addChild(Button.builder(Component.translatable("keybindprofiles.newFolder"),
				b -> this.promptNewFolder()).width(96).build());

		LinearLayout bottom = footer.addChild(LinearLayout.horizontal().spacing(8));
		this.renameButton = bottom.addChild(Button.builder(Component.translatable("keybindprofiles.rename"),
				b -> this.promptRename()).width(96).build());
		this.deleteButton = bottom.addChild(Button.builder(Component.translatable("keybindprofiles.delete"),
				b -> this.promptDelete()).width(96).build());
		bottom.addChild(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).width(96).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();

		this.list.reload();
		this.updateButtons();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();

		if (this.list != null) {
			this.list.updateSize(this.width, this.layout);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		if (this.status != null) {
			graphics.centeredText(this.font, this.status, this.width / 2, this.height - FOOTER_HEIGHT - 12, TEXT_COLOUR);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private Component breadcrumb() {
		String rel = ProfileStore.root().relativize(this.dir).toString().replace(File.separatorChar, '/');
		return Component.literal(rel.isEmpty() ? "/" : "/" + rel);
	}

	private void open(Path folder) {
		this.minecraft.gui.setScreen(new ProfileScreen(this, this.options, folder));
	}

	private void updateButtons() {
		Row selected = this.list == null ? null : this.list.getSelected();

		this.loadButton.active = selected != null && selected.kind == Kind.PROFILE;
		this.renameButton.active = selected != null;
		this.deleteButton.active = selected != null;
	}

	private void loadSelected() {
		Row selected = this.list.getSelected();

		if (selected == null || selected.kind != Kind.PROFILE) {
			return;
		}

		try {
			int skipped = ProfileStore.read(selected.target).applyTo(this.options);
			String name = ProfileStore.displayName(selected.target);
			this.status = skipped == 0
					? Component.translatable("keybindprofiles.status.loaded", name)
					: Component.translatable("keybindprofiles.status.loadedPartly", name, skipped);
		} catch (IOException e) {
			KeybindProfiles.LOG.error("Could not load profile {}", selected.target, e);
			this.status = FAILED;
		}
	}

	private void promptSave() {
		this.minecraft.gui.setScreen(new NamePromptScreen(this,
				Component.translatable("keybindprofiles.prompt.save"), "", this::save));
	}

	private void save(String name) {
		Path file = ProfileStore.fileFor(this.dir, name);

		if (Files.exists(file)) {
			this.minecraft.gui.setScreen(new ConfirmScreen(ok -> {
				if (ok) {
					this.write(file, name);
				}

				this.minecraft.gui.setScreen(this);
			}, CONFIRM_TITLE, Component.translatable("keybindprofiles.confirm.overwrite", name)));
			return;
		}

		this.write(file, name);
	}

	private void write(Path file, String name) {
		try {
			ProfileStore.write(file, Profile.capture(this.options));
			this.status = Component.translatable("keybindprofiles.status.saved", name);
		} catch (IOException e) {
			KeybindProfiles.LOG.error("Could not save profile {}", file, e);
			this.status = FAILED;
		}

		this.list.reload();
		this.updateButtons();
	}

	private void promptNewFolder() {
		this.minecraft.gui.setScreen(new NamePromptScreen(this,
				Component.translatable("keybindprofiles.prompt.folder"), "", this::newFolder));
	}

	private void newFolder(String name) {
		try {
			ProfileStore.createFolder(this.dir, name);
		} catch (FileAlreadyExistsException e) {
			this.status = Component.translatable("keybindprofiles.status.exists", name);
		} catch (IOException e) {
			KeybindProfiles.LOG.error("Could not create folder {} in {}", name, this.dir, e);
			this.status = FAILED;
		}

		this.list.reload();
		this.updateButtons();
	}

	private void promptRename() {
		Row selected = this.list.getSelected();

		if (selected == null) {
			return;
		}

		Path target = selected.target;
		this.minecraft.gui.setScreen(new NamePromptScreen(this,
				Component.translatable("keybindprofiles.prompt.rename"),
				ProfileStore.displayName(target),
				name -> this.rename(target, name)));
	}

	private void rename(Path target, String name) {
		Path dest = Files.isDirectory(target)
				? target.resolveSibling(name)
				: ProfileStore.fileFor(this.dir, name);

		try {
			Files.move(target, dest);
		} catch (FileAlreadyExistsException e) {
			// Typing the name of something that is already there is an ordinary slip, not a bug,
			// so say what happened instead of dumping it in the log.
			this.status = Component.translatable("keybindprofiles.status.exists", name);
		} catch (IOException e) {
			KeybindProfiles.LOG.error("Could not rename {} to {}", target, name, e);
			this.status = FAILED;
		}

		this.list.reload();
		this.updateButtons();
	}

	private void promptDelete() {
		Row selected = this.list.getSelected();

		if (selected == null) {
			return;
		}

		Path target = selected.target;
		String key = selected.kind == Kind.FOLDER
				? "keybindprofiles.confirm.deleteFolder"
				: "keybindprofiles.confirm.deleteProfile";

		this.minecraft.gui.setScreen(new ConfirmScreen(ok -> {
			if (ok) {
				this.delete(target);
			}

			this.minecraft.gui.setScreen(this);
		}, CONFIRM_TITLE, Component.translatable(key, ProfileStore.displayName(target))));
	}

	private void delete(Path target) {
		try {
			ProfileStore.delete(target);
		} catch (IOException e) {
			KeybindProfiles.LOG.error("Could not delete {}", target, e);
			this.status = FAILED;
		}
	}

	private enum Kind {
		FOLDER,
		PROFILE
	}

	private class EntryList extends ObjectSelectionList<Row> {
		EntryList(Minecraft minecraft) {
			super(minecraft, ProfileScreen.this.width,
					ProfileScreen.this.layout.getContentHeight(),
					ProfileScreen.this.layout.getHeaderHeight(), 18);
		}

		@Override
		public int getRowWidth() {
			return super.getRowWidth() + 60;
		}

		@Override
		public void setSelected(Row selected) {
			super.setSelected(selected);
			ProfileScreen.this.updateButtons();
		}

		void reload() {
			List<Row> rows = new ArrayList<>();
			Path here = ProfileScreen.this.dir;

			try {
				for (Path p : ProfileStore.folders(here)) {
					rows.add(new Row(p, Kind.FOLDER, ProfileStore.displayName(p)));
				}

				for (Path p : ProfileStore.profiles(here)) {
					rows.add(new Row(p, Kind.PROFILE, ProfileStore.displayName(p)));
				}
			} catch (IOException e) {
				KeybindProfiles.LOG.error("Could not read {}", here, e);
				ProfileScreen.this.status = FAILED;
			}

			this.replaceEntries(rows);
		}
	}

	private class Row extends ObjectSelectionList.Entry<Row> {
		private final Path target;
		private final Kind kind;
		private final String name;

		Row(Path target, Kind kind, String name) {
			this.target = target;
			this.kind = kind;
			this.name = name;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			int colour = this.kind == Kind.PROFILE ? TEXT_COLOUR : FOLDER_COLOUR;
			String label = this.kind == Kind.FOLDER ? this.name + "/" : this.name;
			graphics.text(ProfileScreen.this.font, label, this.getContentX() + 4, this.getContentYMiddle() - 4, colour);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			ProfileScreen.this.list.setSelected(this);

			if (doubleClick) {
				this.activate();
			}

			return super.mouseClicked(event, doubleClick);
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (event.isSelection()) {
				ProfileScreen.this.list.setSelected(this);
				this.activate();
				return true;
			}

			return super.keyPressed(event);
		}

		private void activate() {
			switch (this.kind) {
				case FOLDER -> ProfileScreen.this.open(this.target);
				case PROFILE -> ProfileScreen.this.loadSelected();
			}
		}

		@Override
		public Component getNarration() {
			String key = this.kind == Kind.FOLDER
					? "keybindprofiles.narrate.folder"
					: "keybindprofiles.narrate.profile";

			return Component.translatable(key, this.name);
		}
	}
}
