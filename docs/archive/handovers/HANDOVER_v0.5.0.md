# LLM handover – v0.5.0

1. Read `AGENTS.md`, `docs/PROJECT_STATE.md`, `docs/releases/v0.5.0.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md` before updating gameplay.
2. Git base: v0.4.0 `feat/v0.4.0-update` commit `6d106ebd`. Verify actual Git status before patch. For new changes create a feature branch.
3. v0.5.0: team UUID commands, OP claim audit, forced-all privacy guard, permanent TeamWars UUID registry/explicit orphan adoption, purchased core armor 0/1/2 (=10/15/20 raid HP). Pure deterministic upgrade code in `core/` with no Minecraft imports.
4. Keep legacy OP player-alias commands for usability, but canonical persistent identifiers are UUIDs. Never auto-assign a previous core by name.
5. Never claim FTB party deletion prevented; never claim FTB private network packet masks chunk coordinates. Actual FTB Chunks 2101.1.22 sends hidden coords without claim flag; verify opponent client separately.
6. No global destructive raids, AE2, Create, FE, per-core chunkloading, Xaero tile sharing. `raids_enabled=false` by default.
7. Tests: JDK 21 `scripts/test-core.sh`, GitHub Actions NeoForge compile, Minecraft disposable 2-client + FTB API integration. Mark each separately; no fabricated build outcome.
8. Required: always deliver **full source ZIP** (AGENTS/docs/LLM/scripts/source), **Git patch**, test matrix, mod requirements. User's local patch helper emits an EXACT tracked-source snapshot using `git archive HEAD` including the original GitHub Actions monitor.
