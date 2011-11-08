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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends Activity {
	private TextView mTimestamp;
	private TextView mLatitude;
	private TextView mLongitude;
	private TextView mAltitude;
	private TextView mAccuracy;
	private TextView mBearing;
	private TextView mSpeed;
	private TextView mSatellites;

	private static final DateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");

	static String formatCoordinate(double coordinate, boolean isLatitude, boolean unicode) {
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
		if(unicode)
			return String.format("%s  %d°  %d′  %.3f″", direction, degrees, minutes, seconds);
		return String.format("%s  %d deg  %d'  %.3f\"", direction, degrees, minutes, seconds);
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
			case LoggingService.MSG_LOCATION:
				Location loc = (Location)msg.obj;
				if(loc != null) {
					Bundle extras = loc.getExtras();
					String satellites = "";
					if(extras != null && extras.containsKey("satellites"))
						satellites = String.format("%d", extras.getInt("satellites"));
					mTimestamp.setText(mDateFormat.format(new Date()));
					mLatitude.setText(formatCoordinate(loc.getLatitude(), true, true));
					mLongitude.setText(formatCoordinate(loc.getLongitude(), false, true));
					mAltitude.setText(formatUnit(loc.getAltitude(), DISTANCE_TYPE));
					mAccuracy.setText(formatUnit(loc.getAccuracy(), DISTANCE_TYPE));
					mBearing.setText(String.format("%3.1f°", loc.getBearing()));
					mSpeed.setText(formatUnit(loc.getSpeed(), SPEED_TYPE));
					mSatellites.setText(satellites);
				}
				break;
			case LoggingService.MSG_GPS:
				switch(msg.arg1) {
				case LocationProvider.AVAILABLE:
					mGpsStatus.setText(R.string.lp_available);
					break;
				case LocationProvider.OUT_OF_SERVICE:
					mGpsStatus.setText(R.string.lp_out_of_service);
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					mGpsStatus.setText(R.string.lp_temporarily_unavailable);
					break;
				}
				break;
			case LoggingService.MSG_STATUS:
				if(msg.arg1 > 0) {
					mStartButton.setOnClickListener(mStopGpsOnClick);
					mStartButton.setText(R.string.stop);
					mStartButton.setTextColor(
					    getResources().getColor(R.color.stop));
					mGpsStatus.setText(R.string.gps_waiting_for_fix);
				} else {
					mStartButton.setOnClickListener(mStartGpsOnClick);
					mStartButton.setText(R.string.start);
					mStartButton.setTextColor(
					    getResources().getColor(R.color.start));
					mGpsStatus.setText(R.string.gps_idle);
				}
				break;
			case LoggingService.MSG_UPLOAD:
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
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			try {
				Message msg = Message.obtain(null, LoggingService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch(RemoteException ex) {
				mService = null;
			}
		}
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	private SharedPreferences mPrefs = null;

	private final View.OnClickListener mStartGpsOnClick = new View.OnClickListener() {
		public void onClick(View v) {
			startService(new Intent(LoggingService.ACTION_START_LOG, null,
						MainActivity.this, LoggingService.class));
		}
	};

	private final View.OnClickListener mStopGpsOnClick = new View.OnClickListener() {
		public void onClick(View v) {
			startService(new Intent(LoggingService.ACTION_STOP_LOG, null,
						MainActivity.this, LoggingService.class));
		}
	};

	private TextView mTrackName;
	private TextView mGpsStatus;
	private TextView mUploadStatus;

	private Button mStartButton;
	private Button mStatusButton;

	private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsChange = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			if(key.equals("photocatalog")) {
				mStatusButton.setEnabled(mPrefs.getBoolean("photocatalog", false));
			} else if(key.equals("track")) {
				long track = mPrefs.getLong("track", 0);
				if(track <= 0) {
					mTrackName.setText(
					    R.string.continuous_record);
					return;
				}
				Cursor c = getContentResolver().query(ContentUris.withAppendedId(LifeLog.Tracks.CONTENT_URI, track), new String[] { LifeLog.Tracks.NAME }, null, null, null);
				if(c == null)
					return;
				int nameCol = c.getColumnIndex(LifeLog.Tracks.NAME);
				if(nameCol >= 0 && c.moveToFirst())
					mTrackName.setText(getString(R.string.track_format, c.getString(nameCol)));
				c.close();
			}
		}
	};

	//private static class ActivityState {
	//	private boolean isRestart = false;
	//}

	private void parseIntent(Intent intent) {
		String action = intent.getAction();
		if(action != null && action.equals(Intent.ACTION_MAIN) &&
		   intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
		   getLastNonConfigurationInstance() == null &&
		   mPrefs.getBoolean("autoStart", false))
			startService(new Intent(LoggingService.ACTION_START_LOG, null, this, LoggingService.class));
		if(mPrefs.getBoolean("firstTime", true))
			showDialog(DIALOG_FIRST_TIME);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mTrackName = (TextView)findViewById(R.id.track);
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

		mStatusButton = (Button)findViewById(R.id.status_but);
		mStatusButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, UploadActivity.class));
			}
		});
		mStatusButton.setEnabled(mPrefs.getBoolean("photocatalog", false));

		Button b = (Button)findViewById(R.id.list_but);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, GPSListActivity.class));
			}
		});

		b = (Button)findViewById(R.id.delete_uploaded_but);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getContentResolver()
				    .delete(LifeLog.Locations.CONTENT_URI,
					    LifeLog.Locations.UPLOADED + "=1",
					    null);
			}
		});

		b = (Button)findViewById(R.id.delete_but);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getContentResolver()
				    .delete(LifeLog.Locations.CONTENT_URI,
					    null,
					    null);
			}
		});

		b = (Button)findViewById(R.id.upload_once_but);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startService(new Intent(LoggingService.ACTION_UPLOAD_ONCE, null, MainActivity.this, LoggingService.class));
			}
		});

		mStartButton = (Button)findViewById(R.id.start_but);
		mStartButton.setOnClickListener(mStartGpsOnClick);

		bindService(new Intent(this, LoggingService.class), mConnection, BIND_AUTO_CREATE);

		mPrefs.registerOnSharedPreferenceChangeListener(mPrefsChange);

		parseIntent(getIntent());
	}

	@Override
	protected void onResume() {
		super.onResume();

		long track = mPrefs.getLong("track", 0);
		if(track <= 0) {
			mTrackName.setText(
			    R.string.continuous_record);
			return;
		}
		Cursor c = getContentResolver().query(ContentUris.withAppendedId(LifeLog.Tracks.CONTENT_URI, track), new String[] { LifeLog.Tracks.NAME }, null, null, null);
		if(c == null)
			return;
		int nameCol = c.getColumnIndex(LifeLog.Tracks.NAME);
		if(nameCol >= 0 && c.moveToFirst())
			mTrackName.setText(getString(R.string.track_format, c.getString(nameCol)));
		c.close();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		//parseIntent(intent);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new Object();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		long track = mPrefs.getLong("track", 0);
		boolean isATrack = track > 0;
		MenuItem item = menu.findItem(R.id.edit_track);
		if(item != null)
			item.setEnabled(isATrack);
		item = menu.findItem(R.id.continuous_record);
		if(item != null)
			item.setEnabled(isATrack);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.new_track:
			Uri trackUri = getContentResolver().insert(LifeLog.Tracks.CONTENT_URI, null);
			long track = ContentUris.parseId(trackUri);
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putLong("track", track);
			editor.commit();
			if(!mPrefs.getBoolean("editDetailsAtStart", false))
				return true;
		case R.id.edit_track:
			track = mPrefs.getLong("track", 0);
			if(track <= 0)
				return true;
			startActivity(
			    new Intent(Intent.ACTION_EDIT,
				       ContentUris.withAppendedId(LifeLog.Tracks.CONTENT_URI, track),
				       this, EditTrackActivity.class));
			return true;
		case R.id.continuous_record:
			editor = mPrefs.edit();
			editor.putLong("track", 0);
			editor.commit();
			return true;
		case R.id.manage_tracks:
			startActivity(
			    new Intent(this, TrackListActivity.class));
			return true;
		case R.id.settings:
			startActivity(new Intent(this, PreferencesActivity.class));
			return true;
		case R.id.quit:
			startService(new Intent(LoggingService.ACTION_STOP_LOG, null,
						this, LoggingService.class));
			finish();
			return true;
		case R.id.kill:
			Process.killProcess(Process.myPid());
			return true;
		case R.id.save:
			startActivity(
			    new Intent(Intent.ACTION_VIEW,
				       LifeLog.Locations.CONTENT_URI,
				       this, ExportActivity.class));
			return true;
		case R.id.maps:
			startActivity(new Intent(this, MapViewActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(mService != null) {
			try {
				Message msg = Message.obtain(null, LoggingService.MSG_REGISTER_CLIENT);
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
				Message msg = Message.obtain(null, LoggingService.MSG_UNREGISTER_CLIENT);
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
	}

	private static final int DIALOG_FIRST_TIME = 1;

	@Override
	protected Dialog onCreateDialog(int which) {
		switch(which) {
		case DIALOG_FIRST_TIME:
			AlertDialog.Builder builder =
			    new AlertDialog.Builder(this);
			builder.setTitle(R.string.first_time_title);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						    int which) {
					if(((CheckBox)((Dialog)dialog).findViewById(R.id.dont_show_again)).isChecked())
						mPrefs.edit().putBoolean("firstTime", false).commit();
					//dialog.dismiss();
				}
			});
			builder.setNeutralButton(R.string.visit_website, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						    int which) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.north-winds.org/unix/photocatalog/")));
				}
			});
			LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.first_time, null);
			builder.setView(view);
			return builder.create();
		}
		return super.onCreateDialog(which);
	}
}
