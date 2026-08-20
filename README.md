# Sable CC Tick Sync

Server-side NeoForge 1.21.1 addon for Sable 2.0.5 and CC:Tweaked 1.120.0.

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

The enabled mode adds exactly **+100 CC ticks per simulated second** on top of
the computer's normal Sable-synchronised rate. Examples:

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

The API is only injected when the computer starts on a Sable construction. If a
computer was already running before the construction was assembled, reboot that
computer after assembly. If the computer leaves the construction, high-frequency
mode is automatically disabled.
