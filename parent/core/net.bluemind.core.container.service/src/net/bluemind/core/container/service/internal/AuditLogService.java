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
import net.bluemind.core.auditlogs.AuditLogUpdateStatus;
import net.bluemind.core.auditlogs.AuditLogUpdateStatus.MessageCriticity;
import net.bluemind.core.auditlogs.ContainerElement;
import net.bluemind.core.auditlogs.ContainerElement.ContainerElementBuilder;
import net.bluemind.core.auditlogs.DefaultLogMapperProvider;
import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.auditlogs.ItemElement;
import net.bluemind.core.auditlogs.OwnerElement;
import net.bluemind.core.auditlogs.SecurityContextElement;
import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public abstract class AuditLogService<T, U> {

	private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
	private IAuditLogClient auditLogClient;

	protected SecurityContext securityContext;
	protected BaseContainerDescriptor container;
	protected ILogMapperProvider<U> mapper;
	protected String type;
	protected String domainUid;

	protected AuditLogService(SecurityContext sc, BaseContainerDescriptor cont, ILogMapperProvider<U> dm) {
		securityContext = sc;
		container = cont;
		mapper = dm;
		type = cont.type;
		domainUid = cont.domainUid;
		AuditLogLoader auditLogProvider = new AuditLogLoader();
		auditLogClient = auditLogProvider.getClient();
	}

	protected AuditLogService(SecurityContext sc, BaseContainerDescriptor cont) {
		securityContext = sc;
		container = cont;
		domainUid = cont.domainUid;
		type = cont.type;
		this.mapper = new DefaultLogMapperProvider<>();
		AuditLogLoader auditLogProvider = new AuditLogLoader();
		auditLogClient = auditLogProvider.getClient();
	}

	protected AuditLogService(String type, ILogMapperProvider<U> dm) {
		this.type = type;
		this.mapper = dm;
		AuditLogLoader auditLogProvider = new AuditLogLoader();
		auditLogClient = auditLogProvider.getClient();
	}

	public void logCreate(T value) {
		AuditLogEntry auditLogEntry = createAuditLogEntry(value);
		auditLogEntry.action = Type.Created.name();
		auditLogEntry.criticity = MessageCriticity.MAJOR;
		auditLogEntry.domainUid = domainUid;
		store(auditLogEntry);
	}

	public void logUpdate(T value, U oldValue) {
		AuditLogEntry auditLogEntry = createAuditLogEntry(value);
		auditLogEntry.action = Type.Updated.name();
		auditLogEntry.domainUid = domainUid;
		AuditLogUpdateStatus updateStatus = createUpdateStatus(value, oldValue);
		auditLogEntry.updatemessage = updateStatus.updateMessage;
		auditLogEntry.criticity = updateStatus.crit;
		store(auditLogEntry);
	}

	public void logDelete(T value) {
		AuditLogEntry auditLogEntry = createAuditLogEntry(value);
		auditLogEntry.domainUid = domainUid;
		auditLogEntry.action = Type.Deleted.name();
		auditLogEntry.criticity = MessageCriticity.MAJOR;
		store(auditLogEntry);
	}

	protected SecurityContextElement createSecurityContextElement(SecurityContext sc) {
		SecurityContextElement securityContextElement;
		try {
			DirEntry entrySecurityContext = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDirectory.class, sc.getContainerUid()).findByEntryUid(sc.getOwnerPrincipal());
			securityContextElement = new SecurityContextElement.SecurityContextElementBuilder()
					.displayName(entrySecurityContext.displayName).email(entrySecurityContext.email)
					.uid(sc.getSubject()).origin(sc.getOrigin()).build();
			return securityContextElement;
		} catch (Exception e) {
			securityContextElement = new SecurityContextElement.SecurityContextElementBuilder()
					.displayName(sc.getSubjectDisplayName()).uid(sc.getSubject()).origin(sc.getOrigin()).build();
			return securityContextElement;
		}

	}

	protected ContainerElement createContainerElement() {

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
		return builder.build();

	}

	protected ItemElement createItemElement(Item item) {
		return new ItemElement.ItemElementBuilder().uid(item.uid).id(item.id).displayName(item.displayName)
				.version(item.version).build();
	}

	protected void store(AuditLogEntry entry) {
		auditLogClient.storeAuditLog(entry);
	}

	protected String type() {
		return type;
	}

	public void setType(String t) {
		type = t;
	}

	protected abstract AuditLogEntry createAuditLogEntry(T value);

	protected abstract AuditLogUpdateStatus createUpdateStatus(T newValue, U oldValue);

	public void setDomainUid(String d) {
		this.domainUid = d;
	}
}
