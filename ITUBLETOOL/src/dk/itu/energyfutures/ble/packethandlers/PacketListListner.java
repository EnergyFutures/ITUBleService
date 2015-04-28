package dk.itu.energyfutures.ble.packethandlers;

import java.util.List;

import dk.itu.energyfutures.ble.AdvertisementPacket;

public interface PacketListListner {
	void newPacketArrived(AdvertisementPacket packet);
	void PacketsDeprecated(List<AdvertisementPacket> deprecatedPackets);
}
