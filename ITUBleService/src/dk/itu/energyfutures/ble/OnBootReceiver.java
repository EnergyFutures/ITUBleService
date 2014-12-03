package dk.itu.energyfutures.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent) 
    {
        // Create Intent
        Intent serviceIntent = new Intent(context, BluetoothLeService.class);
        // Start service
        context.startService(serviceIntent);
    }

 }
