package dk.itu.energyfutures.ble;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import android.drm.DrmStore.RightsStatus;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import dk.itu.energyfutures.ble.helpers.BluetoothHelper;
import dk.itu.energyfutures.ble.helpers.GattAttributes;
import dk.itu.energyfutures.ble.helpers.GzipAndJsonParser;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.helpers.ITUConstants.ITU_MOTE_TYPE;

public class BluetoothLeService extends Service implements NewPacketBroadcaster{
	private final static String TAG = BluetoothLeService.class.getSimpleName();

	protected static final String NEW_PACKET = "NEW_ADV_PACKET";

	private static BluetoothManager btManager;
	private static BluetoothAdapter btAdapter;
	private static boolean isRunnning = false;
	private static ExecutorService gattExecutor;
	private List<BluetoothGatt> gattDevices = new ArrayList<BluetoothGatt>();
	private static AtomicBoolean isDoingStuff = new AtomicBoolean(false);
	private List<Byte> json = new ArrayList<Byte>();

	// public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	// public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	// public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	// public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	// public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
	private boolean moteFound;

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
			try {
				String adr = gatt.getDevice().getAddress();
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					Log.i(TAG, "Connected to GATT server: " + adr);
					gattDevices.add(gatt);
					if(isRunnning){
						gattExecutor.submit(new Runnable() {
							@Override
							public void run() {
								waitForChange();
								isDoingStuff.set(true);
								gatt.discoverServices();								
							}
						});
					}					
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
				BluetoothGattService service = gatt.getService(ITUConstants.BLE_UUID_ITU_ACTUATOR_SERVICE);
				if (service != null) {
					final BluetoothGattCharacteristic jsonChar = service.getCharacteristic(ITUConstants.BLE_UUID_ITU_ACTUATOR_JSON_CHAR);
					if (jsonChar != null && isRunnning) {
						gattExecutor.submit(new Runnable() {
							@Override
							public void run() {
								waitForChange();
								isDoingStuff.set(true);
								gatt.readCharacteristic(jsonChar);						
							}
						});
					}
				}

			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status + " for adr: " + adr);
			}
			isDoingStuff.set(false);
		}

		@Override
		public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				byte[] bytes = characteristic.getValue();
				for(byte b: bytes){
					json.add(b);
				}
				if(bytes.length == 600 ){
					if(isRunnning){
						gattExecutor.submit(new Runnable() {
							@Override
							public void run() {
								waitForChange();
								isDoingStuff.set(true);
								gatt.readCharacteristic(characteristic);						
							}
						});
					}					
				}else{
					try {
						String result = GzipAndJsonParser.unzipAndParseByteList(json);
						json.clear();
						Log.i(TAG,result);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			isDoingStuff.set(false);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			
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

	/*private void setCharacteristicNotification(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		gatt.setCharacteristicNotification(characteristic, true);
		final BluetoothGattDescriptor cccd = characteristic.getDescriptor(ITUConstants.CLIENT_CHARACTERISTIC_CONFIG);
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
	}*/

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			gattExecutor.execute(new Runnable() {

				@Override
				public void run() {
					int[] result = BluetoothHelper.findIndexOfAdvertisementType(scanRecord, GattAttributes.SHORTENED_LOCAL_NAME);
					if (result == null || result.length != 2) return;
					final String deviceName = BluetoothHelper.decodeLocalName(scanRecord, result[0], result[1]);
					result = BluetoothHelper.findIndexOfAdvertisementType(scanRecord, GattAttributes.MANUFACTURER_SPECIFIC_DATA);
					if (result == null || result.length != 2) return;
					boolean isITUMote = BluetoothHelper.decodeManufacturerID(scanRecord, result[0]);
					if (deviceName != null && deviceName.length() > 0 && isITUMote) {
						// We know that the first 2 bytes are for the manufacturer ID... so skip them
						AdvertisementPacket packet = AdvertisementPacket.processITUAdvertisementValue(scanRecord, result[0] + 2, result[1] - 2, deviceName);
						Log.i(TAG,"Type: " + packet.getSensorType() + " value: " + packet.getValue() + " buff_full: " + packet.isBufferFull());
						if (packet.getMoteType() == ITU_MOTE_TYPE.BLE_UUID_ITU_MOTE_ACTUATOR_TYPE) {
							waitForChange();
							//device.connectGatt(getApplicationContext(), false, gattCallback);
						}
						for(NewPacketListner listner: listners){
							listner.newPacketArrived(packet);
						}
						moteFound = true;
					}
				}
			});

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
				// gattExecutor = Executors.newFixedThreadPool(5);
				gattExecutor = Executors.newSingleThreadExecutor();
				Thread scanner = new Thread(new Runnable() {

					@Override
					public void run() {
						String myTag = "Ble scanner thread";
						int count = 0;
						while (true) {
							try {
								Log.v(myTag, "Starting scan");
								btAdapter.startLeScan(mLeScanCallback);
								Log.v(myTag, "Sleeping while scanning");
								while (!moteFound) {
									Thread.sleep(100);
								}
								Log.v(myTag, "mote found... we should reset scanning");
								moteFound = false;
								btAdapter.stopLeScan(mLeScanCallback);
								if (count++ > 9000) { // around 15 min
									count = 0;
									btAdapter.disable();
									Thread.sleep(5000); // 5 sec
									btAdapter.enable();
								}
							}
							catch (Exception e) {
								Log.e(TAG, "Error with scan thread or it has been killed", e);
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
				Toast.makeText(getApplicationContext(), "COULD NOT START BLE :0(", Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(getApplicationContext(), "BLE SERVICE ALREADY RUNNING!", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isRunnning = false;	
		if (gattExecutor != null) {
			gattExecutor.shutdown();
		}
		if (gattDevices.size() > 0) {
			for (BluetoothGatt gatt : gattDevices) {
				gatt.close();
			}
			gattDevices.clear();
		}		
		Log.v(TAG, "onDestroyCommand");
		Toast.makeText(getApplicationContext(), "BLE SERVICE STOOOOPED", Toast.LENGTH_LONG).show();
	}

	private void waitForChange() {
		try {
			int counter = 0;
			while (isDoingStuff.get()) {
				Thread.sleep(50);
				counter += 50;
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

	// BINDER PART

	private final IBinder mBinder = new LocalBinder();

	private List<NewPacketListner> listners = new ArrayList<NewPacketListner>();

	public class LocalBinder extends Binder {
		BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		takeOff();
		return mBinder;
	}

	@Override
	public void registerListner(NewPacketListner listner) {
		listners.add(listner);
	}

	@Override
	public void removeListner(NewPacketListner listner) {
		listners.remove(listner);
	}
}
