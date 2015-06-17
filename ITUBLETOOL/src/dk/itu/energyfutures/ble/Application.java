package dk.itu.energyfutures.ble;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;
import dk.itu.energyfutures.ble.helpers.ITUConstants;

public class Application extends android.app.Application implements DataSinkFlagChangedNotifier{
	private static SharedPreferences sharedPreferences;
	private static Context applicationContext;
	private static Handler handler;
	public static boolean emptyingBuffer = false;
	public static AtomicBoolean connectedToInternet = new AtomicBoolean(false);
	public static List<DataSinkFlagChangedListner> dataSinkFlagListners;
	public static Application instance;
	
	@Override
	public void onCreate() {
		super.onCreate();
		applicationContext = getApplicationContext();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		handler = new Handler(applicationContext.getMainLooper());
		NetworkInfo activeNetwork =((ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		connectedToInternet.set(activeNetwork != null && activeNetwork.isConnected());
		dataSinkFlagListners = new ArrayList<DataSinkFlagChangedListner>();
		Application.instance = this;
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
	
	public static boolean isParticipatoryDataSink(){
		return Application.getPref(ITUConstants.DEVICE_IS_A_PARTICIPATORY_DATA_SINK_KEY,false);
	}
	
	public static void setParticipatoryDataSink(boolean value) {
		Application.putBoolean(ITUConstants.DEVICE_IS_A_PARTICIPATORY_DATA_SINK_KEY, value);
		for(DataSinkFlagChangedListner listner : dataSinkFlagListners){
			listner.onDataSinkFlagChanged(value);
		}
	}
	
	public static boolean isDataSink(){
		return Application.getPref(ITUConstants.DEVICE_IS_A_DATA_SINK_KEY,false);
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

	public static Boolean getShowAdvanceSettings() {
		return Application.getPref(ITUConstants.SHOW_ADVANCE_SETTINGS_KEY,false);
	}

	public static Boolean isConfigNormalMotesEnabled() {
		return Application.getPref(ITUConstants.ENABLE_CONFIG_OF_NORMAL_MOTES,false);
	}

	public static void setIsConfigNormalMotesEnabledFlag(boolean value){
		Application.putBoolean(ITUConstants.ENABLE_CONFIG_OF_NORMAL_MOTES, value);
	}
	
	public static boolean isConnectedToInternet(){
		return connectedToInternet.get();
	}

	@Override
	public void registerDataSinkFlagChangedListner(DataSinkFlagChangedListner listner) {
		dataSinkFlagListners.add(listner);
	}

	@Override
	public void unRegisterDataSinkFlagChangedListner(DataSinkFlagChangedListner listner) {
		dataSinkFlagListners.remove(listner);
	}

	public static boolean useEnergySavingFeatures() {
		return Application.getPref(ITUConstants.USE_ENERGY_SAVINGS_FEATURES, true);
	}
}
