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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.support.v4.widget.SimpleCursorAdapter;

public class GPSListFragment extends ListFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int PAGE_SIZE = 5000;

	private static final String[] FROM = new String[] {
		LifeLog.Locations._ID,
		LifeLog.Locations.TIMESTAMP,
		LifeLog.Locations.LATITUDE,
		LifeLog.Locations.LONGITUDE,
		LifeLog.Locations.ALTITUDE,
		LifeLog.Locations.ACCURACY,
		LifeLog.Locations.SPEED,
		LifeLog.Locations.UPLOADED,
	};
	private static final int[] TO = new int[] {
		0,
		R.id.timestamp,
		R.id.latitude,
		R.id.longitude,
		R.id.altitude,
		R.id.accuracy,
		R.id.speed,
		R.id.row,
	};

	private Uri mBaseUri;
	private int mOffset = 0;
	private SimpleCursorAdapter mAdapter;

	private void refreshLocations() {
		mAdapter =
		   new SimpleCursorAdapter(getActivity(), R.layout.gps_row, null, FROM, TO, 0);
		mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
		    private final int uploadedColor =
			getResources().getColor(R.color.uploaded);

		    private final int notUploadedColor =
			getResources().getColor(R.color.not_uploaded);

		    public boolean setViewValue(View view,
						Cursor cursor,
						int columnIndex) {
			if(cursor.getColumnName(columnIndex)
				 .equals(LifeLog.Locations.UPLOADED)) {
			    if(cursor.getInt(columnIndex) != 0)
				view.setBackgroundColor(uploadedColor);
			    else
				view.setBackgroundColor(notUploadedColor);
			    return true;
			}
			return false;
		    }
		});
		setListAdapter(mAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		Intent intent = getActivity().getIntent();
		mBaseUri = intent.getData();
		if(mBaseUri == null)
			mBaseUri = LifeLog.Locations.CONTENT_URI;

		View view = inflater.inflate(R.layout.gps_list, container, false);

		ImageButton ib = (ImageButton)view.findViewById(R.id.forward);
		ib.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mOffset += PAGE_SIZE;
				getLoaderManager().restartLoader(0, null, GPSListFragment.this);
			}
		});
		ib = (ImageButton)view.findViewById(R.id.back);
		ib.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mOffset -= PAGE_SIZE;
				if(mOffset < 0)
					mOffset = 0;
				getLoaderManager().restartLoader(0, null, GPSListFragment.this);
			}
		});
		Button b = (Button)view.findViewById(R.id.delete_uploaded_but);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getActivity().getContentResolver()
				    .delete(getActivity().getIntent().getData(),
					    LifeLog.Locations.UPLOADED + "=1",
					    null);
				//getLoaderManager().restartLoader(0, null, GPSListFragment.this);
			}
		});

		b = (Button)view.findViewById(R.id.delete_but);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getActivity().getContentResolver()
				    .delete(getActivity().getIntent().getData(),
					    null,
					    null);
				//getLoaderManager().restartLoader(0, null, GPSListFragment.this);
			}
		});
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		refreshLocations();

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onResume() {
		super.onResume();
		//getLoaderManager().restartLoader(0, null, GPSListFragment.this);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = mBaseUri.buildUpon()
			 .appendQueryParameter(LifeLog.PARAM_FORMAT,
					       LifeLog.FORMAT_PRETTY)
			 .appendQueryParameter(LifeLog.PARAM_LIMIT,
					       String.valueOf(PAGE_SIZE))
			 .appendQueryParameter(LifeLog.PARAM_OFFSET,
					       String.valueOf(mOffset))
			 .build();
		getActivity().getIntent().setData(uri);

		return new CursorLoader(getActivity(), uri, FROM, null, null, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}
