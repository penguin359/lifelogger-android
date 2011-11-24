/*
 * Copyright (c) 2010-2011, Loren M. Lang
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;

public class Receiver extends BroadcastReceiver {
	private static final String SMS_RECEIVED =
	    "android.provider.Telephony.SMS_RECEIVED";

	private static final String SMS_START_LOG = "PHOTOCATALOG_START_LOG ";
	private static final String SMS_STOP_LOG = "PHOTOCATALOG_STOP_LOG ";
	private static final String SMS_UPLOAD_ONCE = "PHOTOCATALOG_UPLOAD_ONCE ";
	private static final String SMS_RETRIEVE_LOCATION = "PHOTOCATALOG_RETRIEVE_LOCATION ";

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String action = intent.getAction();
		if(Intent.ACTION_BOOT_COMPLETED.equals(action) &&
			prefs.getBoolean("onBoot", false))
			context.startService(
			    new Intent(LoggingService.ACTION_START_LOG,
				       null,
				       context,
				       LoggingService.class));
		else if(Intent.ACTION_BATTERY_LOW.equals(action))
			context.startService(
			    new Intent(LoggingService.ACTION_STOP_LOG,
				       null,
				       context,
				       LoggingService.class));
		else if(SMS_RECEIVED.equals(action) &&
			prefs.getBoolean("sms", false)) {
			String smsKey = prefs.getString("smsKey", "1234567890");
			String smsStartLog = SMS_START_LOG + smsKey;
			String smsStopLog = SMS_STOP_LOG + smsKey;
			String smsUploadOnce = SMS_UPLOAD_ONCE + smsKey;
			String smsRetrieveLocation = SMS_RETRIEVE_LOCATION + smsKey;
			Bundle extras = intent.getExtras();
			Object[] pdus = (Object[])extras.get("pdus");
			for(Object pdu: pdus) {
				SmsMessage sms =
				    SmsMessage.createFromPdu((byte[])pdu);
				String msg = sms.getMessageBody();
				if(smsStartLog.equals(msg))
					context.startService(
					    new Intent(LoggingService.ACTION_START_LOG,
						       null,
						       context,
						       LoggingService.class));
				else if(smsStopLog.equals(msg))
					context.startService(
					    new Intent(LoggingService.ACTION_STOP_LOG,
						       null,
						       context,
						       LoggingService.class));
				else if(smsUploadOnce.equals(msg))
					context.startService(
					    new Intent(LoggingService.ACTION_UPLOAD_ONCE,
						       null,
						       context,
						       LoggingService.class));
				else if(smsRetrieveLocation.equals(msg)) {
					Intent i = new Intent(LoggingService.ACTION_RETRIEVE_LOCATION,
						       null,
						       context,
						       LoggingService.class);
					i.putExtra(LoggingService.EXTRA_SMS_ADDRESS, sms.getOriginatingAddress());
					context.startService(i);
				}
			}
		}
	}
}
