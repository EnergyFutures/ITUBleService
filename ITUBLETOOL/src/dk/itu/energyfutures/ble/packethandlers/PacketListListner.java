package dk.itu.energyfutures.ble.packethandlers;

import java.util.List;

public interface PacketListListner {
	void newPacketArrived(AdvertisementPacket packet);
	void PacketsDeprecated(List<AdvertisementPacket> deprecatedPackets);
}
