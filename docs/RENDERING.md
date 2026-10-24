# Model and renderer conversion

The user-provided Create-mc1.21.1-dev.zip (declared version 6.0.11) is the asset reference.
The original upload of the compiled Create mod was 6.0.10. SOURCE.json records provenance.

## Imported resources

- 2,684 source model JSON files, including 1,901 block models.
- 643 source blockstate definitions, converted to 1.12 model path semantics.
- Source texture, OBJ and MTL files, including dynamic partial geometry.
- 85 effective OBJ descriptors and 27 effective composites after inherited colored
  variants are materialized; the source directly declares 43 OBJ and 7 composites.
- Exact referenced post-1.12 vanilla textures and missing model templates from the
  verified official 1.21.1 client, under a compatibility namespace.
- 56 palette-permutation sprites baked for the old texture atlas.

Modern parent retexturing chains and texture aliases are materialized before legacy
model loading. The converter strips unsupported metadata, adapts blockstate model
paths, and preserves display transforms. OBJ descriptors use the old Forge loader
with inherited material bindings and flip-v. Composite children retain SOLID,
CUTOUT, CUTOUT_MIPPED or TRANSLUCENT routing; future blocks must enable their required
chunk render layers. Abstract templates retain their unbound slots and are not baked
as standalone concrete models.

`ModelCatalog` prepares the concrete block-model library and emits model-audit.json
on each client resource reload. The audit uses Minecraft's actual 1.12 blockstate
parser and model baking pipeline, and checks baked quads for missing sprites.
The current audit passes 1,874 concrete models with zero failures and zero missing
sprites; 27 templates remain available as resources.

## Active block renderers

KineticRenderer renders shafts/cogs, generator shafts, directional crank handles, the water wheel, millstone internals and per-port split shafts as TESRs.
It uses interpolated client angles and synchronized RPM, preserves the block axis,
applies cog phase offsets, binds the block atlas, and restores matrix/lighting state.
The creative motor's stationary housing is a normal baked block model. Non-cubic
blocks no longer report full opaque cubes, so neighbours are not incorrectly culled.

The 0.3.0-dev gameplay layer registers 13 blocks; this does not make the other source blocks placeable. Connected texture selection,
animated belts, fluids, train/contraption entities, other machine-specific animation,
collision shapes and breaking overlays still require their own ports. Resource
coverage is distinct from main-mod feature or renderer parity.

## Reimporting

```sh
python3 scripts/import-models.py /workspace/create-uploaded-source/Create-mc1.21.1-dev \
  --minecraft-jar /workspace/.gradle/caches/forge_gradle/minecraft_repo/versions/1.12.2/client.jar \
  --modern-minecraft-jar /workspace/toolchains/minecraft-1.21.1-client.jar
```

The modern client is pinned to its official SHA-1 in the importer. The script writes
generated resources and MODEL-IMPORT.json; edit the conversion source rather than
hand-editing generated model files. Source directories are not modified. Notices for
Create's reserved assets and Mojang's resources are preserved in META-INF.

## Reproducible evidence

`bash scripts/render-check.sh` runs a dedicated integrated-world scene and verifies
server and client shaft/cog speeds at +16/-16/+8 RPM, loss of power, stopped animation,
and reversed drive. It captures powered, stopped and reversed frames and checks for
OpenGL errors. The screenshots in render-validation are direct Minecraft captures.
The helper preserves the launcher exit status and rejects stale result files.
