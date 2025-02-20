package com.chaoticsomeone.jpacket.common;

import com.chaoticsomeone.jpacket.packet.Packet;
import com.chaoticsomeone.jpacket.packet.PacketData;
import com.chaoticsomeone.jpacket.packet.PacketDispatcher;
import com.chaoticsomeone.jpacket.packet.PacketType;

import java.io.*;
import java.util.Optional;
import java.util.UUID;

public class PacketIO {
	public static final UUID DESTINATION_SERVER = new UUID(0, 0);
	public static final UUID DESTINATION_BROADCAST = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);

	private static final PacketIO instance = new PacketIO(1_000_000);

	private final int maxPacketLength;

	private PacketIO(int maxPacketLength) {
		this.maxPacketLength = maxPacketLength;
	}

	public void sendPacket(PacketData data, ClientSocket socket, PacketDispatcher dispatcher, UUID destination) throws IOException {
		Packet<PacketData> packet = new Packet<>(data, dispatcher.findTypeId(data), destination);
		writeToStream(packet, socket.getOut());
	}

	public void writeToStream(Packet<PacketData> packet, DataOutputStream out) throws IOException {
		UUID destination = packet.getDestinationUUID();
		byte[] bytes = packet.getBytes();
		out.writeInt(packet.getType());
		out.writeLong(destination.getMostSignificantBits());
		out.writeLong(destination.getLeastSignificantBits());
		out.writeInt(bytes.length);
		out.write(bytes);
		out.flush();
	}

	public Optional<Packet<PacketData>> readFromStream(DataInputStream in, PacketDispatcher dispatcher) throws IOException, ClassNotFoundException {
		if (in.available() <= 0) {
			return Optional.empty();
		}

		int packetType = in.readInt();
		int length = in.readInt();
		long leastSignificantBits = in.readLong();
		long mostSignificantBits = in.readLong();
		UUID destination = new UUID(mostSignificantBits, leastSignificantBits);

		if (length < 0 || length > maxPacketLength) {
			in.skipBytes(length);
			return Optional.empty();
		}

		byte[] buffer = new byte[length];
		in.readFully(buffer);

		PacketData data = deserialize(dispatcher.idToType(packetType), buffer);
		return Optional.of(new Packet<>(data, packetType, destination));
	}

	private PacketData deserialize(PacketType type, byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		return type.getTargetClass().cast(objectStream.readObject());
	}

	public static PacketIO getInstance() {
		return instance;
	}
}
