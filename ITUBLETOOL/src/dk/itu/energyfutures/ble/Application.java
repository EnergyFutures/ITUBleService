package dk.itu.energyfutures.ble;

import dk.itu.energyfutures.ble.BluetoothLeService.LocalBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

public class Application extends android.app.Application{
	public static BluetoothLeService service;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(getApplicationContext(), "APP CLASS", Toast.LENGTH_SHORT).show();
		//Intent intent = new Intent(this, BluetoothLeService.class);
		//bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}
	
	private ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) iBinder;
			service = binder.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
		}
	};
	
}
