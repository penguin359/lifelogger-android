/**
 * 
 */
package org.northwinds.photocatalog;

import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Loren M. Lang
 *
 */
public class DebugActivity extends Activity implements LocationListener {

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(this, provider + " disabled", Toast.LENGTH_SHORT).show();
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(this, provider + " disabled", Toast.LENGTH_SHORT).show();

	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//TextView tv = (TextView)findViewById(R.id.status);
		TextView tv = new TextView(this);
		setContentView(tv);
		Intent intent = getIntent();
		StringBuilder sb = new StringBuilder();
		sb.append("Hello: ");
		sb.append(intent.getAction());
		sb.append(", '");
		sb.append(intent.getDataString());
		sb.append("', [");
		sb.append(intent.getType());
		sb.append("] - ");
		if(intent.getCategories() != null) {
			for(String s : intent.getCategories()) {
				sb.append(" '");
				sb.append(s);
				sb.append(",");
			}
		} else
			sb.append("null");
		sb.append(": ");
		if(intent.getExtras() != null) {
			sb.append(intent.getExtras());
			for(String s : intent.getExtras().keySet()) {
				sb.append(" '");
				sb.append(s);
				sb.append("': ");
				Object o = intent.getExtras().get(s);
				Class<? extends Object> c = o.getClass();
				sb.append(c.getName());
				sb.append(", ");
			}
			try {
				if(intent.getExtras().get(Intent.EXTRA_STREAM) != null) {
					sb.append("\nReading: ");
					Uri uri = (Uri)intent.getExtras().get(Intent.EXTRA_STREAM);
					sb.append(uri.toString());
					sb.append(" ");
					ContentResolver cr = getContentResolver();
					InputStream is = cr.openInputStream(uri);
					InputStreamReader isr = new InputStreamReader(is);
					isr.read();
					isr.read();
					isr.read();
					isr.read();
					isr.read();
					isr.read();
					char buf[] = new char[4];
					isr.read(buf, 0, 4);
					sb.append(buf);
					sb.append(" done!\n");
					is.close();
					Cursor c = cr.query(uri, new String[] { MediaColumns.DISPLAY_NAME, MediaColumns.TITLE, MediaColumns.MIME_TYPE, MediaColumns.SIZE }, null, null, null);
					if(c != null && c.moveToFirst()) {
						sb.append("\n'");
						for(int i = 0; i < c.getColumnCount(); i++) {
							if(i > 0)
								sb.append("', '");
							sb.append(c.getString(i));
						}
						sb.append("'");
					}
					c.close();
				}
			} catch(Exception ex) {
				sb.append(ex);
			}
		} else
			sb.append("null");

		tv.setText(sb.toString());
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Toast.makeText(this, "Destroy", Toast.LENGTH_SHORT).show();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		Toast.makeText(this, "Start", Toast.LENGTH_SHORT).show();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
	}
}
