package dk.itu.energyfutures.ble.helpers;

import java.text.SimpleDateFormat;
import java.util.UUID;

import dk.itu.energyfutures.ble.AdvertisementPacket;
import dk.itu.energyfutures.ble.R;

public class ITUConstants {
	//
	public static final UUID BLE_UUID_ITU_BASE = UUID.fromString("00000000-3444-3231-3454-484557494E21");
	
	public static final UUID BLE_UUID_ITU_MEASUREMENT_SERVICE = UUID.fromString("0000ffa0-3444-3231-3454-484557494E21");
	public static final UUID BLE_UUID_ITU_ACTUATOR_SERVICE = UUID.fromString("0000ffa1-3444-3231-3454-484557494E21");
	public static final UUID BLE_UUID_ITU_READ_ALL_MEASUREMENT_SERVICE = UUID.fromString("0000ffa2-0000-1000-8000-00805f9b34fb");
	//public static final UUID BLE_UUID_ITU_READ_ALL_MEASUREMENT_SERVICE = UUID.fromString("0000ffa2-3444-3231-3454-484557494E21");
	

	public static UUID BLE_UUID_ITU_MEASUREMENT_VALUE_CHAR = UUID.fromString("0000ff00-3444-3231-3454-484557494E21");
	public static UUID BLE_UUID_ITU_MEASUREMENT_CONFIG_CHAR = UUID.fromString("0000ff01-3444-3231-3454-484557494E21");
	public static UUID BLE_UUID_ITU_ACTUATOR_COMMAND_CHAR = UUID.fromString("0000ff02-3444-3231-3454-484557494E21");
	public static UUID BLE_UUID_ITU_ACTUATOR_JSON_CHAR = UUID.fromString("0000ff03-3444-3231-3454-484557494E21");
	//public static UUID BLE_UUID_ITU_READ_ALL_MEASUREMENT_VALUE_CHAR = UUID.fromString("0000ff04-3444-3231-3454-484557494E21");
	public static UUID BLE_UUID_ITU_READ_ALL_MEASUREMENT_VALUE_CHAR = UUID.fromString("0000ff04-0000-1000-8000-00805f9b34fb");

	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("kk:mm:ss"); 
	
	
	public static enum ITU_MOTE_COORDINATE {
		ITS_MEAS_LOCATION_NOT_SET,ITS_MEAS_LOCATION_IN_SOMEWHERE,ITS_MEAS_LOCATION_IN_FLOOR, ITS_MEAS_LOCATION_IN_MIDDLE,ITS_MEAS_LOCATION_IN_CEILING,ITS_MEAS_LOCATION_OUT
	}
	public static final ITU_MOTE_COORDINATE[] ITU_MOTE_COORDINATE_ARRAY = ITU_MOTE_COORDINATE.values();
//BLE_UUID_ITU_SENSOR_TYPE_HUMIDITY,
	public static enum ITU_SENSOR_TYPE {
		BLE_UUID_ITU_SENSOR_TYPE_NOT_SET, BLE_UUID_ITU_SENSOR_TYPE_TEMPERATURE, BLE_UUID_ITU_SENSOR_TYPE_LIGHT, BLE_UUID_ITU_SENSOR_TYPE_SOUND,BLE_UUID_ITU_MULTI_TYPE,BLE_UUID_ITU_ACTUATOR_TYPE_NOT_SET
,BLE_UUID_ITU_ACTUATOR_TYPE_WINDOW,	BLE_UUID_ITU_ACTUATOR_TYPE_AC}
	
	public static final ITU_SENSOR_TYPE[] ITU_SENSOR_TYPE_ARRAY = ITU_SENSOR_TYPE.values();
	
	public static enum ITU_SENSOR_MAKE {
		BLE_UUID_ITU_SENSOR_MAKE_NOT_SET,BLE_UUID_ITU_SENSOR_MAKE_DHT22,BLE_UUID_ITU_SENSOR_MAKE_TMP36,BLE_UUID_ITU_SENSOR_MAKE_LMT85,BLE_UUID_ITU_SENSOR_MAKE_TSL2561,BLE_UUID_ITU_ACTUATOR_MAKE_NOT_SET,BLE_UUID_ITU_ACTUATOR_MAKE_SSD_RELAY,BLE_UUID_ITU_ACTUATOR_MAKE_BISTABLE_RELAY,BLE_UUID_ITU_ACTUATOR_MAKE_MECH_RELAY
	}
	
	public static enum ITU_MOTE_TYPE {
		BLE_UUID_ITU_MOTE_SENSOR_TYPE,BLE_UUID_ITU_MOTE_ACTUATOR_TYPE,BLE_UUID_ITU_MOTE_MULTI_SENSOR_TYPE,BLE_UUID_ITU_MOTE_MULTI_ACTUATOR_TYPE,BLE_UUID_ITU_MOTE_MIXED_TYPE
	}
	public static final ITU_MOTE_TYPE[] ITU_MOTE_TYPE_ARRAY = ITU_MOTE_TYPE.values();
	
	public static String getCoordinateStringFromEnum(AdvertisementPacket advertisementPacket){
		if(ITU_SENSOR_TYPE.BLE_UUID_ITU_ACTUATOR_TYPE_WINDOW.equals(advertisementPacket.getSensorType())){
			return "";
		}			
		ITU_MOTE_COORDINATE coordinate = advertisementPacket.getCoordinate();
		switch (coordinate) {
		case ITS_MEAS_LOCATION_IN_SOMEWHERE:
			return "IN|SOMEWHERE";
		case ITS_MEAS_LOCATION_IN_FLOOR:
			return "IN|FLOOR";
		case ITS_MEAS_LOCATION_IN_MIDDLE:
			return "IN|MIDDLE";
		case ITS_MEAS_LOCATION_IN_CEILING:
			return "IN|CEILING";
		case ITS_MEAS_LOCATION_OUT:
			return "OUTSIDE";
		default:
			return "NOT SET";
		}
	}
	
	public static String getValueStringFromEnum(AdvertisementPacket advertisementPacket){
		if(ITU_SENSOR_TYPE.BLE_UUID_ITU_ACTUATOR_TYPE_WINDOW.equals(advertisementPacket.getSensorType())){
			return "";
		}	
		
		if(ITU_SENSOR_TYPE.BLE_UUID_ITU_ACTUATOR_TYPE_AC.equals(advertisementPacket.getSensorType())){
			return advertisementPacket.getValue() > 0 ? "ON" : "OFF";
		}
		double value = advertisementPacket.getValue();
		if(value == 167772.15){
			return "Not ready";
		}
		return "" +value;
	}
	
	public static int findIconBySensorType(AdvertisementPacket advertisementPacket){
		switch (advertisementPacket.getSensorType()) {
		case BLE_UUID_ITU_SENSOR_TYPE_TEMPERATURE :
			return R.drawable.temp_icon;
		case BLE_UUID_ITU_SENSOR_TYPE_LIGHT :
			return R.drawable.light_icon;
		case BLE_UUID_ITU_SENSOR_TYPE_SOUND :
			return R.drawable.sound_icon;
		case BLE_UUID_ITU_ACTUATOR_TYPE_AC :
			return R.drawable.ac_icon;
		case BLE_UUID_ITU_ACTUATOR_TYPE_WINDOW :
			return R.drawable.window_icon;
		default:
			return R.drawable.unknown_icon;
		}
	}
}
