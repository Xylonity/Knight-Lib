package dev.xylonity.knightlib.api.scheduler;

import dev.xylonity.knightlib.KnightLib;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

public final class TickScheduler {

    private static final byte SERVER = 0;
    private static final byte CLIENT = 1;
    private static final byte BOTH = 2;

    private static final Map<Level, PriorityQueue<ScheduledTask>> SERVER_TASKS = new ConcurrentHashMap<>(4);
    private static final Map<Level, PriorityQueue<ScheduledTask>> CLIENT_TASKS = new ConcurrentHashMap<>(2);
    private static final Map<Level, PriorityQueue<ScheduledTask>> COMMON_TASKS = new ConcurrentHashMap<>(4);

    private static final List<Level> LEVELS_TO_CLEAN = new ArrayList<>();

    public static void schedule(Level level, Runnable runnable, int delay, byte type) {
        scheduleTask(level, null, runnable, delay, 0, type);
    }

    /**
     * Schedule in time a task on the server
     * @param level the level where the task is scheduled
     * @param runnable the executable task
     * @param delay the delay in ticks
     */
    public static void scheduleServer(Level level, Runnable runnable, int delay) {
        scheduleTask(level, null, runnable, delay, 0, SERVER);
    }

    /**
     * Schedule in time a task on the client
     * @param level the level where the task is scheduled
     * @param runnable the executable task
     * @param delay the delay in ticks
     */
    public static void scheduleClient(Level level, Runnable runnable, int delay) {
        scheduleTask(level, null, runnable, delay, 0, CLIENT);
    }

    /**
     * Schedule in time a task on both sides
     * @param level the level where the task is scheduled
     * @param runnable the executable task
     * @param delay the delay in ticks
     */
    public static void scheduleBoth(Level level, Runnable runnable, int delay) {
        scheduleTask(level, null, runnable, delay, 0, BOTH);
    }

    /**
     * Same as {@link #scheduleServer(Level, Runnable, int)} but hands back a cancelable handle
     */
    @Nullable
    public static ScheduledTask scheduleServerTask(Level level, Runnable runnable, int delay) {
        return scheduleTask(level, null, runnable, delay, 0, SERVER);
    }

    /**
     * Same as {@link #scheduleClient(Level, Runnable, int)} but hands back a cancelable handle
     */
    @Nullable
    public static ScheduledTask scheduleClientTask(Level level, Runnable runnable, int delay) {
        return scheduleTask(level, null, runnable, delay, 0, CLIENT);
    }

    /**
     * Same as {@link #scheduleBoth(Level, Runnable, int)} but hands back a cancelable handle
     */
    @Nullable
    public static ScheduledTask scheduleBothTask(Level level, Runnable runnable, int delay) {
        return scheduleTask(level, null, runnable, delay, 0, BOTH);
    }

    /**
     * Schedule a repeating task on the server, so it first runs after the delay ticks and then
     * again every interval ticks until {@link ScheduledTask#cancel()} is called or the level unloads
     */
    @Nullable
    public static ScheduledTask scheduleServerRepeating(Level level, Runnable runnable, int delay, int interval) {
        return scheduleTask(level, null, runnable, delay, Math.max(1, interval), SERVER);
    }

    /**
     * Schedule a repeating task on the client, so it first runs after the delay ticks and then
     * again every interval ticks until {@link ScheduledTask#cancel()} is called or the level unloads
     */
    @Nullable
    public static ScheduledTask scheduleClientRepeating(Level level, Runnable runnable, int delay, int interval) {
        return scheduleTask(level, null, runnable, delay, Math.max(1, interval), CLIENT);
    }

    /**
     * Schedule a task on the server tied to an entity. It silently cancels itself instead of running if the entity has been removed
     */
    @Nullable
    public static ScheduledTask scheduleServer(Entity entity, Runnable runnable, int delay) {
        return entity == null ? null : scheduleTask(entity.level(), entity, runnable, delay, 0, SERVER);
    }

    /**
     * Schedule a repeating task on the server tied to an entity: it stops for good once the entity is removed or {@link ScheduledTask#cancel()} is called
     */
    @Nullable
    public static ScheduledTask scheduleServerRepeating(Entity entity, Runnable runnable, int delay, int interval) {
        return entity == null ? null : scheduleTask(entity.level(), entity, runnable, delay, Math.max(1, interval), SERVER);
    }

    @Nullable
    private static ScheduledTask scheduleTask(Level level, @Nullable Entity owner, Runnable runnable, int delay, int interval, byte type) {
        if (level == null || runnable == null || delay < 0) {
            return null;
        }

        PriorityQueue<ScheduledTask> queue;
        switch (type) {
            case SERVER:
                queue = level.isClientSide() ? null : getOrCreateQueue(SERVER_TASKS, level);
                break;
            case CLIENT:
                queue = level.isClientSide() ? getOrCreateQueue(CLIENT_TASKS, level) : null;
                break;
            case BOTH:
                queue = getOrCreateQueue(COMMON_TASKS, level);
                break;
            default:
                queue = null;
                break;
        }

        if (queue == null) {
            return null;
        }

        final ScheduledTask task = new ScheduledTask(level.getGameTime() + delay, interval, owner, runnable);

        synchronized (queue) {
            queue.add(task);
        }

        return task;
    }

    public static void markForClean(Level level) {
        if (level != null) {
            synchronized (LEVELS_TO_CLEAN) {
                LEVELS_TO_CLEAN.add(level);
            }

        }

    }

    public static void clean() {
        synchronized (LEVELS_TO_CLEAN) {
            if (!LEVELS_TO_CLEAN.isEmpty()) {
                for (Level level : LEVELS_TO_CLEAN) {
                    SERVER_TASKS.remove(level);
                    CLIENT_TASKS.remove(level);
                    COMMON_TASKS.remove(level);
                }

                LEVELS_TO_CLEAN.clear();
            }

        }

    }

    public static boolean hasTasks() {
        return !SERVER_TASKS.isEmpty() || !CLIENT_TASKS.isEmpty() || !COMMON_TASKS.isEmpty();
    }

    private static PriorityQueue<ScheduledTask> getOrCreateQueue(Map<Level, PriorityQueue<ScheduledTask>> map, Level level) {
        return map.computeIfAbsent(level, lvl -> new PriorityQueue<>());
    }

    /**
     * @deprecated the scheduler now follows {@link Level#getGameTime()} directly, so there is no internal counter to advance anymore
     */
    @Deprecated
    public static void incrementTick(Level level) {
        ;;
    }

    public static void processServerTasks(Level level) {
        process(SERVER_TASKS, level);
    }

    public static void processClientTasks(Level level) {
        process(CLIENT_TASKS, level);
    }

    public static void processCommonTasks(Level level) {
        process(COMMON_TASKS, level);
    }

    private static void process(Map<Level, PriorityQueue<ScheduledTask>> map, Level level) {
        final PriorityQueue<ScheduledTask> queue = map.get(level);
        if (queue == null) {
            return;
        }

        final long now = level.getGameTime();

        List<ScheduledTask> due = null;
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peek().execAt <= now) {
                if (due == null) {
                    due = new ArrayList<>();
                }

                due.add(queue.poll());
            }

        }

        if (due == null) {
            return;
        }

        for (final ScheduledTask task : due) {
            try {
                task.execute();
            }
            catch (Throwable throwable) {
                KnightLib.LOGGER.error("Scheduled task threw an exception: ", throwable);
            }

            if (task.shouldRepeat()) {
                task.execAt = now + task.interval;
                synchronized (queue) {
                    queue.add(task);
                }

            }

        }

    }

    public static final class ScheduledTask implements Comparable<ScheduledTask> {

        private long execAt;
        private final int interval;
        @Nullable
        private final Entity owner;
        private final Runnable runnable;
        private volatile boolean cancelled;

        private ScheduledTask(long executeAtTick, int interval, @Nullable Entity owner, Runnable runnable) {
            this.execAt = executeAtTick;
            this.interval = interval;
            this.owner = owner;
            this.runnable = runnable;
        }

        /**
         * Prevents any pending execution (and every future one, for repeating tasks)
         */
        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public int compareTo(ScheduledTask other) {
            return Long.compare(this.execAt, other.execAt);
        }

        public void execute() {
            if (cancelled) {
                return;
            }

            if (owner != null && owner.isRemoved()) {
                cancelled = true;
                return;
            }

            runnable.run();
        }

        private boolean shouldRepeat() {
            return interval > 0 && !cancelled;
        }

    }

}