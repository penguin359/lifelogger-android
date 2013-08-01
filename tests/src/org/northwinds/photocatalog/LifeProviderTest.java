package org.northwinds.photocatalog;

import org.northwinds.photocatalog.LifeLog;
import org.northwinds.photocatalog.LifeProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;

public class LifeProviderTest extends ProviderTestCase2<LifeProvider> {
	public LifeProviderTest() {
		super(LifeProvider.class, LifeLog.AUTHORITY);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testPreconditions() {
		Cursor c = getMockContentResolver().query(LifeLog.Locations.CONTENT_URI, new String[] { LifeLog.Locations._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Locations table initially empty", 0, c.getInt(c.getColumnIndexOrThrow(LifeLog.Locations._COUNT)));
		c.close();

		c = getMockContentResolver().query(LifeLog.Tracks.CONTENT_URI, new String[] { LifeLog.Tracks._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Tracks table initially empty", 0, c.getInt(c.getColumnIndexOrThrow(LifeLog.Tracks._COUNT)));
		c.close();
	}

	public void testInsert() {
		ContentValues values = new ContentValues(4);
		values.put(LifeLog.Locations.TRACK,     1);
		values.put(LifeLog.Locations.TIMESTAMP, 1000000/1000l);
		values.put(LifeLog.Locations.LATITUDE,  45.);
		values.put(LifeLog.Locations.LONGITUDE, -45.);
		getMockContentResolver().insert(LifeLog.Locations.CONTENT_URI, values);
		getMockContentResolver().insert(LifeLog.Locations.CONTENT_URI, values);
		values.put(LifeLog.Locations.TRACK,     2);
		getMockContentResolver().insert(LifeLog.Locations.CONTENT_URI, values);

		Cursor c = getMockContentResolver().query(LifeLog.Locations.CONTENT_URI, new String[] { LifeLog.Locations._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Locatations has values", 3, c.getInt(c.getColumnIndexOrThrow(LifeLog.Locations._COUNT)));
		c.close();

		values.clear();
		values.put(LifeLog.Tracks._ID,     1);
		values.put(LifeLog.Tracks.NAME, "Hello");
		getMockContentResolver().insert(LifeLog.Tracks.CONTENT_URI, values);
		values.put(LifeLog.Tracks._ID,     2);
		values.put(LifeLog.Tracks.NAME, "World");
		getMockContentResolver().insert(LifeLog.Tracks.CONTENT_URI, values);

		c = getMockContentResolver().query(LifeLog.Tracks.CONTENT_URI, new String[] { LifeLog.Tracks._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Tracks has values", 2, c.getInt(c.getColumnIndexOrThrow(LifeLog.Tracks._COUNT)));
		c.close();

		Uri uri = ContentUris.withAppendedId(LifeLog.Tracks.CONTENT_URI, 1);
		c = getMockContentResolver().query(uri, new String[] { LifeLog.Tracks._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Have one track with ID 1", 1, c.getInt(c.getColumnIndexOrThrow(LifeLog.Tracks._COUNT)));
		c.close();

		uri = uri.buildUpon().appendPath("locations").build();
		c = getMockContentResolver().query(uri, new String[] { LifeLog.Locations._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Have two locations on track 1", 2, c.getInt(c.getColumnIndexOrThrow(LifeLog.Locations._COUNT)));
		c.close();

		uri = ContentUris.withAppendedId(LifeLog.Tracks.CONTENT_URI, 2);
		c = getMockContentResolver().query(uri, new String[] { LifeLog.Tracks._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Have one track with ID 2", 1, c.getInt(c.getColumnIndexOrThrow(LifeLog.Tracks._COUNT)));
		c.close();

		uri = uri.buildUpon().appendPath("locations").build();
		c = getMockContentResolver().query(uri, new String[] { LifeLog.Locations._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Have one location on track 2", 1, c.getInt(c.getColumnIndexOrThrow(LifeLog.Locations._COUNT)));
		c.close();

		uri = ContentUris.withAppendedId(LifeLog.Tracks.CONTENT_URI, 3);
		c = getMockContentResolver().query(uri, new String[] { LifeLog.Tracks._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Have zero tracks with ID 3", 0, c.getInt(c.getColumnIndexOrThrow(LifeLog.Tracks._COUNT)));
		c.close();

		uri = uri.buildUpon().appendPath("locations").build();
		c = getMockContentResolver().query(uri, new String[] { LifeLog.Locations._COUNT }, null, null, null);
		assertTrue("Returned a row count", c.moveToFirst());
		assertEquals("Have zero locations on track 3", 0, c.getInt(c.getColumnIndexOrThrow(LifeLog.Locations._COUNT)));
		c.close();
	}
}
