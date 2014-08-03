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

import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.support.v4.widget.SimpleCursorAdapter;

public class TrackListFragment extends ListFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String[] FROM = new String[] {
		LifeLog.Tracks._ID,
		LifeLog.Tracks.NAME,
		LifeLog.Tracks.CMT,
		LifeLog.Tracks.TYPE,
		LifeLog.Tracks.NUM_POINTS,
	};
	private static final int[] TO = new int[] {
		0,
		R.id.name,
		R.id.comment,
		R.id.type,
		R.id.count,
	};

	private SimpleCursorAdapter mAdapter;

	private void refreshTracks() {
		mAdapter =
		    new SimpleCursorAdapter(getActivity(), R.layout.track_row, null, FROM, TO, 0);
		setListAdapter(mAdapter);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.track_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		refreshTracks();
		getLoaderManager().initLoader(0, null, this);
		registerForContextMenu(getListView());
	}

	@Override
	public void onListItemClick(ListView l, View v,
				       int position, long id) {
		super.onListItemClick(l, v, position, id);

		SharedPreferences.Editor editor =
		    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
		editor.putLong("track", id);
		editor.commit();

		getActivity().finish();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.track, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info =
		    (AdapterContextMenuInfo)item.getMenuInfo();

		Uri.Builder uri = ContentUris
		    .appendId(LifeLog.Tracks.CONTENT_URI.buildUpon(), info.id);

		switch(item.getItemId()) {
		case R.id.edit:
			startActivity(
			    new Intent(Intent.ACTION_EDIT, uri.build(),
				       getActivity(), EditTrackActivity.class));
			return true;
		case R.id.delete:
			getActivity().getContentResolver().delete(uri.build(), null, null);
			//refreshTracks();
			return true;
		case R.id.save:
			startActivity(
			    new Intent(Intent.ACTION_VIEW,
				       uri.appendPath("locations").build(),
				       getActivity(), ExportActivity.class));
			return true;
		case R.id.gps_list:
			startActivity(
			    new Intent(Intent.ACTION_VIEW,
				       uri.appendPath("locations").build(),
				       getActivity(), GPSListActivity.class));
			return true;
		}
		return super.onContextItemSelected(item);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = LifeLog.Tracks.CONTENT_URI
				 .buildUpon()
				 .appendQueryParameter(LifeLog.PARAM_FORMAT,
						       LifeLog.FORMAT_DETAILED)
				 .build();

		return new CursorLoader(getActivity(), uri, FROM, null, null, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}
