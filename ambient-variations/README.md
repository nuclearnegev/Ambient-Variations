# Ambient Variations (stage 1)

Fabric mod for Minecraft 1.20.1. This stage contains the foundation only:
the hidden setting, the director, persistent world history, and a host debug command.
Nothing visible happens in the world yet.

## Build it (no installs, uses GitHub)
1. Make a free account at github.com and create a new repository (Private is fine).
2. Click "uploading an existing file" and drag in EVERYTHING inside this folder,
   including the hidden ".github" folder. Click "Commit changes".
3. Open the "Actions" tab. A run called "build" starts by itself (about 3-5 minutes).
4. When it has a green check, open the run, scroll to "Artifacts", and download
   "ambientvariations-jar". Unzip it: the file inside is the mod.

If the ".github" folder did not upload: in the repo click Add file > Create new file,
type  .github/workflows/build.yml  as the name, paste the contents of that file from
this folder, and commit. Then Actions > build > Run workflow.

## Install it
- Minecraft 1.20.1 with Fabric Loader 0.14.21 or newer
- Fabric API for 1.20.1 (from Modrinth)
- Put the mod jar and the Fabric API jar in your mods folder.

## Test it
1. Create a NEW world with "Allow Cheats" ON.
2. Optional: More > Game Rules > Miscellaneous > "Ambient World Behavior" (default OFF).
3. In game, run:  /avdebug status
4. Run:  /avdebug force test_ping   (you should get a "[debug] test_ping ran" message)
5. Run:  /avdebug history           (the ping should be listed)
6. Quit and reload the world, run /avdebug history again. It should still be there.

## Debug commands (operators only)
/avdebug status | decide | advance <min> | state <name> | silence <min>
/avdebug force <event> | history | stop | resume | reset

Config file (created on first launch): config/ambientvariations.json
