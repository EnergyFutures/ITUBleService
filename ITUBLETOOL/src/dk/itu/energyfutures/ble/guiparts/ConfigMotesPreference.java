package dk.itu.energyfutures.ble.guiparts;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import dk.itu.energyfutures.ble.Application;
import dk.itu.energyfutures.ble.R;

public class ConfigMotesPreference extends DialogPreference {
	EditText et;
	public ConfigMotesPreference(Context context, AttributeSet set) {
		super(context,set);
		setDialogLayoutResource(R.layout.config_motes);
		setPositiveButtonText("Enable");
		setNegativeButtonText("");
	}
	
	@Override
	protected void onBindDialogView(View view) {
		et = (EditText) view.findViewById(R.id.config_motes_password);
		super.onBindDialogView(view);
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if(positiveResult){
			if(et.getText().toString().equals("future")){
				Application.setIsConfigNormalMotesEnabledFlag(true);
				Application.showShortToast("Config enabled");
			}else{
				Application.showLongToast("Wronge password");
			}
		}
		super.onDialogClosed(positiveResult);
	}
	
	@Override
	protected void onClick() {
		if(Application.isConfigNormalMotesEnabled()){
			Application.setIsConfigNormalMotesEnabledFlag(false);
			Application.showShortToast("Config disabled");
		}else{
			super.onClick();
		}
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return false;
	}
}
