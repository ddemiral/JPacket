package com.chaoticsomeone.jpacket.packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Packet <T extends PacketData> {
	private final T data;
	private final int type;

	public Packet(T data, int type) {
		this.data = data;
		this.type = type;
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
}
