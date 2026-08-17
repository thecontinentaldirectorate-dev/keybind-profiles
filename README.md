# Keybind Profiles

A client side Fabric mod for Minecraft 26.2. Save your controls as named profiles, switch between
them from the keybinds menu, and sort them into folders.

## Using it

Open Options, then Controls, then Key Binds. There is a "Profiles..." button in the row along the
bottom.

- **Save Current** writes your keys right now into a new profile, in whichever folder you are in.
- **Load** applies the selected profile. Double clicking a profile does the same thing.
- **New Folder** creates a folder where you are. Double click a folder to go in, and use `..` to
  come back out.
- **Rename** and **Delete** work on whatever is selected. Deleting a folder deletes what is in it.

## Where profiles are kept

Everything lives in `config/keybind-profiles`. Folders are ordinary directories and profiles are
ordinary files, so you can move, copy, or hand edit them without the game running:

```json
{
  "key.jump": "key.keyboard.space",
  "key.sneak": "key.keyboard.left.shift"
}
```

Any key a profile does not mention is left alone. That way an old profile will not unbind a mod
you installed after saving it.

## Building

```
./gradlew build
```

The jar ends up in `build/libs`. You need JDK 25.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API is not needed
