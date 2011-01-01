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

package org.northwinds.Test;

import java.util.Iterator;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Logger extends Service implements Runnable {
	private LocationManager mLM;
	private NotificationManager mNM;
	private Notification mNotification;

	private static final int PHOTOCATALOG_ID = 1;

	private boolean mIsListening = false;

	private Thread mUpload = null;

	@Override
	public void onCreate() {
		super.onCreate();
		mLM = (LocationManager)getSystemService(LOCATION_SERVICE);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNotification = new Notification(R.drawable.icon, "PhotoCatalog GPS Logging", System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0);
		mNotification.setLatestEventInfo(getApplicationContext(), "PhotoCatalog", "Starting GPS...", contentIntent);
		mNM.notify(PHOTOCATALOG_ID, mNotification);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLM.removeUpdates(mLocationListener);
		mLM.removeGpsStatusListener(mGpsListener);
		mDbAdapter.close();
		mIsListening = false;
		mNM.cancelAll();
		mUpload.interrupt();
		try {
			mUpload.join();
		} catch(InterruptedException e) {
		}
		Toast.makeText(this, "Stop GPS", Toast.LENGTH_SHORT).show();
	}

	public Handler h = new Handler() {
		Logger context = Logger.this;

		public void handleMessage(Message m) {
			Bundle data = m.getData();
			Location loc = data.getParcelable("loc");
			StringBuilder sb = new StringBuilder();
			sb.append("Location: [ ");
			sb.append(loc.getLatitude());
			sb.append(", ");
			sb.append(loc.getLongitude());
			sb.append(" ]");
			//Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show();
			mDbAdapter.insertLocation(loc);
			if(context.mListener != null)
				context.mListener.onLocationChanged(loc);
			if(mUpload == null || mUpload.getState() == Thread.State.TERMINATED) {
				mUpload = new Thread(Logger.this);
				mUpload.start();
			}
		}
	};

	public LocationListener mListener = null;
	LogDbAdapter mDbAdapter;

	class MyLocListener implements LocationListener {
		Context mContext;
		Handler mH;

		public MyLocListener(Context context, Handler h) {
			mContext = context;
			mH = h;
		}

		public void onLocationChanged(Location location) {
			//Toast.makeText(mContext, "Location Update", Toast.LENGTH_SHORT).show();
			Bundle data = new Bundle();
			data.putParcelable("loc", location);
			Message m = Message.obtain();
			m.setData(data);
			mH.dispatchMessage(m);
		}
		public void onProviderDisabled(String provider) {
			Toast.makeText(mContext, provider + " provider disabled", Toast.LENGTH_SHORT).show();
		}
		public void onProviderEnabled(String provider) {
			Toast.makeText(mContext, provider + " provider enabled", Toast.LENGTH_SHORT).show();
		}
		public void onStatusChanged(String provider, int status, Bundle extras) {
			StringBuilder sb = new StringBuilder();
			sb.append(provider);
			sb.append(" status changed: ");
			switch(status) {
			case LocationProvider.OUT_OF_SERVICE:
				sb.append("Out of Service.");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				sb.append("Temporarily Unavailable.");
				break;
			case LocationProvider.AVAILABLE:
				sb.append("Available.");
				break;
			default:
				sb.append("Unknown.");
				break;
			}
			if(extras != null && extras.size() > 0) {
				Set<String> set = extras.keySet();
				Iterator<String> i = set.iterator();
				while(i.hasNext()) {
					String name = i.next();
					sb.append(", ");
					sb.append(name);
				}
			}
			Toast.makeText(mContext, sb.toString(), Toast.LENGTH_SHORT).show();
		}
	}

	public class LoggerBinder extends Binder {
		Logger getService() {
			return Logger.this;
		}
	}

	private LocationListener mLocationListener = new MyLocListener(this, h);
	private GpsStatus.Listener mGpsListener = new GpsStatus.Listener() {
		GpsStatus status = null;
		//LocationManager mLM = lm;

		@Override
		public void onGpsStatusChanged(int event) {
			status = mLM.getGpsStatus(status);
			Iterator<GpsSatellite> i = status.getSatellites().iterator();
			int nSat = 0;
			int nUsed = 0;
			while(i.hasNext()) {
				GpsSatellite s = i.next();
				nSat++;
				if(s.usedInFix())
					nUsed++;
			}
			mNotification = new Notification(R.drawable.icon, "PhotoCatalog GPS Logging", System.currentTimeMillis());
			PendingIntent contentIntent = PendingIntent.getActivity(Logger.this, 0, new Intent(Logger.this, Main.class), 0);
			mNotification.setLatestEventInfo(getApplicationContext(), "PhotoCatalog", "GPS: " + nUsed + " / " + nSat, contentIntent);
			mNM.notify(PHOTOCATALOG_ID, mNotification);
			//Toast.makeText(Logger.this, "GPS: " + nUsed + " / " + nSat, Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(!mIsListening) {
			Toast.makeText(this, "Start GPS", Toast.LENGTH_SHORT).show();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			mLM.requestLocationUpdates(LocationManager.GPS_PROVIDER, Long.parseLong(prefs.getString("time", "5"))*1000, Float.parseFloat(prefs.getString("distance", "5")), mLocationListener);
			mLM.addGpsStatusListener(mGpsListener);
			mDbAdapter = new LogDbAdapter(this);
			mDbAdapter.open();
			mIsListening = true;
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mListener = null;
		//return super.onUnbind(intent);
		return false;
	}

	public final IBinder mBinder = new LoggerBinder();

	public void setUpdateListener(LocationListener listener) {
		mListener = listener;
	}

	@Override
	public void run() {
		//Cursor c = mDbAdapter.query(Log
	}
}
