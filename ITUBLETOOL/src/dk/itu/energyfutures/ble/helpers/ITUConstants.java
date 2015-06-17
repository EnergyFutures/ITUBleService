package dk.itu.energyfutures.ble.helpers;

import java.text.SimpleDateFormat;
import java.util.UUID;

import dk.itu.energyfutures.ble.R;
import dk.itu.energyfutures.ble.packethandlers.AdvertisementPacket;

public class ITUConstants {
	//0000-1000-8000-00805f9b34fb
	public static final UUID BLE_UUID_ITU_MEASUREMENT_SERVICE = UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_UUID_ITU_ACTUATOR_SERVICE = UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_UUID_ITU_MOTE_SERVICE = UUID.fromString("0000ffa2-0000-1000-8000-00805f9b34fb");

	public static UUID BLE_UUID_ITU_MEASUREMENT_VALUE_CHAR = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");
	public static UUID BLE_UUID_ITU_MEASUREMENT_CONFIG_CHAR = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
	public static UUID BLE_UUID_ITU_ACTUATOR_JSON_COMMAND_CHAR = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
	public static UUID BLE_UUID_ITU_ACTUATOR_JSON_CHAR = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb");
	public static UUID BLE_UUID_ITU_READ_ALL_MEASUREMENTS_VALUE_CHAR = UUID.fromString("0000ff04-0000-1000-8000-00805f9b34fb");
	public static UUID BLE_UUID_ITU_CONFIG_MOTE_CHAR = UUID.fromString("0000ff05-0000-1000-8000-00805f9b34fb");

	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("kk:mm:ss");
	public static final String DEVICE_IS_A_DATA_SINK_KEY = "DEVICE_IS_A_DATA_SINK_KEY";
	public static final String DEVICE_IS_A_PARTICIPATORY_DATA_SINK_KEY = "DEVICE_IS_A_PARTICIPATORY_DATA_SINK_KEY";
	public static final String SHOW_ADVANCE_SETTINGS_KEY = "SHOW_ADVANCE_SETTINGS_KEY";
	public static final String ENABLE_CONFIG_OF_NORMAL_MOTES = "ENABLE_CONFIG_OF_NORMAL_MOTES";
	public static final String USE_ENERGY_SAVINGS_FEATURES = "USE_ENERGY_SAVINGS_FEATURES";

	public static enum ITU_SENSOR_COORDINATE {
		LOCATION_NOT_SET,
		LOCATION_IN_SOMEWHERE,
		LOCATION_IN_FLOOR,
		LOCATION_IN_MIDDLE,
		LOCATION_IN_CEILING,
		LOCATION_OUT
	}

	public static final ITU_SENSOR_COORDINATE[] ITU_SENSOR_COORDINATE_ARRAY = ITU_SENSOR_COORDINATE.values();

	// BLE_UUID_ITU_SENSOR_TYPE_HUMIDITY,
	public static enum ITU_SENSOR_TYPE {
		NOT_SET,
		TEMPERATURE,
		LIGHT,
		SOUND,
		HUMIDITY,
		MOTION,
		ACTUATOR_TYPE_NOT_SET,
		WINDOW, 
		AC,
		AMPERE,
		JSON
	}

	public static final ITU_SENSOR_TYPE[] ITU_SENSOR_TYPE_ARRAY = ITU_SENSOR_TYPE.values();

	public static enum ITU_SENSOR_MAKE {
		NOT_SET,
		DHT22, 
		TMP36,
		LMT85,
		TSL2561,
		SI7021,
		EKMB1303112,
		ACTUATOR_MAKE_NOT_SET,
		SSD_RELAY,
		BISTABLE_RELAY,
		MECH_RELAY,
		ACS712_5A,
		ACS712_20A
	}
	
	public static final ITU_SENSOR_MAKE[] ITU_SENSOR_MAKE_ARRAY = ITU_SENSOR_MAKE.values();

	public static enum ITU_SENSOR_CONFIG_TYPE {
		SENSOR_TYPE,
		ACTUATOR_TYPE
	}

	public static final ITU_SENSOR_CONFIG_TYPE[] ITU_SENSOR_CONFIG_TYPE_ARRAY = ITU_SENSOR_CONFIG_TYPE.values();

	public static String getCoordinateStringFromEnum(AdvertisementPacket advertisementPacket) {
		if (ITU_SENSOR_TYPE.WINDOW.equals(advertisementPacket.getSensorType())) {
			return "";
		}
		if(advertisementPacket.getLocation().contains("4D23")){
			System.out.println();
		}
		ITU_SENSOR_COORDINATE coordinate = advertisementPacket.getCoordinate();
		switch (coordinate) {
		case LOCATION_IN_SOMEWHERE:
			return "IN|SOMEWHE";
		case LOCATION_IN_FLOOR:
			return "IN|FLOOR";
		case LOCATION_IN_MIDDLE:
			return "IN|MIDDLE";
		case LOCATION_IN_CEILING:
			return "IN|CEILING";
		case LOCATION_OUT:
			return "OUTSIDE";
		default:
			return "NOT SET";
		}
	}

	public static String getValueStringFromEnum(AdvertisementPacket advertisementPacket) {
		if (ITU_SENSOR_TYPE.WINDOW.equals(advertisementPacket.getSensorType())) {
			return "";
		}

		if (ITU_SENSOR_TYPE.AC.equals(advertisementPacket.getSensorType())) {
			return advertisementPacket.getValue() > 0 ? "ON" : "OFF";
		}
		double value = advertisementPacket.getValue();
		if (((int)value) == 167772) {
			return "Not ready";
		}
		return "" + value;
	}

	public static int findIconBySensorType(AdvertisementPacket advertisementPacket) {
		return findIconBySensorType(advertisementPacket.getSensorType());
	}
	
	public static int findIconBySensorType(int typeIndex) {
		return findIconBySensorType(ITU_SENSOR_TYPE_ARRAY[typeIndex]);
	}
	
	private static int findIconBySensorType(ITU_SENSOR_TYPE type) {
		switch (type) {
		case TEMPERATURE:
			return R.drawable.temp_icon;
		case LIGHT:
			return R.drawable.light_icon;
		case SOUND:
			return R.drawable.sound_icon;
		case HUMIDITY:
			return R.drawable.humidity_icon;
		case MOTION:
			return R.drawable.motion_icon;
		case AC:
			return R.drawable.ac_icon;
		case WINDOW:
			return R.drawable.window_icon;
		case AMPERE:
			return R.drawable.current_icon;
		default:
			return R.drawable.unknown_icon;
		}
	}
}
