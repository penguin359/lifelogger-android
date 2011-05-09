package org.northwinds.photocatalog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
//import android.widget.Toast;

public class Receiver extends BroadcastReceiver {
	private static final String SMS_RECEIVED =
	    "android.provider.Telephony.SMS_RECEIVED";

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String action = intent.getAction();
		//Toast.makeText(context, "Receive: " + action, Toast.LENGTH_SHORT).show();
		if(action == null)
			return;
		else if(action.equals(Intent.ACTION_BOOT_COMPLETED) &&
			prefs.getBoolean("onBoot", false))
			context.startService(
			    new Intent(Logger.ACTION_START_LOG,
				       null,
				       context,
				       Logger.class));
		else if(action.equals(Intent.ACTION_BATTERY_LOW))
			context.startService(
			    new Intent(Logger.ACTION_STOP_LOG,
				       null,
				       context,
				       Logger.class));
		else if(action.equals(SMS_RECEIVED)) {
			Bundle extras = intent.getExtras();
			Object[] pdus = (Object[])extras.get("pdus");
			//SmsMessage[] messages = new SmsMessage[pdus.length];
			//for(int i; i < pdus.length; i++) {
			//	messages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
			//}
			for(Object pdu: pdus) {
				SmsMessage sms =
				    SmsMessage.createFromPdu((byte[])pdu);
				String msg = sms.getMessageBody();
				if(msg.startsWith("PHOTOCATALOG_START_LOG"))
					context.startService(
					    new Intent(Logger.ACTION_START_LOG,
						       null,
						       context,
						       Logger.class));
				else if(msg.startsWith("PHOTOCATALOG_STOP_LOG"))
					context.startService(
					    new Intent(Logger.ACTION_STOP_LOG,
						       null,
						       context,
						       Logger.class));
			}
		}
	}
}
