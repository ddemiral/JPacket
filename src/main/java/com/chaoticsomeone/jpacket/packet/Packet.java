package com.chaoticsomeone.jpacket.packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class Packet <T extends PacketData> {
	private final T data;
	private final int type;
	private final UUID destinationUUID;

	public Packet(T data, int type, UUID destinationUUID) {
		this.data = data;
		this.type = type;
		this.destinationUUID = destinationUUID;
	}

	public byte[] getBytes() throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(data);
		objectStream.close();
		return byteStream.toByteArray();
	}

	public T getData() {
		return data;
	}

	public int getType() {
		return type;
	}

	public UUID getDestinationUUID() {
		return destinationUUID;
	}
}
