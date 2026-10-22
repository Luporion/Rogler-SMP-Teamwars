# v0.7.2 acceptance checklist

- [ ] Run `scripts/test-core.sh` or `scripts/test-core.ps1` (must include `TeamIntelPolicyTests`).
- [ ] CI P0 Core and NeoForge Build PASS for EXACT HEAD SHA.
- [ ] Staging server and all clients installed same v0.7.2; join without network payload errors.
- [ ] Team Red A, B and Team Blue C: A sees B but never C, B sees A but never C, C sees neither A nor B.
- [ ] Change B's FTB team to Blue while all logged in. Within 2 sec, Red A loses B from HUD; Blue C may now see B.
- [ ] Test stale client when server stops sending packets: no location >10s; logout/reconnect clears cache.
- [ ] Test negative X/Z coordinates, Nether, Overworld spawn distance and basis radius.
- [ ] Test 5 FTB members while Rogler `max_team_members=4` (legacy v0.6.6 fix preserved).
- [ ] Xaero player/entity radar remains disabled; no incompatible public team marker add-on leaking enemies.
- [ ] No change to personal exploration database, FTB claims, BaseCoreSavedData, inventoried cores, inventory.
- [ ] Explicitly verify no Xaero terrain import or waypoint conversion is being claimed.
