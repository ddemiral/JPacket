package com.chaoticsomeone.jpacket.packet;

import com.chaoticsomeone.jpacket.packet.defaulttypes.*;

import java.util.HashMap;
import java.util.Map;

public class PacketTypeRegistry {
	public static final int CUSTOM_ID_START = 1024;
	private int nextId;

	private final Map<Class<? extends PacketData>, PacketType> typeRegistry = new HashMap<>();

	public PacketTypeRegistry() {
		nextId = CUSTOM_ID_START;

		register(16, TerminatePacket.class, null);
		register(32, UUIDSyncPacket.class, null);
		register(48, ClientDiscoveryPacket.class, null);
		register(64, ResponsePacket.class, new ResponseHandler());

	}

	public void register(Class<? extends PacketData> targetClass, PacketHandler handler) {
		register(getNextId(), targetClass, handler);
	}

	private void register(int typeId, Class<? extends PacketData> targetClass, PacketHandler handler) {
		if (typeRegistry.containsKey(targetClass)) {
			return;
		}

		typeRegistry.put(targetClass, new PacketType(typeId, targetClass, handler));
	}

	public PacketType getType(Class<? extends PacketData> packetClass) {
		return typeRegistry.get(packetClass);
	}

	public PacketHandler findHandler(PacketData type) {
		return typeRegistry.get(type.getClass()).getHandler();
	}

	public int findTypeId(PacketData data) {
		Class<? extends PacketData> type = data.getClass();
		return typeRegistry.get(type).getId();
	}

	public PacketType idToType(int id) {
		for (PacketType type : typeRegistry.values()) {
			if (type.getId() == id) {
				return type;
			}
		}

		return null;
	}

	private int getNextId() {
		return nextId++;
	}
}
