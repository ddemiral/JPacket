package com.chaoticsomeone.jpacket.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class ClientSocket {
	private final Socket socket;
	private final DataInputStream in;
	private final DataOutputStream out;

	private boolean isClosed;

	public ClientSocket(Socket socket) throws IOException {
		this.socket = socket;
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		isClosed = false;
	}

	public ClientSocket(String host, int port) throws IOException {
		this(new Socket(host, port));
	}

	public static Optional<ClientSocket> fromAccepted(ServerSocket serverSocket) {
		try {
			Socket socket = serverSocket.accept();
			return Optional.of(new ClientSocket(socket));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	public void close() throws IOException {
		if (socket != null && !socket.isClosed()) socket.close();
		if (in != null) in.close();
		if (out != null) out.close();

		isClosed = true;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public boolean areBytesAvailable() {
		try {
			return in.available() > 0;
		} catch (IOException e) {
			return false;
		}
	}

	public Socket getSocket() {
		return socket;
	}

	public DataInputStream getIn() {
		return in;
	}

	public DataOutputStream getOut() {
		return out;
	}
}
