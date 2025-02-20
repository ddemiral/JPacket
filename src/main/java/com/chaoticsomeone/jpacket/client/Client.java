package com.chaoticsomeone.jpacket.client;

import com.chaoticsomeone.jpacket.common.ClientSocket;
import com.chaoticsomeone.jpacket.common.ConditionalRunner;
import com.chaoticsomeone.jpacket.common.PacketIO;
import com.chaoticsomeone.jpacket.packet.PacketData;
import com.chaoticsomeone.jpacket.packet.PacketDispatcher;
import com.chaoticsomeone.jpacket.packet.defaulttypes.ClientDiscoveryPacket;
import com.chaoticsomeone.jpacket.packet.defaulttypes.TerminatePacket;
import com.chaoticsomeone.jpacket.packet.defaulttypes.UUIDSyncPacket;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Client {
	private final PacketDispatcher dispatcher;
	private ClientSocket socket;
	private UUID uuid;
	private final Set<UUID> knownClients = new HashSet<>();

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
		sendPacket(data, PacketIO.DESTINATION_SERVER);
	}

	public void sendPacket(PacketData data, UUID destination) throws IOException {
		PacketIO.getInstance().sendPacket(data, socket, dispatcher, destination);
	}

	private void receivePacket() {
		try {
			Optional<PacketData> data = PacketIO.getInstance().readFromStream(socket.getIn(), dispatcher);

			ConditionalRunner.run(data.isPresent(), () -> {
				if (data.get() instanceof TerminatePacket) {
					close();
				} else if (data.get() instanceof UUIDSyncPacket packet) {
					uuid = packet.getUuid();
					sendPacket(packet.acknowledge());
				} else if (data.get() instanceof ClientDiscoveryPacket packet) {
					knownClients.addAll(packet.getNewClients());
				} else {
					dispatcher.dispatch(data.get());
				}
			});
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
			sendPacket(new TerminatePacket());
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
