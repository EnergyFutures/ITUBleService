package dk.itu.energyfutures.ble;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class DummyStarterActivity extends ListActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Intent serviceIntent = new Intent(this.getApplicationContext(), BluetoothLeService.class);
        startService(serviceIntent);
        Toast.makeText(getApplicationContext(), "BLE SERVICE SHOULD NOW BE STARTED", Toast.LENGTH_SHORT).show();
      //  finish();
	}
}