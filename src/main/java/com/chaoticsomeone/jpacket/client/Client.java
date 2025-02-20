package com.chaoticsomeone.jpacket.client;

import com.chaoticsomeone.jpacket.common.ClientSocket;
import com.chaoticsomeone.jpacket.common.ConditionalRunner;
import com.chaoticsomeone.jpacket.common.PacketIO;
import com.chaoticsomeone.jpacket.packet.Packet;
import com.chaoticsomeone.jpacket.packet.PacketData;
import com.chaoticsomeone.jpacket.packet.PacketDispatcher;
import com.chaoticsomeone.jpacket.packet.defaulttypes.ClientDiscoveryPacket;
import com.chaoticsomeone.jpacket.packet.defaulttypes.TerminatePacket;
import com.chaoticsomeone.jpacket.packet.defaulttypes.UUIDSyncPacket;

import java.io.IOException;
import java.util.*;

public class Client {
	private final PacketDispatcher dispatcher;
	private ClientSocket socket;
	private UUID uuid;
	private final Set<UUID> knownClients = new LinkedHashSet<>();

	private volatile boolean isRunning;

	public Client(PacketDispatcher dispatcher) {
		this.dispatcher = dispatcher;

		isRunning = false;
	}

	public void connect(String host, int port) throws IOException {
		socket = new ClientSocket(host, port);
		isRunning = true;

		startListening();
	}

	public void sendPacket(PacketData data) throws IOException {
		sendPacket(data, PacketIO.DESTINATION_SERVER);
	}

	public void sendPacket(PacketData data, int destinationIndex) {
		new Thread(() -> {
			try {
				while (knownClients.isEmpty()) {
					Thread.sleep(10);
				}

				sendPacket(data, new ArrayList<>(knownClients).get(destinationIndex));
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public void sendPacket(PacketData data, UUID destination) throws IOException {
		PacketIO.getInstance().sendPacket(data, socket, dispatcher, destination);
	}

	private void receivePacket() {
		try {
			Optional<Packet<PacketData>> packet = PacketIO.getInstance().readFromStream(socket.getIn(), dispatcher);

			ConditionalRunner.run(packet.isPresent(), () -> {
				PacketData packetData = packet.get().getData();

				if (packetData instanceof TerminatePacket) {
					close();
				} else if (packetData instanceof UUIDSyncPacket uuidPacket) {
					uuid = uuidPacket.getUuid();
					sendPacket(uuidPacket.acknowledge());
					System.out.println("Client acknowledged UUID " + uuid);
				} else if (packetData instanceof ClientDiscoveryPacket discoveryPacket) {
					knownClients.addAll(discoveryPacket.getNewClients());
					System.out.println(discoveryPacket.getNewClients());
				} else {
					dispatcher.dispatch(packet.get());
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
