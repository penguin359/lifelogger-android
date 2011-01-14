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
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
//import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentProducer;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
//import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class Logger extends Service implements Runnable {
	private static final String TAG = "PhotoCatalog-Logger";

	private LocationManager mLM;
	private NotificationManager mNM;
	private Notification mNotification;

	private static final int PHOTOCATALOG_ID = 1;

	private boolean mIsStarted = false;

	private Thread mUpload = null;
	private volatile boolean mUploadRun = false;
	private volatile boolean mUploadRunOnce = false;
	private volatile int mUploadRunOnceStartId = 0;
	private volatile int mUploadCount = 0;
	private Object mUploadLock = new Object();

	private SharedPreferences mPrefs = null;

	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	private String mLastUploadStatus = "Stopped.";
	private Location mLastLocation = null;

	static final int MSG_REGISTER_CLIENT = 0;
	static final int MSG_UNREGISTER_CLIENT = 1;
	static final int MSG_LOCATION = 2;
	static final int MSG_STATUS = 3;
	static final int MSG_UPLOAD = 4;

	private final Messenger mMessenger = new Messenger(new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				try {
					msg.replyTo.send(Message.obtain(null, MSG_STATUS, mIsStarted ? 1 : 0, 0));
					msg.replyTo.send(Message.obtain(null, MSG_LOCATION, mLastLocation));
					msg.replyTo.send(Message.obtain(null, MSG_UPLOAD, mLastUploadStatus));
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

	private void sendUploadStatus(String status) {
		mLastUploadStatus = status;
		for(int i = mClients.size()-1; i >= 0; i--) {
			try {
				mClients.get(i).send(Message.obtain(null, MSG_UPLOAD, status));
			} catch(RemoteException ex) {
				mClients.remove(i);
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mLM = (LocationManager)getSystemService(LOCATION_SERVICE);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mDbAdapter = new LogDbAdapter(this);
		mDbAdapter.open();

		/* upload thread hasn't been started yet */
		mUploadCount = mDbAdapter.countUploadLocations();
		/* preferences can only be updated on the main thread so this
		 * should be safe without synchronization
		 */
		if(mPrefs.getBoolean("autoUpload", true)) {
			mUploadRun = true;
			mUploadRunOnce = false;
			mUpload = new Thread(this);
			mUpload.start();
		}
		mPrefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				boolean autoUpload = key.equals("autoUpload");
				if(autoUpload != mUploadRun) {
					if(mPrefs.getBoolean("autoUpload", true)) {
						mUploadRun = true;
						mUploadRunOnce = false;
						if(mUpload == null || mUpload.getState() == Thread.State.TERMINATED) {
							mUpload = new Thread(Logger.this);
							mUpload.start();
						}
					} else {
						mUploadRun = false;
						if(mUpload != null && mUpload.getState() != Thread.State.TERMINATED)
							mUpload.interrupt();
					}
				}
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLM.removeUpdates(mLocationListener);
		mLM.removeGpsStatusListener(mGpsListener);
		mDbAdapter.close();
		mNM.cancel(PHOTOCATALOG_ID);
		mUploadRun = false;
		if(mUpload != null && mUpload.getState() != Thread.State.TERMINATED) {
			mUpload.interrupt();
			try {
				mUpload.join();
			} catch(InterruptedException e) {
			}
		}
	}

	//public LocationListener mListener = null;
	private LogDbAdapter mDbAdapter;

	public class LoggerBinder extends Binder {
		Logger getService() {
			return Logger.this;
		}
	}

	private LocationListener mLocationListener = new LocationListener() {
		public void onLocationChanged(Location loc) {
			if(loc.hasAccuracy() &&
			   loc.getAccuracy() > Float.parseFloat(mPrefs.getString("accuracy", "200"))) {
				Log.v(TAG, "Accuracy too low: " + loc.getAccuracy());
				return;
			}
			mDbAdapter.insertLocation(loc);
			mLastLocation = loc;
			for(int i = mClients.size()-1; i >= 0; i--) {
				try {
					mClients.get(i).send(Message.obtain(null, MSG_LOCATION, loc));
				} catch(RemoteException ex) {
					mClients.remove(i);
				}
			}
			synchronized(mUploadLock) {
				mUploadCount++;
				mUploadLock.notify();
			}
			if(mUpload == null || mUpload.getState() == Thread.State.TERMINATED) {
				if(mUploadRun) {
					mUpload = new Thread(Logger.this);
					mUpload.start();
				}
			}
			if(mStartTime != 0) {
				Long timeDiff = System.currentTimeMillis() - mStartTime;
				StringBuilder sb = new StringBuilder();
				sb.append("Time to first GPS fix: ");
				sb.append(timeDiff/1000);
				sb.append(" seconds");
				Toast.makeText(Logger.this, sb.toString(), Toast.LENGTH_LONG);
				mStartTime = 0L;
			}
		}

		public void onProviderDisabled(String provider) {
			//Toast.makeText(Logger.this, provider + " provider disabled", Toast.LENGTH_SHORT).show();
		}

		public void onProviderEnabled(String provider) {
			//Toast.makeText(Logger.this, provider + " provider enabled", Toast.LENGTH_SHORT).show();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			//StringBuilder sb = new StringBuilder();
			//sb.append(provider);
			//sb.append(" status changed: ");
			//switch(status) {
			//case LocationProvider.OUT_OF_SERVICE:
			//	sb.append("Out of Service.");
			//	break;
			//case LocationProvider.TEMPORARILY_UNAVAILABLE:
			//	sb.append("Temporarily Unavailable.");
			//	break;
			//case LocationProvider.AVAILABLE:
			//	sb.append("Available.");
			//	break;
			//default:
			//	sb.append("Unknown.");
			//	break;
			//}
			//if(extras != null && extras.size() > 0) {
			//	Set<String> set = extras.keySet();
			//	Iterator<String> i = set.iterator();
			//	while(i.hasNext()) {
			//		String name = i.next();
			//		sb.append(", ");
			//		sb.append(name);
			//	}
			//}
			//Toast.makeText(Logger.this, sb.toString(), Toast.LENGTH_SHORT).show();
			StringBuilder sb = new StringBuilder();
			if(extras != null)
				sb.append("Status changed extras(").append(extras.size()).append("): ");
			if(extras != null && extras.size() > 0) {
				Set<String> set = extras.keySet();
				for(String name: set) {
					sb.append(name);
					sb.append(" ISA ");
					sb.append(extras.get(name).getClass().getName());
					sb.append(", ");
				}
				if(extras.containsKey("satellites"))
					sb.append(extras.getInt("satellites")).append(" satellites used");
			}
			Log.v(TAG, sb.toString());
			//Toast.makeText(Logger.this, sb.toString(), Toast.LENGTH_SHORT).show();
		}
	};

	private GpsStatus.Listener mGpsListener = new GpsStatus.Listener() {
		GpsStatus status = null;

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
		}
	};

	public static final String ACTION_START_LOG = "org.northwinds.android.intent.START_LOG";
	public static final String ACTION_STOP_LOG = "org.northwinds.android.intent.STOP_LOG";
	public static final String ACTION_UPLOAD_ONCE = "org.northwinds.android.intent.UPLOAD_ONCE";

	private Long mStartTime = 0L;

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
				mStartTime = System.currentTimeMillis();
			}
		} else if(action.equals(ACTION_STOP_LOG)) {
			if(mIsStarted) {
				Toast.makeText(this, "Stop GPS", Toast.LENGTH_SHORT).show();
				mLM.removeUpdates(mLocationListener);
				mLM.removeGpsStatusListener(mGpsListener);
				//mDbAdapter.close();
				mIsStarted = false;
				mNM.cancel(PHOTOCATALOG_ID);
				mLastLocation = null;
				for(int i = mClients.size()-1; i >= 0; i--) {
					try {
						mClients.get(i).send(Message.obtain(null, MSG_STATUS, 0, 0));
						mClients.get(i).send(Message.obtain(null, MSG_LOCATION, mLastLocation));
					} catch(RemoteException ex) {
						mClients.remove(i);
					}
				}
				stopSelfResult(startId);
			}
		} else if(action.equals(ACTION_UPLOAD_ONCE)) {
			if(!mUploadRun) {
				mUploadRun = true;
				mUploadRunOnce = true;
				mUploadRunOnceStartId = startId;
			}
			if(mUpload == null || mUpload.getState() == Thread.State.TERMINATED) {
				mUpload = new Thread(Logger.this);
				mUpload.start();
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
		//mListener = null;
		//return super.onUnbind(intent);
		return false;
	}

	public final IBinder mBinder = new LoggerBinder();

	@Override
	public void run() {
		try {
			Thread.sleep(2000);
		} catch(InterruptedException e) {
			if(!mUploadRun)
				return;
		}
		while(mUploadRun) {
			if(mUploadRunOnce)
				mUploadRun = false;
			if(mUploadCount <= 0) {
				if(!mUploadRun)
					break;
				sendUploadStatus("Sleeping...");
				synchronized(mUploadLock) {
					try {
						while(mUploadCount <= 0)
							mUploadLock.wait();
					} catch(InterruptedException ex) {
						if(!mUploadRun)
							break;
					}
				}
			}
			sendUploadStatus("Sending...");
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
			Cursor c = mDbAdapter.fetchUploadLocations(cols, "uploaded != 1");
			if(!c.moveToFirst()) {
				c.close();
				mUploadCount = 0;
				Log.w(TAG, "No data to upload but count > 0");
				sendUploadStatus("Internal Error: count > 0");
				try {
					Thread.sleep(5000);
				} catch(InterruptedException ex) {
					if(!mUploadRun)
						return;
				}
				continue;
			}

			String baseUrl = mPrefs.getString("url", "http://www.example.org/photocatalog/");
			String source = mPrefs.getString("source", "0");
			List<Integer> idList = new ArrayList<Integer>();
			Multipart m = new Multipart(this);
			m.put("source", source);
			class MyProducer implements ContentProducer {
				Cursor mC;
				List<Integer> mIdList;
				MyProducer(Cursor c, List<Integer> idList) { mC = c; mIdList = idList; }
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
						mIdList.add(mC.getInt(0));
						for(int i = 0; i < cols.length; i++) {
							if(i > 0)
								os.write(",".getBytes());
							if(!mC.isNull(i))
								os.write(mC.getString(i).getBytes());
						}
						os.write("\n".getBytes());
					} while(mC.moveToNext());
				}
			}
			m.put("file", new MyProducer(c, idList));
			m.put("finish", "1");

			//StringBuilder sb = new StringBuilder();
			//sb.append(baseUrl).append("cgi/test-android.pl?loc=").append("123").append(",").append(c.getCount());
			String uri = baseUrl + "cgi/test-android.pl";
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(uri);
			boolean ok = false;
			try {
				post.setEntity(m.getEntity());
				HttpResponse resp = client.execute(post);
				if(resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					//throw 
				}
				//InputStream is = resp.getEntity().getContent();
				//BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String respString = new BasicResponseHandler().handleResponse(resp);
				BufferedReader r = new BufferedReader(new StringReader(respString));
				String line = r.readLine();
				if(line != null && line.equals("OK")) {
					ContentValues values = new ContentValues();
					values.put("uploaded", 1);
					Integer ids[] = idList.toArray(new Integer[1]);
					for(int i = 0; i < ids.length; i++)
						mDbAdapter.updateLocation(ids[i], values);
					sendUploadStatus("Sent!");
					ok = true;
					synchronized(mUploadLock) {
						int uploadedCount = ids.length;
						if(uploadedCount > mUploadCount)
							uploadedCount = mUploadCount;
						mUploadCount -= uploadedCount;
					}
				} else {
					sendUploadStatus("Invalid response: " + (line == null ? "(null)" : line));
				}
			} catch(HttpResponseException ex) {
				sendUploadStatus("HTTP Error: " + ex);
			} catch (IOException ex) {
				sendUploadStatus("IO Error: " + ex);
			} finally {
				//get.releaseConnection();
				c.close();
			}
			try {
				if(ok)
					Thread.sleep(5000);
				else
					Thread.sleep(60000);
			} catch(InterruptedException ex) {
				if(!mUploadRun)
					break;
			}
		}
		sendUploadStatus("Stopped.");
		if(!mIsStarted && mUploadRunOnce)
			stopSelfResult(mUploadRunOnceStartId);
	}
}
