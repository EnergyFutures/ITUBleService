package dk.itu.energyfutures.ble;

import java.util.List;
import java.util.Set;

public interface NewPacketBroadcaster {
	void registerListner(NewPacketListner listner);
	void removeListner(NewPacketListner listner);
	Set<AdvertisementPacket> getPackets();
}
