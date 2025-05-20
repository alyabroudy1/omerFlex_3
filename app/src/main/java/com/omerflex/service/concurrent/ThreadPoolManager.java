package com.omerflex.service.concurrent;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized manager for thread pools and executors.
 * Provides optimized thread pools for different types of tasks.
 */
public class ThreadPoolManager {
    private static final String TAG = "ThreadPoolManager";

    // Number of CPU cores
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    // Thread pool sizes
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;

    // Singleton instance
    private static ThreadPoolManager instance;

    // Thread pools
    private final ExecutorService networkExecutor;
    private final ExecutorService diskExecutor;
    private final ExecutorService lightweightExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    // Main thread handler
    private final Handler mainThreadHandler;

    private ThreadPoolManager() {
        // Create thread pools with custom thread factories
        networkExecutor = createNetworkExecutor();
        diskExecutor = createDiskExecutor();
        lightweightExecutor = Executors.newFixedThreadPool(
                CORE_POOL_SIZE, 
                new ThreadFactory() {
                    private final AtomicInteger threadCount = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "OmerFlex-Lightweight-" + threadCount.getAndIncrement());
                        thread.setPriority(Thread.NORM_PRIORITY);
                        return thread;
                    }
                }
        );

        scheduledExecutor = Executors.newScheduledThreadPool(
                2,
                new ThreadFactory() {
                    private final AtomicInteger threadCount = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "OmerFlex-Scheduled-" + threadCount.getAndIncrement());
                        thread.setPriority(Thread.NORM_PRIORITY);
                        return thread;
                    }
                }
        );

        // Create main thread handler
        mainThreadHandler = new Handler(Looper.getMainLooper());
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
    private ExecutorService createNetworkExecutor() {
        return new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private final AtomicInteger threadCount = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "OmerFlex-Network-" + threadCount.getAndIncrement());
                        thread.setPriority(Thread.NORM_PRIORITY);
                        return thread;
                    }
                }
        );
    }

    /**
     * Create an executor optimized for disk operations
     * @return ExecutorService for disk operations
     */
    private ExecutorService createDiskExecutor() {
        return new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private final AtomicInteger threadCount = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "OmerFlex-Disk-" + threadCount.getAndIncrement());
                        thread.setPriority(Thread.NORM_PRIORITY - 1);
                        return thread;
                    }
                }
        );
    }

    /**
     * Get executor for network operations
     * @return Network executor
     */
    public Executor getNetworkExecutor() {
        return networkExecutor;
    }

    /**
     * Get executor for disk operations (database, file I/O)
     * @return Disk executor
     */
    public Executor getDiskExecutor() {
        return diskExecutor;
    }

    /**
     * Get executor for lightweight background tasks
     * @return Lightweight executor
     */
    public Executor getLightweightExecutor() {
        return lightweightExecutor;
    }

    /**
     * Get executor for scheduled tasks
     * @return Scheduled executor
     */
    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    /**
     * Get handler for main thread operations
     * @return Main thread handler
     */
    public Handler getMainThreadHandler() {
        return mainThreadHandler;
    }

    /**
     * Execute a task on the main thread
     * @param runnable Task to execute
     */
    public void executeOnMainThread(Runnable runnable) {
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
    public void executeOnMainThreadDelayed(Runnable runnable, long delayMillis) {
        mainThreadHandler.postDelayed(runnable, delayMillis);
    }

    /**
     * Shutdown all thread pools
     */
    public void shutdown() {
        networkExecutor.shutdown();
        diskExecutor.shutdown();
        lightweightExecutor.shutdown();
        scheduledExecutor.shutdown();
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
}
