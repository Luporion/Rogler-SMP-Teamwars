# Save compatibility v0.5.4

SavedData key `roglerteamwars_infrastructure` and TeamRecord / Items list schema stay unchanged. Existing 9 serialized inventory slots (0..8) are loaded into a newly allocated 27-slot inventory (0..26). New slots are empty. Existing energy, Create, armor, token, team ownership, Grace, FTB claims and raids remain untouched. On first new world-save, all nonempty slots up to 26 are serialized. A v0.5.3 downgrade would ignore new slots; take a complete world backup and don't downgrade without manual extraction. No item capabilities are exposed to untrusted pipes.
