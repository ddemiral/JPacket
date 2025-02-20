package com.chaoticsomeone.jpacket.server;

import com.chaoticsomeone.jpacket.common.*;
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
					ConditionalRunner.runOrElse(canAcceptClient(clientSocket), () -> {
						UUID clientUuid = UUID.randomUUID();

						CollectionUtils.mapForEach(clientSockets, (iterator, uuid, other) -> {
							PacketIO.getInstance().sendPacket(new ClientDiscoveryPacket(clientUuid), other, dispatcher);
						});

						PacketIO.getInstance().sendPacket(new ClientDiscoveryPacket(clientSockets.keySet()), socket, dispatcher);

						clientSockets.put(clientUuid, socket);

						PacketIO.getInstance().sendPacket(new UUIDSyncPacket(clientUuid), socket, dispatcher);
					}, () -> PacketIO.getInstance().sendPacket(new TerminatePacket(), socket, dispatcher));
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

					Optional<PacketData> data = PacketIO.getInstance().readFromStream(clientSocket.getIn(), dispatcher);

					ConditionalRunner.run(data.isPresent(), () -> {
						handlePacket(data.get(), clientSocket, iterator);
						Thread.sleep(10);
					});
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			});
		}
	}

	private void handlePacket(PacketData data, ClientSocket clientSocket, Iterator<Map.Entry<UUID, ClientSocket>> iterator) throws IOException {
		if (data instanceof TerminatePacket) {
			handleTerminatePacket(clientSocket, iterator);
		} else if (data instanceof UUIDSyncPacket packet) {
			handleUuidSyncPacket(packet, clientSocket, iterator);
		} else {
			dispatcher.dispatch(data);
			PacketIO.getInstance().sendPacket(new ResponsePacket(200, data), clientSocket, dispatcher);
		}
	}

	private void handleTerminatePacket(ClientSocket clientSocket, Iterator<Map.Entry<UUID, ClientSocket>> iterator) throws IOException {
		PacketIO.getInstance().sendPacket(new TerminatePacket(), clientSocket, dispatcher);
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
