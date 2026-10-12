# TeamWars v0.5.1 – source patch handoff

*Status:* Java rules offline-tested; NeoForge CI and Minecraft gameplay are NOT verified.

- `docs/releases/v0.5.1.md` – implementation details and explicit exclusions.
- `docs/PROJECT_STATE_v0.5.1.md` – verified/unverified state.
- `docs/HANDOVER_v0.5.1.md` – instructions for future LLM agents.
- `docs/research/CORE_CREATE_AE2_SOPHISTICATED_DESIGN.md` – Create, AE2, Sophisticated architecture and security tradeoffs.

### New commands

- `/teamwars base energy` – buffer and configured FE/t.
- `/teamwars base upgrade energy` – spend materials near own placed core.
- Right click placed core – nine-slot protected GUI. Sneak-right-click still packs.

New per-world config: `<world>/serverconfig/roglerteamwars-core-power.properties` with validated `base_fe_per_tick`, `tier1_fe_per_tick`, `tier2_fe_per_tick` (defaults 20/40/80; reload supported).

See `docs/releases/v0.5.1.md` for limitations and required tests. Apply patch ONLY on v0.5.0 source candidate (not v0.4.0), then run `VERIFY.cmd`, push and watch NeoForge CI. Never use a `core` pure Java JAR as a Minecraft mod.
