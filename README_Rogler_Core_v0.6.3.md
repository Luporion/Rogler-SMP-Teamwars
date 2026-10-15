# Rogler Core v0.6.3

*Read-only Sable compatibility assessment and configurable, bounded explosion-event observations.*

**No duplicate Sable mixins. Splitting remains enabled.** Compatible with established Rogler-Core registries and saves. For detailed scope, see `docs/releases/v0.6.3.md` and the LLM handover.

Commands (OP level 4): `/rogler admin compat report`, `sable`, `mods`, `explosions`, `reload`.

Create `world/serverconfig/roglercore-sable.properties` with `explosion_observation_enabled=true` **only** if you want event counters in a test profile; default is OFF. Requires v0.6.3 NeoForge CI and actual Minecraft runtime test before deployment to production.
