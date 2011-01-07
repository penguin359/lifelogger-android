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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentProducer;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Logger extends Service implements Runnable {
	//private static final String TAG = "PhotoCatalog";
	private LocationManager mLM;
	private NotificationManager mNM;
	private Notification mNotification;

	private static final int PHOTOCATALOG_ID = 1;

	private boolean mIsStarted = false;

	private Thread mUpload = null;
	private SharedPreferences mPrefs = null;

	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	static final int MSG_LOCATION = 0;
	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_STATUS = 3;

	private final Messenger mMessenger = new Messenger(new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				try {
					msg.replyTo.send(Message.obtain(null, MSG_STATUS, mIsStarted ? 1 : 0, 0));
				} catch(RemoteException ex) {
					mClients.remove(msg.replyTo);
				}
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	});

	@Override
	public void onCreate() {
		super.onCreate();
		mLM = (LocationManager)getSystemService(LOCATION_SERVICE);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mDbAdapter = new LogDbAdapter(this);
		mDbAdapter.open();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLM.removeUpdates(mLocationListener);
		mLM.removeGpsStatusListener(mGpsListener);
		mDbAdapter.close();
		mNM.cancel(PHOTOCATALOG_ID);
		mUpload.interrupt();
		//try {
		//	mUpload.join();
		//} catch(InterruptedException e) {
		//}
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
			for(int i = mClients.size()-1; i >= 0; i--) {
				try {
					mClients.get(i).send(Message.obtain(null, MSG_LOCATION, loc));
				} catch(RemoteException ex) {
					mClients.remove(i);
				}
			}
			if(mUpload == null || mUpload.getState() == Thread.State.TERMINATED) {
				if(mPrefs.getBoolean("autoUpload", true)) {
					mUpload = new Thread(Logger.this);
					mUpload.start();
				}
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

	public static final String ACTION_START_LOG = "org.northwinds.android.intent.START_LOG";
	public static final String ACTION_STOP_LOG = "org.northwinds.android.intent.STOP_LOG";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if(action == null) {
		} else if(action.equals(ACTION_START_LOG)) {
			if(!mIsStarted) {
				Toast.makeText(this, "Start GPS", Toast.LENGTH_SHORT).show();
				mLM.requestLocationUpdates(LocationManager.GPS_PROVIDER, Long.parseLong(mPrefs.getString("time", "5"))*1000, Float.parseFloat(mPrefs.getString("distance", "5")), mLocationListener);
				mLM.addGpsStatusListener(mGpsListener);
				//mDbAdapter.open();
				mNotification = new Notification(R.drawable.icon, "PhotoCatalog GPS Logging", System.currentTimeMillis());
				PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0);
				mNotification.setLatestEventInfo(getApplicationContext(), "PhotoCatalog", "Starting GPS...", contentIntent);
				mNM.notify(PHOTOCATALOG_ID, mNotification);
				mIsStarted = true;
				for(int i = mClients.size()-1; i >= 0; i--) {
					try {
						mClients.get(i).send(Message.obtain(null, MSG_STATUS, 1, 0));
					} catch(RemoteException ex) {
						mClients.remove(i);
					}
				}
			}
		} else if(action.equals(ACTION_STOP_LOG)) {
			if(mIsStarted) {
				Toast.makeText(this, "Stop GPS", Toast.LENGTH_SHORT).show();
				mLM.removeUpdates(mLocationListener);
				mLM.removeGpsStatusListener(mGpsListener);
				//mDbAdapter.close();
				mIsStarted = false;
				mNM.cancel(PHOTOCATALOG_ID);
				for(int i = mClients.size()-1; i >= 0; i--) {
					try {
						mClients.get(i).send(Message.obtain(null, MSG_STATUS, 0, 0));
					} catch(RemoteException ex) {
						mClients.remove(i);
					}
				}
				stopSelfResult(startId);
			}
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
		//return mBinder;
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
		try {
			Thread.sleep(2000);
		} catch(InterruptedException e) {
			return;
		}
		final String[] cols = new String[] {
			"_id",
			"timestamp",
			"latitude",
			"longitude",
			"altitude",
			"accuracy",
			"bearing",
			"speed",
			"satellites"
		};
		//try {
		//	db = SQLiteDatabase.openDatabase("databases/data.db", null, SQLiteDatabase.OPEN_READWRITE);
		//} catch(SQLiteException e) {
		//	Log.e(TAG, "Failed to open SQLite Database", e);
		//	return;
		//}
		//Cursor c = db.query("locations", cols, "uploaded != 1", null, null, null, null, "100");
		final Cursor c = mDbAdapter.fetchUploadLocations(cols, "uploaded != 1");
		if(!c.moveToFirst()) {
			c.close();
			return;
		}

		String baseUrl = mPrefs.getString("url", "http://www.example.org/photocatalog/");
		String source = mPrefs.getString("source", "0");
		final List<Integer> idList = new ArrayList<Integer>();
		Multipart m = new Multipart(this);
		m.put("source", source);
		m.put("file", new ContentProducer() {
			public void writeTo(OutputStream os) throws IOException {
				os.write("PhotoCatalog v1.0\n".getBytes());
				for(int i = 0; i < cols.length; i++) {
					if(i > 0)
						os.write(",".getBytes());
					if(cols[i].equals("_id"))
						os.write("id".getBytes());
					else
						os.write(cols[i].getBytes());
				}
				os.write("\n".getBytes());
				do {
					idList.add(c.getInt(0));
					for(int i = 0; i < cols.length; i++) {
						if(i > 0)
							os.write(",".getBytes());
						if(!c.isNull(i))
							os.write(c.getString(i).getBytes());
					}
					os.write("\n".getBytes());
				} while(c.moveToNext());
			}
		});
		m.put("finish", "1");

		//StringBuilder sb = new StringBuilder();
		//sb.append(baseUrl).append("cgi/test-android.pl?loc=").append("123").append(",").append(c.getCount());
		String uri = baseUrl + "cgi/test-android.pl";
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(uri);
		try {
			post.setEntity(m.getEntity());
			HttpResponse resp = client.execute(post);
			InputStream is = resp.getEntity().getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			if(r.readLine().equals("OK")) {
				ContentValues values = new ContentValues();
				values.put("uploaded", 1);
				Integer ids[] = idList.toArray(new Integer[1]);
				for(int i = 0; i < ids.length; i++)
					mDbAdapter.updateLocation(ids[i], values);
			}
			//sb.append("\nWrite: '").append(r.readLine()).append("'");
		} catch (Exception ex) {
			//ex.printStackTrace();
			//sb.append("\nError: '" + ex + "'");
		} finally {
			//get.releaseConnection();
		}
		c.close();
	}
}
