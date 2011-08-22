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
import java.util.HashSet;
import java.util.Set;

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
	private static HashMap<String, String> sLocationsPrettyProjectionMap;
	private static HashMap<String, String> sTracksProjectionMap;

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

		sLocationsProjectionMap = new HashMap<String, String>(12);
		sLocationsProjectionMap.put(LifeLog.Locations._ID, LifeLog.Locations._ID);
		sLocationsProjectionMap.put(LifeLog.Locations._COUNT, "count(*) AS " + LifeLog.Locations._COUNT);
		sLocationsProjectionMap.put(LifeLog.Locations.TRACK, LifeLog.Locations.TRACK);
		sLocationsProjectionMap.put(LifeLog.Locations.TIMESTAMP, LifeLog.Locations.TIMESTAMP);
		sLocationsProjectionMap.put(LifeLog.Locations.LATITUDE, LifeLog.Locations.LATITUDE);
		sLocationsProjectionMap.put(LifeLog.Locations.LONGITUDE, LifeLog.Locations.LONGITUDE);
		sLocationsProjectionMap.put(LifeLog.Locations.ALTITUDE, LifeLog.Locations.ALTITUDE);
		sLocationsProjectionMap.put(LifeLog.Locations.ACCURACY, LifeLog.Locations.ACCURACY);
		sLocationsProjectionMap.put(LifeLog.Locations.BEARING, LifeLog.Locations.BEARING);
		sLocationsProjectionMap.put(LifeLog.Locations.SPEED, LifeLog.Locations.SPEED);
		sLocationsProjectionMap.put(LifeLog.Locations.SATELLITES, LifeLog.Locations.SATELLITES);
		sLocationsProjectionMap.put(LifeLog.Locations.UPLOADED, LifeLog.Locations.UPLOADED);

		sLocationsPrettyProjectionMap = new HashMap<String, String>(sLocationsProjectionMap);
		sLocationsPrettyProjectionMap.put(LifeLog.Locations.TIMESTAMP, "datetime(round(timestamp), 'unixepoch') AS " + LifeLog.Locations.TIMESTAMP);

		sTracksProjectionMap = new HashMap<String, String>(7);
		sTracksProjectionMap.put(LifeLog.Tracks._ID, LifeLog.Tracks._ID);
		sTracksProjectionMap.put(LifeLog.Tracks._COUNT, "count(*) AS " + LifeLog.Tracks._COUNT);
		sTracksProjectionMap.put(LifeLog.Tracks.NAME, LifeLog.Tracks.NAME);
		sTracksProjectionMap.put(LifeLog.Tracks.CMT, LifeLog.Tracks.CMT);
		sTracksProjectionMap.put(LifeLog.Tracks.DESC, LifeLog.Tracks.DESC);
		sTracksProjectionMap.put(LifeLog.Tracks.TYPE, LifeLog.Tracks.TYPE);
		sTracksProjectionMap.put(LifeLog.Tracks.UPLOADED, LifeLog.Tracks.UPLOADED);
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

	private Set<Uri> notifyUris = new HashSet<Uri>();

	private Thread notifyThread = new Thread(new Runnable() {
		public void run() {
			while(notifyThread != null) {
				synchronized(notifyUris) {
					for(Uri uri: notifyUris) {
						getContext().getContentResolver().notifyChange(uri, null);
					}
					notifyUris.clear();
				}
				try {
					Thread.sleep(5000);
				} catch(InterruptedException ex) {}
				synchronized(notifyUris) {
					try {
						notifyUris.wait();
					} catch(InterruptedException ex) {}
				}
			}
		}
	});

	private void addNotifyUri(Uri uri) {
		synchronized(notifyUris) {
			notifyUris.add(uri);
			notifyUris.notify();
		}
	}

	private DatabaseHelper mDbHelper;

	@Override
	public boolean onCreate() {
		mDbHelper = new DatabaseHelper(getContext());
		notifyThread.start();
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String limit = uri.getQueryParameter(LifeLog.PARAM_LIMIT);
		String offset = uri.getQueryParameter(LifeLog.PARAM_OFFSET);
		if(offset != null)
			limit = offset + "," + limit;

		switch(sUriMatcher.match(uri)) {
		case LOCATIONS:
			qb.setTables(TABLE_LOCATIONS);
			if(LifeLog.FORMAT_PRETTY.equals(uri.getQueryParameter(LifeLog.PARAM_FORMAT)))
				qb.setProjectionMap(sLocationsPrettyProjectionMap);
			else
				qb.setProjectionMap(sLocationsProjectionMap);
			break;

		case LOCATIONS_ID:
			qb.setTables(TABLE_LOCATIONS);
			if(LifeLog.FORMAT_PRETTY.equals(uri.getQueryParameter(LifeLog.PARAM_FORMAT)))
				qb.setProjectionMap(sLocationsPrettyProjectionMap);
			else
				qb.setProjectionMap(sLocationsProjectionMap);
			qb.appendWhere(LifeLog.Locations._ID + "=" + uri.getPathSegments().get(1));
			break;

		case TRACKS:
			qb.setTables(TABLE_TRACKS);
			qb.setProjectionMap(sTracksProjectionMap);
			break;

		case TRACKS_ID:
			qb.setTables(TABLE_TRACKS);
			qb.setProjectionMap(sTracksProjectionMap);
			qb.appendWhere(LifeLog.Tracks._ID + "=" + uri.getPathSegments().get(1));
			break;

		case TRACKS_LOCATIONS:
			qb.setTables(TABLE_LOCATIONS);
			if(LifeLog.FORMAT_PRETTY.equals(uri.getQueryParameter(LifeLog.PARAM_FORMAT)))
				qb.setProjectionMap(sLocationsPrettyProjectionMap);
			else
				qb.setProjectionMap(sLocationsProjectionMap);
			qb.appendWhere(LifeLog.Locations.TRACK + "=" + uri.getPathSegments().get(1));
			break;

		case TRACKS_LOCATIONS_ID:
			qb.setTables(TABLE_LOCATIONS);
			if(LifeLog.FORMAT_PRETTY.equals(uri.getQueryParameter(LifeLog.PARAM_FORMAT)))
				qb.setProjectionMap(sLocationsPrettyProjectionMap);
			else
				qb.setProjectionMap(sLocationsProjectionMap);
			qb.appendWhere(LifeLog.Locations._ID + "=" + uri.getPathSegments().get(3));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);

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
		String nullColumn;
		boolean hasTrack = false;
		switch(sUriMatcher.match(uri)) {
		case LOCATIONS:
			tableName = TABLE_LOCATIONS;
			nullColumn = null;
			break;

		case TRACKS_LOCATIONS:
			tableName = TABLE_LOCATIONS;
			nullColumn = null;
			hasTrack = true;
			break;

		case TRACKS:
			tableName = TABLE_TRACKS;
			nullColumn = LifeLog.Tracks.NAME;
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if(initialValues != null)
			values = new ContentValues(initialValues);
		else
			values = new ContentValues(0);

		if(hasTrack && !values.containsKey(LifeLog.Locations.TRACK))
			values.put(LifeLog.Locations.TRACK,
				   Long.parseLong(uri.getPathSegments().get(1)));
		if(TABLE_LOCATIONS.equals(tableName) &&
		   !values.containsKey(LifeLog.Locations.TRACK))
			values.put(LifeLog.Locations.TRACK, 0);

		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		long rowId = db.insert(tableName, nullColumn, values);
		if(rowId > 0) {
			Uri rowUri;
			if(TABLE_LOCATIONS.equals(tableName)) {
				rowUri = ContentUris.withAppendedId(LifeLog.Locations.CONTENT_URI, rowId);
				addNotifyUri(rowUri);
				long track = values.getAsLong(LifeLog.Locations.TRACK);
				if(track != 0) {
					Uri trackUri = ContentUris.appendId(ContentUris.appendId(LifeLog.Tracks.CONTENT_URI.buildUpon(), track).appendPath("locations"), rowId).build();
					addNotifyUri(trackUri);
				}
			} else {
				rowUri = ContentUris.withAppendedId(LifeLog.Tracks.CONTENT_URI, rowId);
				addNotifyUri(rowUri);
			}
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
			count = db.update(TABLE_LOCATIONS, values, LifeLog.Locations._ID + "=" + locationId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS:
			count = db.update(TABLE_TRACKS, values, selection, selectionArgs);
			break;

		case TRACKS_ID:
			String trackId = uri.getPathSegments().get(1);
			count = db.update(TABLE_TRACKS, values, LifeLog.Tracks._ID + "=" + trackId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_LOCATIONS:
			trackId = uri.getPathSegments().get(1);
			count = db.update(TABLE_LOCATIONS, values, LifeLog.Locations.TRACK + "=" + trackId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_LOCATIONS_ID:
			locationId = uri.getPathSegments().get(3);
			count = db.update(TABLE_LOCATIONS, values, LifeLog.Locations._ID + "=" + locationId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		addNotifyUri(uri);
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
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations._ID + "=" + locationId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS:
			count = db.delete(TABLE_TRACKS, selection, selectionArgs);
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations.TRACK + "!=0" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_ID:
			String trackId = uri.getPathSegments().get(1);
			count = db.delete(TABLE_TRACKS, LifeLog.Tracks._ID + "=" + trackId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations.TRACK + "=" + trackId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_LOCATIONS:
			trackId = uri.getPathSegments().get(1);
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations.TRACK + "=" + trackId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			break;

		case TRACKS_LOCATIONS_ID:
			locationId = uri.getPathSegments().get(3);
			count = db.delete(TABLE_LOCATIONS, LifeLog.Locations._ID + "=" + locationId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "" ), selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		addNotifyUri(uri);
		return count;
	}

	/*
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
	*/
}
