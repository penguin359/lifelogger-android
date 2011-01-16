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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
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

/**
 * @author Loren M. Lang
 *
 */
public class Upload extends Activity implements Runnable {
	private static final String TAG = "PhotoCatalog-Upload";

	private Thread mThread;
	private Uri mUri = null;
	private ArrayList<Uri> mUris = null;
	private String mTitle = null;
	private String mDescription = null;
	private String mType = null;

	private static final int MSG_STRING = 0;
	private static final int MSG_PROGRESSWHEEL = 1;
	private static final int MSG_PROGRESSBAR = 2;
	private static final int MSG_PROGRESSBAR_UPDATE = 3;

	private static final int DIALOG_PROGRESSWHEEL = 0;
	private static final int DIALOG_PROGRESSBAR = 1;

	private EditText mTitleView;
	private EditText mDescriptionView;
	private TextView mStatusView;
	private ProgressDialog mProgressbar;

	Handler updateUI = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch(m.what) {
			case MSG_STRING:
				mStatusView.setText((String)m.obj);
				break;
			case MSG_PROGRESSWHEEL:
				if(m.arg1 > 0)
					showDialog(DIALOG_PROGRESSWHEEL);
				else
					dismissDialog(DIALOG_PROGRESSWHEEL);
				break;
			case MSG_PROGRESSBAR:
				if(m.arg1 > 0)
					showDialog(DIALOG_PROGRESSBAR);
				else
					dismissDialog(DIALOG_PROGRESSBAR);
				break;
			case MSG_PROGRESSBAR_UPDATE:
				if(mProgressbar != null) {
					//if(mProgressbar.isIndeterminate()) {
					//	mProgressbar.setIndeterminate(false);
					//	mProgressbar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					//}
					mProgressbar.setProgress(m.arg1);
					mProgressbar.setMax(m.arg2);
				}
				break;
			}
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_PROGRESSWHEEL:
			mProgressbar = new ProgressDialog(this);	
			mProgressbar.setMessage("Uploading.  Please wait...");
			mProgressbar.setCancelable(false);
			return mProgressbar;
		case DIALOG_PROGRESSBAR:
			mProgressbar = new ProgressDialog(this);	
			mProgressbar.setMessage("Uploading.  Please wait...");
			mProgressbar.setCancelable(false);
			//mProgressbar.setIndeterminate(false);
			mProgressbar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			return mProgressbar;
		default:
			return super.onCreateDialog(id);
		}
	}

	SharedPreferences mPrefs = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		boolean multifile = false;
		Intent intent = getIntent();
		String action = intent.getAction();
		if(action != null && action.equals(Intent.ACTION_SEND_MULTIPLE))
			multifile = true;
		mType = intent.getType();
		if(multifile)
			setContentView(R.layout.upload_multifile);
		else if(mType != null && mType.toLowerCase().startsWith("image/"))
			setContentView(R.layout.upload_image);
		else
			setContentView(R.layout.upload);
		Bundle extras = intent.getExtras();

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mStatusView = (TextView)findViewById(R.id.status);
		mTitleView = (EditText)findViewById(R.id.title);
		mDescriptionView = (EditText)findViewById(R.id.description);

		ContentResolver cr = getContentResolver();

		if(mType != null && extras != null) {
			if(mType.toLowerCase().startsWith("text/")) {
				CharSequence text = extras.getCharSequence(Intent.EXTRA_TEXT);
				if(text == null) {
					try {
						Uri uri = extras.getParcelable(Intent.EXTRA_STREAM);
						InputStream is = cr.openInputStream(uri);
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						StringBuilder sb = new StringBuilder();
						/* If file is larger than 1024 characters
						 * then it probably wasn't intended for this
						 */
						char buf[] = new char[1024];
						if(reader.read(buf) > 0) {
							sb.append(buf);
							text = sb.toString();
						}
					} catch(FileNotFoundException ex) {
					} catch(IOException ex) {
					}
				}
				if(text != null)
					mDescriptionView.setText(text);
			} else if(mType.toLowerCase().startsWith("image/") &&
				  !multifile) {
				ImageView iv = (ImageView)findViewById(R.id.image);
				try {
					mUri = extras.getParcelable(Intent.EXTRA_STREAM);
					if(mUri != null) {
						iv.setImageURI(mUri);
						if(mUri.getScheme().equals("content")) {
							Cursor c = cr.query(mUri, new String[] {
									MediaColumns.TITLE
								}, null, null, null);
							if(c.moveToFirst())
								mTitle = c.getString(c.getColumnIndexOrThrow(MediaColumns.TITLE));
							c.close();
						} else {
							mTitle = mUri.getLastPathSegment();
						}
					}
				} catch(Exception ex) {
				}
			}
			if(multifile) {
				mUris = extras.getParcelableArrayList(Intent.EXTRA_STREAM);
			}
		}

		if(mTitle == null && extras != null)
			mTitle = extras.getString(Intent.EXTRA_SUBJECT);
		if(mTitle != null) {
			mTitleView.setText(mTitle);
			mTitleView.selectAll();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mThread != null && mThread.getState() != Thread.State.TERMINATED) {
			mThread.interrupt();
			try {
				mThread.join();
			} catch(InterruptedException ex) {
			}
		}
	}

	public void startUpload(View view) {
		if(mThread != null && mThread.getState() != Thread.State.TERMINATED)
			return;

		mTitle = mTitleView.getText().toString();
		mDescription = mDescriptionView.getText().toString();

		mThread = new Thread(this);
		mThread.start();

		//Toast.makeText(this, "Upload started", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void run() {
		StringBuilder sb = new StringBuilder();
		//try {
			if(mUri != null) {
				sb.append("Reading: ");
				sb.append(mUri.toString());
			}
			updateUI.sendMessage(Message.obtain(updateUI, MSG_STRING, sb.toString()));
			//ContentResolver cr = getContentResolver();
			//InputStream is;
			//try {
			//	is = cr.openInputStream(mUri);
			//} catch(FileNotFoundException ex) {
			//	is = null;
			//}
			Multipart m = new Multipart(this);
			m.put("response", "text");
			m.put("title", mTitle);
			m.put("description", mDescription);
			if(mUri != null)
				m.put("file", mUri);
			if(mUris != null)
				m.put("files", mUris);
			m.put("finish", "1");
			//if(is != null)
			//	m.put("file", is);
			m.setProgressUpdate(new Multipart.ProgressUpdate() {
				@Override
				public void onUpdate(int progress, int max) {
					updateUI.sendMessage(Message.obtain(updateUI, MSG_PROGRESSBAR_UPDATE, progress, max));
				}
			});
			HttpEntity entity = m.getEntity();
			if(entity.getContentLength() >= 0)
				updateUI.sendMessage(Message.obtain(updateUI, MSG_PROGRESSBAR, 1, 0));
			else
				updateUI.sendMessage(Message.obtain(updateUI, MSG_PROGRESSWHEEL, 1, 0));
			HttpClient client = new DefaultHttpClient();
			HttpPost req = new HttpPost(mPrefs.getString("url", "http://www.example.org/photocatalog/") + "upload.pl");
			req.setEntity(entity);
			if(mUri != null)
				sb.append("\n");
			sb.append("Uploading...");
			updateUI.sendMessage(Message.obtain(updateUI, MSG_STRING, sb.toString()));
			try {
				HttpResponse resp = client.execute(req);
				//sb.append(String.format("%03d %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase()));
				//sb.append(resp.getStatusLine().getReasonPhrase());
				String respString = new BasicResponseHandler().handleResponse(resp);
				BufferedReader r = new BufferedReader(new StringReader(respString));
				String line = r.readLine();
				if(line != null && line.equals("OK")) {
					sb.append("\ndone!");
					finish();
				} else {
					sb.append("Error: ");
					sb.append(r.readLine());
				}
			} catch(HttpResponseException ex) {
				Log.e(TAG, "Failed client request", ex);
				sb.append(ex);
			} catch(IOException ex) {
				Log.e(TAG, "Failed to upload file", ex);
				sb.append(ex);
			}
			//sb.append("\ndone!");
			updateUI.sendMessage(Message.obtain(updateUI, MSG_STRING, sb.toString()));
			if(entity.getContentLength() >= 0)
				updateUI.sendMessage(Message.obtain(updateUI, MSG_PROGRESSBAR, 0, 0));
			else
				updateUI.sendMessage(Message.obtain(updateUI, MSG_PROGRESSWHEEL, 0, 0));
		//} catch(ClientProtocolException ex) {
		//}
		updateUI.sendMessage(Message.obtain(updateUI, MSG_STRING, sb.toString()));
	}
}
