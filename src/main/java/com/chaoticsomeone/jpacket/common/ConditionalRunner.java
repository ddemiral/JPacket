package com.chaoticsomeone.jpacket.common;

public class ConditionalRunner {
	public static void run(boolean condition, UnsafeRunnable runnable) {
		try {
			if (condition) {
				runnable.run();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void runOrElse(boolean condition, UnsafeRunnable trueRunnable, UnsafeRunnable falseRunnable) {
		run(condition, condition ? trueRunnable : falseRunnable);
	}
}
