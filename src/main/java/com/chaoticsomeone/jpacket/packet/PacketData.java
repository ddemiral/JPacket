package com.chaoticsomeone.jpacket.packet;

import java.io.Serializable;

public interface PacketData extends Serializable {
	void process();
}
