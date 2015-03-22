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
import dk.itu.energyfutures.ble.DoneEmptyingBufferListner;
import dk.itu.energyfutures.ble.helpers.GattAttributes;
import dk.itu.energyfutures.ble.helpers.ITUConstants;

public class EmptyBufferTask implements Runnable,DoneEmptyingBufferNotifer {
	private final static String TAG = EmptyBufferTask.class.getSimpleName();
	private List<BluetoothGattService> services;
	private BluetoothDevice device;
	private BluetoothGattDescriptor readAllDescriptor;
	private BluetoothGattCharacteristic readAllChar;
	private Context context;
	private BluetoothGatt connectGatt;
	private List<DoneEmptyingBufferListner> doneEmptyingBufferListners = new ArrayList<DoneEmptyingBufferListner>();
	
	public List<BluetoothGattService> getServices() {
		return services;
	}

	public void setServices(List<BluetoothGattService> services) {
		this.services = services;
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public void setDevice(BluetoothDevice device) {
		this.device = device;
	}

	@Override
	public void run() {
		for (BluetoothGattService service : services) {
			if(service.getUuid().equals(ITUConstants.BLE_UUID_ITU_READ_ALL_MEASUREMENT_SERVICE)){
				readAllChar = service.getCharacteristic(ITUConstants.BLE_UUID_ITU_READ_ALL_MEASUREMENT_VALUE_CHAR);
				readAllDescriptor = readAllChar.getDescriptor(GattAttributes.BLE_UUID_CCCD_DESCRIPTOR);
				break;
			}
		}
		if(readAllDescriptor != null){
			connectGatt = device.connectGatt(context, false, gattCallback);
			connectGatt.setCharacteristicNotification(readAllChar, true);
			readAllDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
			try {
				Thread.sleep(30000);//30 sec
				for(DoneEmptyingBufferListner listner : doneEmptyingBufferListners){
					listner.onDoneEmptyingBuffer(device.getAddress());
				}
				connectGatt.setCharacteristicNotification(readAllChar, false);
				readAllDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
				connectGatt.writeDescriptor(readAllDescriptor);
				Thread.sleep(5000);//5 sec
				connectGatt.disconnect();
				Thread.sleep(5000);//5 sec
			}catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
			String adr = gatt.getDevice().getAddress();
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.v(TAG, "Connected to GATT server: " + adr);				
				connectGatt.writeDescriptor(readAllDescriptor);
			}else if(newState == BluetoothProfile.STATE_DISCONNECTING){
				Log.v(TAG, "Disconnecting to GATT server: " + adr);
			}else if(newState == BluetoothProfile.STATE_DISCONNECTED){
				Log.v(TAG, "Disconnected to GATT server: " + adr);
			}else if(newState == BluetoothProfile.STATE_CONNECTING){
				Log.v(TAG, "Connecting to GATT server: " + adr);
			}
		}

		@Override
		public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
			String adr = connectGatt.getDevice().getAddress();
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.v(TAG, "Done discovering: " + adr);				
			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status + " for adr: " + adr);
			}
		}

		@Override
		public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.v(TAG, "Received packet: " + characteristic.getValue());	
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
	
	
	
	
	public void setContext(Context applicationContext) {
		this.context = applicationContext;
	}

	@Override
	public void registerDoneEmptyingBufferListner(DoneEmptyingBufferListner listner) {
		doneEmptyingBufferListners.add(listner);
	}
}
