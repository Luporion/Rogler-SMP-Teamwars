# v0.7.6 – Reproducible Registrate CI dependency bootstrap

## Failure

GitHub Actions build run `39618333770`, including its rerun, failed **before Java compilation** because
`https://maven.ithundxr.dev/snapshots` returned HTTP 521 for
`com.tterrag.registrate:Registrate:MC1.21-1.3.0+67`.

## Change

The official Create Maven publishes full (non-`slim`) `create-1.21.1-6.0.10-280.jar`.
It embeds `META-INF/jarjar/Registrate-MC1.21-1.3.0+67.jar`.
The user's installed Create 6.0.10 archive exposed that path and a local hash of the extracted binary:

`510F4041C41739F1D8EA8850AB8364D3E3A5FADA8529BEED5D9F479E2523DB52`

`python3 scripts/bootstrap-registrate.py` downloads the version-pinned **full**
Create JAR from `https://maven.createmod.net`, extracts the nested Registrate JAR,
and verifies its SHA-256 and the expected class before creating a temporary
`.ci-maven` repository containing the exact required Gradle group/artifact/version.
No vendor JARs are committed. This is used by GitHub Actions before Gradle.
Gradle consults this repo first, falling back to the original Maven repositories
when the bootstrap has not run. `scripts/test-registrate-bootstrap.py` tests
extraction, checksum rejection, immutability after errors, and version pinning.

A checksum mismatch is intentionally fatal (e.g. if official full Create 280
and the original end-user distribution embed different binary builds). Never
silently change the fingerprint; inspect/check both originals before deciding.

## Optional local bootstrap (Windows)

Use the original installed Create 6.0.10 (non-slim) file:

`python scripts/bootstrap-registrate.py --create-jar "M:\Downloads\create-1.21.1-6.0.10.jar"`

Then build with `gradle -p mod-neoforge build --no-daemon`. The `.ci-maven`
folder is local-only and ignored by Git.

## Scope and release readiness

This changes **no Minecraft code** and does not alter mod_version=0.7.6,
networking, team policy, or gameplay. The NeoForge compile must still pass
and the JAR must still be verified on a dedicated server and two clients.
Do not re-run the old 0.7.6-update patch: use the standalone CI fix commit on
top of `feat/v0.7.6-update`.
