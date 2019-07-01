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
package net.bluemind.eas.client.commands;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.client.AccountInfos;
import net.bluemind.eas.client.Add;
import net.bluemind.eas.client.Collection;
import net.bluemind.eas.client.Folder;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.client.SyncResponse;
import net.bluemind.eas.utils.DOMUtils;

/**
 * Performs a Sync AS command for the given folders with 0 as syncKey
 * 
 * 
 */
public class Sync extends TemplateBasedCommand<SyncResponse> {

	protected Folder[] folders;
	private Map<Folder, String> syncKeys;

	public Sync(Folder... folders) {
		super(NS.AirSync, "Sync", "SyncRequest.xml");
		this.folders = folders;
		this.syncKeys = new HashMap<Folder, String>();
	}

	public Sync(Document doc) {
		super(NS.AirSync, "Sync", doc);
		this.syncKeys = new HashMap<Folder, String>();
	}

	@Override
	protected void customizeTemplate(AccountInfos ai, OPClient opc) {
		Element cols = DOMUtils.getUniqueElement(tpl.getDocumentElement(), "Collections");
		for (Folder folder : folders) {
			Element col = DOMUtils.createElement(cols, "Collection");
			folder.setXml(col);
			DOMUtils.createElementAndText(col, "SyncKey", syncKeys.get(folder) != null ? syncKeys.get(folder) : "0");
			DOMUtils.createElementAndText(col, "CollectionId", folder.getServerId());
		}
	}

	@Override
	protected SyncResponse parseResponse(Element root) {
		Map<String, Collection> ret = new HashMap<String, Collection>();

		NodeList nl = root.getElementsByTagName("Collection");
		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);
			Collection col = new Collection();
			col.setElement(e);
			col.setSyncKey(DOMUtils.getElementText(e, "SyncKey"));
			col.setCollectionId(DOMUtils.getElementText(e, "CollectionId"));
			col.setStatus(Integer.getInteger(DOMUtils.getElementText(e, "Status"), 0));
			NodeList ap = e.getElementsByTagName("Add");
			for (int j = 0; j < ap.getLength(); j++) {
				Element appData = (Element) ap.item(j);
				String serverId = DOMUtils.getElementText(appData, "ServerId");
				Add add = new Add();
				add.setServerId(serverId);
				col.addAdd(add);
			}
			ret.put(col.getCollectionId(), col);
		}

		SyncResponse sr = new SyncResponse(ret);
		sr.dom = root.getOwnerDocument();
		return sr;
	}

	public void setKey(Folder f, String sk) {
		syncKeys.put(f, sk);
	}

}
