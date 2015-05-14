package dk.itu.energyfutures.ble.smap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import dk.itu.energyfutures.ble.helpers.BluetoothHelper;


public class MeasurementSmapContainer {
	public int id;
	public int[] seqNrs;
	public double[] values;
	public int loop;

	private MeasurementSmapContainer() {
		super();
	}

	public static Collection<MeasurementSmapContainer> processData(byte[] data, int length) {
		Map<Integer,MeasurementSmapContainer> map = new HashMap<Integer, MeasurementSmapContainer>();
		int i = 0;
		int size = length / 8;
		while(i < length){
			int valueInt = BluetoothHelper.unsignedBytesTo32Int(data, i);
			double value = BluetoothHelper.getIEEEFloatValue(valueInt);
			i += 4;
			int seqnr = BluetoothHelper.unsignedBytesTo16Int(data, i);
			i += 2;
			int id = BluetoothHelper.unsignedBytesTo16Int(data, i);
			i += 2;
			MeasurementSmapContainer msc;
			if(map.containsKey(id)){
				msc = map.get(id);
			}else{
				 msc = new MeasurementSmapContainer();
				 msc.seqNrs = new int[size];
				 msc.values = new double[size];
				 msc.id = id;
				 map.put(id, msc);
			}
			msc.seqNrs[msc.loop] = seqnr;
			msc.values[msc.loop] = value;
			msc.loop++;
		}		
		return map.values();
	}
}
