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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.service.internal;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.utils.ImageUtils;
import net.bluemind.document.api.Document;
import net.bluemind.document.api.DocumentMetadata;
import net.bluemind.document.api.IDocument;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.system.api.CustomLogo;

public class CustomTheme {

	private static final Logger logger = LoggerFactory.getLogger(CustomTheme.class);

	private BmContext context;
	private Container container;
	private ItemStore itemStore;

	public CustomTheme(BmContext context, String containerUid) {
		this.context = context;

		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(), SecurityContext.SYSTEM);
		try {
			container = containerStore.get(containerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		itemStore = new ItemStore(context.getDataSource(), container, context.getSecurityContext());

		MQ.registerProducer(Topic.UI_RESOURCES_NOTIFICATIONS);
		logger.debug("created.");
	}

	private String getLogoUid(String uid) {
		return "logo_" + uid;
	}

	private String getLogoVersion(String logoUid, long version) {
		return logoUid + "-" + version;
	}

	public void setLogo(String uid, byte[] logo) throws ServerFault {
		String docUid = getLogoUid(uid);
		Item item;
		try {
			item = itemStore.get(docUid);
			if (item == null) {
				item = new Item();
				item.uid = docUid;
				item.displayName = "logo for " + uid;
				item = itemStore.create(item);
			} else {
				item = itemStore.touch(docUid);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		IDocument service = context.provider().instance(IDocument.class, container.uid, item.uid);

		service.delete(docUid);

		byte[] sanitized = ImageUtils.checkAndSanitize(logo);
		byte[] content = ImageUtils.resize(sanitized, 140, 40);

		Document doc = new Document();
		doc.content = content;
		doc.metadata = new DocumentMetadata();
		doc.metadata.uid = docUid;
		doc.metadata.description = "Logo for " + uid;
		doc.metadata.filename = "logo_" + uid + ".png";
		doc.metadata.name = "Logo " + uid;
		doc.metadata.mime = "image/png";

		service.create(docUid, doc);

		OOPMessage message = MQ.newMessage();
		message.putStringProperty("operation", "setLogo");
		message.putStringProperty("entity", uid);
		message.putStringProperty("version", getLogoVersion(docUid, item.version));
		MQ.getProducer(Topic.UI_RESOURCES_NOTIFICATIONS).send(message);

	}

	public void deleteLogo(String uid) throws ServerFault {
		String docUid = getLogoUid(uid);
		Item item;
		try {
			item = itemStore.get(docUid);
			if (item == null) {
				throw ServerFault.notFound("item for logo " + uid + " not found");
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		IDocument service = context.provider().instance(IDocument.class, container.uid, item.uid);

		service.delete(docUid);

		try {
			itemStore.delete(item);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		OOPMessage message = MQ.newMessage();
		message.putStringProperty("operation", "deleteLogo");
		message.putStringProperty("entity", uid);
		MQ.getProducer(Topic.UI_RESOURCES_NOTIFICATIONS).send(message);
	}

	public CustomLogo getLogo(String uid) {
		String docUid = getLogoUid(uid);
		Item item;
		try {
			item = itemStore.get(docUid);
			if (item == null) {
				return null;
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		IDocument service = context.provider().instance(IDocument.class, container.uid, item.uid);
		Document doc = service.fetch(docUid);

		CustomLogo cl = new CustomLogo();
		cl.content = doc.content;
		cl.version = getLogoVersion(docUid, item.version);
		return cl;
	}

}
