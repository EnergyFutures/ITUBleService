package dk.itu.energyfutures.ble;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import dk.itu.energyfutures.ble.helpers.BluetoothHelper;
import dk.itu.energyfutures.ble.helpers.Devices;
import dk.itu.energyfutures.ble.helpers.GattAttributes;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.helpers.ITUConstants.ITU_SENSOR_CONFIG_TYPE;
import dk.itu.energyfutures.ble.helpers.ITUConstants.ITU_SENSOR_COORDINATE;
import dk.itu.energyfutures.ble.helpers.ITUConstants.ITU_SENSOR_TYPE;
import dk.itu.energyfutures.ble.packethandlers.AdvertisementPacket;
import dk.itu.energyfutures.ble.packethandlers.PacketBroadcaster;
import dk.itu.energyfutures.ble.packethandlers.PacketListListner;
import dk.itu.energyfutures.ble.task.EmptyBufferTask;
import dk.itu.energyfutures.ble.task.EmptyingBufferListner;
import dk.itu.energyfutures.ble.task.EmptyingBufferNotifer;
import dk.itu.energyfutures.ble.task.ParseJsonMoteTask;
import dk.itu.energyfutures.ble.task.TaskDoneListner;

public class BluetoothLEBackgroundService extends Service implements PacketBroadcaster, TaskDoneListner, EmptyingBufferNotifer, DataSinkFlagChangedListner {
	private final static String TAG = BluetoothLEBackgroundService.class.getSimpleName();

	protected static final String NEW_PACKET = "NEW_ADV_PACKET";

	private BluetoothManager btManager;
	private BluetoothAdapter btAdapter;
	private AtomicBoolean isRunnning = new AtomicBoolean(false);
	private ExecutorService executor;
	private Map<String, AdvertisementPacket> packets = new LinkedHashMap<String, AdvertisementPacket>();
	private Map<String, AdvertisementPacket> newBornPackets = new LinkedHashMap<String, AdvertisementPacket>();
	private Set<String> jsonAdr = new HashSet<String>();
	private Set<String> bleTasks = new HashSet<String>();
	private WakeLock wakeLock;
	private final long SLEEP_BT_CHIP_RESET = getResetTime();
	private AtomicBoolean moteFound = new AtomicBoolean(false);
	private AtomicBoolean isResetting = new AtomicBoolean(false);
	private long timeSinceLastBTReset = System.currentTimeMillis();
	//private boolean doOffLoading = getOffLoadingBoolean();
	private List<EmptyingBufferListner> emptyingBufferListners = new ArrayList<EmptyingBufferListner>();
	public static AtomicBoolean toggle = new AtomicBoolean(false);
	public static AtomicBoolean doReset = new AtomicBoolean(false);
	public static boolean isNexus = Devices.getDeviceName().equalsIgnoreCase("Asus Nexus 7 (2013)");
	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			if (isRunnning.get()) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							if (isRunnning.get()) {
								int[] result = BluetoothHelper.findIndexOfAdvertisementType(scanRecord, GattAttributes.SHORTENED_LOCAL_NAME);
								final String deviceName;
								String adr = device.getAddress();
								if (result == null || result.length != 2) {
									deviceName = "";
								} else {
									deviceName = BluetoothHelper.decodeLocalName(scanRecord, result[0], result[1]);
								}
								result = BluetoothHelper.findIndexOfAdvertisementType(scanRecord, GattAttributes.MANUFACTURER_SPECIFIC_DATA);
								if (result == null || result.length != 2) return;
								boolean isITUMote = BluetoothHelper.decodeManufacturerID(scanRecord, result[0]);
								if (isITUMote && isRunnning.get()) {
									// We know that the first 2 bytes are for the manufacturer ID... so skip them
									AdvertisementPacket packet = AdvertisementPacket.processITUAdvertisementValue(scanRecord, result[0] + 2, result[1] - 2, deviceName, device);
									if (packet == null) {
										Log.i(TAG, "NULL PACKET");
										return;
									}
									// Log.i(TAG, "Packet: " + packet);
									if (ITUConstants.ITU_SENSOR_TYPE.JSON.equals(packet.getSensorType())) {
										Log.v(TAG, "MULTI ACT");
										synchronized (jsonAdr) {
											if (!jsonAdr.contains(adr)) {
												synchronized (bleTasks) {
													if (!bleTasks.contains(adr) && bleTasks.size() == 0) {
														ParseJsonMoteTask task = new ParseJsonMoteTask();
														task.setDevice(packet.getDevice());
														task.setContext(getApplicationContext());
														bleTasks.add(adr);
														task.registerTaskDoneListner(BluetoothLEBackgroundService.this);
														executor.execute(task);
														jsonAdr.add(adr);
													}
												}
											}
										}
										packet = new AdvertisementPacket();
										packet.setId("50000");
										packet.setBufferLevel(0);
										packet.setBufferNeedsCleaning(false);
										packet.setBatteryLevel(100);
										packet.setCoordinate(ITU_SENSOR_COORDINATE.LOCATION_IN_SOMEWHERE);
										packet.setDevice(device);
										packet.setDeviceName(deviceName);
										packet.setLocation("4D21");
										packet.setSensorConfigType(ITU_SENSOR_CONFIG_TYPE.ACTUATOR_TYPE);
										packet.setSensorType(ITU_SENSOR_TYPE.JSON);
										packet.setValue(toggle.get() ? 1 : 0);
										packet.timeStamp = new Date();

									}
									if("NEWBORN".equals(deviceName)){
										newBornPackets.put(packet.getDeviceAdr(), packet);
										for (PacketListListner listner : newBornlistners) {
											listner.newPacketArrived(packet);
										}
										moteFound.set(true);
										Log.v(TAG, "NEWBORN Packet id: " + packet.getId());
										return;
									}
									packets.put(packet.getId(), packet);
									for (PacketListListner listner : listners) {
										listner.newPacketArrived(packet);
									}
									moteFound.set(true);
									Log.v(TAG, "Packet id: " + packet.getId());
									if (packet.isBufferNeedsCleaning() && isRunnning.get() && Application.isDataSink()) {
										Log.i(TAG, "Packet: " + packet);
										if(!Application.isConnectedToInternet()){
											Log.i(TAG, "No internet!");
											return;
										}
										synchronized (bleTasks) {
											if (!bleTasks.contains(adr) && bleTasks.size() == 0) {
												EmptyBufferTask task = new EmptyBufferTask();
												task.setPacket(packet);
												task.setContext(getApplicationContext());
												bleTasks.add(adr);
												task.registerTaskDoneListner(BluetoothLEBackgroundService.this);
												executor.execute(task);
												Application.emptyingBuffer = true;
												for (EmptyingBufferListner listner : emptyingBufferListners) {
													listner.emptyingBufferStateChanged();
												}
											}
										}
									}

								}
							}
						}
						catch (Exception e) {
							e.printStackTrace();
							Log.e(TAG,"ERROR: " +e.getMessage());
						}
					}
				});
			}
		}
	};

	private BroadcastReceiver networkChangeReceiver;

	@Override
	public void onCreate() {
		try {
			takeOff();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
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

	private void takeOff() throws Exception {
		if (!isRunnning.get()) {
			isRunnning.set(true);
			if (Application.isDataSink()) {
				enableDataSinkFeatures();
			}
			Application.instance.registerDataSinkFlagChangedListner(this);
			if (initialize()) {
				executor = Executors.newFixedThreadPool(6);
				executor.execute(new Runnable() {
					private long timeSinceLastScanReset;
					private long timeSinceLastPause = System.currentTimeMillis();
					int counter = 0;
					@Override
					public void run() {
						String myTag = "Ble scanner thread";
						Log.v(myTag, "Starting fresh");
						while (isRunnning.get() || isResetting.get()) {
							try {
								while(isResetting.get() || doReset.get()){
									Log.v(myTag, "Found a reset flag... sleeping");
									Thread.sleep(1000);
								}
								counter = 0;
								while (!btAdapter.startLeScan(mLeScanCallback)) {
									Log.v(myTag, "Could not start le-scan... sleeping");
									btAdapter.stopLeScan(mLeScanCallback);
									if (counter++ == 4) {
										Log.v(myTag, "Could not start le-scan... breaking out");
										doReset.set(true);
										break;
									}
									Thread.sleep(1000);
								}
								if(doReset.get()){
									Log.v(myTag, "Found doReset flag... continue");
									continue;
								}
								timeSinceLastScanReset = System.currentTimeMillis();
								while (((System.currentTimeMillis() - timeSinceLastScanReset) < getMilisecFromSec(2)) || (!moteFound.get() && (timeSinceLastScanReset - timeSinceLastPause < getMilisecFromSec(10)))) {
									if (!isRunnning.get()) {
										break;
									}
									Thread.sleep(1000);
								}
								moteFound.set(false);
								btAdapter.stopLeScan(mLeScanCallback);
								if(!isNexus && ((System.currentTimeMillis() - timeSinceLastPause) >= getMilisecFromSec(10+counter)) && isRunnning.get()){
									Log.v(myTag, "Sleeping for pause");
									Thread.sleep(getMilisecFromSec(30));
									timeSinceLastPause = System.currentTimeMillis();
								}
							}
							catch (Throwable e) {
								if (!(e instanceof InterruptedException)) {
									Log.e(TAG, "Error with scan thread or it has been killed", e);
								}
							}
						}
					}
				});
				executor.execute(new Runnable() {
					long sleepBetweenCleaning = getMilisecFromSec(120);
					@Override
					public void run() {
						while (isRunnning.get() || isResetting.get()) {
							try {
								Thread.sleep(sleepBetweenCleaning);// 1 min
								List<AdvertisementPacket> deprecated = new ArrayList<AdvertisementPacket>();
								long time = System.currentTimeMillis();
								for (AdvertisementPacket packet : packets.values()) {
									if (time - packet.getTimeStamp().getTime() >= sleepBetweenCleaning) {
										deprecated.add(packet);
									}
								}
								for (AdvertisementPacket packet : newBornPackets.values()) {
									if (time - packet.getTimeStamp().getTime() >= sleepBetweenCleaning) {
										deprecated.add(packet);
									}
								}
								if (deprecated.size() > 0) {
									for (AdvertisementPacket packet : deprecated) {
										packets.remove(packet.getId());
										newBornPackets.remove(packet.getDeviceAdr());
									}
									for (PacketListListner listner : listners) {
										listner.PacketsDeprecated(deprecated);
									}
								}
							}
							catch (Throwable e) {
								if (!(e instanceof InterruptedException)) {
									Log.e(TAG, "Error with deprecator thread or it has been killed", e);
								}
							}
						}
					}
				});
				executor.execute(new Runnable() {
					@Override
					public void run() {
						while (isRunnning.get() || isResetting.get()) {
							try {
								if (!doReset.get() && (System.currentTimeMillis() - timeSinceLastBTReset < SLEEP_BT_CHIP_RESET)) {
									while(System.currentTimeMillis() - timeSinceLastBTReset < SLEEP_BT_CHIP_RESET){
										Thread.sleep(getMilisecFromSec(5));
										if(doReset.get()){
											break;
										}
									}
								} else {
									Thread.sleep(1000);
								}
								if ((Application.isDataSink() && bleTasks.size() == 0) || doReset.get()) {
									if (!isRunnning.get()) {
										break;
									}
									synchronized (bleTasks) {
										if (bleTasks.size() == 0) {
											isResetting.set(true);
											doReset.set(false);
											isRunnning.set(false);
											btAdapter.stopLeScan(mLeScanCallback);
											Application.showLongToastOnUI("RESETTING BT ADAPTOR");
											Log.i(TAG, "RESETTING BT ADAPTOR");
											btAdapter.disable();
											Thread.sleep(getMilisecFromSec(10)); // 5 sec
											while (!btAdapter.enable()) {
												Thread.sleep(1000);
												Log.i(TAG, "ERROR RESETTING ADAPTOR..sleeping");
											}
											isRunnning.set(true);
											timeSinceLastBTReset = System.currentTimeMillis();
											isResetting.set(false);
										}
									}
								}
							}
							catch (Throwable e) {
								if (!(e instanceof InterruptedException)) {
									Log.e(TAG, "Error with resetter thread or it has been killed", e);
								}
							}
						}
					}
				});
				Application.showLongToast("BLE SERVICE STARTED :0)");
			} else {
				isRunnning.set(false);
				Log.e(TAG, "could not initiate BT");
				Application.showLongToast("COULD NOT START BLE :0(");
			}
		} else {
			Application.showLongToast("BLE SERVICE ALREADY RUNNING!");
		}
	}

	public void enableDataSinkFeatures() {
		if(networkChangeReceiver == null){
			networkChangeReceiver = new NetworkChangeReceiver();
		}
		registerReceiver(networkChangeReceiver,new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
		if(wakeLock == null){
			PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		}
		wakeLock.acquire();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		shutdownAndCleanup();
		Log.v(TAG, "onDestroyCommand");
		Application.showLongToast("BLE SERVICE STOPPED");
	}

	private void shutdownAndCleanup() {
		isRunnning.set(false);
		disableDataSinkFeatures();
		Application.instance.unRegisterDataSinkFlagChangedListner(this);
		btAdapter.stopLeScan(mLeScanCallback);
		if (executor != null) {
			executor.shutdownNow();
			try {
				executor.awaitTermination(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {}
		}
		Application.emptyingBuffer = false;
	}

	private void disableDataSinkFeatures() {
		if(networkChangeReceiver != null){
			unregisterReceiver(networkChangeReceiver);
			networkChangeReceiver = null;
		}
		if (wakeLock != null) {
			wakeLock.release();
		}
	}

	// BINDER PART

	private final IBinder mBinder = new LocalBinder();

	private Set<PacketListListner> listners = new HashSet<PacketListListner>();
	private Set<PacketListListner> newBornlistners = new HashSet<PacketListListner>();

	public class LocalBinder extends Binder {
		public BluetoothLEBackgroundService getService() {
			return BluetoothLEBackgroundService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public Map<String, AdvertisementPacket> getPackets() {
		return packets;
	}
	
	@Override
	public Map<String, AdvertisementPacket> getNewBornPackets() {
		return newBornPackets;
	}

	@Override
	public void onTaskDone(String adr) {
		synchronized (bleTasks) {
			bleTasks.remove(adr);
			Application.emptyingBuffer = false;
			for (EmptyingBufferListner listner : emptyingBufferListners) {
				listner.emptyingBufferStateChanged();
			}
		}
	}

	public void addTaskAdr(String adr) {
		synchronized (bleTasks) {
			bleTasks.add(adr);
		}
	}

	public void removeTaskAdr(String adr) {
		synchronized (bleTasks) {
			bleTasks.remove(adr);
		}
	}

	public static long getResetTime() {
		String deviceName = Devices.getDeviceName();
		if (deviceName.equalsIgnoreCase("Asus Nexus 7 (2013)")) {
			Log.v(TAG, "Nexus device found");
			return 3 * 60 * 1000; //3 min
		}
		return 15 * 60 * 1000; //15 min
	}

	@Override
	public void registerEmptypingListner(EmptyingBufferListner listner) {
		this.emptyingBufferListners.add(listner);
	}

	@Override
	public void unregisterEmptypingListner(EmptyingBufferListner listner) {
		this.emptyingBufferListners.remove(listner);
	}
	
	@Override
	public void registerPacketListner(PacketListListner listner) {
		listners.add(listner);
	}

	@Override
	public void removePacketListner(PacketListListner listner) {
		listners.remove(listner);
	}
	
	@Override
	public void registerNewBornPacketListner(PacketListListner listner) {
		newBornlistners.add(listner);
	}

	@Override
	public void removeNewBornPacketListner(PacketListListner listner) {
		newBornlistners.remove(listner);
	}

	@Override
	public void onDataSinkFlagChanged(boolean state) {
		if(state){
			enableDataSinkFeatures();
		}else{
			disableDataSinkFeatures();
		}
	}
	
	public static long getMilisecFromSec(final double numberOfSeconds){
		return (long) (numberOfSeconds * 1000);
	}
}
