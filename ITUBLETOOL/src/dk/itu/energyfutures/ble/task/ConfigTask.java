package dk.itu.energyfutures.ble.task;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.AsyncTask;
import android.util.Log;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.sensorhandlers.MoteConfigParser;
import dk.itu.energyfutures.ble.sensorhandlers.SensorParser;
import dk.itu.energyfutures.ble.smap.SMAPController;

public class ConfigTask extends AsyncTask<Void, String, Void> implements ConfigTaskNotifier {
	private final static String TAG = ConfigTask.class.getSimpleName();
	private BluetoothDevice device;
	private Activity context;
	private boolean done = false;
	private BluetoothGatt bleGatt;
	private static final int THREAD_SLEEP = 1000;
	private List<BluetoothGattCharacteristic> sensors = new ArrayList<BluetoothGattCharacteristic>();
	private BluetoothGattCharacteristic configChar;
	private int numberOfCharsRead = 0;
	private int numberOfCharReadsReq = 0;
	private int writeIndex = 0;
	private List<BluetoothGattCharacteristic> sensorsToRead = new ArrayList<BluetoothGattCharacteristic>();
	private List<ConfigTaskListner> listners = new ArrayList<ConfigTaskListner>();
	private String deviceAdr;
	

	public ConfigTask(BluetoothDevice device, Activity context) {
		this.device = device;
		this.context = context;
		this.deviceAdr = device.getAddress();
	}

	@Override
	protected void onPostExecute(Void result) {
	
	}

	@Override
	protected void onProgressUpdate(String... values) {
		for (ConfigTaskListner listner : listners) {
			listner.onStatusUpdate(values[0]);
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		device.connectGatt(context, false, gattCallback);
		publishProgress("Connecting...");
		try {
			while (!done) {
				if (isCancelled()) {
					break;
				}
				Thread.sleep(THREAD_SLEEP);
			}
			closeDown("Done... disconnecting");
		}
		catch (Exception e) {
			closeDown("Exception handled... disconnecting");
		}
		return null;
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
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if(characteristic == configChar){
				gatt.readCharacteristic(sensorsToRead.get(0));
			}else{
				sensors.add(characteristic);
				numberOfCharsRead++;
				publishProgress("Read characteristic: " + numberOfCharsRead);
				if(numberOfCharReadsReq == numberOfCharsRead){
					SMAPController.fetchIdsFromSmap(numberOfCharsRead, deviceAdr);
					for (ConfigTaskListner configTaskListner : listners) {
						configTaskListner.onDoneDiscovering(sensors, configChar);
					}
					sensorsToRead.clear();
				}else{
					gatt.readCharacteristic(sensorsToRead.get(numberOfCharsRead));
				}
			}
		}

		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.i(TAG, "Connected to GATT server: " + deviceAdr);
				publishProgress("Almost there, connected...");
				gatt.discoverServices();
				bleGatt = gatt;
			} else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
				Log.v(TAG, "Disconnecting to GATT server: " + deviceAdr);
				done = true;
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.v(TAG, "Disconnected to GATT server: " + deviceAdr);
			} else if (newState == BluetoothProfile.STATE_CONNECTING) {
				Log.v(TAG, "Connecting to GATT server: " + deviceAdr);
			}
		}

		@Override
		public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.v(TAG, "Done discovering: " + deviceAdr);
				publishProgress("Done discovering, collecting info...");
				for (BluetoothGattService service : gatt.getServices()) {
					UUID id = service.getUuid();
					if(id.equals(ITUConstants.BLE_UUID_ITU_MEASUREMENT_SERVICE)){
						BluetoothGattCharacteristic characteristic = service.getCharacteristic(ITUConstants.BLE_UUID_ITU_MEASUREMENT_CONFIG_CHAR);
						if(characteristic != null){
							sensorsToRead.add(characteristic);
						}						
					}else if(id.equals(ITUConstants.BLE_UUID_ITU_MOTE_SERVICE)){
						configChar = service.getCharacteristic(ITUConstants.BLE_UUID_ITU_CONFIG_MOTE_CHAR);
						if(configChar == null){
							publishProgress("ERROR, could not find mote_config");
							Log.e(TAG, "Could not find sensors to configure!!!");
							return;
						}
					}
				}
				if(sensorsToRead.size() == 0){
					//REPORT GUI ERROR
					publishProgress("ERROR, could not find sensors to configure");
					Log.e(TAG, "Could not find sensors to configure!!!");
				}else{
					numberOfCharReadsReq = sensorsToRead.size();
					gatt.readCharacteristic(configChar);
				}
			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status + " for adr: " + deviceAdr);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicWrite received: " + status + " for adr: " + deviceAdr);
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if(characteristic == configChar){
					done = true;
				}else if(writeIndex == sensors.size()){
					bleGatt.writeCharacteristic(configChar);
				}else{
					bleGatt.writeCharacteristic(sensors.get(writeIndex++));
				}
			}else {
				Log.e(TAG, "Error writing config");
			}
		}
	};

	@Override
	public void registerListner(ConfigTaskListner listner) {
		listners.add(listner);
	}
	
	@Override
	public void unregisterListner(ConfigTaskListner listner) {
		listners.remove(listner);
	}

	@Override
	public void writeConfigAndExit(MoteConfigParser moteConfig, List<SensorParser> sensorParsers) throws UnsupportedEncodingException {
		moteConfig.evaluateAndEncode();
		configChar.setValue(moteConfig.getEncodedBytes());
		
		int sensorSize =  sensors.size();
		if("NEWBORN".equals(device.getName())){
			int[] ids = SMAPController.getIdsForAdr(deviceAdr);
			if(ids == null){
				SMAPController.fetchIdsFromSmap(sensorSize, deviceAdr);
				throw new RuntimeException("Could not retrive ids from backend, please try again");
			}
			for(int i = 0; i < sensorSize; i++){
				SensorParser sensorParser = sensorParsers.get(i);
				sensorParser.setIdAndEncode(ids[i]);
				sensors.get(i).setValue(sensorParser.getEncodedBytes());
			}
		}else{
			for(int i = 0; i < sensorSize; i++){
				SensorParser sensorParser = sensorParsers.get(i);
				sensorParser.setIdAndEncode(sensorParser.getId());
				sensors.get(i).setValue(sensorParser.getEncodedBytes());
			}
		}
		SMAPController.postMetaDataToSmap(moteConfig, sensorParsers, deviceAdr);
		if(bleGatt != null){
			bleGatt.writeCharacteristic(sensors.get(writeIndex++));
		}
	}
}
