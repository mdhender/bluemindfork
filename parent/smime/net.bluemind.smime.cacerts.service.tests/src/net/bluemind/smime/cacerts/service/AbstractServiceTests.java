/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.smime.cacerts.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;
import net.bluemind.smime.cacerts.api.ISmimeRevocation;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.persistence.SmimeCacertStore;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractServiceTests {
	private static Logger logger = LoggerFactory.getLogger(AbstractServiceTests.class);

	protected SmimeCacertStore smimeStore;
	protected ItemStore itemStore;
	protected AclStore aclStore;

	protected SecurityContext defaultSecurityContext;
	protected Container container;

	protected BmContext defaultContext;

	protected String datalocation;
	protected DataSource dataDataSource;
	protected String domainUid;
	protected String owner;

	ContainerStoreService<SmimeCacert> smimeContainerStoreService;

	ISystemConfiguration systemConfiguration;
	private static final String GLOBAL_EXTERNAL_URL = "my.test.external.url";

	@Before
	public void before() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		JdbcTestHelper.getInstance().beforeTest();
		PopulateHelper.initGlobalVirt();

		domainUid = "bm.lan";
		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);

		PopulateHelper.addDomain(domainUid);
		owner = PopulateHelper.addUser("test", domainUid);

		defaultSecurityContext = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.asList(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME, BasicRoles.ROLE_MANAGE_SYSTEM_CONF), domainUid);

		defaultContext = new BmTestContext(defaultSecurityContext);

		Sessions.get().put(defaultSecurityContext.getSessionId(), defaultSecurityContext);

		container = createTestContainer();
		itemStore = new ItemStore(dataDataSource, container, defaultSecurityContext);

		aclStore = new AclStore(defaultContext, dataDataSource);
		aclStore.store(container,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		smimeContainerStoreService = new ContainerStoreService<>(dataDataSource, defaultSecurityContext, container,
				new SmimeCacertStore(dataDataSource, container));

		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	protected Container createTestContainer() throws SQLException {
		ContainerStore containerHome = new ContainerStore(defaultContext, dataDataSource, defaultSecurityContext);

		String containerUid = ISmimeCacertUids.domainCreatedCerts(domainUid);
		Container container = Container.create(containerUid, ISmimeCacertUids.TYPE, "test", owner, domainUid);
		container = containerHome.create(container);
		assertNotNull(container);

		containerHome = new ContainerStore(new BmTestContext(defaultSecurityContext),
				JdbcActivator.getInstance().getDataSource(), defaultSecurityContext);
		containerHome.createOrUpdateContainerLocation(container, datalocation);

		return container;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected ItemValue<SmimeCacert> createAndGet(String uid, SmimeCacert cert) {
		try {
			itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
			Item item = itemStore.get(uid);

			new ChangelogStore(dataDataSource, container)
					.itemCreated(ChangelogStore.LogEntry.create(uid, item.externalId, "test", "junit", item.id, 0));
			getServiceCacert(defaultSecurityContext, container.uid).create(item.uid, cert);
			return ItemValue.create(item, smimeStore.get(item));

		} catch (SQLException e) {
			logger.error("error during S/MIME persistence call", e);
			fail(e.getMessage());
			return null;
		}

	}

	protected abstract ISmimeCACert getServiceCacert(SecurityContext context, String containerUid) throws ServerFault;

	protected abstract ISmimeRevocation getServiceRevocation(SecurityContext context, String domainUid)
			throws ServerFault;

	public static SmimeCacert defaultSmimeCacert(String filepath) throws IOException {
		SmimeCacert cert = new SmimeCacert();
		cert.cert = getCertData(filepath);
		return cert;
	}

	public static String getCertData(String filepath) throws IOException {
		return new String(Files.toByteArray(new File(filepath)));
	}

	protected void setGlobalExternalUrl() {
		systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), GLOBAL_EXTERNAL_URL);
		systemConfiguration.updateMutableValues(sysValues);
	}

}
