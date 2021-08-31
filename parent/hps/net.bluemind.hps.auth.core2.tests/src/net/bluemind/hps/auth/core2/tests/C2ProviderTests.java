package net.bluemind.hps.auth.core2.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hps.auth.core2.C2Provider;
import net.bluemind.hps.auth.core2.C2ProviderFactory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.proxy.http.ExternalCreds;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.ILogoutListener;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class C2ProviderTests {

	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		domainUid = String.format("domain-%s.tld", System.currentTimeMillis());
		PopulateHelper.initGlobalVirt();
		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.addDomainAdmin("admin0", "global.virt");
	}

	public static class TestListener implements ILogoutListener {
		@Override
		public void loggedOut(String sessionId) {
			// Do nothing
		}

		@Override
		public void checkAll() {
			// Do nothing
		}
	}

	@Test
	public void testNewSession() throws InterruptedException {
		C2ProviderFactory c2pf = new C2ProviderFactory();
		TestListener tl = new TestListener();
		c2pf.setLogoutListener(tl);
		IAuthProvider provider = c2pf.get(VertxPlatform.getVertx());
		Assert.assertNotNull(provider);
		final BlockingQueue<String> queue = new LinkedBlockingDeque<>();
		provider.sessionId("admin0@global.virt", "admin", true, Collections.emptyList(), new AsyncHandler<String>() {

			@Override
			public void success(String value) {
				queue.offer(value);
			}

			@Override
			public void failure(Throwable e) {
				e.printStackTrace();
			}

		});

		String sessionId = queue.poll(5, TimeUnit.SECONDS);
		System.err.println("sessionid " + sessionId);
		Assert.assertNotNull(sessionId);
	}

	@Test
	public void testCaseInsensitiveLogin() throws InterruptedException {
		C2ProviderFactory c2pf = new C2ProviderFactory();
		TestListener tl = new TestListener();
		c2pf.setLogoutListener(tl);
		IAuthProvider provider = c2pf.get(VertxPlatform.getVertx());
		Assert.assertNotNull(provider);
		final BlockingQueue<String> queue = new LinkedBlockingDeque<>();
		provider.sessionId("Admin0@Global.virt", "admin", true, Collections.emptyList(), new AsyncHandler<String>() {

			@Override
			public void success(String value) {
				queue.offer(value);
			}

			@Override
			public void failure(Throwable e) {
				e.printStackTrace();
			}
		});

		String sessionId = queue.poll(5, TimeUnit.SECONDS);
		System.err.println("sessionid " + sessionId);
		Assert.assertNotNull(sessionId);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void sessionId_externalCreds_invalidLogin() {
		ExternalCreds externalCreds = new ExternalCreds();
		externalCreds.setLoginAtDomain("invalid");

		new C2Provider(null, null).sessionId(externalCreds, null, new AsyncHandler<String>() {
			@Override
			public void success(String value) {
				fail("Must get a failure!");
			}

			@Override
			public void failure(Throwable e) {
				assertTrue(e instanceof ServerFault);
			}
		});
	}

	@Test
	public void sessionId_externalCreds_unknownLogin() {
		ExternalCreds externalCreds = new ExternalCreds();
		externalCreds.setLoginAtDomain(String.format("unknonwn@%s", domainUid));

		new C2Provider(VertxPlatform.getVertx(), null).sessionId(externalCreds, Collections.emptyList(),
				new AsyncHandler<String>() {
					@Override
					public void success(String value) {
						assertNull(value);
					}

					@Override
					public void failure(Throwable e) {
						fail("Must not fail!");
					}
				});
	}

	@Test
	public void sessionId_externalCreds_archivedUser() {
		String userLogin = String.format("%s", System.currentTimeMillis());

		User user = new User();
		user.login = userLogin;
		user.routing = Routing.internal;
		user.contactInfos = new VCard();
		user.contactInfos.identification.name.familyNames = "myName";
		user.archived = true;

		String emailAlias = String.format("mail.%s@%s", userLogin, domainUid);
		user.emails = Arrays.asList(Email.create(emailAlias, true, true));

		String userUid = UUID.randomUUID().toString();

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid,
				user);

		ExternalCreds externalCreds = new ExternalCreds();
		externalCreds.setLoginAtDomain(String.format("%s@%s", userLogin, domainUid));

		new C2Provider(VertxPlatform.getVertx(), null).sessionId(externalCreds, Collections.emptyList(),
				new AsyncHandler<String>() {
					@Override
					public void success(String value) {
						assertNull(value);
					}

					@Override
					public void failure(Throwable e) {
						fail("Must not fail!");
					}
				});
	}

	@Test
	public void sessionId_externalCreds_latdIsMailboxName() {
		String userLogin = String.format("%s", System.currentTimeMillis());

		User user = new User();
		user.login = userLogin;
		user.contactInfos = new VCard();
		user.contactInfos.identification.name.familyNames = "myName";
		user.routing = Routing.internal;

		String emailAlias = String.format("mail.%s@%s", userLogin, domainUid);
		user.emails = Arrays.asList(Email.create(emailAlias, true, true));

		String userUid = UUID.randomUUID().toString();

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid,
				user);

		ExternalCreds externalCreds = new ExternalCreds();
		externalCreds.setLoginAtDomain(String.format("%s@%s", userLogin, domainUid));

		new C2Provider(VertxPlatform.getVertx(), null).sessionId(externalCreds, Collections.emptyList(),
				new AsyncHandler<String>() {
					@Override
					public void success(String value) {
						assertFalse(Strings.isNullOrEmpty(value));
					}

					@Override
					public void failure(Throwable e) {
						fail("Must not fail!");
					}
				});
	}

	@Test
	public void sessionId_externalCreds_latdIsMailAlias() {
		String userLogin = String.format("%s", System.currentTimeMillis());

		User user = new User();
		user.login = userLogin;
		user.contactInfos = new VCard();
		user.contactInfos.identification.name.familyNames = "myName";
		user.routing = Routing.internal;

		String emailAlias = String.format("mail.%s@%s", userLogin, domainUid);
		user.emails = Arrays.asList(Email.create(emailAlias, true, true));

		String userUid = UUID.randomUUID().toString();

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid,
				user);

		ExternalCreds externalCreds = new ExternalCreds();
		externalCreds.setLoginAtDomain(emailAlias);

		new C2Provider(VertxPlatform.getVertx(), null).sessionId(externalCreds, Collections.emptyList(),
				new AsyncHandler<String>() {
					@Override
					public void success(String value) {
						assertFalse(Strings.isNullOrEmpty(value));
					}

					@Override
					public void failure(Throwable e) {
						fail("Must not fail!");
					}
				});
	}

	@Test
	public void sessionId_externalCreds_routingNone() {
		String userLogin = String.format("%s", System.currentTimeMillis());

		User user = new User();
		user.login = userLogin;
		user.contactInfos = new VCard();
		user.contactInfos.identification.name.familyNames = "myName";
		user.routing = Routing.none;

		String userUid = UUID.randomUUID().toString();

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid,
				user);

		ExternalCreds externalCreds = new ExternalCreds();
		externalCreds.setLoginAtDomain(String.format("%s@%s", userLogin, domainUid));

		new C2Provider(VertxPlatform.getVertx(), null).sessionId(externalCreds, Collections.emptyList(),
				new AsyncHandler<String>() {
					@Override
					public void success(String value) {
						assertFalse(Strings.isNullOrEmpty(value));
					}

					@Override
					public void failure(Throwable e) {
						fail("Must not fail!");
					}
				});
	}
}
