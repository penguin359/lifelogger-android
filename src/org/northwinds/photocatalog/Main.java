﻿/*
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

//import java.io.InputStream;
//import java.io.InputStreamReader;

import android.app.Activity;
import android.content.ComponentName;
//import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
//import android.database.Cursor;
import android.location.Location;
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
	private TextView mLatitude;
	private TextView mLongitude;
	private TextView mAltitude;
	private TextView mAccuracy;
	private TextView mBearing;
	private TextView mSpeed;

	private static String formatCoordinate(double coordinate) {
		int degrees = (int)coordinate;
		coordinate -= degrees;
		coordinate *= 60.;
		if(coordinate < 0)
			coordinate *= -1;
		int minutes = (int)coordinate;
		coordinate -= minutes;
		coordinate *= 60.;
		double seconds = coordinate;
		return String.format("%d°  %d′  %.3f″", degrees, minutes, seconds);
	}

	private final Messenger mMessenger = new Messenger(new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case Logger.MSG_LOCATION:
				Location loc = (Location)msg.obj;
				//StringBuilder sb = new StringBuilder();
				//sb.append("Location: [ ");
				//sb.append(loc.getLatitude());
				//sb.append(", ");
				//sb.append(loc.getLongitude());
				//sb.append(" ]");
				//mGpsStatus.setText(sb.toString());
				if(loc != null) {
					mGpsStatus.setText(String.format("Location: [ %.6f, %.6f ]", loc.getLatitude(), loc.getLongitude()));
					mLatitude.setText(formatCoordinate(loc.getLatitude()));
					mLongitude.setText(formatCoordinate(loc.getLongitude()));
					mAltitude.setText(String.format("%.3f m", loc.getAltitude()));
					mAccuracy.setText(String.format("%.3f m", loc.getAccuracy()));
					mBearing.setText(String.format("%3.1f°", loc.getBearing()));
					mSpeed.setText(String.format("%3.1f m/s", loc.getSpeed()));
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
		@Override
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
		@Override
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	private LogDbAdapter mDbAdapter = new LogDbAdapter(this);

	private SharedPreferences mPrefs = null;

	private View.OnClickListener mStartGpsOnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			startService(new Intent(Logger.ACTION_START_LOG, null, Main.this, Logger.class));
		}
	};

	private View.OnClickListener mStopGpsOnClick = new View.OnClickListener() {
		@Override
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
			startService(new Intent(Logger.ACTION_START_LOG, null, Main.this, Logger.class));
		/*
		StringBuilder sb = new StringBuilder();
		sb.append("Hello: ");
		sb.append(intent.getAction());
		sb.append(", '");
		sb.append(intent.getDataString());
		sb.append("', ");
		sb.append(intent.getType());
		sb.append(" - ");
		if(intent.getCategories() != null) {
			for(String s : intent.getCategories()) {
				sb.append(" '");
				sb.append(s);
				sb.append(",");
			}
		} else
			sb.append("null");
		sb.append(": ");
		if(intent.getExtras() != null) {
			sb.append(intent.getExtras().toString());
			for(String s : intent.getExtras().keySet()) {
				sb.append(" '");
				sb.append(s);
				sb.append(",");
				Object o = intent.getExtras().get(s);
				Class<? extends Object> c = o.getClass();
				sb.append(c.getName());
				sb.append("' ");
			}
			try {
				if(intent.getExtras().get(Intent.EXTRA_STREAM) != null) {
					sb.append("Reading: ");
					Uri uri = (Uri)intent.getExtras().get(Intent.EXTRA_STREAM);
					sb.append(uri.toString());
					sb.append(" ");
					ContentResolver cr = getContentResolver();
					InputStream is = cr.openInputStream(uri);
					InputStreamReader isr = new InputStreamReader(is);
					isr.read();
					isr.read();
					isr.read();
					isr.read();
					isr.read();
					isr.read();
					//sb.append(is.read());
					//sb.append(is.read());
					//sb.append(is.read());
					//sb.append(is.read());
					//for(int i = 0; i < 12; i++)
					//	sb.append(String.format("0x%02x ", is.read()));
					char buf[] = new char[4];
					isr.read(buf, 0, 4);
					sb.append(buf);
					sb.append(" done!");
					is.close();
					Cursor c = cr.query(uri, new String[] { MediaColumns.DISPLAY_NAME, MediaColumns.TITLE, MediaColumns.MIME_TYPE, MediaColumns.SIZE }, null, null, null);
					if(c != null && c.moveToFirst()) {
						sb.append("\n'");
						for(int i = 0; i < c.getColumnCount(); i++) {
							if(i > 0)
								sb.append("', '");
							sb.append(c.getString(i));
						}
						sb.append("'");
					}
					c.close();
				}
			} catch(Exception ex) {
				sb.append(ex);
			}
		} else
			sb.append("null");

		mTV.setText(sb.toString());
		*/
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mTV = (TextView)findViewById(R.id.status);
		mTV.setText("Hello, PhotoCataloger!");
		mGpsStatus = (TextView)findViewById(R.id.location);
		mUploadStatus = (TextView)findViewById(R.id.upload);

		mLatitude = (TextView)findViewById(R.id.latitude);
		mLongitude = (TextView)findViewById(R.id.longitude);
		mAltitude = (TextView)findViewById(R.id.altitude);
		mAccuracy = (TextView)findViewById(R.id.accuracy);
		mBearing = (TextView)findViewById(R.id.bearing);
		mSpeed = (TextView)findViewById(R.id.speed);

		Button b = (Button)findViewById(R.id.status_but);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Main.this, Upload.class));
			}
		});

		b = (Button)findViewById(R.id.list_but);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Main.this, GPSList.class));
			}
		});

		b = (Button)findViewById(R.id.delete_uploaded_but);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDbAdapter.deleteUploadedLocations();
			}
		});

		b = (Button)findViewById(R.id.delete_but);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDbAdapter.deleteAllLocations();
			}
		});

		b = (Button)findViewById(R.id.upload_once_but);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(new Intent(Logger.ACTION_UPLOAD_ONCE, null, Main.this, Logger.class));
			}
		});

		//b = (Button)findViewById(R.id.exit_but);
		//b.setOnClickListener(new View.OnClickListener() {
		//	@Override
		//	public void onClick(View v) {
		//		Process.killProcess(Process.myPid());
		//	}
		//});

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
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, PrefAct.class));
			return true;
		case R.id.quit:
			startService(new Intent(Logger.ACTION_STOP_LOG, null, Main.this, Logger.class));
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
