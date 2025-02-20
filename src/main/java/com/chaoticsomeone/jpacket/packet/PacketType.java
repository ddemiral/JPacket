package com.chaoticsomeone.jpacket.packet;

public class PacketType {
	private final int id;
	private final Class<? extends PacketData> targetClass;
	private final PacketHandler<?> handler;

	public PacketType(int id, Class<? extends PacketData> targetClass, PacketHandler<?> handler) {
		this.id = id;
		this.targetClass = targetClass;
		this.handler = handler;
	}

	public Class<? extends PacketData> getTargetClass() {
		return targetClass;
	}

	public int getId() {
		return id;
	}

	public PacketHandler<?> getHandler() {
		return handler;
	}
}
