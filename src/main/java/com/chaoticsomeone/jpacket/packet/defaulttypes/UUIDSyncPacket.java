package com.chaoticsomeone.jpacket.packet.defaulttypes;

import com.chaoticsomeone.jpacket.packet.PacketData;

import java.util.UUID;

public class UUIDSyncPacket implements PacketData {
	private final UUID uuid;
	private int state;

	public UUIDSyncPacket(UUID uuid) {
		this.uuid = uuid;
	}

	public UUIDSyncPacket acknowledge() {
		UUIDSyncPacket updatedPacket = new UUIDSyncPacket(uuid);
		updatedPacket.setState(state + 1);
		return updatedPacket;
	}

	public UUID getUuid() {
		return uuid;
	}

	public int getState() {
		return state;
	}

	private void setState(int state) {
		this.state = state;
	}

	@Override
	public void process() {

	}
}
