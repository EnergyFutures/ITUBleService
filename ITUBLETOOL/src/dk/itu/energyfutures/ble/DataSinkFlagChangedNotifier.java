package dk.itu.energyfutures.ble;

public interface DataSinkFlagChangedNotifier {
	void registerDataSinkFlagChangedListner(DataSinkFlagChangedListner listner);
	void unRegisterDataSinkFlagChangedListner(DataSinkFlagChangedListner listner);
}
