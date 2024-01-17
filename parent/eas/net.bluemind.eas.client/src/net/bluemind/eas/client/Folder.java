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

import org.w3c.dom.Element;

public class Folder {

	private String serverId;
	private String parentId;
	private String name;
	private FolderType type;

	private Element xml;

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String exchangeId) {
		this.serverId = exchangeId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public FolderType getType() {
		return type;
	}

	public void setType(FolderType type) {
		this.type = type;
	}

	public Element getXml() {
		return xml;
	}

	public void setXml(Element xml) {
		this.xml = xml;
	}

}
