package dk.itu.energyfutures.ble.helpers;

import java.util.HashMap;
import java.util.UUID;

public class GattAttributes {
	private static HashMap<UUID, String> attributes = new HashMap<UUID, String>();
	private static HashMap<Integer, String> appearance = new HashMap<Integer, String>();
	private static HashMap<Integer, String> flags = new HashMap<Integer, String>();
	public static final UUID BLE_UUID_ITU_MEASUREMENT_SERVICE = UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_UUID_ITU_ACTUATOR_SERVICE = UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_UUID_DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

	public static UUID BLE_UUID_ITU_MEASUREMENT_VALUE_CHAR = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");
	public static UUID BLE_UUID_ITU_MEASUREMENT_CONFIG_CHAR = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
	public static UUID BLE_UUID_ITU_ACTUATOR_CHAR = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
	public static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	public static UUID MANUFACTOR_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");

	/** < 3 dimension axis, always starting from the corner closest to the door */
	public static enum ITU_COORDINATE {
		ITS_MEAS_LOCATION_NOT_SET, ITS_MEAS_LOCATION_IN_END_END_TOP, ITS_MEAS_LOCATION_IN_END_END_MIDDLE, ITS_MEAS_LOCATION_IN_END_END_FLOOR, ITS_MEAS_LOCATION_IN_END_MIDDLE_TOP, ITS_MEAS_LOCATION_IN_END_BEGIN_TOP, ITS_MEAS_LOCATION_IN_MIDDLE_END_TOP, ITS_MEAS_LOCATION_IN_BEGIN_END_TOP, ITS_MEAS_LOCATION_IN_BEGIN_END_MIDDLE, ITS_MEAS_LOCATION_OUT_WINDOW_1
	}

	public static enum ITU_TYPE {
		BLE_UUID_ITU_SENSOR_TYPE_NOT_SET, BLE_UUID_ITU_SENSOR_TYPE_TEMPERATURE, BLE_UUID_ITU_SENSOR_TYPE_LIGHT, BLE_UUID_ITU_SENSOR_TYPE_SOUND
	}

	static {
		attributes.put(UUID.fromString("00001811-0000-1000-8000-00805f9b34fb"), "Alert Notification Service");
		attributes.put(UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb"), "Battery Service");
		attributes.put(UUID.fromString("00001810-0000-1000-8000-00805f9b34fb"), "Blood Pressure");
		attributes.put(UUID.fromString("0000181B-0000-1000-8000-00805f9b34fb"), "Body Composition");
		attributes.put(UUID.fromString("0000181E-0000-1000-8000-00805f9b34fb"), "Bond Management");
		attributes.put(UUID.fromString("0000181F-0000-1000-8000-00805f9b34fb"), "Continuous Glucose Monitoring");
		attributes.put(UUID.fromString("00001805-0000-1000-8000-00805f9b34fb"), "Current Time Service");
		attributes.put(UUID.fromString("00001818-0000-1000-8000-00805f9b34fb"), "Cycling Power");
		attributes.put(UUID.fromString("00001816-0000-1000-8000-00805f9b34fb"), "Cycling Speed and Cadence");
		attributes.put(UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb"), "Device Information");
		attributes.put(UUID.fromString("0000181A-0000-1000-8000-00805f9b34fb"), "Environmental Sensing");
		attributes.put(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"), "Generic Access");
		attributes.put(UUID.fromString("00001801-0000-1000-8000-00805f9b34fb"), "Generic Attribute");
		attributes.put(UUID.fromString("00001808-0000-1000-8000-00805f9b34fb"), "Glucose");
		attributes.put(UUID.fromString("00001809-0000-1000-8000-00805f9b34fb"), "Health Thermometer");
		attributes.put(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb"), "Heart Rate");
		attributes.put(UUID.fromString("00001812-0000-1000-8000-00805f9b34fb"), "Human Interface Device");
		attributes.put(UUID.fromString("00001802-0000-1000-8000-00805f9b34fb"), "Immediate Alert");
		attributes.put(UUID.fromString("00001803-0000-1000-8000-00805f9b34fb"), "Link Loss");
		attributes.put(UUID.fromString("00001819-0000-1000-8000-00805f9b34fb"), "Location and Navigation");
		attributes.put(UUID.fromString("00001807-0000-1000-8000-00805f9b34fb"), "Next DST Change Service");
		attributes.put(UUID.fromString("0000180E-0000-1000-8000-00805f9b34fb"), "Phone Alert Status Service");
		attributes.put(UUID.fromString("00001806-0000-1000-8000-00805f9b34fb"), "Reference Time Update Service");
		attributes.put(UUID.fromString("00001814-0000-1000-8000-00805f9b34fb"), "Running Speed and Cadence");
		attributes.put(UUID.fromString("00001813-0000-1000-8000-00805f9b34fb"), "Scan Parameters");
		attributes.put(UUID.fromString("00001804-0000-1000-8000-00805f9b34fb"), "Tx Power");
		attributes.put(UUID.fromString("0000181C-0000-1000-8000-00805f9b34fb"), "User Data");
		attributes.put(UUID.fromString("0000181D-0000-1000-8000-00805f9b34fb"), "Weight Scale");
		attributes.put(UUID.fromString("00002900-0000-1000-8000-00805f9b34fb"), "Characteristic Extended Properties");
		attributes.put(UUID.fromString("00002901-0000-1000-8000-00805f9b34fb"), "Characteristic User Description");
		attributes.put(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), "Client Characteristic Configuration");
		attributes.put(UUID.fromString("00002903-0000-1000-8000-00805f9b34fb"), "Server Characteristic Configuration");
		attributes.put(UUID.fromString("00002904-0000-1000-8000-00805f9b34fb"), "Characteristic Presentation Format");
		attributes.put(UUID.fromString("00002905-0000-1000-8000-00805f9b34fb"), "Characteristic Aggregate Format");
		attributes.put(UUID.fromString("00002906-0000-1000-8000-00805f9b34fb"), "Valid Range");
		attributes.put(UUID.fromString("00002907-0000-1000-8000-00805f9b34fb"), "External Report Reference");
		attributes.put(UUID.fromString("00002908-0000-1000-8000-00805f9b34fb"), "Report Reference");
		attributes.put(UUID.fromString("0000290B-0000-1000-8000-00805f9b34fb"), "Environmental Sensing Configuration");
		attributes.put(UUID.fromString("0000290C-0000-1000-8000-00805f9b34fb"), "Environmental Sensing Measurement");
		attributes.put(UUID.fromString("0000290D-0000-1000-8000-00805f9b34fb"), "Environmental Sensing Trigger Setting");
		attributes.put(UUID.fromString("00002A7E-0000-1000-8000-00805f9b34fb"), "Aerobic Heart Rate Lower Limit");
		attributes.put(UUID.fromString("00002A84-0000-1000-8000-00805f9b34fb"), "Aerobic Heart Rate Upper Limit");
		attributes.put(UUID.fromString("00002A7F-0000-1000-8000-00805f9b34fb"), "Aerobic Threshold");
		attributes.put(UUID.fromString("00002A80-0000-1000-8000-00805f9b34fb"), "Age");
		attributes.put(UUID.fromString("00002A43-0000-1000-8000-00805f9b34fb"), "Alert Category ID");
		attributes.put(UUID.fromString("00002A42-0000-1000-8000-00805f9b34fb"), "Alert Category ID Bit Mask");
		attributes.put(UUID.fromString("00002A06-0000-1000-8000-00805f9b34fb"), "Alert Level");
		attributes.put(UUID.fromString("00002A44-0000-1000-8000-00805f9b34fb"), "Alert Notification Control Point");
		attributes.put(UUID.fromString("00002A3F-0000-1000-8000-00805f9b34fb"), "Alert Status");
		attributes.put(UUID.fromString("00002A81-0000-1000-8000-00805f9b34fb"), "Anaerobic Heart Rate Lower Limit");
		attributes.put(UUID.fromString("00002A82-0000-1000-8000-00805f9b34fb"), "Anaerobic Heart Rate Upper Limit");
		attributes.put(UUID.fromString("00002A83-0000-1000-8000-00805f9b34fb"), "Anaerobic Threshold");
		attributes.put(UUID.fromString("00002A73-0000-1000-8000-00805f9b34fb"), "Apparent Wind Direction ");
		attributes.put(UUID.fromString("00002A72-0000-1000-8000-00805f9b34fb"), "Apparent Wind Speed");
		attributes.put(UUID.fromString("00002A01-0000-1000-8000-00805f9b34fb"), "Appearance");
		attributes.put(UUID.fromString("00002AA3-0000-1000-8000-00805f9b34fb"), "Barometric Pressure Trend");
		attributes.put(UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb"), "Battery Level");
		attributes.put(UUID.fromString("00002A49-0000-1000-8000-00805f9b34fb"), "Blood Pressure Feature");
		attributes.put(UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb"), "Blood Pressure Measurement");
		attributes.put(UUID.fromString("00002A9B-0000-1000-8000-00805f9b34fb"), "Body Composition Feature");
		attributes.put(UUID.fromString("00002A9C-0000-1000-8000-00805f9b34fb"), "Body Composition Measurement");
		attributes.put(UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb"), "Body Sensor Location");
		attributes.put(UUID.fromString("00002AA4-0000-1000-8000-00805f9b34fb"), "Bond Management Control Point");
		attributes.put(UUID.fromString("00002AA5-0000-1000-8000-00805f9b34fb"), "Bond Management Feature");
		attributes.put(UUID.fromString("00002A22-0000-1000-8000-00805f9b34fb"), "Boot Keyboard Input Report");
		attributes.put(UUID.fromString("00002A32-0000-1000-8000-00805f9b34fb"), "Boot Keyboard Output Report");
		attributes.put(UUID.fromString("00002A33-0000-1000-8000-00805f9b34fb"), "Boot Mouse Input Report");
		attributes.put(UUID.fromString("00002AA6-0000-1000-8000-00805f9b34fb"), "Central Address Resolution");
		attributes.put(UUID.fromString("00002AA8-0000-1000-8000-00805f9b34fb"), "CGM Feature");
		attributes.put(UUID.fromString("00002AA7-0000-1000-8000-00805f9b34fb"), "CGM Measurement");
		attributes.put(UUID.fromString("00002AAB-0000-1000-8000-00805f9b34fb"), "CGM Session Run Time");
		attributes.put(UUID.fromString("00002AAA-0000-1000-8000-00805f9b34fb"), "CGM Session Start Time");
		attributes.put(UUID.fromString("00002AAC-0000-1000-8000-00805f9b34fb"), "CGM Specific Ops Control Point");
		attributes.put(UUID.fromString("00002AA9-0000-1000-8000-00805f9b34fb"), "CGM Status");
		attributes.put(UUID.fromString("00002A5C-0000-1000-8000-00805f9b34fb"), "CSC Feature");
		attributes.put(UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb"), "CSC Measurement");
		attributes.put(UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb"), "Current Time");
		attributes.put(UUID.fromString("00002A66-0000-1000-8000-00805f9b34fb"), "Cycling Power Control Point");
		attributes.put(UUID.fromString("00002A65-0000-1000-8000-00805f9b34fb"), "Cycling Power Feature");
		attributes.put(UUID.fromString("00002A63-0000-1000-8000-00805f9b34fb"), "Cycling Power Measurement");
		attributes.put(UUID.fromString("00002A64-0000-1000-8000-00805f9b34fb"), "Cycling Power Vector");
		attributes.put(UUID.fromString("00002A99-0000-1000-8000-00805f9b34fb"), "Database Change Increment");
		attributes.put(UUID.fromString("00002A85-0000-1000-8000-00805f9b34fb"), "Date of Birth");
		attributes.put(UUID.fromString("00002A86-0000-1000-8000-00805f9b34fb"), "Date of Threshold Assessment ");
		attributes.put(UUID.fromString("00002A08-0000-1000-8000-00805f9b34fb"), "Date Time");
		attributes.put(UUID.fromString("00002A0A-0000-1000-8000-00805f9b34fb"), "Day Date Time");
		attributes.put(UUID.fromString("00002A09-0000-1000-8000-00805f9b34fb"), "Day of Week");
		attributes.put(UUID.fromString("00002A7D-0000-1000-8000-00805f9b34fb"), "Descriptor Value Changed");
		attributes.put(UUID.fromString("00002A00-0000-1000-8000-00805f9b34fb"), "Device Name");
		attributes.put(UUID.fromString("00002A7B-0000-1000-8000-00805f9b34fb"), "Dew Point");
		attributes.put(UUID.fromString("00002A0D-0000-1000-8000-00805f9b34fb"), "DST Offset");
		attributes.put(UUID.fromString("00002A6C-0000-1000-8000-00805f9b34fb"), "Elevation");
		attributes.put(UUID.fromString("00002A87-0000-1000-8000-00805f9b34fb"), "Email Address");
		attributes.put(UUID.fromString("00002A0C-0000-1000-8000-00805f9b34fb"), "Exact Time 256");
		attributes.put(UUID.fromString("00002A88-0000-1000-8000-00805f9b34fb"), "Fat Burn Heart Rate Lower Limit");
		attributes.put(UUID.fromString("00002A89-0000-1000-8000-00805f9b34fb"), "Fat Burn Heart Rate Upper Limit");
		attributes.put(UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb"), "Firmware Revision String");
		attributes.put(UUID.fromString("00002A8A-0000-1000-8000-00805f9b34fb"), "First Name");
		attributes.put(UUID.fromString("00002A8B-0000-1000-8000-00805f9b34fb"), "Five Zone Heart Rate Limits");
		attributes.put(UUID.fromString("00002A8C-0000-1000-8000-00805f9b34fb"), "Gender");
		attributes.put(UUID.fromString("00002A51-0000-1000-8000-00805f9b34fb"), "Glucose Feature");
		attributes.put(UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb"), "Glucose Measurement");
		attributes.put(UUID.fromString("00002A34-0000-1000-8000-00805f9b34fb"), "Glucose Measurement Context");
		attributes.put(UUID.fromString("00002A74-0000-1000-8000-00805f9b34fb"), "Gust Factor");
		attributes.put(UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb"), "Hardware Revision String");
		attributes.put(UUID.fromString("00002A39-0000-1000-8000-00805f9b34fb"), "Heart Rate Control Point");
		attributes.put(UUID.fromString("00002A8D-0000-1000-8000-00805f9b34fb"), "Heart Rate Max");
		attributes.put(UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb"), "Heart Rate Measurement");
		attributes.put(UUID.fromString("00002A7A-0000-1000-8000-00805f9b34fb"), "Heat Index");
		attributes.put(UUID.fromString("00002A8E-0000-1000-8000-00805f9b34fb"), "Height");
		attributes.put(UUID.fromString("00002A4C-0000-1000-8000-00805f9b34fb"), "HID Control Point");
		attributes.put(UUID.fromString("00002A4A-0000-1000-8000-00805f9b34fb"), "HID Information");
		attributes.put(UUID.fromString("00002A8F-0000-1000-8000-00805f9b34fb"), "Hip Circumference");
		attributes.put(UUID.fromString("00002A6F-0000-1000-8000-00805f9b34fb"), "Humidity");
		attributes.put(UUID.fromString("00002A2A-0000-1000-8000-00805f9b34fb"), "IEEE 11073-20601 Regulatory Certification Data List");
		attributes.put(UUID.fromString("00002A36-0000-1000-8000-00805f9b34fb"), "Intermediate Cuff Pressure");
		attributes.put(UUID.fromString("00002A1E-0000-1000-8000-00805f9b34fb"), "Intermediate Temperature");
		attributes.put(UUID.fromString("00002A77-0000-1000-8000-00805f9b34fb"), "Irradiance");
		attributes.put(UUID.fromString("00002AA2-0000-1000-8000-00805f9b34fb"), "Language");
		attributes.put(UUID.fromString("00002A90-0000-1000-8000-00805f9b34fb"), "Last Name");
		attributes.put(UUID.fromString("00002A6B-0000-1000-8000-00805f9b34fb"), "LN Control Point");
		attributes.put(UUID.fromString("00002A6A-0000-1000-8000-00805f9b34fb"), "LN Feature");
		attributes.put(UUID.fromString("00002A0F-0000-1000-8000-00805f9b34fb"), "Local Time Information");
		attributes.put(UUID.fromString("00002A67-0000-1000-8000-00805f9b34fb"), "Location and Speed");
		attributes.put(UUID.fromString("00002A2C-0000-1000-8000-00805f9b34fb"), "Magnetic Declination");
		attributes.put(UUID.fromString("00002AA0-0000-1000-8000-00805f9b34fb"), "Magnetic Flux Density - 2D");
		attributes.put(UUID.fromString("00002AA1-0000-1000-8000-00805f9b34fb"), "Magnetic Flux Density - 3D");
		attributes.put(UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb"), "Manufacturer Name String");
		attributes.put(UUID.fromString("00002A91-0000-1000-8000-00805f9b34fb"), "Maximum Recommended Heart Rate");
		attributes.put(UUID.fromString("00002A21-0000-1000-8000-00805f9b34fb"), "Measurement Interval");
		attributes.put(UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb"), "Model Number String");
		attributes.put(UUID.fromString("00002A68-0000-1000-8000-00805f9b34fb"), "Navigation");
		attributes.put(UUID.fromString("00002A46-0000-1000-8000-00805f9b34fb"), "New Alert");
		attributes.put(UUID.fromString("00002A04-0000-1000-8000-00805f9b34fb"), "Peripheral Preferred Connection Parameters");
		attributes.put(UUID.fromString("00002A05-0000-1000-8000-00805f9b34fb"), "Service Changed");
		attributes.put(UUID.fromString("00002A02-0000-1000-8000-00805f9b34fb"), "Peripheral Privacy Flag");
		attributes.put(UUID.fromString("00002A50-0000-1000-8000-00805f9b34fb"), "PnP ID");
		attributes.put(UUID.fromString("00002A75-0000-1000-8000-00805f9b34fb"), "Pollen Concentration");
		attributes.put(UUID.fromString("00002A69-0000-1000-8000-00805f9b34fb"), "Position Quality");
		attributes.put(UUID.fromString("00002A6D-0000-1000-8000-00805f9b34fb"), "Pressure");
		attributes.put(UUID.fromString("00002A4E-0000-1000-8000-00805f9b34fb"), "Protocol Mode");
		attributes.put(UUID.fromString("00002A23-0000-1000-8000-00805f9b34fb"), "System ID");
		// Sample Services.
		attributes.put(BLE_UUID_DEVICE_INFORMATION_SERVICE, "Device Information Service");
		attributes.put(BLE_UUID_ITU_MEASUREMENT_SERVICE, "ITU MEASUREMENT SERVICE");
		attributes.put(BLE_UUID_ITU_ACTUATOR_SERVICE, "ITU ACTUATOR SERVICE");

		// Sample Characteristics.
		attributes.put(BLE_UUID_ITU_MEASUREMENT_VALUE_CHAR, "ITU BLE MEASUREMENT");
		attributes.put(BLE_UUID_ITU_MEASUREMENT_CONFIG_CHAR, "ITU BLE MEASUREMENT CONFIG");
		attributes.put(BLE_UUID_ITU_ACTUATOR_CHAR, "ITU BLE ACTUATOR");
		attributes.put(CLIENT_CHARACTERISTIC_CONFIG, "CLIENT CHAR CONFIG");
		attributes.put(MANUFACTOR_NAME, "Manufacturer Name String");

		appearance.put(0, "Unknown");
		appearance.put(64, "Generic Phone");
		appearance.put(128, "Generic Computer");
		appearance.put(192, "Generic Watch");
		appearance.put(193, "Watch: Sports Watch");
		appearance.put(256, "Generic Clock");
		appearance.put(320, "Generic Display");
		appearance.put(384, "Generic Remote Control");
		appearance.put(448, "Generic Eye-glasses");
		appearance.put(512, "Generic Tag");
		appearance.put(576, "Generic Keyring");
		appearance.put(640, "Generic Media Player");
		appearance.put(704, "Generic Barcode Scanner");
		appearance.put(768, "Generic Thermometer");
		appearance.put(769, "Thermometer: Ear");
		appearance.put(832, "Generic Heart rate Sensor");
		appearance.put(833, "Heart Rate Sensor: Heart Rate Belt");
		appearance.put(896, "Generic Blood Pressure");
		appearance.put(897, "Blood Pressure: Arm");
		appearance.put(898, "Blood Pressure: Wrist");
		appearance.put(960, "Human Interface Device (HID)");
		appearance.put(961, "Keyboard");
		appearance.put(962, "Mouse");
		appearance.put(963, "Joystick");
		appearance.put(964, "Gamepad");
		appearance.put(965, "Digitizer Tablet");
		appearance.put(966, "Card Reader");
		appearance.put(967, "Digital Pen");
		appearance.put(968, "Barcode Scanner");
		appearance.put(1024, "Generic Glucose Meter");
		appearance.put(1088, "Generic: Running Walking Sensor");
		appearance.put(1089, "Running Walking Sensor: In-Shoe");
		appearance.put(1090, "Running Walking Sensor: On-Shoe");
		appearance.put(1091, "Running Walking Sensor: On-Hip");
		appearance.put(1152, "Generic: Cycling");
		appearance.put(1153, "Cycling: Cycling Computer");
		appearance.put(1154, "Cycling: Speed Sensor");
		appearance.put(1155, "Cycling: Cadence Sensor");
		appearance.put(1156, "Cycling: Power Sensor");
		appearance.put(1157, "Cycling: Speed and Cadence Sensor");
		appearance.put(3136, "Generic: Pulse Oximeter");
		appearance.put(3137, "Fingertip");
		appearance.put(3138, "Wrist Worn");
		appearance.put(3200, "Generic: Weight Scale");
		appearance.put(5184, "Generic: Outdoor Sports Activity");
		appearance.put(5185, "Location Display Device");
		appearance.put(5186, "Location and Navigation Display Device");
		appearance.put(5187, "Location Pod");
		appearance.put(5188, "Location and Navigation Pod");

		flags.put(0, "LE Limited Discoverable Mode");
		flags.put(1, "LE General Discoverable Mode");
		flags.put(2, "BR/EDR Not Supported");
		flags.put(3, "Simultaneous LE and BR/EDR to Same Device Capable (Controller)");
		flags.put(4, "Simultaneous LE and BR/EDR to Same Device Capable (Host)");
		flags.put(6, "LE General Discoverable Mode AND BR/EDR Not Supported");
	}

	public static String lookup(UUID uuid, String defaultName) {
		String name = attributes.get(uuid);
		return name == null ? defaultName : name;
	}

	public static String lookupAppearance(int id) {
		return appearance.get(id);
	}

	public static String lookupFlags(int id) {
		return flags.get(id);
	}
}
