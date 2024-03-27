package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AclServiceTests {
	private Container container;
	private AclStore aclStore;

	private SecurityContext context;
	private ContainerStore containerStore;
	private String testGroup = "testGroup";
	private String domainUid = "testbm.lan";

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		BmConfIni ini = new BmConfIni();

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		Server pg = new Server();
		pg.tags = Arrays.asList(TagDescriptor.bm_pgsql_data.getTag());
		pg.ip = ini.get("bluemind/postgres-tests");

		PopulateHelper.initGlobalVirt(pipo, pg);

		PopulateHelper.addDomain(domainUid);
		PopulateHelper.addUser("subject", domainUid);

		PopulateHelper.addUser("accesstest", domainUid);

		context = new SecurityContext("testSessionId", "test", Arrays.<String>asList(testGroup),
				Arrays.<String>asList(), domainUid);

		Sessions.get().put(context.getSessionId(), context);

		containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), context);

		String containerUid = "test_" + System.nanoTime();
		container = Container.create(containerUid, "test", "test", "subject", domainUid, true);
		container = containerStore.create(container);
		assertNotNull(container);


		aclStore = new AclStore(new BmTestContext(SecurityContext.SYSTEM),
				JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void implicitOwnerRights() throws ServerFault, SQLException, ElasticsearchException, IOException {
		aclStore.deleteAll(container);
		List<AccessControlEntry> actual = service().get();
		assertTrue(can(actual, "subject", Verb.All));
	}

	@Test
	public void expandAcl() throws ServerFault, SQLException, ElasticsearchException, IOException {
		aclStore.store(container, List.of(AccessControlEntry.create("test", Verb.All)));
		List<AccessControlEntry> actual = service().get();
		Verb[] all = Verb.values();
		for (Verb verb : all) {
			assertTrue(actual.contains(AccessControlEntry.create("test", verb)));
		}
	}

	@Test
	public void compactAcl() throws ServerFault, SQLException, ElasticsearchException, IOException {
		service().store(List.of(AccessControlEntry.create("test", Verb.Write),
				AccessControlEntry.create("test", Verb.Read), AccessControlEntry.create("test", Verb.Invitation),
				AccessControlEntry.create("test", Verb.SendOnBehalf)));
		List<AccessControlEntry> actual = aclStore.get(container);
		assertEquals(2, actual.size());
		assertTrue(actual.contains(AccessControlEntry.create("test", Verb.Write)));
		assertTrue(actual.contains(AccessControlEntry.create("test", Verb.SendOnBehalf)));

		service().retrieveAndStore(List.of(AccessControlEntry.create("test", Verb.Write),
				AccessControlEntry.create("test", Verb.Read), AccessControlEntry.create("test", Verb.Invitation)));
		actual = aclStore.get(container);
		assertEquals(1, actual.size());
		assertTrue(actual.contains(AccessControlEntry.create("test", Verb.Write)));
	}

	private boolean can(List<AccessControlEntry> acls, String subject, Verb verb) {
		return acls.stream().filter(acl -> acl.subject.equals(subject)).anyMatch(acl -> acl.verb.can(verb));
	}
	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected AclService service() throws ServerFault {
		return new AclService(new BmTestContext(context), context, JdbcTestHelper.getInstance().getDataSource(),
				container);
	}

}
