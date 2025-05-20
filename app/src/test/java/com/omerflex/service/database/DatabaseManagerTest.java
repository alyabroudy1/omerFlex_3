package com.omerflex.service.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.omerflex.service.concurrent.ThreadPoolManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DatabaseManagerTest {

    private DatabaseManager databaseManager;
    private Context context;

    @Mock
    private MovieDbHelper mockDbHelper;

    @Mock
    private SQLiteDatabase mockDatabase;

    @Mock
    private ThreadPoolManager mockThreadPoolManager;

    @Mock
    private Executor mockExecutor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.application;

        // Reset singletons before each test to ensure a clean state
        ThreadPoolManager.reset();
        DatabaseManager.reset();

        // Set up mocks
        when(mockDbHelper.getReadableDatabase()).thenReturn(mockDatabase);
        when(mockDbHelper.getWritableDatabase()).thenReturn(mockDatabase);

        // Create a real instance for testing
        databaseManager = DatabaseManager.getInstance(context);
    }

    @After
    public void tearDown() {
        // Reset singletons after each test to clean up resources
        ThreadPoolManager.reset();
        DatabaseManager.reset();
    }

    @Test
    public void testGetInstance() {
        // Verify that getInstance returns a non-null instance
        assertNotNull(databaseManager);

        // Verify that getInstance always returns the same instance
        DatabaseManager anotherInstance = DatabaseManager.getInstance(context);
        assertSame(databaseManager, anotherInstance);
    }

    @Test
    public void testGetDbHelper() {
        // Verify that getDbHelper returns a non-null instance
        MovieDbHelper dbHelper = databaseManager.getDbHelper();
        assertNotNull(dbHelper);
    }

    @Test
    public void testExecuteReadSync() {
        // Create a mock operation
        DatabaseManager.DatabaseOperation mockOperation = mock(DatabaseManager.DatabaseOperation.class);

        // Execute the operation synchronously
        databaseManager.executeReadSync(mockOperation);

        // Verify that the operation was executed
        verify(mockOperation).execute(any(SQLiteDatabase.class));
    }

    @Test
    public void testExecuteWriteSync() {
        // Create a mock operation
        DatabaseManager.DatabaseOperation mockOperation = mock(DatabaseManager.DatabaseOperation.class);

        // Execute the operation synchronously
        databaseManager.executeWriteSync(mockOperation);

        // Verify that the operation was executed
        verify(mockOperation).execute(any(SQLiteDatabase.class));
    }

    @Test
    public void testExecuteRead() throws InterruptedException {
        // Create a real operation with a latch to wait for completion
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean operationExecuted = new AtomicBoolean(false);

        DatabaseManager.DatabaseOperation operation = new DatabaseManager.DatabaseOperation() {
            @Override
            public void execute(SQLiteDatabase db) {
                operationExecuted.set(true);
                latch.countDown();
            }
        };

        // Execute the operation asynchronously
        databaseManager.executeRead(operation);

        // Wait for the operation to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Verify that the operation was executed
        assertTrue(operationExecuted.get());
    }

    @Test
    public void testExecuteWrite() throws InterruptedException {
        // Create a real operation with a latch to wait for completion
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean operationExecuted = new AtomicBoolean(false);

        DatabaseManager.DatabaseOperation operation = new DatabaseManager.DatabaseOperation() {
            @Override
            public void execute(SQLiteDatabase db) {
                operationExecuted.set(true);
                latch.countDown();
            }
        };

        // Execute the operation asynchronously
        databaseManager.executeWrite(operation);

        // Wait for the operation to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Verify that the operation was executed
        assertTrue(operationExecuted.get());
    }

    @Test
    public void testExecuteReadWithError() throws InterruptedException {
        // Create a real operation that throws an exception
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean errorHandled = new AtomicBoolean(false);
        final AtomicReference<Exception> caughtException = new AtomicReference<>();

        DatabaseManager.DatabaseOperation operation = new DatabaseManager.DatabaseOperation() {
            @Override
            public void execute(SQLiteDatabase db) {
                throw new RuntimeException("Test exception");
            }

            @Override
            public void onError(Exception e) {
                errorHandled.set(true);
                caughtException.set(e);
                latch.countDown();
            }
        };

        // Execute the operation asynchronously
        databaseManager.executeRead(operation);

        // Wait for the operation to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Verify that the error was handled
        assertTrue(errorHandled.get());
        assertNotNull(caughtException.get());
        assertEquals("Test exception", caughtException.get().getMessage());
    }

    @Test
    public void testExecuteWriteWithError() throws InterruptedException {
        // Create a real operation that throws an exception
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean errorHandled = new AtomicBoolean(false);
        final AtomicReference<Exception> caughtException = new AtomicReference<>();

        DatabaseManager.DatabaseOperation operation = new DatabaseManager.DatabaseOperation() {
            @Override
            public void execute(SQLiteDatabase db) {
                throw new RuntimeException("Test exception");
            }

            @Override
            public void onError(Exception e) {
                errorHandled.set(true);
                caughtException.set(e);
                latch.countDown();
            }
        };

        // Execute the operation asynchronously
        databaseManager.executeWrite(operation);

        // Wait for the operation to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Verify that the error was handled
        assertTrue(errorHandled.get());
        assertNotNull(caughtException.get());
        assertEquals("Test exception", caughtException.get().getMessage());
    }
}
