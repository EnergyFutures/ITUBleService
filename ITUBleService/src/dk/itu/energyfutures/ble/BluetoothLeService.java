package dk.itu.energyfutures.ble;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import dk.itu.energyfutures.ble.helpers.BluetoothHelper;
import dk.itu.energyfutures.ble.helpers.GattAttributes;

public class BluetoothLeService extends Service {
	private final static String TAG = BluetoothLeService.class.getSimpleName();

	private static BluetoothManager btManager;
	private static BluetoothAdapter btAdapter;
	private static boolean isRunnning = false;
	private static ExecutorService gattExecutor;
	private List<BluetoothGatt> gattDevices = new ArrayList<BluetoothGatt>();
	private static AtomicBoolean isDoingStuff = new AtomicBoolean(false);

	public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
			try {
				String adr = gatt.getDevice().getAddress();
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					Log.i(TAG, "Connected to GATT server: " + adr);
					gattDevices.add(gatt);
					gattExecutor.submit(new Runnable() {
						@Override
						public void run() {
							waitForChange();
							gatt.discoverServices();
							isDoingStuff.set(true);
						}
					});
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					Log.i(TAG, "Disconnected from GATT server: " + adr);
					gattDevices.remove(gatt);
				}
			}
			finally {
				isDoingStuff.set(false);
			}
		}

		@Override
		public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
			String adr = gatt.getDevice().getAddress();
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.v(TAG, "Done discovering: " + adr);
				List<BluetoothGattService> services = gatt.getServices();
				for (BluetoothGattService btService : services) {
					final BluetoothGattCharacteristic configChar = btService.getCharacteristic(GattAttributes.BLE_UUID_ITU_MEASUREMENT_CONFIG_CHAR);
					if (configChar != null) {
						gattExecutor.submit(new Runnable() {
							@Override
							public void run() {
								waitForChange();
								configChar.setValue(new byte[] { 0x08 });
								gatt.writeCharacteristic(configChar);
								isDoingStuff.set(true);
							}
						});
					}
					BluetoothGattCharacteristic valueChar = btService.getCharacteristic(GattAttributes.BLE_UUID_ITU_MEASUREMENT_VALUE_CHAR);
					if (valueChar != null) {
						setCharacteristicNotification(gatt, valueChar);
					}
				}
			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status + " for adr: " + adr);
			}
			isDoingStuff.set(false);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.e(TAG, "onCharacteristicRead received: " + status + " for adr: " + gatt.getDevice().getAddress());
			}
			isDoingStuff.set(false);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			// Log.v(TAG, "onCharacteristicChanged received for adr: " + gatt.getDevice().getAddress());
			processITUMeasurementValue(characteristic);
			// isDoingStuff.set(false);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());
			isDoingStuff.set(false);
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.v(TAG, "onDescriptorRead received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + descriptor.getUuid());
			isDoingStuff.set(false);
		};

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.v(TAG, "onDescriptorWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + descriptor.getUuid());
			isDoingStuff.set(false);
		};

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			isDoingStuff.set(false);
		};

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			Log.v(TAG, "onReliableWriteCompleted received: " + status + " for adr: " + gatt.getDevice().getAddress());
			isDoingStuff.set(false);
		};
	};

	private void processITUMeasurementValue(BluetoothGattCharacteristic characteristic) {
		Log.v(TAG, "Measurement received: " + BluetoothHelper.bytesToHex(characteristic.getValue()));
		int offset = 1;
		double tempValue = BluetoothHelper.getIEEEFloatValue(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset));
		offset += 4;
		int id = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
		SMAPPoster.submitMeasurement(id, tempValue);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private boolean initialize() {
		if (btManager == null) {
			btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (btManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		btAdapter = btManager.getAdapter();
		if (btAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		if (btAdapter.isEnabled()) {
			return true;
		}
		return btAdapter.enable();
	}

	private void setCharacteristicNotification(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		gatt.setCharacteristicNotification(characteristic, true);
		final BluetoothGattDescriptor cccd = characteristic.getDescriptor(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
		if (cccd != null) {
			gattExecutor.submit(new Runnable() {
				@Override
				public void run() {
					waitForChange();
					cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					gatt.writeDescriptor(cccd);
					isDoingStuff.set(true);
				}
			});
		}
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			final String deviceName = device.getName();
			if (deviceName != null && deviceName.length() > 0 && deviceName.contains("ITU")) {
				gattExecutor.execute(new Runnable() {
					@Override
					public void run() {
						waitForChange();
						device.connectGatt(getApplicationContext(), true, gattCallback);
					}
				});
			}
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "onStartCommand");
		takeOff();
		return START_STICKY;
	}

	private void takeOff() {
		if (!isRunnning) {
			if (initialize()) {
				// executorService = Executors.newFixedThreadPool(5);
				gattExecutor = Executors.newSingleThreadExecutor();
				Thread scanner = new Thread(new Runnable() {
					@Override
					public void run() {
						String myTag = "Ble scanner thread";
						while (true) {
							try {
								Log.v(myTag,"Starting scan");
								btAdapter.startLeScan(mLeScanCallback);
								Log.v(myTag,"Sleeping for 5");
								Thread.sleep(5*60*1000); //5 min
								Log.v(myTag,"Awake for 5");
								Log.v(myTag,"Stopping scan");
								btAdapter.stopLeScan(mLeScanCallback);
								Log.v(myTag,"Sleeping for 25");
								Thread.sleep(25*60*1000); //25 min
								Log.v(myTag,"Awake for 25");
							}
							catch (Exception e) {
								Log.e(TAG, "Error with scan thread or it has been killed",e);
							}
						}
					}
				});
				scanner.setDaemon(true);
				scanner.start();
				isRunnning = true;
				Toast.makeText(getApplicationContext(), "BLE SERVICE STARTED :0)", Toast.LENGTH_LONG).show();
			} else {
				Log.e(TAG, "could not initiate BT");
				Toast.makeText(getApplicationContext(), "COULD NO START BLE :0(", Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(getApplicationContext(), "BLE SERVICE ALREADY RUNNING!", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (gattDevices.size() > 0) {
			for (BluetoothGatt gatt : gattDevices) {
				gatt.close();
			}
			gattDevices.clear();
		}
		isRunnning = false;
		if (gattExecutor != null) {
			gattExecutor.shutdown();
		}
		Log.v(TAG, "onDestroyCommand");
	}

	private void waitForChange() {
		try {
			int counter = 0;
			while (isDoingStuff.get()) {
				Thread.sleep(200);
				counter += 200;
				if (counter >= 45000) { // 45 sec
					isDoingStuff.set(false);
					break;
				}
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Error while sleeping in waitForChange");
			e.printStackTrace();
		}
	}
}
