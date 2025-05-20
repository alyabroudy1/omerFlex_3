package com.omerflex.service.concurrent;

import android.os.Handler;
import android.os.Looper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.annotation.LooperMode.Mode.PAUSED;

@RunWith(RobolectricTestRunner.class)
@LooperMode(PAUSED)
public class ThreadPoolManagerTest {

    private ThreadPoolManager threadPoolManager;
    private ShadowLooper shadowLooper;

    @Before
    public void setUp() {
        // Reset the ThreadPoolManager before each test to ensure a clean state
        ThreadPoolManager.reset();
        threadPoolManager = ThreadPoolManager.getInstance();
        shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
    }

    @After
    public void tearDown() {
        // Reset the ThreadPoolManager after each test to clean up resources
        ThreadPoolManager.reset();
    }

    @Test
    public void testGetInstance() {
        // Verify that getInstance returns a non-null instance
        assertNotNull(threadPoolManager);

        // Verify that getInstance always returns the same instance
        ThreadPoolManager anotherInstance = ThreadPoolManager.getInstance();
        assertEquals(threadPoolManager, anotherInstance);
    }

    @Test
    public void testGetExecutors() {
        // Verify that all executors are properly initialized
        assertNotNull(threadPoolManager.getNetworkExecutor());
        assertNotNull(threadPoolManager.getDiskExecutor());
        assertNotNull(threadPoolManager.getLightweightExecutor());
        assertNotNull(threadPoolManager.getScheduledExecutor());
        assertNotNull(threadPoolManager.getMainThreadHandler());
    }

    @Test
    public void testExecuteOnMainThread() throws InterruptedException {
        // Test executing a task on the main thread
        final AtomicBoolean taskExecuted = new AtomicBoolean(false);

        threadPoolManager.executeOnMainThread(() -> {
            taskExecuted.set(true);
        });

        // Process all posted tasks
        shadowLooper.idle();

        assertTrue(taskExecuted.get());
    }

    @Test
    public void testExecuteOnMainThreadDelayed() throws InterruptedException {
        // Test executing a task on the main thread with delay
        final AtomicBoolean taskExecuted = new AtomicBoolean(false);

        threadPoolManager.executeOnMainThreadDelayed(() -> {
            taskExecuted.set(true);
        }, 1000);

        // Verify task is not executed immediately
        assertFalse(taskExecuted.get());

        // Advance looper by 500ms
        shadowLooper.idleFor(500, TimeUnit.MILLISECONDS);
        assertFalse(taskExecuted.get());

        // Advance looper by another 600ms (total 1100ms)
        shadowLooper.idleFor(600, TimeUnit.MILLISECONDS);
        assertTrue(taskExecuted.get());
    }

    @Test
    public void testExecuteOnExecutors() throws InterruptedException {
        // Test executing tasks on different executors
        testExecutor(threadPoolManager.getNetworkExecutor(), "Network");
        testExecutor(threadPoolManager.getDiskExecutor(), "Disk");
        testExecutor(threadPoolManager.getLightweightExecutor(), "Lightweight");
    }

    private void testExecutor(Executor executor, String name) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean taskExecuted = new AtomicBoolean(false);

        executor.execute(() -> {
            taskExecuted.set(true);
            latch.countDown();
        });

        // Wait for task to complete with timeout
        assertTrue(name + " executor task did not complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue(name + " executor task was not executed", taskExecuted.get());
    }

    @Test
    public void testScheduledExecutor() throws InterruptedException {
        ScheduledExecutorService scheduledExecutor = threadPoolManager.getScheduledExecutor();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean taskExecuted = new AtomicBoolean(false);

        scheduledExecutor.schedule(() -> {
            taskExecuted.set(true);
            latch.countDown();
            return null;
        }, 100, TimeUnit.MILLISECONDS);

        // Wait for task to complete with timeout
        assertTrue("Scheduled task did not complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Scheduled task was not executed", taskExecuted.get());
    }

    @Test
    public void testShutdown() throws InterruptedException {
        // Get the singleton instance for this test
        ThreadPoolManager manager = ThreadPoolManager.getInstance();

        // Execute a task before shutdown
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean taskExecuted = new AtomicBoolean(false);

        manager.getNetworkExecutor().execute(() -> {
            taskExecuted.set(true);
            latch.countDown();
        });

        // Wait for task to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(taskExecuted.get());

        // Shutdown the manager
        manager.shutdown();

        // Note: We can't easily test that new tasks are rejected after shutdown
        // without making the ThreadPoolManager class more testable
    }
}
