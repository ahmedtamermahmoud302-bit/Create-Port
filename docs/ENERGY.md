# Rotational energy implementation

The kinetic graph has separate shaft-face nodes and one center node per block.
External axle connections join matching face nodes. Gear contacts connect rotational
centers with the source gear ratios. Internal port factors implement clutches,
gearshifts and gearboxes without conflating every face's RPM.

Each connected graph component solves consistent RPM constraints before accounting
for stress. Source capacity is the source's coefficient times its candidate absolute
RPM; consumers contribute their coefficient times candidate absolute RPM. Each source
and machine is counted once at its center. Excessive demand stops that component
while preserving the candidate capacity/demand for diagnostics. Overload does not
remove the generator's configured speed, so the next solve can recover when a load
is removed. A disconnected clutch produces separate graph components and stress budgets.

Current coefficients: creative motor 16,384; crank 8; water wheel 32 SU per RPM.
The millstone impact is 4 SU per RPM, including when its inventory is idle. Power
and overload state plus per-port RPM are synchronized to the client. Rotating split
shafts render their own port speeds, so a disconnected output need not rotate with
its input.

Milling uses the source's integer work rate clamped to 1–512 at nonzero RPM, with
zero work while stopped. Recipes and result chances are imported from the supplied
Create source with explicit legacy ID/metadata translations. Potential outputs are
checked for space before consuming input. Empty-handed extraction, hopper ports,
world-save inventory and container-return crafting are exercised by native tests.

The native energy scene checks 256 SU capacity, 128 SU normal mill demand, 512 SU
geared-up demand, overload, recovery when the load is removed, redstone transmission,
water-driven generation and inventory/recipe behavior. Latest results are saved in
render-validation/energy-smoke.json. These are prototype capabilities; this does not
establish complete Create parity or dedicated-server readiness.
