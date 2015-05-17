package dk.itu.energyfutures.ble.task;

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
import dk.itu.energyfutures.ble.Application;
import dk.itu.energyfutures.ble.BluetoothLEBackgroundService;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.packethandlers.AdvertisementPacket;

public class ActuationTask extends AsyncTask<Void, Void, Void> {
	private final static String TAG = ActuationTask.class.getSimpleName();
	private AdvertisementPacket packet;
	private Dialog dialog;
	private BluetoothDevice device;
	private Context context;
	private boolean done = false;
	private BluetoothGatt bleGatt;
	private BluetoothLEBackgroundService service;
	private static final int WAIT_TIME = 5 * 1000;
	private static final int THREAD_SLEEP = 250;

	public ActuationTask(AdvertisementPacket packet, Dialog dialog,Context context, BluetoothLEBackgroundService service){
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
		if(!Application.isConnectedToInternet()){
			Log.i(TAG, "No internet!");
			return null;
		}
		service.addTaskAdr(device.getAddress());
		device.connectGatt(context, false, gattCallback);
		long time = System.currentTimeMillis();
		try {
			while (!done) {
				if(System.currentTimeMillis() - time > WAIT_TIME){
					throw new IllegalStateException("We timed out");
				}else if(isCancelled()){
					throw new IllegalStateException("We were interrupted");
				}
				Thread.sleep(THREAD_SLEEP);
			}
		}
		catch (Exception e) {
			if(bleGatt != null){
				bleGatt.disconnect();
			}	
			
		}finally{
			if(bleGatt != null){
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
				if(service == null){
					Log.e(TAG, "Service is null");
					gatt.disconnect();
					done = true;
					return;
				}
				BluetoothGattCharacteristic characteristic = service.getCharacteristic(ITUConstants.BLE_UUID_ITU_ACTUATOR_JSON_COMMAND_CHAR);
				gatt.readCharacteristic(characteristic);
			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status + " for adr: " + adr);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());
			try {
				Thread.sleep(350);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			gatt.disconnect();
			done = true;
			return;
		}
		
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicRead received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());
			byte[] value = characteristic.getValue();
			characteristic.setValue(value[0] > 0 ? new byte[]{0} : new byte[]{1});
			gatt.writeCharacteristic(characteristic);
		}
	};

}
