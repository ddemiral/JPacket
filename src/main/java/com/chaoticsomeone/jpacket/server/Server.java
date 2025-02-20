package com.chaoticsomeone.jpacket.server;

import com.chaoticsomeone.jpacket.common.*;
import com.chaoticsomeone.jpacket.packet.Packet;
import com.chaoticsomeone.jpacket.packet.PacketData;
import com.chaoticsomeone.jpacket.packet.PacketDispatcher;
import com.chaoticsomeone.jpacket.packet.defaulttypes.ClientDiscoveryPacket;
import com.chaoticsomeone.jpacket.packet.defaulttypes.ResponsePacket;
import com.chaoticsomeone.jpacket.packet.defaulttypes.TerminatePacket;
import com.chaoticsomeone.jpacket.packet.defaulttypes.UUIDSyncPacket;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends Thread {
	private final PacketDispatcher dispatcher;
	private final int port;
	private final int maxClients;

	private ServerSocket serverSocket;
	private volatile boolean isRunning;
	private volatile boolean isAdvertising;

	private final Map<UUID, ClientSocket> clientSockets = new ConcurrentHashMap<>();
	private final Thread clientAcceptThread;

	public Server(PacketDispatcher dispatcher, int port, int maxClients) {
		this.dispatcher = dispatcher;
		this.port = port;
		this.maxClients = maxClients;

		clientAcceptThread = new Thread(() -> {
			while (isAdvertising) {
				final Optional<ClientSocket> clientSocket = ClientSocket.fromAccepted(serverSocket);

				clientSocket.ifPresent((socket) -> {
					UUID clientUuid = UUID.randomUUID();

					ConditionalRunner.runOrElse(canAcceptClient(clientSocket), () -> {
						CollectionUtils.mapForEach(clientSockets, (iterator, uuid, other) -> {
							PacketIO.getInstance().sendPacket(new ClientDiscoveryPacket(clientUuid), other, dispatcher, uuid);
						});

						PacketIO.getInstance().sendPacket(new ClientDiscoveryPacket(clientSockets.keySet()), socket, dispatcher, clientUuid);

						clientSockets.put(clientUuid, socket);

						PacketIO.getInstance().sendPacket(new UUIDSyncPacket(clientUuid), socket, dispatcher, clientUuid);
					}, () -> PacketIO.getInstance().sendPacket(new TerminatePacket(), socket, dispatcher, clientUuid));
				});
			}
		});
	}

	private boolean canAcceptClient(Optional<ClientSocket> clientSocket) {
		if (clientSocket.isPresent()) {
			ClientSocket socket = clientSocket.get();
			return clientSockets.size() < maxClients && !socket.isClosed() && !clientSockets.containsValue(socket);
		}

		return false;
	}

	@Override
	public void run() {
		initialize();

		while (isRunning) {
			CollectionUtils.mapForEach(clientSockets, (iterator,uuid, clientSocket) -> {
				try {
					if (clientSocket.isClosed() || !clientSocket.areBytesAvailable()) {
						return;
					}

					Optional<Packet<PacketData>> data = PacketIO.getInstance().readFromStream(clientSocket.getIn(), dispatcher);

					ConditionalRunner.run(data.isPresent(), () -> {
						handlePacket(data.get(), clientSocket, uuid, iterator);
						Thread.sleep(10);
					});
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			});
		}
	}

	private void handlePacket(Packet<PacketData> packet, ClientSocket clientSocket, UUID uuid, Iterator<Map.Entry<UUID, ClientSocket>> iterator) throws IOException {
		UUID destinationUuid = packet.getDestinationUUID();

		if (destinationUuid.equals(PacketIO.DESTINATION_SERVER)) {
			handlePacketSelf(packet, clientSocket, uuid, iterator);
		} else if (destinationUuid.equals(PacketIO.DESTINATION_BROADCAST)) {
			CollectionUtils.mapForEach(clientSockets, (iterator1, clientUuid, socket) -> {
				PacketIO.getInstance().sendPacket(packet, socket);
			});
		} else if (clientSockets.containsKey(destinationUuid)) {
			ClientSocket destinationSocket = clientSockets.get(packet.getDestinationUUID());
			PacketIO.getInstance().sendPacket(packet, destinationSocket);
		} else {
			throw new IllegalArgumentException("Unknown destination uuid: " + destinationUuid);
		}
	}

	private void handlePacketSelf(Packet<PacketData> packet, ClientSocket clientSocket, UUID uuid, Iterator<Map.Entry<UUID, ClientSocket>> iterator) throws IOException {
		PacketData data = packet.getData();

		if (data instanceof TerminatePacket) {
			handleTerminatePacket(clientSocket, uuid, iterator);
		} else if (data instanceof UUIDSyncPacket uuidPacket) {
			handleUuidSyncPacket(uuidPacket, clientSocket, iterator);
		} else {
			dispatcher.dispatch(packet);
			PacketIO.getInstance().sendPacket(new ResponsePacket(200, data), clientSocket, dispatcher, uuid);
		}
	}

	private void handleTerminatePacket(ClientSocket clientSocket, UUID uuid, Iterator<Map.Entry<UUID, ClientSocket>> iterator) throws IOException {
		PacketIO.getInstance().sendPacket(new TerminatePacket(), clientSocket, dispatcher, uuid);
		iterator.remove();
		close(); // @ToDo
	}

	private void handleUuidSyncPacket(UUIDSyncPacket packet, ClientSocket clientSocket, Iterator<Map.Entry<UUID, ClientSocket>> iterator) throws IOException {
		if (packet.getState() == 0) {
			clientSocket.close();
			iterator.remove();
		}
	}

	private void initialize() {
		try {
			serverSocket = new ServerSocket(port);
			clientAcceptThread.start();
			isRunning = true;
			isAdvertising = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			isRunning = false;
			stopAdvertising();

			for (Map.Entry<UUID, ClientSocket> client : clientSockets.entrySet()) {
				client.getValue().close();
			}

			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stopAdvertising() {
		isAdvertising = false;
	}

	public PacketDispatcher getDispatcher() {
		return dispatcher;
	}

	public int getPort() {
		return port;
	}

	public int getMaxClients() {
		return maxClients;
	}

	public boolean isRunning() {
		return isRunning;
	}
}
