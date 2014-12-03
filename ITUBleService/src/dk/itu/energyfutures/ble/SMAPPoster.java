package dk.itu.energyfutures.ble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class SMAPPoster {
	private final static String TAG = BluetoothLeService.class.getSimpleName();
	private static LinkedBlockingQueue<ITUMeasurement> measurements = new LinkedBlockingQueue<ITUMeasurement>();
	private static final Map<Integer, String> ids = new HashMap<Integer, String>();
	private static final Map<Integer, String> paths = new HashMap<Integer, String>();
	private static ExecutorService executor = Executors.newSingleThreadExecutor();
	private static final String url = "http://130.226.142.195/backend/add/tp0sgjVoKNQZmk6HzgAjGfGMqlGcQQ68C45J";
	static {
		ids.put(1, "43571867-b4fa-5b44-afcf-011486b2f277");
		ids.put(2, "43571867-b4fa-5b44-afcf-011486b2f288");
		ids.put(3, "43571867-b4fa-5b44-afcf-011486b2f299");
		ids.put(4, "c9db1ba0-7aed-11e4-b116-123b93f75cba");
		paths.put(1, "/ITU/4D21/BluetoothNode1/Humidity");
		paths.put(2, "/ITU/4D21/BluetoothNode1/Temperature");
		paths.put(3, "/ITU/4D21/BluetoothNode1/Sound");
		paths.put(4, "/ITU/4D21/BluetoothNode2/Temperature");
	}

	public static void submitMeasurement(int id, double value) {
		measurements.offer(new ITUMeasurement(id, value));
		processIfEnoughMeasurements();
	}

	private static void processIfEnoughMeasurements() {
		int size = measurements.size();
		Log.v(TAG, "Measurements in list: " + size);
		if (size >= 100) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					List<ITUMeasurement> allItems = new ArrayList<ITUMeasurement>();
					measurements.drainTo(allItems);
					Map<Integer, ArrayList<ITUMeasurement>> idToMeasurementMap = new HashMap<Integer, ArrayList<ITUMeasurement>>();
					for (ITUMeasurement m : allItems) {
						ArrayList<ITUMeasurement> list = idToMeasurementMap.get(m.id);
						if (list == null) {
							list = new ArrayList<ITUMeasurement>();
							idToMeasurementMap.put(m.id, list);
						}
						list.add(m);
					}
					allItems = null;

					for (Integer key : idToMeasurementMap.keySet()) {
						try {
							HttpPost httpost = new HttpPost(url);
							JSONArray readings = new JSONArray();
							ArrayList<ITUMeasurement> list = idToMeasurementMap.get(key);
							for (ITUMeasurement m : list) {
								if (m.value > 160000) continue;
								JSONArray reading = new JSONArray();
								reading.put(m.timeStamp);
								int x = (int) (m.value * 100);
								reading.put(x / 100.0);
								readings.put(reading);
							}
							JSONObject measurement = new JSONObject();
							measurement.put("Readings", readings);
							int id = list.get(0).id;
							measurement.put("uuid", ids.get(id));
							JSONObject holder = new JSONObject();
							holder.put(paths.get(id), measurement);
							StringEntity se = new StringEntity(holder.toString());
							Log.v(TAG, holder.toString());
							httpost.setEntity(se);
							httpost.setHeader("Content-type", "application/json");
							HttpResponse response = new DefaultHttpClient().execute(httpost);
							System.out.println(response.getStatusLine());
							if (response.getStatusLine().getStatusCode() != 200) {
								Log.e(TAG, "Error post value to server");
							} else {
								Log.v(TAG, "Success sending value to server");
							}
						}
						catch (Exception e) {
							Log.e(TAG, "Error post value to server due to exception");
							e.printStackTrace();
						}
					}
				}
			});
		}
	}
}
