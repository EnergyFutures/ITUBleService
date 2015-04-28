package dk.itu.energyfutures.ble.activities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import dk.itu.energyfutures.ble.AdvertisementPacket;
import dk.itu.energyfutures.ble.Application;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService.LocalBinder;
import dk.itu.energyfutures.ble.R;
import dk.itu.energyfutures.ble.packethandlers.PacketListListner;

public class MoteListActivity extends Activity implements PacketListListner {
	private final static String TAG = MoteListActivity.class.getSimpleName();
	private HashMap<String, HashSet<AdvertisementPacket>> packets = new HashMap<String, HashSet<AdvertisementPacket>>();
	private MyAdapter adapter = new MyAdapter(packets);
	protected BluetoothLEBackgroundService service;
	private boolean bound;
	private static final int backgroundColor = Color.argb(135, 215, 120, 249);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mote_list_view);
		GridView gv = (GridView) findViewById(R.id.gridView2);
		gv.setAdapter(adapter);
		setTitle("Mote View");
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService
		Intent intent = new Intent(this, BluetoothLEBackgroundService.class);
		if (!bindService(intent, connection, BIND_AUTO_CREATE)) {
			Application.showLongToast("COULD NOT BIND TO BLE :0(");
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (bound) {
			service.removeListner(this);
			unbindService(connection);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshList();
	}

	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) iBinder;
			service = binder.getService();
			service.registerListner(MoteListActivity.this);
			bound = true;
			refreshList();
			adapter.notifyDataSetChanged();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			bound = false;
			System.out.println();
		}
	};

	// Adapter for holding devices found through scanning.
	private class MyAdapter extends BaseAdapter {
		public final ArrayList<Entry<String, HashSet<AdvertisementPacket>>> mData;

		public MyAdapter(Map<String, HashSet<AdvertisementPacket>> map) {
			mData = new ArrayList<Entry<String, HashSet<AdvertisementPacket>>>();
			mData.addAll(map.entrySet());
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		@Override
		public Entry<String, HashSet<AdvertisementPacket>> getItem(int position) {
			return mData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int i, View v, ViewGroup viewGroup) {
			if (v == null) {
				v = getLayoutInflater().inflate(R.layout.mote_view, null, false);
				v.setBackgroundColor(backgroundColor);
			}
			Entry<String, HashSet<AdvertisementPacket>> item = getItem(i);
			AdvertisementPacket packet = null;
			for(AdvertisementPacket p : item.getValue()){
				packet = p;
				break;
			}
			if(packet != null){
				TextView deviceName = (TextView) v.findViewById(R.id.device_name);
				deviceName.setText("Name: " + packet.getDeviceName());
				
				TextView locationName = (TextView) v.findViewById(R.id.location);
				locationName.setText("Location: " + packet.getLocation());
				
				TextView buffer = (TextView) v.findViewById(R.id.buffer_level);
				buffer.setText("Buffer: " + packet.getBufferLevel()+"%");
				
				TextView bufferFlag = (TextView) v.findViewById(R.id.buffer_flag);
				bufferFlag.setText("Buf-full: " + packet.isBufferNeedsCleaning());
				
				TextView battery = (TextView) v.findViewById(R.id.battery_level);
				battery.setText("Battery: " + packet.getBatteryLevel() +"%");
				
				TextView count = (TextView) v.findViewById(R.id.number_of_services);
				count.setText("Services: " + item.getValue().size());
			}
			v.setTag(item.getValue());
			return v;
		}
	}

	@Override
	public void newPacketArrived(AdvertisementPacket packet) {
		HashSet<AdvertisementPacket> localPackets = packets.get(packet.getDevice().getAddress());
		if (localPackets == null) {
			localPackets = new HashSet<AdvertisementPacket>();
			packets.put(packet.getDevice().getAddress(), localPackets);
			adapter.mData.clear();
			adapter.mData.addAll(packets.entrySet());
		}
		localPackets.remove(packet);
		localPackets.add(packet);
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mote, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case R.id.mote_refresh:
			if (bound) {
				service.getPackets().clear();
				packets.clear();
				adapter.mData.clear();
				refreshList();
			}
			return true;
		case R.id.mote_exit:
			Intent intent = new Intent(getApplicationContext(), LocationListActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra("EXIT", true);
			startActivity(intent);
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void refreshList() {
		if (bound) {
			Collection<AdvertisementPacket> allPackets = service.getPackets();
			for (AdvertisementPacket packet : allPackets) {
				HashSet<AdvertisementPacket> locationPackets = packets.get(packet.getDevice().getAddress());
				if (locationPackets == null) {
					locationPackets = new HashSet<AdvertisementPacket>();
					packets.put(packet.getDevice().getAddress(), locationPackets);
					adapter.mData.clear();
					adapter.mData.addAll(packets.entrySet());
				}
				locationPackets.add(packet);
			}
			adapter.notifyDataSetChanged();
			service.registerListner(this);
		}
	}

	@Override
	public void PacketsDeprecated(List<AdvertisementPacket> deprecatedPackets) {
		boolean removedPacket = false;
		for (AdvertisementPacket packet : deprecatedPackets) {
			HashSet<AdvertisementPacket> localPackets = packets.get(packet.getDevice().getAddress());
			if (localPackets != null) {
				localPackets.remove(packet);
				removedPacket = true;
				if (localPackets.size() == 0) {
					packets.remove(packet.getDevice().getAddress());
				}
			}
		}
		if (removedPacket) {
			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					adapter.notifyDataSetChanged();
				}
			});
		}
	}
}
