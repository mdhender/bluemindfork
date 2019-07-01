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
package net.bluemind.documentfolder.domainfolder;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.documentfolder.api.IDocumentFolderUids;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;

public class DomainHookFolder extends DomainHookAdapter {

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {

		ContainerDescriptor abContainerDescriptor = ContainerDescriptor.create(
				IDocumentFolderUids.getDocumentFolderContainerUid(domain.uid),
				domain.displayName + "'s document folder", context.getSecurityContext().getSubject(),
				IDocumentFolderUids.TYPE, domain.uid, true);

		IContainers containers = context.provider().instance(IContainers.class);

		containers.create(abContainerDescriptor.uid, abContainerDescriptor);

	}

	@Override
	public void onBeforeDelete(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		context.provider().instance(IContainers.class)
				.delete(IDocumentFolderUids.getDocumentFolderContainerUid(domain.uid));
	}

}
