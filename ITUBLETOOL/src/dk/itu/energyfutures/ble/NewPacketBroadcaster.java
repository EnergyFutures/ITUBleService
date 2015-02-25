package dk.itu.energyfutures.ble;

public interface NewPacketBroadcaster {
	void registerListner(NewPacketListner listner);
	void removeListner(NewPacketListner listner);
}
