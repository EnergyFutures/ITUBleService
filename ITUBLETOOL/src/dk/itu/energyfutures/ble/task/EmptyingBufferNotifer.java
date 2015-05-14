package dk.itu.energyfutures.ble.task;

public interface EmptyingBufferNotifer {
	public void registerEmptypingListner(EmptyingBufferListner listner);
	public void unregisterEmptypingListner(EmptyingBufferListner listner);
}
