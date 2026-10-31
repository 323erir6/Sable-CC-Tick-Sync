package dev.sableccticksync;

import dan200.computercraft.api.lua.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class PhysicsDispatchRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Context implements ILuaContext {
        LuaTask pending;
        public long issueMainThreadTask(LuaTask task) {
            pending = task;
            return 1;
        }
    }

    private static void dispatch() throws Exception {
        check(PhysicsPeripheralDispatch.requiresServerThread(
            new io.p2vman.neoperipheral.peripheral.SableEnginePeripheral()), "engine routing");
        check(!PhysicsPeripheralDispatch.requiresServerThread(new Object()), "other routing");
        check(!PhysicsPeripheralDispatch.requiresServerThread(null), "null routing");

        boolean[] escaped = {false};
        IArguments arguments = (IArguments) Proxy.newProxyInstance(
            IArguments.class.getClassLoader(), new Class<?>[]{IArguments.class},
            (proxy, method, args) -> {
                if (method.getName().equals("escapes")) { escaped[0] = true; return null; }
                if (method.getName().equals("get")) return 42.0;
                throw new AssertionError("Unexpected argument operation " + method);
            });
        Context context = new Context();
        AtomicInteger calls = new AtomicInteger();
        MethodResult waiting = PhysicsPeripheralDispatch.defer((target, ctx, pc, a) -> {
            calls.incrementAndGet();
            check(escaped[0], "arguments must escape before execution");
            check(a == arguments, "argument identity");
            return MethodResult.of(a.get(0), "ok", true);
        }, new Object(), context, null, arguments);
        check(escaped[0] && calls.get() == 0, "worker must not execute physics");
        check(waiting.getCallback() != null, "worker must yield, not block");
        context.pending.execute();
        check(calls.get() == 1, "exactly once");
        MethodResult resumed = waiting.getCallback().resume(
            new Object[]{"task_complete", 1L, true});
        check(Arrays.equals(resumed.getResult(), new Object[]{42.0, "ok", true}), "Lua resume values");

        MethodResult failed = PhysicsPeripheralDispatch.defer((t, c, p, a) -> { throw new LuaException("native error"); },
            null, context, null, arguments);
        try { context.pending.execute(); throw new AssertionError("missing error"); }
        catch (LuaException expected) { check(expected.getMessage().equals("native error"), "error"); }
        try {
            failed.getCallback().resume(new Object[]{"task_complete", 1L, false});
            throw new AssertionError("missing Lua error");
        } catch (LuaException expected) { check(expected.getMessage().equals("native error"), "Lua error resume"); }

        PhysicsPeripheralDispatch.defer((t, c, p, a) -> null, null, context, null, arguments);
        check(context.pending.execute() == null, "empty result");
        PhysicsPeripheralDispatch.defer((t, c, p, a) -> MethodResult.pullEvent("test", x -> MethodResult.of()),
            null, context, null, arguments);
        try { context.pending.execute(); throw new AssertionError("lost continuation"); }
        catch (LuaException expected) { check(expected.getMessage().contains("yielding"), "future API guard"); }
    }

    private static void drainUntil(AtomicInteger completed, int target) {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (completed.get() < target && System.nanoTime() < deadline) {
            HighFrequencyController.drainFastTasks();
            Thread.yield();
        }
        check(completed.get() == target, "queue incomplete: " + completed + "/" + target);
    }

    private static void queue() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        check(!HighFrequencyController.enqueueFastTask(-1, calls::incrementAndGet), "unknown computer");
        HighFrequencyController.registerComputer(1);
        for (int n = 0; n < 4096; n++) {
            check(HighFrequencyController.enqueueFastTask(1, calls::incrementAndGet), "capacity");
        }
        check(!HighFrequencyController.enqueueFastTask(1, calls::incrementAndGet), "backpressure");
        drainUntil(calls, 4096);
        HighFrequencyController.removeComputer(1);

        calls.set(0);
        HighFrequencyController.registerComputer(2);
        HighFrequencyController.enqueueFastTask(2, () -> { throw new AssertionError("closed task ran"); });
        HighFrequencyController.removeComputer(2);
        HighFrequencyController.registerComputer(2);
        HighFrequencyController.enqueueFastTask(2, calls::incrementAndGet);
        drainUntil(calls, 1);
        HighFrequencyController.enqueueFastTask(2, () -> { throw new IllegalStateException("test failure"); });
        HighFrequencyController.enqueueFastTask(2, calls::incrementAndGet);
        try { HighFrequencyController.drainFastTasks(); throw new AssertionError("missing task error"); }
        catch (IllegalStateException expected) { /* the next task must remain serviceable */ }
        check(!FastPeripheralCallContext.isOptimizedTask(), "thread marker leak");
        drainUntil(calls, 2);
        HighFrequencyController.removeComputer(2);

        calls.set(0);
        List<Thread> workers = new ArrayList<>();
        Thread server = Thread.currentThread();
        AtomicInteger wrongThread = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        for (int id = 100; id < 200; id++) {
            int computer = id;
            HighFrequencyController.registerComputer(computer);
            Thread worker = new Thread(() -> {
                for (int n = 0; n < 200; n++) {
                    if (!HighFrequencyController.enqueueFastTask(computer, () -> {
                        if (Thread.currentThread() != server) wrongThread.incrementAndGet();
                        calls.incrementAndGet();
                    })) rejected.incrementAndGet();
                }
            });
            workers.add(worker);
            worker.start();
        }
        drainUntil(calls, 20_000);
        for (Thread worker : workers) worker.join(1000);
        check(wrongThread.get() == 0 && rejected.get() == 0, "thread ownership/backpressure");
        for (int id = 100; id < 200; id++) HighFrequencyController.removeComputer(id);
    }

    public static void main(String[] args) throws Exception {
        dispatch();
        queue();
        System.out.println("PASS: deferred physics calls, Lua results/errors, queue capacity, close/reopen, failure recovery, 100 computers / 20000 calls");
    }
}
