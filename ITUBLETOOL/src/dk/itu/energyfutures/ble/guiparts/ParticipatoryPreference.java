package dk.itu.energyfutures.ble.guiparts;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import dk.itu.energyfutures.ble.Application;

public class ParticipatoryPreference extends DialogPreference {

	public ParticipatoryPreference(Context context, AttributeSet set) {
		super(context,set);
		setDialogMessage("This allows the app to collect data and use your internet.\nRandom BT-chip reset will occur.\nWe appreciate any help :0)");
		setPositiveButtonText("Enable");
		setNegativeButtonText("Disable");
		
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		Application.setParticipatoryDataSink(positiveResult);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return false;
	}
}
