/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.ldap.export.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.LdapHelper;
import net.bluemind.system.ldap.export.hook.LdapServerHook;
import net.bluemind.system.ldap.export.objects.DomainDirectoryRoot;

public class DomainRootUpdateService {
	private static final Logger logger = LoggerFactory.getLogger(DomainRootUpdateService.class);

	public static DomainRootUpdateService build(BmContext context, ItemValue<Server> server, ItemValue<Domain> domain) {
		return new DomainRootUpdateService(domain, server);
	}

	public static Optional<DomainRootUpdateService> build(String domainUid) {
		if (domainUid == null || domainUid.isEmpty()) {
			throw new ServerFault("Invalid domain UID", ErrorCode.INVALID_PARAMETER);
		}

		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		ItemValue<Domain> domain = context.provider().instance(IDomains.class, domainUid).get(domainUid);
		if (domain == null) {
			throw new ServerFault.notFound(String.format("Domain %s not found", domainUid));
		}

		List<ItemValue<Server>> ldapExportServers = ldapExportServer(context, domain.uid);
		if (ldapExportServers.size() != 1) {
			return Optional.empty();
		}

		return Optional.of(new DomainRootUpdateService(domain, ldapExportServers.get(0)));
	}

	public static List<ItemValue<Server>> ldapExportServer(BmContext context, String domainUid) {
		IServer server = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		List<String> ldapExportServersUids = server.byAssignment(domainUid, LdapServerHook.LDAPTAG);

		if (ldapExportServersUids.size() == 0) {
			return Collections.emptyList();
		}

		return ldapExportServersUids.stream().map(serverUid -> server.getComplete(serverUid))
				.collect(Collectors.toList());
	}

	private final ItemValue<Domain> domain;
	private final ItemValue<Server> ldapExportServer;

	private DomainRootUpdateService(ItemValue<Domain> domain, ItemValue<Server> ldapExportServer) {
		this.domain = domain;
		this.ldapExportServer = ldapExportServer;
	}

	public void sync() throws Exception {
		try (LdapConnection ldapCon = LdapHelper.connectDirectory(ldapExportServer)) {
			updateDomain(ldapCon);
		} catch (Exception e) {
			throw e;
		}
	}

	private void updateDomain(LdapConnection ldapCon) throws LdapInvalidDnException, LdapException {
		DomainDirectoryRoot domainDirectoryRoot = new DomainDirectoryRoot(domain);
		Entry ldapEntry = ldapCon.lookup(new Dn(domainDirectoryRoot.getDn()));

		if (ldapEntry == null) {
			LdapHelper.addLdapEntry(ldapCon, new DomainDirectoryRoot(domain).getLdapEntry());
			return;
		}

		LdapHelper.modifyLdapEntry(ldapCon, domainDirectoryRoot.getModifyRequest(ldapEntry));
	}
}
