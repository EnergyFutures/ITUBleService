package dk.itu.energyfutures.ble.task;

import java.io.UnsupportedEncodingException;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.packethandlers.AdvertisementPacket;

public class JSONTask extends AsyncTask<Void, Void, Void> {
	private final static String TAG = JSONTask.class.getSimpleName();
	private AdvertisementPacket packet;
	private Dialog dialog;
	private BluetoothDevice device;
	private Context context;
	private boolean done = false;
	private BluetoothGatt bleGatt;
	private BluetoothLEBackgroundService service;
	private static final int WAIT_TIME = 20 * 1000;
	private static final int THREAD_SLEEP = 250;
	private int state = 0;

	public JSONTask(AdvertisementPacket packet, Dialog dialog, Context context, BluetoothLEBackgroundService service) {
		this.packet = packet;
		this.dialog = dialog;
		this.device = this.packet.getDevice();
		this.context = context;
		this.service = service;
	}

	@Override
	protected void onPostExecute(Void result) {
		dialog.dismiss();
	}

	@Override
	protected Void doInBackground(Void... params) {
		service.addTaskAdr(device.getAddress());
		device.connectGatt(context, false, gattCallback);
		long time = System.currentTimeMillis();
		try {
			while (!done) {
				if (System.currentTimeMillis() - time > WAIT_TIME) {
					throw new IllegalStateException("We timed out");
				} else if (isCancelled()) {
					throw new IllegalStateException("We were interrupted");
				}
				Thread.sleep(THREAD_SLEEP);
			}
		}
		catch (Exception e) {
			if (bleGatt != null) {
				bleGatt.disconnect();
			}

		}
		finally {
			if (bleGatt != null) {
				bleGatt.close();
			}
			service.removeTaskAdr(device.getAddress());
		}
		return null;
	}

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
			String adr = gatt.getDevice().getAddress();
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.i(TAG, "Connected to GATT server: " + adr);
				gatt.discoverServices();
				bleGatt = gatt;
			} else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
				Log.v(TAG, "Disconnecting to GATT server: " + adr);
				done = true;
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
				BluetoothGattService service = gatt.getService(ITUConstants.BLE_UUID_ITU_ACTUATOR_SERVICE);
				if (service == null) {
					Log.e(TAG, "Service is null");
					gatt.disconnect();
					done = true;
					return;
				}
				BluetoothGattCharacteristic characteristic = service.getCharacteristic(ITUConstants.BLE_UUID_ITU_ACTUATOR_JSON_COMMAND_CHAR);
				gatt.readCharacteristic(characteristic);
				BluetoothLEBackgroundService.toggle.set(!BluetoothLEBackgroundService.toggle.get());
			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status + " for adr: " + adr);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());		
			if(state == 0){
				state++;
				boolean res = false;
				try {
					//characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
					res = characteristic.setValue(("/on_act?state=" + (BluetoothLEBackgroundService.toggle.get() ? "1" : "0") + "\n").getBytes("UTF-8"));
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				Log.i(TAG,"res: " + res);
				gatt.writeCharacteristic(characteristic);
				return;
			}
			gatt.disconnect();
			done = true;
			return;
		}

		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicRead received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());
			boolean res = false;
			try {
				//characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
				res = characteristic.setValue(("HueBridge0/2/state").getBytes("UTF-8"));
			}
			catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i(TAG,"res: " + res);
			gatt.writeCharacteristic(characteristic);
		}
	};

}
