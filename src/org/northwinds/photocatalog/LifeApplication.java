package org.northwinds.photocatalog;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.GoogleFormSender;
import org.acra.sender.HttpPostSender;

@ReportsCrashes(formKey = "",
		mode = ReportingInteractionMode.NOTIFICATION,
		resToastText = R.string.crash_toast_text,
		resNotifTickerText = R.string.crash_notif_ticker_text,
		resNotifTitle = R.string.crash_notif_title,
		resNotifText = R.string.crash_notif_text,
		resNotifIcon = android.R.drawable.stat_notify_error,
		resDialogText = R.string.crash_dialog_text,
		resDialogIcon = android.R.drawable.ic_dialog_info,
		resDialogTitle = R.string.crash_dialog_title,
		resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
		resDialogOkToast = R.string.crash_dialog_ok_toast)

public class LifeApplication extends Application {
	@Override
	public void onCreate() {
		ACRA.init(this);
		ErrorReporter reporter = ErrorReporter.getInstance();
		if(!"".equals(getString(R.string.acra_post_url)))
		  reporter.addReportSender(
		   new HttpPostSender(getString(R.string.acra_post_url), null));
		if(!"".equals(getString(R.string.acra_form_key)))
		  reporter.addReportSender(
		   new GoogleFormSender(getString(R.string.acra_form_key)));
		reporter.checkReportsOnApplicationStart();
		super.onCreate();
	}
}
