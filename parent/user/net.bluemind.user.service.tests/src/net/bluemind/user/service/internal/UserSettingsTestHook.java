package net.bluemind.user.service.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.bluemind.user.hook.settings.IUserSettingsHook;

public class UserSettingsTestHook implements IUserSettingsHook {

	private static CountDownLatch latch = new CountDownLatch(1);

	@Override
	public void onSettingsUpdate(String containerUid, String userUid) {
		latch.countDown();
	}

	public static void reset() {
		latch = new CountDownLatch(1);
	}

	public static boolean called() {
		try {
			return latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

}
