# Main Create port: initial source mapping

Upstream source is checked out at /workspace/create-upstream, commit ac0c444d9828da3453ae8cc65338e8de063286fb, matching the uploaded Create 6.0.10 manifest. This is a reference checkout, not the 1.12.2 implementation. Do not copy its NeoForge build configuration into the target unchanged.

Initial source review identified these migration boundaries:

| Upstream component | Forge 1.12.2 work |
| --- | --- |
| KineticBlock / ShaftBlock / CogWheelBlock | Legacy block registry events, IBlockState properties, metadata round trips, ItemBlock registration |
| KineticBlockEntity | TileEntity lifecycle, NBTTagCompound serialization and update packets |
| RotationPropagator | EnumFacing/BlockPos/IBlockState access; replace modern Java pattern matching and world APIs |
| KineticNetwork | World-owned graph lifecycle, unloaded chunk boundaries, stress propagation |
| CreativeMotorBlockEntity | Authoritative server source speed and client synchronization |
| KineticBlockEntityRenderer and visuals | TileEntitySpecialRenderer baseline; modern Flywheel is not directly usable |
| Registrate registration | Forge 1.12.2 registry events and explicit model registration |

Rotation semantics observed in upstream RotationPropagator: shafts preserve speed, adjacent equal small gears reverse direction, large-to-small gears multiply by -2, and small-to-large gears multiply by -0.5. These are only part of the full connectivity rules; chain drives, gearboxes and controllers need additional handling.

First playable acceptance checks: motor powers aligned shafts; equal gears reverse; valid large/small gear placement changes speed; removal clears downstream power; conflicting loops are handled; chunks and world reload preserve behavior; dedicated-server synchronization works; client displays rotating components.

Current source reference: the user subsequently supplied Create-mc1.21.1-dev.zip, with mod_version 6.0.11. It is extracted at /workspace/create-uploaded-source/Create-mc1.21.1-dev. SOURCE.json records its fingerprint; it differs from the original uploaded 6.0.10 binary. Neither source reference is itself a 1.12.2 port.

Current implementation: src/main/java/dev/createport contains a Java 8 rotation constraint solver and experimental Forge 1.12.2 adapters. The 12 standalone solver checks and 173 placement/reciprocal checks pass. World integration, static prototype assets and packets remain unvalidated in Minecraft.

Current validation: the main README and docs/VALIDATION.md describe the rendering milestone. The Forge build/reobfuscation, 185 kinetic/placement checks, full model-reference audit and native client model bake pass. The converted source includes all 1,901 block model files: 1,874 concrete models bake without missing sprites and 27 templates retain their unbound slots. The four active blocks use supplied Create geometry; shafts and cogs have interpolated rotating TESRs.

An actual integrated client/server world verified powered rotation, client/server synchronization, source removal, stopping and reverse drive. Screenshots and results are in docs/render-validation. Main Create functionality and specialized machine renderers remain incomplete; resources do not register the other source blocks. Dedicated-server testing still requires explicit Minecraft EULA agreement; eula=false remains unchanged.

Current gameplay milestone: see ENERGY.md and BLOCK-FUNCTIONS.md. The prototype now has 13 blocks with port-based kinetic connections, source capacity, machine stress and overload stalling, plus 34 source-derived milling recipes. Native 0.3.0-dev energy/world tests pass; the full main mod remains incomplete.
