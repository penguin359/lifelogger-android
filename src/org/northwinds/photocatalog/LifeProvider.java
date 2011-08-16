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

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class LifeProvider extends ContentProvider {
	private static final String TAG = "PhotoCatalog-LifeProvider";

	private static final String DATABASE_NAME = "data.db";
	private static final int DATABASE_VERSION = 2;
	private static final String TABLE_LOCATIONS = "locations";
	private static final String TABLE_TRACKS = "tracks";

	private static HashMap<String, String> sLocationsProjectionMap;

	private static final int LOCATIONS = 1;
	private static final int LOCATIONS_ID = 2;
	private static final int TRACKS = 3;
	private static final int TRACKS_ID = 4;
	private static final int TRACKS_LOCATIONS = 5;
	private static final int TRACKS_LOCATIONS_ID = 6;

	private static final UriMatcher sUriMatcher;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(LifeLog.AUTHORITY, "locations", LOCATIONS);
		sUriMatcher.addURI(LifeLog.AUTHORITY, "locations/#", LOCATIONS_ID);
		sUriMatcher.addURI(LifeLog.AUTHORITY, "tracks", TRACKS);
		sUriMatcher.addURI(LifeLog.AUTHORITY, "tracks/#", TRACKS_ID);
		sUriMatcher.addURI(LifeLog.AUTHORITY, "tracks/#/locations", TRACKS_LOCATIONS);
		sUriMatcher.addURI(LifeLog.AUTHORITY, "tracks/#/locations/#", TRACKS_LOCATIONS_ID);

		sLocationsProjectionMap = new HashMap<String, String>();
		sLocationsProjectionMap.put(LifeLog.Locations._ID, LifeLog.Locations._ID);
	}

	private static final String TABLE_LOCATION_CREATE = "CREATE TABLE " + TABLE_LOCATIONS + " (" +
			LifeLog.Locations._ID     + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			LifeLog.Locations.TRACK     + " INTEGER NOT NULL DEFAULT 0, " +
			LifeLog.Locations.TIMESTAMP + " INTEGER NOT NULL, " +
			LifeLog.Locations.LATITUDE  + " DOUBLE NOT NULL, " +
			LifeLog.Locations.LONGITUDE + " DOUBLE NOT NULL, " +
			LifeLog.Locations.ALTITUDE  + " DOUBLE, " +
			LifeLog.Locations.ACCURACY  + " REAL, " +
			LifeLog.Locations.BEARING   + " REAL, " +
			LifeLog.Locations.SPEED     + " REAL, " +
			LifeLog.Locations.SATELLITES+ " INTEGER, " +
			LifeLog.Locations.UPLOADED  + " BOOLEAN NOT NULL DEFAULT FALSE" +
			")";
	private static final String TABLE_TRACKS_CREATE = "CREATE TABLE " + TABLE_TRACKS + " (" +
			LifeLog.Tracks._ID     + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			LifeLog.Tracks.NAME      + " TEXT, " +
			LifeLog.Tracks.CMT       + " TEXT, " +
			LifeLog.Tracks.DESC      + " TEXT, " +
			LifeLog.Tracks.TYPE      + " TEXT, " +
			LifeLog.Tracks.UPLOADED  + " BOOLEAN NOT NULL DEFAULT FALSE" +
			")";

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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACKS);
			onCreate(db);
		}
	}

	private DatabaseHelper mDbHelper;

	@Override
	public boolean onCreate() {
		mDbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch(sUriMatcher.match(uri)) {
		case LOCATIONS:
			qb.setTables(TABLE_LOCATIONS);
			qb.setProjectionMap(sLocationsProjectionMap);
			break;

		case LOCATIONS_ID:
			qb.setTables(TABLE_LOCATIONS);
			qb.setProjectionMap(sLocationsProjectionMap);
			qb.appendWhere(LifeLog.Locations._ID + "=" + uri.getPathSegments().get(1));
			break;

		case TRACKS:
			qb.setTables(TABLE_TRACKS);
			qb.setProjectionMap(sLocationsProjectionMap);
			break;

		case TRACKS_ID:
			qb.setTables(TABLE_TRACKS);
			qb.setProjectionMap(sLocationsProjectionMap);
			qb.appendWhere(LifeLog.Tracks._ID + "=" + uri.getPathSegments().get(1));
			break;

		case TRACKS_LOCATIONS:
			qb.setTables(TABLE_LOCATIONS);
			qb.setProjectionMap(sLocationsProjectionMap);
			qb.appendWhere(LifeLog.Locations.TRACK + "=" + uri.getPathSegments().get(1));
			break;

		case TRACKS_LOCATIONS_ID:
			qb.setTables(TABLE_LOCATIONS);
			qb.setProjectionMap(sLocationsProjectionMap);
			qb.appendWhere(LifeLog.Locations._ID + "=" + uri.getPathSegments().get(3));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		/* Tell cursor what URI to watch for for data changes */
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch(sUriMatcher.match(uri)) {
		case LOCATIONS:
		case TRACKS_LOCATIONS:
			return LifeLog.Locations.CONTENT_TYPE;
		case LOCATIONS_ID:
		case TRACKS_LOCATIONS_ID:
			return LifeLog.Locations.CONTENT_ITEM_TYPE;
		case TRACKS:
			return LifeLog.Tracks.CONTENT_TYPE;
		case TRACKS_ID:
			return LifeLog.Tracks.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		String tableName;
		switch(sUriMatcher.match(uri)) {
		case LOCATIONS:
			tableName = TABLE_LOCATIONS;
			break;

		case TRACKS:
			tableName = TABLE_LOCATIONS;
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if(initialValues != null)
			values = new ContentValues(initialValues);
		else
			values = new ContentValues();

		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		long rowId = db.insert(tableName, null, values);
		if(rowId > 0) {
			Uri rowUri = ContentUris.withAppendedId(LifeLog.Locations.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(rowUri, null);
			return rowUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count;
		switch(sUriMatcher.match(uri)) {
		case LOCATIONS:
			count = db.update(TABLE_LOCATIONS, values, selection, selectionArgs);
			break;

		case LOCATIONS_ID:
			String locationId = uri.getPathSegments().get(1);
			count = db.update(TABLE_LOCATIONS, values, LifeLog.Locations._ID + "=" + locationId + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS:
			count = db.update(TABLE_TRACKS, values, selection, selectionArgs);
			break;

		case TRACKS_ID:
			String trackId = uri.getPathSegments().get(1);
			count = db.update(TABLE_TRACKS, values, LifeLog.Tracks._ID + "=" + trackId + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_LOCATIONS:
			trackId = uri.getPathSegments().get(1);
			count = db.update(TABLE_LOCATIONS, values, LifeLog.Locations.TRACK + "=" + trackId + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_LOCATIONS_ID:
			locationId = uri.getPathSegments().get(3);
			count = db.update(TABLE_LOCATIONS, values, LifeLog.Locations._ID + "=" + locationId + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count;
		switch(sUriMatcher.match(uri)) {
		case LOCATIONS:
			count = db.delete(TABLE_LOCATIONS, selection, selectionArgs);
			break;

		case LOCATIONS_ID:
			String locationId = uri.getPathSegments().get(1);
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations._ID + "=" + locationId + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS:
			count = db.delete(TABLE_TRACKS, selection, selectionArgs);
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations.TRACK + "!=0" + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_ID:
			String trackId = uri.getPathSegments().get(1);
			count = db.delete(TABLE_TRACKS, LifeLog.Tracks._ID + "=" + trackId + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations.TRACK + "=" + trackId + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_LOCATIONS:
			trackId = uri.getPathSegments().get(1);
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations.TRACK + "=" + trackId + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_LOCATIONS_ID:
			locationId = uri.getPathSegments().get(3);
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations._ID + "=" + locationId + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "" ), selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/*
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
		Cursor c = mDb.query(true, TABLE_LOCATIONS, new String[] {
			KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE,
			KEY_ACCURACY
		}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchAllLocationsF() {
		//return mDb.query(TABLE_LOCATIONS, new String[] {
		//	KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE
		//}, null, null, null, null, null);
		return mDb.rawQuery("SELECT _id, datetime(round(timestamp), 'unixepoch') AS timestamp, latitude, longitude, uploaded, altitude, accuracy FROM locations", null);
	}

	public Cursor fetchAllLocations2() {
		return mDb.query(TABLE_LOCATIONS, new String[] {
			KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE
		}, null, null, null, null, null);
	}

	public Cursor fetchLocationsByTrack(long track) {
		if(track == 0)
			return fetchAllLocations2();
		return mDb.query(TABLE_LOCATIONS, new String[] {
			KEY_ROWID, KEY_TIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE, KEY_ACCURACY
		}, KEY_TRACK + " = " + track, null, null, null, null);
	}

	public Cursor fetchLocationsByTrackF(long track) {
		if(track == 0)
			return fetchAllLocationsF();
		return mDb.rawQuery("SELECT _id, datetime(round(timestamp), 'unixepoch') AS timestamp, latitude, longitude, uploaded, altitude, accuracy FROM locations WHERE " + KEY_TRACK + " = " + track, null);
	}

	public Cursor fetchUploadLocations(String[] cols, String condition) {
		return mDb.query(TABLE_LOCATIONS, cols, condition, null, null, null, null, "100");
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

		return mDb.insert(TABLE_LOCATIONS, null, values);
	}

	public boolean updateTrack(long rowId, ContentValues values) {
		return mDb.update(TABLE_TRACKS, values, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateLocation(long rowId, Location location) {
		ContentValues args = new ContentValues();
		args.put(KEY_TIMESTAMP, location.getTime()/1000);
		args.put(KEY_LATITUDE, location.getLatitude());
		args.put(KEY_LONGITUDE, location.getLongitude());

		return mDb.update(TABLE_LOCATIONS, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateLocation(long rowId, ContentValues args) {
		return mDb.update(TABLE_LOCATIONS, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteTrack(long rowId) {
		return mDb.delete(TABLE_TRACKS, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteLocation(long rowId) {
		return mDb.delete(TABLE_LOCATIONS, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteUploadedLocations() {
		return mDb.delete(TABLE_LOCATIONS, "uploaded = 1", null) > 0;
	}

	public boolean deleteAllLocations() {
		return mDb.delete(TABLE_LOCATIONS, null, null) > 0;
	}

	public boolean deleteUploadedLocationsByTrack(long rowId) {
		if(rowId == 0)
			return deleteUploadedLocations();
		return mDb.delete(TABLE_LOCATIONS, "uploaded = 1 AND " + KEY_TRACK + " = " + rowId, null) > 0;
	}

	public boolean deleteAllLocationsByTrack(long rowId) {
		if(rowId == 0)
			return deleteAllLocations();
		return mDb.delete(TABLE_LOCATIONS, KEY_TRACK + " = " + rowId, null) > 0;
	}

	public long newTrack(ContentValues values) {
		return mDb.insert(TABLE_TRACKS, KEY_NAME, values);
	}
	*/
}
