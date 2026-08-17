package dev.continental.keybindprofiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads and writes profiles under config/keybind-profiles.
 *
 * Folders in the UI are plain directories and profiles are plain files, so anything you can do
 * in the game you can also do in a file browser, including moving a profile between folders.
 */
public final class ProfileStore {
	public static final String EXT = ".json";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("keybind-profiles");
	private static final Comparator<Path> BY_NAME =
			Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER);

	// Everything Windows rejects in a file name, plus the control characters. Linux only really
	// cares about the slash, but a profile folder that will not copy to another machine is worse
	// than one that turned down a name with a colon in it.
	private static final Pattern ILLEGAL = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]");

	private ProfileStore() {
	}

	public static Path root() {
		return ROOT;
	}

	public static void createRoot() throws IOException {
		Files.createDirectories(ROOT);
	}

	public static List<Path> folders(Path dir) throws IOException {
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(Files::isDirectory).sorted(BY_NAME).toList();
		}
	}

	public static List<Path> profiles(Path dir) throws IOException {
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(EXT))
					.sorted(BY_NAME)
					.toList();
		}
	}

	public static Path fileFor(Path dir, String name) {
		return dir.resolve(name + EXT);
	}

	public static String displayName(Path path) {
		String name = path.getFileName().toString();
		return name.endsWith(EXT) ? name.substring(0, name.length() - EXT.length()) : name;
	}

	public static Profile read(Path file) throws IOException {
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonObject json = GSON.fromJson(r, JsonObject.class);
			Map<String, String> binds = new LinkedHashMap<>();

			if (json != null) {
				for (Map.Entry<String, com.google.gson.JsonElement> e : json.entrySet()) {
					if (e.getValue().isJsonPrimitive()) {
						binds.put(e.getKey(), e.getValue().getAsString());
					}
				}
			}

			return new Profile(binds);
		} catch (JsonParseException e) {
			// The format is meant to be hand editable, so a broken file is a thing users will
			// actually produce. Turn it into an IOException the screen already knows how to report.
			throw new IOException("Malformed profile " + file.getFileName(), e);
		}
	}

	public static void write(Path file, Profile profile) throws IOException {
		Files.createDirectories(file.getParent());

		try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			GSON.toJson(profile.binds(), w);
		}
	}

	public static void createFolder(Path dir, String name) throws IOException {
		Files.createDirectory(dir.resolve(name));
	}

	public static void delete(Path path) throws IOException {
		if (!Files.isDirectory(path)) {
			Files.delete(path);
			return;
		}

		// Deepest first, because a directory has to be empty before it will go.
		try (Stream<Path> s = Files.walk(path)) {
			for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(p);
			}
		}
	}

	/**
	 * Trims a name typed by the user, or returns null when it cannot be used as a file name.
	 */
	public static String clean(String raw) {
		String name = raw.trim();

		// A trailing dot is fine on Linux and silently dropped on Windows, which would leave the
		// two machines disagreeing about what the profile is called. "." and ".." would climb out
		// of the profiles folder entirely.
		if (name.isEmpty() || name.endsWith(".") || name.length() > 64) {
			return null;
		}

		return ILLEGAL.matcher(name).find() ? null : name;
	}
}
