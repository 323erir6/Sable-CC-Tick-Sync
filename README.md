# Sable CC Tick Sync

Current mod version: **1.2.0**.

Server-side NeoForge 1.21.1 addon for Sable 2.0.5 and CC:Tweaked 1.120.0.
Build target: Java 21 / NeoForge 21.1.248.

Computers placed on Sable physical constructions tick once per Sable physics
substep. Computers in the normal Minecraft world retain the standard CC:Tweaked
tick rate.

With Sable's default `substepsPerTick = 2`, construction computers run at 40
computer ticks per simulated second while the server runs at 20 TPS. Changing
Sable's substep count automatically changes their base rate.

## Manual high-frequency mode

A computer which starts while it is on an active Sable construction receives a
built-in Lua API named `sableSync`.

Enable the additional update rate with:

```lua
sableSync.setHighFrequency(true)
```

Disable it with:

```lua
sableSync.setHighFrequency(false)
```

The enabled mode adds exactly **+100 CC logical ticks per simulated second** on
top of the computer's normal Sable-synchronised rate. Examples:

- 40 Hz Sable-synchronised base -> 140 Hz total
- 100 Hz Sable-synchronised base -> 200 Hz total
- 200 Hz Sable-synchronised base -> 300 Hz total

The extra rate is accumulated from Sable's physics timestep, so rates which do
not divide evenly into 100 Hz still average exactly +100 Hz.

Additional methods:

```lua
sableSync.isHighFrequency()   -- true/false
sableSync.getExtraFrequency() -- 100 when enabled, 0 when disabled
```

## Physics-step peripheral fast lane

CC:Tweaked normally queues `@LuaFunction(mainThread = true)` calls for its normal
Minecraft main-thread scheduler. In high-frequency mode this mod intercepts only
selected control peripherals and executes their queued main-thread task at Sable
physics-substep boundaries instead.

Supported peripheral types:

- `synaxis_dynamic_motor`
- `compact_flap`
- `Create_RotationSpeedController`

This accelerates calls such as Synaxis `setTarget`, Compact Flap
`setAngle`/`setTilt`, and Create `setTargetSpeed`. Other peripheral types keep
CC:Tweaked's normal scheduling behaviour.

The fast lane is active only when all of these are true:

1. the computer is on an active Sable construction;
2. `sableSync.setHighFrequency(true)` is enabled for that computer;
3. the peripheral type is in the allow-list above;
4. the peripheral method is a normal CC:Tweaked main-thread task.

The mod matches peripherals by their CC:Tweaked type string, so Create, Synaxis,
and NeoPeripheral remain optional runtime integrations rather than hard compile
dependencies.

The API is only exposed when the Lua machine starts while its computer is on a
Sable construction. If a computer was already running before the construction
was assembled, reboot that computer after assembly. If the computer leaves the
construction, high-frequency mode is automatically disabled.
