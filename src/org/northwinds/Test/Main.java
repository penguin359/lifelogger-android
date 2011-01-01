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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements LocationListener {
	final String boundary = "ak3fGvsHkRacd-Fhkud4";

	private Logger mBoundLogger = null;

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundLogger = ((Logger.LoggerBinder)service).getService();
			mBoundLogger.setUpdateListener(Main.this);
			mIsListening = true;
		}
		@Override
		public void onServiceDisconnected(ComponentName className) {
			mBoundLogger = null;
			//mIsBound = false;
		}
	};

	LogDbAdapter mDbAdapter = new LogDbAdapter(this);

	View.OnClickListener startGpsOnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			startService(new Intent(Main.this, Logger.class));
			bindService(new Intent(Main.this, Logger.class), mConnection, BIND_AUTO_CREATE);
			mIsBound = true;
			Button b = (Button)v;
			b.setOnClickListener(stopGpsOnClick);
			b.setText("Stop");
			b.setTextColor(0xffff0000);
		}
	};

	View.OnClickListener stopGpsOnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			stopService(new Intent(Main.this, Logger.class));
			if(mIsBound) {
				unbindService(mConnection);
				mIsBound = false;
			}
			Button b = (Button)v;
			b.setOnClickListener(startGpsOnClick);
			b.setText("Start");
			b.setTextColor(0xff00ff00);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		TextView tv = (TextView)findViewById(R.id.status);
		tv.setText("Hello, it worked!");
		StringBuilder sb = new StringBuilder();
		Intent intent = getIntent();
		sb.append("Hello: ");
		sb.append(intent.getAction());
		sb.append(", '");
		if(intent.getData() != null)
			sb.append(intent.getData().toString());
		else
			sb.append("null");
		sb.append("', ");
		sb.append(intent.getType());
		sb.append(" - ");
		if(intent.getCategories() != null) {
			Set<String> set2 = intent.getCategories();
			Iterator<String> it2 = set2.iterator();
			while(it2.hasNext()) {
				String s = it2.next();
				sb.append(" '");
				sb.append(s);
				sb.append(",");
			}
		} else
			sb.append("null");
		sb.append(": ");
		if(intent.getExtras() != null) {
			sb.append(intent.getExtras().toString());
			Set<String> set = intent.getExtras().keySet();
			Iterator<String> it = set.iterator();
			while(it.hasNext()) {
			//String str[] = (String[])set.toArray();
			//for(String s : str) {
				String s = it.next();
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
					//File f = new File(uri);
					//InputStream is = new FileInputStream(f);
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
					char c[] = new char[4];
					isr.read(c, 0, 4);
					sb.append(c);
					sb.append(" done!");
					is.close();

					is = cr.openInputStream(uri);
					BasicHttpEntity entity = new BasicHttpEntity();
					entity.setContent(is);
					HttpClient client = new DefaultHttpClient();
					HttpPost req = new HttpPost(mPrefs.getString("url", "http://www.example.org/photocatalog/") + "cgi/photocatalog.pl");
					//req.setHeader("Content-Type", "multipart/form-data; boundary="+boundary);
					req.setHeader("Content-Type", intent.getType());
					req.setEntity(entity);
					sb.append("Uploading... ");
					try {
						HttpResponse resp = client.execute(req);
						//sb.append(String.format("%03d %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase()));
						sb.append(resp.getStatusLine().getReasonPhrase());
					} catch(Exception e) {
						sb.append(e);
					}
					sb.append(" done!");
				}
			} catch(Exception e) {
				sb.append(e);
			}
		} else
			sb.append("null");

		tv.setText(sb.toString());

		Button b = (Button)findViewById(R.id.list_but);
		b.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			startActivity(new Intent(Main.this, GPSList.class));
		}
	});
		b = (Button)findViewById(R.id.delete_but);
		b.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			mDbAdapter.deleteAllLocations();
		}
	});
		b = (Button)findViewById(R.id.start_but);
		b.setOnClickListener(startGpsOnClick);
		//b = (Button)findViewById(R.id.stop_but);
		//b.setOnClickListener(stopGpsOnClick);
		b = (Button)findViewById(R.id.exit_but);
		b.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Process.killProcess(Process.myPid());
		}
	});

	mDbAdapter.open();
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
			startActivity(new Intent(getBaseContext(), PrefAct.class));
		return true;
		case R.id.quit:
		Process.killProcess(Process.myPid());
		return true;
		}
	return super.onOptionsItemSelected(item);
	}

	public void startUpload(View view) {
		startActivity(new Intent(this, Upload.class));
	}

	private boolean mIsBound = false;
	private boolean mIsListening = false;

//	public void startGPS(View view) {
//		startService(new Intent(this, Logger.class));
//		bindService(new Intent(this, Logger.class), mConnection, BIND_AUTO_CREATE);
//	mIsBound = true;
//	}

	@Override
	public void onStop() {
		super.onStop();
		if(mIsListening) {
			Toast.makeText(this, "Stop listening to GPS", Toast.LENGTH_SHORT).show();
		if(mBoundLogger != null)
			mBoundLogger.setUpdateListener(null);
			mIsListening = false;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if(mIsBound && !mIsListening) {
		if(mBoundLogger != null) {
			mBoundLogger.setUpdateListener(this);
			mIsListening = true;
		}
		}
	}

	SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mIsBound) {
			Toast.makeText(this, "Unbind GPS", Toast.LENGTH_SHORT).show();
		if(mBoundLogger != null)
			mBoundLogger.setUpdateListener(null);
			unbindService(mConnection);
			mIsBound = false;
		mIsListening = false;
		}
	}

	@Override
	public void onLocationChanged(Location loc) {
	TextView tv = (TextView)findViewById(R.id.location);
	StringBuilder sb = new StringBuilder();
	sb.append("Location: [ ");
	sb.append(loc.getLatitude());
	sb.append(", ");
	sb.append(loc.getLongitude());
	sb.append(" ]");
	tv.setText(sb.toString());
	}
	public void onProviderDisabled(String provider) {
	}
	public void onProviderEnabled(String provider) {
	}
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
