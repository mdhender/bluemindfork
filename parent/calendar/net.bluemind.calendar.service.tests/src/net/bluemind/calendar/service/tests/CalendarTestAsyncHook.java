package net.bluemind.calendar.service.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.bluemind.calendar.hook.VEventMessage;

public class CalendarTestAsyncHook extends CalendarTestHook {

	private static CountDownLatch latch = new CountDownLatch(1);
	private static Action action;
	private static VEventMessage receivedMessage;

	@Override
	protected void open(Action verb, VEventMessage message) {
		receivedMessage = message;
		action = verb;
		latch.countDown();
	}

	public static void reset() {
		latch = new CountDownLatch(1);
	}

	public static VEventMessage message() {
		return waitEvent() ? receivedMessage : null;
	}

	public static Action action() {
		return waitEvent() ? action : null;
	}

	public static boolean waitEvent() {
		try {
			return latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

}
