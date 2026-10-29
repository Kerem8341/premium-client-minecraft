# Premium Client (Fabric, Minecraft 1.21.11)

Client-side base-hunting mod. Needs Fabric Loader 0.18.4+ and Fabric API 0.141.1+1.21.11 in your mods folder.

## Get the jar (no programming needed) - GitHub Actions
1. Make a free GitHub account, click "New repository" (any name), then "uploading an existing file".
2. Drag EVERYTHING from this folder in, including the hidden `.github` folder (if it is hidden, enable "show hidden files").
3. Commit. Open the **Actions** tab -> the "build" run -> download **premium-client-jar** at the bottom.
4. Unzip it and put `premium-client-2.0.0.jar` in `.minecraft/mods`.

## Or build locally
Install JDK 21 + Gradle 9.2.1, then run `gradle build`. The jar is in `build/libs/`.

## Use
Right Shift = menu (toggle modules, tune thresholds). Found bases are saved to `.minecraft/premium_bases.tsv`.
