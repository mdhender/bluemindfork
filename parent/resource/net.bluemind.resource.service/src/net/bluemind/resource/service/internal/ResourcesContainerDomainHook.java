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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;

public class ResourcesContainerDomainHook extends DomainHookAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ResourcesContainerDomainHook.class);

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		// FIXME: asked by BM-5300. Not sure
		logger.info("create default resource type for domain [{}]", domain.uid);
		IResourceTypes resources = context.su().provider().instance(IResourceTypes.class, domain.uid);
		resources.create("default", ResourceTypeDescriptor.create("default"));

	}

}
