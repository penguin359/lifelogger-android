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

	private static final String SMS_START_LOG = "PHOTOCATALOG_START_LOG ";
	private static final String SMS_STOP_LOG = "PHOTOCATALOG_STOP_LOG ";
	private static final String SMS_UPLOAD_ONCE = "PHOTOCATALOG_UPLOAD_ONCE ";

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
		else if(action.equals(SMS_RECEIVED) &&
			prefs.getBoolean("sms", false)) {
			String smsKey = prefs.getString("smsKey", "1234567890");
			String smsStartLog = SMS_START_LOG + smsKey;
			String smsStopLog = SMS_STOP_LOG + smsKey;
			String smsUploadOnce = SMS_UPLOAD_ONCE + smsKey;
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
				if(smsStartLog.equals(msg))
					context.startService(
					    new Intent(Logger.ACTION_START_LOG,
						       null,
						       context,
						       Logger.class));
				else if(smsStopLog.equals(msg))
					context.startService(
					    new Intent(Logger.ACTION_STOP_LOG,
						       null,
						       context,
						       Logger.class));
				else if(smsUploadOnce.equals(msg))
					context.startService(
					    new Intent(Logger.ACTION_UPLOAD_ONCE,
						       null,
						       context,
						       Logger.class));
			}
		}
	}
}
