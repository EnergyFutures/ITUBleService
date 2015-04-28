package dk.itu.energyfutures.ble;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import dk.itu.energyfutures.ble.helpers.BluetoothHelper;
import dk.itu.energyfutures.ble.helpers.GattAttributes;
import dk.itu.energyfutures.ble.packethandlers.PacketBroadcaster;
import dk.itu.energyfutures.ble.packethandlers.PacketListListner;
import dk.itu.energyfutures.ble.task.DoneEmptyingBufferListner;
import dk.itu.energyfutures.ble.task.EmptyBufferTask;

public class BluetoothLEBackgroundService extends Service implements PacketBroadcaster, DoneEmptyingBufferListner {
	private final static String TAG = BluetoothLEBackgroundService.class.getSimpleName();

	protected static final String NEW_PACKET = "NEW_ADV_PACKET";

	private BluetoothManager btManager;
	private BluetoothAdapter btAdapter;
	private AtomicBoolean isRunnning = new AtomicBoolean(false);
	private ExecutorService executor;
	private List<Byte> json = new ArrayList<Byte>();
	private Map<String,AdvertisementPacket> packets = new LinkedHashMap<String,AdvertisementPacket>();
	private Set<String> adrHits = new HashSet<String>();
	private Set<String> bleTasks = new HashSet<String>();
	private FileWriter fileWriter;
	private WakeLock wakeLock;
	private static final long WAIT_BEFORE_BT_CHIP_RESET = 120 * 60 * 1000; //2 HOURS
	public static final long TIME_TO_RETRY_DISCOVERY = 2 * 60 * 1000; // 2 min
	private static AtomicBoolean moteFound = new AtomicBoolean(false);
	
	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			if (isRunnning.get()) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						if (isRunnning.get()) {
							int[] result = BluetoothHelper.findIndexOfAdvertisementType(scanRecord, GattAttributes.SHORTENED_LOCAL_NAME);
							final String deviceName;
							if (result == null || result.length != 2){
								deviceName = "";
							}else{
								deviceName = BluetoothHelper.decodeLocalName(scanRecord, result[0], result[1]);
							}							
							result = BluetoothHelper.findIndexOfAdvertisementType(scanRecord, GattAttributes.MANUFACTURER_SPECIFIC_DATA);
							if (result == null || result.length != 2) return;
							boolean isITUMote = BluetoothHelper.decodeManufacturerID(scanRecord, result[0]);
							if (isITUMote && isRunnning.get()) {
								// We know that the first 2 bytes are for the manufacturer ID... so skip them
								AdvertisementPacket packet = AdvertisementPacket.processITUAdvertisementValue(scanRecord, result[0] + 2, result[1] - 2, deviceName, device);
								packets.put(packet.getId(),packet);
								for (PacketListListner listner : listners) {
									listner.newPacketArrived(packet);
								}
								String adr = device.getAddress();
								synchronized (adrHits) {
									if(!adrHits.contains(adr)){
										adrHits.add(adr);
										moteFound.set(true);
									}
								}	
								Log.v(TAG, "Packet: " + packet.getId());
								if (packet.isBufferNeedsCleaning() && isRunnning.get()) {
									Log.i(TAG, "Packet: " + packet);
									synchronized (bleTasks) {
										if (!bleTasks.contains(adr)) {
											if(Application.isDataSink()){
												try {
													fileWriter.append("Off-loading packet: " + packet+"\n" );
													fileWriter.flush();
												}
												catch (IOException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
											}
											EmptyBufferTask task = new EmptyBufferTask();
											task.setDevice(packet.getDevice());
											task.setContext(getApplicationContext());
											bleTasks.add(adr);
											task.registerDoneEmptyingBufferListner(BluetoothLEBackgroundService.this);
											executor.execute(task);
										}
										bleTasks.notify();
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
			if(Application.isDataSink()){
				String path = getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
				File file = new File(path + "/mumilog.txt");
				System.out.println("MUMI: " + file.getAbsolutePath());
				fileWriter = new FileWriter(file);	
				fileWriter.append("Starting: " + System.currentTimeMillis() + "\n");
				fileWriter.flush();
				PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
				wakeLock.acquire();
			}			
			if (initialize()) {
				executor = Executors.newFixedThreadPool(5);
				// gattExecutor = Executors.newSingleThreadExecutor();
				executor.execute(new Runnable() {
					private long timeSinceLastReset = System.currentTimeMillis();
					private long timeSinceLastScanReset;
					@Override
					public void run() {
						String myTag = "Ble scanner thread";
						Log.v(myTag, "Starting fresh");
						while (isRunnning.get()) {
							try {
								//Log.v(myTag, "Starting scan");
								while (!btAdapter.startLeScan(mLeScanCallback)) {
									Thread.sleep(250);
								}
								//Log.v(myTag, "Sleeping while scanning");
								timeSinceLastScanReset = System.currentTimeMillis();
								synchronized (adrHits) {
									adrHits.clear();
								}	
								while (!moteFound.get() || ((System.currentTimeMillis() - timeSinceLastScanReset) < 1000)) {
									if (!isRunnning.get()) {
										break;
									}
									Thread.sleep(100);
								}
								moteFound.set(false);
								btAdapter.stopLeScan(mLeScanCallback);
								
								if (Application.isDataSink() && (System.currentTimeMillis() - timeSinceLastReset >= WAIT_BEFORE_BT_CHIP_RESET) && bleTasks.size() == 0) { 
									if (!isRunnning.get()) {
										break;
									}
									synchronized (bleTasks) {
										if(bleTasks.size() == 0){
											Application.showLongToastOnUI("RESETTING BT ADAPTOR");
											timeSinceLastReset = System.currentTimeMillis();
											btAdapter.disable();
											Thread.sleep(5000); // 5 sec
											btAdapter.enable();
											isRunnning.set(true);
										}
									}
								}
							}
							catch (Exception e) {
								if(!(e instanceof InterruptedException)){
									Log.e(TAG, "Error with scan thread or it has been killed", e);
								}
								
							}
						}
					}
				});
				executor.execute(new Runnable() {
					@Override
					public void run() {
						while(isRunnning.get()){
							try {
								Thread.sleep(30 * 1000);//30 sec
								//Application.showShortToastOnUI("Cleaning old packets");
								List<AdvertisementPacket> deprecated = new ArrayList<AdvertisementPacket>();
								long time = System.currentTimeMillis();
								for(AdvertisementPacket packet : packets.values()){
									if(time - packet.getTimeStamp().getTime() >= 60000){
										deprecated.add(packet);
									}
								}
								if(deprecated.size() > 0){
									for(AdvertisementPacket packet : deprecated){
										packets.remove(packet);
									}
									for (PacketListListner listner : listners) {
										listner.PacketsDeprecated(deprecated);
									}
								}
							}
							catch (Exception e) {
								if(!(e instanceof InterruptedException)){
									Log.e(TAG, "Error with deprecator thread or it has been killed", e);
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
			Application.showLongToast( "BLE SERVICE ALREADY RUNNING!");
		}
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
		if(Application.isDataSink()){
			wakeLock.release();
		}
		btAdapter.stopLeScan(mLeScanCallback);
		if (executor != null) {
			executor.shutdownNow();
			try {
				executor.awaitTermination(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {}
		}
	}

	// BINDER PART

	private final IBinder mBinder = new LocalBinder();

	private Set<PacketListListner> listners = new HashSet<PacketListListner>();

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
	public void registerListner(PacketListListner listner) {
		listners.add(listner);
	}

	@Override
	public void removeListner(PacketListListner listner) {
		listners.remove(listner);
	}

	@Override
	public Collection<AdvertisementPacket> getPackets() {
		return packets.values();
	}

	@Override
	public void onDoneEmptyingBuffer(String adr) {
		synchronized (bleTasks) {
			bleTasks.remove(adr);
		}
	}
	
	public void addTaskAdr(String adr){
		synchronized (bleTasks) {
			bleTasks.add(adr);
		}
	}
	
	public void removeTaskAdr(String adr){
		synchronized (bleTasks) {
			bleTasks.remove(adr);
		}
	}
}
