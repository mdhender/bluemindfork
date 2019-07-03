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
package net.bluemind.system.ldap.importation.internal.scanner;

import java.io.IOException;
import java.util.Optional;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper.DeleteTreeException;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class MemberLdapScannerTests extends ScannerMemberMemberOf {
	private ItemValue<Domain> domain;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer(false);
	}

	@Before
	public void before() throws LdapException, DeleteTreeException, IOException {
		domain = initDomain();

		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);
	}

	private ItemValue<Domain> initDomain() {
		Domain domain = new Domain();

		domain.name = "member.virt";

		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(),
				new BmConfIni().get(DockerContainer.LDAP.getName()));
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), LdapDockerTestHelper.LDAP_ROOT_DN);
		domain.properties.put(LdapProperties.import_ldap_login_dn.name(), LdapDockerTestHelper.LDAP_LOGIN_DN);
		domain.properties.put(LdapProperties.import_ldap_password.name(), LdapDockerTestHelper.LDAP_LOGIN_PWD);

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(),
				LdapProperties.import_ldap_user_filter.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_group_filter.name(),
				LdapProperties.import_ldap_group_filter.getDefaultValue());

		domain.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(),
				LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

		return ItemValue.create(Item.create(domain.name, null), domain);
	}

	@Override
	protected Domain getDomain() {
		return domain.value;
	}

	@Override
	protected void scanLdap(ImportLogger importLogger, CoreServicesTest coreService, LdapParameters ldapParameters) {
		MemberLdapScanner ldapScanner = new MemberLdapScanner(importLogger, coreService, ldapParameters, domain);
		ldapScanner.scan();
	}

	@Override
	protected void scanLdap(ImportLogger importLogger, CoreServicesTest coreService, LdapParameters ldapParameters,
			Optional<String> beforeDate) {
		MemberLdapScanner ldapScanner = new MemberLdapScanner(importLogger, coreService,
				ldapParameters.updateLastUpdate(beforeDate), domain);
		ldapScanner.scan();
	}
}
