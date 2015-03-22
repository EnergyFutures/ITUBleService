package dk.itu.energyfutures.ble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import dk.itu.energyfutures.ble.BluetoothLeService.LocalBinder;

public class DeviceScanActivity extends ListActivity implements NewPacketListner {
	private final static String TAG = DeviceScanActivity.class.getSimpleName();
	private HashMap<String, HashSet<AdvertisementPacket>> packets = new HashMap<String, HashSet<AdvertisementPacket>>();
	private MyAdapter adapter = new MyAdapter(packets);
	private BluetoothLeService service;
	private boolean bound;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.getBooleanExtra("EXIT", false)) {
				finish();
				return;
			}
		}
		getActionBar().setTitle("ITU BLE TOOL");
		this.setListAdapter(adapter);
		this.getListView().setDivider(getResources().getDrawable(R.drawable.divider));
		this.getListView().setDividerHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
	}

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
		public View getView(final int i, View view, ViewGroup viewGroup) {
			TextView v = (TextView) view;
			if (v == null) {
				v = new TextView(DeviceScanActivity.this);
				v.setTextSize(30);
				v.setGravity(Gravity.CENTER_HORIZONTAL);
				v.setClickable(true);
				v.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (v != null && v.getTag() != null) {
							Intent intent = new Intent(DeviceScanActivity.this, LocationActivity.class);
							intent.putExtra(LocationActivity.MOTE_LOCATION, mData.get(i).getKey());
							startActivity(intent);
						}
					}
				});
			}
			Entry<String, HashSet<AdvertisementPacket>> item = getItem(i);
			v.setText(item.getKey());
			v.setTag(item.getValue());
			return v;
		}
	}

	@Override
	public void newPacketArrived(final AdvertisementPacket packet) {
		HashSet<AdvertisementPacket> localPackets = packets.get(packet.getLocation());
		if (localPackets == null) {
			localPackets = new HashSet<AdvertisementPacket>();
			packets.put(packet.getLocation(), localPackets);
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
	protected void onStart() {
		super.onStart();
		// Bind to LocalService
		Intent intent = new Intent(this, BluetoothLeService.class);
		startService(intent);
		if (!bindService(intent, connection, BIND_AUTO_CREATE)) {
			Toast.makeText(getApplicationContext(), "COULD NOT BIND TO BLE :0(", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (bound) {
			service.removeListner(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshRooms();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (bound) {
			unbindService(connection);
			bound = false;
			if (!isChangingConfigurations()) {
				Intent intent = new Intent(this, BluetoothLeService.class);
				stopService(intent);
			}
		}
	}

	private void refreshRooms() {
		if (bound) {
			Set<AdvertisementPacket> allPackets = service.getPackets();
			for (AdvertisementPacket packet : allPackets) {
				HashSet<AdvertisementPacket> locationPackets = packets.get(packet.getLocation());
				if (locationPackets == null) {
					locationPackets = new HashSet<AdvertisementPacket>();
					packets.put(packet.getLocation(), locationPackets);
					adapter.mData.clear();
					adapter.mData.addAll(packets.entrySet());
				}
				locationPackets.add(packet);
			}
			adapter.notifyDataSetChanged();
			service.registerListner(this);
		}
	}

	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) iBinder;
			service = binder.getService();
			service.registerListner(DeviceScanActivity.this);
			bound = true;
			refreshRooms();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			bound = false;
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.main_refresh) {
			if (bound) {
				service.getPackets().clear();
				packets.clear();
				adapter.mData.clear();
				refreshRooms();
			}
			return true;
		} else if (id == R.id.main_exit) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}