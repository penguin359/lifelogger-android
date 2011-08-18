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

import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class MapViewActivity extends MapActivity {
	private Projection mProjection;

	private static class MapItemizedOverlay extends ItemizedOverlay<OverlayItem> {
		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		private Context mContext;

		public MapItemizedOverlay(Drawable defaultMarker, Context context) {
			super(boundCenterBottom(defaultMarker));
			mContext = context;
		}

		public void addOverlay(OverlayItem overlay) {
			mOverlays.add(overlay);
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mOverlays.get(i);
		}

		@Override
		public int size() {
			return mOverlays.size();
		}
		
		@Override
		protected boolean onTap(int index) {
			OverlayItem item = mOverlays.get(index);
			AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
			dialog.setTitle(item.getTitle());
			dialog.setMessage(item.getSnippet());
			dialog.show();
			return true;
		}
	}

	private class PathOverlay extends Overlay {
		Paint mPaint;

		public PathOverlay() {
			mPaint = new Paint();
			mPaint.setDither(true);
			//mPaint.setColor(Color.RED);
			//mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setColor(Color.CYAN);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(5);
		}

		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);

			if(mPathList == null)
				return;

			Path path = new Path();
			if(mPathList.length > 0) {
				Point p1 = new Point();
				mProjection.toPixels(mPathList[0], p1);
				path.moveTo(p1.x, p1.y);
			}

			for(GeoPoint point: mPathList) {
				Point p1 = new Point();
				mProjection.toPixels(point, p1);
				path.lineTo(p1.x, p1.y);
			}

			//GeoPoint point = new GeoPoint(19240000,-99120000);
			//GeoPoint point2 = new GeoPoint(35410000, 139460000);

			//Point p1 = new Point();
			//Point p2 = new Point();
			//Path path = new Path();

			//mProjection.toPixels(point, p1);
			//mProjection.toPixels(point2, p2);

			//path.moveTo(p2.x, p2.y);
			//path.lineTo(p1.x, p1.y);

			canvas.drawPath(path, mPaint);
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private GeoPoint[] mPathList;

	private GeoPoint[] refreshPath() {
		ArrayList<GeoPoint> pathList = new ArrayList<GeoPoint>();
		try {
			Cursor c = getContentResolver().query(getIntent().getData(), new String[] { LifeLog.Locations.LATITUDE, LifeLog.Locations.LONGITUDE, }, null, null, null);
			int latCol = c.getColumnIndexOrThrow("latitude");
			int lonCol = c.getColumnIndexOrThrow("longitude");
			//if(c.moveToFirst()) {
			//	int lat = (int)(c.getDouble(latCol)*1000000.);
			//	int lon = (int)(c.getDouble(lonCol)*1000000.);
			//	GeoPoint point = new GeoPoint(lat, lon);
			//	pathList.add(point);
			//}
			while(c.moveToNext()) {
				int lat = (int)(c.getDouble(latCol)*1000000.);
				int lon = (int)(c.getDouble(lonCol)*1000000.);
				GeoPoint point = new GeoPoint(lat, lon);
				pathList.add(point);
			}
			c.close();
		} catch(SQLException ex) {
		}

		return pathList.toArray(new GeoPoint[pathList.size()]);
	}

	private boolean mIsRefreshing = false;
	private MapView mMapView;

	private class RefreshTask extends AsyncTask<Void, Void, GeoPoint[]> {
		protected void onPreExecute() {
			mIsRefreshing = true;
		}

		protected GeoPoint[] doInBackground(Void... obj) {
			try {
				Thread.sleep(2000);
			} catch(InterruptedException ex) {}
			return refreshPath();
		}

		protected void onPostExecute(GeoPoint[] pathList) {
			mPathList = pathList;
			mMapView.invalidate();
			mIsRefreshing = false;
		}
	}

	private ContentObserver mObserver = new ContentObserver(null) {
		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		Handler mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				mPathList = (GeoPoint[])msg.obj;
				mMapView.invalidate();
			}
		};

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			//if(!mIsRefreshing) {
			//	new RefreshTask().execute();
			//}
			try {
				Thread.sleep(2000);
			} catch(InterruptedException ex) {}
			mHandler.sendMessage(Message.obtain(null, 0, refreshPath()));
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_view);

		Intent intent = getIntent();
		if(intent.getData() == null)
			intent.setData(LifeLog.Locations.CONTENT_URI);

		mPathList = refreshPath();

		mMapView = (MapView)findViewById(R.id.map_view);
		mMapView.setBuiltInZoomControls(true);
		List<Overlay> mapOverlays = mMapView.getOverlays();
		mProjection = mMapView.getProjection();
		Drawable drawable = getResources().getDrawable(R.drawable.androidmarker);
		MapItemizedOverlay itemizedOverlay = new MapItemizedOverlay(drawable, this);
		GeoPoint point = new GeoPoint(19240000,-99120000);
		OverlayItem overlayItem = new OverlayItem(point, "Hola, Mundo!", "I'm in Mexico City!");
		itemizedOverlay.addOverlay(overlayItem);
		GeoPoint point2 = new GeoPoint(35410000, 139460000);
		OverlayItem overlayItem2 = new OverlayItem(point2, "Sekai, konichiwa!", "I'm in Japan!");
		itemizedOverlay.addOverlay(overlayItem2);
		mapOverlays.add(itemizedOverlay);
		mapOverlays.add(new PathOverlay());

		getContentResolver().registerContentObserver(getIntent().getData(), true, mObserver);
	}

	@Override
	protected void onDestroy() {
		getContentResolver().unregisterContentObserver(mObserver);
		super.onDestroy();
	}
}
