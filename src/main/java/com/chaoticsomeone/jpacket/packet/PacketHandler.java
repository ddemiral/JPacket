package com.chaoticsomeone.jpacket.packet;

public interface PacketHandler <T extends PacketData> {
	void handle(T data);
}
