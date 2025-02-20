package com.chaoticsomeone.jpacket.exceptions;

import com.chaoticsomeone.jpacket.packet.PacketData;

public class HandlerNotRegisteredException extends RuntimeException {
	public HandlerNotRegisteredException(Class<? extends PacketData> packetClass) {
		super(String.format("No handler registered for packet of class '%s'", packetClass.getSimpleName()));
	}
}
