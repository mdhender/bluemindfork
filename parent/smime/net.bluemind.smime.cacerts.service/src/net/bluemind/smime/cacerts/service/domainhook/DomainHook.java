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
package net.bluemind.smime.cacerts.service.domainhook;

import java.util.Arrays;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;

public class DomainHook extends DomainHookAdapter {

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		String containerUid = ISmimeCacertUids.domainCreatedCerts(domain.uid);

		IContainers containerService = context.su().provider().instance(IContainers.class);
		ContainerDescriptor descriptor = new ContainerDescriptor();
		descriptor.type = ISmimeCacertUids.TYPE;
		descriptor.uid = containerUid;
		descriptor.owner = domain.uid;
		descriptor.name = containerUid;
		descriptor.defaultContainer = true;
		descriptor.offlineSync = false;
		descriptor.readOnly = false;
		descriptor.domainUid = domain.uid;
		containerService.create(containerUid, descriptor);

		addDomainReadAccess(context, containerUid, domain.uid);
	}

	private void addDomainReadAccess(BmContext context, String containerUid, String domainUid) {
		context.su().provider().instance(IContainerManagement.class, containerUid)
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(domainUid, Verb.Read)));
	}

	@Override
	public void onBeforeDelete(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		context.su().getServiceProvider().instance(IContainers.class)
				.delete(ISmimeCacertUids.domainCreatedCerts(domain.uid));
	}

}
