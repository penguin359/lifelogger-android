/*
 * Copyright (c) 2011, Loren M. Lang
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.northwinds.photocatalog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener ;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

class LifeAnalyticsTracker {
	private static final String TAG = "PhotoCatalog-LifeAnalyticsTracker";

	private Context mContext;
	private int mTrackerCount = 0;
	private GoogleAnalyticsTracker mTracker = null;

	private void startSession() {
		if(mTracker == null) {
			mTracker = GoogleAnalyticsTracker.getInstance();
			mTracker.startNewSession(
			    mContext.getString(R.string.analytics_id), 20,
			    mContext);
		}
	}

	private void stopSession() {
		if(mTracker != null) {
			mTracker.stopSession();
			mTracker = null;
		}
	}

	private SharedPreferences mPrefs = null;

	private final OnSharedPreferenceChangeListener mPrefsChange =
	    new OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(
		    SharedPreferences sharedPreferences,
		    String key) {
			if(key.equals("analytics") && mTrackerCount > 0) {
				if(mPrefs.getBoolean("analytics", false))
					startSession();
				else
					stopSession();
			}
		}
	};

	public LifeAnalyticsTracker(Context context) {
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mPrefs.registerOnSharedPreferenceChangeListener(mPrefsChange);
	}

	public LifeAnalyticsTracker alloc() {
		mTrackerCount++;

		if(mPrefs.getBoolean("analytics", false))
			startSession();

		return this;
	}

	public boolean release() {
		mTrackerCount--;

		if(mTrackerCount < 0) {
			Log.e(TAG, "Analytics Tracker freed too many times.");
			mTrackerCount = 0;
		}
		if(mTrackerCount == 0)
			stopSession();

		return mTracker == null;
	}

	public void dispatch() {
		if(mTracker == null)
			return;
		mTracker.dispatch();
	}

	public void trackEvent(String category, String action,
			       String label, int value) {
		if(mTracker == null)
			return;
		mTracker.trackEvent(category, action, label, value);
	}

	public void trackPageView(String url) {
		if(mTracker == null)
			return;
		mTracker.trackPageView(url);
	}
}
