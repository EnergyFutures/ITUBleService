package dk.itu.energyfutures.ble.activities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import dk.itu.energyfutures.ble.Application;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService.LocalBinder;
import dk.itu.energyfutures.ble.R;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.packethandlers.AdvertisementPacket;
import dk.itu.energyfutures.ble.packethandlers.PacketListListner;
import dk.itu.energyfutures.ble.task.EmptyingBufferListner;

public class MoteListActivity extends Activity implements PacketListListner, EmptyingBufferListner {
	private final static String TAG = MoteListActivity.class.getSimpleName();
	private Map<String, HashSet<AdvertisementPacket>> packets = new TreeMap<String, HashSet<AdvertisementPacket>>();
	private MyAdapter adapter = new MyAdapter(packets);
	protected BluetoothLEBackgroundService service;
	private boolean bound;
	private static final int DEFAULT_COLOR = Color.argb(135, 215, 120, 249);
	private static final int CONFIG_COLOR = Color.argb(135, 215, 120, 120);
	private static final int BUFFER_FULL_COLOR = Color.argb(135, 120, 120, 120);
	private static final int CONFIG_DEVICE_REQUEST = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
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
			service.removePacketListner(this);
			service.removeNewBornPacketListner(this);
			service.unregisterEmptypingListner(this);
			unbindService(connection);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshList();
		setProgressBarIndeterminateVisibility(Application.emptyingBuffer);
	}

	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) iBinder;
			service = binder.getService();
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
			}
			Entry<String, HashSet<AdvertisementPacket>> item = getItem(i);
			AdvertisementPacket packet = null;
			for (AdvertisementPacket p : item.getValue()) {
				packet = p;
				break;
			}
			if (packet != null) {
				TextView deviceAdr = (TextView) v.findViewById(R.id.device_adr);
				deviceAdr.setText(packet.getDeviceAdr());

				TextView deviceName = (TextView) v.findViewById(R.id.device_name);
				deviceName.setText("Name: " + packet.getDeviceName());

				TextView locationName = (TextView) v.findViewById(R.id.location);
				locationName.setText("Location: " + packet.getLocation());
				boolean isNewBorn = "NEWBORN".equals(packet.getDeviceName()) && "YYY".equals(packet.getLocation());
				boolean isConnectable = packet.isBufferNeedsCleaning() && !"NEWBORN".equals(packet.getDeviceName()) && Application.isConfigNormalMotesEnabled();
				if(isNewBorn || isConnectable){
					if(isNewBorn){
						v.setBackgroundColor(CONFIG_COLOR);
					}else{
						v.setBackgroundColor(DEFAULT_COLOR);
					}
					v.setClickable(true);
					v.setOnClickListener(new ClickAction());
				}else{
					v.setBackgroundColor(DEFAULT_COLOR);
					v.setClickable(false);
					v.setOnClickListener(null);
				}

				TextView buffer = (TextView) v.findViewById(R.id.buffer_level);
				buffer.setText("Buffer: " + packet.getBufferLevel() + "%");

				TextView bufferFlag = (TextView) v.findViewById(R.id.buffer_flag);
				bufferFlag.setText("Buf-full: " + packet.isBufferNeedsCleaning());
				if(packet.isBufferNeedsCleaning()){
					bufferFlag.setBackgroundColor(BUFFER_FULL_COLOR);
				}else{
					bufferFlag.setBackgroundColor(Color.TRANSPARENT);
				}

				TextView battery = (TextView) v.findViewById(R.id.battery_level);
				battery.setText("Battery: " + packet.getBatteryLevel() + "%");

				TextView count = (TextView) v.findViewById(R.id.number_of_services);
				count.setText("Services: " + item.getValue().size());
				
				TextView time = (TextView) v.findViewById(R.id.timestamp);
				time.setText("Updated: " + ITUConstants.dateFormat.format(packet.getTimeStamp()));
				
				v.setTag(packet);
			}
			return v;
		}
	}
	
	private class ClickAction implements View.OnClickListener{
		@Override
		public void onClick(View v) {
			if(v != null && v.getTag() != null){
				AdvertisementPacket tag = (AdvertisementPacket) v.getTag();
				Intent intent = new Intent(MoteListActivity.this, DeviceConfigActivity.class);
				intent.putExtra(DeviceConfigActivity.DEVICE_ADR, tag.getDevice().getAddress());
				intent.putExtra(DeviceConfigActivity.DEVICE_ID, tag.getId());
				startActivityForResult(intent, CONFIG_DEVICE_REQUEST);;
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CONFIG_DEVICE_REQUEST) {
			if (resultCode == RESULT_OK) {
				String deviceAdr = data.getStringExtra(DeviceConfigActivity.DEVICE_ADR);
				service.getNewBornPackets().remove(deviceAdr);
				packets.remove(deviceAdr);
				adapter.mData.clear();
				adapter.mData.addAll(packets.entrySet());
				adapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void newPacketArrived(final AdvertisementPacket packet) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				HashSet<AdvertisementPacket> localPackets = packets.get(packet.getDeviceAdr());
				if (localPackets == null) {
					localPackets = new HashSet<AdvertisementPacket>();
					packets.put(packet.getDeviceAdr(), localPackets);
					adapter.mData.clear();
					adapter.mData.addAll(packets.entrySet());
				}
				localPackets.remove(packet);
				localPackets.add(packet);
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
				service.getNewBornPackets().clear();
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
			List<AdvertisementPacket> allPackets = new ArrayList<AdvertisementPacket>(service.getPackets().values());
			allPackets.addAll(service.getNewBornPackets().values());
			for (AdvertisementPacket packet : allPackets) {
				HashSet<AdvertisementPacket> locationPackets = packets.get(packet.getDeviceAdr());
				if (locationPackets == null) {
					locationPackets = new HashSet<AdvertisementPacket>();
					packets.put(packet.getDeviceAdr(), locationPackets);
					adapter.mData.clear();
					adapter.mData.addAll(packets.entrySet());
				}
				locationPackets.add(packet);
			}
			adapter.notifyDataSetChanged();
			service.registerPacketListner(this);
			service.registerNewBornPacketListner(this);
			service.registerEmptypingListner(this);
		}
	}

	@Override
	public void PacketsDeprecated(final List<AdvertisementPacket> deprecatedPackets) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				boolean removedPacket = false;
				for (AdvertisementPacket packet : deprecatedPackets) {
					HashSet<AdvertisementPacket> localPackets = packets.get(packet.getDeviceAdr());
					if (localPackets != null) {
						localPackets.remove(packet);
						removedPacket = true;
						if (localPackets.size() == 0) {
							packets.remove(packet.getDeviceAdr());
						}
					}
				}
				if (removedPacket) {
					adapter.mData.clear();
					adapter.mData.addAll(packets.entrySet());
					adapter.notifyDataSetChanged();
				}
			}
		});
	}

	@Override
	public void emptyingBufferStateChanged() {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setProgressBarIndeterminateVisibility(Application.emptyingBuffer);
			}
		});
	}
}
