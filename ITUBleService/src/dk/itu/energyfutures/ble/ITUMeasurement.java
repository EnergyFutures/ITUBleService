package dk.itu.energyfutures.ble;


public class ITUMeasurement {
	public int id;
	public double value;
	public long timeStamp;
	public ITUMeasurement(int id, double value) {
		super();
		this.id = id;
		this.value = value;
		timeStamp = System.currentTimeMillis();
	}
}
