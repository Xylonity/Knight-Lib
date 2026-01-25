package dev.xylonity.knightlib.api.scheduler;

import net.minecraft.world.level.Level;

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
    private static final Map<Level, Long> LEVEL_TICK_COUNTER = new ConcurrentHashMap<>();

    private static final List<Level> LEVELS_TO_CLEAN = new ArrayList<>();

    public static void schedule(Level level, Runnable runnable, int delay, byte type) {
        if (level == null || runnable == null || delay < 0) return;

        ScheduledTask task = new ScheduledTask(getOrCreateTickCounter(level) + delay, runnable);
        switch (type) {
            case SERVER:
                if (!level.isClientSide()) getOrCreateQueue(SERVER_TASKS, level).add(task);
                break;
            case CLIENT:
                if (level.isClientSide()) getOrCreateQueue(CLIENT_TASKS, level).add(task);
                break;
            case BOTH:
                getOrCreateQueue(COMMON_TASKS, level).add(task);
                break;
            default:
                break;
        }

    }

    /**
     * Schedule in time a task on the server
     * @param level the level where the task is scheduled
     * @param runnable the executable task
     * @param delay the delay in ticks
     */
    public static void scheduleServer(Level level, Runnable runnable, int delay) {
        schedule(level, runnable, delay, SERVER);
    }

    /**
     * Schedule in time a task on the client
     * @param level the level where the task is scheduled
     * @param runnable the executable task
     * @param delay the delay in ticks
     */
    public static void scheduleClient(Level level, Runnable runnable, int delay) {
        schedule(level, runnable, delay, CLIENT);
    }

    /**
     * Schedule in time a task on both sides
     * @param level the level where the task is scheduled
     * @param runnable the executable task
     * @param delay the delay in ticks
     */
    public static void scheduleBoth(Level level, Runnable runnable, int delay) {
        schedule(level, runnable, delay, BOTH);
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
                    LEVEL_TICK_COUNTER.remove(level);
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

    private static long getOrCreateTickCounter(Level level) {
        return LEVEL_TICK_COUNTER.computeIfAbsent(level, lvl -> 0L);
    }

    public static void incrementTick(Level level) {
        LEVEL_TICK_COUNTER.put(level, getOrCreateTickCounter(level) + 1);
    }

    public static void processServerTasks(Level level) {
        PriorityQueue<ScheduledTask> queue = SERVER_TASKS.get(level);

        if (queue == null || queue.isEmpty()) return;

        while (!queue.isEmpty()) {
            ScheduledTask top = queue.peek();
            if (top.execAt > getOrCreateTickCounter(level)) break;

            queue.poll();
            top.execute();
        }

    }

    public static void processClientTasks(Level level) {
        PriorityQueue<ScheduledTask> queue = CLIENT_TASKS.get(level);

        if (queue == null || queue.isEmpty()) return;

        while (!queue.isEmpty()) {
            ScheduledTask top = queue.peek();

            if (top.execAt > getOrCreateTickCounter(level)) break;

            queue.poll();
            top.execute();
        }

    }

    public static void processCommonTasks(Level level) {
        PriorityQueue<ScheduledTask> queue = COMMON_TASKS.get(level);

        if (queue == null || queue.isEmpty()) return;

        while (!queue.isEmpty()) {
            ScheduledTask top = queue.peek();

            if (top.execAt > getOrCreateTickCounter(level)) break;

            queue.poll();
            top.execute();
        }

    }

    public static final class ScheduledTask implements Comparable<ScheduledTask> {
        private final long execAt;
        private final Runnable runnable;

        private ScheduledTask(long executeAtTick, Runnable runnable) {
            this.execAt = executeAtTick;
            this.runnable = runnable;
        }

        @Override
        public int compareTo(ScheduledTask other) {
            return Long.compare(this.execAt, other.execAt);
        }

        public void execute() {
            runnable.run();
        }

    }

}