package dk.itu.energyfutures.ble.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import dk.itu.energyfutures.ble.R;
import dk.itu.energyfutures.ble.guiparts.SettingsFragment;
import dk.itu.energyfutures.ble.helpers.ITUConstants;

public class SettingsActivity extends Activity implements OnSharedPreferenceChangeListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new SettingsFragment())
        .commit();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//		if (key.equals(ITUConstants.SHOW_ADVANCE_SETTINGS_KEY)) {
//			
//		}
	}
}
