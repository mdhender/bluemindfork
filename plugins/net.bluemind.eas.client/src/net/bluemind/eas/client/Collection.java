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
package net.bluemind.eas.client;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <Collection> <SyncKey>f0e0ec53-40a6-432a-bfee-b8c1d391478c</SyncKey>
 * <CollectionId>179</CollectionId> <Status>1</Status> </Collection>
 * 
 * 
 */
public class Collection {

	private String syncKey;
	private String collectionId;
	private SyncStatus status;
	private List<Add> adds = new LinkedList<Add>();
	private Element element;

	public String getSyncKey() {
		return syncKey;
	}

	public void setSyncKey(String syncKey) {
		this.syncKey = syncKey;
	}

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public SyncStatus getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = SyncStatus.getSyncStatus(status);
	}

	public List<Add> getAdds() {
		return adds;
	}

	public void addAdd(Add applicationData) {
		adds.add(applicationData);
	}

	public Element getElement() {
		return element;
	}

	public void setElement(Element element) {
		this.element = element;
	}

	public boolean hasMoreAvailable() {
		NodeList list = element.getElementsByTagName("MoreAvailable");
		return list.getLength() == 1;
	}

}
