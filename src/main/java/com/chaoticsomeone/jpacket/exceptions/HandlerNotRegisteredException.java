package com.chaoticsomeone.jpacket.exceptions;

public class HandlerNotRegisteredException extends RuntimeException {
	public HandlerNotRegisteredException(Class<?> packetClass) {
		super(String.format("No handler registered for packet of class '%s'", packetClass.getSimpleName()));
	}
}
