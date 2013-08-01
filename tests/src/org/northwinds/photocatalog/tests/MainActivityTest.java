package org.northwinds.photocatalog.tests;

import org.northwinds.photocatalog.MainActivity;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.widget.Button;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
	private Button mStartButton;
	private Activity mActivity;

	public MainActivityTest() {
		super("org.northwinds.photocatalog", MainActivity.class);
	}
	/*
	* Sets up the test environment before each test.
	* @see android.test.ActivityInstrumentationTestCase2#setUp()
	*/
	@Override
	protected void setUp() throws Exception {
		/*
		* Call the super constructor (required by JUnit)
		*/

		super.setUp();

		/*
		* prepare to send key events to the app under test by turning off touch mode.
		* Must be done before the first call to getActivity()
		*/

		setActivityInitialTouchMode(false);

		/*
		* Start the app under test by starting its main activity. The test runner already knows
		* which activity this is from the call to the super constructor, as mentioned
		* previously. The tests can now use instrumentation to directly access the main
		* activity through mActivity.
		*/
		mActivity = getActivity();
		mStartButton = (Button)mActivity.findViewById(org.northwinds.photocatalog.R.id.start_but);
	}

	public void testPreconditions() {
		assertEquals("Button text is", "Start", mStartButton.getText());
	}

	public void testHaveFun() {
		mActivity.runOnUiThread(
			new Runnable() {
				public void run() {
					mStartButton.requestFocus();
				}
			}
		);

		this.sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
		//assertTrue("Stop".equals(mStartButton.getText()));
	}

	public void testStateDestroy() {
		// Halt the Activity by calling Activity.finish() on it
		mActivity.finish();

		// Restart the activity by calling ActivityInstrumentationTestCase2.getActivity()
		mActivity = this.getActivity();
	}
}
