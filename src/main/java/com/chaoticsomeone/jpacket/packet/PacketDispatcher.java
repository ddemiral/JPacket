package com.chaoticsomeone.jpacket.packet;

import com.chaoticsomeone.jpacket.exceptions.HandlerNotRegisteredException;


public class PacketDispatcher {
	private final PacketTypeRegistry registry;

	public PacketDispatcher(PacketTypeRegistry registry) {
		this.registry = registry;
	}

	public <T extends PacketData> void dispatch(Packet<T> packet) {
		T data = packet.getData();
		PacketType type = registry.getType(data.getClass());
		PacketHandler<T> handler = (PacketHandler<T>) type.getHandler();

		if (handler != null) {
			handler.handle(data);
		} else if (type.getId() >= PacketTypeRegistry.CUSTOM_ID_START) {
			throw new HandlerNotRegisteredException(packet.getClass());
		}
	}

	public int findTypeId(PacketData data) {
		return registry.findTypeId(data);
	}

	public PacketType idToType(int id) {
		return registry.idToType(id);
	}
}
