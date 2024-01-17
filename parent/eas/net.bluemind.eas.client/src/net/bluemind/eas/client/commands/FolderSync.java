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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.client.AccountInfos;
import net.bluemind.eas.client.Folder;
import net.bluemind.eas.client.FolderSyncResponse;
import net.bluemind.eas.client.FolderType;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.utils.DOMUtils;

/**
 * Performs a FolderSync AS command with the given sync key
 * 
 * 
 */
public class FolderSync extends TemplateBasedCommand<FolderSyncResponse> {

	private String syncKey;

	public FolderSync(String syncKey) {
		super(NS.FolderHierarchy, "FolderSync", "FolderSyncRequest.xml");
		this.syncKey = syncKey;
	}

	@Override
	protected void customizeTemplate(AccountInfos ai, OPClient opc) {
		Element sk = DOMUtils.getUniqueElement(tpl.getDocumentElement(),
				"SyncKey");
		sk.setTextContent(syncKey);
	}

	@Override
	protected FolderSyncResponse parseResponse(Element root) {
		String key = DOMUtils.getElementText(root, "SyncKey");
		int count = Integer.parseInt(DOMUtils.getElementText(root, "Count"));
		Map<FolderType, List<Folder>> ret = new HashMap<FolderType, List<Folder>>(
				count + 1);

		// TODO process updates / deletions
		NodeList nl = root.getElementsByTagName("Add");
		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);
			Folder f = new Folder();
			f.setServerId(DOMUtils.getElementText(e, "ServerId"));
			f.setParentId(DOMUtils.getElementText(e, "ParentId"));
			f.setName(DOMUtils.getElementText(e, "DisplayName"));
			f.setType(FolderType.getValue(Integer.parseInt(DOMUtils
					.getElementText(e, "Type"))));
			List<Folder> list = ret.get(f.getType());
			if (list == null) {
				list = new LinkedList<Folder>();
				ret.put(f.getType(), list);
			}
			list.add(f);
		}

		return new FolderSyncResponse(key, ret);
	}

}
