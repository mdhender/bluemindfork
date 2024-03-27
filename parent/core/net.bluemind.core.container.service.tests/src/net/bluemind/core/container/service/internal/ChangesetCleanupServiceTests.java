package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ChangesetCleanupServiceTests {
	protected String partition;
	protected String user1Uid;
	protected String user1MboxRoot;
	protected String domainUid = "test" + System.currentTimeMillis() + ".lab";
	private Container container;
	private ChangesetCleanupService service;

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.port", "1144");
	}

	protected IServiceProvider provider(String userUid) {
		SecurityContext secCtx = new SecurityContext("sid-" + userUid, userUid, Collections.emptyList(),
				Collections.emptyList(), domainUid);
		return ServerSideServiceProvider.getProvider(secCtx);
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);
		partition = "dataloc__" + domainUid.replace('.', '_');
		JdbcActivator.getInstance().addMailboxDataSource("dataloc",
				JdbcTestHelper.getInstance().getMailboxDataDataSource());
		PopulateHelper.initGlobalVirt(pipo);
		PopulateHelper.addDomain(domainUid, Routing.internal);
		user1Uid = PopulateHelper.addUser("u1-" + System.currentTimeMillis(), domainUid, Routing.internal);
		user1MboxRoot = "user." + user1Uid.replace('.', '^');
		assertNotNull(user1Uid);
		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				SecurityContext.SYSTEM);
		String containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);
		service = new ChangesetCleanupService(JdbcTestHelper.getInstance().getMailboxDataDataSource(), pipo.name);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void shouldDelete2ItemsOlderThan50Days() throws SQLException {
		Integer days = 100;
		Integer expiredBeforeDays = 50;
		populateContainerChangeset(1, container.id, "test", 0, days, 99L);
		populateContainerChangeset(2, container.id, "test", 2, days, 99L);
		populateContainerChangeset(1, container.id, "test1", 0, days, 100L);
		populateContainerChangeset(2, container.id, "test1", 2, days, 100L);
		assertEquals(2, getQChangesetCleanup(container.id));
		service.deleteOldDeletedChangesetItems(expiredBeforeDays);
		assertEquals(0, getQChangesetCleanup(container.id));
	}

	@Test
	public void shouldDelete1ItemsOlderThan50Days() throws SQLException {
		Integer days = 100;
		Integer expiredBeforeDays = 50;
		populateContainerChangeset(1, container.id, "test", 0, days, 99L);
		populateContainerChangeset(2, container.id, "test", 1, days, 99L);
		populateContainerChangeset(1, container.id, "test1", 0, days, 100L);
		populateContainerChangeset(2, container.id, "test1", 2, days, 100L);
		assertEquals(1, getQChangesetCleanup(container.id));
		service.deleteOldDeletedChangesetItems(expiredBeforeDays);
		assertEquals(0, getQChangesetCleanup(container.id));
	}

	private int populateContainerChangeset(int version, long containerId, String itemUid, int type, int days,
			long itemId) throws SQLException {
		try (Connection con = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
				PreparedStatement stm = con.prepareStatement(
						"INSERT INTO t_changeset (version, container_id, item_uid, type, date, item_id, weight_seed) VALUES"
								+ "(?,?,?,?,?,?,1);")) {
			stm.setInt(1, version);
			stm.setLong(2, containerId);
			stm.setString(3, itemUid);
			stm.setInt(4, type);
			stm.setTimestamp(5, new Timestamp(Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli()));
			stm.setLong(6, itemId);
			return stm.executeUpdate();
		}

	}

	private int getQChangesetCleanup(long containerId) throws SQLException {
		try (Connection con = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
				PreparedStatement stm = con
						.prepareStatement("SELECT count(*) FROM q_changeset_cleanup WHERE container_id=?;")) {
			stm.setLong(1, containerId);
			ResultSet rs = stm.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		}

	}

}
