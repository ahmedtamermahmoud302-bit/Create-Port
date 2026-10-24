# Working block functions: 0.3.0-dev

Thirteen blocks are registered with the behavior below. The remaining Create model
resources do not register or implement the other source blocks.

| Block | Implemented behavior |
| --- | --- |
| Creative Motor | Rotational source, initially 16 RPM. Right-click with redstone to step through 0–256 RPM; sneak-right-click reverses. Capacity is 16,384 SU per absolute RPM. Creative-only source. |
| Shaft | Transfers rotation unchanged between its two aligned axle ends. |
| Cogwheel | Transfers axle rotation; adjacent small cogs reverse direction at equal RPM. |
| Large Cogwheel | A large-to-small mesh doubles RPM and reverses direction; small-to-large halves RPM. Perpendicular large cogs turn a corner. |
| Andesite Encased Shaft | Transfers rotation along its axle inside an andesite casing. Craftable from a shaft and casing. |
| Brass Encased Shaft | The same transmission behavior with its brass casing model. Available in creative; brass production/its survival acquisition are not yet ported. |
| Clutch | Transmits normally; redstone disconnects its two shaft ports, stopping an unpowered output while leaving the input network free to run. |
| Gearshift | Transmits normally; redstone reverses rotation between its two ports. |
| Gearbox | Routes rotation through the four faces perpendicular to its selected axis. Opposite ports reverse rotation; corner-port signs follow Create's gearbox rules. |
| Hand Crank | Right-click/hold to drive at 32 RPM in short renewable bursts, with 256 SU capacity. Sneak-right-click reverses. Its output is behind the handle. |
| Water Wheel | Uses vanilla liquid flow around the wheel to generate ±8 RPM and 256 SU. Dry or balanced flow stops it. The actual water-flow drive and shaft output are tested. |
| Millstone | Processes items using rotational power from its bottom shaft. Draws 4 SU per absolute RPM, accepts inputs at the top/sides and exposes outputs below. Contains 34 compatible source milling recipes with their timings/chances. |
| Andesite Casing | Placeable/minable casing block used by the transmission and millstone recipes. Use andesite alloy on a vanilla log to make it; survival use consumes one alloy. |

## Rotational energy

Capacity and demand scale with absolute RPM. Each connected rotational component
gets its own stress budget. A 32 RPM hand crank provides 256 SU; a millstone at
32 RPM requires 128 SU. Gearing that millstone to 128 RPM requires 512 SU, so that
single-crank network stalls. Removing the excessive load restores rotation.
Multiple compatible sources contribute capacity; incompatible speeds or contradictory
gear loops stop the affected component. A powered clutch separates the mechanical
ports rather than introducing a zero gear ratio.

Right-click a kinetic block to read RPM and demand/capacity in SU. Overloaded
components display OVERSTRESSED. Machine processing pauses at zero RPM/overload.
The source-speed configuration and millstone inventory/progress persist in world saves;
network speed/stress are recomputed after loading and synchronized to clients.

## A usable survival processing chain

1. Craft andesite alloy from two iron nuggets and two andesite blocks in the source's diagonal pattern.
2. Use alloy on a log to obtain andesite casing. This is the 1.12 adaptation; stripped logs do not exist in vanilla 1.12.
3. Craft shafts from alloy, cogs from shafts/planks, and a hand crank from alloy/planks.
4. Craft the millstone from a cog, casing and stone; connect a shaft to its underside.
5. Right-click the millstone with wheat to insert it. Drive it using the crank or a water wheel/gearbox arrangement.
6. Empty-hand right-click extracts outputs; hoppers can insert and extract through the appropriate faces.
7. Craft flour plus a water bucket into dough; the empty bucket is returned. Smelt dough into bread.

The 34 milling recipes are translated from the supplied source, including result
chances and timing. Thirteen source recipes are skipped because their input does not
exist in 1.12 or a flattened dye mapping would create a self-duplication loop; see
MILLING-IMPORT.json. Input/output capabilities and inventory persistence are tested.

## Remaining gaps

This is still not full Create gameplay. Belts, fans, presses, mixers, other processing
machines, fluid plumbing, survival brass production, configurable valve handles,
contraptions, trains, stress UI/goggles and many other blocks remain unimplemented.
Water-wheel structural clearance/collision, full wrench interactions and exact
survival acquisition of every variant remain incomplete. Topology is polled once per
second with immediate refreshes for manual/redstone interactions, and networks are
capped at 512 discovered blocks. Addons remain deferred until the main mod is complete.
