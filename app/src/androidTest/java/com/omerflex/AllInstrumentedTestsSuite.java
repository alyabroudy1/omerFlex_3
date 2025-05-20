package com.omerflex;

import com.omerflex.view.ExoplayerMediaPlayerTest;
import com.omerflex.view.MainActivityTest;
import com.omerflex.view.PlaybackActivityTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite that runs all instrumented tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        OmerFlexApplicationTest.class,
        MainActivityTest.class,
        PlaybackActivityTest.class,
        ExoplayerMediaPlayerTest.class
})
public class AllInstrumentedTestsSuite {
    // This class is empty, it's just a holder for the annotations
}