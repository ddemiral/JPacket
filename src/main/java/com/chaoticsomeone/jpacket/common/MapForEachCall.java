package com.chaoticsomeone.jpacket.common;

@FunctionalInterface
public interface MapForEachCall<K, V> {
	void call(K key, V value);
}
