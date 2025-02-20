package com.chaoticsomeone.jpacket.common;

import java.util.Iterator;
import java.util.Map;

public class CollectionUtils {
	public static <K, V> void mapForEach(Map<K, V> map, MapForEachCall<K, V> callable) {
		Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<K, V> entry = iterator.next();
			callable.call(entry.getKey(), entry.getValue());
		}
	}
}
