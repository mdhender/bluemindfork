/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.system.ldap.importation.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.commons.scanner.RepportStatus;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.tests.defaultdata.PopulateHelper;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class ImportLdapJobTests {
	private ItemValue<Domain> domain;
	private Date initialDate;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		String domainUid = "bm.lan";

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain(domainUid);
		domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class).get(domainUid);

		// Initial date set to 5 minutes ago
		initialDate = new Date((new Date().getTime()) - 5 * 60000);
		domain.value.properties.put(LdapProperties.import_ldap_lastupdate.name(),
				ImportLdapJob.getDateInGeneralizedTimeFormat(initialDate));
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class).update(domainUid,
				domain.value);
	}

	@Test
	public void scanSuccess() throws ServerFault {
		Date startDate = new Date();
		assertNotEquals(ImportLdapJob.getDateInGeneralizedTimeFormat(startDate),
				ImportLdapJob.getDateInGeneralizedTimeFormat(initialDate));

		ImportLdapJob importLdapJob = new ImportLdapJob();
		importLdapJob.updateLastUpdateDomainDate(domain,
				getImportStatus(false, false).repportStatus.get().getJobStatus(), startDate);

		ItemValue<Domain> d = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
				.get(domain.uid);

		assertEquals(ImportLdapJob.getDateInGeneralizedTimeFormat(startDate),
				d.value.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	@Test
	public void scanWarning() throws ServerFault {
		Date startDate = new Date();
		assertNotEquals(ImportLdapJob.getDateInGeneralizedTimeFormat(startDate),
				ImportLdapJob.getDateInGeneralizedTimeFormat(initialDate));

		ImportLdapJob importLdapJob = new ImportLdapJob();
		importLdapJob.updateLastUpdateDomainDate(domain,
				getImportStatus(true, false).repportStatus.get().getJobStatus(), startDate);

		ItemValue<Domain> d = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
				.get(domain.uid);

		assertEquals(ImportLdapJob.getDateInGeneralizedTimeFormat(initialDate),
				d.value.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	@Test
	public void scanError() throws ServerFault {
		Date startDate = new Date();
		assertNotEquals(ImportLdapJob.getDateInGeneralizedTimeFormat(startDate),
				ImportLdapJob.getDateInGeneralizedTimeFormat(initialDate));

		ImportLdapJob importLdapJob = new ImportLdapJob();
		importLdapJob.updateLastUpdateDomainDate(domain,
				getImportStatus(false, true).repportStatus.get().getJobStatus(), startDate);

		ItemValue<Domain> d = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
				.get(domain.uid);

		assertEquals(ImportLdapJob.getDateInGeneralizedTimeFormat(initialDate),
				d.value.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	private ImportLogger getImportStatus(boolean warnings, boolean errors) {
		ImportLogger importStatus = new ImportLogger(Optional.empty(), Optional.empty(),
				Optional.of(new RepportStatus()));

		if (warnings) {
			importStatus.warning(new HashMap<String, String>());
		}

		if (errors) {
			importStatus.error(new HashMap<String, String>());
		}

		return importStatus;
	}
}
