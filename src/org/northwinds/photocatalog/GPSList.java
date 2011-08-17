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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;

public class GPSList extends ListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gps_list);

		Intent intent = getIntent();
		Uri uri = intent.getData();
		if(uri == null)
			uri = LifeLog.Locations.CONTENT_URI;
		uri = uri.buildUpon()
		         .appendQueryParameter(LifeLog.PARAM_FORMAT,
					       LifeLog.FORMAT_PRETTY)
			 .build();
		intent.setData(uri);

		fillData();

		Button b = (Button)findViewById(R.id.delete_uploaded_but);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getContentResolver().delete(getIntent().getData(), LifeLog.Locations.UPLOADED + "=1", null);
				fillData();
			}
		});

		b = (Button)findViewById(R.id.delete_but);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getContentResolver().delete(getIntent().getData(), null, null);
				fillData();
			}
		});
	}

	static final SimpleCursorAdapter.ViewBinder sBinder = new SimpleCursorAdapter.ViewBinder() {
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if(cursor.getColumnName(columnIndex).equals(LifeLog.Locations.UPLOADED)) {
				if(cursor.getInt(columnIndex) != 0)
					view.setBackgroundColor(0xff008000);
				else
					view.setBackgroundColor(0xff800000);
				return true;
			}
			return false;
		}
	};
	static final String[] sFrom = new String[] {
		LifeLog.Locations._ID,
		LifeLog.Locations.TIMESTAMP,
		LifeLog.Locations.LATITUDE,
		LifeLog.Locations.LONGITUDE,
		LifeLog.Locations.ACCURACY,
		LifeLog.Locations.UPLOADED
	};
	static final int[] sTo = new int[] {
		0,
		R.id.timestamp,
		R.id.latitude,
		R.id.longitude,
		R.id.accuracy,
		R.id.row
	};

	private void fillData() {
		Cursor c = managedQuery(getIntent().getData(), sFrom, null, null, null);
		SimpleCursorAdapter entries =
			new SimpleCursorAdapter(this, R.layout.gps_row, c, sFrom, sTo);
		entries.setViewBinder(sBinder);
		setListAdapter(entries);
	}
}
