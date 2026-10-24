# Create Port for Minecraft 1.12.2

An independent development prototype. **The main Create port is incomplete.**
Main Create comes first; addons and Pollution of the Realms integration remain deferred.

See [block functions](docs/BLOCK-FUNCTIONS.md) for what the four currently implemented
blocks do and how their connections work. Other source blocks remain unimplemented.

## Rendering milestone (0.2.0-dev)

The supplied Create source's 2,684 block/item model files, 643 blockstate definitions,
textures and OBJ/MTL assets have been imported for 1.12.2. Among them are 1,901 block
models: 1,874 concrete models pass an actual client bake with no loader failures or
missing-texture quads; 27 unbound templates are retained for textured descendants.

The four implemented blocks now use supplied Create geometry and textures. Shafts,
small cogs, large cogs and the motor's shaft have interpolated rotating TESRs. The
resource backend supports OBJ geometry and composite models with separate render
layers. These resources do not register the hundreds of unported blocks or implement
machine behavior, connected textures, fluids, contraptions or trains.

Client-world smoke checks passed for powered rotation, source removal, stopping and
reversed drive, including matching client/server speeds. Screenshots and machine-readable
results are in [docs/render-validation](docs/render-validation). The updated prototype
builds as `build/libs/create-port-1.12.2-0.2.0-dev.jar`.

## Build and test

In the cloud checkout:

```sh
bash scripts/bootstrap.sh
bash scripts/build.sh
```

This uses Java 8, Gradle 4.9 and Forge 14.23.5.2860. The build includes 185 kinetic and
placement checks and a full resource-reference audit. The wrapper also corrects the
legacy MCP merge's injected annotation stubs using the official tool's `--inject false`
option, then verifies the generated Forge runtime. Original downloads and checksum
verification remain intact.

For graphical checks, `scripts/render-check.sh` starts the retained Xvfb binary when
DISPLAY is unset, runs a local client test world, verifies current-run results and
stops its own display. `bash scripts/render-check.sh --audit-only` validates the model
bake and exits at the main menu. The full check creates/uses only its dedicated
`run/saves/createport-render-smoke` world.

For normal play testing, set JAVA_HOME=/workspace/toolchains/jdk8u504-b01, prepend
$JAVA_HOME/bin to PATH, set GRADLE_USER_HOME=/workspace/.gradle and run `./gradlew runClient`
with an available display. Prototype blocks appear in the Redstone tab. Placement face
selects the shaft axis; right click displays RPM; sneak-right-click the motor reverses
its fixed 16 RPM drive.

Dedicated-server world testing remains unrun: `run-server/eula.txt` is still false
pending the user's explicit Minecraft EULA agreement. Integrated client-world checks
do not imply a passed dedicated-server test.

## Remaining main-mod work

Stress, production power sources, processing machines, belts, fluids, recipes,
contraptions, trains and other Create features are not implemented. Kinetic discovery
still polls once per second and is capped at 512 blocks. Collision geometry and
machine-specific renderers need further porting. Do not treat this as a complete
Create release.

See [render conversion details](docs/RENDERING.md), [validation](docs/VALIDATION.md),
and the [input inventory](docs/uploaded-mods.json). Source and asset license notices
are preserved in the JAR; asset reuse does not grant redistribution rights.
