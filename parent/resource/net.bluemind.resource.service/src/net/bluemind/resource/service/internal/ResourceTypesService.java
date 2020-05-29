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
package net.bluemind.resource.service.internal;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.PathParam;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.utils.ImageUtils;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.document.storage.DocumentStorage;
import net.bluemind.document.storage.IDocumentStore;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceType;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.persistence.ResourceTypeStore;
import net.bluemind.role.api.BasicRoles;

public class ResourceTypesService implements IResourceTypes {

	private ResourceTypeStore store;
	private BmContext context;
	private ResourceTypesValidator validator = new ResourceTypesValidator();
	private String domainUid;
	private Sanitizer sanitizer;
	private Validator extValidator;
	private IDocumentStore iconStore;
	private RBACManager rbacManager;

	public ResourceTypesService(BmContext context, String domainUid, Container resourcesContainer) throws ServerFault {
		this.context = context;
		this.domainUid = domainUid;
		this.store = new ResourceTypeStore(context.getDataSource(), resourcesContainer);
		this.iconStore = DocumentStorage.store;
		sanitizer = new Sanitizer(context);
		extValidator = new Validator(context);
		rbacManager = new RBACManager(context).forContainer(resourcesContainer);
	}

	@Override
	public void create(String uid, ResourceTypeDescriptor descriptor) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_RESOURCE_TYPE);

		if (store.otherExists(descriptor.label, Optional.empty())) {
			throw new ServerFault("resource type " + descriptor.label + " already exists", ErrorCode.ALREADY_EXISTS);
		}

		sanitizer.create(descriptor);

		ParametersValidator.notNullAndNotEmpty(uid);
		validator.validate(descriptor);
		extValidator.create(descriptor);

		store.create(uid, descriptor);
	}

	@Override
	public void update(String uid, ResourceTypeDescriptor descriptor) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_RESOURCE_TYPE);

		ParametersValidator.notNullAndNotEmpty(uid);

		if (store.otherExists(descriptor.label, Optional.of(uid))) {
			throw new ServerFault("resource type " + descriptor.label + " already exists", ErrorCode.ALREADY_EXISTS);
		}

		ResourceTypeDescriptor previous = null;
		try {
			previous = store.get(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		sanitizer.update(previous, descriptor);

		validator.validate(descriptor);
		extValidator.update(previous, descriptor);

		if (previous == null) {
			throw new ServerFault("ResourceType " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		// FIXME Check Resource ref type
		store.update(uid, descriptor);

	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_RESOURCE_TYPE);

		ParametersValidator.notNullAndNotEmpty(uid);
		ResourceTypeDescriptor previous = null;
		try {
			previous = store.get(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (previous == null) {
			throw new ServerFault("ResourceType " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		final IResources resourcesService = context.provider().instance(IResources.class, domainUid);
		IDirectory dir = context.provider().instance(IDirectory.class, domainUid);
		dir.search(DirEntryQuery.filterKind(Kind.RESOURCE)).values.forEach(r -> {
			ResourceDescriptor resourceDescriptor = resourcesService.get(r.value.entryUid);
			if (resourceDescriptor.typeIdentifier.equals(uid)) {
				throw new ServerFault("Resource type is still referenced by Resource " + resourceDescriptor.label);
			}
		});

		if (iconStore.exists(uid)) {
			iconStore.delete(uid);
		}

		store.delete(uid);

	}

	@Override
	public ResourceTypeDescriptor get(@PathParam("uid") String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_MANAGE_RESOURCE_TYPE, BasicRoles.ROLE_MANAGER);
		try {
			return store.get(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<ResourceType> getTypes() throws ServerFault {
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_MANAGE_RESOURCE_TYPE, BasicRoles.ROLE_MANAGER);
		try {
			return store.getTypes();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public byte[] getIcon(String uid) throws ServerFault {
		try {
			if (store.get(uid) == null) {
				return null;
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		byte[] data = iconStore.get(domainUid + "/resourceTypes/" + uid + "-icon");
		if (data == null) {
			return ResourceDirHandler.EMPTY_PNG;
		}
		return data;
	}

	@Override
	public void setIcon(String uid, byte[] icon) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_RESOURCE_TYPE);
		if (get(uid) == null) {
			throw new ServerFault("ResourceType " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		byte[] png = ImageUtils.checkAndSanitize(icon);

		iconStore.store(domainUid + "/resourceTypes/" + uid + "-icon", png);
	}

}
