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
import java.util.List;

import android.app.Activity;
//import android.app.AlertDialog;
import android.content.Intent;
//import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.graphics.Canvas;
//import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapViewActivity extends Activity {
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
			Cursor c = getContentResolver().query(getIntent().getData(), new String[] { LifeLog.Locations.LATITUDE, LifeLog.Locations.LONGITUDE, }, null, null, null);
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
			public void handleMessage(Message msg) {
				mPathList = (ArrayList<LatLng>)msg.obj;
				mPath.setPoints(mPathList);
				if(mPathList.size() > 0)
					mPoint.setPosition(mPathList.get(mPathList.size()-1));
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

	private LifeAnalyticsTracker mTracker = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		mMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
		mTracker = LifeApplication.getTrackerInstance(this);
		mTracker.trackPageView("/maps");

		Intent intent = getIntent();
		if(intent.getData() == null)
			intent.setData(LifeLog.Locations.CONTENT_URI);

		mPathList = refreshPath();

		Resources r = getResources();
		mPath = mMap.addPolyline(new PolylineOptions().geodesic(true).color(r.getColor(R.color.map_color)).width(10).addAll(mPathList));
		LatLng loc = new LatLng(0., 0.);
		if(mPathList.size() > 0)
			loc = mPathList.get(mPathList.size()-1);
		mPoint = mMap.addMarker(new MarkerOptions().position(loc));

		getContentResolver().registerContentObserver(getIntent().getData(), true, mObserver);
	}

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

	@Override
	protected void onDestroy() {
		getContentResolver().unregisterContentObserver(mObserver);
		mTracker.release();
		super.onDestroy();
	}
}
