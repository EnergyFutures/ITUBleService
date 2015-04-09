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
	private ITUConstants.ITU_SENSOR_CONFIG_TYPE sensorConfigType;
	private int batteryLevel;
	private Date timeStamp;
	private BluetoothDevice device;
	private long timeOfLastDiscoveryCheck;
	private int bufferLevel;
	private int id;
	private boolean bufferNeedsCleaning;

	/*
	 * public AdvertisementPacket(String deviceName, String location, ITU_SENSOR_TYPE sensorType, double value, ITU_MOTE_COORDINATE coordinate, ITU_MOTE_TYPE moteType) { super(); this.deviceName =
	 * deviceName; this.location = location; this.sensorType = sensorType; this.value = value; this.coordinate = coordinate; this.moteType = moteType; }
	 */



	public AdvertisementPacket() {}

	public static AdvertisementPacket processITUAdvertisementValue(byte[] data, int index, int length, String deviceName, BluetoothDevice device) {
		AdvertisementPacket packet = new AdvertisementPacket();
		packet.setDeviceName(deviceName);
		packet.setDevice(device);

		String locationName = BluetoothHelper.decodeLocation(data, index);
		
		index += locationName.length() + 1;
		packet.setLocation(locationName);
		
		packet.setBufferLevel(data[index++]);
		
		int misc = data[index++] & 0x000000ff;

		int sensorConfigType = (misc & 0x00000001);
		packet.setSensorConfigType(ITUConstants.ITU_SENSOR_CONFIG_TYPE_ARRAY[sensorConfigType]);
		
		packet.setBufferNeedsCleaning(((misc >> 1) & 0x00000001) == 1);

		int batteryLevel = (misc >> 2) & 0x000000ff;
		packet.setBatteryLevel((int) ((100 - batteryLevel) * 1.1)); // Go from 3,6 to 3,3 ref

		ITUConstants.ITU_SENSOR_TYPE type = ITUConstants.ITU_SENSOR_TYPE_ARRAY[data[index++]];
		packet.setSensorType(type);

		double value = BluetoothHelper.getIEEEFloatValue(BluetoothHelper.unsignedBytesToInt(data, index));
		index += 4;
		packet.setValue(value);

		ITUConstants.ITU_MOTE_COORDINATE coor = ITUConstants.ITU_MOTE_COORDINATE_ARRAY[data[index++]];
		packet.setCoordinate(coor);

		int id = ((data[index++]) & 0x000000ff)  | ((data[index++] << 8) & 0x0000ff00);
		packet.setId(id);

		packet.timeStamp = new Date();
		
		return packet;
	}

	private void setDevice(BluetoothDevice device) {
		this.device = device;
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public int getBatteryLevel() {
		return batteryLevel;
	}

	private void setBatteryLevel(int batteryLevel) {
		this.batteryLevel = batteryLevel;
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

	public ITUConstants.ITU_SENSOR_CONFIG_TYPE getSensorConfigType() {
		return sensorConfigType;
	}

	private void setSensorConfigType(ITUConstants.ITU_SENSOR_CONFIG_TYPE moteType) {
		this.sensorConfigType = moteType;
	}

	public long getTimeOfLastDiscoveryCheck() {
		return timeOfLastDiscoveryCheck;
	}

	public void setTimeOfLastDiscoveryCheck(long timeOfLastDiscoveryCheck) {
		this.timeOfLastDiscoveryCheck = timeOfLastDiscoveryCheck;
	}

	public int getBufferLevel() {
		return bufferLevel;
	}

	public void setBufferLevel(int bufferLevel) {
		this.bufferLevel = bufferLevel;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isBufferNeedsCleaning() {
		return bufferNeedsCleaning;
	}

	public void setBufferNeedsCleaning(boolean bufferNeedsCleaning) {
		this.bufferNeedsCleaning = bufferNeedsCleaning;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AdvertisementPacket other = (AdvertisementPacket) obj;
		if (id != other.id) return false;
		return true;
	}

	@Override
	public String toString() {
		return "AdvertisementPacket [deviceName=" + deviceName + ", location=" + location + ", sensorType=" + sensorType + ", value=" + value + ", coordinate=" + coordinate + ", sensorConfigType="
				+ sensorConfigType + ", batteryLevel=" + batteryLevel + ", timeStamp=" + timeStamp + ", bufferLevel=" + bufferLevel + ", id=" + id + ", bufferNeedsCleaning=" + bufferNeedsCleaning
				+ "]";
	}

	
}
