package dk.itu.energyfutures.ble.helpers;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

public class BluetoothHelper {
	public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";

	public static UUID sixteenBitUuid(long shortUuid) {
		return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
	}

	public static String getDeviceInfoText(BluetoothDevice device, int rssi, byte[] scanRecord) {
		return new StringBuilder().append("Name: ").append(device.getName()).append("\nMAC: ").append(device.getAddress()).append("\nRSSI: ").append(rssi).append("\nScan Record:")
				.append(parseScanRecord(scanRecord)).toString();
	}

	public static String bytesToHex(byte[] data) {
		return bytesToHex(data, 0, data.length);
	}

	public static String bytesToHex(byte[] data, int offset, int length) {
		if (length <= 0) {
			return "";
		}

		StringBuilder hex = new StringBuilder();
		for (int i = offset; i < offset + length; i++) {
			hex.append(String.format(" %02X", data[i]));
		}
		hex.deleteCharAt(0);
		return hex.toString();
	}

	private static final int PRINTABLE_ASCII_MIN = 0x20; // ' '
	private static final int PRINTABLE_ASCII_MAX = 0x7E; // '~'

	private static boolean isPrintableAscii(int c) {
		return c >= PRINTABLE_ASCII_MIN && c <= PRINTABLE_ASCII_MAX;
	}

	public static String bytesToAscii(byte[] data, int offset, int length) {
		StringBuilder ascii = new StringBuilder();
		boolean zeros = false;
		for (int i = offset; i < offset + length; i++) {
			int c = data[i] & 0xFF;
			if (isPrintableAscii(c)) {
				if (zeros) {
					return null;
				}
				ascii.append((char) c);
			} else if (c == 0) {
				zeros = true;
			} else {
				return null;
			}
		}
		return ascii.toString();
	}

	// Bluetooth Spec V4.0 - Vol 3, Part C, section 8
	private static String parseScanRecord(byte[] scanRecord) {
		StringBuilder output = new StringBuilder();
		int i = 0;
		while (i < scanRecord.length) {
			int len = scanRecord[i++] & 0xFF;
			if (len == 0) break;
			switch (scanRecord[i] & 0xFF) {
			// https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
			case 0x0A: // Tx Power
				output.append("\n  Tx Power: ").append(scanRecord[i + 1]);
				break;
			case 0x01: // FLAGS
				output.append("\n  Flags: ").append(GattAttributes.lookupFlags(scanRecord[i + 1]));
				break;
			case 0x19: // Appearance
				int appearance = scanRecord[i + 2];
				appearance = (appearance << 8) | scanRecord[i + 1];
				output.append("\n  Appearance: ").append(GattAttributes.lookupAppearance(appearance));
				break;
			case 0xFF: // Manufacturer Specific data (RFduinoBLE.advertisementData)
				output.append("\n  Advertisement Data: ").append(bytesToHex(scanRecord, i + 3, len));

				String ascii = bytesToAscii(scanRecord, i + 3, len);
				if (ascii != null) {
					output.append(" (\"").append(ascii).append("\")");
				}
				break;
			}
			i += len;
		}
		return output.toString();
	}

	public static boolean isCharacteristicWriteable(BluetoothGattCharacteristic characteristic) {
		return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
	}

	public static boolean isCharacterisitcReadable(BluetoothGattCharacteristic characteristic) {
		return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
	}

	public static boolean isCharacterisiticNotifiable(BluetoothGattCharacteristic characteristic) {
		return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
	}

	public static boolean getBitAsBoolean(int value, int bitPlacement) {
		return ((value >> bitPlacement) & 0x01) == 0x01;
	}

	public static double getIEEEFloatValue(int measurement) {
		byte exponent = (byte) (measurement >> 24);
		int mantissa = measurement & 0x00ffffff;
		if (exponent == 0) return mantissa;
		return mantissa * Math.pow(10, exponent);
	}
}
