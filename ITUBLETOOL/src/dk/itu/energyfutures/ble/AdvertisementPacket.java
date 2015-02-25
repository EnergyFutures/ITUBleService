package dk.itu.energyfutures.ble;

import dk.itu.energyfutures.ble.helpers.BluetoothHelper;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.helpers.ITUConstants.ITU_MOTE_COORDINATE;
import dk.itu.energyfutures.ble.helpers.ITUConstants.ITU_MOTE_TYPE;
import dk.itu.energyfutures.ble.helpers.ITUConstants.ITU_SENSOR_TYPE;

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
	
	public AdvertisementPacket(String deviceName, String location, ITU_SENSOR_TYPE sensorType, double value, ITU_MOTE_COORDINATE coordinate, ITU_MOTE_TYPE moteType) {
		super();
		this.deviceName = deviceName;
		this.location = location;
		this.sensorType = sensorType;
		this.value = value;
		this.coordinate = coordinate;
		this.moteType = moteType;
	}
	
	public AdvertisementPacket(){}
	
	public static AdvertisementPacket processITUAdvertisementValue(byte[] data, int index, int length, String deviceName) {
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
			ITUConstants.ITU_SENSOR_TYPE type = ITUConstants.ITU_SENSOR_TYPE_ARRAY[data[index++]];
			//Log.i(TAG, "type: " + type);
			//System.out.println("___ type: " + type );
			packet.setSensorType(type);
		}
		if((header & (0x01 << 2)) > 0){		
			double value = BluetoothHelper.getIEEEFloatValue(BluetoothHelper.unsignedBytesToInt(data, index));
			//Log.v(TAG, "value: " + value);
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
		//Log.v(TAG, "******************************************" );
		return packet;
	}
	
	public boolean isBufferFull() {
		return bufferFull;
	}

	public void setBufferFull(boolean bufferFull) {
		this.bufferFull = bufferFull;
	}

	public int getBatteryLevelIncreamentOf10() {
		return batteryLevelIncreamentOf10;
	}

	public void setBatteryLevelIncreamentOf10(int batteryLevelIncreamentOf10) {
		this.batteryLevelIncreamentOf10 = batteryLevelIncreamentOf10;
	}

	public String getDeviceName() {
		return deviceName;
	}
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public ITUConstants.ITU_SENSOR_TYPE getSensorType() {
		return sensorType;
	}
	public void setSensorType(ITUConstants.ITU_SENSOR_TYPE sensorType) {
		this.sensorType = sensorType;
	}
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
	}
	public ITUConstants.ITU_MOTE_COORDINATE getCoordinate() {
		return coordinate;
	}
	public void setCoordinate(ITUConstants.ITU_MOTE_COORDINATE coordinate) {
		this.coordinate = coordinate;
	}
	public ITUConstants.ITU_MOTE_TYPE getMoteType() {
		return moteType;
	}
	public void setMoteType(ITUConstants.ITU_MOTE_TYPE moteType) {
		this.moteType = moteType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coordinate == null) ? 0 : coordinate.hashCode());
		result = prime * result + ((deviceName == null) ? 0 : deviceName.hashCode());
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((sensorType == null) ? 0 : sensorType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AdvertisementPacket other = (AdvertisementPacket) obj;
		if (coordinate != other.coordinate) return false;
		if (deviceName == null) {
			if (other.deviceName != null) return false;
		} else if (!deviceName.equals(other.deviceName)) return false;
		if (location == null) {
			if (other.location != null) return false;
		} else if (!location.equals(other.location)) return false;
		if (sensorType != other.sensorType) return false;
		return true;
	}
}
