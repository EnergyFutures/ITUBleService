package dk.itu.energyfutures.ble.activities;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import dk.itu.energyfutures.ble.Application;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService.LocalBinder;
import dk.itu.energyfutures.ble.R;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.packethandlers.AdvertisementPacket;
import dk.itu.energyfutures.ble.packethandlers.PacketListListner;
import dk.itu.energyfutures.ble.task.ActuationTask;
import dk.itu.energyfutures.ble.task.EmptyingBufferListner;
import dk.itu.energyfutures.ble.task.JSONTask;
import dk.itu.energyfutures.ble.task.WindowTask;

public class LocationActivity extends Activity implements PacketListListner, EmptyingBufferListner {
	private final static String TAG = LocationActivity.class.getSimpleName();
	public static final String MOTE_LOCATION = "MOTE.LOCATION";
	private List<AdvertisementPacket> packets = new ArrayList<AdvertisementPacket>();
	private GridAdapter gridAdapter = new GridAdapter();
	private String location;
	protected BluetoothLEBackgroundService service;
	private boolean bound;
	private static final int actuationColor = Color.argb(135, 215, 252, 249);
	private static final int defaultColor = Color.argb(0, 0, 0, 0);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.location_view);
		GridView gv = (GridView) findViewById(R.id.gridView1);
		gv.setAdapter(gridAdapter);
		location = getIntent().getStringExtra(LocationActivity.MOTE_LOCATION);
		if (location == null) {
			finish();
			return;
		}
		setTitle("Location " + location);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		try {
	        ViewConfiguration config = ViewConfiguration.get(this);
	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception ex) {
	        Log.e(TAG,"Error with menu button: " + ex.getMessage());
	    }
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
			service.registerPacketListner(LocationActivity.this);
			service.registerEmptypingListner(LocationActivity.this);
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
		if (bound) {
			packets.clear();
			Collection<AdvertisementPacket> allPackets = service.getPackets().values();
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
				v = (RelativeLayout) getLayoutInflater().inflate(R.layout.sensor_container_layout, null);
				v.setClickable(true);
				v.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (v != null && v.getTag() != null) {
							final AdvertisementPacket packet = (AdvertisementPacket) v.getTag();
							if (ITUConstants.ITU_SENSOR_TYPE.AC.equals(packet.getSensorType())) {
								final ProgressDialog dialog = ProgressDialog.show(LocationActivity.this, "Please wait", "Connecting...", true, false);
								new ActuationTask(packet, dialog, LocationActivity.this, service).execute(null, null);
							} else if (ITUConstants.ITU_SENSOR_TYPE.WINDOW.equals(packet.getSensorType())) {
								final WindowTask task = new WindowTask(packet, LocationActivity.this, service);
								Builder builder = new AlertDialog.Builder(LocationActivity.this);
								builder.setTitle("Window Control");
								builder.setMessage("Please wait, connecting...");
								builder.setPositiveButton("Close", null);
								builder.setNegativeButton("Open", null);
								builder.setNeutralButton("Stop", null);
								builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
									@Override
									public void onDismiss(DialogInterface dialog) {
										task.dismissed();
									}
								});
								final AlertDialog dialog = builder.create();
								dialog.setCanceledOnTouchOutside(true);
								dialog.setOnShowListener(new DialogInterface.OnShowListener() {

									@Override
									public void onShow(DialogInterface dialogI) {
										Button open = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
										open.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												task.closeWindow();
											}
										});
										Button close = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
										close.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												task.openWindow();
											}
										});
										Button stop = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
										stop.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												task.stopWindow();
											}
										});
									}
								});
								task.setDialog(dialog);
								task.execute(null, null);
								dialog.show();
							} else if (ITUConstants.ITU_SENSOR_TYPE.JSON.equals(packet.getSensorType())) {
								final ProgressDialog dialog = ProgressDialog.show(LocationActivity.this, "Please wait", "Connecting...", true, false);
								new JSONTask(packet, dialog, LocationActivity.this, service).execute(null, null);
							}
						}
					}
				});
			}
			ImageView im = (ImageView) v.findViewById(R.id.imageView);
			im.setImageResource(ITUConstants.findIconBySensorType(advertisementPacket));
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) im.getLayoutParams();
			TextView vt = (TextView) v.findViewById(R.id.ValueText);
			String value = ITUConstants.getValueStringFromEnum(advertisementPacket);
			if ("" == value) {
				vt.setVisibility(View.GONE);
				lp.addRule(RelativeLayout.CENTER_IN_PARENT);
				lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
				im.setLayoutParams(lp);
			} else {
				lp.removeRule(RelativeLayout.CENTER_IN_PARENT);
				lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				im.setLayoutParams(lp);
				vt.setText(value);
				vt.setVisibility(View.VISIBLE);
			}
			if (ITUConstants.ITU_SENSOR_CONFIG_TYPE.ACTUATOR_TYPE.equals(advertisementPacket.getSensorConfigType())) {
				v.setBackgroundColor(actuationColor);
				vt.setText(vt.getText() + "|" + advertisementPacket.getDeviceName());
			} else {
				v.setBackgroundColor(defaultColor);
			}
			if (ITUConstants.ITU_SENSOR_TYPE.AMPERE.equals(advertisementPacket.getSensorType())) {
				vt.setText(vt.getText() + "|" + advertisementPacket.getDeviceName());
			}
			TextView ts = (TextView) v.findViewById(R.id.timestamp);
			ts.setText(ITUConstants.dateFormat.format(advertisementPacket.getTimeStamp()));
			TextView ct = (TextView) v.findViewById(R.id.coordinateText);
			String coor = ITUConstants.getCoordinateStringFromEnum(advertisementPacket);
			if ("".equals(coor)) {
				ct.setText("");
				ct.setVisibility(View.GONE);
			} else {
				ct.setText(coor);
				ct.setVisibility(View.VISIBLE);
			}
			v.setTag(advertisementPacket);
			return v;
		}
	}

	@Override
	public void newPacketArrived(final AdvertisementPacket packet) {
		if (location.equals(packet.getLocation())) {
			final int index = packets.indexOf(packet);
			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (index >= 0) {
						packets.set(index, packet);
					} else {
						packets.add(packet);
					}
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
		case R.id.location_refresh:
			packets.clear();
			gridAdapter.notifyDataSetChanged();
			return true;
		case R.id.location_exit:
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

	@Override
	public void PacketsDeprecated(final List<AdvertisementPacket> deprecatedPackets) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				boolean didDeprecation = false;
				for (AdvertisementPacket packet : deprecatedPackets) {
					if (packets.remove(packet)) {
						didDeprecation = true;
					}
				}
				if (didDeprecation) {
					gridAdapter.notifyDataSetChanged();
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
