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

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Paint;
//import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapViewFragment extends Fragment {
	/*
	private class PathOverlay extends Overlay {
		Paint mPaint;
		Drawable mIndicator;
		//Bitmap mIndicator;

		public PathOverlay() {
			Resources r = getResources();
			mPaint = new Paint();
			mPaint.setDither(true);
			//mPaint.setColor(Color.RED);
			//mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setColor(r.getColor(R.color.map_color));
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(r.getColor(R.integer.map_width));

			mIndicator = DummyItemizedOverlay.myBoundCenter(getResources().getDrawable(R.drawable.ic_maps_indicator_current_position));
			//mIndicator = BitmapFactory.decodeResource(getResources(), R.drawable.ic_maps_indicator_current_position);
		}
	}
	*/

	private ArrayList<LatLng> mPathList;
	private Marker mPoint;
	private Polyline mPath;

	private ArrayList<LatLng> refreshPath() {
		ArrayList<LatLng> pathList = new ArrayList<LatLng>();
		try {
			//Cursor c = getActivity().getContentResolver().query(getArguments().getString("data"), new String[] { LifeLog.Locations.LATITUDE, LifeLog.Locations.LONGITUDE, }, null, null, null);
			Cursor c = getActivity().getContentResolver().query(getActivity().getIntent().getData(), new String[] { LifeLog.Locations.LATITUDE, LifeLog.Locations.LONGITUDE, }, null, null, null);
			int latCol = c.getColumnIndexOrThrow(LifeLog.Locations.LATITUDE);
			int lonCol = c.getColumnIndexOrThrow(LifeLog.Locations.LONGITUDE);
			while(c.moveToNext()) {
				double lat = c.getDouble(latCol);
				double lon = c.getDouble(lonCol);
				LatLng point = new LatLng(lat, lon);
				pathList.add(point);
			}
			c.close();
		} catch(SQLException ex) {
		}

		return pathList;
	}

	private GoogleMap mMap;

	private ContentObserver mObserver = new ContentObserver(null) {
		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		Handler mHandler = new Handler() {
			@Override
			@SuppressWarnings("unchecked")
			public void handleMessage(Message msg) {
				//if(msg.obj instanceof ArrayList<?>) {
					mPathList = (ArrayList<LatLng>)msg.obj;
					mPath.setPoints(mPathList);
					if(mPathList.size() > 0)
						mPoint.setPosition(mPathList.get(mPathList.size()-1));
				//}
			}
		};

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			try {
				Thread.sleep(2000);
			} catch(InterruptedException ex) {}
			mHandler.sendMessage(Message.obtain(null, 0, refreshPath()));
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//super.onCreate(savedInstanceState);
		//setContentView(R.layout.map);
		mMap = ((SupportMapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();

		//String data = getArguments().getString("data");
		//if(data == null)
		//	getArguments().putString("data", LifeLog.Locations.CONTENT_URI);
		Intent intent = getActivity().getIntent();
		if(intent.getData() == null)
			intent.setData(LifeLog.Locations.CONTENT_URI);

		mPathList = refreshPath();

		Resources r = getResources();
		mPath = mMap.addPolyline(new PolylineOptions().geodesic(true).color(r.getColor(R.color.map_color)).width(10).addAll(mPathList));
		LatLng loc = new LatLng(0., 0.);
		if(mPathList.size() > 0)
			loc = mPathList.get(mPathList.size()-1);
		mPoint = mMap.addMarker(new MarkerOptions().position(loc));
		return inflater.inflate(R.layout.map, container, false);
	}

	@Override
	public void onStart() {
		//getActivity().getContentResolver().registerContentObserver(getArguments().getString("data"), true, mObserver);
		getActivity().getContentResolver().registerContentObserver(getActivity().getIntent().getData(), true, mObserver);
	}

	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_view, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.map_mode:
			if(mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL)
				mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			else
				mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			return true;
		case R.id.center_map:
			if(mPathList.size() > 0)
				mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mPathList.get(mPathList.size()-1), 18.0f));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	*/

	public void mapMode() {
		if(mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL)
			mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		else
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	}

	public void centerMap() {
		if(mPathList.size() > 0)
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mPathList.get(mPathList.size()-1), 18.0f));
	}

	@Override
	public void onStop() {
		getActivity().getContentResolver().unregisterContentObserver(mObserver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
