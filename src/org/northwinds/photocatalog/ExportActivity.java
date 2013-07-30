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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

public class ExportActivity extends Activity {
	private static final DateFormat mFilenameFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
	private EditText mName;
	//private RadioButton mGpx;
	private RadioButton mKml;
	private RadioButton mCsv;

	private LifeAnalyticsTracker mTracker = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.export);
		mTracker = LifeApplication.getTrackerInstance(this);
		mTracker.trackPageView("/save");

		String date = mFilenameFormat.format(new Date());
		String filename = "photocatalog-" + date;

		mName = (EditText)findViewById(R.id.name);
		mName.setText(filename);

		//mGpx = (RadioButton)findViewById(R.id.gpx);
		mKml = (RadioButton)findViewById(R.id.kml);
		mCsv = (RadioButton)findViewById(R.id.csv);

		Button save = (Button)findViewById(R.id.save);
		save.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int type = ExportGPS.TYPE_GPX;
				if(mKml.isChecked())
					type = ExportGPS.TYPE_KML;
				if(mCsv.isChecked())
					type = ExportGPS.TYPE_CSV;
				ExportGPS exportGPS = new ExportGPS(ExportActivity.this);
				exportGPS.exportAsGPX(getIntent().getData(),
						      mName.getText().toString(), type);
				finish();
			}
		});
	}

	@Override
	protected void onDestroy() {
		mTracker.release();
		super.onDestroy();
	}
}
