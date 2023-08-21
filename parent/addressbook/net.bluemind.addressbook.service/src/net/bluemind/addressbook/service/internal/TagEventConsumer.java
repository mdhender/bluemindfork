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
package net.bluemind.addressbook.service.internal;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.persistence.VCardIndexStore;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemUri;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.AuditLogService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.persistence.TagReferencesStore;
import net.bluemind.tag.service.ITagEventConsumer;

public class TagEventConsumer implements ITagEventConsumer {

	private static final Logger logger = LoggerFactory.getLogger(TagEventConsumer.class);

	public TagEventConsumer() {
	}

	@Override
	public void tagChanged(String tagContainerUid, String tagUid) {
		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

		DataSource dsTags = DataSourceRouter.get(context, tagContainerUid);
		TagReferencesStore tagReferencesStore = new TagReferencesStore(dsTags);

		List<ItemUri> items = null;
		try {
			items = tagReferencesStore.referencedBy(IAddressBookUids.TYPE, tagContainerUid, tagUid);
		} catch (SQLException e) {
			logger.error("error during retrieving references ", e);
			return;
		}

		if (items.isEmpty()) {
			return;
		}

		Container currentContainer = null;
		DataSource dsCards = null;

		for (ItemUri itemUri : items) {
			if (currentContainer != null && !itemUri.containerUid.equals(currentContainer.uid)) {
				notifyContainerChanged(currentContainer);
			}

			if (currentContainer == null || !itemUri.containerUid.equals(currentContainer.uid)) {
				try {
					dsCards = DataSourceRouter.get(context, itemUri.containerUid);
					ContainerStore containerStore = new ContainerStore(context, dsCards, SecurityContext.SYSTEM);
					currentContainer = containerStore.get(itemUri.containerUid);
				} catch (SQLException e) {
					logger.error("error during loading container ", e);
				}
			}

			if (currentContainer == null) {
				continue;
			}

			BaseContainerDescriptor containerDescriptor = BaseContainerDescriptor.create(currentContainer.uid,
					currentContainer.name, currentContainer.owner, currentContainer.type, currentContainer.domainUid,
					currentContainer.defaultContainer);
			containerDescriptor.internalId = currentContainer.id;
			AuditLogService<VCard> logService = new AuditLogService<>(context.getSecurityContext(),
					containerDescriptor);
			VCardContainerStoreService vcardContainerStore = new VCardContainerStoreService(context, dsCards,
					SecurityContext.SYSTEM, currentContainer, new VCardStore(dsCards, currentContainer),
					new VCardIndexStore(ESearchActivator.getClient(), currentContainer,
							DataSourceRouter.location(context, currentContainer.uid)),
					logService);

			try {
				vcardContainerStore.touch(itemUri.itemUid);
			} catch (ServerFault e) {
				logger.error("error during updating item ", e);
			}

		}

		ItemValue<Tag> tag = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ITags.class, tagContainerUid).getComplete(tagUid);
		if (tag == null) {
			logger.info("Remove references");
			try {
				tagReferencesStore.deleteReferences(IAddressBookUids.TYPE, tagContainerUid, tagUid);
			} catch (SQLException e) {
				logger.error("Fail to remove references ", e);
			}
		}

		if (currentContainer != null) {
			notifyContainerChanged(currentContainer);
		}
	}

	private void notifyContainerChanged(Container currentContainer) {
		new AddressBookEventProducer(currentContainer, SecurityContext.SYSTEM, VertxPlatform.eventBus()).changed();
	}
}
