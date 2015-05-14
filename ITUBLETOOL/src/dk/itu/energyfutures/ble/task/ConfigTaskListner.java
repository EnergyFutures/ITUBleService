package dk.itu.energyfutures.ble.task;

import java.util.List;

import android.bluetooth.BluetoothGattCharacteristic;

public interface ConfigTaskListner {
	void onDoneDiscovering(List<BluetoothGattCharacteristic> sensors,BluetoothGattCharacteristic configChar);
	void onStatusUpdate(String status);
}
