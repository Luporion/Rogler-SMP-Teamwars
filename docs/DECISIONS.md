# Architecture decisions

## ADR-001 — One Xaero map, no map replacement (2026-10-09)
Accepted. Xaero Minimap + World Map remain player-facing; FTB claims background only. No second map window. Exploration team overlay and revocation are a separate feasibility risk.

## ADR-002 — Per-player provenance, derived teams (2026-10-09)
Accepted. Server stores exploration by UUID/dimension/chunk. Visibility is union of current roster, never copying visited ownership. Avoid destructive changes to personal map.

## ADR-003 — Vanilla Java 21 core, thin NeoForge adapter (2026-10-09)
Accepted. No external Minecraft dependencies in core; regular java tests can run even without online Gradle. Real mod integration must compile/test separately.

## ADR-004 — Separate quotas and server authority (2026-10-09)
Accepted. Core-specific chunkloading is NOT a bypass of total budgets; quotas counted and physically registered centrally. No client-trusted team/core declarations.

## ADR-005 — Fast MVP by Monday (2026-10-09)
Accepted. Team memberships, protected bases, raids and scoreboard outrank perfect shared maps, marketplace and Sable nuke. Features not proven may ship behind disabled flags or be omitted. No sacrificing world integrity for deadline.

## ADR-006 — Real explosion is a transaction, not just a command (2026-10-09)
Accepted in design, pending implementation. Effects separated from damage. Suppress cross-base/neutral-spawn griefing.
