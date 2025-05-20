package com.omerflex;

import com.omerflex.entity.MovieTest;
import com.omerflex.service.concurrent.ThreadPoolManagerTest;
import com.omerflex.service.database.DatabaseManagerTest;
import com.omerflex.service.image.ImageLoaderTest;
import com.omerflex.service.network.HttpClientManagerTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite that runs all unit tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        MovieTest.class,
        ThreadPoolManagerTest.class,
        HttpClientManagerTest.class,
        ImageLoaderTest.class,
        DatabaseManagerTest.class
})
public class AllUnitTestsSuite {
    // This class is empty, it's just a holder for the annotations
}