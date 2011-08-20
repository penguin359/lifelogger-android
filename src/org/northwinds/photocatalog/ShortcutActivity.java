package org.northwinds.photocatalog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class ShortcutActivity extends Activity {
	private static final String ACTION_START_LOG = "org.northwinds.android.intent.START_LOG";
	private static final String ACTION_START_LOG_AND_PHOTOCATALOG = "org.northwinds.android.intent.START_LOG_AND_PHOTOCATALOG";
	private static final String ACTION_START_PHOTOCATALOG = "org.northwinds.android.intent.START_PHOTOCATALOG";
	private static final String ACTION_STOP_LOG = "org.northwinds.android.intent.STOP_LOG";
	private static final String ACTION_TOGGLE_LOG = "org.northwinds.android.intent.TOGGLE_LOG";

	private static final String CATEGORY_SHORTCUT = "org.northwinds.android.intent.CATEGORY_SHORTCUT";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if(intent.hasCategory(CATEGORY_SHORTCUT)) {
			if(ACTION_START_LOG.equals(intent.getAction())) {
				startService(new Intent(Logger.ACTION_START_LOG, null, this, Logger.class));
				Toast.makeText(this, "GPS Logger started", Toast.LENGTH_SHORT).show();
			}
			if(ACTION_START_LOG_AND_PHOTOCATALOG.equals(intent.getAction())) {
				startService(new Intent(Logger.ACTION_START_LOG, null, this, Logger.class));
				startActivity(new Intent(this, Main.class));
			}
			if(ACTION_START_PHOTOCATALOG.equals(intent.getAction())) {
				startActivity(new Intent(this, Main.class));
			}
			if(ACTION_TOGGLE_LOG.equals(intent.getAction())) {
				startService(new Intent(Logger.ACTION_TOGGLE_LOG, null, this, Logger.class));
				Toast.makeText(this, "GPS Logger toggled", Toast.LENGTH_SHORT).show();
			}
			if(ACTION_STOP_LOG.equals(intent.getAction())) {
				startService(new Intent(Logger.ACTION_STOP_LOG, null, this, Logger.class));
				Toast.makeText(this, "GPS Logger stopped", Toast.LENGTH_SHORT).show();
			}
			finish();
		}

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		RadioGroup radioGroup = new RadioGroup(this);
		radioGroup.setOrientation(LinearLayout.VERTICAL);
		final RadioButton startRadio = new RadioButton(this);
		startRadio.setText("Start GPS");
		radioGroup.addView(startRadio);
		final RadioButton startAndOpenRadio = new RadioButton(this);
		startAndOpenRadio.setText("Start GPS and open PhotoCatalog");
		//startAndOpenRadio.setChecked(true);
		radioGroup.addView(startAndOpenRadio);
		final RadioButton openRadio = new RadioButton(this);
		openRadio.setText("Open PhotoCatalog");
		radioGroup.addView(openRadio);
		final RadioButton toggleRadio = new RadioButton(this);
		toggleRadio.setText("Toggle GPS");
		radioGroup.addView(toggleRadio);
		final RadioButton stopRadio = new RadioButton(this);
		stopRadio.setText("Stop GPS");
		radioGroup.addView(stopRadio);
		layout.addView(radioGroup);
		Button button = new Button(this);
		button.setText("Add");
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent shortcutIntent;
				Intent intent = new Intent();
				if(startAndOpenRadio.isChecked()) {
					shortcutIntent = new Intent(ACTION_START_LOG_AND_PHOTOCATALOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Start GPS and PC");
				} else if(openRadio.isChecked()) {
					shortcutIntent = new Intent(ACTION_START_PHOTOCATALOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Start PC");
				} else if(toggleRadio.isChecked()) {
					shortcutIntent = new Intent(ACTION_TOGGLE_LOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Toggle GPS");
				} else if(stopRadio.isChecked()) {
					shortcutIntent = new Intent(ACTION_STOP_LOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Stop GPS");
				} else {
					shortcutIntent = new Intent(ACTION_START_LOG, null, ShortcutActivity.this, ShortcutActivity.class);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Start GPS");
				}
				shortcutIntent.addCategory(CATEGORY_SHORTCUT);
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
				Parcelable iconResource = Intent.ShortcutIconResource.fromContext(ShortcutActivity.this, R.drawable.icon);
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		layout.addView(button);
		setContentView(layout);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
}
