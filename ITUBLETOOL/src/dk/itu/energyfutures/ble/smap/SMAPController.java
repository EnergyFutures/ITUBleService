package dk.itu.energyfutures.ble.smap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.sensorhandlers.MoteConfigParser;
import dk.itu.energyfutures.ble.sensorhandlers.SensorParser;
import android.util.Log;

public class SMAPController {
	private final static String TAG = SMAPController.class.getSimpleName();
	private static ExecutorService executor = Executors.newFixedThreadPool(3);
	private static final String POST_READINGS_URL = "http://130.226.142.195:8888/api/v1/bleot/addreadings";
	private static final String GET_IDS_URL = "http://130.226.142.195:8888/api/v1/bleot/generateids";
	private static final String POST_META_URL = "http://130.226.142.195:8888/api/v1/bleot/addmeta";
	private static final Map<String, int[]> idsMap = new HashMap<String, int[]>();
	public static String payload = null;
	public static void postReadingsToSmap(final byte[] data, final int length) {
		executor.submit(new Runnable() {
			String myPayload = "";
			@Override
			public void run() {
				try {
					long time = System.currentTimeMillis();
					Collection<MeasurementSmapContainer> measurementSmapContainers = MeasurementSmapContainer.processData(data, length);
					for (MeasurementSmapContainer msc : measurementSmapContainers) {
						JSONObject container = new JSONObject();
						container.put("ID", msc.id);
						container.put("Time", time);
						JSONArray readings = new JSONArray();
						for (int i = 0; i < msc.loop; i++) {
							JSONArray reading = new JSONArray();
							reading.put(msc.seqNrs[i]);
							reading.put(msc.values[i]);
							readings.put(reading);
						}
						container.put("Readings", readings);
						HttpPost httpost = new HttpPost(POST_READINGS_URL);
						myPayload += container.toString();
						StringEntity se = new StringEntity(container.toString());
						httpost.setEntity(se);
						httpost.setHeader("Content-type", "application/json");
						HttpResponse response = new DefaultHttpClient().execute(httpost);
						if (response.getStatusLine().getStatusCode() != 200) {
							Log.e(TAG, "Error post meta values to server with response: " + response.getStatusLine().getStatusCode());
						}
					}
					Log.i(TAG, "PAYLOAD: " + myPayload);
					payload = myPayload;
				}
				catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "Error post value to server due to exception: " + e.getMessage());
				}
			}
		});
	}
	
	public static void fetchIdsFromSmap(final int count, final String adr) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					HttpPost httpost = new HttpPost(GET_IDS_URL);
					JSONObject idText = new JSONObject();
					idText.put("number", count);
					StringEntity se = new StringEntity(idText.toString());
					httpost.setEntity(se);
					httpost.setHeader("Content-type", "application/json");
					HttpResponse response = new DefaultHttpClient().execute(httpost);
					if (response.getStatusLine().getStatusCode() != 200) {
						Log.e(TAG, "Error post value to server");
						idsMap.put(adr, null);
						return;
					}
					JSONArray jsonResponse = new JSONArray(org.apache.http.util.EntityUtils.toString(response.getEntity(), "UTF-8"));
					int[] ids = new int[count];
					for (int i = 0; i < count; i++) {
						ids[i] = jsonResponse.getInt(i);
					}
					idsMap.put(adr, ids);
				}
				catch (Exception e) {
					Log.e(TAG, "Error post value to server due to exception");
					e.printStackTrace();
					idsMap.put(adr, null);
				}
			}
		});
	}
	
	/**
	 * This call will return the array of ids (ints) if they were succesfully fetched from the server.
	 * A call to fetchIdsFromSmap should have been made prior to this methode-call
	 * @param adr The mac adr of the device
	 * @return The array of ids if they were succusfully fetched, or null
	 */
	public static int[] getIdsForAdr(String adr){
		return idsMap.get(adr);
	}
	
	public static void postMetaDataToSmap(final MoteConfigParser moteConfig, final List<SensorParser> sensorParsers, final String deviceAdr){
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					JSONArray containers = new JSONArray();
					for(SensorParser p : sensorParsers){
						JSONObject container = new JSONObject();
						container.put("ID", p.getId());
						container.put("Name", moteConfig.getDeviceName());
						container.put("Kind", "Sensor");
						container.put("Servicetype", ITUConstants.ITU_SENSOR_TYPE_ARRAY[p.getSensorType()]);
						container.put("Servicemake", ITUConstants.ITU_SENSOR_MAKE_ARRAY[p.getSensorMake()]);
						container.put("Samplefrequency", p.getSampleFrequency());
						container.put("Location", moteConfig.getLocationName());
						container.put("Coordinates", ITUConstants.ITU_SENSOR_COORDINATE_ARRAY[p.getCoordinateIndex()]);
						container.put("Mote", deviceAdr);
						containers.put(container);
					}
					HttpPost httpost = new HttpPost(POST_META_URL);
					String payload = containers.toString();
					Log.i(TAG,"PAYLOAD: " + payload);
					StringEntity se = new StringEntity(payload);
					httpost.setEntity(se);
					httpost.setHeader("Content-type", "application/json");
					HttpResponse response = new DefaultHttpClient().execute(httpost);
					if (response.getStatusLine().getStatusCode() != 201) {
						Log.e(TAG, "Error post meta values to server with response: " + response.getStatusLine().getStatusCode());
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "Error post value to server due to exception: " +  e.getMessage());
				}
			}
		});
	}
}
