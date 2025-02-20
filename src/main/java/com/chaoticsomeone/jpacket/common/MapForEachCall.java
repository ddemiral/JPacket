package com.chaoticsomeone.jpacket.common;

import java.util.Iterator;
import java.util.Map;

@FunctionalInterface
public interface MapForEachCall<K, V> {
	void call(Iterator<Map.Entry<K, V>> iterator, K key, V value) throws Exception;
}
