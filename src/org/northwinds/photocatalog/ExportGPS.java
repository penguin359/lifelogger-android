﻿/*
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

//import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

class ExportGPS {
	public static final int TYPE_GPX = 0;
	public static final int TYPE_KML = 1;
	public static final int TYPE_CSV = 2;

	private static final DateFormat mFilenameFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
	private static final DateFormat mGpxFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

	private Context mCtx;

	public ExportGPS(Context ctx) {
		mCtx = ctx;
	}

	public void exportAsGPX(Uri track) {
		// /Android/data/<package_name>/files/
		String date = mFilenameFormat.format(new Date());
		String filename = "photocatalog-" + date;
		exportAsGPX(track, filename);
	}

	public void exportAsGPX(Uri track, String filename) {
		exportAsGPX(track, filename, TYPE_GPX);
		exportAsGPX(track, filename, TYPE_KML);
		exportAsGPX(track, filename, TYPE_CSV);
	}

	public void exportAsGPX(Uri track, String filename, int type) {
		String ext = "";
		switch(type) {
		case TYPE_GPX:
			ext = ".gpx";
			break;
		case TYPE_KML:
			ext = ".kml";
			break;
		case TYPE_CSV:
			ext = ".csv";
			break;
		}
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			Toast.makeText(mCtx, "SD Card is read-only.", Toast.LENGTH_LONG).show();
			return;
		} else if(!Environment.MEDIA_MOUNTED.equals(state)) {
			Toast.makeText(mCtx, "No valid SD Card found.", Toast.LENGTH_LONG).show();
			return;
		}
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, filename + ext);
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
		Cursor trackCursor = null;
		Cursor locationCursor = null;
		try {
			if("tracks".equals(track.getPathSegments().get(0))) {
				try {
					String trackId = track.getPathSegments().get(1);
					trackCursor = mCtx.getContentResolver().query(Uri.withAppendedPath(LifeLog.Tracks.CONTENT_URI, trackId), null, null, null, null);
				} catch(Exception ex) {
				}
			}
			locationCursor = mCtx.getContentResolver().query(track, null, null, null, null);
			writeFile(file, trackCursor, locationCursor, type);
		} catch(IOException ex) {
			Toast.makeText(mCtx, "Exception writing to file: " + ex.toString(), Toast.LENGTH_LONG).show();
		} finally {
			if(trackCursor != null)
				trackCursor.close();
			if(locationCursor != null)
				locationCursor.close();
		}
		Toast.makeText(mCtx, "Saved GPX log to " + file.toString(), Toast.LENGTH_LONG).show();
	}

	public void writeFile(File file, Cursor headerCursor, Cursor bodyCursor, int type) throws FileNotFoundException {
		try {
		PrintWriter wr = new PrintWriter(file, "UTF-8");
		switch(type) {
		case TYPE_GPX:
			wr.print("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
			wr.print("<gpx creator=\"PhotoCatalog\" version=\"1.1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n");
			break;
		case TYPE_KML:
			wr.print("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
			wr.print("<kml xsi:schemaLocation=\"http://www.opengis.net/kml/2.2 ogckml22.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/kml/2.2\">\r\n");
			wr.print("  <Document>\r\n");
			wr.print("    <name></name>\r\n");
			wr.print("    <description></description>\r\n");
			break;
		case TYPE_CSV:
			wr.print("PhotoCatalog v1.0\r\n");
			wr.print("track,timestamp,latitude,longituder\r\n");
			break;
		}
		if(headerCursor != null)
			writeHeader(wr, headerCursor, type);
		writeBody(wr, bodyCursor, type);
		switch(type) {
		case TYPE_GPX:
			wr.print("</gpx>\r\n");
			break;
		case TYPE_KML:
			wr.print("  </Document>\r\n");
			wr.print("</kml>\r\n");
			break;
		case TYPE_CSV:
			break;
		}
		wr.flush();
		wr.close();
		} catch(UnsupportedEncodingException ex) {
		}
	}

	private void writeHeader(PrintWriter wr, Cursor c, int type) {
		if(type != TYPE_GPX)
			return;
		wr.print("  <trk>\r\n");
		int nameCol = c.getColumnIndexOrThrow("name");
		int cmtCol  = c.getColumnIndexOrThrow("cmt");
		int descCol = c.getColumnIndexOrThrow("desc");
		int idCol   = c.getColumnIndexOrThrow("_id");
		int typeCol = c.getColumnIndexOrThrow("type");
		if(!c.moveToFirst())
			return;
		if(!c.isNull(nameCol) &&
		   !"".equals(c.getString(nameCol)))
			wr.print("    <name>"+c.getString(nameCol)+"</name>\r\n");
		if(!c.isNull(cmtCol) &&
		   !"".equals(c.getString(cmtCol)))
			wr.print("    <cmt>"+c.getString(cmtCol)+"</cmt>\r\n");
		if(!c.isNull(descCol) &&
		   !"".equals(c.getString(descCol)))
			wr.print("    <desc>"+c.getString(descCol)+"</desc>\r\n");
		if(!c.isNull(idCol) &&
		   !"".equals(c.getString(idCol)))
			wr.print("    <number>"+c.getString(idCol)+"</number>\r\n");
		if(!c.isNull(typeCol) &&
		   !"".equals(c.getString(typeCol)))
			wr.print("    <type>"+c.getString(typeCol)+"</type>\r\n");
	}

	private void writeBody(PrintWriter wr, Cursor c, int type) {
		switch(type) {
		case TYPE_GPX:
			wr.print("    <trkseg>\r\n");
			break;
		case TYPE_KML:
			break;
		case TYPE_CSV:
			break;
		}
		int trackCol = c.getColumnIndexOrThrow("track");
		int latCol  = c.getColumnIndexOrThrow("latitude");
		int lonCol  = c.getColumnIndexOrThrow("longitude");
		int altCol  = c.getColumnIndexOrThrow("altitude");
		int timeCol = c.getColumnIndexOrThrow("timestamp");
		while(!wr.checkError() && c.moveToNext()) {
			long track  = c.getLong(trackCol);
			double lat  = c.getDouble(latCol);
			double lon  = c.getDouble(lonCol);
			double alt  = c.getDouble(altCol);
			Date gDate  = new Date(c.getLong(timeCol) * 1000);
			String time = mGpxFormat.format(gDate);
			switch(type) {
			case TYPE_GPX:
				wr.format(Locale.US,
					  "      <trkpt lat=\"%f\" lon=\"%f\">\r\n" +
					  "        <ele>%f</ele>\r\n" +
					  "        <time>%s</time>\r\n" +
					  "      </trkpt>\r\n", lat, lon, alt, time);
				break;
			case TYPE_KML:
				wr.format(Locale.US,
						"    <Placemark><TimeStamp><when>%s</when></TimeStamp><Point><coordinates>%f,%f,%f</coordinates></Point></Placemark>\r\n", time, lat, lon, alt);
				break;
			case TYPE_CSV:
				wr.format(Locale.US,
					  "%d,%s,%f,%f,%f\r\n", track, time, lat, lon, alt);
				break;
			}
		}
		switch(type) {
		case TYPE_GPX:
			wr.print("    </trkseg>\r\n");
			wr.print("  </trk>\r\n");
			break;
		case TYPE_KML:
			break;
		case TYPE_CSV:
			break;
		}
	}
}
