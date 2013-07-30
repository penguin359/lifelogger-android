/*
 * Copyright (c) 2010-2011, Loren M. Lang
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class ShortcutActivity extends Activity {
	private static final String ACTION_START_LOG = "org.northwinds.android.intent.START_LOG";
	private static final String ACTION_START_LOG_AND_PHOTOCATALOG = "org.northwinds.android.intent.START_LOG_AND_PHOTOCATALOG";
	private static final String ACTION_START_PHOTOCATALOG = "org.northwinds.android.intent.START_PHOTOCATALOG";
	private static final String ACTION_STOP_LOG = "org.northwinds.android.intent.STOP_LOG";
	private static final String ACTION_TOGGLE_LOG = "org.northwinds.android.intent.TOGGLE_LOG";

	private static final String CATEGORY_SHORTCUT = "org.northwinds.android.intent.CATEGORY_SHORTCUT";

	private LifeAnalyticsTracker mTracker = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTracker = LifeApplication.getTrackerInstance(this);
		mTracker.trackPageView("/shortcut");

		Intent intent = getIntent();
		if(intent.hasCategory(CATEGORY_SHORTCUT)) {
			if(ACTION_START_LOG.equals(intent.getAction())) {
				startService(new Intent(LoggingService.ACTION_START_LOG, null, this, LoggingService.class));
				Toast.makeText(this, "GPS logging service started", Toast.LENGTH_SHORT).show();
			}
			if(ACTION_START_LOG_AND_PHOTOCATALOG.equals(intent.getAction())) {
				startService(new Intent(LoggingService.ACTION_START_LOG, null, this, LoggingService.class));
				startActivity(new Intent(this, MainActivity.class));
			}
			if(ACTION_START_PHOTOCATALOG.equals(intent.getAction())) {
				startActivity(new Intent(this, MainActivity.class));
			}
			if(ACTION_TOGGLE_LOG.equals(intent.getAction())) {
				startService(new Intent(LoggingService.ACTION_TOGGLE_LOG, null, this, LoggingService.class));
				Toast.makeText(this, "GPS logging service toggled", Toast.LENGTH_SHORT).show();
			}
			if(ACTION_STOP_LOG.equals(intent.getAction())) {
				startService(new Intent(LoggingService.ACTION_STOP_LOG, null, this, LoggingService.class));
				Toast.makeText(this, "GPS logging service stopped", Toast.LENGTH_SHORT).show();
			}
			finish();
		}

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		RadioGroup radioGroup = new RadioGroup(this);
		radioGroup.setOrientation(LinearLayout.VERTICAL);
		final RadioButton startRadio = new RadioButton(this);
		startRadio.setText("Start GPS");
		radioGroup.addView(startRadio);
		final RadioButton startAndOpenRadio = new RadioButton(this);
		startAndOpenRadio.setText("Start GPS and open PhotoCatalog");
		//startAndOpenRadio.setChecked(true);
		radioGroup.addView(startAndOpenRadio);
		final RadioButton openRadio = new RadioButton(this);
		openRadio.setText("Open PhotoCatalog");
		radioGroup.addView(openRadio);
		final RadioButton toggleRadio = new RadioButton(this);
		toggleRadio.setText("Toggle GPS");
		radioGroup.addView(toggleRadio);
		final RadioButton stopRadio = new RadioButton(this);
		stopRadio.setText("Stop GPS");
		radioGroup.addView(stopRadio);
		layout.addView(radioGroup);
		Button button = new Button(this);
		button.setText("Add");
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent shortcutIntent;
				Intent intent = new Intent();
				if(startAndOpenRadio.isChecked()) {
					shortcutIntent = new Intent(ACTION_START_LOG_AND_PHOTOCATALOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Start GPS and PC");
				} else if(openRadio.isChecked()) {
					shortcutIntent = new Intent(ACTION_START_PHOTOCATALOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Start PC");
				} else if(toggleRadio.isChecked()) {
					shortcutIntent = new Intent(ACTION_TOGGLE_LOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Toggle GPS");
				} else if(stopRadio.isChecked()) {
					shortcutIntent = new Intent(ACTION_STOP_LOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Stop GPS");
				} else {
					shortcutIntent = new Intent(ACTION_START_LOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Start GPS");
				}
				shortcutIntent.addCategory(CATEGORY_SHORTCUT);
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
				Parcelable iconResource = Intent.ShortcutIconResource.fromContext(ShortcutActivity.this, R.drawable.icon);
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		layout.addView(button);
		setContentView(layout);
	}

	@Override
	protected void onDestroy() {
		mTracker.release();
		super.onDestroy();
	}
}
