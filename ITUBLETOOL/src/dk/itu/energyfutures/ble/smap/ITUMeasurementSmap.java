package dk.itu.energyfutures.ble.smap;


public class ITUMeasurementSmap {
	public int id;
	public double value;
	public long timeStamp;
	public ITUMeasurementSmap(int id, double value) {
		super();
		this.id = id;
		this.value = value;
		timeStamp = System.currentTimeMillis();
	}
}
