# Validation record: gameplay and rotational energy (0.3.0-dev)

- Verified Java 8u504-b01, Gradle 4.9, Forge 14.23.5.2860 and ForgeGradle 3.0.197.
- `bash scripts/build.sh`: passed; compilation, reobfuscation and generated Forge runtime verification passed.
- 12 kinetic constraint checks, 803 placement/reciprocal checks and 25 energy/transmission/milling-speed checks passed (840 total).
- 21,168 model/parent/texture/OBJ/blockstate reference checks passed.
- Native client audit: all 643 source blockstate files parse; 1,874 concrete source block models bake with no loader failures or missing-sprite quads. 27 unbound templates are retained.
- `bash scripts/render-check.sh`: passed on 0.3.0-dev, including current-run result validation and a successful Gradle launcher exit.
- Integrated-world core checks: client/server RPM, moving and stopped animation, source removal and reversed drive passed.
- Integrated-world energy checks: redstone clutch disconnect/recovery, gearshift reverse, gearbox ports, flow-driven water wheel and shaft output, alloy/dough/bread recipes, returned bucket and survival log-to-casing conversion passed.
- Millstone checks: 34 compatible source recipes loaded; simulated inventory insert, input/output port restrictions, powered wheat processing, hopper-style extraction and inventory NBT persistence passed.
- Stress checks: crank capacity 256 SU, normal mill demand 128 SU, geared-up demand 512 SU, overload stalling and recovery after removing the excessive load passed. Restoring the load stalls again.
- Four direct game screenshots and machine-readable reports are in render-validation.

The smoke harness shutdown race was fixed by using Minecraft's normal shutdown path rather than clearing its world/player mid-tick. The final native run exits successfully.

Dedicated-server world testing remains unrun; eula=false is preserved pending explicit user agreement. The integrated-world tests do not imply a passed dedicated-server test. Network/source and inventory persistence are tested at the NBT level; complete cross-chunk lifecycle/parity is still unfinished.

Thirteen blocks have implemented behaviors. Most Create blocks, machines, fluid systems, belts, contraptions and trains remain unported; addons and pollution integration are deferred. See BLOCK-FUNCTIONS.md for exact supported behavior and limits.

Output: create-port-1.12.2-0.3.0-dev.jar. This is a development prototype, not a full Create release.
