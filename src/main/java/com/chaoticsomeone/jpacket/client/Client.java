package com.chaoticsomeone.jpacket.client;

import com.chaoticsomeone.jpacket.common.ClientSocket;
import com.chaoticsomeone.jpacket.common.PacketIO;
import com.chaoticsomeone.jpacket.packet.Packet;
import com.chaoticsomeone.jpacket.packet.PacketData;
import com.chaoticsomeone.jpacket.packet.PacketDispatcher;
import com.chaoticsomeone.jpacket.packet.defaulttypes.TerminatePacket;
import com.chaoticsomeone.jpacket.packet.defaulttypes.UUIDSyncPacket;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class Client {
	private final PacketDispatcher dispatcher;
	private ClientSocket socket;
	private UUID uuid;

	private volatile boolean isRunning;

	public Client(PacketDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	public void connect(String host, int port) throws IOException {
		socket = new ClientSocket(host, port);
		isRunning = true;

		startListening();
	}

	public void sendPacket(PacketData data) throws IOException {
		PacketIO.getInstance().sendPacket(data, socket, dispatcher);
	}

	private void receivePacket() {
		try {
			Optional<PacketData> data = PacketIO.getInstance().readFromStream(socket.getIn(), dispatcher);

			if (data.isPresent() && data.get() instanceof TerminatePacket) {
				close();
			} else if (data.isPresent() && data.get() instanceof UUIDSyncPacket packet) {
				uuid = packet.getUuid();
				PacketIO.getInstance().sendPacket(packet.acknowledge(), socket, dispatcher);
			} else {
				data.ifPresent(dispatcher::dispatch);
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void startListening() {
		new Thread(() -> {
			while (isRunning) {
				try {
					if (!socket.isClosed() && socket.areBytesAvailable()) {
						receivePacket();
						Thread.sleep(10);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void disconnect() {
		try {
			PacketIO.getInstance().sendPacket(new TerminatePacket(), socket, dispatcher);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void close() throws IOException {
		isRunning = false;
		socket.close();
	}

	public PacketDispatcher getDispatcher() {
		return dispatcher;
	}

	public ClientSocket getSocket() {
		return socket;
	}

	public boolean isRunning() {
		return isRunning;
	}
}
