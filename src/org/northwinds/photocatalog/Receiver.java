package org.northwinds.photocatalog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
//import android.widget.Toast;

public class Receiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String action = intent.getAction();
		//Toast.makeText(context, "Receive: " + action, Toast.LENGTH_SHORT).show();
		if(action == null)
			return;
		else if(action.equals(Intent.ACTION_BOOT_COMPLETED) &&
				prefs.getBoolean("onBoot", false))
			context.startService(new Intent(Logger.ACTION_START_LOG, null, context, Logger.class));
		else if(action.equals(Intent.ACTION_BATTERY_LOW))
			context.startService(new Intent(Logger.ACTION_STOP_LOG, null, context, Logger.class));
	}
}
