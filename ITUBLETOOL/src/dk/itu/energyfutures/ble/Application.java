package dk.itu.energyfutures.ble;

import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.Toast;
import dk.itu.energyfutures.ble.helpers.ITUConstants;

public class Application extends android.app.Application{
	private static SharedPreferences sharedPreferences;
	private static Context applicationContext;
	private static Boolean isDataSink;
	private static Handler handler;

	@Override
	public void onCreate() {
		super.onCreate();
		applicationContext = getApplicationContext();
		sharedPreferences = getSharedPreferences(this.getClass().getSimpleName(), MODE_PRIVATE);
		handler = new Handler(applicationContext.getMainLooper());
	}
	
	public static void showLongToast(String msg){
		showToast(msg, Toast.LENGTH_LONG);
	}
	
	public static void showShortToast(String msg){
		showToast(msg, Toast.LENGTH_SHORT);
	}
	
	private static void showToast(String msg, int length){
		Toast.makeText(applicationContext, msg, length).show();
	}
	
	public static boolean putString(String key, String value) {
		return sharedPreferences.edit().putString(key, value).commit();
	}

	public static boolean putStringSet(String key, Set<String> values) {
		return sharedPreferences.edit().putStringSet(key, values).commit();
	}

	public static boolean putInt(String key, int value) {
		return sharedPreferences.edit().putInt(key, value).commit();
	}

	public static boolean putLong(String key, long value) {
		return sharedPreferences.edit().putLong(key, value).commit();
	}

	public static boolean putFloat(String key, float value) {
		return sharedPreferences.edit().putFloat(key, value).commit();
	}

	public static boolean putBoolean(String key, boolean value) {
		return sharedPreferences.edit().putBoolean(key, value).commit();
	}

	public static boolean remove(String key) {
		return sharedPreferences.edit().remove(key).commit();
	}

	public boolean clear() {
		return sharedPreferences.edit().clear().commit();
	}
	
	public static String getPref(String key, String defaulValue){
		return sharedPreferences.getString(key, defaulValue);
	}

	public static Map<String, ?> getPrefs() {
		return sharedPreferences.getAll();
	}

	public static Set<String> getPrefSet(String key, Set<String> defValues) {
		return sharedPreferences.getStringSet(key, defValues);
	}

	public static int getPref(String key, int defValue) {
		return sharedPreferences.getInt(key, defValue);
	}

	public static long getPref(String key, long defValue) {
		return sharedPreferences.getLong(key, defValue);
	}

	public static float getPref(String key, float defValue) {
		return sharedPreferences.getFloat(key, defValue);
	}

	public static boolean getPref(String key, boolean defValue) {
		return sharedPreferences.getBoolean(key, defValue);
	}

	public static boolean containsPref(String key) {
		return sharedPreferences.contains(key);
	}
	
	public static boolean isDataSink(){
		if(isDataSink == null){
			isDataSink = Application.getPref(ITUConstants.DEVICE_IS_A_GATEWAY_KEY,false);
		}
		return isDataSink;
	}
	
	public static boolean toggleDataSink(){
		if(isDataSink == null){
			isDataSink = Application.getPref(ITUConstants.DEVICE_IS_A_GATEWAY_KEY,false);
		}
		Application.putBoolean(ITUConstants.DEVICE_IS_A_GATEWAY_KEY, !isDataSink);
		isDataSink = !isDataSink;
		return isDataSink;
	}

	public static void showShortToastOnUI(final String msg) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				showShortToast(msg);
			}
		});
	}
	
	public static void showLongToastOnUI(final String msg) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				showLongToast(msg);
			}
		});
	}
}
