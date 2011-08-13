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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.SimpleCursorAdapter;

public class TrackListActivity extends ListActivity {
	private SharedPreferences mPrefs = null;

	private LogDbAdapter mDbAdapter;

	private void refreshTracks() {
		Cursor c = mDbAdapter.fetchAllTracks();
		startManagingCursor(c);

		String[] from = new String[] {
			LogDbAdapter.KEY_NAME
		};
		int[] to = new int[] {
			R.id.name
		};

		SimpleCursorAdapter entries =
			new SimpleCursorAdapter(this, R.layout.track_row, c, from, to);
		setListAdapter(entries);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.track_list);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mDbAdapter = new LogDbAdapter(this);
		mDbAdapter.open();
		refreshTracks();

		registerForContextMenu(getListView());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.track, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		switch(item.getItemId()) {
		case R.id.edit:
			Intent intent = new Intent(this, EditTrackActivity.class);
			intent.putExtra("track", info.id);
			startActivity(intent);
			refreshTracks();
			return true;
		case R.id.delete:
			mDbAdapter.deleteTrack(info.id);
			refreshTracks();
			return true;
		case R.id.select:
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putLong("track", info.id);
			editor.commit();
			return true;
		case R.id.save:
			ExportGPS exportGPS = new ExportGPS(this);
			exportGPS.exportAsGPX(info.id);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}
}
