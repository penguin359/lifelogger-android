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
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.SimpleCursorAdapter;

public class GPSList extends ListActivity {
	LogDbAdapter mDbAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gps_list);

		mDbAdapter = new LogDbAdapter(this);
		mDbAdapter.open();
		Cursor c = mDbAdapter.fetchAllLocations();
		startManagingCursor(c);

		String[] from = new String[] {
			LogDbAdapter.KEY_TIMESTAMP,
			LogDbAdapter.KEY_LATITUDE,
			LogDbAdapter.KEY_LONGITUDE,
			LogDbAdapter.KEY_UPLOADED
		};
		int[] to = new int[] {
			R.id.timestamp,
			R.id.latitude,
			R.id.longitude,
			R.id.row
		};

		SimpleCursorAdapter entries =
			new SimpleCursorAdapter(this, R.layout.gps_row, c, from, to);
		entries.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if(cursor.getColumnName(columnIndex).equals(LogDbAdapter.KEY_UPLOADED)) {
					if(cursor.getInt(columnIndex) != 0)
						view.setBackgroundColor(0xff008000);
					else
						view.setBackgroundColor(0xff800000);
					return true;
				}
				return false;
			}
		});
		setListAdapter(entries);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}
}
