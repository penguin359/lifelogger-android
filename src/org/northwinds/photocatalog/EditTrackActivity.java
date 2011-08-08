/*
 * Copyright (c) 2010, Loren M. Lang
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
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.widget.EditText;

public class EditTrackActivity extends Activity {
	private long mTrack = 0;

	EditText mName;
	EditText mComment;
	EditText mType;
	EditText mDescription;

	LogDbAdapter mDbAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.edit_track);
		Intent intent = getIntent();
		mTrack = intent.getLongExtra("track", 0);

		if(mTrack <= 0) {
			finish();
			return;
		}

		mName = (EditText)findViewById(R.id.name);
		mComment = (EditText)findViewById(R.id.comment);
		mType = (EditText)findViewById(R.id.type);
		mDescription = (EditText)findViewById(R.id.description);

		mDbAdapter = new LogDbAdapter(this);
		mDbAdapter.open();
		Cursor c = null;
		try {
			c = mDbAdapter.fetchTrack(mTrack);

			int nameCol = c.getColumnIndexOrThrow(LogDbAdapter.KEY_NAME);
			int commentCol = c.getColumnIndexOrThrow(LogDbAdapter.KEY_CMT);
			int typeCol = c.getColumnIndexOrThrow(LogDbAdapter.KEY_TYPE);
			int descriptionCol = c.getColumnIndexOrThrow(LogDbAdapter.KEY_DESC);
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
		args.put(LogDbAdapter.KEY_NAME, mName.getText().toString());
		args.put(LogDbAdapter.KEY_CMT, mComment.getText().toString());
		args.put(LogDbAdapter.KEY_TYPE, mType.getText().toString());
		args.put(LogDbAdapter.KEY_DESC, mDescription.getText().toString());
		mDbAdapter.updateTrack(mTrack, args);
	}

	@Override
	public void onDestroy() {
		mDbAdapter.close();
		super.onDestroy();
	}
}
