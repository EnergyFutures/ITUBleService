package dk.itu.energyfutures.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {
	private static final String TAG = NetworkChangeReceiver.class.getSimpleName();
	private static ConnectivityManager connMgr;
	@Override
	public void onReceive(final Context context, final Intent intent) {
		if(connMgr == null){
			connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		}
		Application.connectedToInternet.set(connMgr.getActiveNetworkInfo().isConnected());
		Log.i(TAG,"Receiver called");
	}
}