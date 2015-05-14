package dk.itu.energyfutures.ble.sensorhandlers;

import dk.itu.energyfutures.ble.helpers.BluetoothHelper;

public class SensorParser {

	private int coordinateIndex;
	private int sampleFrequency;
	private int sensorType;
	private byte[] encodedBytes;
	private int id;
	private int sensorMake;

	public SensorParser(byte[] content) {
		this.coordinateIndex = (int) content[1];
		this.setSensorType((int) content[6]);
		this.setSensorMake((int) content[7]);
		this.sampleFrequency = (int) BluetoothHelper.unsignedBytesTo32Int(content, 10);
	}

	private void setSensorMake(int sensorMake) {
		this.sensorMake = sensorMake;
	}

	public int getSensorMake() {
		return sensorMake;
	}

	public int getCoordinateIndex() {
		return coordinateIndex;
	}

	public void setCoordinateIndex(int coordinateIndex) {
		this.coordinateIndex = coordinateIndex;
	}

	public long getSampleFrequency() {
		return sampleFrequency;
	}

	public void setSampleFrequency(int sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	public int getSensorType() {
		return sensorType;
	}

	public void setSensorType(int sensorType) {
		this.sensorType = sensorType;
	}

	public byte[] getEncodedBytes() {
		return encodedBytes;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setIdAndEncode(int id) {
		this.id = id;
		encode();
	}

	private void encode() {
		if (id < 0) {
			throw new RuntimeException("ID must be zero or above");
		}
		if (sampleFrequency < 1000) {
			throw new RuntimeException("Sampling frequency must be 1000 ms or above");
		}
		encodedBytes = new byte[10];
		int index = 0;
		encodedBytes[index++] = 49; // 0b00110001 = coor, id and sample.freq is to be overridden
		encodedBytes[index++] = (byte) coordinateIndex;
		encodedBytes[index++] = (byte) (id & 0xff);
		encodedBytes[index++] = (byte) ((id >> 8) & 0xff);
		encodedBytes[index++] = (byte) (sampleFrequency & 0xff);
		encodedBytes[index++] = (byte) ((sampleFrequency >> 8) & 0xff);
		encodedBytes[index++] = (byte) ((sampleFrequency >> 16) & 0xff);
		encodedBytes[index] = (byte) ((sampleFrequency >> 24) & 0xff);
	}
}
