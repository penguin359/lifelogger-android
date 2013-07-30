package org.northwinds.photocatalog.tests;

import org.northwinds.photocatalog.DebugActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class org.northwinds.photocatalog.DebugActivityTest \
 * org.northwinds.photocatalog.tests/android.test.InstrumentationTestRunner
 */
public class DebugActivityTest extends ActivityInstrumentationTestCase2<DebugActivity> {

    public DebugActivityTest() {
        super("org.northwinds.photocatalog", DebugActivity.class);
    }

}
