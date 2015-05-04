package dk.itu.energyfutures.ble.activities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import dk.itu.energyfutures.ble.AdvertisementPacket;
import dk.itu.energyfutures.ble.Application;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService;
import dk.itu.energyfutures.ble.R;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService.LocalBinder;
import dk.itu.energyfutures.ble.R.drawable;
import dk.itu.energyfutures.ble.R.id;
import dk.itu.energyfutures.ble.R.menu;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.packethandlers.PacketListListner;

public class LocationListActivity extends Activity implements PacketListListner {
	private final static String TAG = LocationListActivity.class.getSimpleName();
	private Map<String, HashSet<AdvertisementPacket>> packets = new TreeMap<String, HashSet<AdvertisementPacket>>();
	private MyAdapter adapter = new MyAdapter(packets);
	private BluetoothLEBackgroundService service;
	private static final int backgroundColor = Color.argb(135, 200, 120, 249);
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
		setContentView(R.layout.location_list_view);
		GridView gv = (GridView) findViewById(R.id.gridView3);
		gv.setAdapter(adapter);
//		this.setListAdapter(adapter);
//		this.getListView().setDivider(getResources().getDrawable(R.drawable.divider));
//		this.getListView().setDividerHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
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
				v = new TextView(LocationListActivity.this);
				// v.setBackgroundColor(backgroundColor);
			}
			v.setGravity(Gravity.CENTER_HORIZONTAL);
			v.setTextSize(30);
			v.setMaxLines(1);
			v.setClickable(true);
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v != null && v.getTag() != null) {
						Intent intent = new Intent(LocationListActivity.this, LocationActivity.class);
						intent.putExtra(LocationActivity.MOTE_LOCATION, mData.get(i).getKey());
						startActivity(intent);
					}
				}
			});
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
		Intent intent = new Intent(this, BluetoothLEBackgroundService.class);
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
				Intent intent = new Intent(this, BluetoothLEBackgroundService.class);
				stopService(intent);
			}
		}
	}

	private void refreshRooms() {
		if (bound) {
			Collection<AdvertisementPacket> allPackets = service.getPackets();
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
			service.registerListner(LocationListActivity.this);
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
		MenuItem item = menu.findItem(R.id.main_mote_view);
		item.setVisible(Application.isDataSink());
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
		}else if (id == R.id.main_mote_view) {
			Intent intent = new Intent(LocationListActivity.this, MoteListActivity.class);
			startActivity(intent);
		}
		else if (id == R.id.main_toggle_data_sink) {
			Application.toggleDataSink();
			invalidateOptionsMenu();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void PacketsDeprecated(List<AdvertisementPacket> deprecatedPackets) {
		boolean removedLocation = false;
		for(AdvertisementPacket packet : deprecatedPackets){
			HashSet<AdvertisementPacket> localPackets = packets.get(packet.getLocation());
			if (localPackets != null) {
				localPackets.remove(packet);
				if(localPackets.size() == 0){
					packets.remove(packet.getLocation());
					removedLocation = true;
				}
			}
		}
		if(removedLocation){
			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					adapter.notifyDataSetChanged();
				}
			});
		}
	}
}