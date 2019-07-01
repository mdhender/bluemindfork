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
package net.bluemind.system.ldap.importation.internal;

import java.util.Locale;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.system.ldap.importation.api.ILdapImport;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.LdapHelper;
import net.bluemind.system.ldap.importation.internal.tools.LdapParametersValidator;

public class LdapImportService implements ILdapImport {
	private BmContext context;

	public LdapImportService(BmContext context) {
		this.context = context;
	}

	@Override
	public void testParameters(String hostname, String protocol, String allCertificate, String baseDn, String loginDn,
			String password, String userFilter, String groupFilter) throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()
				&& !context.getSecurityContext().getRoles().contains(SecurityContext.ROLE_ADMIN)) {
			throw new ServerFault("Only admin users can test LDAP parameters", ErrorCode.FORBIDDEN);
		}

		Locale userLocale = new Locale(context.getSecurityContext().getLang());
		LdapParametersValidator.checkLdapHostname(hostname, userLocale);
		LdapParametersValidator.checkLdapProtocol(protocol, userLocale);
		LdapParametersValidator.checkLdapAllCertificate(allCertificate);
		LdapParametersValidator.checkLdapBaseDn(baseDn, userLocale);
		LdapParametersValidator.checkLdapLoginDn(loginDn, userLocale);
		LdapParametersValidator.checkLdapUserFilter(userFilter, userLocale);
		LdapParametersValidator.checkLdapGroupFilter(groupFilter, userLocale);

		LdapParameters importLdapParameters = LdapParameters.build(hostname, protocol, allCertificate,
				baseDn, loginDn, password);

		LdapHelper.checkLDAPParameters(importLdapParameters);
	}

	@Override
	public void fullSync(String domainUid) throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("Only global.virt users can start LDAP global sync", ErrorCode.FORBIDDEN);
		}
		ParametersValidator.notNullAndNotEmpty(domainUid);

		IDomains domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		ItemValue<Domain> domainValue = domainService.get(domainUid);
		if (domainValue == null) {
			throw new ServerFault("Invalid domain UID: " + domainUid, ErrorCode.INVALID_PARAMETER);
		}

		if (!Boolean.parseBoolean(domainValue.value.properties.get(LdapProperties.import_ldap_enabled.name()))) {
			throw new ServerFault(
					"LDAP import is disabled for domain: " + domainValue.uid + " - " + domainValue.value.name);
		}

		domainValue.value.properties.remove(LdapProperties.import_ldap_lastupdate.name());
		domainService.update(domainValue.uid, domainValue.value);

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class).start(LdapConstants.JID,
				domainValue.value.name);
	}
}
