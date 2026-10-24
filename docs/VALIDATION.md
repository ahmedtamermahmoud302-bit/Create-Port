# Validation record: rendering milestone

- Toolchain: verified Temurin Java 8u504-b01, Gradle 4.9, Forge 14.23.5.2860 and ForgeGradle 3.0.197.
- `./gradlew clean build --console=plain`: passed after the rendering changes, including reobfuscation.
- `bash scripts/build.sh`: passed and verified the official two-side Forge enum in the generated runtime.
- `kineticTest`: 12 checks passed; `rotationRulesTest`: 173 checks passed.
- `modelResourceTest`: 21,112 model, parent, texture, OBJ and blockstate reference checks passed.
- Actual Minecraft client: all 643 converted blockstate JSON files parse. 1,874 concrete source block models load and bake with zero failures and zero missing-sprite quads. 27 abstract templates are intentionally not baked alone.
- `bash scripts/render-check.sh --audit-only`: passed and exited successfully, including current-run result validation.
- Client-world smoke: powered animation, stopping, source removal and reverse drive passed; client/server RPM values matched. OpenGL checks passed and three direct screenshots were captured. `bash scripts/render-check.sh` passed on 0.2.0-dev and the Gradle launcher exited successfully; current-run evidence is saved alongside the screenshots.
- Dedicated-server world: still unrun; eula=false is preserved pending the user's explicit agreement. The integrated-world test does not accept or change that file.
- Main Create parity: incomplete. Other block registrations, machine logic, specialized renderers, connected textures, fluids and contraptions remain unimplemented. Addons and pollution integration remain deferred.

The earlier development-runtime crash was traced to the plain MCP merge injecting a three-value Side enum from the merge tool. The dated Forge userdev recipe uses --inject false. The repair helper applies that supported option to derived outputs, preserves prior files and verifies the generated runtime. No publisher checksums or original downloaded artifacts were changed.

The earlier apparent daemon failure after successful game shutdown was ForgeGradle's forceExit option. Client and server run configurations now set forceExit false; client audit and world launch commands exit normally.

Output: build/libs/create-port-1.12.2-0.2.0-dev.jar. This remains an experimental core prototype, not the full Create mod.
