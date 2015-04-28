package dk.itu.energyfutures.ble.packethandlers;

import java.util.Collection;
import java.util.Set;

import dk.itu.energyfutures.ble.AdvertisementPacket;

public interface PacketBroadcaster {
	void registerListner(PacketListListner listner);
	void removeListner(PacketListListner listner);
	Collection<AdvertisementPacket> getPackets();
}
