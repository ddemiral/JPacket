package com.chaoticsomeone.jpacket.packet.defaulttypes;

import com.chaoticsomeone.jpacket.common.ClientSocket;
import com.chaoticsomeone.jpacket.packet.PacketData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClientDiscoveryPacket implements PacketData {
	private final Set<UUID> newClients;

	public ClientDiscoveryPacket(Set<UUID> newClients) {
		this.newClients = newClients;
	}

	public ClientDiscoveryPacket(UUID newClient) {
		this(Set.of(newClient));
	}

	public ClientDiscoveryPacket(Map<UUID, ClientSocket> clients) {
		this(new HashSet<>(clients.keySet()));
	}

	public Set<UUID> getNewClients() {
		return newClients;
	}

	@Override
	public void process() {

	}
}
