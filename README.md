# Sable CC Tick Sync

Server-side NeoForge 1.21.1 addon for Sable 2.0.3 and CC:Tweaked 1.120.0.

Computers placed on Sable physical constructions tick once per Sable physics
substep. Computers in the normal Minecraft world retain the standard CC:Tweaked
tick rate.

With Sable's default `substepsPerTick = 2`, construction computers run at 40
computer ticks per second while the server runs at 20 TPS. Changing Sable's
substep count automatically changes their rate without restarting or editing
this mod.
