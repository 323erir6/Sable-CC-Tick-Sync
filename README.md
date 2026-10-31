# Sable CC Tick Sync 1.6.3

Server-side NeoForge 1.21.1 addon for Sable 2.0.5 and CC:Tweaked 1.120.0.

## 1.6.3 native physics safety

NeoPeripheral 1.4ve's Sable Engine exposes synchronous Lua methods that access
Rapier directly from CC worker threads. These calls now yield and run on the
server between physics steps through the existing bounded queue. Lua arguments,
results and errors are preserved; unrelated peripherals retain their scheduling.
There is no blocking wait on the CC worker and no cache of stale body velocities.

Queue service no longer takes the registry lock for each task. Closed-computer
queue entries cannot be confused with a reopened computer using the same ID,
and an exceptional task cannot strand the remaining queue.

Run `gradlew regressionTest` to test dispatch, return values, backpressure,
close/reopen isolation, task failure recovery and 20,000 calls from 100 producers.
This is a scheduler regression test, not a Minecraft TPS benchmark. A game
restart and live profiling are required to validate the native freeze fix.

Every CC:Tweaked computer is automatically advanced once per real Sable physics
substep, regardless of whether it is in the world, in a turtle or on a Sable
physical construction. There is no manual frequency switch, configurable clock
or artificial Lua instruction limiter.

## Tick domains

- Lua/executor work on every computer advances at Sable physics frequency.
- Redstone input polling, CC timers and other Minecraft-tick state advance
  once per Minecraft tick (20 TPS), even when Sable has more physics substeps.
- Main-thread calls from mod peripherals may be serviced on physics substeps
  through a bounded shared queue.
- Built-in CC peripherals whose backing state is tied to Minecraft ticks stay
  on CC:Tweaked's normal 20 TPS main-thread queue.
- Tom's Peripherals GPU/monitor operations stay on that same 20 TPS queue so
  monitor touch and mouse events cannot race Sable's pre-physics substeps.
- CC:Tweaked's ordinary server tick is retained for reliable external-event,
  attachment, timer and redstone delivery; one matching Sable substep is
  skipped so the total computer tick rate still follows Sable physics.
- Closing or removing a computer clears its pending accelerated peripheral
  work.

## Read-only Lua status API

The old `sableSync.setHighFrequency()` frequency switch has been removed.
Acceleration is always enabled for every running computer.

```lua
print(sableSync.isHighFrequency())
print(sableSync.getPhysicsFrequency())
```
