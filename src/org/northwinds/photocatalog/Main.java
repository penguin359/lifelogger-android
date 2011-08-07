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

//import java.io.InputStream;
//import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ComponentName;
//import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
//import android.database.Cursor;
import android.location.Location;
import android.location.LocationProvider;
//import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.preference.PreferenceManager;
//import android.provider.MediaStore.MediaColumns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
//import android.widget.Toast;

public class Main extends Activity {
	private TextView mTimestamp;
	private TextView mLatitude;
	private TextView mLongitude;
	private TextView mAltitude;
	private TextView mAccuracy;
	private TextView mBearing;
	private TextView mSpeed;
	private TextView mSatellites;

	private static final DateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");

	private static String formatCoordinate(double coordinate, boolean isLatitude) {
		String direction = "N";
		if(!isLatitude)
			direction = "E";
		if(coordinate < 0) {
			direction = "S";
			if(!isLatitude)
				direction = "W";
			coordinate *= -1;
		}
		int degrees = (int)coordinate;
		coordinate -= degrees;
		coordinate *= 60.;
		int minutes = (int)coordinate;
		coordinate -= minutes;
		coordinate *= 60.;
		double seconds = coordinate;
		return String.format("%s  %d°  %d′  %.3f″", direction, degrees, minutes, seconds);
	}

	private static final int NAUTICAL_UNITS	= 0;
	private static final int METRIC_UNITS	= 1;
	private static final int STATUTE_UNITS	= 2;

	private static final int DISTANCE_TYPE	= 0;
	private static final int SPEED_TYPE	= 1;

	private static final double meters2feet = 3.2808399;
	private static final double mps2mph = meters2feet/5280*3600;
	private static final double mps2knots = 3600/1852;

	private String formatUnit(double value, int type) {
		int units = Integer.parseInt(mPrefs.getString("units", "1"));
		switch(units) {
		case NAUTICAL_UNITS:
			switch(type) {
			case DISTANCE_TYPE:
				return String.format("%.3f ft", value*meters2feet);
			case SPEED_TYPE:
				return String.format("%3.1f knots", value*mps2knots);
			default:
				return "Unknown value type";
			}
		case METRIC_UNITS:
			switch(type) {
			case DISTANCE_TYPE:
				return String.format("%.3f m", value);
			case SPEED_TYPE:
				return String.format("%3.1f m/s", value);
			default:
				return "Unknown value type";
			}
		case STATUTE_UNITS:
			switch(type) {
			case DISTANCE_TYPE:
				return String.format("%.3f ft", value*meters2feet);
			case SPEED_TYPE:
				return String.format("%3.1f mph", value*mps2mph);
			default:
				return "Unknown value type";
			}
		default:
			return "Unknown unit type";
		}
	}

	private final Messenger mMessenger = new Messenger(new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case Logger.MSG_LOCATION:
				Location loc = (Location)msg.obj;
				if(loc != null) {
					//mGpsStatus.setText(String.format("Location: [ %.6f, %.6f ]", loc.getLatitude(), loc.getLongitude()));
					Bundle extras = loc.getExtras();
					String satellites = "";
					if(extras != null && extras.containsKey("satellites"))
						satellites = String.format("%d", extras.getInt("satellites"));
					//mGpsStatus.setText("Tracking...");
					mTimestamp.setText(mDateFormat.format(new Date()));
					mLatitude.setText(formatCoordinate(loc.getLatitude(), true));
					mLongitude.setText(formatCoordinate(loc.getLongitude(), false));
					mAltitude.setText(formatUnit(loc.getAltitude(), DISTANCE_TYPE));
					mAccuracy.setText(formatUnit(loc.getAccuracy(), DISTANCE_TYPE));
					mBearing.setText(String.format("%3.1f°", loc.getBearing()));
					mSpeed.setText(formatUnit(loc.getSpeed(), SPEED_TYPE));
					mSatellites.setText(satellites);
				}
				break;
			case Logger.MSG_GPS:
				switch(msg.arg1) {
				case LocationProvider.AVAILABLE:
					mGpsStatus.setText("Tracking...");
					break;
				case LocationProvider.OUT_OF_SERVICE:
					mGpsStatus.setText("GPS Out of Service");
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					mGpsStatus.setText("GPS Unavailable");
					break;
				}
				break;
			case Logger.MSG_STATUS:
				if(msg.arg1 > 0) {
					mStartButton.setOnClickListener(mStopGpsOnClick);
					mStartButton.setText("Stop");
					mStartButton.setTextColor(0xffff0000);
					mGpsStatus.setText("GPS waiting for fix");
				} else {
					mStartButton.setOnClickListener(mStartGpsOnClick);
					mStartButton.setText("Start");
					mStartButton.setTextColor(0xff00ff00);
					mGpsStatus.setText("GPS Idle");
				}
				break;
			case Logger.MSG_UPLOAD:
				mUploadStatus.setText((String)msg.obj);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	});

	private Messenger mService = null;

	private ServiceConnection mConnection = new ServiceConnection() {
		//@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			try {
				Message msg = Message.obtain(null, Logger.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch(RemoteException ex) {
				mService = null;
			}
		}
		//@Override
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	private SharedPreferences mPrefs = null;

	private final LogDbAdapter mDbAdapter = new LogDbAdapter(this);

	private final View.OnClickListener mStartGpsOnClick = new View.OnClickListener() {
		//@Override
		public void onClick(View v) {
			startService(new Intent(Logger.ACTION_START_LOG, null, Main.this, Logger.class));
		}
	};

	private final View.OnClickListener mStopGpsOnClick = new View.OnClickListener() {
		//@Override
		public void onClick(View v) {
			startService(new Intent(Logger.ACTION_STOP_LOG, null, Main.this, Logger.class));
		}
	};

	private TextView mTV;
	private TextView mGpsStatus;
	private TextView mUploadStatus;

	private Button mStartButton;

	private void parseIntent(Intent intent) {
		String action = intent.getAction();
		if(action != null && action.equals(Intent.ACTION_MAIN) &&
		   intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
		   mPrefs.getBoolean("autoStart", false))
			startService(new Intent(Logger.ACTION_START_LOG, null, this, Logger.class));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mTV = (TextView)findViewById(R.id.status);
		mTV.setText("Hello, PhotoCatalogger!");
		mGpsStatus = (TextView)findViewById(R.id.location);
		mUploadStatus = (TextView)findViewById(R.id.upload);

		mTimestamp = (TextView)findViewById(R.id.timestamp);
		mLatitude = (TextView)findViewById(R.id.latitude);
		mLongitude = (TextView)findViewById(R.id.longitude);
		mAltitude = (TextView)findViewById(R.id.altitude);
		mAccuracy = (TextView)findViewById(R.id.accuracy);
		mBearing = (TextView)findViewById(R.id.bearing);
		mSpeed = (TextView)findViewById(R.id.speed);
		mSatellites = (TextView)findViewById(R.id.satellites);

		Button b = (Button)findViewById(R.id.status_but);
		b.setOnClickListener(new View.OnClickListener() {
			//@Override
			public void onClick(View v) {
				startActivity(new Intent(Main.this, Upload.class));
			}
		});

		b = (Button)findViewById(R.id.list_but);
		b.setOnClickListener(new View.OnClickListener() {
			//@Override
			public void onClick(View v) {
				startActivity(new Intent(Main.this, GPSList.class));
			}
		});

		b = (Button)findViewById(R.id.delete_uploaded_but);
		b.setOnClickListener(new View.OnClickListener() {
			//@Override
			public void onClick(View v) {
				mDbAdapter.deleteUploadedLocations();
			}
		});

		b = (Button)findViewById(R.id.delete_but);
		b.setOnClickListener(new View.OnClickListener() {
			//@Override
			public void onClick(View v) {
				mDbAdapter.deleteAllLocations();
			}
		});

		b = (Button)findViewById(R.id.upload_once_but);
		b.setOnClickListener(new View.OnClickListener() {
			//@Override
			public void onClick(View v) {
				startService(new Intent(Logger.ACTION_UPLOAD_ONCE, null, Main.this, Logger.class));
			}
		});

		mStartButton = (Button)findViewById(R.id.start_but);
		mStartButton.setOnClickListener(mStartGpsOnClick);

		mDbAdapter.open();
		bindService(new Intent(this, Logger.class), mConnection, BIND_AUTO_CREATE);

		parseIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		//parseIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		long track = mPrefs.getLong("track", 0);
		if(track <= 0) {
			MenuItem item = menu.findItem(R.id.edit_track);
			if(item != null)
				item.setEnabled(false);
			item = menu.findItem(R.id.continuous_record);
			if(item != null)
				item.setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.new_track:
			long track = mDbAdapter.newTrack(new ContentValues());
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putLong("track", track);
			editor.commit();
			if(!mPrefs.getBoolean("editTrackAtStart", false))
				return true;
		case R.id.edit_track:
			track = mPrefs.getLong("track", 0);
			if(track <= 0)
				return true;
			Intent intent = new Intent(this, EditTrackActivity.class);
			intent.putExtra("track", track);
			startActivity(intent);
			return true;
		case R.id.continuous_record:
			editor = mPrefs.edit();
			editor.putLong("track", 0);
			editor.commit();
			return true;
		case R.id.settings:
			startActivity(new Intent(this, PrefAct.class));
			return true;
		case R.id.quit:
			startService(new Intent(Logger.ACTION_STOP_LOG, null, this, Logger.class));
			finish();
			return true;
		case R.id.kill:
			Process.killProcess(Process.myPid());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(mService != null) {
			try {
				Message msg = Message.obtain(null, Logger.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch(RemoteException ex) {
				mService = null;
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(mService != null) {
			try {
				Message msg = Message.obtain(null, Logger.MSG_UNREGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch(RemoteException ex) {
				mService = null;
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mConnection);
		mDbAdapter.close();
	}
}
