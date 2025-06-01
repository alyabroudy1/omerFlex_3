package com.omerflex.service.concurrent;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.omerflex.OmerFlexApplication;
import com.omerflex.service.config.ConfigManager;
import com.omerflex.service.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized manager for thread pools and executors.
 * Provides optimized thread pools for different types of tasks.
 */
public class ThreadPoolManager {
    private static final String TAG = "ThreadPoolManager";

    // Singleton instance
    private static volatile ThreadPoolManager instance;

    // Thread pools
    private final ExecutorService networkExecutor;
    private final ExecutorService diskExecutor;
    private final ExecutorService lightweightExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    // Main thread handler
    private final Handler mainThreadHandler;

    // Thread statistics
    private final Map<String, ThreadStatistics> threadStats = new HashMap<>();
    private final AtomicInteger totalTasksSubmitted = new AtomicInteger(0);
    private final AtomicInteger totalTasksCompleted = new AtomicInteger(0);

    // Thread monitoring
    private final ScheduledExecutorService monitorExecutor;
    private boolean monitoringEnabled = false;

    private ThreadPoolManager() {
        // Get configuration from ConfigManager
        ConfigManager config = null;
        try {
            config = OmerFlexApplication.getInstance().getConfigManager();
        } catch (IllegalStateException e) {
            Logger.w(TAG, "OmerFlexApplication not available, using default thread pool settings");
        }

        // Use config values or defaults
        final int corePoolSize = (config != null) ?
                config.getInt("thread.core_pool_size", Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4))) :
                Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4));

        final int maxPoolSize = (config != null) ?
                config.getInt("thread.max_pool_size", Runtime.getRuntime().availableProcessors() * 2 + 1) :
                Runtime.getRuntime().availableProcessors() * 2 + 1;

        final int keepAliveSeconds = (config != null) ?
                config.getInt("thread.keep_alive_seconds", 30) :
                30;

        Logger.d(TAG, String.format("Creating thread pools with core=%d, max=%d",
                corePoolSize, maxPoolSize));

        // Create thread pools with custom thread factories
        networkExecutor = createNetworkExecutor(corePoolSize, maxPoolSize, keepAliveSeconds);
        diskExecutor = createDiskExecutor(corePoolSize, maxPoolSize, keepAliveSeconds);
        lightweightExecutor = createLightweightExecutor(corePoolSize);
        scheduledExecutor = createScheduledExecutor(2);

        // Monitoring executor (for thread statistics)
        monitorExecutor = Executors.newSingleThreadScheduledExecutor(
                createThreadFactory("OmerFlex-Monitor", Thread.MIN_PRIORITY)
        );

        // Create main thread handler
        mainThreadHandler = new Handler(Looper.getMainLooper());

        // Start monitoring if configured
        if (config != null && config.getBoolean("thread.enable_monitoring", false)) {
            startMonitoring();
        }
    }

    /**
     * Get the singleton instance of ThreadPoolManager
     * @return ThreadPoolManager instance
     */
    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    /**
     * Create an executor optimized for network operations
     * @return ExecutorService for network operations
     */
    private ExecutorService createNetworkExecutor(int corePoolSize, int maxPoolSize, int keepAliveSeconds) {
        return new MonitoredThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                createThreadFactory("OmerFlex-Network", Thread.NORM_PRIORITY),
                "network"
        );
    }

    /**
     * Create an executor optimized for disk operations
     * @return ExecutorService for disk operations
     */
    private ExecutorService createDiskExecutor(int corePoolSize, int maxPoolSize, int keepAliveSeconds) {
        return new MonitoredThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                createThreadFactory("OmerFlex-Disk", Thread.NORM_PRIORITY - 1),
                "disk"
        );
    }

    /**
     * Create an executor for lightweight tasks
     *
     * @return ExecutorService for lightweight tasks
     */
    private ExecutorService createLightweightExecutor(int corePoolSize) {
        return new MonitoredThreadPoolExecutor(
                corePoolSize,
                corePoolSize,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                createThreadFactory("OmerFlex-Lightweight", Thread.NORM_PRIORITY),
                "lightweight"
        );
    }

    /**
     * Create an executor for scheduled tasks
     *
     * @return ScheduledExecutorService for scheduled tasks
     */
    private ScheduledExecutorService createScheduledExecutor(int corePoolSize) {
        return Executors.newScheduledThreadPool(
                corePoolSize,
                createThreadFactory("OmerFlex-Scheduled", Thread.NORM_PRIORITY)
        );
    }

    /**
     * Create a thread factory with the given prefix
     *
     * @param prefix   Thread name prefix
     * @param priority Thread priority
     * @return ThreadFactory
     */
    @NonNull
    private ThreadFactory createThreadFactory(final String prefix, final int priority) {
        return new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(1);

            @Override
            public Thread newThread(@NonNull final Runnable r) {
                Thread thread = new Thread(r, prefix + "-" + threadCount.getAndIncrement());
                thread.setPriority(priority);
                thread.setUncaughtExceptionHandler((t, e) ->
                        Logger.e(TAG, "Uncaught exception in thread " + t.getName(), e));
                return thread;
            }
        };
    }

    /**
     * Get executor for network operations
     * @return Network executor
     */
    @NonNull
    public Executor getNetworkExecutor() {
        return networkExecutor;
    }

    /**
     * Get executor for disk operations (database, file I/O)
     * @return Disk executor
     */
    @NonNull
    public Executor getDiskExecutor() {
        return diskExecutor;
    }

    /**
     * Get executor for lightweight background tasks
     * @return Lightweight executor
     */
    @NonNull
    public Executor getLightweightExecutor() {
        return lightweightExecutor;
    }

    /**
     * Get executor for scheduled tasks
     * @return Scheduled executor
     */
    @NonNull
    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    /**
     * Get handler for main thread operations
     * @return Main thread handler
     */
    @NonNull
    public Handler getMainThreadHandler() {
        return mainThreadHandler;
    }

    /**
     * Execute a task on the main thread
     * @param runnable Task to execute
     */
    public void executeOnMainThread(@NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainThreadHandler.post(runnable);
        }
    }

    /**
     * Execute a task on the main thread with delay
     * @param runnable Task to execute
     * @param delayMillis Delay in milliseconds
     */
    public void executeOnMainThreadDelayed(@NonNull Runnable runnable, long delayMillis) {
        mainThreadHandler.postDelayed(runnable, delayMillis);
    }

    /**
     * Start thread pool monitoring
     */
    public synchronized void startMonitoring() {
        if (!monitoringEnabled) {
            monitoringEnabled = true;
            monitorExecutor.scheduleAtFixedRate(this::logThreadPoolStatus, 5, 60, TimeUnit.SECONDS);
            Logger.i(TAG, "Thread pool monitoring started");
        }
    }

    /**
     * Stop thread pool monitoring
     */
    public synchronized void stopMonitoring() {
        if (monitoringEnabled) {
            monitoringEnabled = false;
            monitorExecutor.shutdownNow();
            Logger.i(TAG, "Thread pool monitoring stopped");
        }
    }

    /**
     * Log the status of all thread pools
     */
    private void logThreadPoolStatus() {
        StringBuilder status = new StringBuilder("Thread Pool Status:\n");

        if (networkExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) networkExecutor;
            status.append(String.format("Network: active=%d, queue=%d, completed=%d\n",
                    executor.getActiveCount(), executor.getQueue().size(), executor.getCompletedTaskCount()));
        }

        if (diskExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) diskExecutor;
            status.append(String.format("Disk: active=%d, queue=%d, completed=%d\n",
                    executor.getActiveCount(), executor.getQueue().size(), executor.getCompletedTaskCount()));
        }

        if (lightweightExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) lightweightExecutor;
            status.append(String.format("Lightweight: active=%d, queue=%d, completed=%d\n",
                    executor.getActiveCount(), executor.getQueue().size(), executor.getCompletedTaskCount()));
        }

        synchronized (threadStats) {
            for (Map.Entry<String, ThreadStatistics> entry : threadStats.entrySet()) {
                ThreadStatistics stats = entry.getValue();
                status.append(String.format("%s: tasks=%d, avgTime=%.2fms, maxTime=%dms\n",
                        entry.getKey(), stats.taskCount.get(), stats.getAverageExecutionTime(), stats.maxExecutionTime.get()));
            }
        }

        Logger.d(TAG, status.toString());
    }

    /**
     * Get thread pool statistics
     * @return Map of thread pool statistics
     */
    @NonNull
    public Map<String, ThreadStatistics> getThreadStatistics() {
        synchronized (threadStats) {
            return new HashMap<>(threadStats);
        }
    }

    /**
     * Increment the task count for a pool
     *
     * @param poolName Pool name
     */
    void recordTaskStart(String poolName) {
        totalTasksSubmitted.incrementAndGet();
        synchronized (threadStats) {
            // API compatible version of computeIfAbsent
            ThreadStatistics stats = threadStats.get(poolName);
            if (stats == null) {
                stats = new ThreadStatistics();
                threadStats.put(poolName, stats);
            }
            stats.taskCount.incrementAndGet();
        }
    }

    /**
     * Record task completion
     *
     * @param poolName        Pool name
     * @param executionTimeMs Execution time in milliseconds
     */
    void recordTaskCompletion(String poolName, long executionTimeMs) {
        totalTasksCompleted.incrementAndGet();
        synchronized (threadStats) {
            ThreadStatistics stats = threadStats.get(poolName);
            if (stats != null) {
                stats.totalExecutionTime.addAndGet(executionTimeMs);
                stats.updateMaxExecutionTime(executionTimeMs);
            }
        }
    }

    /**
     * Shutdown all thread pools
     */
    public void shutdown() {
        networkExecutor.shutdown();
        diskExecutor.shutdown();
        lightweightExecutor.shutdown();
        scheduledExecutor.shutdown();
        monitorExecutor.shutdown();

        Logger.i(TAG, "All thread pools shut down");
    }

    /**
     * Reset the singleton instance (for testing purposes)
     */
    public static synchronized void reset() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    /**
     * Thread pool executor that monitors task execution time
     */
    private class MonitoredThreadPoolExecutor extends ThreadPoolExecutor {
        private final String poolName;
        private final ThreadLocal<Long> startTime = new ThreadLocal<>();

        MonitoredThreadPoolExecutor(
                int corePoolSize, int maximumPoolSize,
                long keepAliveTime, TimeUnit unit,
                LinkedBlockingQueue<Runnable> workQueue,
                ThreadFactory threadFactory, String poolName) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
            this.poolName = poolName;
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            startTime.set(System.currentTimeMillis());
            recordTaskStart(poolName);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            try {
                long time = System.currentTimeMillis() - startTime.get();
                recordTaskCompletion(poolName, time);
                startTime.remove();

                if (t != null) {
                    Logger.e(TAG, "Task in pool " + poolName + " failed after " + time + "ms", t);
                }
            } finally {
                super.afterExecute(r, t);
            }
        }
    }

    /**
     * Statistics for a thread pool
     */
    public static class ThreadStatistics {
        final AtomicInteger taskCount = new AtomicInteger(0);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        final AtomicLong totalExecutionTime = new AtomicLong(0);
        final AtomicLong maxExecutionTime = new AtomicLong(0);

        /**
         * Get the average execution time in milliseconds
         *
         * @return Average execution time
         */
        public double getAverageExecutionTime() {
            int count = taskCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0;
        }

        /**
         * Update the max execution time if the new time is higher
         *
         * @param executionTimeMs Execution time to check
         */
        void updateMaxExecutionTime(long executionTimeMs) {
            while (true) {
                long currentMax = maxExecutionTime.get();
                if (executionTimeMs <= currentMax ||
                        maxExecutionTime.compareAndSet(currentMax, executionTimeMs)) {
                    break;
                }
            }
        }
    }
}