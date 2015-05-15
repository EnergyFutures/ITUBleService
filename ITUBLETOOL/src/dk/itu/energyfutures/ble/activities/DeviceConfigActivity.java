package dk.itu.energyfutures.ble.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import dk.itu.energyfutures.ble.Application;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService.LocalBinder;
import dk.itu.energyfutures.ble.R;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.helpers.ITUConstants.ITU_SENSOR_COORDINATE;
import dk.itu.energyfutures.ble.packethandlers.AdvertisementPacket;
import dk.itu.energyfutures.ble.sensorhandlers.MoteConfigParser;
import dk.itu.energyfutures.ble.sensorhandlers.SensorParser;
import dk.itu.energyfutures.ble.task.ConfigTask;
import dk.itu.energyfutures.ble.task.ConfigTaskListner;

public class DeviceConfigActivity extends Activity implements ConfigTaskListner {
	private final static String TAG = DeviceConfigActivity.class.getSimpleName();
	public static final String DEVICE_ADR = "DEVICE_ADR";
	public static final String DEVICE_ID = "DEVICE_ID";
	private String deviceAdr;
	protected BluetoothLEBackgroundService service;
	private boolean bound;
	private AdvertisementPacket packet;
	private TextView textView;
	private static ConfigTask configTask;
	private LinearLayout ll;
	private static MoteConfigParser moteConfigParser;
	private static List<SensorParser> sensorParsers;
	private ScrollView scrollView;
	private static String statusMSG = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		textView = new TextView(this);
		textView.setText(statusMSG);
		textView.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				statusMSG = s.toString();
			}
		});
		setContentView(R.layout.device_activity_layout);
		scrollView = (ScrollView) findViewById(R.id.scrollview);
		ll = (LinearLayout) findViewById(R.id.main_ll);
		ll.addView(textView);
		if (DeviceConfigActivity.configTask != null) {
			DeviceConfigActivity.configTask.registerListner(this);
		}
		deviceAdr = getIntent().getStringExtra(DeviceConfigActivity.DEVICE_ADR);
		if (deviceAdr == null) {
			setResult(Activity.RESULT_CANCELED);
			finish();
			return;
		}
		setTitle(deviceAdr);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		if (moteConfigParser != null) {
			processMoteConfig();
		}
		if (sensorParsers != null) {
			processSensors();
		}
		if (moteConfigParser != null || sensorParsers != null) {
			addConfigureButton();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService
		Intent intent = new Intent(this, BluetoothLEBackgroundService.class);
		if (!bindService(intent, connection, BIND_AUTO_CREATE)) {
			Application.showLongToast("COULD NOT BIND TO BLE :0(");
			setResult(Activity.RESULT_CANCELED);
			finish();
		}
		scrollView.scrollTo(0, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (bound) {
			unbindService(connection);
			DeviceConfigActivity.configTask.unregisterListner(this);
			if (!isChangingConfigurations()) {
				DeviceConfigActivity.configTask.cancel(false);
				DeviceConfigActivity.configTask = null;
				moteConfigParser = null;
				sensorParsers = null;
				statusMSG = "";
			}
		}
	}

	private ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) iBinder;
			service = binder.getService();
			bound = true;
			if (DeviceConfigActivity.configTask == null) {
				packet = service.getNewBornPackets().get(deviceAdr);
				if (packet == null) {
					String id = getIntent().getStringExtra(DEVICE_ID);
					if (id == null) {
						setResult(Activity.RESULT_CANCELED);
						finish();
					}
					packet = service.getPackets().get(id);
					if (packet == null) {
						setResult(Activity.RESULT_CANCELED);
						finish();
					}
				}
				DeviceConfigActivity.configTask = new ConfigTask(packet.getDevice(), DeviceConfigActivity.this);
				DeviceConfigActivity.configTask.registerListner(DeviceConfigActivity.this);
				DeviceConfigActivity.configTask.execute(null, null, null);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			bound = false;
			System.out.println();
		}
	};

	@Override
	public void onDoneDiscovering(final List<BluetoothGattCharacteristic> sensors, final BluetoothGattCharacteristic configChar) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textView.setText("READY to configure " + sensors.size() + " service" + (sensors.size() > 1 ? "s" : ""));
				try {
					moteConfigParser = new MoteConfigParser(configChar.getValue());
					processMoteConfig();
					sensorParsers = new ArrayList<SensorParser>();
					for (BluetoothGattCharacteristic bleChar : sensors) {
						sensorParsers.add(new SensorParser(bleChar.getValue()));
					}
					processSensors();
					scrollView.scrollTo(0, 0);
					scrollView.smoothScrollTo(0, 0);
					addConfigureButton();
				}
				catch (Exception e) {
					Application.showLongToast("Error parsing moteConfig");
					setResult(Activity.RESULT_CANCELED);
					finish();
				}
			}
		});
	}

	protected void addConfigureButton() {
		Button b = new Button(this);
		b.setText("Configure mote");
		b.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					configTask.writeConfigAndExit(moteConfigParser, sensorParsers);
					Application.showLongToast("Configuration sent");
					Intent intent = new Intent();
					intent.putExtra(DeviceConfigActivity.DEVICE_ADR, deviceAdr);
					setResult(RESULT_OK, intent);
					finish();
				}
				catch (Exception e) {
					Application.showLongToast(e.getMessage());
				}
			}
		});
		ll.addView(b);
	}

	protected void processSensors() {
		int counter = 1;
		for (final SensorParser parser : sensorParsers) {
			RelativeLayout rl = (RelativeLayout) getLayoutInflater().inflate(R.layout.sensor_container_config_layout, null);
			ImageView im = (ImageView) rl.findViewById(R.id.imageView);
			im.setImageResource(ITUConstants.findIconBySensorType(parser.getSensorType()));

			TextView number = (TextView) rl.findViewById(R.id.sensor_number);
			number.setText("" + counter++);

			Spinner spin = (Spinner) rl.findViewById(R.id.sensor_coordinate_spinner);
			ArrayAdapter<ITU_SENSOR_COORDINATE> arrayAdapter = new ArrayAdapter<ITUConstants.ITU_SENSOR_COORDINATE>(this, android.R.layout.simple_spinner_item,
					ITUConstants.ITU_SENSOR_COORDINATE_ARRAY);
			arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spin.setAdapter(arrayAdapter);
			spin.setSelection(parser.getCoordinateIndex(), false);
			spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					parser.setCoordinateIndex(position);
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {}
			});

			EditText samplingFreq = (EditText) rl.findViewById(R.id.sensor_sampl_freq);
			samplingFreq.setText(parser.getSampleFrequency() + "");
			samplingFreq.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

				@Override
				public void afterTextChanged(Editable s) {
					String num = s.toString();
					if (!"".equals(num)) {
						parser.setSampleFrequency(Integer.parseInt(num));
					} else {
						parser.setSampleFrequency(0);
					}
				}
			});
			ll.addView(rl);
		}
	}

	protected void processMoteConfig() {
		TableLayout table = (TableLayout) getLayoutInflater().inflate(R.layout.mote_config_layout, null);
		EditText deviceName = (EditText) table.findViewById(R.id.device_name);
		deviceName.setText(moteConfigParser.getDeviceName());
		deviceName.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				moteConfigParser.setDeviceName(s.toString());
			}
		});

		EditText deviceLocation = (EditText) table.findViewById(R.id.device_location);
		deviceLocation.setText(moteConfigParser.getLocationName());
		deviceLocation.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				moteConfigParser.setLocationName(s.toString());
			}
		});

		EditText freq = (EditText) table.findViewById(R.id.adv_freq_s);
		freq.setText(moteConfigParser.getAdvFreqInSec() + "");
		freq.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				String num = s.toString();
				if (!"".equals(num)) {
					moteConfigParser.setAdvFreqInSec(Integer.parseInt(num));
				} else {
					moteConfigParser.setAdvFreqInSec(0);
				}
			}
		});

		EditText buffPercen = (EditText) table.findViewById(R.id.percen_for_buffer_full);
		buffPercen.setText(moteConfigParser.getPercentageForBufferFull() + "");
		buffPercen.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				String num = s.toString();
				if (!"".equals(num)) {
					moteConfigParser.setPercentageForBufferFull(Integer.parseInt(num));
				} else {
					moteConfigParser.setPercentageForBufferFull(0);
				}
			}
		});
		
		EditText con_db = (EditText) table.findViewById(R.id.connected_transmit_db);
		con_db.setText(moteConfigParser.getConnectedTransmitPower() + "");
		con_db.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					String num = s.toString();
					int val = Integer.parseInt(num);
					moteConfigParser.setConnectedTransmitPower(val);
				}
				catch (NumberFormatException e) {
					moteConfigParser.setConnectedTransmitPower(100);
				}
				
			}
		});
		
		EditText non_con_db = (EditText) table.findViewById(R.id.non_connected_transmit_db);
		non_con_db.setText(moteConfigParser.getNonConnectedTransmitPower() + "");
		non_con_db.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					String num = s.toString();
					int val = Integer.parseInt(num);
					moteConfigParser.setNonConnectedTransmitPower(val);
				}
				catch (NumberFormatException e) {
					moteConfigParser.setNonConnectedTransmitPower(100);
				}
			}
		});
		
		ll.addView(table);
	}

	@Override
	public void onStatusUpdate(String status) {
		textView.setText(status);
	}
}
