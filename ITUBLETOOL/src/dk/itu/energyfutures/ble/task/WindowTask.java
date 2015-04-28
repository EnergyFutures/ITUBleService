package dk.itu.energyfutures.ble.task;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import dk.itu.energyfutures.ble.AdvertisementPacket;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService;
import dk.itu.energyfutures.ble.helpers.ITUConstants;

public class WindowTask extends AsyncTask<Void, String, Void> {
	private final static String TAG = WindowTask.class.getSimpleName();
	private AdvertisementPacket packet;
	private AlertDialog dialog;
	private BluetoothDevice device;
	private Context context;
	private boolean done = false;
	private BluetoothGatt bleGatt;
	private static final int THREAD_SLEEP = 250;
	private BluetoothGattCharacteristic windowChar;
	private BluetoothLEBackgroundService service;

	public WindowTask(AdvertisementPacket packet, Context context, BluetoothLEBackgroundService service) {
		this.packet = packet;
		this.device = this.packet.getDevice();
		this.context = context;
		this.service = service;
	}

	@Override
	protected void onPostExecute(Void result) {
		dialog.dismiss();
	}

	@Override
	protected void onProgressUpdate(String... values) {
		dialog.setMessage(values[0]);
	}

	@Override
	protected Void doInBackground(Void... params) {
		service.addTaskAdr(device.getAddress());
		device.connectGatt(context, false, gattCallback);
		try {
			while (!done) {
				if (isCancelled()) {
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
				publishProgress("Almost there, connected...");
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
				publishProgress("Ready. Dismiss (ex. push back) when done!");
				BluetoothGattService service = gatt.getService(ITUConstants.BLE_UUID_ITU_ACTUATOR_SERVICE);
				if (service == null) {
					Log.e(TAG, "Service is null");
					gatt.disconnect();
					done = true;
					return;
				}
				windowChar = service.getCharacteristic(ITUConstants.BLE_UUID_ITU_ACTUATOR_COMMAND_CHAR);
			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status + " for adr: " + adr);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());
		}

	};

	public void openWindow() {
		if (bleGatt != null && windowChar != null) {
			windowChar.setValue(new byte[] { 1 });
			bleGatt.writeCharacteristic(windowChar);
		}
	}

	public void closeWindow() {
		if (bleGatt != null && windowChar != null) {
			windowChar.setValue(new byte[] { 0 });
			bleGatt.writeCharacteristic(windowChar);
		}
	}

	public void stopWindow() {
		if (bleGatt != null && windowChar != null) {
			windowChar.setValue(new byte[] { 2 });
			bleGatt.writeCharacteristic(windowChar);
		}
	}

	public void dismissed() {
		if (bleGatt != null) {
			bleGatt.disconnect();
		}
		done = true;
	}

	public void setDialog(AlertDialog dialog) {
		this.dialog = dialog;
	}
}
