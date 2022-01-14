package net.bluemind.user.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.persistence.security.HashAlgorithm;
import net.bluemind.user.persistence.security.HashFactory;

public class UserStoreTests {
	private static Logger logger = LoggerFactory.getLogger(UserStoreTests.class);
	private UserStore userStore;
	private ItemStore userItemStore;
	private Container mbox;
	private String uid;
	private String domainUid;
	private String domainUid2;
	private UserStore userStore2;
	private ItemStore userItemStore2;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		PopulateHelper.initGlobalVirt();

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		domainUid = "test" + System.nanoTime() + ".fr";
		domainUid2 = "test2" + System.nanoTime() + ".fr";

		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.createTestDomain(domainUid2);

		this.uid = "test_" + System.nanoTime();

		String mboxId = "mailbox_" + uid;
		this.mbox = Container.create(mboxId, "mailbox", mboxId, "me", true);
		mbox = containerStore.create(mbox);

		userItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), containerStore.get(domainUid),
				securityContext);

		userStore = new UserStore(JdbcTestHelper.getInstance().getDataSource(), containerStore.get(domainUid));

		userItemStore2 = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), containerStore.get(domainUid2),
				securityContext);

		userStore2 = new UserStore(JdbcTestHelper.getInstance().getDataSource(), containerStore.get(domainUid2));

		logger.debug("stores: {} {}", userItemStore, userStore);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateGetUpdateDelete() throws Exception {
		userItemStore.create(Item.create(uid, null));
		Item item = userItemStore.get(uid);
		User u = getDefaultUser();
		userStore.create(item, u);

		User created = userStore.get(item);
		assertNotNull("Nothing found", created);
		created.login = "updated_" + System.nanoTime();
		userStore.update(item, created);

		User found = userStore.get(item);
		assertNotNull("Nothing found", found);
		assertEquals(created.login, found.login);

		userStore.delete(item);
		found = userStore.get(item);
		assertNull(found);
	}

	private User getDefaultUser() {
		User u = new User();
		u.login = "test" + System.nanoTime();
		u.password = "password";
		u.routing = Routing.external;
		u.archived = false;
		u.hidden = false;
		u.system = false;
		Email e = new Email();
		e.address = u.login + "@blue-mind.loc";
		u.emails = Arrays.asList(e);
		u.dataLocation = null;
		return u;
	}

	@Test
	public void testCustomProperties() throws Exception {
		userItemStore.create(Item.create(uid, null));
		Item item = userItemStore.get(uid);
		User u = getDefaultUser();
		userStore.create(item, u);

		User created = userStore.get(item);
		assertEquals(0, created.properties.size());

		Map<String, String> properties = new HashMap<>();
		u.properties = properties;
		userStore.update(item, u);
		created = userStore.get(item);
		assertEquals(0, created.properties.size());

		properties.put("wat", "da funk");
		u.properties = properties;
		userStore.update(item, u);
		created = userStore.get(item);
		assertEquals(1, created.properties.size());
		assertEquals("da funk", created.properties.get("wat"));
	}

	@Test
	public void testCreatePasswordShouldSetAlgorithm() throws Exception {
		userItemStore.create(Item.create(uid, null));
		Item item = userItemStore.get(uid);
		User u = getDefaultUser();
		u.password = HashFactory.getDefault().create(u.password);
		userStore.create(item, u);

		User created = userStore.get(item);
		assertEquals(HashFactory.DEFAULT, HashFactory.algorithm(created.password));
	}

	@Test
	public void testCreatePasswordWithKnownAlgorithm() throws Exception {
		userItemStore.create(Item.create(uid, null));
		Item item = userItemStore.get(uid);
		User u = getDefaultUser();
		u.password = HashFactory.get(HashAlgorithm.SSHA512).create(u.password);
		userStore.create(item, u);

		User created = userStore.get(item);
		assertEquals(HashAlgorithm.SSHA512, HashFactory.algorithm(created.password));
	}

	@Test
	public void testFindByLogin() throws Exception {
		userItemStore.create(Item.create("t1", null));
		Item item = userItemStore.get("t1");
		User u = getDefaultUser();
		u.login = "test1";
		userStore.create(item, u);

		userItemStore2.create(Item.create("t2", null));
		item = userItemStore2.get("t2");
		u = getDefaultUser();
		u.login = "test1";
		userStore2.create(item, u);

		String uid = userStore.byLogin("test1");
		assertNotNull(uid);
		assertEquals("t1", uid);

		uid = userStore.byLogin("fakeLogin");
		assertNull(uid);

		uid = userStore2.byLogin("test1");
		assertNotNull(uid);
		assertEquals("t2", uid);

		// test login is case insensitive
		uid = userStore2.byLogin("tEst1");
		assertNotNull(uid);
		assertEquals("t2", uid);
	}
}
