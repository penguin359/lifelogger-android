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

import android.net.Uri;
import android.provider.BaseColumns;;

public final class LifeLog {
	/* This class cannot be instantiated */
	private LifeLog() {}

	public static final String AUTHORITY = "org.northwinds.photocatalog.lifelog";

	public static final String PARAM_FORMAT = "format";
	public static final String PARAM_LIMIT  = "limit";
	public static final String PARAM_OFFSET = "offset";

	public static final String FORMAT_DEFAULT = "default";
	public static final String FORMAT_PRETTY  = "pretty";

	public static final class Locations implements BaseColumns {
		/* This class cannot be instantiated */
		private Locations() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/locations");

		public static final String CONTENT_TYPE = "vnd.google.cursor.dir/vnd.northwinds.location";
		public static final String CONTENT_ITEM_TYPE = "vnd.google.cursor.item/vnd.northwinds.location";

		public static final String TRACK     = "track";
		public static final String TIMESTAMP = "timestamp";
		public static final String LATITUDE  = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String ALTITUDE  = "altitude";
		public static final String ACCURACY  = "accuracy";
		public static final String BEARING   = "bearing";
		public static final String SPEED     = "speed";
		public static final String SATELLITES= "satellites";
		public static final String UPLOADED  = "uploaded";
	}

	public static final class Tracks implements BaseColumns {
		/* This class cannot be instantiated */
		private Tracks() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/tracks");

		public static final String CONTENT_TYPE = "vnd.google.cursor.dir/vnd.northwinds.track";
		public static final String CONTENT_ITEM_TYPE = "vnd.google.cursor.item/vnd.northwinds.track";

		public static final String NAME      = "name";
		public static final String CMT       = "cmt";
		public static final String DESC      = "desc";
		public static final String TYPE      = "type";
		public static final String UPLOADED  = "uploaded";
	}
}
