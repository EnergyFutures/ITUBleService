package dk.itu.energyfutures.ble;

import java.util.Date;

import android.bluetooth.BluetoothDevice;
import dk.itu.energyfutures.ble.helpers.BluetoothHelper;
import dk.itu.energyfutures.ble.helpers.ITUConstants;

public class AdvertisementPacket {
	private final static String TAG = AdvertisementPacket.class.getSimpleName();
	private String deviceName;
	private String location;
	private ITUConstants.ITU_SENSOR_TYPE sensorType; 
	private double value;
	private ITUConstants.ITU_MOTE_COORDINATE coordinate;
	private ITUConstants.ITU_MOTE_TYPE moteType;
	private boolean bufferFull;
	private int batteryLevelIncreamentOf10;
	private String id;
	private Date timeStamp;
	private BluetoothDevice device;
	private long timeOfLastDiscoveryCheck;
	
	/*public AdvertisementPacket(String deviceName, String location, ITU_SENSOR_TYPE sensorType, double value, ITU_MOTE_COORDINATE coordinate, ITU_MOTE_TYPE moteType) {
		super();
		this.deviceName = deviceName;
		this.location = location;
		this.sensorType = sensorType;
		this.value = value;
		this.coordinate = coordinate;
		this.moteType = moteType;
	}*/
	
	public AdvertisementPacket(){}
	
	public static AdvertisementPacket processITUAdvertisementValue(byte[] data, int index, int length, String deviceName, BluetoothDevice device) {
		AdvertisementPacket packet = new AdvertisementPacket();
		packet.setDeviceName(deviceName);
		byte header = data[index++];
		if((header & (0x01 << 4)) > 0){
			int locationNameLength = 14 - deviceName.length();
			String locationName = BluetoothHelper.decodeLocalName(data, index, locationNameLength);
			//Log.v(TAG, "Location name: " + locationName);
			index += locationNameLength;
			packet.setLocation(locationName);
		}
		if((header & (0x01 << 3)) > 0){
			ITUConstants.ITU_SENSOR_TYPE type = ITUConstants.ITU_SENSOR_TYPE_ARRAY[data[index] > 100 ? (data[index] - 107) : (data[index])];
			//Log.i(TAG, "type: " + type);
			//System.out.println("___ type: " + type );
			packet.setSensorType(type);
			index++;
		}
		if((header & (0x01 << 2)) > 0){		
			double value = BluetoothHelper.getIEEEFloatValue(BluetoothHelper.unsignedBytesToInt(data, index));
			//Log.i(TAG, "value: " + value);
			index += 4;
			packet.setValue(value);
		}
		if((header & (0x01 << 1)) > 0){			
			ITUConstants.ITU_MOTE_COORDINATE coor = ITUConstants.ITU_MOTE_COORDINATE_ARRAY[data[index++]];
			//Log.v(TAG, "coord: " + coor);
			packet.setCoordinate(coor);
		}
		if((header & 0x01) > 0){
			int misc = data[index] & 0x000000ff;
			
			int moteType = ((misc >> 1) & 0x07);
			//Log.v(TAG, "sensor type: " + moteType );
			packet.setMoteType(ITUConstants.ITU_MOTE_TYPE_ARRAY[moteType]);
			
			if(moteType == 0){
				int batteryLevel = (misc >> 4);
				//Log.v(TAG, "battery level: " + batteryLevel);
				packet.setBatteryLevelIncreamentOf10(batteryLevel);
				
				boolean bufferFull = (misc & 0x01) == 1;
				//Log.v(TAG, "buffer full: " +  bufferFull);
				packet.setBufferFull(bufferFull);
			}			
		}
		packet.setId();
		packet.timeStamp = new Date();
		packet.setDevice(device);
		return packet;
	}
	
	private void setDevice(BluetoothDevice device) {
		this.device = device;
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public boolean isBufferFull() {
		return bufferFull;
	}

	private void setBufferFull(boolean bufferFull) {
		this.bufferFull = bufferFull;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public int getBatteryLevelIncreamentOf10() {
		return batteryLevelIncreamentOf10;
	}

	private void setBatteryLevelIncreamentOf10(int batteryLevelIncreamentOf10) {
		this.batteryLevelIncreamentOf10 = batteryLevelIncreamentOf10;
	}

	public String getDeviceName() {
		return deviceName;
	}
	private void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	public String getLocation() {
		return location;
	}
	private void setLocation(String location) {
		this.location = location;
	}
	public ITUConstants.ITU_SENSOR_TYPE getSensorType() {
		return sensorType;
	}
	private void setSensorType(ITUConstants.ITU_SENSOR_TYPE sensorType) {
		this.sensorType = sensorType;
	}
	public double getValue() {
		return value;
	}
	private void setValue(double value) {
		this.value = value;
	}
	public ITUConstants.ITU_MOTE_COORDINATE getCoordinate() {
		return coordinate;
	}
	private void setCoordinate(ITUConstants.ITU_MOTE_COORDINATE coordinate) {
		this.coordinate = coordinate;
	}
	public ITUConstants.ITU_MOTE_TYPE getMoteType() {
		return moteType;
	}
	private void setMoteType(ITUConstants.ITU_MOTE_TYPE moteType) {
		this.moteType = moteType;
	}
	
	private void setId() {
		this.id = deviceName+location+coordinate+sensorType;
	}

	public String getId() {
		return id;
	}

	public long getTimeOfLastDiscoveryCheck() {
		return timeOfLastDiscoveryCheck;
	}

	public void setTimeOfLastDiscoveryCheck(long timeOfLastDiscoveryCheck) {
		this.timeOfLastDiscoveryCheck = timeOfLastDiscoveryCheck;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AdvertisementPacket other = (AdvertisementPacket) obj;
		if (id == null) {
			if (other.id != null) return false;
		} else if (!id.equals(other.id)) return false;
		return true;
	}

	@Override
	public String toString() {
		return "AdvertisementPacket [deviceName=" + deviceName + ", location=" + location + ", sensorType=" + sensorType + ", value=" + value + ", coordinate=" + coordinate + ", moteType=" + moteType
				+ ", bufferFull=" + bufferFull + ", batteryLevelIncreamentOf10=" + batteryLevelIncreamentOf10 + "]";
	}
}
