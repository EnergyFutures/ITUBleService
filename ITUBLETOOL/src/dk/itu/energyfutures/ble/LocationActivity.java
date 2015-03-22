package dk.itu.energyfutures.ble;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import dk.itu.energyfutures.ble.BluetoothLeService.LocalBinder;
import dk.itu.energyfutures.ble.helpers.ITUConstants;

public class LocationActivity extends Activity implements NewPacketListner{
	private final static String TAG = LocationActivity.class.getSimpleName();
	public static final String MOTE_LOCATION = "MOTE.LOCATION";
	private List<AdvertisementPacket> packets = new ArrayList<AdvertisementPacket>();
	private GridAdapter gridAdapter = new GridAdapter();
	private String location;
	protected BluetoothLeService service;
	private boolean bound;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ble_service);
		GridView gv = (GridView) findViewById(R.id.gridView1);
		gv.setAdapter(gridAdapter);
		location = getIntent().getStringExtra(LocationActivity.MOTE_LOCATION);
		if (location == null) {
			finish();
			return;
		}		
		setTitle("Location " + location);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	
	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService
		Intent intent = new Intent(this, BluetoothLeService.class);
		if(!bindService(intent, connection, BIND_AUTO_CREATE)){
			Toast.makeText(getApplicationContext(), "COULD NOT BIND TO BLE :0(", Toast.LENGTH_LONG).show();
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
			service.registerListner(LocationActivity.this);
			bound = true;
			refreshList();
			gridAdapter.notifyDataSetChanged();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			bound = false;
			System.out.println();
		}
	};
	
	private void refreshList() {
		if(bound){
			packets.clear();
			Set<AdvertisementPacket> allPackets = service.getPackets();
			for (AdvertisementPacket packet : allPackets) {
				if (location.equalsIgnoreCase(packet.getLocation())) {
					packets.add(packet);
				}
			}
		}
	}

	// Adapter for holding devices found through scanning.
	private class GridAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return packets.size();
		}

		@Override
		public AdvertisementPacket getItem(int position) {
			return packets.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			RelativeLayout v = (RelativeLayout) view;
			AdvertisementPacket advertisementPacket = packets.get(i);
			if (v == null) {
				
				v = (RelativeLayout) getLayoutInflater().inflate(R.layout.sensor_container_layout,null);
				v.setClickable(true);
				v.setOnClickListener(new View.OnClickListener() {					
					@Override
					public void onClick(View v) {
						if(v != null && v.getTag() != null){
							System.out.println("Click :0)");
						}
					}
				});
			}
			ImageView im = (ImageView) v.findViewById(R.id.imageView);
			im.setImageResource(ITUConstants.findIconBySensorType(advertisementPacket));
			TextView vt = (TextView) v.findViewById(R.id.ValueText);
			String value = ITUConstants.getValueStringFromEnum(advertisementPacket);
			if("" == value){
				vt.setVisibility(View.GONE);
			}else{
				vt.setText(value);
				vt.setVisibility(View.VISIBLE);
			}			
			TextView ts = (TextView) v.findViewById(R.id.timestamp);
			ts.setText(ITUConstants.dateFormat.format(advertisementPacket.getTimeStamp()));
			TextView ct = (TextView) v.findViewById(R.id.coordinateText);
			String coor = ITUConstants.getCoordinateStringFromEnum(advertisementPacket);
			if(coor == ""){
				ct.setVisibility(View.GONE);
			}else{
				ct.setText(coor);
				ct.setVisibility(View.VISIBLE);
			}			
			ImageView mv = (ImageView) v.findViewById(R.id.multiple_values_imageview);
			mv.setVisibility(View.GONE);
			v.setTag(advertisementPacket);
			return v;
		}
	}
	
	

	@Override
	public void newPacketArrived(AdvertisementPacket packet) {
		if (location.equalsIgnoreCase(packet.getLocation())) {
			int index = packets.indexOf(packet);
			if(index >= 0){
				packets.set(index,packet);
			}else{
				packets.add(packet);
			}
			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					gridAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.location, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case R.id.location_sort:			
			return true;
		case R.id.location_exit:
			Intent intent = new Intent(getApplicationContext(), DeviceScanActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra("EXIT", true);
			startActivity(intent);
			finish();
	        return true;
		default:
			return super.onOptionsItemSelected(item);
		}		
	}
}
