package com.chaoticsomeone.jpacket.packet.defaulttypes;

import com.chaoticsomeone.jpacket.packet.PacketData;

public class ResponsePacket implements PacketData {
	private final int status;
	private final PacketData request;

	public ResponsePacket(int status, PacketData request) {
		this.status = status;
		this.request = request;
	}

	@Override
	public void process() {

	}

	public int getStatus() {
		return status;
	}

	public PacketData getRequest() {
		return request;
	}

	@Override
	public String toString() {
		return "ResponsePacket{" +
				"status=" + status +
				", request=" + request +
				'}';
	}
}
