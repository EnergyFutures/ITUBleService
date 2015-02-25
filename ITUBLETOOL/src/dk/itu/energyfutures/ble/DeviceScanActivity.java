/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.itu.energyfutures.ble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import dk.itu.energyfutures.ble.BluetoothLeService.LocalBinder;

public class DeviceScanActivity extends ListActivity implements NewPacketListner {
	
    private BluetoothLeService service;
    private boolean bound;
    private HashMap<String,ArrayList<AdvertisementPacket>> packets = new HashMap<String, ArrayList<AdvertisementPacket>>();
    private MyAdapter adapter = new MyAdapter(packets);
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("ITU BLE 2");
        this.setListAdapter(adapter);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, BluetoothLeService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (bound) {
        	service.removeListner(this);
            unbindService(connection);
            bound = false;
        }
    }
    
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,IBinder iBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) iBinder;
            service = binder.getService();
            service.registerListner(DeviceScanActivity.this);
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    // Adapter for holding devices found through scanning.
    private class MyAdapter extends BaseAdapter {
        public final ArrayList<Entry<String, ArrayList<AdvertisementPacket>>> mData;

        public MyAdapter(Map<String,ArrayList<AdvertisementPacket>> map) {
            mData = new ArrayList<Entry<String, ArrayList<AdvertisementPacket>>>() ;
            mData.addAll(map.entrySet());
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Entry<String, ArrayList<AdvertisementPacket>> getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
		
		@Override
        public View getView(int i, View view, ViewGroup viewGroup) {
           TextView v = new TextView(DeviceScanActivity.this);
           v.setText("Hello");
           return v;
        }
    }


	@Override
	public void newPacketArrived(AdvertisementPacket packet) {
		ArrayList<AdvertisementPacket> localPackets = packets.get(packet.getLocation());
        if(localPackets == null){
        	localPackets = new ArrayList<AdvertisementPacket>();
        	packets.put(packet.getLocation(), localPackets);
        }
        localPackets.add(packet);
        adapter.mData.clear();
        adapter.mData.addAll(packets.entrySet());
        this.runOnUiThread(new Runnable() {			
			@Override
			public void run() {
				 adapter.notifyDataSetChanged();
			}
		});       
	}
}