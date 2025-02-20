package com.chaoticsomeone.jpacket.packet.defaulttypes;

import com.chaoticsomeone.jpacket.packet.PacketHandler;

public class ResponseHandler implements PacketHandler<ResponsePacket> {
	@Override
	public void handle(ResponsePacket data) {
		System.out.println("ResponseHandler received packet " + data);
	}
}
