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
	public static final String KEY_ROWID     = "_id";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_LATITUDE  = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_ALTITUDE  = "altitude";
	public static final String KEY_ACCURACY  = "accuracy";
	public static final String KEY_BEARING   = "bearing";
	public static final String KEY_SPEED     = "speed";
	public static final String KEY_SATELLITES= "satellites";
	public static final String KEY_UPLOADED  = "uploaded";

	private static final String TAG = "LogDbAdapter";

	private static final String DATABASE_NAME = "data.db";
	private static final String DATABASE_TABLE = "locations";
	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE + " (" +
			KEY_ROWID     + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
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

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " +
					   newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
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

	public Cursor fetchLocation(long rowId) throws SQLException {
		Cursor c = mDb.query(true, DATABASE_TABLE, new String[] {
			KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE
		}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchAllLocations() {
		//return mDb.query(DATABASE_TABLE, new String[] {
		//	KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE
		//}, null, null, null, null, null);
		return mDb.rawQuery("SELECT _id, datetime(round(timestamp/1000), 'unixepoch') AS timestamp, latitude, longitude, uploaded FROM locations", null);
	}

	public Cursor fetchUploadLocations(String[] cols, String condition) {
		return mDb.query(DATABASE_TABLE, cols, condition, null, null, null, null, "100");
	}

	public int countUploadLocations() {
		Cursor c = mDb.rawQuery("SELECT count(*) FROM locations WHERE uploaded != 1", null);
		int count = 0;
		if(c.moveToFirst())
			count = c.getInt(0);
		c.close();
		return count;
	}

	public long insertLocation(Location location) {
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, location.getTime());
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

		return mDb.insert(DATABASE_TABLE, null, values);
	}

	public boolean updateLocation(long rowId, Location location) {
		ContentValues args = new ContentValues();
		args.put(KEY_TIMESTAMP, location.getTime());
		args.put(KEY_LATITUDE, location.getLatitude());
		args.put(KEY_LONGITUDE, location.getLongitude());

		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateLocation(long rowId, ContentValues args) {
		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteLocation(long rowId) {
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteUploadedLocations() {
		return mDb.delete(DATABASE_TABLE, "uploaded = 1", null) > 0;
	}

	public boolean deleteAllLocations() {
		return mDb.delete(DATABASE_TABLE, null, null) > 0;
	}
}
