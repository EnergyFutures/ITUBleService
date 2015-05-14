package dk.itu.energyfutures.ble.task;

import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import dk.itu.energyfutures.ble.helpers.GzipAndJsonParser;
import dk.itu.energyfutures.ble.helpers.ITUConstants;

public class ParseJsonMoteTask implements Runnable, TaskDoneNotifer {
	private final static String TAG = ParseJsonMoteTask.class.getSimpleName();
	private BluetoothDevice device;
	private Context context;
	private List<TaskDoneListner> taskDoneListners = new ArrayList<TaskDoneListner>();
	private boolean done;
	private BluetoothGatt bleGatt;
	private static final int WAIT_TIME = 1 * 60 * 1000;
	private static final int THREAD_SLEEP = 1 * 1000;
	private long timeOfLastActivity = System.currentTimeMillis();
	private List<Byte> json = new ArrayList<Byte>();
	
	public BluetoothDevice getDevice() {
		return device;
	}

	public void setDevice(BluetoothDevice device) {
		this.device = device;
	}

	@Override
	public void run() {
		device.connectGatt(context, false, gattCallback);
		Thread thisThread = Thread.currentThread();
		try {
			while (!done) {
				if (System.currentTimeMillis() - timeOfLastActivity > WAIT_TIME) {
					throw new IllegalStateException("We timed out");
				} else if (thisThread.isInterrupted()) {
					throw new IllegalStateException("We were interrupted");
				}
				Thread.sleep(THREAD_SLEEP);
			}
			closeDown("Done... disconnecting");
		}
		catch (Exception e) {
			closeDown("Exception handled... disconnecting, msg: " + e.getMessage());
		}
		finally {
			for (TaskDoneListner listner : taskDoneListners) {
				listner.onTaskDone(device.getAddress());
			}
		}
	}
	
	private void closeDown(String msg) {
		if (bleGatt != null) {
			bleGatt.disconnect();
			Log.v(TAG, msg);
			try {
				Thread.sleep(250);
			}
			catch (InterruptedException ex) {}
			bleGatt.close();
		}
	}

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
			String adr = gatt.getDevice().getAddress();
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.i(TAG, "Connected to GATT server: " + adr);
				gatt.discoverServices();
				timeOfLastActivity = System.currentTimeMillis();
				bleGatt = gatt;
			} else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
				Log.v(TAG, "Disconnecting to GATT server: " + adr);
				done = true;
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.v(TAG, "Disconnected to GATT server: " + adr);
				done = true;
			} else if (newState == BluetoothProfile.STATE_CONNECTING) {
				Log.v(TAG, "Connecting to GATT server: " + adr);
				timeOfLastActivity = System.currentTimeMillis();
			}
		}

		@Override
		public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
			String adr = gatt.getDevice().getAddress();
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.v(TAG, "Done discovering: " + adr);
				timeOfLastActivity = System.currentTimeMillis();
				BluetoothGattService service = gatt.getService(ITUConstants.BLE_UUID_ITU_ACTUATOR_SERVICE);
				if (service == null) {
					Log.e(TAG, "Service is null");
					done = true;
					return;
				}
				BluetoothGattCharacteristic characteristic = service.getCharacteristic(ITUConstants.BLE_UUID_ITU_ACTUATOR_JSON_CHAR);
				if (characteristic == null) {
					Log.e(TAG, "characteristic is null");
					done = true;
					return;
				}
				gatt.readCharacteristic(characteristic);
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
						gatt.readCharacteristic(characteristic);
				} else {
					try {
						String result = GzipAndJsonParser.unzipAndParseByteList(json);
						json.clear();
						Log.i(TAG, "JSON: " + result);
						done = true;
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			timeOfLastActivity = System.currentTimeMillis();
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Log.v(TAG, "onCharacteristicChanged adr: " + gatt.getDevice().getAddress());
			timeOfLastActivity = System.currentTimeMillis();
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());
			timeOfLastActivity = System.currentTimeMillis();
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			timeOfLastActivity = System.currentTimeMillis();
		};

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			timeOfLastActivity = System.currentTimeMillis();
		};

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {};

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			timeOfLastActivity = System.currentTimeMillis();
		};
	};

	public void setContext(Context applicationContext) {
		this.context = applicationContext;
	}

	@Override
	public void registerTaskDoneListner(TaskDoneListner listner) {
		taskDoneListners.add(listner);
	}
}
