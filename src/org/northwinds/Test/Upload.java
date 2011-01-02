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

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
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

		private static final String boundary = "fjd3Fb5Xr8Hfrb6hnDv3Lg";

		public static void send(OutputStream os, Set<Entry<String,Object>> set) {
			DataOutputStream dos = new DataOutputStream(os);

			try {
				for(Entry<String,Object> entry : set) {
					String name = entry.getKey();
					Object obj = entry.getValue();
					dos.writeChars("--" + boundary + "\r\n");
					if(obj instanceof Uri) {
						String value = "a";
						String filename = "f";
						dos.writeChars("Content-Disposition: form-data; name=\"" + value + "\"; filename=\"" + filename + "\"\r\n");
						dos.writeChars("Content-Type: application/octet-stream\r\n");
						dos.writeChars("Content-Transfer-Encoding: binary\r\n");
						dos.writeChars("\r\n");
					} else {
						String value = obj.toString();
						dos.writeChars("Content-Disposition: form-data; name=\"" + value + "\"\r\n");
						dos.writeChars("Content-Type: text/plain; name=\"utf-8\"\r\n");
						dos.writeChars("Content-Transfer-Encoding: 8bit\r\n");
						dos.writeChars("\r\n");
						dos.writeChars("\r\n");
					}
				}
				dos.writeChars("--" + boundary + "--\r\n");
			} catch(IOException e) {
				Log.e(TAG, "Failed to upload form", e);
			}
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
			InputStream is = cr.openInputStream(mUri);
			BasicHttpEntity entity = new BasicHttpEntity();
			entity.setContent(is);
			HttpClient client = new DefaultHttpClient();
			HttpPost req = new HttpPost(mPrefs.getString("url", "http://www.example.org/photocatalog/") + "cgi/photocatalog.pl");
			//req.setHeader("Content-Type", "multipart/form-data; boundary="+boundary);
			req.setHeader("Content-Type", mType);
			req.setEntity(entity);
			sb.append("\nUploading...");
			updateUI.sendMessage(Message.obtain(updateUI, MSG_STRING, sb.toString()));
			try {
				HttpResponse resp = client.execute(req);
				//sb.append(String.format("%03d %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase()));
				sb.append(resp.getStatusLine().getReasonPhrase());
			} catch(Exception e) {
				sb.append(e);
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
