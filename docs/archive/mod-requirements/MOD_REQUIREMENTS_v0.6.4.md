# v0.6.4 requirements

UNCHANGED REQUIRED: Minecraft 1.21.1, NeoForge 21.1.249, Create 6.0.10, FTB Teams + FTB Chunks. Internal mod namespace remains `roglerteamwars`. Optional Sable 2.0.6 enables the targeted lifecycle preflight. Optional Sable: Destructive is NOT required and is NOT recommended as default in this pack; if detected the Rogler guard disables itself rather than patching the same lifecycle. New optional dependencies: NONE. If Sable is not installed, its Mixin target is declared optional via @Pseudo and the guard remains inactive. No separate compat jar.
