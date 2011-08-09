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

//import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

public class LogDbAdapter {
	private static final String TAG = "PhotoCatalog-LogDbAdapter";

	public static final String KEY_ROWID     = "_id";
	public static final String KEY_TRACK     = "track";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_LATITUDE  = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_ALTITUDE  = "altitude";
	public static final String KEY_ACCURACY  = "accuracy";
	public static final String KEY_BEARING   = "bearing";
	public static final String KEY_SPEED     = "speed";
	public static final String KEY_SATELLITES= "satellites";
	public static final String KEY_UPLOADED  = "uploaded";

	public static final String KEY_NAME      = "name";
	public static final String KEY_CMT       = "cmt";
	public static final String KEY_DESC      = "desc";
	public static final String KEY_TYPE      = "type";

	private static final String DATABASE_NAME = "data.db";
	private static final int DATABASE_VERSION = 2;
	private static final String TABLE_LOCATION = "locations";
	private static final String TABLE_TRACKS = "tracks";

	private static final String TABLE_LOCATION_CREATE = "CREATE TABLE " + TABLE_LOCATION + " (" +
			KEY_ROWID     + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			KEY_TRACK     + " INTEGER NOT NULL DEFAULT 0, " +
			KEY_TIMESTAMP + " INTEGER NOT NULL, " +
			KEY_LATITUDE  + " DOUBLE NOT NULL, " +
			KEY_LONGITUDE + " DOUBLE NOT NULL, " +
			KEY_ALTITUDE  + " DOUBLE, " +
			KEY_ACCURACY  + " REAL, " +
			KEY_BEARING   + " REAL, " +
			KEY_SPEED     + " REAL, " +
			KEY_SATELLITES+ " INTEGER, " +
			KEY_UPLOADED  + " BOOLEAN NOT NULL DEFAULT FALSE" +
			")";
	private static final String TABLE_TRACKS_CREATE = "CREATE TABLE " + TABLE_TRACKS + " (" +
			KEY_ROWID     + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			KEY_NAME      + " TEXT, " +
			KEY_CMT       + " TEXT, " +
			KEY_DESC      + " TEXT, " +
			KEY_TYPE      + " TEXT, " +
			KEY_UPLOADED  + " BOOLEAN NOT NULL DEFAULT FALSE" +
			")";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(TABLE_LOCATION_CREATE);
			db.execSQL(TABLE_TRACKS_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " +
					   newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACKS);
			onCreate(db);
		}
	}

	public LogDbAdapter(Context ctx) {
		mCtx = ctx;
	}

	public LogDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public Cursor fetchTrack(long rowId) throws SQLException {
		Cursor c = mDb.query(true, TABLE_TRACKS, new String[] {
			KEY_ROWID, KEY_NAME, KEY_CMT, KEY_TYPE, KEY_DESC
		}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchAllTracks() throws SQLException {
		Cursor c = mDb.query(true, TABLE_TRACKS, new String[] {
			KEY_ROWID, KEY_NAME, KEY_CMT, KEY_TYPE, KEY_DESC
		}, null, null, null, null, null, null);
		return c;
	}

	public Cursor fetchLocation(long rowId) throws SQLException {
		Cursor c = mDb.query(true, TABLE_LOCATION, new String[] {
			KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE
		}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchAllLocations() {
		//return mDb.query(TABLE_LOCATION, new String[] {
		//	KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE
		//}, null, null, null, null, null);
		return mDb.rawQuery("SELECT _id, datetime(round(timestamp), 'unixepoch') AS timestamp, latitude, longitude, uploaded, altitude FROM locations", null);
	}

	public Cursor fetchAllLocations2() {
		return mDb.query(TABLE_LOCATION, new String[] {
			KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE
		}, null, null, null, null, null);
	}

	public Cursor fetchLocationsByTrack(long track) {
		if(track == 0)
			return fetchAllLocations2();
		return mDb.query(TABLE_LOCATION, new String[] {
			KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE
		}, KEY_TRACK + " = " + track, null, null, null, null);
	}

	public Cursor fetchUploadLocations(String[] cols, String condition) {
		return mDb.query(TABLE_LOCATION, cols, condition, null, null, null, null, "100");
	}

	public int countUploadLocations() {
		Cursor c = mDb.rawQuery("SELECT count(*) FROM locations WHERE uploaded != 1", null);
		int count = 0;
		if(c.moveToFirst())
			count = c.getInt(0);
		c.close();
		return count;
	}

	public long insertLocation(long track, Location location) {
		ContentValues values = new ContentValues();
		values.put(KEY_TRACK,     track);
		values.put(KEY_TIMESTAMP, location.getTime()/1000);
		values.put(KEY_LATITUDE,  location.getLatitude());
		values.put(KEY_LONGITUDE, location.getLongitude());
		if(location.hasAltitude())
			values.put(KEY_ALTITUDE, location.getAltitude());
		if(location.hasAccuracy())
			values.put(KEY_ACCURACY, location.getAccuracy());
		if(location.hasBearing())
			values.put(KEY_BEARING,  location.getBearing());
		if(location.hasSpeed())
			values.put(KEY_SPEED,    location.getSpeed());
		Bundle extras = location.getExtras();
		if(extras != null && extras.containsKey("satellites"))
			values.put(KEY_SATELLITES, extras.getInt("satellites"));
		//StringBuilder sb = new StringBuilder();
		//if(extras != null)
		//	sb.append("Location extras(").append(extras.size()).append("): ");
		//if(extras != null && extras.size() > 0) {
		//	Set<String> set = extras.keySet();
		//	for(String name: set) {
		//		sb.append(name);
		//		sb.append(" ISA ");
		//		sb.append(extras.get(name).getClass().getName());
		//		sb.append(", ");
		//	}
		//}
		//Log.v(TAG, sb.toString());
		//Toast.makeText(Logger.this, sb.toString(), Toast.LENGTH_SHORT).show();

		return mDb.insert(TABLE_LOCATION, null, values);
	}

	public boolean updateTrack(long rowId, ContentValues values) {
		return mDb.update(TABLE_TRACKS, values, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateLocation(long rowId, Location location) {
		ContentValues args = new ContentValues();
		args.put(KEY_TIMESTAMP, location.getTime()/1000);
		args.put(KEY_LATITUDE, location.getLatitude());
		args.put(KEY_LONGITUDE, location.getLongitude());

		return mDb.update(TABLE_LOCATION, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateLocation(long rowId, ContentValues args) {
		return mDb.update(TABLE_LOCATION, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteTrack(long rowId) {
		return mDb.delete(TABLE_TRACKS, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteLocation(long rowId) {
		return mDb.delete(TABLE_LOCATION, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteUploadedLocations() {
		return mDb.delete(TABLE_LOCATION, "uploaded = 1", null) > 0;
	}

	public boolean deleteAllLocations() {
		return mDb.delete(TABLE_LOCATION, null, null) > 0;
	}

	public long newTrack(ContentValues values) {
		return mDb.insert(TABLE_TRACKS, KEY_NAME, values);
	}
}
