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

package net.bluemind.core.container.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.ContainerElement;
import net.bluemind.core.auditlogs.ContainerElement.ContainerElementBuilder;
import net.bluemind.core.auditlogs.DefaultLogMapperProvider;
import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.auditlogs.ItemElement;
import net.bluemind.core.auditlogs.OwnerElement;
import net.bluemind.core.auditlogs.SecurityContextElement;
import net.bluemind.core.auditlogs.client.loader.AuditLogClientLoader;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class AuditLogService<T> {

	private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
	private ILogMapperProvider<T> mapper;

	private AuditLogClientLoader auditLogClientProvider = new AuditLogClientLoader();
	private IAuditLogClient auditLogClient;
	private SecurityContext securityContext;
	private BaseContainerDescriptor container;

	public AuditLogService(SecurityContext sc, BaseContainerDescriptor cont, ILogMapperProvider<T> documentMapper) {
		securityContext = sc;
		container = cont;
		mapper = documentMapper;
		auditLogClient = auditLogClientProvider.get();
	}

	public AuditLogService(SecurityContext sc, BaseContainerDescriptor cont) {
		securityContext = sc;
		container = cont;
		this.mapper = new DefaultLogMapperProvider<>();
		auditLogClient = auditLogClientProvider.get();
	}

	public void log(Item item, T oldValue, T newValue, Type action) {
		AuditLogEntry defaultAuditLogEntry = createAuditLogEntry(item, newValue, action);
		AuditLogEntry auditLogEntry = mapper.enhanceAuditLogEntry(item, oldValue, newValue, action,
				defaultAuditLogEntry);
		if (auditLogEntry != null) {
			store(auditLogEntry);
		}
	}

	public void loginLog(SecurityContext securityContext) {
		AuditLogEntry auditLogEntry = new AuditLogEntry();
		SecurityContextElement securityContextElement;

		try {
			DirEntry entrySecurityContext = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDirectory.class, securityContext.getContainerUid())
					.findByEntryUid(securityContext.getOwnerPrincipal());
			if (entrySecurityContext != null) {
				securityContextElement = new SecurityContextElement.SecurityContextElementBuilder()
						.displayName(entrySecurityContext.displayName).email(entrySecurityContext.email)
						.uid(securityContext.getSubject()).origin(securityContext.getOrigin()).build();
				auditLogEntry.securityContext = securityContextElement;
			}
		} catch (ServerFault e) {
			logger.error("Problem fetching security context data : {}", e.getMessage());
			securityContextElement = new SecurityContextElement.SecurityContextElementBuilder()
					.displayName(securityContext.getSubjectDisplayName()).uid(securityContext.getSubject())
					.origin(securityContext.getOrigin()).build();
			auditLogEntry.securityContext = securityContextElement;
		}

		auditLogEntry.logtype = "LoginEvent";
		auditLogEntry.action = Type.Created.name();
		store(auditLogEntry);
	}

	protected void store(AuditLogEntry entry) {
		auditLogClient.store(entry);
	}

	public AuditLogEntry createAuditLogEntry(Item item, T newValue, Type action) {
		SecurityContextElement securityContextElement;
		ItemElement itemElement;
		AuditLogEntry auditLogEntry = new AuditLogEntry();

		ContainerElementBuilder builder = new ContainerElement.ContainerElementBuilder();
		builder.name(container.name).uid(container.uid);

		try {
			if (container.domainUid != null && container.owner != null) {
				DirEntry entryContainer = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDirectory.class, container.domainUid).findByEntryUid(container.owner);
				if (entryContainer != null) {

					OwnerElement ownerElement = new OwnerElement.OwnerElementBuilder()
							.displayName(entryContainer.displayName).email(entryContainer.email)
							.entryUid(entryContainer.entryUid).path(entryContainer.path).build();
					builder.ownerElement(ownerElement);
				}
			}
		} catch (ServerFault e) {
			logger.error("Problem fetching container owner data : {}", e.getMessage());
		}

		auditLogEntry.container = builder.build();

		try {
			DirEntry entrySecurityContext = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDirectory.class, securityContext.getContainerUid())
					.findByEntryUid(securityContext.getOwnerPrincipal());
			if (entrySecurityContext != null) {
				securityContextElement = new SecurityContextElement.SecurityContextElementBuilder()
						.displayName(entrySecurityContext.displayName).email(entrySecurityContext.email)
						.uid(securityContext.getSubject()).origin(securityContext.getOrigin()).build();
				auditLogEntry.securityContext = securityContextElement;
			}
		} catch (ServerFault e) {
			logger.error("Problem fetching security context data : {}", e.getMessage());
			securityContextElement = new SecurityContextElement.SecurityContextElementBuilder()
					.displayName(securityContext.getSubjectDisplayName()).uid(securityContext.getSubject())
					.origin(securityContext.getOrigin()).build();
			auditLogEntry.securityContext = securityContextElement;
		}
		itemElement = new ItemElement.ItemElementBuilder().uid(item.uid).id(item.id).displayName(item.displayName)
				.version(item.version).build();
		auditLogEntry.item = itemElement;
		auditLogEntry.logtype = newValue.getClass().getSimpleName();

		auditLogEntry.action = action.toString();

		return auditLogEntry;
	}

}
