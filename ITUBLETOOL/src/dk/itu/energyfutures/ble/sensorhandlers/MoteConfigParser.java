package dk.itu.energyfutures.ble.sensorhandlers;

import java.io.UnsupportedEncodingException;

public class MoteConfigParser {

	private String deviceName;
	private String locationName;
	private int advFreqInSec;
	private int percentageForBufferFull;
	private byte[] encodedBytes;

	public MoteConfigParser(byte[] content) throws UnsupportedEncodingException {
		int firstZeroIndex  = 0;
		for(int i = 0; i < content.length; i++){
			if(content[i] == '\0'){
				firstZeroIndex = i;
				break;
			}
		}
		
		int secondZeroIndex  = 0;
		for(int i = firstZeroIndex+1; i < content.length; i++){
			if(content[i] == '\0'){
				secondZeroIndex = i;
				break;
			}
		}
		this.deviceName = new String(content, 0, firstZeroIndex+1, "UTF-8");
		this.locationName = new String(content, firstZeroIndex+1, secondZeroIndex-firstZeroIndex, "UTF-8");
		this.advFreqInSec = (int) content[secondZeroIndex+1];
		this.percentageForBufferFull = (int) content[secondZeroIndex+2];
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public int getAdvFreqInSec() {
		return advFreqInSec;
	}

	public void setAdvFreqInSec(int advFreqInSec) {
		this.advFreqInSec = advFreqInSec;
	}

	public int getPercentageForBufferFull() {
		return percentageForBufferFull;
	}

	public void setPercentageForBufferFull(int percentageForBufferFull) {
		this.percentageForBufferFull = percentageForBufferFull;
	}
	
	public byte[] getEncodedBytes() {
		return encodedBytes;
	}

	public void evaluateAndEncode() throws UnsupportedEncodingException {
		int nameLength = deviceName.length();
		int locationLength = locationName.length();
		int nameLengthBytes = nameLength > 0 ? 2 : 0;
		if(nameLength + locationLength + nameLengthBytes > 13){
			throw new RuntimeException("Length of device name and location must be under 13");
		}
		//header = 1, +1 is for termination zero, +2 is for the freq and buffer full level
		encodedBytes = new byte[1 + nameLength + 1 + locationLength + 1 + 2];
		int i = 0;
		encodedBytes[i++] = 15; // 0b00001111
		byte[] nameBytes = deviceName.getBytes("UTF-8");
		for(int j = 0;j < nameBytes.length; j++){
			encodedBytes[i++] = nameBytes[j];
		}
		encodedBytes[i++] = '\0';
		byte[] loactionBytes = locationName.getBytes("UTF-8");
		for(int j = 0;j < loactionBytes.length; j++){
			encodedBytes[i++] = loactionBytes[j];
		}
		encodedBytes[i++] = '\0';
		encodedBytes[i++] = (byte) (advFreqInSec & 0xff);
		encodedBytes[i] = (byte) (percentageForBufferFull & 0xff);
	}
}
