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
package net.bluemind.system.ldap.importation.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.directory.api.ldap.codec.decorators.SearchResultEntryDecorator;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.importation.search.DirectorySearch;
import net.bluemind.system.importation.search.LdapSearchCursor;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.internal.tools.LdapHelper;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class LdapSearchTestHelper {
	public static LdapParameters getLdapParameters() {
		return getLdapParameters(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				LdapDockerTestHelper.LDAP_LOGIN_DN, LdapDockerTestHelper.LDAP_LOGIN_PWD, null);
	}

	public static LdapParameters getLdapParametersWithSplitGroup(String splitGroupName) {
		return getLdapParameters(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(), null, null,
				splitGroupName);
	}

	public static LdapParameters getLdapParameters(String entryUuid, String login, String password,
			String splitGroupName) {
		Domain domain = Domain.create("", "", "", Collections.<String>emptySet());
		domain.properties = new HashMap<>();
		BmConfIni config = new BmConfIni();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(),
				config.get(DockerContainer.LDAP.getHostProperty()));
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), LdapDockerTestHelper.LDAP_ROOT_DN);
		domain.properties.put(LdapProperties.import_ldap_group_filter.name(),
				LdapProperties.import_ldap_group_filter.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_user_filter.name(),
				LdapProperties.import_ldap_user_filter.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(), entryUuid);

		if (splitGroupName != null) {
			domain.properties.put(LdapProperties.import_ldap_relay_mailbox_group.name(), splitGroupName);
		}

		if (login != null && password != null) {
			domain.properties.put(LdapProperties.import_ldap_login_dn.name(), login);
			domain.properties.put(LdapProperties.import_ldap_password.name(), password);
		}

		return LdapParameters.build(domain, Collections.<String, String>emptyMap());
	}

	public static LdapConProxy getConnection(LdapParameters parameters) throws ServerFault {
		return LdapHelper.connectLdap(parameters);
	}

	public static String getDate() {
		SimpleDateFormat sdf = new SimpleDateFormat(LdapConstants.GENERALIZED_TIME_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date());
	}

	private static Entry getExistingGroupEntry(LdapParameters ldapParameters, String dn)
			throws LdapInvalidDnException, LdapException, IOException, ServerFault, CursorException {
		Entry entry = null;

		try (LdapConProxy ldapCon = LdapHelper.connectLdap(ldapParameters)) {
			LdapSearchCursor entries = new DirectorySearch<>(ldapParameters, new LdapGroupSearchFilter(),
					new LdapUserSearchFilter()).findByFilterAndBaseDnAndScopeAndAttributes(ldapCon, "(objectclass=*)",
							new Dn(dn), SearchScope.OBJECT, "*", "+",
							LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

			while (entries.next()) {
				Response response = entries.get();

				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				entry = ((SearchResultEntryDecorator) response).getEntry();
			}
		}

		return entry;
	}

	public static void updateEntry(LdapParameters ldapParameters, String dn)
			throws IOException, ServerFault, LdapException, CursorException {
		Entry entry = getExistingGroupEntry(ldapParameters, dn);

		ModifyRequestImpl modifyRequest = new ModifyRequestImpl();
		modifyRequest.setName(entry.getDn());
		modifyRequest.replace("description", "Incremental scan " + new Date().toString());
		try (LdapConProxy ldapCon = LdapHelper.connectLdap(ldapParameters)) {
			ModifyResponse mr = ldapCon.modify(modifyRequest);
			assertEquals(ResultCodeEnum.SUCCESS, mr.getLdapResult().getResultCode());
		}
	}
}
