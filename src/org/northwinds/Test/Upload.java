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
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * @author Loren M. Lang
 *
 */
public class Upload extends Activity implements Runnable {
	Thread thread;
	Uri uri;
	String title;
	String description;
	Context context2;

	private class Multipart {
		private static final String boundary = "fjd3Fb5Xr8Hfrb6hnDv3Lg";

		public void send(OutputStream os) {
			DataOutputStream dos = new DataOutputStream(os);

			try {
				dos.writeChars("--" + boundary + "\r\n");
				dos.writeChars("Content-Disposition: form-data; name=\"name\"\r\n");
				dos.writeChars("Content-Type: text/plain; name=\"utf-8\"\r\n");
				dos.writeChars("Content-Transfer-Encoding: 8bit\r\n");
				dos.writeChars("\r\n");
			} catch(IOException e) {
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upload);
		ImageView iv = (ImageView)findViewById(R.id.image);
		Intent intent = getIntent();
		try {
			if(intent.getExtras().get(Intent.EXTRA_STREAM) != null) {
				uri = (Uri)intent.getExtras().get(Intent.EXTRA_STREAM);
				iv.setImageURI(uri);
			}
		} catch(Exception e) {
		}
	}

	@Override
	public void onDestroy() {
	super.onDestroy();
	if(thread != null)
		thread.interrupt();
	}

	public void startUpload(View view) {
		title = ((EditText)findViewById(R.id.title)).getText().toString();
		description = ((EditText)findViewById(R.id.description)).getText().toString();

		context2 = getApplicationContext();
		thread = new Thread(this);
		thread.start();

		//Toast.makeText(this, "Thread started", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void run() {
		//Toast toast = new Toast(this);
		//toast.setText("hi");
		//toast.show();
		try {
			Toast.makeText(context2, "Upload started", Toast.LENGTH_SHORT).show();
		} catch(Exception e) {
			Toast a = new Toast(this);
			a.setText("Hi");
		}
	}
}
