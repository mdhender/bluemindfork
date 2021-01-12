package net.bluemind.authentication.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.provider.LogoutHook;
import net.bluemind.core.context.SecurityContext;

public class TestLogoutHook implements LogoutHook {

	public static final CountDownLatch latch = new CountDownLatch(1);

	@Override
	public void beforeLogout(SecurityContext securityContext, AuthUser user) {
		assertNotNull(securityContext);
		assertEquals("admin0", user.value.login);
		latch.countDown();
	}

}
