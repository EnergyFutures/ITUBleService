package dk.itu.energyfutures.ble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import dk.itu.energyfutures.ble.helpers.BluetoothHelper;
import dk.itu.energyfutures.ble.helpers.GattAttributes;
import dk.itu.energyfutures.ble.helpers.GzipAndJsonParser;
import dk.itu.energyfutures.ble.task.EmptyBufferTask;

public class BluetoothLeService extends Service implements NewPacketBroadcaster, DoneEmptyingBufferListner {
	private final static String TAG = BluetoothLeService.class.getSimpleName();

	protected static final String NEW_PACKET = "NEW_ADV_PACKET";

	private BluetoothManager btManager;
	private BluetoothAdapter btAdapter;
	private AtomicBoolean isRunnning = new AtomicBoolean(false);
	private AtomicLong timeOfLastPacketReceived = new AtomicLong();
	private ExecutorService gattExecutor;
	private Map<String, List<BluetoothGattService>> devices = new HashMap<String, List<BluetoothGattService>>();
	// private static AtomicBoolean isDoingStuff = new AtomicBoolean(false);
	private List<Byte> json = new ArrayList<Byte>();
	private Set<AdvertisementPacket> packets = new HashSet<AdvertisementPacket>();
	private Map<String, Long> timeOfLastDiscoveryCheckMap = new HashMap<String, Long>();
	private Set<String> emptyBufferTasks = new HashSet<String>();
	// public static BluetoothLeService instance;

	// public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	// public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	// public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	// public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	// public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
	public static final long TIME_TO_RETRY_DISCOVERY = 2 * 60 * 1000; // min
	private static AtomicBoolean moteFound = new AtomicBoolean(false);

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
			String adr = gatt.getDevice().getAddress();
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.v(TAG, "Connected to GATT server: " + adr);
			} else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
				Log.v(TAG, "Disconnecting to GATT server: " + adr);
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.v(TAG, "Disconnected to GATT server: " + adr);
			} else if (newState == BluetoothProfile.STATE_CONNECTING) {
				Log.v(TAG, "Connecting to GATT server: " + adr);
			}
		}

		@Override
		public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
			String adr = gatt.getDevice().getAddress();
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.v(TAG, "Done discovering: " + adr);
			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status + " for adr: " + adr);
			}
		}

		@Override
		public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				byte[] bytes = characteristic.getValue();
				for (byte b : bytes) {
					json.add(b);
				}
				if (bytes.length == 600) {
					if (isRunnning.get()) {
						gatt.readCharacteristic(characteristic);
					}
				} else {
					gattExecutor.execute(new Runnable() {
						@Override
						public void run() {
							try {
								String result = GzipAndJsonParser.unzipAndParseByteList(json);
								json.clear();
								Log.i(TAG, result);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Log.v(TAG, "onCharacteristicChanged adr: " + gatt.getDevice().getAddress());
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.v(TAG, "onDescriptorRead received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + descriptor.getUuid());
		};

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.v(TAG, "onDescriptorWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + descriptor.getUuid());
		};

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {};

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			Log.v(TAG, "onReliableWriteCompleted received: " + status + " for adr: " + gatt.getDevice().getAddress());
		};
	};

	/*
	 * private void setCharacteristicNotification(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) { gatt.setCharacteristicNotification(characteristic, true); final
	 * BluetoothGattDescriptor cccd = characteristic.getDescriptor(ITUConstants.CLIENT_CHARACTERISTIC_CONFIG); if (cccd != null) { gattExecutor.submit(new Runnable() {
	 * 
	 * @Override public void run() { waitForChange(); cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); gatt.writeDescriptor(cccd); isDoingStuff.set(true); } }); } }
	 */

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			if (isRunnning.get()) {
				gattExecutor.execute(new Runnable() {
					@Override
					public void run() {
						if (isRunnning.get()) {
							int[] result = BluetoothHelper.findIndexOfAdvertisementType(scanRecord, GattAttributes.SHORTENED_LOCAL_NAME);

							if (result == null || result.length != 2) return;
							final String deviceName = BluetoothHelper.decodeLocalName(scanRecord, result[0], result[1]);
							result = BluetoothHelper.findIndexOfAdvertisementType(scanRecord, GattAttributes.MANUFACTURER_SPECIFIC_DATA);
							if (result == null || result.length != 2) return;
							boolean isITUMote = BluetoothHelper.decodeManufacturerID(scanRecord, result[0]);
							if (deviceName != null && deviceName.length() > 0 && isITUMote) {
								// We know that the first 2 bytes are for the manufacturer ID... so skip them
								AdvertisementPacket packet = AdvertisementPacket.processITUAdvertisementValue(scanRecord, result[0] + 2, result[1] - 2, deviceName, device);
								packets.remove(packet);
								packets.add(packet);
								timeOfLastPacketReceived.set(System.currentTimeMillis());
								for (NewPacketListner listner : listners) {
									listner.newPacketArrived(packet);
								}
								moteFound.set(true);
								String adr = device.getAddress();
								Log.v(TAG, "Buffer state: " + packet.isBufferFull() + " adr: " + adr);
								if (packet.isBufferFull()) {
									synchronized (emptyBufferTasks) {
										if (!emptyBufferTasks.contains(adr)) {
											EmptyBufferTask task = new EmptyBufferTask();
											task.setDevice(packet.getDevice());
											task.setContext(getApplicationContext());
											emptyBufferTasks.add(adr);
											task.registerDoneEmptyingBufferListner(BluetoothLeService.this);
											gattExecutor.execute(task);
										}
									}
								}
							}
						}
					}
				});
			}
		}
	};

	@Override
	public void onCreate() {
		takeOff();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
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

	private void takeOff() {
		if (!isRunnning.get()) {
			isRunnning.set(true);
			if (initialize()) {
				// instance = this;
				gattExecutor = Executors.newFixedThreadPool(3);
				// gattExecutor = Executors.newSingleThreadExecutor();
				Thread scanner = new Thread(new Runnable() {
					private long timeSinceLastReset = System.currentTimeMillis();

					@Override
					public void run() {
						String myTag = "Ble scanner thread";
						Log.v(myTag, "Starting fresh");
						while (true) {
							try {
								if (!isRunnning.get()) {
									break;
								}
								Log.v(myTag, "Starting scan");
								while (!btAdapter.startLeScan(mLeScanCallback)) {
									Thread.sleep(250);
								}
								Log.v(myTag, "Sleeping while scanning");
								while (!moteFound.get() || ((System.currentTimeMillis() - timeOfLastPacketReceived.get()) < 1000)) {
									if (!isRunnning.get()) {
										break;
									}
									Thread.sleep(100);
								}
								Log.v(myTag, "mote found... we should reset scanning");
								moteFound.set(false);
								btAdapter.stopLeScan(mLeScanCallback);
								// We need to check if we are in data-sink mode
								if (System.currentTimeMillis() - timeSinceLastReset >= 20 * 60 * 1000) { // around 60 min
									if (!isRunnning.get()) {
										break;
									}
									timeSinceLastReset = System.currentTimeMillis();
									shutdownAndCleanup(false);
									btAdapter.disable();
									Thread.sleep(5000); // 5 sec
									btAdapter.enable();
									gattExecutor = Executors.newFixedThreadPool(3);
									isRunnning.set(true);
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
				Toast.makeText(getApplicationContext(), "BLE SERVICE STARTED :0)", Toast.LENGTH_LONG).show();
			} else {
				isRunnning.set(false);
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
		shutdownAndCleanup(true);
		Log.v(TAG, "onDestroyCommand");
		Toast.makeText(getApplicationContext(), "BLE SERVICE STOPPED", Toast.LENGTH_LONG).show();
	}

	private void shutdownAndCleanup(boolean cleanGatt) {
		isRunnning.set(false);
		btAdapter.stopLeScan(mLeScanCallback);
		if (gattExecutor != null) {
			gattExecutor.shutdown();
			try {
				gattExecutor.awaitTermination(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {}
		}
	}

	/*
	 * private void waitForChange() { try { int counter = 0; while (isDoingStuff.get()) { Thread.sleep(50); counter += 50; if (counter >= 45000) { // 45 sec isDoingStuff.set(false); break; } } } catch
	 * (Exception e) { Log.e(TAG, "Error while sleeping in waitForChange"); e.printStackTrace(); } }
	 */

	// BINDER PART

	private final IBinder mBinder = new LocalBinder();

	private Set<NewPacketListner> listners = new HashSet<NewPacketListner>();

	public class LocalBinder extends Binder {
		BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
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

	@Override
	public Set<AdvertisementPacket> getPackets() {
		return packets;
	}

	@Override
	public void onDoneEmptyingBuffer(String adr) {
		synchronized (emptyBufferTasks) {
			emptyBufferTasks.remove(adr);
		}
	}
}
