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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

class ExportGPS {
	private static final DateFormat mFilenameFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
	private static final DateFormat mGpxFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	private Context mCtx;

	public ExportGPS(Context ctx) {
		mCtx = ctx;
	}

	public void exportAsGPX(Uri track) {
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			Toast.makeText(mCtx, "SD Card is read-only.", Toast.LENGTH_LONG).show();
			return;
		} else if(!Environment.MEDIA_MOUNTED.equals(state)) {
			Toast.makeText(mCtx, "No valid SD Card found.", Toast.LENGTH_LONG).show();
			return;
		}
		File root = Environment.getExternalStorageDirectory();
		// /Android/data/<package_name>/files/
		String date = mFilenameFormat.format(new Date());
		String filename = "photocatalog-" + date + ".gpx";
		File file = new File(root, filename);
		try {
			file.createNewFile();
		} catch(IOException ex) {
		}
		if(!file.canRead()) {
			Toast.makeText(mCtx, "Can't read " + file.toString() + " file!", Toast.LENGTH_LONG).show();
			return;
		}
		if(!file.canWrite()) {
			Toast.makeText(mCtx, "Can't write file!", Toast.LENGTH_LONG).show();
			return;
		}
		try {
			OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
			os.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes());
			os.write("<gpx creator=\"PhotoCatalog\" version=\"1.1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n".getBytes());
			os.write("  <trk>\n    <trkseg>\n".getBytes());
			Cursor c = mCtx.getContentResolver().query(track, null, null, null, null);
			int latCol = c.getColumnIndexOrThrow("latitude");
			int lonCol = c.getColumnIndexOrThrow("longitude");
			int altCol = c.getColumnIndexOrThrow("altitude");
			int timeCol = c.getColumnIndexOrThrow("timestamp");
			while(c.moveToNext()) {
				double lat = c.getDouble(latCol);
				double lon = c.getDouble(lonCol);
				double alt = c.getDouble(altCol);
				Date gDate = new Date(c.getLong(timeCol) * 1000);
				String time = mGpxFormat.format(gDate);
				String line = String.format("      <trkpt lat=\"%f\" lon=\"%f\"><ele>%f</ele><time>%s</time></trkpt>\n", lat, lon, alt, time);
				os.write(line.getBytes());
			}
			c.close();
			os.write("    </trkseg>\n  </trk>\n</gpx>\n".getBytes());
			os.close();
		} catch(IOException ex) {
			Toast.makeText(mCtx, "Exception writing to file: " + ex.toString(), Toast.LENGTH_LONG).show();
		}
		Toast.makeText(mCtx, "Saved GPX log to " + file.toString(), Toast.LENGTH_LONG).show();
	}
}
