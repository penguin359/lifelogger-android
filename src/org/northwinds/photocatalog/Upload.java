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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Loren M. Lang
 *
 */
public class Upload extends Activity implements Runnable {
	private static final String TAG = "Upload";

	Thread mThread;
	Uri mUri;
	String mTitle;
	String mDescription;
	String mType;

	private static class Multipart {
		private static final String TAG = "Multipart";

		public static final String boundary = "fjd3Fb5Xr8Hfrb6hnDv3Lg";

		public static void send(OutputStream os, Set<Entry<String,Object>> set) {
			DataOutputStream dos = new DataOutputStream(os);

			try {
				Log.i(TAG, "Starting the push");
				for(Entry<String,Object> entry : set) {
					String name = entry.getKey();
					Object obj = entry.getValue();
					//dos.writeUTF("--" + boundary + "\r\n");
					dos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
					//if(obj instanceof Uri) {
					if(obj instanceof InputStream) {
						//ContentResolver cr = getContentResolver();
						//InputStream is = cr.openInputStream(mUri);
						String filename = "f";
						InputStream is = (InputStream)obj;
						dos.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes("UTF-8"));
						dos.write("Content-Type: application/octet-stream\r\n".getBytes("UTF-8"));
						dos.write("Content-Transfer-Encoding: binary\r\n".getBytes("UTF-8"));
						dos.write("\r\n".getBytes("UTF-8"));

						Log.i(TAG, "Start file");
					    byte[] buffer = new byte[1024]; // Adjust if you want
					    int bytesRead;
					    while((bytesRead = is.read(buffer)) != -1) {
					        dos.write(buffer, 0, bytesRead);
					    }
						Log.i(TAG, "Finish file");

						dos.write("\r\n".getBytes("UTF-8"));
					} else {
						String value = obj.toString();
						//dos.writeUTF("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
						//dos.writeUTF("Content-Type: text/plain; name=\"utf-8\"\r\n");
						//dos.writeUTF("Content-Transfer-Encoding: 8bit\r\n");
						//dos.writeUTF("\r\n");
						//dos.writeUTF(value);
						//dos.writeUTF("\r\n");
						StringBuilder sb = new StringBuilder();
						sb.append("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
						sb.append("Content-Type: text/plain; charset=\"utf-8\"\r\n");
						sb.append("Content-Transfer-Encoding: 8bit\r\n");
						sb.append("\r\n");
						sb.append(value);
						sb.append("\r\n");
						dos.write(sb.toString().getBytes("UTF-8"));
					}
				}
				dos.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
			} catch(IOException e) {
				Log.e(TAG, "Failed to upload form", e);
			}
			Log.i(TAG, "done pushing");
		}
	}

	private static final int MSG_STRING = 0;
	private TextView mStatus;

	Handler updateUI = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch(m.what) {
			case MSG_STRING:
				mStatus.setText((String)m.obj);
				break;
			}
		}
	};

	SharedPreferences mPrefs = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upload);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mStatus = (TextView)findViewById(R.id.status);

		ImageView iv = (ImageView)findViewById(R.id.image);
		Intent intent = getIntent();
		mType = intent.getType();
		try {
			if(intent.getExtras().get(Intent.EXTRA_STREAM) != null) {
				mUri = (Uri)intent.getExtras().get(Intent.EXTRA_STREAM);
				iv.setImageURI(mUri);
			}
		} catch(Exception e) {
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mThread != null)
			mThread.interrupt();
	}

	public void startUpload(View view) {
		mTitle = ((EditText)findViewById(R.id.title)).getText().toString();
		mDescription = ((EditText)findViewById(R.id.description)).getText().toString();

		mThread = new Thread(this);
		mThread.start();

		Toast.makeText(this, "Upload started", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void run() {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("Reading: ");
			sb.append(mUri.toString());
			updateUI.sendMessage(Message.obtain(updateUI, MSG_STRING, sb.toString()));
			ContentResolver cr = getContentResolver();
			final InputStream is = cr.openInputStream(mUri);
			//BasicHttpEntity entity = new BasicHttpEntity();
			//PipedInputStream is2 = new PipedInputStream();
			//final PipedOutputStream os = new PipedOutputStream(is2);
			//new Thread() {
			//	@Override
			//	public void run() {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
					Map<String, Object> map = new LinkedHashMap<String, Object>();
					map.put("title", mTitle);
					map.put("description", mDescription);
					//map.put("file", mUri);
					map.put("file", is);
					Log.i(TAG, "Start pipe");
						Multipart.send(os, map.entrySet());
					//} catch(IOException e) { Log.e(TAG, "Failed send", e); }
			ByteArrayEntity entity = new ByteArrayEntity(os.toByteArray());
			//	}
			//}.start();
			//entity.setContent(is2);
			HttpClient client = new DefaultHttpClient();
			HttpPost req = new HttpPost(mPrefs.getString("url", "http://www.example.org/photocatalog/") + "cgi/photocatalog.pl");
			req.setHeader("Content-Type", "multipart/form-data; boundary="+Multipart.boundary);
			//req.setHeader("Content-Type", mType);
			req.setEntity(entity);
			sb.append("\nUploading...");
			updateUI.sendMessage(Message.obtain(updateUI, MSG_STRING, sb.toString()));
			try {
				HttpResponse resp = client.execute(req);
				//sb.append(String.format("%03d %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase()));
				sb.append(resp.getStatusLine().getReasonPhrase());
			} catch(Exception e) {
				sb.append(e);
				Log.e(TAG, "Failed client request", e);
			}
			sb.append("\ndone!");
			updateUI.sendMessage(Message.obtain(updateUI, MSG_STRING, sb.toString()));
		} catch(Exception e) {
			Log.e(TAG, "Failed to upload file", e);
			sb.append(e);
		}
		updateUI.sendMessage(Message.obtain(updateUI, MSG_STRING, sb.toString()));
	}
}
