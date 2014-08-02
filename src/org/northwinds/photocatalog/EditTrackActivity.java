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

import android.support.v7.app.ActionBarActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.widget.EditText;

public class EditTrackActivity extends ActionBarActivity {
	private EditText mName;
	private EditText mComment;
	private EditText mType;
	private EditText mDescription;

	private LifeAnalyticsTracker mTracker = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_track);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mTracker = LifeApplication.getTrackerInstance(this);
		mTracker.trackPageView("/edit_track");

		if(getIntent().getData() == null) {
			finish();
			return;
		}

		mName = (EditText)findViewById(R.id.name);
		mComment = (EditText)findViewById(R.id.comment);
		mType = (EditText)findViewById(R.id.type);
		mDescription = (EditText)findViewById(R.id.description);

		Cursor c = null;
		try {
			c = getContentResolver().query(getIntent().getData(), null, null, null, null);

			int nameCol = c.getColumnIndexOrThrow(LifeLog.Tracks.NAME);
			int commentCol = c.getColumnIndexOrThrow(LifeLog.Tracks.CMT);
			int typeCol = c.getColumnIndexOrThrow(LifeLog.Tracks.TYPE);
			int descriptionCol = c.getColumnIndexOrThrow(LifeLog.Tracks.DESC);
			if(c.moveToFirst()) {
				mName.setText(c.getString(nameCol));
				mComment.setText(c.getString(commentCol));
				mType.setText(c.getString(typeCol));
				mDescription.setText(c.getString(descriptionCol));
			}
		} catch(SQLException ex) {
		} finally {
			if(c != null)
				c.close();
		}
	}

	@Override
	public void onPause() {
		ContentValues args = new ContentValues();
		args.put(LifeLog.Tracks.NAME, mName.getText().toString());
		args.put(LifeLog.Tracks.CMT, mComment.getText().toString());
		args.put(LifeLog.Tracks.TYPE, mType.getText().toString());
		args.put(LifeLog.Tracks.DESC, mDescription.getText().toString());
		getContentResolver().update(getIntent().getData(), args, null, null);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mTracker.release();
		super.onDestroy();
	}
}
