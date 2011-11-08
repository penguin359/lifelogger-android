package org.northwinds.photocatalog;

import android.app.Application;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

@ReportsCrashes(formKey = "dGExM01WVDVDSG5Ja1c0aXY1Wm9qTUE6MQ",
		mode = ReportingInteractionMode.TOAST,
		forceCloseDialogAfterToast = true,
		resToastText = R.string.crash_toast_text)
public class LifeApplication extends Application {
	private static final String TAG = "PhotoCatalog-LifeApplication";

	private int mTrackerCount = 0;
	private GoogleAnalyticsTracker mTracker = null;

	GoogleAnalyticsTracker getTrackerInstance() {
		mTrackerCount++;

		if(mTracker == null) {
			mTracker = GoogleAnalyticsTracker.getInstance();
			mTracker.startNewSession(
			    getString(R.string.analytics_id), 20, this);
		}

		return mTracker;
	}

	boolean putTrackerInstance() {
		mTrackerCount--;

		if(mTrackerCount < 0) {
			Log.e(TAG, "Analytics Tracker freed too many times.");
			mTrackerCount = 0;
		}
		if(mTrackerCount == 0 && mTracker != null) {
			mTracker.stopSession();
			mTracker = null;

			return true;
		}

		return false;
	}

	@Override
	public void onCreate() {
		ACRA.init(this);
		super.onCreate();
	}
}
