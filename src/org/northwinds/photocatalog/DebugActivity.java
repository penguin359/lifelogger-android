/*
 * Copyright (c) 2010-2011, Loren M. Lang
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

import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.widget.TextView;
import android.widget.Toast;

public class DebugActivity extends Activity implements LocationListener {

	public void onLocationChanged(Location location) {
	}

	public void onProviderDisabled(String provider) {
		Toast.makeText(this, provider + " disabled", Toast.LENGTH_SHORT).show();
	}

	public void onProviderEnabled(String provider) {
		Toast.makeText(this, provider + " disabled", Toast.LENGTH_SHORT).show();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TextView tv = new TextView(this);
		setContentView(tv);
		Intent intent = getIntent();
		StringBuilder sb = new StringBuilder();
		sb.append("Hello: ");
		sb.append(intent.getAction());
		sb.append(", '");
		sb.append(intent.getDataString());
		sb.append("', [");
		sb.append(intent.getType());
		sb.append("] - ");
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
			sb.append(intent.getExtras());
			for(String s : intent.getExtras().keySet()) {
				sb.append(" '");
				sb.append(s);
				sb.append("': ");
				Object o = intent.getExtras().get(s);
				Class<? extends Object> c = o.getClass();
				sb.append(c.getName());
				sb.append(", ");
			}
			try {
				if(intent.getExtras().get(Intent.EXTRA_STREAM) != null) {
					sb.append("\nReading: ");
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
					char buf[] = new char[4];
					isr.read(buf, 0, 4);
					sb.append(buf);
					sb.append(" done!\n");
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

		tv.setText(sb.toString());
	}

	@Override
	protected void onStart() {
		super.onStart();
		Toast.makeText(this, "Start", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onStop() {
		Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Toast.makeText(this, "Destroy", Toast.LENGTH_SHORT).show();
		super.onDestroy();
	}
}
