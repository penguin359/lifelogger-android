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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore.MediaColumns;
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

	private class Multipart {
		private static final String TAG = "Multipart";

		private LinkedHashMap<String, Object> mFields = new LinkedHashMap<String, Object>();

		private class MultipartEntity extends AbstractHttpEntity {
			private static final String boundary = "fjd3Fb5Xr8Hfrb6hnDv3Lg";

			private ArrayList<Object> mContent = new ArrayList<Object>();
			private long mLength = 0;

			MultipartEntity() {
				contentType = new BasicHeader("Content-Type", "multipart/form-data; boundary=" + boundary);

				StringBuilder sb = new StringBuilder();
				for(Entry<String, Object> entry: mFields.entrySet()) {
					String name = entry.getKey();
					Object obj = entry.getValue();
					sb.append("--").append(boundary).append("\r\n");
					if(obj instanceof String) {
						sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
						sb.append("Content-Type: text/plain; charset=\"utf-8\"\r\n");
						sb.append("Content-Transfer-Encoding: 8bit\r\n");
						sb.append("\r\n");
						sb.append(((String)obj).replace("\n", "\r\n"));
						sb.append("\r\n");
					} else if(obj instanceof InputStream) {
						sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
						sb.append("Content-Type: application/octet-stream\r\n");
						sb.append("Content-Transfer-Encoding: binary\r\n");
						sb.append("\r\n");

						try {
							mContent.add(sb.toString().getBytes("UTF-8"));
						} catch(UnsupportedEncodingException e) {
							Log.e(TAG, "Failed to convert string to UTF-8");
						}
						sb = new StringBuilder();
						mContent.add(obj);
						mLength = -1;

						sb.append("\r\n");
					} else if(obj instanceof Uri) {
						String filename = null;
						String type		= null;
						String size		= null;
						Uri uri = (Uri)obj;
						ContentResolver cr = getContentResolver();
						Cursor cc = cr.query(uri, new String[] {
									MediaColumns.DISPLAY_NAME,
									MediaColumns.MIME_TYPE,
									MediaColumns.SIZE
								}, null, null, null);
						if(cc.moveToFirst()) {
							filename = cc.getString(cc.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME));
							type	 = cc.getString(cc.getColumnIndexOrThrow(MediaColumns.MIME_TYPE));
							size	 = cc.getString(cc.getColumnIndexOrThrow(MediaColumns.SIZE));
						}
						if(type == null)
							type = "application/octet-stream";
						if(size == null)
							mLength = -1;
						else if(mLength >= 0)
							mLength += Long.parseLong(size);
						sb.append("Content-Disposition: form-data; name=\"").append(name);
						if(filename != null)
							sb.append("\"; filename=\"").append(filename);
						sb.append("\"\r\n");
						sb.append("Content-Type: ");
						sb.append(type);
						sb.append("\r\n");
						sb.append("Content-Transfer-Encoding: binary\r\n");
						sb.append("\r\n");

						byte[] str;
						try {
							str = sb.toString().getBytes("UTF-8");
							mContent.add(str);
							if(mLength >= 0)
								mLength += str.length;
							sb = new StringBuilder();
							InputStream is = cr.openInputStream(uri);
							mContent.add(is);
						} catch(UnsupportedEncodingException ex) {
							Log.e(TAG, "Failed to convert string to UTF-8", ex);
						} catch(FileNotFoundException ex) {
							Log.e(TAG, "Failed to open file", ex);
							mLength = -1;
						}

						sb.append("\r\n");
					}
				}
				sb.append("--").append(boundary).append("--\r\n");

				try {
					byte[] str = sb.toString().getBytes("UTF-8");
					mContent.add(str);
					if(mLength >= 0)
						mLength += str.length;
				} catch(UnsupportedEncodingException e) {
					Log.e(TAG, "Failed to convert string to UTF-8");
				}
			}

			public InputStream getContent() throws IOException {
				throw new IOException("Entity only supports writing");
			}

			public long getContentLength() {
				return mLength;
			}

			public boolean isRepeatable() {
				return true;
			}

			public boolean isStreaming() {
				return false;
			}

			public void writeTo(OutputStream os) throws IOException {
				for(Object obj: mContent) {
					if(obj instanceof byte[]) {
						os.write((byte[])obj);
					} else if(obj instanceof InputStream) {
						Log.i(TAG, "Start file");
						InputStream is = (InputStream)obj;
						byte[] buffer = new byte[8192]; // Adjust if you want
						int bytesRead;
						while((bytesRead = is.read(buffer)) != -1) {
							os.write(buffer, 0, bytesRead);
						}
						Log.i(TAG, "Finish file");
					}
				}
			}
		}

		public HttpEntity getEntity() {
			return new MultipartEntity();
		}

		public void put(String name, String value) {
			mFields.put(name, value);
		}

		public void put(String name, InputStream value) {
			mFields.put(name, value);
		}

		public void put(String name, Uri value) {
			mFields.put(name, value);
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
			Multipart m = new Multipart();
			m.put("title", mTitle);
			m.put("description", mDescription);
			m.put("file", is);
			//m.put("file", mUri);
			HttpEntity entity = m.getEntity();
			HttpClient client = new DefaultHttpClient();
			HttpPost req = new HttpPost(mPrefs.getString("url", "http://www.example.org/photocatalog/") + "cgi/photocatalog.pl");
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
