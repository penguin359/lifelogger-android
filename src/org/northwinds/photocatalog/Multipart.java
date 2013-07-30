package org.northwinds.photocatalog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentProducer;
import org.apache.http.message.BasicHeader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

public class Multipart {
	private static final String TAG = "PhotoCatalog-Multipart";

	private final Context mContext;

	private LinkedHashMap<String, Object> mFields = new LinkedHashMap<String, Object>();

	public static interface ProgressUpdate {
		void onUpdate(int progress, int max);
	}

	private ProgressUpdate mUpdate = null;

	private class MultipartEntity extends AbstractHttpEntity {
		private static final String mBoundary = "fjd3Fb5Xr8Hfrb6hnDv3Lg";
		private static final String mSubBoundary = "3dHeyj2Deq6ghBy2Ds67Hp";

		private ArrayList<Object> mContent = new ArrayList<Object>();
		private long mLength = 0;

		StringBuilder sb = new StringBuilder();

		MultipartEntity() {
			contentType = new BasicHeader("Content-Type", "multipart/form-data; boundary=" + mBoundary);

			for(Entry<String, Object> entry: mFields.entrySet()) {
				String name = entry.getKey();
				Object obj = entry.getValue();
				addObject(name, obj, mBoundary);
			}
			sb.append("--").append(mBoundary).append("--\r\n");

			try {
				byte[] str = sb.toString().getBytes("UTF-8");
				mContent.add(str);
				if(mLength >= 0)
					mLength += str.length;
			} catch(UnsupportedEncodingException e) {
				Log.e(TAG, "Failed to convert string to UTF-8");
			}
		}

		private void addObject(String name, Object obj, String boundary) {
			boolean isSubPart = (name == null);
			sb.append("--").append(boundary).append("\r\n");
			if(obj instanceof String) {
				if(isSubPart)
					sb.append("Content-Disposition: file\r\n");
				else
					sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
				sb.append("Content-Type: text/plain; charset=utf-8\r\n");
				sb.append("Content-Transfer-Encoding: 8bit\r\n");
				sb.append("\r\n");
				sb.append(((String)obj).replace("\n", "\r\n"));
				sb.append("\r\n");
			} else if(obj instanceof InputStream ||
				  obj instanceof ContentProducer) {
				if(isSubPart)
					sb.append("Content-Disposition: file\r\n");
				else
					sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
				sb.append("Content-Type: application/octet-stream\r\n");
				sb.append("Content-Transfer-Encoding: binary\r\n");
				sb.append("\r\n");

				try {
					mContent.add(sb.toString().getBytes("UTF-8"));
				} catch(UnsupportedEncodingException e) {
					Log.e(TAG, "Failed to convert string to UTF-8");
				}
				sb = new StringBuilder();
				mContent.add(obj);
				mLength = -1;

				sb.append("\r\n");
			} else if(obj instanceof Uri) {
				String filename = null;
				String type		= null;
				String size		= null;
				ContentResolver cr = mContext.getContentResolver();
				Uri uri = (Uri)obj;
				if(uri.getScheme().equals("content")) {
					Cursor c = cr.query(uri, new String[] {
							MediaColumns.DISPLAY_NAME,
							MediaColumns.MIME_TYPE,
							MediaColumns.SIZE
						}, null, null, null);
					if(c.moveToFirst()) {
						filename = c.getString(c.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME));
						type	 = c.getString(c.getColumnIndexOrThrow(MediaColumns.MIME_TYPE));
						size	 = c.getString(c.getColumnIndexOrThrow(MediaColumns.SIZE));
					}
					c.close();
				} else {
					filename = uri.getLastPathSegment();
				}
				if(type == null)
					type = "application/octet-stream";
				if(size == null)
					mLength = -1;
				else if(mLength >= 0)
					mLength += Long.parseLong(size);
				if(isSubPart)
					sb.append("Content-Disposition: file");
				else
					sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"");
				if(filename != null)
					sb.append("; filename=\"").append(filename).append("\"");
				sb.append("\r\n");
				sb.append("Content-Type: ");
				sb.append(type);
				sb.append("\r\n");
				sb.append("Content-Transfer-Encoding: binary\r\n");
				sb.append("\r\n");

				byte[] str;
				try {
					str = sb.toString().getBytes("UTF-8");
					mContent.add(str);
					if(mLength >= 0)
						mLength += str.length;
					sb = new StringBuilder();
					InputStream is = cr.openInputStream(uri);
					mContent.add(is);
				} catch(UnsupportedEncodingException ex) {
					Log.e(TAG, "Failed to convert string to UTF-8", ex);
				} catch(FileNotFoundException ex) {
					Log.e(TAG, "Failed to open file", ex);
					mLength = -1;
				}

				sb.append("\r\n");
			} else if(obj instanceof ArrayList<?>) {
				ArrayList<?> list = (ArrayList<?>)obj;
				sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
				sb.append("Content-Type: multipart/mixed; boundary=").append(mSubBoundary).append("\r\n");
				sb.append("Content-Transfer-Encoding: binary\r\n");
				sb.append("\r\n");
				for(Object element: list) {
					addObject(null, element, mSubBoundary);
				}
				sb.append("--").append(mSubBoundary).append("--\r\n");
			}
		}

		public InputStream getContent() throws IOException {
			throw new IOException("Entity only supports writing");
		}

		public long getContentLength() {
			return mLength;
		}

		public boolean isRepeatable() {
			return true;
		}

		public boolean isStreaming() {
			return false;
		}

		public void writeTo(OutputStream os) throws IOException {
			Long progress = 0L;
			if(mLength >= 0 && mUpdate != null)
				mUpdate.onUpdate(0, 100);
			for(Object obj: mContent) {
				if(obj instanceof byte[]) {
					os.write((byte[])obj);
					progress += ((byte[])obj).length;
					if(mLength >= 0 && mUpdate != null)
						mUpdate.onUpdate((int)(progress*100/mLength), 100);
				} else if(obj instanceof InputStream) {
					//Log.i(TAG, "Start file");
					InputStream is = (InputStream)obj;
					byte[] buffer = new byte[8192];
					int bytesRead;
					while((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
						progress += bytesRead;
						if(mLength >= 0 && mUpdate != null)
							mUpdate.onUpdate((int)(progress*100/mLength), 100);
					}
					//Log.i(TAG, "Finish file");
				} else if(obj instanceof ContentProducer) {
					//Log.i(TAG, "Start content");
					ContentProducer cp = (ContentProducer)obj;
					cp.writeTo(os);
					//Log.i(TAG, "Finish content");
				}
			}
		}
	}

	public Multipart() {
		mContext = null;
	}

	public Multipart(Context context) {
		mContext = context;
	}

	public HttpEntity getEntity() {
		return new MultipartEntity();
	}

	public void put(String name, String value) {
		mFields.put(name, value);
	}

	public void put(String name, ContentProducer value) {
		mFields.put(name, value);
	}

	public void put(String name, InputStream value) {
		mFields.put(name, value);
	}

	public void put(String name, Uri value) {
		mFields.put(name, value);
	}

	public void put(String name, ArrayList<? extends Object> value) {
		mFields.put(name, value);
	}

	public void setProgressUpdate(ProgressUpdate update) {
		mUpdate = update;
	}
}
