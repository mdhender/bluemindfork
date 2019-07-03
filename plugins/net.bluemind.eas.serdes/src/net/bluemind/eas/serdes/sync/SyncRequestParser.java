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
package net.bluemind.eas.serdes.sync;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.sync.CollectionSyncRequest;
import net.bluemind.eas.dto.sync.CollectionSyncRequest.Options.ConflicResolution;
import net.bluemind.eas.dto.sync.FilterType;
import net.bluemind.eas.dto.sync.SyncRequest;
import net.bluemind.eas.serdes.IEasRequestParser;
import net.bluemind.eas.serdes.base.BodyOptionsParser;
import net.bluemind.eas.utils.DOMUtils;

public class SyncRequestParser implements IEasRequestParser<SyncRequest> {

	public static final Integer MAX_WINDOW_SIZE = 50;

	private static final Logger logger = LoggerFactory.getLogger(SyncRequestParser.class);

	public SyncRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge previousKnowledge) {
		SyncRequest sr = new SyncRequest();
		if (doc == null) {
			// empty body, reuse cached
			if (previousKnowledge.getLastMonitored() != null) {
				sr.collections = previousKnowledge.getLastMonitored();
			}
		} else {
			Element query = doc.getDocumentElement();
			NodeList nl = query.getElementsByTagName("Collection");
			for (int i = 0; i < nl.getLength(); i++) {
				Element col = (Element) nl.item(i);
				CollectionSyncRequest collec = parseCollection(col);
				if (collec != null) {
					sr.collections.add(collec);
				}
			}

			String wait = DOMUtils.getElementText(query, "Wait");
			if (wait != null) {
				sr.waitIntervalSeconds = Integer.parseInt(wait) * 60;
			}

			String heartbeat = DOMUtils.getElementText(query, "HeartbeatInterval");
			if (heartbeat != null) {
				sr.heartbeatInterval = Integer.parseInt(heartbeat);
			}

			if (query.getElementsByTagName("Partial").getLength() > 0) {
				logger.info("Partial element has been found. " + previousKnowledge.getLastMonitored().size()
						+ " collection(s) are loaded from cache");
				sr.partial = true;
				sr.collections.addAll(previousKnowledge.getLastMonitored());
			}
		}
		return sr;

	}

	private CollectionSyncRequest parseCollection(Element col) {
		CollectionSyncRequest collection = new CollectionSyncRequest();
		collection.setDataClass(DOMUtils.getElementText(col, "Class"));
		collection.setSyncKey(DOMUtils.getElementText(col, "SyncKey"));
		Element fid = DOMUtils.getUniqueElement(col, "CollectionId");
		if (fid != null) {
			try {
				collection.setCollectionId(Integer.parseInt(fid.getTextContent()));
			} catch (NumberFormatException e) {
				logger.error(e.getMessage(), e);
				return null;
			}

		}

		Element getChanges = DOMUtils.getUniqueElement(col, "GetChanges");
		if (getChanges != null) {
			if ("0".equals(getChanges.getTextContent())) {
				collection.setGetChanges(false);
			} else {
				collection.setGetChanges(true);
			}
		}

		Element wse = DOMUtils.getUniqueElement(col, "WindowSize");
		if (wse != null) {
			int windowSize = Integer.parseInt(wse.getTextContent());
			if (windowSize > MAX_WINDOW_SIZE) {
				logger.warn("Device asks WindowSize = {}. Force WindowsSize = {}", windowSize, MAX_WINDOW_SIZE);
				windowSize = MAX_WINDOW_SIZE;
			}
			collection.setWindowSize(windowSize);
		}

		Element option = DOMUtils.getUniqueElement(col, "Options");
		collection.options = new CollectionSyncRequest.Options();
		if (option != null) {
			BodyOptionsParser bop = new BodyOptionsParser();
			collection.options.bodyOptions = bop.fromOptionsElement(option);

			String conflict = DOMUtils.getElementText(option, "Conflict");
			if (conflict != null) {
				collection.options.conflictPolicy = ConflicResolution.fromXml(conflict);
			}

			String filterType = DOMUtils.getElementText(option, "FilterType");

			if (filterType != null) {
				collection.options.filterType = FilterType.getFilterType(filterType);
			}

		} else {
			// BM-6600 Sync without Options (BB)
			// force filterType to 3 days back to prevent full sync
			// TODO store Options
			logger.warn("Request without options. Force FilterType to THREE_DAYS_BACK");
			collection.options.filterType = FilterType.THREE_DAYS_BACK;
		}

		Element perform = DOMUtils.getUniqueElement(col, "Commands");

		if (perform == null) {
			return collection;
		}

		// get our sync state for this collection
		NodeList mod = perform.getChildNodes();
		Collection<CollectionItem> fetchIds = collection.getFetchIds();
		Collection<CollectionItem> deletedItems = collection.getDeletedIds();
		Collection<Element> changedItems = collection.getChangedItems();
		Collection<Element> createdItems = collection.getCreatedItems();
		Element deletesAsMoves = DOMUtils.getUniqueElement(col, "DeletesAsMoves");
		if (deletesAsMoves != null) {
			if ("1".equals(deletesAsMoves.getTextContent())) {
				collection.setDeletesAsMoves(true);
			}
		}
		for (int j = 0; j < mod.getLength(); j++) {
			Element modification = (Element) mod.item(j);
			String modType = modification.getNodeName();
			switch (modType) {
			case "Delete":
				String serverId = DOMUtils.getElementText(modification, "ServerId");
				deletedItems.add(CollectionItem.of(serverId));
				break;
			case "Change":
				changedItems.add(modification);
				break;
			case "Add":
				createdItems.add(modification);
				break;
			case "Fetch":
				fetchIds.add(CollectionItem.of(DOMUtils.getElementText(modification, "ServerId")));
				break;
			}
		}

		return collection;

	}

}
