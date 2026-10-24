# Block functions and implementation status

Only these four blocks have gameplay implementations in the current prototype.
The other imported Create models are resources for future ports; they do not add
placeable or working blocks.

| Block | Working function | How to use it |
| --- | --- | --- |
| Creative Motor | Generates a fixed 16 RPM rotational drive. | Place its shaft against an aligned shaft or cog. Sneak-right-click to reverse the drive. |
| Shaft | Transfers rotation along its axis without changing speed or direction. | Place end to end along the same axis. |
| Cogwheel | Transfers rotation through its axle; adjacent equal small cogs rotate in opposite directions at the same speed. | Align axles for shaft connections, or place equal cogs beside one another in the plane perpendicular to their axes. |
| Large Cogwheel | Transfers axle rotation, changes gear ratios with a small cog, and can connect to another large cog on a perpendicular axis. | A large cog driving a correctly positioned small cog doubles RPM and reverses direction; the reverse connection halves RPM. A small/large pair with parallel axes connects diagonally in the plane perpendicular to the axes. |

The face clicked during placement selects the shaft axis. Right-click any implemented
block to display its RPM. Two incompatible drives or a contradictory gear loop stop
their connected network. Removing the power source stops the remaining network.
Topology changes are currently polled once per second and discovery is capped at
512 blocks. Production stress and capacity mechanics are not implemented.

The integrated-world test verifies the actual server and client speeds, animations,
source removal, stopped motion and reversed drive. See render-validation for evidence.

## Main-mod functions still to port

- Survival power sources, configurable motor speeds, water wheels, windmills and steam engines.
- Gearboxes, clutches, gearshifts, belts and other transmission components.
- Processing machines, recipe handling, item logistics and fluid transport.
- Redstone control, stress/capacity, contraption assembly, trains and their interactions.
- Registrations and gameplay behavior for the remaining source blocks and their variants.

This document distinguishes verified prototype behavior from future functionality;
the presence of a block's model is not an implementation of that block.
