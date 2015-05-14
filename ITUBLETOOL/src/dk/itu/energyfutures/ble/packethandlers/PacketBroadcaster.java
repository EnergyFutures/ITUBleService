package dk.itu.energyfutures.ble.packethandlers;

import java.util.Map;

public interface PacketBroadcaster {
	void registerPacketListner(PacketListListner listner);
	void removePacketListner(PacketListListner listner);
	Map<String, AdvertisementPacket> getPackets();
	Map<String, AdvertisementPacket> getNewBornPackets();
	void registerNewBornPacketListner(PacketListListner listner);
	void removeNewBornPacketListner(PacketListListner listner);
}
