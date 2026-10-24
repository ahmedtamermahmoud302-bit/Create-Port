# Create for Minecraft 1.12.2: port assessment

Status: early main-mod prototype with imported source models and rotating core renderers. Forge compilation/reobfuscation, 185 standalone checks, resource-reference validation, native model baking and integrated client-world smoke checks pass. Full main-mod functionality is incomplete; dedicated-server testing remains pending EULA agreement.

## Requested target

Minecraft 1.12.2, Forge, Create 6.0.10 functionality and all uploaded addons integrated into the port. Use the uploaded Pollution of the Realms (AdPother) 1.12.2-1.2.25.0 instead of its 1.21.1 release. Keep every uploaded addon in scope; staged implementation does not remove features from the request.

## Verified inputs

All 29 uploaded archives were inspected without executing their code. uploaded-mods.json records SHA-256 fingerprints, top-level mod metadata, license entry names and nested archive names. Fingerprints identify these inputs; they do not establish publisher authenticity. Nested archives have not yet been recursively audited. These binaries are references, not a buildable source tree.

Create requires Minecraft 1.21.1 and NeoForge, and bundles Flywheel, Ponder and Registrate. Aeronautics bundles Aeronautics, Simulated and Offroad. Sable includes additional physics/rendering dependencies. These systems also require backporting or replacement; changing version metadata cannot make their classes compatible with 1.12.2.

The official Create source repository is https://github.com/Creators-of-Create/Create. Read access was verified. Its current HEAD is not automatically the uploaded release source; the uploaded Create manifest records ac0c444d9828da3453ae8cc65338e8de063286fb as its source commit. Other source versions must be matched independently.

## Pollution

Select AdPother-1.12.2-1.2.25.0-build.0673.jar. Its mcmod.info declares Minecraft 1.12.2 and ForgeEndertech as a dependency. The supplied ForgeEndertech-1.21.1-12.1.4.0-NeoForge-build.1161.jar cannot satisfy that target. Obtain and validate a compatible 1.12.2 ForgeEndertech release before a pollution runtime test.

Pollution of Create is separate integration code targeting Create 6 and modern AdPother. Its emissions integration must be rewritten against the old AdPother API once the port has working machines. Retaining old AdPother alone does not implement Create machine emissions.

## Source and asset reuse

Aeronautics bundled LICENSE.md gives its code an MIT license and reserves the listed Aeronautics, Simulated and Offroad assets. A distributable combined port must use permitted or replacement assets, or obtain permission for the reserved assets. Do not assume all addon assets or code use Create’s license. Inspect each source license and included dependency before copying.

The supplied legacy GeckoLib has both mcmod.info and mods.toml with inconsistent or unresolved version fields. Neither its filename nor the modern descriptor alone proves runtime compatibility. Validate its actual 1.12.2 entrypoint and Java bytecode before integration; GeckoLib 3 is not API-compatible with GeckoLib 4.

## Confirmed priority

The user selected main Create first, then addons after the main port is complete. Addon source collection and integration are deferred. The first development milestone is core machinery; completing that milestone does not mean the whole main Create mod is finished.

## Implementation sequence

1. Match source releases and licenses; establish a reproducible Forge 1.12.2 development build and compatible Java toolchain. Only Java 21 availability was checked in the current environment; no legacy build was tested.
2. Implement and validate Create core registration, shaft rotation, cog transmission, persistence and client/server synchronization. First playable check: a power source drives shafts and gears, survives reload, and synchronizes on a dedicated server.
3. Add machine recipes, fluids, item transport and pollution emissions through legacy AdPother; test the exact supported ForgeEndertech pair.
4. Port contraption assembly, movement, collision and rendering; then trains and addon content that depends on them.
5. Port the physics stack and Aeronautics/Offroad/Coasters/Tracks/Shield integrations, alongside cannons and projectile integrations.
6. Integrate all remaining requested addons and test recipes, world saves, networking and combined performance. Keep addon sources/modules separable internally even if the deliverable is bundled.

## Current validation and outstanding work

See VALIDATION.md and RENDERING.md for the latest rendering milestone. Core client-world checks now pass; this is not full Create gameplay parity. Dedicated-server testing remains pending explicit EULA agreement. Addons remain deferred until the main mod is complete.

Gameplay milestone 0.3.0-dev adds rotational stress/capacity, manual/water generation, transmissions, survival casing progression and a working millstone. See BLOCK-FUNCTIONS.md and VALIDATION.md. Remaining main-mod features are not implemented merely by importing their resources.
