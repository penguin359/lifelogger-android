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

//import android.app.AlertDialog;
import android.content.Intent;
//import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class MapViewActivity extends MapActivity {
	private Projection mProjection;

	/*
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
	*/

	/* Quick hack to get access to the boundCenter static method of
	 * ItemizedOverlay. */
	private static class DummyItemizedOverlay extends ItemizedOverlay<OverlayItem> {
		public static Drawable myBoundCenter(Drawable d) {
			return boundCenter(d);
		}

		public DummyItemizedOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
		}

		@Override
		protected OverlayItem createItem(int i) {
			return null;
		}

		@Override
		public int size() {
			return 0;
		}
	}

	private class PathOverlay extends Overlay {
		Paint mPaint;
		Drawable mIndicator;
		//Bitmap mIndicator;

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

			mIndicator = DummyItemizedOverlay.myBoundCenter(getResources().getDrawable(R.drawable.ic_maps_indicator_current_position));
			//mIndicator = BitmapFactory.decodeResource(getResources(), R.drawable.ic_maps_indicator_current_position);
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

			Point p1 = null;
			for(GeoPoint point: mPathList) {
				p1 = new Point();
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
			if(p1 != null)
				drawAt(canvas, mIndicator, p1.x, p1.y, false);
				//canvas.drawBitmap(mIndicator, p1.x, p1.y, null);
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return true;
	}

	private GeoPoint[] mPathList;

	private GeoPoint[] refreshPath() {
		ArrayList<GeoPoint> pathList = new ArrayList<GeoPoint>();
		try {
			Cursor c = getContentResolver().query(getIntent().getData(), new String[] { LifeLog.Locations.LATITUDE, LifeLog.Locations.LONGITUDE, }, null, null, null);
			int latCol = c.getColumnIndexOrThrow("latitude");
			int lonCol = c.getColumnIndexOrThrow("longitude");
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

	private MapView mMapView;

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
		mapOverlays.add(new PathOverlay());

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
			mMapView.setSatellite(!mMapView.isSatellite());
			return true;
		case R.id.center_map:
			if(mPathList.length > 0)
				mMapView.getController().setCenter(mPathList[mPathList.length-1]);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		getContentResolver().unregisterContentObserver(mObserver);
		super.onDestroy();
	}
}
