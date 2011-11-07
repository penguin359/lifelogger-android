package org.northwinds.photocatalog;

import android.app.Application;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "dGExM01WVDVDSG5Ja1c0aXY1Wm9qTUE6MQ")
public class LifeApplication extends Application {
	@Override
	public void onCreate() {
		ACRA.init(this);
		super.onCreate();
	}
}
